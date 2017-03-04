package hr.fer.zemris.java.shell.commands.writing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * A command that is used for move one file and only one file, to another
 * location. The destination directory may or may not exist before the copying
 * is done. If the destination directory does not exist, a corresponding
 * directory structure is created. If the last name in the path name sequence is
 * an existing directory, the moving file name will be kept. Else if the last
 * name in the pathname's name sequence is a non-existing directory (a file),
 * the moving file will be renamed.
 *
 * @author Mario Bobic
 */
public class MoveCommand extends AbstractCommand {

	/**
	 * Constructs a new command object of type {@code MoveCommand}.
	 */
	public MoveCommand() {
		super("MOVE", createCommandDescription());
	}
	
	@Override
	protected String getCommandSyntax() {
		return "<path1> <path2>";
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
		desc.add("Can be used to trivially rename files and directories.");
		desc.add("The first argument must be a source file to be moved, "
				+ "whereas the second argument may be either a file or a directory.");
		desc.add("If the second argument is not a directory, the file is moved and named as specified.");
		desc.add("If the second argument is a directory, the file is moved into the directory.");
		desc.add("The destination directory may or may not exist before the copying is done.");
		desc.add("If the destination directory does not exist, a corresponding directory structure is created.");
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) {
		String[] args = Helper.extractArguments(s);
		if (args.length != 2) {
			throw new SyntaxException();
		}
		
		Path source = Helper.resolveAbsolutePath(env, args[0]);
		Path target = Helper.resolveAbsolutePath(env, args[1]);
		Helper.requireExists(source);
		
		moveFile(source, target, env);
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Validates both paths and moves <tt>source</tt> to <tt>target</tt>. This
	 * method also writes out the full path to the newly created file upon
	 * succeeding.
	 * 
	 * @param source the path to file to be copied
	 * @param target the path to destination file or directory
	 * @param env an environment
	 */
	private static void moveFile(Path source, Path target, Environment env) {
		if (Files.isDirectory(target) && !source.equals(target)) {
			target = target.resolve(source.getFileName());
		}
		
		if (Files.exists(target) && !source.equals(target)) {
			boolean overwrite = promptConfirm(env, "File " + target + " already exists. Overwrite?");
			if (!overwrite) {
				writeln(env, "Cancelled.");
				return;
			}
		}
	
		try {
			Files.createDirectories(target.getParent());
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			writeln(env, "Moved: " + target);
		} catch (IOException e) {
			writeln(env, "Could not move " + source + " to " + target);
		}
	}

}
