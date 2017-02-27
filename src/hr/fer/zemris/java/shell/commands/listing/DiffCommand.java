package hr.fer.zemris.java.shell.commands.listing;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * Analyzes line by line and displays a list of changes between two files. The
 * command requires 2 arguments - path to first and second file or directory.
 * <p>
 * Third, optional argument is a charset. If the charset argument isn't
 * specified, UTF-8 is used as default.
 * <p>
 * If a fourth argument is specified, it is expected to be 'ALL'. The ALL
 * argument means that the whole document will be printed, with differences
 * highlighted.
 * <p>
 * Paths must be either both files or both directories. Printing differences of
 * directories works on files with same name and same relative position with
 * respect to the specified 'root' directory. If a file exists in subdirectory
 * of path1, but not in subdirectory of path2, it is ignored.
 *
 * @author Mario Bobic
 */
public class DiffCommand extends VisitorCommand {

	/* Flags */
	/** Charset for decoding files. */
	private Charset charset;
	/** Indicates if the whole document must be printed. */
	private boolean all;
	/** Indicates if files that do not exists in second path should be printed. */
	private boolean noExist;

	/**
	 * Constructs a new command object of type {@code DiffCommand}.
	 */
	public DiffCommand() {
		super("DIFF", createCommandDescription(), createFlagDescriptions());
		commandArguments.addFlagDefinition("x", false);
		commandArguments.addFlagDefinition("a", "all", false);
		commandArguments.addFlagDefinition("c", "charset", true);
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
		desc.add("Analyzes files line by line and displays a list of changes between them.");
		desc.add("The command requires 2 arguments - path to first and second file or directory.");
		desc.add("Default charset is UTF-8.");
		desc.add("Paths must be either both files or both directories.");
		desc.add("Printing differences of directories works on files with same name and relative "
				+ "position with respect to the specified 'root' directory and same name.");
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
		desc.add(new FlagDescription("x", null, null, "Print file names that exist only in the first directory tree."));
		desc.add(new FlagDescription("a", "all", null, "Print whole document with differences highlighted."));
		desc.add(new FlagDescription("c", "charset", "charset", "Specify the charset to be used."));
		return desc;
	}
	
	@Override
	protected String compileFlags(Environment env, String s) {
		/* Initialize default values. */
		charset = StandardCharsets.UTF_8;
		all = false;
		noExist = false;

		/* Compile! */
		s = commandArguments.compile(s);

		/* Replace default values with flag values, if any. */
		if (commandArguments.containsFlag("c", "charset")) {
			charset = resolveCharset(commandArguments.getFlag("c", "charset").getArgument());
		}
		
		if (commandArguments.containsFlag("a", "all")) {
			all = true;
		}
		
		if (commandArguments.containsFlag("x")) {
			noExist = true;
		}
		
		return super.compileFlags(env, s);
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		/* Extract arguments and check the array length. */
		String[] args = Helper.extractArguments(s);
		if (args.length != 2) {
			throw new SyntaxException();
		}
		
		/* Resolve paths. */
		Path path1 = Helper.resolveAbsolutePath(env, args[0]);
		Path path2 = Helper.resolveAbsolutePath(env, args[1]);
		if (!Files.exists(path1)) {
			writeln(env, "The system cannot find the path specified: " + path1);
			return CommandStatus.CONTINUE;
		}
		if (!Files.exists(path2)) {
			writeln(env, "The system cannot find the path specified: " + path2);
			return CommandStatus.CONTINUE;
		}
		
		/* If the flag procedure didn't give a valid charset, scream! */
		if (charset == null) {
			String arg = commandArguments.getFlag("c", "charset").getArgument();
			writeln(env, "Invalid charset: " + arg);
			writeln(env, "Check available charsets with charsets command.");
			return CommandStatus.CONTINUE;
		}
		
		/* Both paths must be of same type. */
		boolean path1IsFile = Files.isRegularFile(path1);
		boolean path2IsFile = Files.isRegularFile(path2);
		if (path1IsFile != path2IsFile) {
			writeln(env, "Can not match file with directory.");
			return CommandStatus.CONTINUE;
		}

		/* Passed all checks, start working. */
		path1 = path1.toRealPath();
		path2 = path2.toRealPath();
		Map<Path, Path> fails = new LinkedHashMap<>();

		if (path1IsFile) {
			boolean success = processFiles(env, path1, path2, charset, all);
			if (!success) fails.put(path1, path2);
		} else {
			DiffFileVisitor diffVisitor = new DiffFileVisitor(env, path1, path2);
			Files.walkFileTree(path1, diffVisitor);
			fails = diffVisitor.getFails();
		}
		
		/* If there are decoding fails, print it. */
		if (fails.size() != 0) {
			formatln(env,
				"Could not decode %d files using %s encoding. Try a different encoding.",
				fails.size(), charset
			);
		}

		/* Print errors if not silent. */
		if (!silent && fails.size() != 0) {
			writeln(env, "Files that failed to be decoded:");
			fails.forEach((file1, file2) -> {
				writeln(env, file1 + " against " + file2);
			});
		}
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Returns a charset object for the named charset, or <tt>null</tt> if a
	 * charset with the specified <tt>name<tt> can not be resolved.
	 * 
	 * @param name name of the requested charset; may be either a canonical name
	 *        or an alias
	 * @return a charset object for the named charset
	 */
	private static Charset resolveCharset(String name) {
		try {
			return Charset.forName(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	/**
	 * Processes the two files <tt>file1</tt> and <tt>file2</tt> and writes out
	 * its differences, with line numbers included in front of the lines that
	 * differ. If the boolean <tt>all</tt> is <tt>true</tt>, both files are
	 * written out until the end.
	 * <p>
	 * This method returns a boolean value that indicates if the processing was
	 * successful. The processing may not succeed if one of the files does not
	 * exist, is not accessible or can not be decoded using the specified
	 * <tt>charset</tt>.
	 * 
	 * @param env an environment
	 * @param file1 first file to be processed
	 * @param file2 second file to be processed
	 * @param charset charset for decoding files
	 * @param all indicates that the whole document should be printed
	 * @return true if processing succeeds, false otherwise.
	 */
	private static boolean processFiles(Environment env, Path file1, Path file2, Charset charset, boolean all) {
		try {
			Iterator<String> iter1 = Files.lines(file1, charset).iterator();
			Iterator<String> iter2 = Files.lines(file2, charset).iterator();
			
			int counter = 0;
			int numDifferences = 0;
			while (true) {
				counter++;
				
				/* Setup */
				String line1 = null; String line2 = null;
				if (iter1.hasNext()) line1 = iter1.next();
				if (iter2.hasNext()) line2 = iter2.next();

				/* Break conditions */
				if (line1 == null && line2 == null) break;
				if (!all && (line1 == null || line2 == null)) break;
				
				String differences = getDifferences(line1, line2, counter, all);
				if (differences == null) continue;
				
				numDifferences++;
				if (numDifferences == 1) {
					writeln(env, "Analyzing files " + file1 + " and " + file2);
				}
				writeln(env, differences);
			}
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Returns differences of the two specified lines <tt>line1</tt> and
	 * <tt>line2</tt>, with line number included in front. If the boolean
	 * <tt>all</tt> is <tt>true</tt> and if the specified lines do not differ,
	 * or more formally if <tt>line1.equals(line2)</tt>, one of the lines is
	 * returned.
	 * 
	 * @param line1 first line for comparison
	 * @param line2 second line for comparison
	 * @param lineNum line number
	 * @param all indicates if the line should be returned even if there are no
	 *        differences between the two lines
	 * @return differences between the two lines
	 */
	private static String getDifferences(String line1, String line2, int lineNum, boolean all) {
		if (line1 == null) line1 = "";
		if (line2 == null) line2 = "";
		
		String differences = null;
		if (!Objects.equals(line1, line2)) {
			differences = "  " + lineNum + ": " + line1 + " --> " + line2;
		} else if (all) {
			differences = "  " + lineNum + ": " + line1;
		}
		
		return differences;
	}
	
	/**
	 * A {@linkplain SimpleFileVisitor} extended and used to serve the
	 * {@linkplain DiffCommand}. This file visitor is used to write differences
	 * between files with the same relative parent and same file name.
	 *
	 * @author Mario Bobic
	 */
	private class DiffFileVisitor extends SimpleFileVisitor<Path> {
		
		/** An environment. */
		private Environment environment;
		
		/** This path's root directory. */
		private Path root;
		/** Other path's root directory. */
		private Path otherRoot;
		
		/** Map of files that failed to be processed. */
		private Map<Path, Path> fails;

		/**
		 * Initializes a new instance of this class setting only an environment used
		 * only for writing out messages.
		 * 
		 * @param environment an environment
		 * @param root root directory of this file visitor
		 * @param otherRoot other path's root directory
		 */
		public DiffFileVisitor(Environment environment, Path root, Path otherRoot) {
			this.environment = environment;
			this.root = root;
			this.otherRoot = otherRoot;
			this.fails = new LinkedHashMap<>();
		}
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (isExcluded(dir)) {
				return FileVisitResult.SKIP_SUBTREE;
			}

			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (isExcluded(file)) {
				return FileVisitResult.CONTINUE;
			}
			
			Path relative = root.relativize(file);
			Path otherFile = otherRoot.resolve(relative);
			
			if (Files.exists(otherFile)) {
				boolean success = processFiles(environment, file, otherFile, charset, all);
				if (!success) {
					fails.put(file, otherFile);
				}
			} else if (noExist) {
				formatln(environment, "Exists in %s but not in %s: %s",
					root.getFileName(), otherRoot.getFileName(), relative);
			}

			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			writeln(environment, "Failed to access " + file);
			return FileVisitResult.CONTINUE;
		}

		/**
		 * Returns a map of files that failed to be processed, where key is a
		 * path to file of the starting file tree and value is a path to file of
		 * the other file tree.
		 * 
		 * @return a map of files that failed to be processed
		 */
		public Map<Path, Path> getFails() {
			return fails;
		}
	}

}
