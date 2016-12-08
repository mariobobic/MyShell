package hr.fer.zemris.java.shell.extracommands;

import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A command that is used for printing the current working directory.
 *
 * @author Mario Bobic
 */
public class PwdCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command */
	private static final String SYNTAX = "pwd";
	
	/**
	 * Constructs a new command object of type {@code PwdCommand}.
	 */
	public PwdCommand() {
		super("PWD", createCommandDescription());
	}
	
	/**
	 * Creates a list of strings where each string represents a new line of this
	 * command's description. This method is generates description exclusively
	 * for the command that this class represents.
	 * 
	 * @return a list of strings that represents description
	 */
	private static List<String> createCommandDescription() {
		List<String> desc = new ArrayList<>();
		desc.add("Prints out the working directory (absolute directory path).");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		writeln(env, env.getCurrentPath().toString());
		return CommandStatus.CONTINUE;
	}

}
