package hr.fer.zemris.java.shell.commands.system;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

/**
 * Launches the associated application to open the file specified as a command
 * argument. If the specified file is a directory, the file manager of the
 * current platform is launched to open it.
 *
 * @author Mario Bobic
 */
public class OpenCommand extends AbstractCommand {

	/* Flags */
	/** Indicates if a random file should be opened. */
	private boolean random;
	
	/**
	 * Constructs a new command object of type {@code OpenCommand}.
	 */
	public OpenCommand() {
		super("OPEN", createCommandDescription(), createFlagDescriptions());
	}
	
	@Override
	public String getCommandSyntax() {
		return "<path> (<arguments>)";
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
		desc.add("If the specified path is a valid application, and if arguments "
				+ "were specified these arguments are passed to the process.");
		return desc;
	}
	
	/**
	 * Creates a list of {@code FlagDescription} objects where each entry
	 * describes the available flags of this command. This method is generates
	 * description exclusively for the command that this class represents.
	 * 
	 * @return a list of strings that represents description
	 */
	private static List<FlagDescription> createFlagDescriptions() {
		List<FlagDescription> desc = new ArrayList<>();
		desc.add(new FlagDescription("r", "random", null, "Open a random file in the specified directory."));
		return desc;
	}
	
	@Override
	protected String compileFlags(Environment env, String s) {
		/* Initialize default values. */
		random = false;

		/* Compile! */
		s = commandArguments.compile(s);

		/* Replace default values with flag values, if any. */
		if (commandArguments.containsFlag("r", "random")) {
			random = true;
		}
		
		return super.compileFlags(env, s);
	}

	@Override
	protected ShellStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			throw new SyntaxException();
		}
		
		// Fetch and check path
		String[] args = StringUtility.extractArguments(s);
		Path path = Utility.resolveAbsolutePath(env, args[0]);
		Utility.requireExists(path);

		// Pick a random element, if the flag is present
		if (random) {
			if (!Files.isDirectory(path)) {
				env.writeln("By using 'random' flag, the specified path must be a directory!");
				return ShellStatus.CONTINUE;
			}

			// TODO try using only stream API for getting a random element
			List<Path> files = Files.list(path).collect(Collectors.toList());
			if (files.isEmpty()) {
				env.writeln("Directory " + path + " is empty.");
				return ShellStatus.CONTINUE;
			}
			
			int index = ThreadLocalRandom.current().nextInt(files.size());
			path = files.get(index);
		}

		// Open the file (with possible arguments)
		try {
			args[0] = path.toString();
			new ProcessBuilder(args).start();
		} catch (IOException e) {
			Desktop.getDesktop().open(path.toFile());
		}
		
		return ShellStatus.CONTINUE;
	}

}
