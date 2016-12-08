package hr.fer.zemris.java.shell.extracommands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A command that is used for removing empty directories. In order to remove a
 * non-empty directory, use {@linkplain RmCommand}.
 *
 * @author Mario Bobic
 */
public class RmdirCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "rmdir <path>";

	/**
	 * Constructs a new command object of type {@code RmdirCommand}.
	 */
	public RmdirCommand() {
		super("RMDIR", createCommandDescription());
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
		desc.add("Removes a directory.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		Path dir = Helper.resolveAbsolutePath(env, s);
		
		if (!Files.isDirectory(dir)){
			writeln(env, dir.getFileName() + " is not a directory.");
		} else {
			try {
				Files.delete(dir);
				writeln(env, "Removed " + dir);
			} catch (IOException e) {
				writeln(env, "The directory must be empty in order to be removed. Use RM instead.");
			}
		}

		return CommandStatus.CONTINUE;
	}

}