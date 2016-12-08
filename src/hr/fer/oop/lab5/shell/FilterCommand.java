package hr.fer.oop.lab5.shell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A command that is used for filtering and writing out the absolute path of
 * files that match the given pattern. This search begins in the current working
 * directory and is going through all of the subdirectories.
 *
 * @author Mario Bobic
 */
public class FilterCommand extends AbstractCommand {

	/**
	 * Constructs a new command object of type {@code FilterCommand}.
	 */
	public FilterCommand() {
		super("FILTER", "Searches the current directory and all its subdirectories and displays the absolute path of files that match the given pattern.");
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, "filter <pattern>");
			return CommandStatus.CONTINUE;
		}
		
		FilterFileVisitor filterVisitor = new FilterFileVisitor(env, s);
		Path path = env.getCurrentPath();
		try {
			Files.walkFileTree(path, filterVisitor);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return CommandStatus.CONTINUE;
	}

}
