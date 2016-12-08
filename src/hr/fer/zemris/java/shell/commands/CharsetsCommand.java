package hr.fer.zemris.java.shell.commands;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * Lists names of supported charsets for the Java platform where it is executed.
 * This command takes no arguments and a single charset name is written per
 * line.
 *
 * @author Mario Bobic
 */
public class CharsetsCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "charsets";
	
	/**
	 * Constructs a new command object of type {@code CharsetsCommand}.
	 */
	public CharsetsCommand() {
		super("CHARSETS", createCommandDescription());
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
		desc.add("Lists names of supported charsets for the Java platform where it is executed.");
		desc.add("This command takes no arguments and a single charset name is written per line.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		Set<String> charsetNames = Charset.availableCharsets().keySet();
		
		for (String name : charsetNames) {
			writeln(env, name);
		}
		
		return CommandStatus.CONTINUE;
	}

}
