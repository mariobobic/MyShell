package hr.fer.zemris.java.shell.extracommands;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A command that is used for renaming all files and directories with the path
 * and new name specified as command arguments.
 * <p>
 * If the new name is <tt>example</tt> the and there are 101 files to be
 * renamed, the renamed files would be:
 * <blockquote><pre>
 *    example000
 *    example001
 *    ...
 *    example099
 *    example100
 * </blockquote></pre>
 * A start index can also be specified for this command, as well as the position
 * of file index in its file name.
 *
 * @author Mario Bobic
 */
public class RenameAllCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "renameall <path> <newname> (optional: <startindex>)";

	/** Default numbering start index */
	private static final int DEF_START_INDEX = 0;
	
	/**
	 * Constructs a new command object of type {@code RenameAllCommand}.
	 */
	public RenameAllCommand() {
		super("RENAMEALL", createCommandDescription());
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
		desc.add("Renames all files and directories to the new name.");
		desc.add("Optional start index may be included.");
		desc.add("To position the file index in its name, write the {i} "
				+ "sequence which will be substituted by the index.");
		desc.add("To get the last index, use the {n} sequence.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}
	
	@Override
	protected CommandStatus execute0(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		String[] args = Helper.extractArguments(s);
		if (args.length < 2) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		Path path = Helper.resolveAbsolutePath(env, args[0]);
		if (!Files.exists(path) || !Files.isDirectory(path)) {
			writeln(env, "The system cannot find the path specified.");
			return CommandStatus.CONTINUE;
		}
		
		String name = args[1];
		
		int offset = DEF_START_INDEX;
		try {
			offset = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		} catch (IndexOutOfBoundsException ignorable) {}
		
		if (offset < 0) {
			writeln(env, "The start index must be positive: " + offset);
			return CommandStatus.CONTINUE;
		}

		File dir = path.toFile();
		
		/* Create a sorted list of files in the specified directory. */
		List<File> listOfFiles = Arrays.asList(dir.listFiles());
		Collections.sort(listOfFiles);
		
		/* Check if the directory was empty. */
		int n = listOfFiles.size();
		if (n == 0) {
			writeln(env, "There are no files in the specified directory.");
			return CommandStatus.CONTINUE;
		}
		
		/* All occurrences of * symbols will be replaced with file index. */
		name = name.replace("{n}", Integer.toString(n+offset-1));
		boolean containsSubs = name.contains("{i}");
		
		/* Rename all files. */
		for (int i = 0; i < n; i++) {
			int index = i + offset;
			String number = getLeadingZeros(n, offset, index) + index;
			String newName = containsSubs ? name.replace("{i}", number) : name+number;
			
			File originalFile = listOfFiles.get(i);
			File renamingFile = new File(dir, newName);
			
			boolean renamed = originalFile.renameTo(renamingFile);
			if (renamed) {
				writeln(env, originalFile.getName() + " renamed to " + renamingFile.getName());
			} else {
				writeln(env, originalFile.getName() + " cannot be renamed to " + newName);
			}
		}
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Returns a string of zeroes that should be leading the
	 * <code>currentIndex</code> in relation to <code>numOfFiles</code>.
	 * 
	 * @param numOfFiles total number of files
	 * @param offset start index a.k.a. index offset
	 * @param currentIndex index of the current processing object
	 * @return a string of leading zeroes
	 */
	private static String getLeadingZeros(int numOfFiles, int offset, int currentIndex) {
		int decimalPlaces = Integer.toString(numOfFiles+offset-1).length();
		int numZeroes = decimalPlaces - (Integer.toString(currentIndex).length() % 10);
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < numZeroes; i++) {
			sb.append('0');
		}
		return sb.toString();
	}

}
