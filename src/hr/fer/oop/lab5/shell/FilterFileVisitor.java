package hr.fer.oop.lab5.shell;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A {@linkplain SimpleFileVisitor} extended and used to serve the {@linkplain
 * FilterCommand}. Only the {@code visitFile} method is overriden.
 *
 * @author Mario Bobic
 */
public class FilterFileVisitor extends SimpleFileVisitor<Path> {
	
	/** An environment */
	private Environment environment;
	/** The wanted pattern to be filtered out */
	private String pattern;

	/**
	 * Initializes a new instance of this class setting the desired pattern and
	 * an environment used only for writing out messages.
	 * 
	 * @param environment an environment
	 * @param pattern the wanted pattern to be filtered out
	 */
	public FilterFileVisitor(Environment environment, String pattern) {
		super();
		this.environment = environment;
		this.pattern = pattern.trim().toUpperCase();
	}

	/**
	 * Checks if the file matches the given {@link FilterFileVisitor#pattern
	 * pattern} and writes it out if it does.
	 */
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		String fileName = file.toFile().getName().toUpperCase();
		
		if (matches(fileName, pattern)) {
			environment.writeln(file.toString());
		}
		
		return super.visitFile(file, attrs);
	}
	
	/**
	 * Continues searching for the filtering pattern, even though a certain file
	 * failed to be visited.
	 */
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		System.out.println("Failed to access " + file);
		return FileVisitResult.CONTINUE;
	}
	
	/**
	 * Returns true if the given param {@code name} matches the {@code pattern}.
	 * The {@code pattern} can contain an astrix character ("*") that represents
	 * 0 or more characters that should not be considered while matching.
	 * 
	 * @param name name that is being examined
	 * @param pattern a pattern that may contain the astrix character
	 * @return true if {@code name} matches the {@code pattern}. False otherwise
	 */
	private static boolean matches(String name, String pattern) {
		if (pattern.contains("*")) {
			/* Remove the astrix from the pattern */
			String sample = pattern.replace("*", "");
			
			if (pattern.startsWith("*")) {
				if (name.endsWith(sample))
					return true;
			} else if (pattern.endsWith("*")) {
				if (name.startsWith(sample))
					return true;
			} else {
				int r = pattern.indexOf("*");
				String start = pattern.substring(0, r);
				String end = pattern.substring(r+1);
				if (name.startsWith(start) && name.endsWith(end))
					return true;
			}
			
		} else if (name.equals(pattern)){
			return true;
		}
		
		return false;
	}
	
}
