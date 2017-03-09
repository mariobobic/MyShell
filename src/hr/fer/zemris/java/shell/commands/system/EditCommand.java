package hr.fer.zemris.java.shell.commands.system;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
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
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

/**
 * Launches the associated editor application to open the file specified as a
 * command argument. If the specified file has no application associated for
 * this operation, an error message is printed. Directories can not be edited.
 *
 * @author Mario Bobic
 */
public class EditCommand extends VisitorCommand {
	
	/* Flags */
	/** True if Sublime Text should be used for editing. */
	private boolean useSubl;

	/**
	 * Constructs a new command object of type {@code EditCommand}.
	 */
	public EditCommand() {
		super("EDIT", createCommandDescription(), createFlagDescriptions());
		commandArguments.addFlagDefinition("subl", false);
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
		desc.add("Launches the associated editor application to edit the file specified.");
		desc.add("If a file has no application associated for the edit operation, an error message is printed.");
		desc.add("If the specified path is a directory, its files are opened for editing.");
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
		desc.add(new FlagDescription("subl", null, null, "Open file in Sublime Text."));
		return desc;
	}
	
	@Override
	protected String compileFlags(Environment env, String s) {
		/* Initialize default values. */
		useSubl = false;

		/* Compile! */
		s = commandArguments.compile(s);
		
		/* Replace default values with flag values, if any. */
		if (commandArguments.containsFlag("subl")) {
			useSubl = true;
		}

		return super.compileFlags(env, s);
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			throw new SyntaxException();
		}
		
		Path path = Helper.resolveAbsolutePath(env, s);
		Helper.requireExists(path);
		
		/* Edit file(s). */
		EditFileVisitor editVisitor = new EditFileVisitor(env);
		walkFileTree(path, editVisitor);
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Launches the associated editor application and opens a file for editing.
	 * 
	 * @param env an environment
	 * @param file file to be opened for editing
	 * @throws IOException if an I/O error occurs while opening the file
	 */
	private void edit(Environment env, Path file) throws IOException {
		// TODO find a more universal way to run applications
		if (useSubl) {
			// Open file in Sublime Text
			new ProcessBuilder(
				"C:/Program Files (x86)/Sublime Text 3/sublime_text.exe",
				file.toString()
			).start();
			return;
		}
		
		try {
			// Does not work on Windows 2003 and XP
			Desktop.getDesktop().edit(file.toFile());
		} catch (IOException e) {
			write(env, e.getMessage());
			writeln(env, "Try opening the file using the open command.");
		}
	}
	
	/**
	 * A {@linkplain FileVisitor} implementation that is used to serve the
	 * {@linkplain EditCommand}.
	 *
	 * @author Mario Bobic
	 */
	private class EditFileVisitor extends SimpleFileVisitor<Path> {
		
		/** An environment. */
		private Environment environment;
		
		/**
		 * Constructs an instance of TreeFileVisitor with the specified
		 * <tt>environment</tt>.
		 * 
		 * @param environment an environment
		 */
		public EditFileVisitor(Environment environment) {
			this.environment = environment;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			edit(environment, file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			writeln(environment, "Failed to access " + file);
			return FileVisitResult.CONTINUE;
		}
		
	}

}
