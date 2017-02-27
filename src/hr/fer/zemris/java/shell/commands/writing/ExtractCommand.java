package hr.fer.zemris.java.shell.commands.writing;

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
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * Extracts all files from all folders of a directory. After running this
 * command, all files will be in the path specified as an argument. All files in
 * subdirectories are directly extracted to root folder. If a name collision
 * occurs, the second file is renamed to its parent directories plus its own
 * name.
 *
 * @author Mario Bobic
 */
public class ExtractCommand extends VisitorCommand {
	
	/* Flags */
	/** True if empty directories should be removed after extracting. */
	private boolean removeDirectories;
	
	/**
	 * Constructs a new command object of type {@code ExtractCommand}.
	 */
	public ExtractCommand() {
		super("EXTRACT", createCommandDescription(), createFlagDescriptions());
		commandArguments.addFlagDefinition("r", false);
	}

	@Override
	protected String getCommandSyntax() {
		return "<path>";
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
		desc.add(new FlagDescription("r", null, null, "Remove empty directories after extracting."));
		return desc;
	}
	
	@Override
	protected String compileFlags(Environment env, String s) {
		/* Initialize default values. */
		removeDirectories = false;

		/* Compile! */
		s = commandArguments.compile(s);
		
		/* Replace default values with flag values, if any. */
		if (commandArguments.containsFlag("r")) {
			removeDirectories = true;
		}

		return super.compileFlags(env, s);
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			throw new SyntaxException();
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
	 * ExtractCommand}.
	 *
	 * @author Mario Bobic
	 */
	private class ExtractFileVisitor extends SimpleFileVisitor<Path> {
		
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
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (isExcluded(dir)) {
				return FileVisitResult.SKIP_SUBTREE;
			}

			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (isExcluded(file)) {
				return FileVisitResult.SKIP_SUBTREE;
			}
			
			if (file.getParent().equals(root)) {
				return FileVisitResult.CONTINUE;
			}
			
			Path target = root.resolve(file.getFileName());
			
			if (Files.exists(target)) {
				String relativized = root.relativize(file).toString();
				String newName = relativized.replace(File.separator, "_");
				target = Helper.firstAvailable(root.resolve(newName));
			}

			Files.move(file, target);
			writeln(environment, "Extracted " + file);
			
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			writeln(environment, "Unable to move " + file);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (!dir.equals(root) && removeDirectories) {
				try {
					Files.delete(dir);
				} catch (IOException e) {
					writeln(environment, "Unable to remove " + dir);
				}
			}
			
			return FileVisitResult.CONTINUE;
		}
		
	}

}
