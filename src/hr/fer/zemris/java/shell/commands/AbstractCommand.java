package hr.fer.zemris.java.shell.commands;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.system.HelpCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.interfaces.ShellCommand;
import hr.fer.zemris.java.shell.utility.CommandArguments;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.exceptions.IllegalPathException;
import hr.fer.zemris.java.shell.utility.exceptions.InvalidFlagException;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

/**
 * Used as a superclass for other, usable Shell commands.
 * 
 * @author Mario Bobic
 */
public abstract class AbstractCommand implements ShellCommand {
	
	/** Name of this Shell command. */
	private String commandName;
	/** Description of this Shell command. */
	private List<String> commandDescription;
	/** Flag descriptions of this Shell command. */
	@SuppressWarnings("unused")
	private List<FlagDescription> flagDescriptions;
	
	/** Command arguments and flags reader. */
	protected CommandArguments commandArguments;

	/**
	 * Constructs a new command of a type extending {@code AbstractCommand} with
	 * an empty list of flag descriptions.
	 * 
	 * @param commandName name of this Shell command
	 * @param commandDescription description of this Shell command
	 */
	protected AbstractCommand(String commandName, List<String> commandDescription) {
		this(commandName, commandDescription, new ArrayList<>());
	}
	
	/**
	 * Constructs a new command of a type extending {@code AbstractCommand}.
	 * 
	 * @param commandName name of this Shell command
	 * @param commandDescription description of this Shell command
	 * @param flagDescriptions flag descriptions of this Shell command
	 */
	protected AbstractCommand(String commandName, List<String> commandDescription, List<FlagDescription> flagDescriptions) {
		this.commandArguments = new CommandArguments();
		
		this.commandName = commandName;
		this.flagDescriptions = flagDescriptions;
		this.commandDescription = mergeDescriptions(commandDescription, flagDescriptions);
		
		// Add the help flag support
		this.commandArguments.addFlagDefinition("help", "?", false);
	}
	
	/**
	 * Modifies a list of strings where each string represents a new line of
	 * this command's description. Includes descriptions of flags if this
	 * command contains any.
	 * 
	 * @param commandDescription description of this Shell command
	 * @param flagDescriptions flag descriptions of this Shell command
	 * @return description of the command with flags descriptions included
	 */
	// TODO put command descriptions for all commands in a single .properties file?
	private List<String> mergeDescriptions(List<String> commandDescription, List<FlagDescription> flagDescriptions) {
		String usage = String.format("Usage: %s %s%s",
			getCommandName().toLowerCase(),
			flagDescriptions.isEmpty() ? "" : flagDescriptions+" ",
			getCommandSyntax()
		);
		
		List<String> desc = new ArrayList<>(commandDescription);
		desc.add(usage);
		
		if (!flagDescriptions.isEmpty()) {
			/* Get the maximum flag header length. */
			int len = flagDescriptions.stream()
				.map(FlagDescription::getHeader)
				.mapToInt(String::length)
				.max()
				.getAsInt();
			
			/* For each flag, add its header and description. */
			desc.add("");
			desc.add("Flags:");
			flagDescriptions.stream().sorted().forEach(flag -> {
				desc.add(String.format("   %-"+len+"s  %s", flag.getHeader(), flag.description));
			});
		}
		
		return Collections.unmodifiableList(desc);
	}
	
	/**
	 * Reads the user's input from the specified environment <tt>env</tt> and
	 * returns it as a string.
	 * <p>
	 * This method calls the {@link Environment#readLine() env.readLine()}
	 * method and throws a {@code RuntimeException} if an I/O exception occurs.
	 * 
	 * @param env environment from where to read
	 * @return the user's input
	 * @throws RuntimeException if an I/O exception occurs
	 */
	protected static final String readLine(Environment env) {
		try {
			return env.readLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Writes the specified string <tt>s</tt> to the specified environment
	 * <tt>env</tt> without a newline separator.
	 * <p>
	 * This method calls the {@link Environment#write(String) env.write(String)}
	 * method and throws a {@code RuntimeException} if an I/O exception occurs.
	 * 
	 * @param env environment where to write
	 * @param obj object to be written to the environment
	 * @throws RuntimeException if an I/O exception occurs
	 */
	protected static final void write(Environment env, Object obj) {
		try {
			env.write(String.valueOf(obj));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Writes the specified string <tt>s</tt> to the specified environment
	 * <tt>env</tt> with a newline separator.
	 * <p>
	 * This method calls the {@link Environment#writeln(String)
	 * env.writeln(String)} method and throws a {@code RuntimeException} if an
	 * I/O exception occurs.
	 * 
	 * @param env environment where to write
	 * @param obj object to be written to the environment
	 * @throws RuntimeException if an I/O exception occurs
	 */
	protected static final void writeln(Environment env, Object obj) {
		try {
			env.writeln(String.valueOf(obj));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Writes a formatted string to the specified environment <tt>env</tt> using
	 * the specified format string and arguments. If there are more arguments
	 * than format specifiers, the extra arguments are ignored. The number of
	 * arguments is variable and may be zero.
	 * <p>
	 * This method calls the {@link Environment#write(String) env.write(String)}
	 * method with a string returned by the
	 * {@link String#format(String, Object...)} method. Throws a
	 * {@code RuntimeException} if an I/O exception occurs.
	 * 
	 * @param env environment where to write
	 * @param format format string to be written
	 * @param args arguments referenced by the format specifiers in the format
	 *        string
	 * @throws RuntimeException if an I/O exception occurs
	 */
	protected static final void format(Environment env, String format, Object... args) {
		try {
			env.write(String.format(format, args));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Writes a formatted string to the specified environment <tt>env</tt> using
	 * the specified format string and arguments <strong>followed by a newline
	 * separator</strong>.
	 * 
	 * @param env environment where to write
	 * @param format format string to be written
	 * @param args arguments referenced by the format specifiers in the format
	 *        string
	 * @throws RuntimeException if an I/O exception occurs
	 * @see #format(Environment, String, Object...)
	 */
	public static final void formatln(Environment env, String format, Object... args) {
		format(env, format+"%n", args);
	}

	/**
	 * Prompts the user to confirm an action. The user is expected to input
	 * <tt>Y</tt> as a <i>yes</i> or <tt>N</tt> as a <i>no</i>. Returns true if
	 * the user answered yes, false if no.
	 * <p>
	 * This method blocks until the user answers yes or no.
	 * 
	 * @param env an environment
	 * @param message message to be written out before prompting
	 * @return true if the user answered yes, false if no
	 */
	public static boolean promptConfirm(Environment env, String message) {
		write(env, message + " (Y/N) ");
		while (true) {
			String line = readLine(env);
			
			if (line.equalsIgnoreCase("Y")) {
				return true;
			} else if (line.equalsIgnoreCase("N")) {
				return false;
			} else {
				write(env, "Please answer Y / N: ");
				continue;
			}
		}
	}
	
	/**
	 * Marks the specified path <tt>path</tt> and prints the full path name with
	 * its ID number. The printed string is followed by a newline separator.
	 * 
	 * @param env an environment
	 * @param path path to be marked and printed out
	 */
	protected static final void markAndPrintPath(Environment env, Path path) {
		write(env, path);
		markAndPrintNumber(env, path);
	}
	
	/**
	 * Marks the specified path <tt>path</tt> and prints its ID number. The
	 * printed string is followed by a newline separator.
	 * 
	 * @param env an environment
	 * @param path path to be marked
	 */
	protected static void markAndPrintNumber(Environment env, Path path) {
		int num = env.mark(path);
		writeln(env, " <" + num + ">");
	}
	
	/**
	 * Writes out a syntax error of this command, showing what the command
	 * actually expected as input arguments.
	 * 
	 * @param env an environment
	 */
	protected final void printSyntaxError(Environment env) {
		formatln(env, "The syntax of the command is incorrect. Expected: %s %s", 
			getCommandName().toLowerCase(), getCommandSyntax());
	}

	@Override
	public String getCommandName() {
		return commandName;
	}

	@Override
	public List<String> getCommandDescription() {
		return commandDescription;
	}
	
	/**
	 * Returns the syntax for proper usage of this Shell command.
	 * 
	 * @return the syntax of this Shell command
	 */
	protected abstract String getCommandSyntax();
	
	/**
	 * Compiles the specified input string <tt>s</tt> using this command's flags
	 * and returns a string cleared of flags. For more information, check the
	 * {@link CommandArguments#compile(String)} method.
	 * 
	 * @param env an environment
	 * @param s input string possibly containing flags
	 * @return the string <tt>s</tt> cleared of flags
	 */
	protected String compileFlags(Environment env, String s) {
		return commandArguments.compile(s);
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		CommandStatus status = CommandStatus.CONTINUE;
		
		try {
			s = compileFlags(env, s);
			
			if (commandArguments.containsFlag("help", "?")) {
				// TODO Don't access HelpCommand from AbstractCommand!
				HelpCommand.printFullDescription(env, this);
//				new HelpCommand().execute(env, commandName);
			} else {
				status = execute0(env, s);
			}
		} catch (IOException e) {
			writeln(env, "An I/O error occured:");
			writeln(env, e.getMessage());
		} catch (InvalidPathException e) {
			writeln(env, "Invalid path: " + e.getInput());
		} catch (SyntaxException e) {
			printSyntaxError(env);
		} catch (InvalidFlagException e) {
			writeln(env, e.getMessage());
		} catch (IllegalPathException e) {
			writeln(env, e.getMessage());
		}
		
		commandArguments.clearFlags();
		return status;
	}
	
	/**
	 * Executes the command exactly as the {@link #execute(Environment, String)}
	 * method would. This method serves so that its implementation is
	 * independent of {@code IOException}.
	 * 
	 * @param env an environment
	 * @param s arguments
	 * @return the status of this command
	 * @throws IOException if an I/O error occurs
	 * @throws InvalidPathException if a path string cannot be converted to a
	 *         {@code Path}
	 * @throws SyntaxException if the argument <tt>s</tt> has an invalid syntax
	 * @throws InvalidFlagException if a flag found in the argument <tt>s</tt>
	 *         was not defined, or there are multiple one-letter flags where at
	 *         least one receives an argument or there is no argument provided
	 *         for a flag that is defined to require an argument
	 * @throws IllegalPathException if a path on the file system is required but
	 *         does not exist or a file is required but directory is given etc.
	 */
	protected abstract CommandStatus execute0(Environment env, String s) throws IOException;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commandName == null) ? 0 : commandName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractCommand))
			return false;
		AbstractCommand other = (AbstractCommand) obj;
		return Objects.equals(commandName, other.commandName);
	}

}
