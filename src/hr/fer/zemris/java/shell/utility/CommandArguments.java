package hr.fer.zemris.java.shell.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import hr.fer.zemris.java.shell.utility.exceptions.InvalidFlagException;

/**
 * This class acts as a bridge between a single input string and arguments by
 * which some commands operate on.
 * <h2>Pre-compilation (Defining)</h2>
 * <p>
 * Upon creation, flag definitions must be given in order to appropriately
 * compile the input string. When all desired flag definitions are given, an
 * instance of this class is ready to compile an input string storing all its
 * flags to a map of flags, leaving out the given string without flags. In other
 * words, the given argument is <em>clean</em> of flags.
 * <p>
 * There are various constructors and static methods of this class that attempt
 * to make command parsing and compiling easier and within a few lines of code.
 * <p>
 * Flags can be defined as one-letter flags or multiple-letter flags. One-letter
 * flags can be defined using the constructors or static compilation methods of
 * this class. For multiple-letter flags, flag definitions must be added
 * manually using the appropriate method.
 * <p>
 * Flags may or may not be defined to require an additional argument. Additional
 * arguments are useful when defining quantity, method, size or any additional
 * element that the flag uses. Flags that do not require additional arguments
 * are <tt>boolean</tt> flags and define behavior of should something exist or
 * not.
 * <h2>Post-compilation (Usage)</h2>
 * <p>
 * In the input string, flags with one letters can be merged into a single
 * argument, beginning with a single dash followed by a desired number of flags.
 * In this case all flags must be defined not to require an additional argument.
 * <p>
 * Multiple-letter flags, as well as one-letter flags that require an argument
 * should be separated by a whitespace in the input string.
 * <p>
 * Dash escaping is supported. In order to use an argument starting with a dash
 * that should not be interpreted as a flag, escape the beginning dash symbol.
 * <p>
 * Flag argument starting with a dash should not be escaped since the flag
 * consumes its argument in plain text as it is given.
 *
 * @author Mario Bobic
 */
public class CommandArguments {
	
	/** Definitions of flags to be used while compiling. */
	private Map<String, Boolean> flagDefinitions = new HashMap<>();
	/** Map of compiled flags where key is flag name and value is the flag. */
	private Map<String, Flag> flags = new HashMap<>();
	
	/** Compiled argument cleared of flags. */
	private String cleanArgument;
	
	/**
	 * Constructs an instance of {@code CommandArguments} without flag
	 * definitions. Flag definitions should be added with the
	 * {@link #addFlagDefinition(String, boolean)} method.
	 */
	public CommandArguments() {
	}
	
	/**
	 * Constructs an instance of {@code CommandArguments} with the specified
	 * one-letter flags of which none expect an additional argument.
	 *
	 * @param flags one-letter flag definitions that do not require arguments
	 * @throws NullPointerException if <tt>flags</tt> is <tt>null</tt>
	 */
	public CommandArguments(String flags) {
		this(flags, createFalseArray(flags.length()));
	}
	
	/**
	 * Constructs an instance of {@code CommandArguments} with the specified
	 * one-letter flags and boolean values that indicate must each flag receive
	 * an argument or not.
	 *
	 * @param flags one-letter flag definitions
	 * @param hasArguments sequential boolean values of flag argument requirement
	 * @throws NullPointerException if <tt>flags</tt> is <tt>null</tt>
	 * @throws IllegalArgumentException if <tt>flags.length() != hasArguments.length</tt>
	 */
	public CommandArguments(String flags, boolean... hasArguments) {
		Objects.requireNonNull(flags, "Flags must not be null.");
		Objects.requireNonNull(hasArguments, "hasArguments array must not be null.");
		if (flags.length() != hasArguments.length) {
			throw new IllegalArgumentException("flags and hasArguments must be of same length!");
		}
		
		for (int i = 0; i < flags.length(); i++) {
			addFlagDefinition(flags.substring(i, i+1), hasArguments[i]);
		}
	}
	
	/**
	 * Returns a {@code CommandArguments} object with {@link #compile(String)
	 * compiled} one-letter flags from the specified string <tt>s</tt>. These
	 * flags, by default, do not require an argument.
	 * <p>
	 * To manually define which flags should use an argument and which should
	 * not, use the {@link #compile(String, String, boolean...)} method.
	 * 
	 * @param s string to be compiled
	 * @param flags one-letter flag definitions that do not require arguments
	 * @return a {@code CommandArguments} object with compiled one-letter flags
	 * @throws NullPointerException if <tt>flags</tt> is <tt>null</tt>
	 */
	public static CommandArguments compile(String s, String flags) {
		return compile(s, flags, createFalseArray(flags.length()));
	}
	
	/**
	 * Returns a {@code CommandArguments} object with {@link #compile(String)
	 * compiled} one-letter flags from the specified string <tt>s</tt>. The
	 * <tt>hasArguments</tt> parameter indicates should each flag be given with
	 * an additional argument upon compiling or no.
	 * <p>
	 * Check the linked <tt>compile</tt> method for more information.
	 * 
	 * @param s input string to be compiled
	 * @param flags one-letter flag definitions
	 * @param hasArguments sequential boolean values of flag argument requirement
	 * @return a {@code CommandArguments} object with compiled one-letter flags
	 * @throws IllegalArgumentException if
	 *         <tt>flags.length() != hasArguments.length</tt>
	 * @throws NullPointerException if either <tt>flags</tt> or
	 *         <tt>hasArguments</tt> is <tt>null</tt>
	 */
	public static CommandArguments compile(String s, String flags, boolean... hasArguments) {
		CommandArguments cmdArgs = new CommandArguments(flags, hasArguments);
		cmdArgs.compile(s);
		return cmdArgs;
	}
	
	/**
	 * Creates and returns a <tt>boolean</tt> array with the specified
	 * <tt>length</tt> containing all <tt>false</tt> values.
	 * 
	 * @param length length of the array to be created
	 * @return a boolean array with the specified length containing false values
	 */
	private static boolean[] createFalseArray(int length) {
		boolean[] arr = new boolean[length];
		Arrays.fill(arr, false);
		return arr;
	}
	
	/**
	 * Puts an escape symbol in front of dashes that are preceded by a
	 * whitespace symbol, in the specified string <tt>s</tt>.
	 * <p>
	 * This method is convenient since the {@link #compile(String)} method does
	 * not remove the escape symbol from escaped dashes.
	 * 
	 * @param s string whose flags are to be escaped
	 * @return the string with escaped flags
	 */
	public static String escapeFlags(String s) {
		return s == null ? null : s.replaceAll("(\\s|^)-", "$1\\\\-");
	}
	
	/**
	 * Adds a flag definition which is used upon compiling an input string with
	 * the {@link #compile(String)} method. Flags that were not defined but were
	 * found in the input string of the compile method throw an
	 * {@code InvalidFlagException} with an appropriate detail message.
	 * <p>
	 * The <tt>hasArgument</tt> attribute indicates if the flag should be
	 * followed by an argument or not. Flags that require an argument are
	 * whitespace-separated from the argument.
	 * <p>
	 * If the given <tt>name</tt> is a <tt>null</tt> reference, this method
	 * returns without mapping any value.
	 * 
	 * @param name name of the flag
	 * @param hasArgument defines if the flag receives an argument
	 * @throws IllegalArgumentException if flag with the specified name has
	 *         already been defined
	 */
	public void addFlagDefinition(String name, boolean hasArgument) {
		if (name == null) return;
		
		if (flagDefinitions.containsKey(name)) {
			throw new IllegalArgumentException("Flag " + name + " is already defined.");
		}
		
		flagDefinitions.put(name, hasArgument);
	}
	
	/**
	 * Adds a flag definition which is used upon compiling an input string with
	 * the {@link #compile(String)} method. Flags that were not defined but were
	 * found in the input string of the compile method throw an
	 * {@code InvalidFlagException} with an appropriate detail message.
	 * <p>
	 * The <tt>hasArgument</tt> attribute indicates if the flag should be
	 * followed by an argument or not. Flags that require an argument are
	 * whitespace-separated from the argument.
	 * <p>
	 * Names that are <tt>null</tt> will not be mapped and will be ignored.
	 * <p>
	 * This method is useful for defining flags that have the same
	 * functionality, but different names - a long and short flag name.
	 * 
	 * @param shortName one-letter name of the flag
	 * @param longName multiple-letter name of the flag
	 * @param hasArgument defines if the flag receives an argument
	 * @throws IllegalArgumentException if flag with either given short name or
	 *         long name has already been defined
	 */
	public void addFlagDefinition(String shortName, String longName, boolean hasArgument) {
		addFlagDefinition(shortName, hasArgument);
		addFlagDefinition(longName, hasArgument);
	}
	
	/**
	 * Compiles the specified string <tt>s</tt>, storing all its flags to a map
	 * of flags, leaving out the given string without flags. In other words, the
	 * argument is <em>clean</em> of flags.
	 * <p>
	 * In order for this method to successfully process flags, they must be
	 * defined earlier with the {@link #addFlagDefinition(String, boolean)}
	 * method. Flags that were not defined, but were listed in the specified
	 * string <tt>s</tt> will throw an {@code InvalidFlagException}. One-letter
	 * flags that were defined to receive an argument but are merged with
	 * another flag will throw the same exception with a different detail
	 * message. If there is no argument provided for a flag that is defined to
	 * require an argument, the same exception is thrown.
	 * <p>
	 * If the given input string <tt>s</tt> is <tt>null</tt>, it means no flags
	 * are defined and the returning clean string is also <tt>null</tt>.
	 * <p>
	 * String arguments with an escape symbol in front of a dash <strong>will
	 * not be</strong> unescaped in this method.
	 * 
	 * @param s string to be compiled
	 * @return a string <em>clean</em> of flags, or null if s was null
	 * @throws InvalidFlagException if a flag found in <tt>s</tt> was not
	 *         defined, there are multiple one-letter flags where at least one
	 *         receives an argument or there is no argument provided for a flag
	 *         that is defined to require an argument
	 */
	// TODO compiling flags breaks newlines and tabs and replaces them with spaces
	public String compile(String s) {
		return compile(s, true);
	}
	
	/**
	 * Compiles the specified string <tt>s</tt>, storing all its flags to a map
	 * of flags, leaving out the given string without flags. In other words, the
	 * argument is <em>clean</em> of flags.
	 * <p>
	 * In order for this method to successfully process flags, they must be
	 * defined earlier with the {@link #addFlagDefinition(String, boolean)}
	 * method. Flags that were not defined, but were listed in the specified
	 * string <tt>s</tt> will throw an {@code InvalidFlagException}, unless the
	 * <tt>throwForUndefined</tt> value is set to true. One-letter flags that
	 * were defined to receive an argument but are merged with another flag will
	 * throw the same exception with a different detail message. If there is no
	 * argument provided for a flag that is defined to require an argument, the
	 * same exception is thrown.
	 * <p>
	 * If the given input string <tt>s</tt> is <tt>null</tt>, it means no flags
	 * are defined and the returning clean string is also <tt>null</tt>.
	 * <p>
	 * String arguments with an escape symbol in front of a dash <strong>will
	 * not be</strong> unescaped in this method.
	 * 
	 * @param s string to be compiled
	 * @param throwForUndefined true if {@code InvalidFlagException} should be
	 *        thrown if undefined flags are encountered
	 * @return a string <em>clean</em> of flags, or null if s was null
	 * @throws InvalidFlagException if a flag found in <tt>s</tt> was not
	 *         defined, there are multiple one-letter flags where at least one
	 *         receives an argument or there is no argument provided for a flag
	 *         that is defined to require an argument
	 */
	public String compile(String s, boolean throwForUndefined) {
		if (s == null) {
			return null;
		}
		
		cleanArgument = null;
		String[] args = StringHelper.extractArguments(s, 0, true);
		Set<String> undefinedFlags = new LinkedHashSet<>();
		StringJoiner cleanArgumentBuilder = new StringJoiner(" ");
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			
			if (arg.startsWith("--") && arg.length() > 2) {
				/* Long flags */
				String name = arg.substring(2);
				i = processFlag(name, undefinedFlags, args, 1, i);
			} else if (arg.startsWith("-") && arg.length() > 1) {
				/* Short flags */
				String names = arg.substring(1);
				for (int j = 0, n = names.length(); j < n; j++) {
					String name = names.substring(j, j+1);
					i = processFlag(name, undefinedFlags, args, n, i);
				}
			} else {
				cleanArgumentBuilder.add(arg);
			}
			
			if (!throwForUndefined && !undefinedFlags.isEmpty()) {
				cleanArgumentBuilder.add(arg);
				undefinedFlags.clear();
			}
		}
		
		if (!undefinedFlags.isEmpty()) {
			StringJoiner sj = new StringJoiner(", ");
			undefinedFlags.forEach(sj::add);
			throw new InvalidFlagException("Argument contains undefined flags: " + sj, undefinedFlags);
		}
		
		if (cleanArgumentBuilder.length() == 0) {
			cleanArgument = null;
		} else {
			cleanArgument = cleanArgumentBuilder.toString().replaceAll("\\\\-", "-");
		}
		
		return cleanArgument;
	}
	
	/**
	 * Processes a flag with the specified <tt>name</tt>, storing defined and
	 * valid flags to a map of flags and undefined flags to the given set. For
	 * invalid flags, an {@code InvalidFlagException} is thrown with an
	 * appropriate detail message.
	 * <p>
	 * The array of arguments extracted from the input string <tt>args</tt> is
	 * needed for flags that are defined to require an argument.
	 * <p>
	 * The number of flags contained within one argument <tt>n</tt> makes sense
	 * for one-letter flags, as they can be given together. One-letter flags
	 * that were defined to receive an argument but are merged with another flag
	 * will throw an exception. For multiple-letter flags, <tt>n</tt> should
	 * always be one.
	 * <p>
	 * The argument index within the array of arguments <tt>i</tt> is needed in
	 * order for the flag to consume its argument, that is, the next argument of
	 * the array. The index is then incremented by one and returned as an
	 * indication that argument <tt>i+1</tt> was also consumed.
	 * 
	 * @param name name of the flag
	 * @param undefinedFlags set of undefined flags
	 * @param args array of arguments extracted from the input string
	 * @param i argument index within the array of arguments
	 * @param n number of flags contained within one argument
	 * @return the argument index <tt>i</tt> which may be increased by one
	 * @throws InvalidFlagException if there are multiple one-letter flags where
	 *         at least one receives an argument or there is no argument
	 *         provided for a flag that is defined to require an argument
	 */
	private int processFlag(String name, Set<String> undefinedFlags, String[] args, int n, int i) {
		// Flag name has to be predefined
		if (!flagDefinitions.containsKey(name)) {
			undefinedFlags.add(name);
			return i;
		}

		boolean requiresArgument = flagDefinitions.get(name);
		String argument = null;
		
		// If flag requires an argument
		if (requiresArgument) {
			// but there is more than 1 flag
			if (n > 1) {
				throw new InvalidFlagException("Flags with arguments must be defined separately: " + name, name);
			}
			
			// or the flag is not followed by an argument
			if (i == args.length-1) {
				throw new InvalidFlagException("Flag argument not given: " + name, name);
			}
			
			// else all is OK, consume the argument
			argument = args[++i];
		}

		// Get flag or null
		Flag flag = flags.get(name);
		
		if (flag != null && requiresArgument) {
			// This is another occurrence of same flag, add its argument
			flag.addArgument(argument);
		} else if (flag == null) {
			// This is a new flag, put it in the map
			flag = new Flag(name, argument);
			flags.put(name, flag);
		}
		
		// Return the index as it may have been increased
		return i;
	}
	
	/**
	 * Returns <tt>true</tt> if a {@link #compile(String) compiled} input string
	 * contained a flag with the specified <tt>name</tt>.
	 * <p>
	 * Multiple names can be specified for which <tt>true</tt> is returned on
	 * the <strong>first occurrence</strong> of the searched flag. This is
	 * useful for testing flags that have the same functionality, but different
	 * names, for example to test if either long or short flag is provided.
	 * 
	 * @param name name of the flag whose presence is to be tested, if found
	 * @param more more names of flags whose presence is to be tested
	 * @return true if a flag in the compiled input string existed, false
	 *         otherwise
	 * @throws NullPointerException if <tt>names == null</tt>
	 */
	public boolean containsFlag(String name, String... more) {
		if (flags.containsKey(name)) {
			return true;
		}
		
		for (String n : more) {
			if (flags.containsKey(n))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Returns a flag with the specified <tt>name</tt> or the first occurrence
	 * of <tt>names</tt> created from the input string given to the
	 * {@link #compile(String)} method or <tt>null</tt> if the flag was not
	 * found.
	 * <p>
	 * Multiple names can be specified by which the flag is defined. It is
	 * expected that the given names relate to the same flag. This is useful for
	 * getting flags that have the same functionality, but different names, for
	 * example if either or both long and short flags were provided.
	 * <p>
	 * Note that flags with different names are stored separately. If multiple
	 * flags are found, a new {@code Flag} is created by the name of
	 * <tt>name</tt>, filled with arguments of each found flag and returned.
	 * 
	 * @param name name of the flag to be returned, if found
	 * @param more more names of the flag to be returned
	 * @return a flag with the specified <tt>name</tt> or <tt>null</tt> if the
	 *         flag was not given as an input string to the <tt>compile</tt>
	 *         method.
	 * @throws NullPointerException if <tt>names == null</tt>
	 */
	public Flag getFlag(String name, String... more) {
		List<Flag> retFlags = new ArrayList<>();
		
		retFlags.add(flags.get(name));
		for (String n : more) {
			retFlags.add(flags.get(n));
		}
		
		retFlags.removeIf(Objects::isNull);
		
		// Get first flag or null
		Flag flag = Helper.firstElement(retFlags);
		if (retFlags.size() > 1) {
			flag = new Flag(name);
			for (Flag retFlag : retFlags) {
				flag.addArguments(retFlag.arguments);
			}
		}
		
		return flag;
	}
	
	/**
	 * Returns a collection of flags created from the input string given to the
	 * {@link #compile(String)} method. If there were no flags given, an empty
	 * collection is returned.
	 * 
	 * @return a collection of flags created from the {@link #compile(String)
	 *         compiled} input string
	 */
	public Collection<Flag> getFlags() {
		return flags.values();
	}
	
	/**
	 * Returns a compiled argument clean of flags.
	 * <p>
	 * Upon compiling with the {@link #compile(String)} method, all flags from
	 * the input string are left out and therefore the argument is
	 * <em>clean</em> of flags.
	 * 
	 * @return a compiled input string clean of flags, or <tt>null</tt> if
	 *         arguments were not compiled previously
	 */
	public String getCleanArgument() {
		return cleanArgument;
	}

	/**
	 * Removes all of the flag mappings from this object. Map of flags that were
	 * recorded upon compiling will be empty after this call returns.
	 */
	public void clearFlags() {
		flags.clear();
	}
	
	/**
	 * Represents a flag with a name and a list of optional arguments.
	 *
	 * @author Mario Bobic
	 */
	public static class Flag {

		/** Name of this flag. */
		private final String name;
		/** Arguments of this flag. */
		private final List<String> arguments;
		
		/**
		 * Constructs an instance of {@code Flag} with the specified name. Flag
		 * arguments are set to an empty list.
		 *
		 * @param name name of the flag
		 */
		public Flag(String name) {
			this(name, null);
		}

		/**
		 * Constructs an instance of {@code Flag} with the specified name and
		 * one argument to be added to the argument list.
		 * <p>
		 * If the specified <tt>argument</tt> is <tt>null</tt>, it is not added
		 * to the list of arguments.
		 *
		 * @param name name of the flag
		 * @param argument flag argument
		 */
		public Flag(String name, String argument) {
			this.name = name;
			this.arguments = new ArrayList<>();
			
			if (argument != null) {
				 arguments.add(argument);
			}
		}
		
		/**
		 * Returns the name of this flag.
		 * 
		 * @return the name of this flag
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns <tt>true</tt> if this flag has a defined argument. More
		 * formally, returns <tt>argument != null</tt>.
		 * 
		 * @return true if this flag has a defined argument
		 */
		public boolean hasArgument() {
			return !arguments.isEmpty();
		}
		
		/**
		 * Returns this flag's first argument or <tt>null</tt> if it does not
		 * exist.
		 * 
		 * @return the first argument of this flag or <tt>null</tt>
		 */
		public String getArgument() {
			return Helper.firstElement(arguments);
		}
		
		/**
		 * Returns an unmodifiable list of this flag's arguments. If this flag
		 * contains no arguments, an empty list is returned.
		 * 
		 * @return an unmodifiable list of this flag's arguments
		 */
		public List<String> getArguments() {
			return Collections.unmodifiableList(arguments);
		}
		
		/**
		 * Adds the specified <tt>argument</tt> to the list of arguments.
		 * 
		 * @param argument argument to be added to the list of arguments
		 */
		public void addArgument(String argument) {
			arguments.add(argument);
		}
		
		/**
		 * Adds the specified list of <tt>arguments</tt> to the list of flag's
		 * arguments.
		 * 
		 * @param arguments arguments to be added to the list of arguments
		 */
		public void addArguments(List<String> arguments) {
			this.arguments.addAll(arguments);
		}
		
		/**
		 * Returns this flag's first argument as an integer.
		 * 
		 * @return the argument of this flag as an integer
		 * @throws InvalidFlagException if parsing to an integer fails
		 */
		public int getIntArgument() {
			try {
				return Integer.parseInt(getArgument());
			} catch (IllegalArgumentException e) {
				throw exceptionForType("a 32-bit integer");
			}
		}
		
		/**
		 * Returns this flag's first argument as a positive integer. Throws an
		 * exception if the argument is not 32-bit integer, or is negative or
		 * zero. May throw an exception if zero is not allowed but the argument
		 * is zero.
		 * 
		 * @param zeroAllowed indicates if zero value is allowed
		 * @return the argument of this flag as a positive integer
		 * @throws InvalidFlagException if parsing to an integer fails or the
		 *         integer is not positive
		 */
		public int getPositiveIntArgument(boolean zeroAllowed) {
			try {
				int i = Integer.parseInt(getArgument());
				if (i < 0 || !zeroAllowed && i == 0)
					throw new IllegalArgumentException();
				return i;
			} catch (IllegalArgumentException e) {
				throw exceptionForType("a 32-bit positive integer");
			}
		}
		
		/**
		 * Returns this flag's first argument as a long integer.
		 * 
		 * @return the argument of this flag as a long integer
		 * @throws InvalidFlagException if parsing to a long integer fails
		 */
		public long getLongArgument() {
			try {
				return Long.parseLong(getArgument());
			} catch (IllegalArgumentException e) {
				throw exceptionForType("a 64-bit integer");
			}
		}
		
		/**
		 * Returns this flag's first argument as a positive long integer. Throws
		 * an exception if the argument is not 64-bit integer, or is negative.
		 * May throw an exception if zero is not allowed but the argument is
		 * zero.
		 * 
		 * @param zeroAllowed indicates if zero value is allowed
		 * @return the argument of this flag as a positive integer
		 * @throws InvalidFlagException if parsing to a long integer fails or
		 *         the long integer is not positive
		 */
		public long getPositiveLongArgument(boolean zeroAllowed) {
			try {
				long i = Long.parseLong(getArgument());
				if (i < 0 || !zeroAllowed && i == 0)
					throw new IllegalArgumentException();
				return i;
			} catch (IllegalArgumentException e) {
				throw exceptionForType("a 32-bit positive integer");
			}
		}
		
		/**
		 * Returns this flag's first argument as a double.
		 * 
		 * @return the argument of this flag as a double
		 * @throws InvalidFlagException if parsing to a double fails
		 */
		public double getDoubleArgument() {
			try {
				return Double.parseDouble(getArgument());
			} catch (IllegalArgumentException e) {
				throw exceptionForType("a double");
			}
		}
		
		/**
		 * Returns this flag's first argument as a long integer considering that
		 * the argument is given as a size parsable by the
		 * {@link Helper#parseSize(String)} method.
		 * 
		 * @return the argument of this flag as a long integer parsed from size
		 * @throws InvalidFlagException if parsing to a long integer fails
		 */
		public long getSizeArgument() {
			try {
				return Helper.parseSize(getArgument());
			} catch (IllegalArgumentException e) {
				throw exceptionForType("size in bytes");
			}
		}
		
		/**
		 * Throws an {@code InvalidFlagException} with a detail message
		 * specifying that the casting from String to cast <tt>type</tt> has
		 * failed.
		 * 
		 * @param type type to which the casting has failed
		 * @return an InvalidFlagException with detail message and flag name
		 */
		private InvalidFlagException exceptionForType(String type) {
			String s = "Argument of flag "+name+" must be "+type+": "+arguments;
			return new InvalidFlagException(s, name);
		}

		@Override
		public String toString() {
			return name + (hasArgument() ? ": "+arguments : "");
		}
		
	}
	
}
