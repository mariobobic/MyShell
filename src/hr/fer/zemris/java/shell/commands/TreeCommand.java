package hr.fer.zemris.java.shell.commands;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * Walks the directory tree from the specified path and prints a whole file tree
 * where each directory level shifts output two characters to the right.
 *
 * @author Mario Bobic
 */
public class TreeCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "tree (optional: <path>)";
	
	/**
	 * Constructs a new command object of type {@code TreeCommand}.
	 */
	public TreeCommand() {
		super("TREE", createCommandDescription());
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
		desc.add("Walks the directory tree from the specified path.");
		desc.add("Prints a whole file tree where each directory level "
				+ "shifts output two characters to the right.");
		desc.add("Use tree <path> to walk file tree of a certain directory.");
		desc.add("Use tree to walk file tree of the current directory.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}
	
	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		Path path = s == null ?
			env.getCurrentPath() : Helper.resolveAbsolutePath(env, s);
		
		if (!Files.isDirectory(path)) {
			writeln(env, "The specified path must be a directory.");
			return CommandStatus.CONTINUE;
		}
		
		/* Passed all checks, start working. */
		Files.walkFileTree(path, new TreeFileVisitor(env));
		
		return CommandStatus.CONTINUE;
	}

	/**
	 * A {@linkplain FileVisitor} implementation that is used to serve the
	 * {@linkplain TreeCommand}. This method prints out the directory tree.
	 *
	 * @author Mario Bobic
	 */
	private static class TreeFileVisitor implements FileVisitor<Path> {
		
		/** The level in relation to root that is currently being visited. */
		private int level;
		
		/** An environment. */
		private Environment environment;
		
		/**
		 * Constructs an instance of TreeFileVisitor with the specified
		 * <tt>environment</tt>.
		 * 
		 * @param environment an environment
		 */
		public TreeFileVisitor(Environment environment) {
			this.environment = environment;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			print(dir);
			level++;
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			print(file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			level--;
			return FileVisitResult.CONTINUE;
		}
		
		/**
		 * Prints out the specified <tt>path</tt> to the environment, with
		 * leading spaces based on the current <tt>level</tt>.
		 * 
		 * @param path path to be written out
		 */
		private void print(Path path) {
			if (level == 0) {
				writeln(environment, path.normalize().toAbsolutePath().toString());
			} else {
				printSpaces(level);
				writeln(environment, path.getFileName().toString());
			}
		}
		
		/**
		 * Prints the <tt>amount</tt> of spaces onto the environment in a single
		 * line.
		 * 
		 * @param amount the amount of spaces to be written out
		 */
		private void printSpaces(int amount) {
			write(environment, String.format("%" + (2*amount) + "s", ""));
		}
		
	}

}
