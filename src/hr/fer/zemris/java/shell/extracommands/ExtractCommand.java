package hr.fer.zemris.java.shell.extracommands;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * Extracts all files from all folders of a directory. After running this
 * command, all files will be in the path specified as an argument. All files in
 * subfolders are directly extracted to root folder. If a name collision occurs,
 * the second file is renamed to its parent folders plus its own name.
 *
 * @author Mario Bobic
 */
public class ExtractCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "extract <path>";
	
	/**
	 * Constructs a new command object of type {@code ExtractCommand}.
	 */
	public ExtractCommand() {
		super("EXTRACT", createCommandDescription());
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
		desc.add("Extracts all files from all folders of the specified directory.");
		desc.add("After running this command, all files will be in the specified path.");
		desc.add("Files in subfolders are directly extracted to root folder.");
		desc.add("If a name collision occurs, the second file is renamed to its parent folders.");
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
			writeln(env, "The system cannot find the path specified.");
			return CommandStatus.CONTINUE;
		}
		if (!Files.isDirectory(path)) {
			writeln(env, "The specified path must be a directory.");
			return CommandStatus.CONTINUE;
		}
		
		ExtractFileVisitor extractVisitor = new ExtractFileVisitor(env, path);
		Files.walkFileTree(path, extractVisitor);

		return CommandStatus.CONTINUE;
	}
	
	/**
	 * A {@linkplain SimpleFileVisitor} extended and used to serve the {@linkplain
	 * ExtractCommand}. Only the {@code visitFile} method is overridden.
	 *
	 * @author Mario Bobic
	 */
	private static class ExtractFileVisitor extends SimpleFileVisitor<Path> {
		
		/** An environment. */
		private Environment environment;
		/** Root directory. Files are extracted to this one. */
		private Path root;

		/**
		 * Initializes a new instance of this class setting the root directory
		 * and an environment used only for writing out messages.
		 * 
		 * @param environment an environment
		 * @param root root directory to which files are extracted
		 */
		public ExtractFileVisitor(Environment environment, Path root) {
			this.environment = environment;
			this.root = root;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Path target = root.resolve(file.getFileName());
			
			if (Files.exists(target)) {
				String relativized = root.relativize(file).toString();
				String newName = relativized.replace(File.separator, "_");
				target = root.resolve(newName);
			}

			if (!file.equals(target)) {
				Files.move(file, target);
				writeln(environment, "Extracted " + file);
		}
			
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (!dir.equals(root)) {
				Files.delete(dir);
			}
			
			return FileVisitResult.CONTINUE;
		}
		
	}

}
