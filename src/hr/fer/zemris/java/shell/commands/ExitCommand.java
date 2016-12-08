package hr.fer.zemris.java.shell.commands;

import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A command that is used for terminating the Shell.
 *
 * @author Mario Bobic
 */
public class ExitCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "exit";

	/**
	 * Constructs a new command object of type {@code ExitCommand}.
	 */
	public ExitCommand() {
		super("EXIT", createCommandDescription());
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
		desc.add("Exits the MyShell program.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) {
		return CommandStatus.TERMINATE;
	}

}