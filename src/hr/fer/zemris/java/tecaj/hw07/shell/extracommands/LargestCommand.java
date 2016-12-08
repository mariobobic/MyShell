package hr.fer.zemris.java.tecaj.hw07.shell.extracommands;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import hr.fer.zemris.java.tecaj.hw07.shell.CommandStatus;
import hr.fer.zemris.java.tecaj.hw07.shell.Environment;
import hr.fer.zemris.java.tecaj.hw07.shell.Helper;
import hr.fer.zemris.java.tecaj.hw07.shell.commands.AbstractCommand;

/**
 * Walks the directory tree from the given path, or current path if no path is
 * entered. Prints out the given maximum quantity of largest files (or less, if
 * that many files are not found) to the current environment. If no quantity is
 * given, the quantity is set to 10 by default.
 *
 * @author Mario Bobic
 */
public class LargestCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command */
	private static final String SYNTAX = "largest <path> (optional: <quantity>)";

	/** Default amount of top largest files */
	private static final int DEF_QUANTITY = 10;
	
	/**
	 * Constructs a new command object of type {@code LargestCommand}.
	 */
	public LargestCommand() {
		super("LARGEST", createCommandDescription());
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
		desc.add("Displays the specified quantity of largest files in the specified directory tree.");
		desc.add("The expected syntax: " + SYNTAX);
		return desc;
	}
	
	@Override
	public CommandStatus execute(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		int quantity;
		
		String[] args = Helper.extractArguments(s);
		
		/* Resolve path from the first argument. */
		Path dir = Helper.resolvePath(args[0]);
		if (dir == null) {
			writeln(env, "Invalid path!");
			return CommandStatus.CONTINUE;
		}
		
		/* Resolve quantity from the second argument, if present. */
		if (args.length == 1) {
			quantity = DEF_QUANTITY;
		} else {
			try {
				quantity = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				printSyntaxError(env, SYNTAX);
				return CommandStatus.CONTINUE;
			}
		}
		
		/* Make necessary checks. */
		if (!Files.isDirectory(dir)) {
			writeln(env, "The specified path must be a directory.");
			return CommandStatus.CONTINUE;
		}
		
		LargestFileVisitor largestVisitor = new LargestFileVisitor(quantity);
		try {
			Files.walkFileTree(dir, largestVisitor);
		} catch (IOException e) {
			writeln(env, e.getMessage());
		}
		
		List<Path> largestFiles = largestVisitor.getLargest();
		for (Path f : largestFiles) {
			writeln(env, f.normalize() + " (" + Helper.humanReadableByteCount(size(f)) + ")");
		}
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Returns the size of a file (in bytes). The size may differ from the
	 * actual size on the file system due to compression, support for sparse
	 * files, or other reasons. The size of files that are not
	 * {@link Files#isRegularFile regular} files is implementation specific and
	 * therefore unspecified.
	 *
	 * @param file the path to the file
	 * @return the file size, in bytes
	 */
	private static long size(Path file) {
		try {
			return Files.size(file);
		} catch (IOException e) {
			return 0; // ??
		}
	}

	/**
	 * A {@linkplain SimpleFileVisitor} extended and used to serve the
	 * {@linkplain LargestCommand}. Only the <tt>visitFile</tt> method is
	 * overridden.
	 *
	 * @author Mario Bobic
	 */
	private static class LargestFileVisitor extends SimpleFileVisitor<Path> {

		/** Number of largest files to be printed out */
		private int quantity;
		/** List of largest files in the given directory tree */
		private List<Path> largestFiles;

		/** A comparator that compares files by their size, largest first */
		private static final Comparator<Path> BY_SIZE = (f1, f2) -> {
			return -Long.compare(size(f1), size(f2));
		};
		
		/**
		 * Initializes a new instance of this class setting the quantity to the
		 * desired value.
		 * 
		 * @param quantity number of largest files to be printed out
		 */
		public LargestFileVisitor(int quantity) {
			this.quantity = quantity;
			largestFiles = new ArrayList<>();
		}
		
		/**
		 * Adds the file to the list of candidates for largest files.
		 */
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			addCandidate(file);
			return FileVisitResult.CONTINUE;
		}
		
		/**
		 * Continues searching for largest files.
		 */
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
		
		/**
		 * Adds files unconditionally if the list has not yet reached the
		 * desired maximum quantity. If the list has been filled, the smallest
		 * file is checked with param <tt>file</tt> and the largest of these two
		 * will be left in the list.
		 * 
		 * @param file candidate file
		 */
		private void addCandidate(Path file) {
			boolean added = false;
			
			if (largestFiles.size() < quantity) {
				largestFiles.add(file);
				added = true;
			} else {
				int lastIndex = quantity-1;
				Path lastFile = largestFiles.get(lastIndex);
				if (size(lastFile) < size(file)) {
					largestFiles.remove(lastIndex);
					largestFiles.add(file);
					added = true;
				}
			}
			
			// Only sort if a file was added
			if (added) {
				Collections.sort(largestFiles, BY_SIZE);
			}
		}
		
		/**
		 * Returns the list of largest files in the directory tree.
		 * 
		 * @return the list of largest files in the directory tree
		 */
		public List<Path> getLargest() {
			return largestFiles;
		}
	}

}
