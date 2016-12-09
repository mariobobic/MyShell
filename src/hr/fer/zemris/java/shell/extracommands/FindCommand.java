package hr.fer.zemris.java.shell.extracommands;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A command that is used for filtering file names or contents of a file and
 * writing out the absolute path of files whose file names or content match the
 * given pattern interpreted as a regular expression. The search begins in the
 * directory provided, or current working directory if no arguments are provided
 * and goes through all of the subdirectories.
 *
 * @author Mario Bobic
 */
public class FindCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "find (-r) <path> <pattern>";
	
	/** Size limit of files that this command will search through. */
	private static final long SIZE_LIMIT = 5*1024*1024;
	
	/** Indicates if regex matching should be used. */
	private boolean useRegex;
	
	/**
	 * Constructs a new command object of type {@code FindCommand}.
	 */
	public FindCommand() {
		super("FIND", createCommandDescription());
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
		desc.add("Searches for the pattern provided as argument, looking in file names and their content.");
		desc.add("Displays the absolute path of files whose file names or content match the given pattern.");
		desc.add("Pattern can be interpreted as a regular expression if the -r flag is provided.");
		desc.add("In need to include spaces to the pattern, use double quotation marks on the argument.");
		desc.add("Files that are exceeding the size limit will be ignored.");
		desc.add("The specified path may also be a file in which the pattern is searched for.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		/* Possible 1 to 3 arguments, where the third is a regex
		 * with possible spaces and quotation marks. */
		String[] args = Helper.extractArguments(s, 3);
		
		/* Set path and filter pattern, and useRegex if any. */
		Path path;
		String filter;
		useRegex = false;
		
		if (args.length == 1) {
			path = env.getCurrentPath();
			filter = args[0];
		} else if (args.length == 2) {
			path = Helper.resolveAbsolutePath(env, args[0]);
			filter = args[1];
		} else if (args.length == 3) {
			if ("-r".equals(args[0])) useRegex = true;
			path = Helper.resolveAbsolutePath(env, args[1]);
			filter = args[2];
		} else {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		/* Compile the pattern. */
		MyPattern myPattern;
		try {
			if (useRegex) {
				myPattern = new MyPattern(Pattern.compile(filter, Pattern.CASE_INSENSITIVE));
			} else {
				myPattern = new MyPattern(filter);
			}
		} catch (PatternSyntaxException e) {
			writeln(env, "Pattern error occurred:");
			writeln(env, e.getMessage());
			return CommandStatus.CONTINUE;
		}
		
		/* Clear previously marked files if this machine is a host. */
		if (env.isConnected()) {
			env.getConnection().clearDownloadMarks();
		}
		
		/* If path is a file, find matching lines inside a file. */
		if (Files.isRegularFile(path)) {
			printMatches(env, myPattern, path);
			return CommandStatus.CONTINUE;
		}

		FindFileVisitor filterVisitor = new FindFileVisitor(env, myPattern);
		Files.walkFileTree(path, filterVisitor);

		return CommandStatus.CONTINUE;
	}
	
	/**
	 * This method writes to the Environment <tt>env</tt> only in case the
	 * pattern matching with the specified <tt>pattern</tt> is satisfied.
	 * <p>
	 * Pattern matching is executed upon the specified <tt>file</tt>, and
	 * searches for the following:
	 * <ol>
	 * <li>If contents of the specified file match the given pattern, the file
	 * name and matched contents are printed out along with the line number of
	 * the matched content.
	 * <li>If there are no contents of the file that match the given pattern,
	 * but the file name matches, only the file name is printed out.
	 * <li>If neither contents of the file and file name match the given
	 * pattern, nothing is printed out.
	 * </ol>
	 * 
	 * @param env an environment
	 * @param myPattern pattern to be matched against
	 * @param file file upon which pattern matching is executed
	 * @throws IOException if an I/O exception occurs
	 */
	private static void printMatches(Environment env, MyPattern myPattern, Path file) throws IOException {
		if (!Files.isReadable(file)) {
			writeln(env, "Failed to access " + file);
			return;
		}
		
		BufferedReader br = new BufferedReader(
			new InputStreamReader(
				new BufferedInputStream(
					Files.newInputStream(file)), StandardCharsets.UTF_8));
		
		StringBuilder sb = new StringBuilder();
		int counter = 0;
		String line;
		while ((line = br.readLine()) != null) {
			counter++;
			if (myPattern.matches(line)) {
				sb	.append("   ")
					.append(counter)
					.append(": ")
					.append(line)
					.append("\n");
			}
		}
		
		boolean nameMatches = myPattern.matches(file.getFileName().toString());
		if (sb.length() != 0 || nameMatches) {
			write(env, file.toString());
			markForDownloadAndPrintNumber(env, file);
			writeln(env, "");
			writeln(env, sb.toString());
		}
	}
	
	/**
	 * Represents a pattern that can be given either as a {@code String} or a
	 * {@code Pattern}. If a string is given, it is decompiled to pattern parts
	 * using the {@link Helper#splitPattern(String)} method. Else the pattern is
	 * already a compiled representation of a regular expression.
	 * <p>
	 * This class contains a {@link #matches(String)} method that matches the
	 * specified input string to the argument given in the constructor.
	 *
	 * @author Mario Bobic
	 */
	private static class MyPattern {
		
		/** Parts of the pattern to be matched against. */
		private String[] patternParts;
		
		/** Regular expression pattern to be matched against. */
		private Pattern regexPattern;
		
		/**
		 * Constructs an instance of {@code MyPattern} with the specified string
		 * pattern.
		 *
		 * @param pattern a string pattern possibly containing asterisks
		 */
		public MyPattern(String pattern) {
			patternParts = Helper.splitPattern(pattern.toUpperCase());
		}
		
		/**
		 * Constructs an instance of {@code MyPattern} with the specified
		 * regular expression pattern.
		 *
		 * @param regex a compiled representation of a regular expression
		 */
		public MyPattern(Pattern regex) {
			regexPattern = regex;
		}
		
		/**
		 * Returns true if the specified <tt>input</tt> matches this pattern.
		 * 
		 * @param input input to be matched against
		 * @return true if the specified input matches this pattern
		 */
		public boolean matches(String input) {
			if (patternParts != null) {
				return Helper.matches(input.toUpperCase(), patternParts);
			} else {
				return regexPattern.matcher(input).matches();
			}
		}
		
	}
	
	/**
	 * A {@linkplain SimpleFileVisitor} extended and used to serve the
	 * {@linkplain FindCommand}. Only the {@code visitFile} and
	 * {@code visitFileFailed} methods are overridden. Pattern matching is case
	 * insensitive.
	 *
	 * @author Mario Bobic
	 */
	private static class FindFileVisitor extends SimpleFileVisitor<Path> {
		
		/** An environment. */
		private Environment environment;
		/** The wanted pattern to be filtered out. */
		private MyPattern pattern;
		
		/** Size limit converted to human readable byte count. */
		private String limitStr = Helper.humanReadableByteCount(SIZE_LIMIT);
		/** Files that were too big to process. */
		private List<Path> bigFiles = new ArrayList<>();
		/** Indicates if big files should be printed out. */
		private boolean printBigFiles = false;

		/**
		 * Initializes a new instance of this class setting the desired pattern
		 * and an environment used only for writing out messages.
		 * 
		 * @param environment an environment
		 * @param pattern the wanted pattern to be filtered out
		 */
		public FindFileVisitor(Environment environment, MyPattern pattern) {
			this.environment = environment;
			this.pattern = pattern;
		}

		/**
		 * Checks if the file name or content match the given
		 * {@link FindFileVisitor#pattern pattern} and writes it out if it does.
		 */
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (Files.size(file) <= SIZE_LIMIT) {
				try {
					printMatches(environment, pattern, file);
				} catch (IOException e) {
					visitFileFailed(file, e);
				}
			} else {
				bigFiles.add(file);
			}
			
			return super.visitFile(file, attrs);
		}

		/**
		 * Continues searching for the filtering pattern, even though a certain file
		 * failed to be visited.
		 */
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			writeln(environment, "Failed to access " + file);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (printBigFiles && !bigFiles.isEmpty()) {
				Path relativeDir = environment.getCurrentPath().relativize(dir);
				writeln(environment, String.format(
					"Files in %s that were too big to process (Exceeded %s):",
					relativeDir, limitStr
				));
				
				for (Path file : bigFiles) {
					Path relativeFile = environment.getCurrentPath().relativize(file);
					writeln(environment, String.format(
						"   %s (%s)",
						relativeFile,
						Helper.humanReadableByteCount(Files.size(file))
					));
				}
				
				bigFiles.clear();
			}
			
			return super.postVisitDirectory(dir, exc);
		}
	}

}
