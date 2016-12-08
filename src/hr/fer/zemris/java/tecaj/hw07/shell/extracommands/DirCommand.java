package hr.fer.zemris.java.tecaj.hw07.shell.extracommands;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hr.fer.zemris.java.tecaj.hw07.shell.CommandStatus;
import hr.fer.zemris.java.tecaj.hw07.shell.Environment;
import hr.fer.zemris.java.tecaj.hw07.shell.Helper;
import hr.fer.zemris.java.tecaj.hw07.shell.commands.AbstractCommand;

/**
 * A command that is used for writing out the current contents of a directory in
 * a Windows CMD-like environment. The specified argument must be an existing
 * directory path.
 * <p>
 * While listing directory's contents, this command also writes out if it
 * stumbled upon a file or a directory. In case of a file, this command writes
 * out the file's size in bytes.
 *
 * @author Mario Bobic
 */
public class DirCommand extends AbstractCommand {

	/** Defines the proper syntax for using this command */
	private static final String SYNTAX = "dir <path>";
	
	/** Date format used for formatting file date attribute. */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy.  HH:mm");
	/** Decimal format used for formatting file size attribute. */
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###,###.##");
	
	/**
	 * Constructs a new command object of type {@code LsCommand}.
	 */
	public DirCommand() {
		super("DIR", createCommandDescription());
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
		desc.add("Lists directory contents with a CMD-like environment.");
		desc.add("The expected syntax: " + SYNTAX);
		return desc;
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		Path dir = Helper.resolvePath(s);
		if (dir == null) {
			writeln(env, "Invalid path!");
			return CommandStatus.CONTINUE;
		}
		
		if (!Files.exists(dir)) {
			writeln(env, "The system cannot find the path specified.");
			return CommandStatus.CONTINUE;
		}
		if (!Files.isDirectory(dir)) {
			writeln(env, "The specified path must be a directory.");
			return CommandStatus.CONTINUE;
		}
		
		/* Passed all checks, start working. */
		writeln(env, " Directory of " + dir);
		writeln(env, "");

		File[] files = dir.toFile().listFiles();
		int noFiles = 0;
		int noDirs = 0;
		long filesLength = 0;
		
		for (File file : files) {
			String name = file.getName();
			Date date = new Date(file.lastModified());
			
			write(env, DATE_FORMAT.format(date) + "");
			if (file.isFile()) {
				noFiles++;
				long size = file.length();
				filesLength += size;
				write(env, String.format(" %17s ", DECIMAL_FORMAT.format(size)));
			} else {
				noDirs++;
				write(env, "    <DIR>          ");
			}
			writeln(env, name);			
		}
		writeln(env, String.format("%15d", noFiles) + " File(s), " + DECIMAL_FORMAT.format(filesLength) + " bytes");
		writeln(env, String.format("%15d", noDirs) + " Dir(s)");
		
		return CommandStatus.CONTINUE;
	}

}
