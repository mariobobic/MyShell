package hr.fer.zemris.java.shell.extracommands;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A command that is used for removing files and non-empty directories.
 *
 * @author Mario Bobic
 */
public class RmCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "rm <path/filename>";

	/**
	 * Constructs a new command object of type {@code RmCommand}.
	 */
	public RmCommand() {
		super("RM", createCommandDescription());
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
		desc.add("Removes a file or a directory.");
		desc.add("THE REMOVE OPERATION IS IRREVERSIBLE.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		Path path = Helper.resolveAbsolutePath(env, s);
		if (path == null) {
			writeln(env, "Invalid path!");
			return CommandStatus.CONTINUE;
		}

		if (!Files.exists(path)) {
			writeln(env, "The system cannot find the file specified.");
			return CommandStatus.CONTINUE;
		}
		
		// Remove directory
		if (Files.isDirectory(path)) {
			boolean confirmed = confirm(env, path);
			if (!confirmed) {
				writeln(env, "Cancelled.");
				return CommandStatus.CONTINUE;
			}
			
			RmFileVisitor rmVisitor = new RmFileVisitor(env);
			try {
				Files.walkFileTree(path, rmVisitor);
			} catch (IOException e) {
				writeln(env, e.getMessage());
			}
		// Remove file
		} else {
			try {
				Files.delete(path);
				writeln(env, "Deleted file " + path.getFileName());
			} catch (IOException e) {
				writeln(env, "Failed to delete file " + path.getFileName());
			}
		}
			
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Prompts the user if he wants to remove the specified directory
	 * <tt>dir</tt>. The user is expected to input <tt>Y</tt> as a <i>yes</i> or
	 * <tt>N</tt> as a <i>no</i>. Returns true if the user answered yes, false
	 * if no.
	 * 
	 * @param env an environment
	 * @param dir directory whose removal is to be confirmed
	 * @return true if the user answered yes, false if no
	 */
	private boolean confirm(Environment env, Path dir) {
		write(env, "Remove directory " + dir + "? (Y/N) ");
		while (true) {
			String line = readLine(env);
			
			if (line.equalsIgnoreCase("Y")) {
				return true;
			} else if (line.equalsIgnoreCase("N")) {
				return false;
			} else {
				write(env, "Please answer Y / N: ");
				continue;
			}
		}
	}

	/**
	 * A {@linkplain SimpleFileVisitor} extended and used to serve the {@linkplain
	 * RmCommand}. This file visitor is mostly used to remove non-empty directories.
	 *
	 * @author Mario Bobic
	 */
	private static class RmFileVisitor extends SimpleFileVisitor<Path> {
		
		/** An environment. */
		private Environment environment;

		/**
		 * Initializes a new instance of this class setting only an environment used
		 * only for writing out messages.
		 * 
		 * @param environment an environment
		 */
		public RmFileVisitor(Environment environment) {
			this.environment = environment;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			try {
				Files.delete(file);
				writeln(environment, "Deleted file " + file.getFileName());
			} catch (IOException e) {
				writeln(environment, "Failed to delete file " + file.getFileName());
			}

			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			System.out.println("Failed to access " + file);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			try {
				Files.delete(dir);
				writeln(environment, "Removed directory " + dir.getFileName());
			} catch (IOException e) {
				writeln(environment, "Failed to remove directory " + dir.getFileName());
			}

			return FileVisitResult.CONTINUE;
		}
	}

}
