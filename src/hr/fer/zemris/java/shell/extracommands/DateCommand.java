package hr.fer.zemris.java.shell.extracommands;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A simple command that is used for writing out the current date and time.
 *
 * @author Mario Bobic
 */
public class DateCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "date";
	
	/** Date format used for formatting date output. */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy. HH:mm:ss");

	/**
	 * Constructs a new command object of type {@code HelpCommand}.
	 */
	public DateCommand() {
		super("DATE", createCommandDescription());
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
		desc.add("Displays the current date.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		Date date = new Date();
		writeln(env, DATE_FORMAT.format(date));
		return CommandStatus.CONTINUE;
	}

}
