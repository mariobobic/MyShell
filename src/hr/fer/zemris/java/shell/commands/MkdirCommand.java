package hr.fer.zemris.java.shell.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A command that is used for creating directories.
 *
 * @author Mario Bobic
 */
public class MkdirCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "mkdir <path>";

	/**
	 * Constructs a new command object of type {@code MkdirCommand}.
	 */
	public MkdirCommand() {
		super("MKDIR", createCommandDescription());
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
		desc.add("Creates one or multiple directories.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		Path path = Helper.resolveAbsolutePath(env, s);
		
		if (Files.exists(path)) {
			writeln(env, path + " already exists!");
		} else {
			try {
				Files.createDirectories(path);
				writeln(env, "Created " + path);
			} catch (IOException e) {
				writeln(env, "Can not create directory " + path);
			}
		}
		
		return CommandStatus.CONTINUE;
	}

}
