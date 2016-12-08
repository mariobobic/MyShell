package hr.fer.oop.lab5.shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A command that is used for removing files and non-empty directories.
 *
 * @author Mario Bobic
 */
public class RmCommand extends AbstractCommand {

	/**
	 * Constructs a new command object of type {@code RmCommand}.
	 */
	public RmCommand() {
		super("RM", "Removes a file or a directory.");
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, "rm <arg>");
			return CommandStatus.CONTINUE;
		}
		
		Path path = Helper.resolveAbsolutePath(env, s);
		if (path == null) {
			env.writeln("Invalid path!");
			return CommandStatus.CONTINUE;
		}
		File file = path.toFile();

		if (file.exists()) {
			if (file.isDirectory()) {
				RmFileVisitor rmVisitor = new RmFileVisitor(env);
				try {
					Files.walkFileTree(file.toPath(), rmVisitor);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (file.delete() == true){
				env.writeln("Deleted file " + file.getName());
			} else {
				env.writeln("Failed to delete file " + file.getName());
			}
		} else {
			env.writeln("The system cannot find the file specified.");
		}
		return CommandStatus.CONTINUE;
	}

}
