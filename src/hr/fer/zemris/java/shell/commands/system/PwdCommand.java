package hr.fer.zemris.java.shell.commands.system;

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
	
	/**
	 * Constructs a new command object of type {@code PwdCommand}.
	 */
	public PwdCommand() {
		super("PWD", createCommandDescription());
	}
	
	@Override
	protected String getCommandSyntax() {
		return "";
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
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) {
		writeln(env, env.getCurrentPath().toString());
		return CommandStatus.CONTINUE;
	}

}
