package hr.fer.zemris.java.shell.commands.writing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * A command that is used for copying one file and only one file, to another
 * location. The given location must be an absolute path. The destination
 * directory may or may not exist before the copying is done. If the destination
 * directory does not exist, a corresponding directory structure is created. If
 * the last name in the pathname's name sequence is an existing directory, the
 * newly made file will be named as the original file. Else if the last name in
 * the pathname's name sequence is a non-existing directory (a file), the newly
 * made file will be named as it.
 *
 * @author Mario Bobic
 */
public class CopyCommand extends VisitorCommand {

	/**
	 * Constructs a new command object of type {@code CopyCommand}.
	 */
	public CopyCommand() {
		super("COPY", createCommandDescription());
	}
	
	@Override
	protected String getCommandSyntax() {
		return "<source_file> <target_path>";
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
		desc.add("Copies one file to another location.");
		desc.add("The first argument must be a source file to be copied, "
				+ "whereas the second argument may be either a file or a directory.");
		desc.add("If the second argument is not a directory, it means it is a new file name.");
		desc.add("If the second argument is a directory, a file with the same name is copied into it.");
		desc.add("The destination directory may or may not exist before the copying is done.");
		desc.add("If the destination directory does not exist, a corresponding directory structure is created.");
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			throw new SyntaxException();
		}
		
		String[] args = Helper.extractArguments(s);
		if (args.length != 2) {
			throw new SyntaxException();
		}
		
		Path source = Helper.resolveAbsolutePath(env, args[0]);
		Path target = Helper.resolveAbsolutePath(env, args[1]);
		Helper.requireExists(source);
		
		/* Both paths must be of same type. */
		if (Files.isDirectory(source) && Files.isRegularFile(target)) {
			writeln(env, "Can not copy directory onto a file.");
			return CommandStatus.CONTINUE;
		}
		
		/* Passed all checks, start working. */
		CopyFileVisitor copyVisitor = new CopyFileVisitor(env, source, target);
		walkFileTree(source, copyVisitor);
		
		return CommandStatus.CONTINUE;
	}

	/**
	 * Validates both paths and copies <tt>source</tt> to <tt>target</tt>. This
	 * method also writes out the full path to the newly created file upon
	 * succeeding.
	 * 
	 * @param source the path to file to be copied
	 * @param target the path to destination file or directory
	 * @param env an environment
	 */
	private static void copyFile(Path source, Path target, Environment env) {
		if (source.equals(target)) {
			writeln(env, "File cannot be copied onto itself: " + source);
			return;
		}
		
		if (Files.isDirectory(target)) {
			target = target.resolve(source.getFileName());
		}
		
		if (Files.exists(target)) {
			if (!promptConfirm(env, "File " + target + " already exists. Overwrite?")) {
				writeln(env, "Cancelled.");
				return;
			}
		}
		
		try {
			Files.createDirectories(target.getParent());
			createNewFile(source, target);
			writeln(env, "Copied: " + target);
		} catch (IOException e) {
			writeln(env, "Could not copy " + source + " to " + target);
		}
	}
	
	/**
	 * A file creator method. It copies the exact same contents from the
	 * <tt>source</tt> to the <tt>target</tt>, creating a new file. 
	 * <p>
	 * Implementation note: creates files using binary streams.
	 * 
	 * @param source an original file to be copied
	 * @param target the destination directory
	 * @throws IOException if an I/O error occurs
	 */
	private static void createNewFile(Path source, Path target) throws IOException {
		try (
				BufferedInputStream in = new BufferedInputStream(Files.newInputStream(source));
				BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(target));
		) {
			int len;
			byte[] buff = new byte[1024];
			while ((len = in.read(buff)) > 0) {
				out.write(buff, 0, len);
			}
		}
	}
	
	/**
	 * A {@linkplain FileVisitor} implementation that is used to serve the
	 * {@linkplain CopyCommand}.
	 *
	 * @author Mario Bobic
	 */
	private class CopyFileVisitor extends SimpleFileVisitor<Path> {
		
		/** An environment. */
		private Environment environment;
		
		/** This path's root directory. */
		private Path root;
		/** Other path's root directory. */
		private Path otherRoot;
		
		/**
		 * Constructs an instance of {@code CopyFileVisitor} with the specified
		 * arguments.
		 * 
		 * @param environment an environment
		 * @param root root directory of this file visitor
		 * @param otherRoot other path's root directory
		 */
		public CopyFileVisitor(Environment environment, Path root, Path otherRoot) {
			this.environment = environment;
			this.root = Files.isRegularFile(root) ? root.getParent() : root;
			this.otherRoot = Files.isRegularFile(otherRoot) ? otherRoot.getParent() : otherRoot;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Path relative = root.relativize(file);
			Path target = otherRoot.resolve(relative);
			
			copyFile(file, target, environment);
			
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			writeln(environment, "Failed to access " + file);
			return FileVisitResult.CONTINUE;
		}
	}

}
