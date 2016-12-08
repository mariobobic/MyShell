package hr.fer.zemris.java.shell.extracommands;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A command that is used for changing the current working directory. The given
 * path may be absolute or relative to the current working directory. It can
 * also be inputed with quotation marks, which is widely used to interpret files
 * and directories containing whitespaces.
 *
 * @author Mario Bobic
 */
public class CdCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "cd (optional: <path>)";

	/**
	 * Constructs a new command object of type {@code CdCommand}.
	 */
	public CdCommand() {
		super("CD", createCommandDescription());
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
		desc.add("Changes the current directory.");
		desc.add("Use cd <path> to navigate to a certain path.");
		desc.add("Use cd to navigate to MyShell home directory.");
		desc.add("Use cd ~ to navigate to user home directory.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		if (s == null) {
			env.setCurrentPath(env.getHomePath());
			return CommandStatus.CONTINUE;
		}

		Path newPath = Helper.resolveAbsolutePath(env, s);
		if (newPath == null) {
			writeln(env, "Invalid path!");
			return CommandStatus.CONTINUE;
		}
		
		if (!newPath.toFile().exists()) {
			writeln(env, "The system cannot find the path specified.");
		} else {
			env.setCurrentPath(newPath);
		}

		return CommandStatus.CONTINUE;
	}

}
