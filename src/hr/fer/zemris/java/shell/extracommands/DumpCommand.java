package hr.fer.zemris.java.shell.extracommands;

import java.io.BufferedOutputStream;
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
 * Dumps empty bytes (zeroes) to the given file name. Number of dumped bytes is
 * user-defined. The user input can be given in any unit, with either standard
 * or digital prefixes.
 * <p>
 * <strong>Standard</strong> prefix means the unit 1000 is raised to the power
 * of the prefix, while <strong>digital</strong> prefix means the unit 1024 is
 * raised to the specified power. If the input is given without a unit, it is
 * considered as bytes.
 *
 * @author Mario Bobic
 */
public class DumpCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "dump <size> <filename>";
	
	/** Standard size for the loading byte buffer array */
	public static final int STD_LOADER_SIZE = 16*1024;

	/**
	 * Constructs a new command object of type {@code DumpCommand}.
	 */
	public DumpCommand() {
		super("DUMP", createCommandDescription());
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
		desc.add("Creates a new dump file with the given size and file name.");
		desc.add("Size can be given in any unit, with either standard (kB) or digital (kiB) prefixes.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}
	
	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		String[] args = Helper.extractArguments(s);
		
		/* Consider size having a space. */
		String sizeUnit;
		String pathname;
		if (args.length == 2) {
			sizeUnit = args[0];
			pathname = args[1];
		} else if (args.length == 3) {
			sizeUnit = args[0] + args[1];
			pathname = args[2];
		} else {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		long size;
		try {
			size = Helper.parseSize(sizeUnit);
		} catch (IllegalArgumentException e) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}

		Path path = Helper.resolveAbsolutePath(env, pathname);
		
		if (Files.isDirectory(path)) {
			writeln(env, "Directory " + path.getFileName() + " already exists.");
			return CommandStatus.CONTINUE;
		}
		
		if (Files.exists(path)) {
			if (!promptOverwrite(env, path)) {
				return CommandStatus.CONTINUE;
			}
		}
		
		dumpBytes(env, path, size);
		writeln(env, "Dumped " + Helper.humanReadableByteCount(size) + " in file " + path.getFileName());
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Prompts the user to overwrite the currently existing file. Returns true
	 * if the user answers "yes" and false if the user answers "no". Goes in an
	 * infinite loop if none of the answers are given.
	 * 
	 * @param env an environment
	 * @param file the currently existing file
	 * @return true if the user answers "yes", false if "no"
	 */
	private static boolean promptOverwrite(Environment env, Path file) {
		write(env, "File " + file.getFileName() + " already exists. Overwrite? (Y/N) ");
		while (true) {
			String answer = readLine(env);
			if ("y".equalsIgnoreCase(answer) || "yes".equalsIgnoreCase(answer)) {
				return true;
			} else if ("n".equalsIgnoreCase(answer) || "no".equalsIgnoreCase(answer)) {
				return false;
			} else {
				writeln(env, "Unknown answer: " + answer);
			}
		}
	}
	
	/**
	 * Dumps random bytes into the given {@code file} with the given
	 * {@code size}.
	 * 
	 * @param env an environment
	 * @param file file to be created
	 * @param size number of bytes to be generated
	 * @throws IOException if an I/O error occurs
	 */
	private static void dumpBytes(Environment env, Path file, long size) throws IOException {
		long writtenBytes = 0;
		
		try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(file))) {
			int len = STD_LOADER_SIZE;
			byte[] bytes = new byte[STD_LOADER_SIZE];

			while (writtenBytes < size) {
				if (size - writtenBytes < len) {
					len = (int) (size - writtenBytes);
				}
				out.write(bytes, 0, len);
				writtenBytes += len;
			}
		}
	}

}
