package hr.fer.zemris.java.shell.commands;

import static hr.fer.zemris.java.shell.utility.CommandUtility.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import hr.fer.zemris.java.shell.MyShell;
import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.system.HelpCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.interfaces.ShellCommand;
import hr.fer.zemris.java.shell.utility.CommandArguments;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.exceptions.IllegalPathException;
import hr.fer.zemris.java.shell.utility.exceptions.InvalidFlagException;
import hr.fer.zemris.java.shell.utility.exceptions.NotEnoughDiskSpaceException;
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
	public AbstractCommand(String commandName, List<String> commandDescription) {
		this(commandName, commandDescription, new ArrayList<>());
	}
	
	/**
	 * Constructs a new command of a type extending {@code AbstractCommand}.
	 * 
	 * @param commandName name of this Shell command
	 * @param commandDescription description of this Shell command
	 * @param flagDescriptions flag descriptions of this Shell command
	 */
	public AbstractCommand(String commandName, List<String> commandDescription, List<FlagDescription> flagDescriptions) {
		this.commandArguments = new CommandArguments();
		
		this.commandName = commandName;
		this.flagDescriptions = flagDescriptions;
		this.commandDescription = mergeDescriptions(commandDescription, flagDescriptions);
		
		// Add the help flag support
		commandArguments.addFlagDefinition("help", "?", false);
		// Add the server environment flag support
		commandArguments.addFlagDefinition("server", false);
		
		// Add all flags this command defines
		for (FlagDescription desc : flagDescriptions) {
			String name = FlagDescription.unflag(desc.name);
			String otherName = FlagDescription.unflag(desc.otherName);
			commandArguments.addFlagDefinition(name, otherName, desc.argument != null);
		}
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
	 * Returns the static name of the specified class in uppercase letters. This
	 * method may not return the same result as the non-static method
	 * {@link #getCommandName()}.
	 * 
	 * @param clazz class of the command whose name is to be returned
	 * @return the static name of this class
	 */
	public static String getStaticName(Class<? extends ShellCommand> clazz) {
		return clazz.getSimpleName().replaceFirst("Command$", "").toUpperCase();
	}
	
	@Override
	public final ShellStatus execute(Environment env, String s) {
		try {
			s = compileFlags(env, s);
			if (commandArguments.containsFlag("help", "?")) {
				ShellCommand helpCmd = MyShell.getCommand(getStaticName(HelpCommand.class));
				return helpCmd.execute(env, commandName);
			}
			if (commandArguments.containsFlag("server") && env.isConnected()) {
				env.push(null, Environment.stdOut); // PUSH STDOUT
			}
			
			/* Execute this command. */
			return execute0(env, s);
			
		} catch (InvalidPathException e) {
			env.writeln("Invalid path: " + e.getInput());
		} catch (SyntaxException e) {
			printSyntaxError(env);
		} catch (InvalidFlagException e) {
			env.writeln(e.getMessage());
		} catch (IllegalPathException e) {
			env.writeln(e.getMessage());
		} catch (NotEnoughDiskSpaceException e) {
			env.writeln(e.getMessage());
		} catch (FileNotFoundException e) {
			env.writeln("File not found: " + e.getMessage());
		} catch (IOException e) {
			env.writeln("An I/O error occured: " + e.getMessage());
		} finally {
			if (commandArguments.containsFlag("server") && env.isConnected()) {
				env.pop(false); // POP STDOUT
			}
			commandArguments.clearFlags();
		}
		
//		env.writeln("");
		return ShellStatus.CONTINUE;
	}
	
	/**
	 * Executes the command exactly as the {@link #execute(Environment, String)}
	 * method would. This method serves so that its implementation is
	 * independent of {@code IOException}.
	 * 
	 * @param env an environment
	 * @param s arguments
	 * @return the status of this command
	 * @throws InvalidPathException if a path string cannot be converted to a
	 *         {@code Path}
	 * @throws SyntaxException if the argument <tt>s</tt> has an invalid syntax
	 * @throws InvalidFlagException if a flag found in the argument <tt>s</tt>
	 *         was not defined, or there are multiple one-letter flags where at
	 *         least one receives an argument or there is no argument provided
	 *         for a flag that is defined to require an argument
	 * @throws IllegalPathException if a path on the file system is required but
	 *         does not exist or a file is required but directory is given etc.
	 * @throws NotEnoughDiskSpaceException if there is not enough disk space for
	 *         an I/O operation
	 * @throws IOException if an I/O error occurs
	 */
	protected abstract ShellStatus execute0(Environment env, String s) throws IOException;
	
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
	public abstract String getCommandSyntax();

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
		String clean = commandArguments.compile(s);
		return clean == null ? null : clean.replaceAll("\\\\-", "-");
	}
	
	@Override
	public String toString() {
		return commandName;
	}

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
