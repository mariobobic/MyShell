package hr.fer.zemris.java.tecaj.hw07.shell.commands;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import hr.fer.zemris.java.tecaj.hw07.shell.CommandStatus;
import hr.fer.zemris.java.tecaj.hw07.shell.Environment;

/**
 * Used as a superclass for other, usable Shell commands.
 * 
 * @author Mario Bobic
 */
public abstract class AbstractCommand implements ShellCommand {
	
	/** Name of the Shell command. */
	private String commandName;
	/** Description of the Shell command. */
	private List<String> commandDescription;

	/**
	 * Generates a new command of a type extending {@code AbstractCommand}.
	 * 
	 * @param commandName name of the Shell command
	 * @param commandDescription description of the Shell command
	 */
	protected AbstractCommand(String commandName, List<String> commandDescription) {
		this.commandName = commandName;
		this.commandDescription = commandDescription;
	}
	
	/**
	 * Writes out the syntax error of a command. Also shows what the command
	 * expected as arguments.
	 * 
	 * @param env an environment
	 * @param syntax the expected syntax
	 */
	protected static final void printSyntaxError(Environment env, String syntax) {
		writeln(env, "The syntax of the command is incorrect. Expected: " + syntax);
	}
	
	/**
	 * Reads the user's input from the specified enviroment <tt>env</tt> and
	 * returns it as a string. This method calls the
	 * {@link Environment#readLine() env.readLine()} method and throws a
	 * {@linkplain RuntimeException} if an I/O exception occurs.
	 * 
	 * @param env environment from where to read
	 * @return the user's input
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
	 * <tt>env</tt> without a newline separator. This method calls the
	 * {@link Environment#write(String) env.write(String)} method and throws a
	 * {@linkplain RuntimeException} if an I/O exception occurs.
	 * 
	 * @param env environment where to write
	 * @param s string to be written to the environment
	 */
	protected static final void write(Environment env, String s) {
		try {
			env.write(s);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Writes the specified string <tt>s</tt> to the specified environment
	 * <tt>env</tt> with a newline separator. This method calls the
	 * {@link Environment#writeln(String) env.writeln(String)} method and throws a
	 * {@linkplain RuntimeException} if an I/O exception occurs.
	 * 
	 * @param env environment where to write
	 * @param s string to be written to the environment
	 */
	protected static final void writeln(Environment env, String s) {
		try {
			env.writeln(s);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getCommandName() {
		return commandName;
	}

	@Override
	public List<String> getCommandDescription() {
		return Collections.unmodifiableList(commandDescription);
	}

	@Override
	public abstract CommandStatus execute(Environment env, String s);

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
		if (commandName == null) {
			if (other.commandName != null)
				return false;
		} else if (!commandName.equals(other.commandName))
			return false;
		return true;
	}

}
