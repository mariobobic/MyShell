package hr.fer.zemris.java.shell.commands.writing;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * A command that is used for shuffling file names. Argument must be an existing
 * directory.
 *
 * @author Mario Bobic
 */
public class NameShuffleCommand extends AbstractCommand {
	
	/** Prefix used for temporarily renaming files */
	private static final String RENAMING_PREFIX = "__temp-";

	/**
	 * Constructs a new command object of type {@code NameShuffleCommand}.
	 */
	public NameShuffleCommand() {
		super("NAMESHUFFLE", createCommandDescription());
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
		desc.add("Shuffles the file names of a specified directory.");
		desc.add("Argument must be an existing directory.");
		desc.add("BE CAREFUL WHILE USING THIS COMMAND!");
		return desc;
	}
	
	@Override
	protected CommandStatus execute0(Environment env, String s) {
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
		
		File dir = path.toFile();
		
		/* Create an original list of files and keep it for later printing */
		List<File> originalListOfFiles = Arrays.asList(dir.listFiles());
		
		/* Check if the directory was empty */
		if (originalListOfFiles.size() == 0) {
			writeln(env, "There are no files in the specified directory.");
			return CommandStatus.CONTINUE;
		}
		
		/* Create a list of file names and shuffle it */
		List<String> listOfFileNames = originalListOfFiles.stream()
				.map((file) -> {
					return file.getName();
				})
				.collect(Collectors.toList());
		
		Collections.shuffle(listOfFileNames);
		
		/* Temporarily rename all files */
		for (int i = 0, n = originalListOfFiles.size(); i < n; i++) {
			File tempFile = new File(dir, RENAMING_PREFIX + Integer.toString(i));
			originalListOfFiles.get(i).renameTo(tempFile);
		}
		
		/* Make a list of temp files */
		List<File> tempListOfFiles = Arrays.asList(dir.listFiles());
		
		/* Start shuffle-renaming */
		for (int i = 0, n = tempListOfFiles.size(); i < n; i++) {
			File originalFile = originalListOfFiles.get(i);
			File tempFile = tempListOfFiles.get(i);
			File renamingFile = new File(dir, listOfFileNames.get(i));
			
			tempFile.renameTo(renamingFile);
			
			writeln(env, originalFile.getName() + " renamed to " + renamingFile.getName());
		}
		
		return CommandStatus.CONTINUE;
	}

}
