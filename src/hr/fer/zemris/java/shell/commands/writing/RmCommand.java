package hr.fer.zemris.java.shell.commands.writing;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * A command that is used for removing files and non-empty directories.
 *
 * @author Mario Bobic
 */
public class RmCommand extends VisitorCommand {

	/**
	 * Constructs a new command object of type {@code RmCommand}.
	 */
	public RmCommand() {
		super("RM", createCommandDescription());
	}

	@Override
	protected String getCommandSyntax() {
		return "<path>";
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
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			throw new SyntaxException();
		}
		
		Path path = Helper.resolveAbsolutePath(env, s);
		Helper.requireExists(path);
		
		// Require confirmation for directories
		if (Files.isDirectory(path)) {
			boolean confirmed = promptConfirm(env, "Remove directory " + path + "?");
			if (!confirmed) {
				writeln(env, "Cancelled.");
				return CommandStatus.CONTINUE;
			}
		}
		
		RmFileVisitor rmVisitor = new RmFileVisitor(env, path);
		walkFileTree(path, rmVisitor);
			
		return CommandStatus.CONTINUE;
	}

	/**
	 * A {@linkplain SimpleFileVisitor} extended and used to serve the {@linkplain
	 * RmCommand}. This file visitor is mostly used to remove non-empty directories.
	 *
	 * @author Mario Bobic
	 */
	private class RmFileVisitor extends SimpleFileVisitor<Path> {
		
		/** An environment. */
		private Environment environment;
		/** Starting path. */
		private Path root;

		/**
		 * Constructs an instance of {@code RmFileVisitor} with the specified arguments.
		 * 
		 * @param environment an environment
		 * @param root starting directory of the tree walker
		 */
		public RmFileVisitor(Environment environment, Path root) {
			this.environment = environment;
			this.root = root.getParent();
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Path relative = root.relativize(file);
			try {
				Files.delete(file);
				writeln(environment, "Deleted file " + relative);
			} catch (IOException e) {
				writeln(environment, "Failed to delete file " + relative);
			}

			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			writeln(environment, "Failed to access " + file);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			Path relative = root.relativize(dir);
			
			try {
				Files.delete(dir);
				writeln(environment, "Removed directory " + relative);
			} catch (IOException e) {
				writeln(environment, "Failed to remove directory " + relative);
			}

			return FileVisitResult.CONTINUE;
		}
	}

}
