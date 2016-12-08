package hr.fer.zemris.java.tecaj.hw07.shell.commands;

import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.tecaj.hw07.shell.CommandStatus;
import hr.fer.zemris.java.tecaj.hw07.shell.Environment;

/**
 * A command that is used for displaying all other commands, including this one,
 * and its descriptions. This command can also be used to display a description
 * for only a certain command, by passing the wanted command name as this
 * command's argument.
 *
 * @author Mario Bobic
 */
public class HelpCommand extends AbstractCommand {

	/**
	 * Constructs a new command object of type {@code HelpCommand}.
	 */
	public HelpCommand() {
		super("HELP", createCommandDescription());
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
		desc.add("Provides help information for MyShell commands.");
		desc.add("Commands are in sorted order where each command is given by name and description.");
		return desc;
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		Iterable<ShellCommand> commands = env.commands();

		if (s == null) {
			printAllCommands(env, commands);
		} else {
			s = s.toUpperCase();
			printSpecifiedCommand(env, commands, s);
		}

		return CommandStatus.CONTINUE;
	}

	/**
	 * Writes out all the command names and the first line of their description.
	 * This method expects all commands to have at least one line of
	 * description, else a {@linkplain IndexOutOfBoundsException} is thrown.
	 * 
	 * @param env an environment
	 * @param commands this Shell's commands
	 */
	private static void printAllCommands(Environment env, Iterable<ShellCommand> commands) {
		for (ShellCommand command : commands) {
			writeln(env, command.getCommandName() + ": " + command.getCommandDescription().get(0));
		}
	}

	/**
	 * Writes out all lines of description of a specified command.
	 * 
	 * @param env an environment
	 * @param commands supported MyShell commands
	 * @param s name of the specified command
	 */
	private static void printSpecifiedCommand(Environment env, Iterable<ShellCommand> commands, String s) {
		for (ShellCommand command : commands) {
			if (command.getCommandName().equals(s)) {
				printFullDescription(env, command.getCommandDescription());
				return;
			}
		}
		writeln(env, "Cannot provide help for command: " + s);
	}
	
	/**
	 * Writes out the full description by writing out all lines, that is all
	 * strings from the specified list <tt>desc</tt>.
	 * 
	 * @param env environment to where write lines
	 * @param desc list of lines representing a description
	 */
	private static void printFullDescription(Environment env, List<String> desc) {
		for (String line : desc) {
			writeln(env, line);
		}
	}

}
