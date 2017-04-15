package hr.fer.zemris.java.shell.commands.system;

import static hr.fer.zemris.java.shell.utility.CommandUtility.*;

import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.interfaces.ShellCommand;
import hr.fer.zemris.java.shell.utility.Helper;

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

	@Override
	public String getCommandSyntax() {
		return "(<command>)";
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
	protected ShellStatus execute0(Environment env, String s) {
		Iterable<ShellCommand> commands = env.commands();

		if (s == null) {
			printAllCommands(env, commands);
		} else {
			s = s.toUpperCase();
			printSpecifiedCommand(env, commands, s);
		}

		return ShellStatus.CONTINUE;
	}

	/**
	 * Writes out all the command names and the first line of their description.
	 * 
	 * @param env an environment
	 * @param commands this Shell's commands
	 */
	private static void printAllCommands(Environment env, Iterable<ShellCommand> commands) {
		int len = 0;
		for (ShellCommand command : commands) {
			len = Math.max(len, command.getCommandName().length());
		}
		len += 1;
		
		for (ShellCommand command : commands) {
			String name = command.getCommandName();
			String desc = Helper.firstElement(command.getCommandDescription());
			formatln(env, "%-"+len+"s %s", name+":", desc);
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
				printFullDescription(env, command);
				return;
			}
		}
		env.writeln("Cannot provide help for command: " + s);
	}
	
	/**
	 * Writes out the full description of the specified shell command, that is
	 * all description strings and flag descriptions of the command.
	 * 
	 * @param env environment to where write lines
	 * @param command command of which the description is to be printed
	 */
	private static void printFullDescription(Environment env, ShellCommand command) {
		for (String line : command.getCommandDescription()) {
			env.writeln(line);
		}
	}

}
