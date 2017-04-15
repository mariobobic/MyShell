package hr.fer.zemris.java.shell.utility;

import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import com.fathzer.soft.javaluator.DoubleEvaluator;

import hr.fer.zemris.java.shell.MyShell;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.interfaces.ShellCommand;

/**
 * After the command has been split into tokens, these tokens or words are
 * expanded or resolved. There are several kinds of expansion performed, which
 * are listed below, in the order that they are expanded.
 * <ol>
 * <li>special character expansion,
 * <li>variable expansion,
 * <li>arithmetic expansion and
 * <li>brace expansion
 * </ol>
 *
 * @author Mario Bobic
 */
// http://tldp.org/LDP/Bash-Beginners-Guide/html/sect_03_04.html
public abstract class Expander {
	
	/** Shorthand abbreviation for last user input. */
	public static final String LAST_INPUT = "!!";
	
	/** Decimal number formatter. */
	private static NumberFormat nf = new DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.US));

	/**
	 * Disable instantiation or inheritance.
	 */
	private Expander() {
	}
	
	/**
	 * Does the expansion of the specified <tt>input</tt> in the following
	 * order:
	 * <ol>
	 * <li>special character expansion (double exclamation-mark),
	 * <li>variable expansion and
	 * <li>brace expansion
	 * </ol>
	 * 
	 * @param env an environment
	 * @param input input to be expanded
	 * @return the expanded input
	 */
	public static List<String> expand(Environment env, String input) {
		String output = input;
		
		/* Special character expansion. */
		output = specialCharacterExpansion(env, output);
		/* Variable expansion. */
		output = variableExpansion(env, output);
		/* Arithmetic expansion. */
		output = arithmeticExpansion(env, output);
		/* Command substitution. */
		output = commandSubstitutionExpansion(env, output);
		
		/* Brace expansion. */
		List<String> lines = braceExpansion(env, output);
		
		// Replace all escaped symbols and escape flags
		lines.replaceAll(line -> {
			return line	.replace("\\$", "$")
						.replace("\\{", "{")
						.replace("\\"+LAST_INPUT, LAST_INPUT)
						.replace("\\\\", "\\")
						.replaceAll("(\\s|^)-", "$1\\\\-");
		});
		
		return lines;
	}

	/**
	 * Expands the specified <tt>input</tt> as a brace expansion. This means
	 * that legal character sequences surrounded with curly braces will be
	 * expanded into multiple lines of input, recursively.
	 * <p>
	 * <strong>Legal character sequences</strong> for this expansion are:
	 * <ul>
	 * <li>arguments separated by a comma, e.g. {old,new,stale,fresh}
	 * <li>exactly two integers or one-letter arguments separated by a
	 * double-period, e.g. {1..10} or {a..z}
	 * <li>a combination of the above, e.g. {1..3,A..C,d..f}
	 * </ul>
	 * 
	 * <h2>Arguments separated by a comma</h2>
	 * <p>
	 * This type of expression will be expanded to a number of lines equal to
	 * the product of amount of arguments in each curly-braces expression.
	 * <p>
	 * For an example, if the input contains two curly-braces expressions, where
	 * the first one has three arguments and the second one has two, the total
	 * number of lines generated will be three times two:
	 * <blockquote><pre>
	 * {1,2,3}{A,B} expands to 1A, 1B, 2A, 2B, 3A, 3B.
	 * </pre></blockquote>
	 * 
	 * <h2>Arguments separated by a double-period</h2>
	 * <p>
	 * This type of expression will be expanded to a list of comma separated
	 * arguments with an increasing or decreasing order, from the first integer
	 * or one-letter argument to the second one.
	 * <p>
	 * For an example, if the input contains two curly-braces expressions, where
	 * the first one are integers ten and twelve and the second are letters a
	 * and c, the total number of lines generated will be three times three:
	 * <blockquote><pre>
	 * {10..12}{a..c} expands to 10a, 10b, 10c, 11a, 11b, 11c, 12a, 12b, 12c.
	 * </pre></blockquote>
	 * 
	 * <h2>Combined input</h2>
	 * <p>
	 * Both types of expressions can be combined into a single curly-braces
	 * expression, as shown below:
	 * <blockquote><pre>
	 * {5..1}{ab,yz} expands to 5ab, 5yz, 4ab, 4yz, 3ab, 3yz, 2ab, 2yz, 1ab, 1yz
	 * </pre></blockquote>
	 * 
	 * @param env an environment
	 * @param input input to be expanded
	 * @return the specified input expanded as a brace expansion
	 */
	static List<String> braceExpansion(Environment env, String input) {
		final String OPENED = "{";
		final String CLOSED = "}";
		List<String> lines = new ArrayList<>();
		int i = input.length();
		
l:		while (true) {
			int start = input.lastIndexOf(OPENED, i);
			int insideStart = start+OPENED.length();
			
			int end   = input.indexOf(CLOSED, insideStart);
			int outsideEnd = end+CLOSED.length();
			if (start == -1 || end == -1) {
				if (lines.isEmpty()) {
					lines.add(input);
					return lines;
				}
				break;
			}
			
			i = start-1; // set i to the first place before {

			String match = input.substring(start, outsideEnd);
			String inside = input.substring(insideStart, end);
			if (inside.isEmpty() || StringHelper.isEscaped(input, start)) {
				continue;
			}

			String[] commaArgs = inside.split(",");
			List<String> currentLines = new ArrayList<>();
			
			for (String arg : commaArgs) {
				// Test if the argument is a double-period expansion
				String[] dotArgs = arg.split("\\.\\.");
				if (dotArgs.length == 2) {
					
					// Both are integers
					if (Helper.isLong(dotArgs[0]) && Helper.isLong(dotArgs[1])) {
						long l0 = Long.parseLong(dotArgs[0]);
						long l1 = Long.parseLong(dotArgs[1]);
						currentLines.addAll(loopThrough(l0, l1, input, match, start));
						
					// Both are characters
					} else if (dotArgs[0].length() == 1 && dotArgs[1].length() == 1) {
						char c0 = dotArgs[0].charAt(0);
						char c1 = dotArgs[1].charAt(0);
						currentLines.addAll(loopThrough(c0, c1, input, match, start));
					
					// Neither
					} else {
						// Invalid braces
						continue l;
					}
					
				} else {
					// This is the only argument in braces
					if (commaArgs.length == 1) {
						// Invalid braces
						continue l;
					}

					// There are more arguments in braces
					currentLines.add(StringHelper.replaceFirst(input, match, arg, start));
				}
			}

			// Braces are valid
			lines.addAll(currentLines);
			break;
		}
		
		/* Expand the remaining braces. */
		ListIterator<String> iterator = lines.listIterator();
		while (iterator.hasNext()) {
			String line = iterator.next();
			iterator.remove();
			
			List<String> l = braceExpansion(env, line);
			l.forEach(iterator::add);
		}
		
		return lines;
	}
	
	/**
	 * Loops from <tt>l0</tt> to <tt>l1</tt>, both inclusive and returns a list
	 * of <tt>source</tt> replacements with increasing or decreasing numbers.
	 * 
	 * @param l0 the starting long integer, inclusive
	 * @param l1 the ending long integer, inclusive
	 * @param source source string on which the replacement is made
	 * @param target the string to be replaced
	 * @param startIndex the index from which to start the replacement
	 * @return list of numbers included the given source string
	 */
	private static List<String> loopThrough(long l0, long l1, String source, String target, int startIndex) {
		List<String> list = new ArrayList<>();
		
		if (l0 < l1)
			for (long l = l0; l <= l1; l++) {
				list.add(StringHelper.replaceFirst(source, target, Long.toString(l), startIndex));
			}
		else
			for (long l = l0; l >= l1; l--) {
				list.add(StringHelper.replaceFirst(source, target, Long.toString(l), startIndex));
			}
		
		return list;
	}
	
	/**
	 * Loops from <tt>c0</tt> to <tt>c1</tt>, both inclusive and returns a list
	 * of <tt>source</tt> replacements with increasing or decreasing characters.
	 * 
	 * @param c0 the starting character, inclusive
	 * @param c1 the ending character, inclusive
	 * @param source source string on which the replacement is made
	 * @param target the string to be replaced
	 * @param startIndex the index from which to start the replacement
	 * @return list of characters included the given source string
	 */
	private static List<String> loopThrough(char c0, char c1, String source, String target, int startIndex) {
		List<String> list = new ArrayList<>();
		
		if (c0 < c1)
			for (char c = c0; c <= c1; c++) {
				list.add(StringHelper.replaceFirst(source, target, Character.toString(c), startIndex));
			}
		else
			for (char c = c0; c >= c1; c--) {
				list.add(StringHelper.replaceFirst(source, target, Character.toString(c), startIndex));
			}
		
		return list;
	}
	
	/**
	 * Expands the specified <tt>input</tt> as a special character expansion.
	 * This means that character sequences that match the special character
	 * sequences will be expanded into appropriate values. Escaping of all these
	 * sequences is supported.
	 * <p>
	 * The {@link #LAST_INPUT} sequence will be expanded to the last input that
	 * was given to the console which it stored in history.
	 * 
	 * @param env an environment
	 * @param input input to be expanded
	 * @return the specified input expanded as a special character expansion
	 */
	static String specialCharacterExpansion(Environment env, String input) {
		return StringHelper.replaceUnescaped(input, LAST_INPUT, Helper.lastElement(env.getHistory()));
		// Code reserved for future use.
//		int i = 0;
//		
//		while (true) {
//			int start = input.indexOf(LAST_INPUT, i);
//			int end = start + LAST_INPUT.length();
//			if (start == -1) break;
//			
//			i = end; // set i to the first character after LAST_INPUT
//
//			if (StringHelper.isEscaped(input, start)) {
//				continue;
//			}
//
//			String last = Helper.lastElement(env.getHistory());
//			input = StringHelper.replaceFirst(input, LAST_INPUT, last, start);
//			i -= StringHelper.lengthDifference(LAST_INPUT, last);
//		}
//		
//		return input;
	}
	
	/**
	 * Expands the specified <tt>input</tt> as a variable expansion. This means
	 * that character sequences preceded by a dollar symbol <tt>('$')</tt> (and
	 * optionally surrounded with curly braces to avoid conflicts) will be
	 * expanded into values of their variables.
	 * <p>
	 * While variables may have constraints on their names, this method does not
	 * take it into account as long as the variable name can be uniquely
	 * determined.
	 * 
	 * <h2>Uniquely determining variable name</h2>
	 * <p>
	 * The parameter name or symbol to be expanded may be enclosed in braces,
	 * which are optional but serve to protect the variable to be expanded from
	 * characters immediately following it which could be interpreted as part of
	 * the name. When braces are used, the matching ending brace is the first
	 * '}'.
	 * <p>
	 * The basic form of parameter expansion is '${PARAMETER}'. The value of
	 * 'PARAMETER' is substituted. The braces are required when 'PARAMETER' is a
	 * positional parameter with more than one digit, or when 'PARAMETER' is
	 * followed by a character that is not to be interpreted as part of its
	 * name.
	 * 
	 * <h2>Default variable values</h2>
	 * <p>
	 * There are different variants of variable expansion:
	 * <ul>
	 * <li>using a default value if the variable is not set:
	 * <strong><tt>${variable:-value}</tt></strong>
	 * <li>setting and using a default value if the variable is not set:
	 * <strong><tt>${variable:=value}</tt></strong>
	 * </ul>
	 * 
	 * @param env an environment
	 * @param input input to be expanded
	 * @return the specified input expanded as a variable expansion
	 */
	static String variableExpansion(Environment env, String input) {
		int i = 0;
		
		while (true) {
			// wrapped=${var}, unwrapped=$var
			boolean wrapped = false;
			
			int start = input.indexOf("$", i);
			if (start == -1) break;
			
			int end = StringHelper.indexOfNonIdentifier(input, start+1);
			if (end != -1 && input.charAt(end) == '{') {
				end = input.indexOf('}');
				wrapped = true;
			}
			if (end == -1) {
				end = input.length();
				wrapped = false;
			}
			
			i = end; // set i to the first non-identifier
			
			if (StringHelper.isEscaped(input, start)) {
				continue;
			}

			String varInput = input.substring(start+(wrapped? 2 : 1), end);
			String varName = varInput;
			String value = env.getVariable(varName);
			
			if (varInput.contains(":")) {
				String[] nameAndDefault = varInput.split(":", 2);
				String def = nameAndDefault[1];
				
				varName = nameAndDefault[0];
				value = env.getVariable(varName);

				if (value == null) {
					if (!StringHelper.isValidIdentifierName(varName)) {
						continue;
					}
					
					/* Set temporary value. */
					if (def.startsWith("-") || def.startsWith("=")) {
						value = def.substring(1);
					}
					/* Set environment variable. */
					if (def.startsWith("=")) {
						env.setVariable(varName, value);
					}
				}
			}
			
			if (value != null) {
				String target = wrapped ? "${"+varInput+"}" : "$"+varInput;
				input = StringHelper.replaceFirst(input, target, value, start);
				i -= StringHelper.lengthDifference(target, value);
			}
		}
		
		return input;
	}

	/**
	 * Expands the specified <tt>input</tt> as a command substitution expansion.
	 * This means that character sequences preceded by a dollar symbol
	 * <tt>('$')</tt> followed by and ending with single round parenthesis will
	 * be expanded into the result of their command standard output.
	 * <p>
	 * If a single token inside the command wrapper is invalid, or the
	 * expression is missing a closing tag, the whole command substitution is
	 * skipped and that part of input is left <em>as is</em>.
	 * 
	 * <h2>Definition</h2>
	 * <p>
	 * Command substitution allows the output of a command to replace the
	 * command itself. Command substitution occurs when a command is enclosed
	 * like this: <blockquote> <strong>$(command)</strong> </blockquote>
	 * 
	 * Shell performs the expansion by executing COMMAND and replacing the
	 * command substitution with the standard output of the command.
	 * 
	 * @param env an environment
	 * @param input input to be expanded
	 * @return the specified input expanded as a command substitution expansion
	 */
	static String commandSubstitutionExpansion(Environment env, String input) {
		final String OPENED = "$(";
		final String CLOSED = ")";
		int i = input.length();
		
		while (true) {
			int start = input.lastIndexOf(OPENED, i);
			if (start == -1) break;
			int insideStart = start+OPENED.length();
			
			int end = input.indexOf(CLOSED, insideStart);
			if (end == -1) break;
			int outsideEnd = end+CLOSED.length();

			i = start-1; // set i to the first place before $(
			
			String match = input.substring(start, outsideEnd);
			String inside = input.substring(insideStart, end);
			if (inside.isEmpty() || StringHelper.isEscaped(input, start)) {
				continue;
			}
			
			String result = executeCommand(env, inside);
			if (result == null) continue;
			
			input = StringHelper.replaceFirst(input, match, result, start);
		}
		
		return input;
	}
	
	/**
	 * Runs the command and returns its output. If the command specified in the
	 * beginning of <tt>line</tt> does not exist, <tt>null</tt> is returned.
	 * 
	 * @param env an environment
	 * @param line line containing the command name and arguments
	 * @return the command output or <tt>null</tt> if command does not exist
	 */
	private static String executeCommand(Environment env, String line) {
		line = line.trim();
		if (line.isEmpty()) {
			return null;
		}
		
		String cmd;
		String arg;
		int splitter = StringHelper.indexOfWhitespace(line);
		if (splitter != -1) {
			cmd = line.substring(0, splitter).toUpperCase();
			arg = line.substring(splitter+1).trim();
		} else {
			cmd = line.toUpperCase();
			arg = null;
		}

		ShellCommand command = MyShell.getCommand(cmd);
		if (command == null) {
			return null;
		}
		
		StringWriter sw = new StringWriter();
		env.push(null, sw);
		
		try {
			command.execute(env, arg);
			env.pop();
		} catch (Exception e) {
			env.pop();
			env.writeln("Error occurred while executing command:");
			env.writeln(command+": " + e.getMessage());
		}
		
		// Escape flag symbols
		return sw.toString();
	}
	
	/**
	 * Expands the specified <tt>input</tt> as an arithmetic expansion. This
	 * means that character sequences preceded by a dollar symbol <tt>('$')</tt>
	 * followed by and ending with double round parenthesis will be expanded
	 * into the result of their arithmetic expression.
	 * <p>
	 * If a single token inside the expression wrapper is invalid, or the
	 * expression is missing a closing tag, the whole expression is skipped and
	 * left <em>as is</em>.
	 * 
	 * <h2>Nesting arithmetic expansions</h2>
	 * <p>
	 * Nesting of arithmetic expansions is allowed. The nesting depth is much as
	 * the <tt>stack</tt> allows.
	 * 
	 * <h2>Supported arithmetic operations</h2>
	 * <p>
	 * There are a number of simple arithmetic operations supported for the
	 * expansion, as indicated below:
	 * <ul>
	 * <li>addition (+)
	 * <li>subtraction (-)
	 * <li>multiplication (*)
	 * <li>division (/)
	 * <li>modulus (%)
	 * <li>power (^)
	 * </ul>
	 * 
	 * @param env an environment
	 * @param input input to be expanded
	 * @return the specified input expanded as a variable expansion
	 */
	static String arithmeticExpansion(Environment env, String input) {
		final String OPENED = "$((";
		final String CLOSED = "))";
		int i = input.length();
		
		while (true) {
			int start = input.lastIndexOf(OPENED, i);
			if (start == -1) break;
			int insideStart = start+OPENED.length();
			
			int end = input.indexOf(CLOSED, insideStart);
			if (end == -1) break;
			int outsideEnd = end+CLOSED.length();

			i = start-1; // set i to the first place before $((
			
			String match = input.substring(start, outsideEnd);
			String inside = input.substring(insideStart, end);
			if (inside.isEmpty() || StringHelper.isEscaped(input, start)) {
				continue;
			}
			
			String result;
			try {
				double res = new DoubleEvaluator().evaluate(inside);
				result = nf.format(res);
			} catch (IllegalArgumentException e) {
				continue;
			}

			input = StringHelper.replaceFirst(input, match, result, start);
		}
		
		return input;
	}
	
}
