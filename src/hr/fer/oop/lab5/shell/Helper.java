package hr.fer.oop.lab5.shell;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A helper class. Used for defining and providing helper methods.
 *
 * @author Mario Bobic
 */
public class Helper {
	
	/**
	 * Resolves the given path by checking if it's relative or absolute. If the
	 * {@code str} path parameter is an absolute path then this method trivially
	 * returns the given path. In the simplest case, the given path does not
	 * have a root component, in which case this method joins the given path
	 * with the root and returns the absolute path. If the given path has
	 * invalid characters {@code null} value is returned.
	 * 
	 * @param env an environment
	 * @param str the given path string
	 * @return the absolute path of the given path
	 */
	public static Path resolveAbsolutePath(Environment env, String str) {
		Path path;
		try {
			path = Paths.get(str);
		} catch (InvalidPathException e) {
			return null;
		}
		
		Path newPath;
		if (path.isAbsolute()) {
			newPath = path;
		} else {
			newPath = env.getCurrentPath().resolve(path).normalize();
		}
		return newPath;
	}
	
}
