package hr.fer.zemris.java.shell.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.exceptions.IllegalPathException;

/**
 * A helper class. Provides helper methods with various functionalities for the
 * whole application.
 *
 * @author Mario Bobic
 */
public abstract class Helper {

	// used by ConnectCommand and DownloadCommand
	/** Keyword used for sending and detecting a download start. */
	public static final char[] DOWNLOAD_KEYWORD = "__DOWNLOAD_START".toCharArray();
	
	/** Extension used for encrypted files. */
	public static final String CRYPT_FILE_EXT = ".crypt";
	
	/** Shorthand abbreviation for home directory. */
	private static final String HOME_DIR = "~";
	
	/** Shorthand abbreviation for last requested path. */
	private static final String LAST_PATH = "!!";
	
	/**
	 * Disable instantiation or inheritance.
	 */
	private Helper() {
	}
	
	/**
	 * Resolves the given path by checking if it's relative or absolute. If the
	 * {@code str} path parameter is an absolute path then this method trivially
	 * returns the given path. In the simplest case, the given path does not
	 * have a root component, in which case this method joins the given path
	 * with the root and returns the absolute path. If the given path has
	 * invalid characters an {@code InvalidPathException} is thrown.
	 * <p>
	 * The specified string <tt>str</tt> may also be an integer, in which case
	 * it is first checked if a file marked with that number exists.
	 * 
	 * @param env an environment
	 * @param str the given path string
	 * @return the absolute path of the given path
	 * @throws InvalidPathException if string cannot be converted into a Path
	 */
	public static Path resolveAbsolutePath(Environment env, String str) {
		/* If the entered argument is parsable as an integer,
		 * see if a file is marked with that number. */
		if (Helper.isInteger(str)) {
			int num = Integer.parseInt(str);
			Path path = env.getMarked(num);
			if (path != null) return path;
		}
		
		/* Paths.get() may throw an exception. */
		str = str.replace("\"", "");
		Path path = Paths.get(str);
		
		/* If it starts with a tilde, recurse back to this method with user.home. */
		if (path.startsWith(HOME_DIR)) {
			String home = System.getProperty("user.home");
			String rest = str.substring(HOME_DIR.length());
			return resolveAbsolutePath(env, home+rest);
		}
		
		/* If it starts with two exclamations, recurse back to this method with last path. */
		if (path.startsWith(LAST_PATH)) {
			Path last = env.getLastPath();
			String rest = str.substring(LAST_PATH.length());
			return resolveAbsolutePath(env, last+rest);
		}
		
		if (!path.isAbsolute()) {
			path = env.getCurrentPath().resolve(path);
		}
		path = path.normalize();
		
		env.setLastPath(path);
		return path;
	}
	
	/**
	 * Returns the name of the file or directory denoted by this path as a
	 * {@code Path} object. The file name is the <em>farthest</em> element from
	 * the root in the directory hierarchy.
	 * <p>
	 * This method is convenient when <tt>path</tt> is a root directory, because
	 * it trivially returns the given path instead of {@code null}.
	 *
	 * @param path path whose file name is to be returned
	 * @return a path representing the name of the file or directory, or
	 *         {@code null} if this path has zero elements
	 */
	public static Path getFileName(Path path) {
		return path.equals(path.getRoot()) ? path : path.getFileName();
	}
	
	/**
	 * Returns the <em>parent path</em> of the specified <tt>path</tt>.
	 * <p>
	 * The parent of this path object consists of this path's root component, if
	 * any, and each element in the path except for the <em>farthest</em> from
	 * the root in the directory hierarchy. This method does not access the file
	 * system; the path or its parent may not exist.
	 * <p>
	 * This method is convenient when <tt>path</tt> is a root directory, because
	 * it trivially returns the given path instead of {@code null}.
	 *
	 * @param path path whose parent is to be returned
	 * @return a path representing the path's parent
	 */
	public static Path getParent(Path path) {
		return path.equals(path.getRoot()) ? path : path.getParent();
	}

	/**
	 * Tells whether or not a file is considered <em>hidden</em>. The exact
	 * definition of hidden is platform or provider dependent. On UNIX for
	 * example a file is considered to be hidden if its name begins with a
	 * period character ('.'). On Windows a file is considered hidden if the DOS
	 * {@link DosFileAttributes#isHidden hidden} attribute is set.
	 * <p>
	 * Depending on the implementation this method may require to access the
	 * file system to determine if the file is considered hidden.
	 *
	 * @param path the path to the file to be tested
	 * @return {@code true} if the file is considered hidden
	 * @throws SecurityException In the case of the default provider, and a
	 *         security manager is installed, the
	 *         {@link SecurityManager#checkRead(String) checkRead} method is
	 *         invoked to check read access to the file.
	 */
	public static boolean isHidden(Path path) {
		return path.toFile().isHidden();
	}
	
	/**
	 * Returns the first available {@code Path} with a unique file name. The
	 * first available path means that, if a file with the specified
	 * <tt>path</tt> exists on disk, an index is appended to it. If a file with
	 * that path still exists, index is incremented and so on, until a unique
	 * path is generated. This path is then returned.
	 * <p>
	 * If a file with the specified <tt>path</tt> does not exist, this method
	 * trivially returns <tt>path</tt>.
	 * <p>
	 * For an example, if the parent directory of the specified path already
	 * contains <tt>file.txt</tt>, <tt>file-0.txt</tt> and <tt>file-1.txt</tt>,
	 * and the file name of this path is <tt>file.txt</tt>, then a path with
	 * file name <tt>file-2.txt</tt> is returned.
	 * 
	 * @param path path from which the first available is returned
	 * @return a path with a unique file name
	 */
	public static Path firstAvailable(Path path) {
		if (!Files.exists(path))
			return path;
		
		int namingIndex = 0;
		String name = path.getFileName().toString();
		String extension = "";
		
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex > 0) {
			extension = name.substring(dotIndex);
			name = name.substring(0, dotIndex);
		}
		name += "-";
		
		while (Files.exists(path)) {
			path = path.resolveSibling(name + namingIndex + extension);
			namingIndex++;
		}
		return path;
	}
	
	/**
	 * Returns the file name extension of the specified <tt>path</tt>. Extension
	 * is considered as the last period symbol in the file name that may be
	 * followed by sequence of characters.
	 * <p>
	 * If the specified path does not have an extension, or in other words if
	 * its file name does not contain a period symbol, the extension is
	 * considered non-existent and an empty string is returned.
	 * <p>
	 * If the file name ends with a period symbol, the period is returned as an
	 * extension.
	 * 
	 * @param path path whose extension is to be determined
	 * @return the file name extension of the specified path
	 */
	public static String extension(Path path) {
		String name = path.getFileName().toString();
		String extension = "";
		
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex > 0) {
			extension = name.substring(dotIndex);
		}
		
		return extension;
	}
	
	/**
	 * Checks that the specified <tt>path</tt> exists . This method is designed
	 * primarily for doing parameter validation in methods and constructors, as
	 * demonstrated below:
	 * <blockquote><pre>
     * public Foo(Path path) {
     *     this.path = Helper.requireExists(path);
     * }
     * </pre></blockquote>
	 *
	 * @param path path to be checked
	 * @return <tt>path</tt> if it exists
	 * @throws IllegalPathException if path does not exist
	 */
	public static Path requireExists(Path path) {
		if (!Files.exists(path)) {
			throw new IllegalPathException("The system cannot find the path specified: " + path);
		}
		
		return path;
	}
	
	/**
	 * Checks that the specified <tt>path</tt> exists and is a directory. This
	 * method is designed primarily for doing parameter validation in methods
	 * and constructors, as demonstrated below:
	 * <blockquote><pre>
     * public Foo(Path path) {
     *     this.path = Helper.requireDirectory(path);
     * }
     * </pre></blockquote>
	 *
	 * @param path path to be checked
	 * @return <tt>path</tt> if it exists and is a directory
	 * @throws IllegalPathException if path does not exist or is not a directory
	 */
	public static Path requireDirectory(Path path) {
		requireExists(path);
		if (!Files.isDirectory(path)) {
			throw new IllegalPathException("The specified path must be a directory: " + path);
		}
		
		return path;
	}
	
	/**
	 * Checks that the specified <tt>path</tt> exists and is a regular file.
	 * This method is designed primarily for doing parameter validation in
	 * methods and constructors, as demonstrated below:
	 * <blockquote><pre>
     * public Foo(Path path) {
     *     this.path = Helper.requireFile(path);
     * }
     * </pre></blockquote>
	 *
	 * @param path path to be checked
	 * @return <tt>path</tt> if it exists and is a regular file
	 * @throws IllegalPathException if path does not exist or is not a file
	 */
	public static Path requireFile(Path path) {
		requireExists(path);
		if (!Files.isRegularFile(path)) {
			throw new IllegalPathException("The specified path must be a file: " + path);
		}
		
		return path;
	}
	
	/**
	 * Extracts arguments from the specified string <tt>s</tt>. This method
	 * supports an unlimited number of arguments, and can be entered either with
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
	 * Extracts arguments from the specified string <tt>s</tt>. This method
	 * splits arguments until it runs out of matches or reaches the specified
	 * <tt>limit</tt>, which ever comes first. Arguments can be entered either
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
	 * Returns the index within the specified string <tt>str</tt> of the first
	 * occurrence of a whitespace character determined by the
	 * {@linkplain Character#isWhitespace(char)} method.
	 * 
	 * @param str string whose index of the first whitespace is to be returned
	 * @return the index of the first occurrence of a whitespace character
	 */
	public static int indexOfWhitespace(String str) {
		for (int i = 0, n = str.length(); i < n; i++) {
			if (Character.isWhitespace(str.charAt(i))) {
				return i;
			}
		}
		return -1;
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
		return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
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
	 * <tr><td><br></td><td></td></tr>
	 * <tr><td>parseSize("1 kB")</td><td>1000 bytes</td></tr>
	 * <tr><td>parseSize("1 MB")</td><td>1000000 bytes</td></tr>
	 * <tr><td>parseSize("1 gb")</td><td>1000000000 bytes</td></tr>
	 * <tr><td>parseSize("1gB")</td><td>1000000000 bytes</td></tr>
	 * <tr><td>parseSize("1.23456789 GB")</td><td>1234567890 bytes</td></tr>
	 * <tr><td><br></td><td></td></tr>
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
	 * Splits the specified <tt>pattern</tt> string around matches of asterisk
	 * symbols.
	 * 
	 * @param pattern pattern to be split
	 * @return the array of strings computed by splitting this string around
	 *         matches of asterisk symbols
	 */
	public static String[] splitPattern(String pattern) {
		// (?<!\\)    Matches if the preceding character is not a backslash
		// (?:\\\\)*  Matches any number of occurrences of two backslashes
		// \*         Matches an asterisk
		String[] split = pattern.split("(?<!\\\\)(?:\\\\\\\\)*\\*");
		for (int i = 0; i < split.length; i++) {
			split[i] = split[i].replace("\\*", "*");
		}
		
		return split;
	}
	
	/**
	 * Splits the specified <tt>pattern</tt> string around matches of asterisk
	 * symbols.
	 * 
	 * @param pattern pattern to be split
	 * @return the array of strings computed by splitting this string around
	 *         matches of asterisk symbols
	 */
	// TODO should I leave this method?
	public static String[] splitPattern2(String pattern) {
		// (?<!\\)    Matches if the preceding character is not a backslash
		// (?:\\\\)*  Matches any number of occurrences of two backslashes
		// \*         Matches an asterisk
		String[] split = pattern.split("(?<!\\\\)(?:\\\\\\\\)*\\*");
		return Arrays.stream(split)
			.map(str -> str.replace("\\*", "*"))
			.collect(Collectors.toList())
			.toArray(split);
	}
	
	/**
	 * Returns true if the given param {@code str} matches the {@code pattern}.
	 * The {@code pattern} can contain asterisk characters ("*") that represent
	 * 0 or more characters between the parts that are matching.
	 * <p>
	 * For an example, pattern &quot;*.java&quot; will list all files containing
	 * the .java character sequence.
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
			// TODO use a better algorithm (SHA-512?) http://stackoverflow.com/questions/2640566/why-use-sha1-for-hashing-secrets-when-sha-512-is-more-secure
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new InternalError("Algorithm unavailable (SHA-1)", e);
		}
		
		return DatatypeConverter.printHexBinary(md.digest(passwordBytes));
	}
	
	/**
	 * Returns a charset object for the named charset, or <tt>null</tt> if a
	 * charset with the specified <tt>name</tt> can not be resolved.
	 * 
	 * @param name name of the requested charset; may be either a canonical name
	 *        or an alias
	 * @return a charset object for the named charset
	 */
	public static Charset resolveCharset(String name) {
		try {
			return Charset.forName(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
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
	 * Returns the first element of the specified <tt>list</tt>. If the list is
	 * empty, <tt>null</tt> is returned. This is convenient because it cuts out
	 * exception handling.
	 * 
	 * @param <T> type
	 * @param list list from which first element is returned
	 * @return the first element of the specified list
	 */
	public static <T> T firstElement(List<T> list) {
		return list.isEmpty() ? null : list.get(0);
	}
	
	/**
	 * Returns the last element of the specified <tt>list</tt>. If the list is
	 * empty, <tt>null</tt> is returned. This is convenient because it cuts out
	 * exception handling.
	 * 
	 * @param <T> type
	 * @param list list from which last element is returned
	 * @return the last element of the specified list
	 */
	public static <T> T lastElement(List<T> list) {
		return list.isEmpty() ? null : list.get(list.size()-1);
	}
	
	/**
	 * Returns the first argument if it is not <tt>null</tt>, or the second
	 * argument if the first one is <tt>null</tt>.
	 * 
	 * @param <T> argument type
	 * @param nullable value that may be null
	 * @param substitute substitute value, if the first one is null
	 * @return <tt>nullable != null ? nullable : substitute</tt>
	 */
	public static <T> T ifNull(T nullable, T substitute) {
		return nullable != null ? nullable : substitute;
	}

	/**
	 * Returns this computer's local IP address or <tt>null</tt> if the IP
	 * address is inaccessible.
	 * 
	 * @return this computer's local IP address or <tt>null</tt>
	 */
	// TODO Any better way of obtaining local IP address?
	public static String getLocalIP() {
		try {
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
			while (en.hasMoreElements()) {
				NetworkInterface n = en.nextElement();
				
				Enumeration<InetAddress> ee = n.getInetAddresses();
			    while (ee.hasMoreElements()) {
			        InetAddress addr = ee.nextElement();
			        if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
			        	return addr.getHostAddress();
			        }
			    }
			}
		} catch (SocketException e) {}
		return null;
	}
	
	/**
	 * Returns the external IP address of the router this computer is connected
	 * to. Returns {@code null} if the IP address is inaccessible.
	 * 
	 * @return this computer's public IP address or <tt>null</tt>
	 */
	// TODO Any better way of obtaining public IP address?
	public static String getPublicIP() {
		try {
			URL whatismyip = new URL("http://checkip.amazonaws.com");
			
			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(whatismyip.openStream())
			)) {
				String ip = in.readLine();
				return ip;
			}
		} catch (IOException e) {
			return null;
		}
    }
	
}
