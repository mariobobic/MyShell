package hr.fer.zemris.java.tecaj.hw07.shell.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.tecaj.hw07.shell.CommandStatus;
import hr.fer.zemris.java.tecaj.hw07.shell.Environment;
import hr.fer.zemris.java.tecaj.hw07.shell.Helper;

/**
 * A command that is used for creating directories.
 *
 * @author Mario Bobic
 */
public class MkdirCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command */
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
		return desc;
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		Path path = Helper.resolvePath(s);
		if (path == null) {
			writeln(env, "Invalid path!");
			return CommandStatus.CONTINUE;
		}
		
		if (Files.exists(path)) {
			writeln(env, path + " already exists!");
		} else {
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				writeln(env, "An error occured during creation of directory " + path);
				writeln(env, e.getMessage());
			}
			writeln(env, "Created " + path);
		}
		
		return CommandStatus.CONTINUE;
	}

}
