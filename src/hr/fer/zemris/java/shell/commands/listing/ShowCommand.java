package hr.fer.zemris.java.shell.commands.listing;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

/**
 * Walks the directory tree from the given path, or current path if no path is
 * entered. Prints out the given maximum quantity of files, or less if that many
 * files are not found, to the current environment. If no quantity is given, the
 * quantity is set to 10 by default.
 * <p>
 * Currently supported arguments for file querying are <strong>largest</strong>,
 * <strong>smallest</strong>, <strong>newest</strong>, <strong>oldest</strong>.
 *
 * @author Mario Bobic
 */
public class ShowCommand extends VisitorCommand {

	/** Default amount of files to be shown. */
	private static final int DEFAULT_QUANTITY = 10;

	/** The standard date-time formatter. */
	private static final DateTimeFormatter FORMATTER =
			DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.systemDefault());

	/** A comparator that compares files by their size, smallest first. */
	private static final Comparator<Path> COMP_SMALLEST = (f1, f2) -> {
		return Long.compare(size(f1), size(f2));
	};
	
	/** A comparator that compares files by their modification date, oldest first. */
	private static final Comparator<Path> COMP_OLDEST = (f1, f2) -> {
		return lastModified(f1).compareTo(lastModified(f2));
	};
	
	/* Flags */
	/** Amount of files to be shown. */
	private int count;
	
	/**
	 * Constructs a new command object of type {@code LargestCommand}.
	 */
	public ShowCommand() {
		super("SHOW", createCommandDescription(), createFlagDescriptions());
		commandArguments.addFlagDefinition("n", "count", true);
	}
	
	@Override
	protected String getCommandSyntax() {
		return "<largest|smallest|newest|oldest> (<path>)";
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
		desc.add("Displays files ordered by a specified attribute in the directory tree.");
		desc.add("There are four supported arguments for file querying: largest, smallest, newest and oldest.");
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
		desc.add(new FlagDescription("n", "count", "count", "Amount of files to be shown."));
		return desc;
	}
	
	@Override
	protected String compileFlags(Environment env, String s) {
		/* Initialize default values. */
		count = DEFAULT_QUANTITY;

		/* Compile! */
		s = commandArguments.compile(s);
		
		/* Replace default values with flag values, if any. */
		if (commandArguments.containsFlag("n", "count")) {
			count = commandArguments.getFlag("n", "count").getPositiveIntArgument(false);
		}

		return super.compileFlags(env, s);
	}
	
	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			throw new SyntaxException();
		}
		
		String[] args = Helper.extractArguments(s, 2);
		
		/* Resolve path from the second argument, if present. */
		Path dir;
		if (args.length == 1) {
			dir = env.getCurrentPath();
		} else {
			dir = Helper.resolveAbsolutePath(env, args[1]);
		}

		Comparator<Path> comparator = getComparator(args[0]);
		if (comparator == null) {
			writeln(env, "Unknown argument: " + args[0]);
			return CommandStatus.CONTINUE;
		}
		
		/* Make necessary checks. */
		Helper.requireDirectory(dir);
		
		ShowFileVisitor largestVisitor = new ShowFileVisitor(comparator);
		walkFileTree(dir, largestVisitor);

		/* Clear previously marked paths. */
		env.clearMarks();
		
		List<Path> largestFiles = largestVisitor.getFiles();
		for (Path f : largestFiles) {
			String bytes = " (" + Helper.humanReadableByteCount(size(f)) + ")";
			String modTime = " (" + FORMATTER.format(lastModified(f).toInstant()) + ")";
			write(env, f.normalize() + bytes + modTime);
			markAndPrintNumber(env, f);
		}
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Returns a comparator that matches the specified attribute <tt>attr</tt>.
	 * 
	 * @param attr comparison attribute
	 * @return a comparator that matches the specified attribute
	 */
	private static Comparator<Path> getComparator(String attr) {
		Comparator<Path> comparator = null;
		
		if ("largest".equalsIgnoreCase(attr)) {
			comparator = COMP_SMALLEST.reversed();
		} else if ("smallest".equalsIgnoreCase(attr)) {
			comparator = COMP_SMALLEST;
		} else if ("newest".equalsIgnoreCase(attr)) {
			comparator = COMP_OLDEST.reversed();
		} else if ("oldest".equalsIgnoreCase(attr)) {
			comparator = COMP_OLDEST;
		}
		
		return comparator;
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
			return -1; // ??
		}
	}
	
	/**
	 * Returns a file's last modified time. If an I/O exception occurs while
	 * trying to obtain the file's last modified time,
	 * <tt>FileTime.fromMillis(0)</tt> is returned.
	 *
	 * @param file the path to the file
	 * @return a {@code FileTime} representing the time the file was last
	 *         modified, or an implementation specific default when a time stamp
	 *         to indicate the time of last modification is not supported by the
	 *         file system
	 */
	private static FileTime lastModified(Path file) {
		try {
			return Files.getLastModifiedTime(file);
		} catch (IOException e) {
			return FileTime.fromMillis(0); // ??
		}
	}

	/**
	 * A {@linkplain SimpleFileVisitor} extended and used to serve the
	 * {@linkplain ShowCommand}.
	 *
	 * @author Mario Bobic
	 */
	private class ShowFileVisitor extends SimpleFileVisitor<Path> {

		/** Comparator used to compare files. */
		private Comparator<Path> comparator;
		
		/** List of largest files in the given directory tree. */
		private List<Path> filteredFiles;
		
		/**
		 * Initializes a new instance of this class setting the quantity to the
		 * desired value.
		 * 
		 * @param comparator comparator used for comparing files
		 */
		public ShowFileVisitor(Comparator<Path> comparator) {
			this.comparator = comparator;
			filteredFiles = new ArrayList<>();
		}
		
		/**
		 * Adds the file to the list of candidates.
		 */
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			addCandidate(file);
			return FileVisitResult.CONTINUE;
		}
		
		/**
		 * Continues searching for files.
		 */
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//			writeln(environment, "Failed to access " + file);
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
		private synchronized void addCandidate(Path file) {
			boolean added = false;
			
			if (filteredFiles.size() < count) {
				filteredFiles.add(file);
				added = true;
			} else {
				int lastIndex = count-1;
				Path lastFile = filteredFiles.get(lastIndex);
				if (comparator.compare(lastFile, file) > 0) {
					filteredFiles.remove(lastIndex);
					filteredFiles.add(file);
					added = true;
				}
			}
			
			// Only sort if a file was added
			if (added) {
				Collections.sort(filteredFiles, comparator);
			}
		}
		
		/**
		 * Returns the list of largest files in the directory tree.
		 * 
		 * @return the list of largest files in the directory tree
		 */
		public List<Path> getFiles() {
			return filteredFiles;
		}
	}

}
