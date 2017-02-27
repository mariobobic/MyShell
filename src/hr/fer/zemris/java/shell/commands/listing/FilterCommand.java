package hr.fer.zemris.java.shell.commands.listing;

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
 * A command that is used for filtering and writing out the absolute path of
 * files that match the given pattern. This search begins in the current working
 * directory and is going through all of the subdirectories.
 *
 * @author Mario Bobic
 */
public class FilterCommand extends VisitorCommand {
	
	/**
	 * Constructs a new command object of type {@code FilterCommand}.
	 */
	public FilterCommand() {
		super("FILTER", createCommandDescription());
	}

	@Override
	protected String getCommandSyntax() {
		return "<pattern>";
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
		desc.add("Searches the current directory and all its subdirectories.");
		desc.add("Displays the absolute path of files that match the given pattern.");
		desc.add("Pattern may be given with optional asterisk (*) symbols.");
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			throw new SyntaxException();
		}
		
		/* Clear previously marked paths. */
		env.clearMarks();
		
		FilterFileVisitor filterVisitor = new FilterFileVisitor(env, s);
		Path path = env.getCurrentPath();
		
		Files.walkFileTree(path, filterVisitor);
		int fails = filterVisitor.getFails();
		if (fails != 0) {
			writeln(env, "Failed to access " + fails + " paths.");
		}

		return CommandStatus.CONTINUE;
	}
	
	/**
	 * A {@linkplain SimpleFileVisitor} extended and used to serve the {@linkplain
	 * FilterCommand}. Only the {@code visitFile} method is overridden.
	 *
	 * @author Mario Bobic
	 */
	private class FilterFileVisitor extends SimpleFileVisitor<Path> {
		
		/** An environment. */
		private Environment environment;
		/** Parts of the pattern to be matched against. */
		private String[] patternParts;
		
		/** Number of files that failed to be accessed. */
		private int fails;

		/**
		 * Initializes a new instance of this class setting the desired pattern and
		 * an environment used only for writing out messages.
		 * 
		 * @param environment an environment
		 * @param pattern the wanted pattern to be filtered out
		 */
		public FilterFileVisitor(Environment environment, String pattern) {
			this.environment = environment;
			this.patternParts = Helper.splitPattern(pattern.trim().toUpperCase());
		}
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (isExcluded(dir)) {
				return FileVisitResult.SKIP_SUBTREE;
			}

			return FileVisitResult.CONTINUE;
		}

		/**
		 * Checks if the file matches the given {@link #patternParts} and writes
		 * it out if it does.
		 */
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (isExcluded(file)) {
				return FileVisitResult.CONTINUE;
			}
			
			String fileName = file.getFileName().toString().toUpperCase();
			
			if (Helper.matches(fileName, patternParts)) {
				markAndPrintPath(environment, file);
			}

			return FileVisitResult.CONTINUE;
		}
		
		/**
		 * Continues searching for the filtering pattern, even though a certain file
		 * failed to be visited.
		 */
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//			writeln(environment, "Failed to access " + file);
			fails++;
			return FileVisitResult.CONTINUE;
		}

		/**
		 * Returns the number of files that failed to be accessed.
		 * 
		 * @return the number of files that failed to be accessed
		 */
		public int getFails() {
			return fails;
		}
		
		/**
		 * Returns true if the given param {@code name} matches the
		 * {@code pattern}. The {@code pattern} can contain an asterisk
		 * character ("*") that represents 0 or more characters that should not
		 * be considered while matching.
		 * 
		 * @param name name that is being examined
		 * @param pattern a pattern that may contain the asterisk character
		 * @return true if {@code name} matches the {@code pattern}. False otherwise
		 * @deprecated this method can have only 2 parts of the pattern. Use
		 *             {@link Helper#matches(String, String)} instead
		 */
		@Deprecated
		@SuppressWarnings("unused")
		private boolean matches(String name, String pattern) {
			if (pattern.contains("*")) {
				int r = pattern.indexOf("*");
				String start = pattern.substring(0, r);
				String end = pattern.substring(r+1);
				if (name.startsWith(start) && name.endsWith(end))
					return true;
			} else if (name.equals(pattern)) {
				return true;
			}
			
			return false;
		}
	}

}
