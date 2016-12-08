package hr.fer.zemris.java.shell.extracommands;

import java.awt.Desktop;
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
 * Launches the associated application to open the file specified as a command
 * argument. If the specified file is a directory, the file manager of the
 * current platform is launched to open it.
 *
 * @author Mario Bobic
 */
public class OpenCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "open <file>";

	/**
	 * Constructs a new command object of type {@code OpenCommand}.
	 */
	public OpenCommand() {
		super("OPEN", createCommandDescription());
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
		desc.add("Launches the associated application to open the file specified.");
		desc.add("If the specified file is a directory, "
				+ "the file manager of the current platform is launched to open it.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		Path path = Helper.resolveAbsolutePath(env, s);
		if (!Files.exists(path)) {
			writeln(env, "The system cannot find the file specified.");
			return CommandStatus.CONTINUE;
		}
		
		Desktop.getDesktop().open(path.toFile());
		
		return CommandStatus.CONTINUE;
	}

}
