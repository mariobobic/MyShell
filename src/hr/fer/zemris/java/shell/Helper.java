package hr.fer.zemris.java.shell;

import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A helper class. Used for defining and providing helper methods.
 *
 * @author Mario Bobic
 */
public class Helper {
	
	/** Keyword used for sending and detecting a download start. */
	// used by ConnectCommand and DownloadCommand
	public static final char[] DOWNLOAD_KEYWORD = "__DOWNLOAD_START".toCharArray();
	
	/** Shorthand abbreviation for home directory. */
	private static final String HOME_DIR = "~";
	
	/**
	 * Resolves the given path by checking if it's relative or absolute. If the
	 * {@code str} path parameter is an absolute path then this method trivially
	 * returns the given path. In the simplest case, the given path does not
	 * have a root component, in which case this method joins the given path
	 * with the root and returns the absolute path. If the given path has
	 * invalid characters an {@code InvalidPathException} is thrown.
	 * 
	 * @param env an environment
	 * @param str the given path string
	 * @return the absolute path of the given path
	 * @throws InvalidPathException if string cannot be converted into a Path
	 */
	public static Path resolveAbsolutePath(Environment env, String str) {
		str = str.replace("\"", "");
		
		/* May throw an exception. */
		Path path = Paths.get(str);
		
		/* If it starts with a tilde, recurse back to this method with user.home. */
		if (path.startsWith(HOME_DIR)) {
			return resolveAbsolutePath(env, System.getProperty("user.home").concat(str.substring(1)));
		}
		
		Path newPath;
		if (path.isAbsolute()) {
			newPath = path;
		} else {
			newPath = env.getCurrentPath().resolve(path).normalize();
		}
		return newPath;
	}
	
	/**
	 * Used for extracting arguments passed to a function. This method supports
	 * an unlimited number of arguments, and can be inputed either with
	 * quotation marks or not. Returns an array of strings containing the
	 * extracted arguments.
	 * <p>
	 * This is the same as calling the {@link #extractArguments(String, int)
	 * extractArguments(s, 0)} method.
	 * 
	 * @param s a string containing arguments
	 * @return an array of strings containing extracted arguments.
	 */
	public static String[] extractArguments(String s) {
		return extractArguments(s, 0);
	}
	
	/**
	 * Used for extracting arguments passed to a function. This method splits
	 * arguments until it runs out of matches or reaches the specified
	 * <tt>limit</tt>, which ever comes first. Arguments can be inputed either
	 * with quotation marks or not. Returns an array of strings containing the
	 * extracted arguments.
	 * 
	 * @param s a string containing arguments
	 * @param limit limit of splitting the arguments, 0 for no limit
	 * @return an array of strings containing extracted arguments.
	 */
	public static String[] extractArguments(String s, int limit) {
		if (s == null) return new String[0];
		
		List<String> list = new ArrayList<>();
		
		String regex = "\"([^\"]*)\"|(\\S+)";
		Matcher m = Pattern.compile(regex).matcher(s);
		while (m.find()) {
			if (--limit == 0 && !m.hitEnd()) {
				list.add(s.substring(m.start()).trim());
				break;
			}
			
			if (m.group(1) != null) {
				list.add(m.group(1));
			} else {
				list.add(m.group(2));
			}
		}

		return list.toArray(new String[list.size()]);
	}

	/**
	 * Prompts the user to confirm an action. The user is expected to input
	 * <tt>Y</tt> as a <i>yes</i> or <tt>N</tt> as a <i>no</i>. Returns true if
	 * the user answered yes, false if no.
	 * <p>
	 * This method blocks until the user answers yes or no.
	 * 
	 * @param env an environment
	 * @return true if the user answered yes, false if no
	 */
	public static boolean promptConfirm(Environment env) {
		while (true) {
			String line = AbstractCommand.readLine(env);
			
			if (line.equalsIgnoreCase("Y")) {
				return true;
			} else if (line.equalsIgnoreCase("N")) {
				return false;
			} else {
				AbstractCommand.write(env, "Please answer Y / N: ");
				continue;
			}
		}
	}
	
	/**
	 * Converts the number of bytes to a human readable byte count with binary
	 * prefixes.
	 * 
	 * @param bytes number of bytes
	 * @return human readable byte count with binary prefixes
	 */
	public static String humanReadableByteCount(long bytes) {
		/* Use the natural 1024 units and binary prefixes. */
		int unit = 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = "kMGTPE".charAt(exp - 1) + "i";
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	/**
	 * Parses the specified string <tt>str</tt> as size and returns size in
	 * bytes. The given argument may be a human readable byte count with units
	 * specified, not necessarily separated by a space. If units are not
	 * specified in the string, the parsed <tt>long</tt> value is returned.
	 * Argument is case insensitive.
	 * <p>
	 * Examples of this method usage are the following:
	 * <p>
	 * <table border="1">
	 * <tr><th>Method call</th><th>Result</th></tr>
	 * <tr><td>parseSize("2 kiB")</td><td>2048 bytes</td></tr>
	 * <tr><td>parseSize("10 MiB")</td><td>10485760 bytes</td></tr>
	 * <tr><td>parseSize("1 GiB")</td><td>1073741824 bytes</td></tr>
	 * <tr><td>parseSize("16 B")</td><td>16 bytes</td></tr>
	 * <tr><td>parseSize("4096")</td><td>4096 bytes</td></tr>
	 * <tr><td>parseSize("2.5 kiB")</td><td>2560 bytes</td></tr>
	 * <tr><td>parseSize("1kiB")</td><td>1024 bytes</td></tr>
	 * <tr><td>parseSize("1MIB")</td><td>1048576 bytes</td></tr>
	 * <tr><td><br/></td><td></td></tr>
	 * <tr><td>parseSize("1 kB")</td><td>1000 bytes</td></tr>
	 * <tr><td>parseSize("1 MB")</td><td>1000000 bytes</td></tr>
	 * <tr><td>parseSize("1 gb")</td><td>1000000000 bytes</td></tr>
	 * <tr><td>parseSize("1gB")</td><td></td></tr>
	 * <tr><td>parseSize("1.23456789 GB")</td><td>1234567890 bytes</td></tr>
	 * <tr><td><br/></td><td></td></tr>
	 * <tr><td>parseSize("")</td><td>IllegalArgumentException</td></tr>
	 * <tr><td>parseSize("-2 kB")</td><td>IllegalArgumentException</td></tr>
	 * <tr><td>parseSize("2..5 kB")</td><td>IllegalArgumentException</td></tr>
	 * <tr><td>parseSize("Foo")</td><td>IllegalArgumentException</td></tr>
	 * </table>
	 * 
	 * @param s string to be parsed as size
	 * @return long value of the parsed size
	 * @throws IllegalArgumentException if string can not be parsed as size
	 */
	public static long parseSize(String s) {
		if (s == null || s.trim().isEmpty()) {
			throw new IllegalArgumentException("Cannot parse empty string.");
		}
		
		/* Trim and replace commas with dots. */
		s = s.trim().replace(",", ".");
		
		/* Check if it is only a number without a unit. */
		s = s.trim();
		if (isDouble(s)) {
			return (long) Double.parseDouble(s);
		}
		
		/* Separate digits and alphas - number and unit. */
		StringBuilder numSb = new StringBuilder();
		String unitStr = "";
		for (int i = 0, n = s.length(); i < n; i++) {
			char c = s.charAt(i);
			if (Character.isDigit(c) || c == '.') {
				numSb.append(c);
			} else {
				unitStr = s.substring(i).trim();
				break;
			}
		}
		
		/* Number is composed of digits. Parse it. */
		double num = Double.parseDouble(numSb.toString());
		
		/* If the unit is bytes, number can be returned immediately. */
		if (unitStr.equalsIgnoreCase("B")) {
			return (long) num;
		}
		
		/* Get the unit prefix (i.e. k, M, G, ...) and suffix (iB or B). */
		char unitPrefix = unitStr.toUpperCase().charAt(0);
		String unitSuffix = unitStr.substring(1);
		
		/* Set up the unit if suffix is valid. */
		int unit;
		if (unitSuffix.equalsIgnoreCase("iB")) {
			unit = 1024;
		} else if (unitSuffix.equalsIgnoreCase("B")) {
			unit = 1000;
		} else {
			throw new IllegalArgumentException("Invalid unit: " + unitStr);
		}
		
		/* Set up the exponent if prefix is valid. */
		int exp = "KMGTPE".indexOf(unitPrefix) + 1;
		if (exp == 0) {
			throw new IllegalArgumentException("Invalid unit: " + unitStr);
		}
		
		/* Final calculation. */
		return (long) (num * Math.pow(unit, exp));
	}
	
	/**
	 * Converts nanoseconds to a human readable time unit, rounding it to the
	 * nearest whole number.
	 * 
	 * @param nanoseconds nanoseconds to be converted
	 * @return human readable time unit
	 */
	public static String humanReadableTimeUnit(long nanoseconds) {
		final long nanosecond = 1L;
		final long microsecond = nanosecond * 1000L;
		final long milisecond = microsecond * 1000L;
		final long second = milisecond * 1000L;
		final long minute = second * 60L;
		final long hour = minute * 60L;
		final long day = hour * 24L;
		
		String retVal;
		long remainder = 0L;
		if (nanoseconds < microsecond) {
			retVal = nanoseconds + " ns";
		} else if (nanoseconds < milisecond) {
			retVal = nanoseconds / microsecond + " us";
		} else if (nanoseconds < second) {
			retVal = nanoseconds / milisecond + " ms";
		} else if (nanoseconds < minute) {
			retVal = nanoseconds / second + " s";
		} else if (nanoseconds < hour) {
			retVal = nanoseconds / minute + " min";
			remainder = nanoseconds % minute;
		} else if (nanoseconds < day) {
			retVal = nanoseconds / hour + " hr";
			remainder = nanoseconds % hour;
		} else {
			retVal = nanoseconds / day + " days";
			remainder = nanoseconds % day;
		}

		return retVal + (remainder != 0L ? " "+humanReadableTimeUnit(remainder) : "");
	}
	
	/**
	 * Returns true if string <tt>s</tt> can be parsed as an <tt>Integer</tt>
	 * using the {@linkplain Integer#parseInt(String)} method. False otherwise.
	 * 
	 * @param s the user parameter entry
	 * @return true if <tt>s</tt> can be parsed as Integer, false otherwise
	 */
	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * Returns true if string <tt>s</tt> can be parsed as a <tt>Long</tt>
	 * using the {@linkplain Long#parseLong(String)} method. False otherwise.
	 * 
	 * @param s the user parameter entry
	 * @return true if <tt>s</tt> can be parsed as Long, false otherwise
	 */
	public static boolean isLong(String s) {
		try {
			Long.parseLong(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * Returns true if string <tt>s</tt> can be parsed as a <tt>Double</tt>
	 * using the {@linkplain Double#parseDouble(String)} method. False otherwise.
	 * 
	 * @param s the user parameter entry
	 * @return true if <tt>s</tt> can be parsed as Double, false otherwise
	 */
	public static boolean isDouble(String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * Splits the specified <tt>pattern</tt> string around matches of asterisk
	 * symbols.
	 * 
	 * @param pattern pattern to be split
	 * @return the array of strings computed by splitting this string around
	 *         matches of asterisk symbols
	 */
	public static String[] splitPattern(String pattern) {
		return pattern.split("\\*");
	}
	
	/**
	 * Returns true if the given param {@code str} matches the {@code pattern}.
	 * The {@code pattern} can contain asterisk characters ("*") that represent
	 * 0 or more characters between the parts that are matching.
	 * <p>
	 * For an example, the pattern &quot;*.java&quot; will list all files ending
	 * with .java extension.
	 * 
	 * @param str a string that is being examined
	 * @param pattern a pattern that may contain the asterisk character
	 * @return true if {@code str} matches the {@code pattern}. False otherwise
	 */
	public static boolean matches(String str, String pattern) {
		return matches(str, splitPattern(pattern));
	}
	
	/**
	 * Returns true if the given param {@code str} matches the
	 * {@code patternParts}. The {@code patternParts} are parts of the pattern
	 * which all must match the specified {@code str}. There may be 0 or more
	 * characters between each part of the pattern, which will be ignored.
	 * <p>
	 * For an example, the pattern parts [&quot;M&quot;, &quot;.java&quot;] will
	 * list all files containing the letter 'M' before the .java literal.
	 * 
	 * @param str a string that is being examined
	 * @param patternParts parts of the pattern to be matched against
	 * @return true if {@code str} matches the {@code patternParts}. False otherwise
	 */
	public static boolean matches(String str, String[] patternParts) {
		int lastIndex = -1;
		for (String part : patternParts) {
			int index = str.indexOf(part);
			if (index <= lastIndex) return false;
			else lastIndex = index;
		}
		
		return true;
	}
	
	/**
	 * Generates the 40 character long SHA-1 password hash of the user's
	 * password by converting the specified <tt>password</tt> to an array of
	 * bytes decoded with the {@link StandardCharsets#UTF_8 UTF-8} charset and
	 * digested with the hash-algorithm.
	 * 
	 * @param password password to be hashed
	 * @return the hash of the specified <tt>password</tt>
	 */
	public static String generatePasswordHash(String password) {
		String pass = password.concat("peaches.*");
		byte[] passwordBytes = pass.getBytes(StandardCharsets.UTF_8);
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new InternalError("Algorithm unavailable (SHA-1)", e);
		}
		
		return DatatypeConverter.printHexBinary(md.digest(passwordBytes));
	}
	
}
