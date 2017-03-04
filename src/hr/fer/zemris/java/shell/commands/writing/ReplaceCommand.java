package hr.fer.zemris.java.shell.commands.writing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * Replaces a target character sequence with a replacement character sequence in
 * the name of a specified file, or in the name of all files if a directory is
 * specified.
 *
 * @author Mario Bobic
 */
public class ReplaceCommand extends AbstractCommand {
	
	/**
	 * Constructs a new command object of type {@code ReplaceCommand}.
	 */
	public ReplaceCommand() {
		super("REPLACE", createCommandDescription());
	}

	@Override
	protected String getCommandSyntax() {
		return "<path> <target_sequence> <replacement_sequence>";
	}
	
	/**
	 * Creates a list of strings where each string represents a new line of this
	 * command's description. This method is generates description exclusively
	 * for the command that this class represents.
	 * 
	 * @return a list of strings that represents description
	 */
	private static List<String> createCommandDescription() {
		List<String> desc = new ArrayList<>();
		desc.add("Replaces a target sequence with a replacement sequence in file names.");
		desc.add("If the specified path is a file, its file name is modified.");
		desc.add("If the specified path is a directory, file names of files inside are modified.");
		return desc;
	}
	
	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			throw new SyntaxException();
		}
		
		String[] args = Helper.extractArguments(s);
		if (args.length < 3) {
			throw new SyntaxException();
		}
		
		Path path = Helper.resolveAbsolutePath(env, args[0]);
		Helper.requireExists(path);
		
		String target = args[1];
		String replacement = args[2];
		if (target.equals(replacement)) {
			return CommandStatus.CONTINUE;
		}
		
		List<Path> files;
		if (Files.isDirectory(path)) {
			files = Files.list(path).collect(Collectors.toList());
		} else {
			files = Arrays.asList(path);
		}
		
		/* Check if the directory was empty. */
		if (files.size() == 0) {
			writeln(env, "There are no files in the specified directory.");
			return CommandStatus.CONTINUE;
		}
		
		for (Path file : files) {
			String name = file.getFileName().toString();
			String newName = name.replace(target, replacement);
			
			Path dest = file.resolveSibling(newName);
			if (!name.equalsIgnoreCase(newName)) {
				dest = Helper.firstAvailable(dest);
			}
			
			/* Atomic move serves just for case-sensitive rename. */
			Files.move(file, dest, StandardCopyOption.ATOMIC_MOVE);
		};
		
		return CommandStatus.CONTINUE;
	}

}
