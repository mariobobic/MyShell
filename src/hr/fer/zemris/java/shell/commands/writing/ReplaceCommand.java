package hr.fer.zemris.java.shell.commands.writing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

/**
 * Replaces a target character sequence with a replacement character sequence in
 * the name of a specified file, or in the name of all files if a directory is
 * specified.
 *
 * @author Mario Bobic
 */
public class ReplaceCommand extends AbstractCommand {
	
	/* Flags */
	/** Charset for decoding files. */
	private boolean useRegex;
	
	/**
	 * Constructs a new command object of type {@code ReplaceCommand}.
	 */
	public ReplaceCommand() {
		super("REPLACE", createCommandDescription(), createFlagDescriptions());
	}

	@Override
	public String getCommandSyntax() {
		return "(<path>) <target_sequence> <replacement_sequence>";
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
	
	/**
	 * Creates a list of {@code FlagDescription} objects where each entry
	 * describes the available flags of this command. This method is generates
	 * description exclusively for the command that this class represents.
	 * 
	 * @return a list of strings that represents description
	 */
	private static List<FlagDescription> createFlagDescriptions() {
		List<FlagDescription> desc = new ArrayList<>();
		desc.add(new FlagDescription("r", null, null, "Use regex pattern matching."));
		return desc;
	}
	
	@Override
	protected String compileFlags(Environment env, String s) {
		/* Initialize default values. */
		useRegex = false;

		/* Compile! */
		s = commandArguments.compile(s);

		/* Replace default values with flag values, if any. */
		if (commandArguments.containsFlag("r")) {
			useRegex = true;
		}
		
		return super.compileFlags(env, s);
	}
	
	@Override
	protected ShellStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			throw new SyntaxException();
		}
		
		String[] args = StringUtility.extractArguments(s);
		if (args.length < 2 || args.length > 3) {
			throw new SyntaxException();
		}
		
		Path path;
		String target;
		String replacement;
		if (args.length == 3) {
			path = Utility.resolveAbsolutePath(env, args[0]);
			target = args[1];
			replacement = args[2];
		} else {
			path = env.getCurrentPath();
			target = args[0];
			replacement = args[1];
		}
		Utility.requireExists(path);
		
		/* If sequences are equal, you are done. */
		if (target.equals(replacement)) {
			return ShellStatus.CONTINUE;
		}
		
		List<Path> files;
		if (Files.isDirectory(path)) {
			files = Files.list(path).collect(Collectors.toList());
		} else {
			files = Arrays.asList(path);
		}
		
		/* Check if the directory was empty. */
		if (files.size() == 0) {
			env.writeln("There are no files in the specified directory.");
			return ShellStatus.CONTINUE;
		}
		
		for (Path file : files) {
			String name = file.getFileName().toString();
			String newName = useRegex ?
				name.replaceAll(target, replacement) :
				name.replace(target, replacement);
			
			Path dest = file.resolveSibling(newName);
			if (!name.equalsIgnoreCase(newName)) {
				dest = Utility.firstAvailable(dest);
			}
			
			/* Atomic move serves just for case-sensitive rename. */
			Files.move(file, dest, StandardCopyOption.ATOMIC_MOVE);
		};
		
		return ShellStatus.CONTINUE;
	}

}
