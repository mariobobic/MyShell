package hr.fer.zemris.java.shell.extracommands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A command that is used for move one file and only one file, to another
 * location. The destination directory may or may not exist before the copying
 * is done. If the destination directory does not exist, a corresponding
 * directory structure is created. If the last name in the path name sequence is
 * an existing directory, the newly made file will be named as the original
 * file. Else if the last name in the pathname's name sequence is a non-existing
 * directory (a file), the newly made file will be named as it.
 *
 * @author Mario Bobic
 */
public class MoveCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "move <path1> <path2>";

	/**
	 * Constructs a new command object of type {@code MoveCommand}.
	 */
	public MoveCommand() {
		super("MOVE", createCommandDescription());
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
		desc.add("Moves one file to another location.");
		desc.add("The first argument must be a source file to be moved, "
				+ "whereas the second argument may be either a file or a directory.");
		desc.add("If the second argument is not a directory, it means it is a new file name.");
		desc.add("If the second argument is a directory, a file with the same name is copied into it.");
		desc.add("The destination directory may or may not exist before the copying is done.");
		desc.add("If the destination directory does not exist, a corresponding directory structure is created.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) {
		String[] args = Helper.extractArguments(s, 2);
		if (args.length != 2) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		Path source = Helper.resolveAbsolutePath(env, args[0]);
		Path target = Helper.resolveAbsolutePath(env, args[1]);
		
		if (!Files.exists(source)) {
			writeln(env, "The system cannot find the file specified: " + source);
			return CommandStatus.CONTINUE;
		}
		
		if (Files.isDirectory(target)) {
			target = target.resolve(source.getFileName());
		}
		
		if (Files.exists(target)) {
			boolean overwrite = Helper.promptConfirm(env, "File " + target + " already exists. Overwrite?");
			if (!overwrite) {
				writeln(env, "Cancelled.");
				return CommandStatus.CONTINUE;
			}
		}
	
		try {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			writeln(env, "Could not move " + source + " to " + target);
		}
		
		return CommandStatus.CONTINUE;
	}

}
