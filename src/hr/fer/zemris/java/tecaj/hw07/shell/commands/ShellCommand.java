package hr.fer.zemris.java.tecaj.hw07.shell.commands;

import java.util.List;

import hr.fer.zemris.java.tecaj.hw07.shell.CommandStatus;
import hr.fer.zemris.java.tecaj.hw07.shell.Environment;

/**
 * An interface that is used as a contract for implementing Shell commands.
 *
 * @author Mario Bobic
 */
public interface ShellCommand {

	/**
	 * Returns the name of the Shell command.
	 * 
	 * @return the name of the Shell command
	 */
	public String getCommandName();
	
	/**
	 * Returns the description of the Shell command.
	 * 
	 * @return the description of the Shell command
	 */
	public List<String> getCommandDescription();
	
	/**
	 * Executes the given Shell command. Every shell command has its own unique
	 * way of executing. Most Shell commands write out their steps of executing,
	 * or they write out certain errors, so the {@code Environment} type param
	 * is given. Arguments may or may not exist (String has a valid value or
	 * {@code null}). The user is advised to check the implementing class
	 * documentation in order to see what this command does.
	 * 
	 * @param env an environment
	 * @param s arguments
	 * @return the status of this command
	 */
	public CommandStatus execute(Environment env, String s);
}
