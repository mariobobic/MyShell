package hr.fer.zemris.java.shell.commands.writing;

import static hr.fer.zemris.java.shell.utility.CommandUtility.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.MyShell;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.interfaces.ShellCommand;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.exceptions.NotEnoughDiskSpaceException;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

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
	public String getCommandSyntax() {
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
		desc.add("Both arguments may be either files or directories.");
		desc.add("If the second argument is not a directory, the source is moved and named as specified.");
		desc.add("If the second argument is a directory, the source is moved into the directory.");
		desc.add("The destination directory structure may or may not exist before the copying is done.");
		desc.add("If the destination directory does not exist, a corresponding directory structure is created.");
		return desc;
	}

	@Override
	protected ShellStatus execute0(Environment env, String s) throws IOException {
		String[] args = StringUtility.extractArguments(s);
		if (args.length != 2) {
			throw new SyntaxException();
		}
		
		Path source = Utility.resolveAbsolutePath(env, args[0]);
		Path target = Utility.resolveAbsolutePath(env, args[1]);
		Utility.requireExists(source);
		
		moveFile(source, target, env);
		
		return ShellStatus.CONTINUE;
	}
	
	/**
	 * Validates both paths and moves <tt>source</tt> to <tt>target</tt>. This
	 * method also writes out the full path to the newly created file upon
	 * succeeding. A {@code NotEnoughDiskSpaceException} is thrown if
	 * <tt>target</tt> is located at a root directory that is different from the
	 * <tt>source</tt> root directory, and there is not enough available disk
	 * space.
	 * 
	 * @param source the path to file to be copied
	 * @param target the path to destination file or directory
	 * @param env an environment
	 * @throws NotEnoughDiskSpaceException if there is not enough disk space
	 * @throws IOException if an I/O error occurs
	 */
	private static void moveFile(Path source, Path target, Environment env) throws IOException {
		if (Files.isDirectory(target) && !source.equals(target)) {
			/* If target is a directory, but it is not a rename. */
			target = target.resolve(source.getFileName());
		}
		
		// If source and target have different root directory, the move operation wouldn't work
		if (!source.getRoot().equals(target.getRoot())) {
			ShellCommand copyCmd = MyShell.getCommand(getStaticName(CopyCommand.class));
			copyCmd.execute(env, "-s " + StringUtility.quote(source, target));
			
			ShellCommand rmCmd = MyShell.getCommand(getStaticName(RmCommand.class));
			rmCmd.execute(env, "-fs " + StringUtility.quote(source));

			env.writeln("Moved: " + target);
			return;
		}
		
		// Source and target are located in the same root directory, continue with move
		if (Files.exists(target) && !source.equals(target)) {
			String type = Files.isDirectory(target) ? "Directory " : "File ";
			boolean overwrite = promptConfirm(env, type + target + " already exists. Overwrite?");
			if (!overwrite) {
				env.writeln("Cancelled.");
				return;
			}
		}
	
		try {
			Files.createDirectories(target.getParent());
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			env.writeln("Moved: " + target);
		} catch (IOException e) {
			throw new IOException("Could not move " + source + " to " + target + ": " + e.getMessage());
		}
	}

}
