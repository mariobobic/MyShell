package hr.fer.zemris.java.shell.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A command that is used for writing out the current contents of a directory.
 * The specified argument must be an existing directory path.
 * <p>
 * While listing directory's contents, alphabetically, this command also writes
 * the attributes of the file it encountered. All attributes will be written in
 * four of the following columns:
 * <ol>
 * <li>The first column indicates if current object is directory (d), readable
 * (r), writable (w) and executable (x).
 * <li>The second column contains object size in bytes that is right aligned and
 * occupies 10 characters.
 * <li>The third column shows file creation date and time with, where the date
 * format is specified by the {@linkplain #DATE_FORMAT}.
 * <li>The fourth column contains the name of the file or directory.
 * </ol>
 *
 * @author Mario Bobic
 */
public class LsCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "ls (-h) (<path>)";
	
	/** Date format used for formatting file date attribute. */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/** Indicates if file sizes should be printed in human readable byte count. */
	private boolean humanReadable;
	
	/**
	 * Constructs a new command object of type {@code LsCommand}.
	 */
	public LsCommand() {
		super("LS", createCommandDescription());
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
		desc.add("Lists directory contents.");
		desc.add("While listing directory's contents, alphabetically, "
				+ "this command also writes the attributes of the file it encountered.");
		desc.add("Use ls <path> to list contents of a certain directory.");
		desc.add("Use ls to list contents of the current directory.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		String[] args = Helper.extractArguments(s, 2);

		Path dir = env.getCurrentPath();
		humanReadable = false;
		if (args.length == 1) {
			if ("-h".equals(args[0])) humanReadable = true;
			else dir = Helper.resolveAbsolutePath(env, args[0]);
		} else if (args.length == 2) {
			if ("-h".equals(args[0])) {
				humanReadable = true;
				dir = Helper.resolveAbsolutePath(env, args[1]);
			} else {
				dir = Helper.resolveAbsolutePath(env, s);
			}
		}
		
		if (!Files.exists(dir)) {
			writeln(env, "The system cannot find the path specified: " + dir);
			return CommandStatus.CONTINUE;
		}
		if (!Files.isDirectory(dir)) {
			writeln(env, "The specified path must be a directory.");
			return CommandStatus.CONTINUE;
		}
		
		/* Clear previously marked paths. */
		env.clearMarks();
		
		/* Passed all checks, start working. */
		try (Stream<Path> pathStream = Files.list(dir)) {
			pathStream.forEachOrdered(file -> {
				printFile(env, file, humanReadable);
			});
		}
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Prints out a single file or directory specified by the <tt>path</tt> to
	 * the specified environment <tt>env</tt>. The path is printed in four
	 * columns:
	 * <ol>
	 * <li>The first column indicates if current object is directory (d),
	 * readable (r), writable (w) and executable (x).
	 * <li>The second column contains object size in bytes that is right aligned
	 * and occupies 10 characters.
	 * <li>The third column shows file creation date and time with, where the
	 * date format is specified by the {@linkplain #DATE_FORMAT}.
	 * <li>The fourth column contains the name of the file or directory.
	 * </ol>
	 * 
	 * @param env environment to where the path attributes are printed
	 * @param path path to be printed
	 * @param humanReadable if file size should be in human readable byte count
	 */
	private static void printFile(Environment env, Path path, boolean humanReadable) {
		try {
			write(env, getFileString(path, humanReadable));
			markAndPrintNumber(env, path);
		} catch (IOException e) {
			writeln(env, "An I/O error has occured.");
			writeln(env, e.getMessage());
		}
	}
	
	/**
	 * Returns a string representation of a single file or directory specified
	 * by the <tt>path</tt>. The path is written in four columns:
	 * <ol>
	 * <li>The first column indicates if current object is directory (d),
	 * readable (r), writable (w) and executable (x).
	 * <li>The second column contains object size in bytes that is right aligned
	 * and occupies 10 characters.
	 * <li>The third column shows file creation date and time with, where the
	 * date format is specified by the {@linkplain #DATE_FORMAT}.
	 * <li>The fourth column contains the name of the file or directory.
	 * </ol>
	 * 
	 * @param path path to be written
	 * @param humanReadable if file size should be in human readable byte count
	 * @return a string representation of a single file or directory
	 * @throws IOException if an I/O error occurs when reading the path
	 */
	public static String getFileString(Path path, boolean humanReadable) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		/* First column */
		sb.append(String.format("%c%c%c%c",
			Files.isDirectory(path)	? 'd' : '-',
			Files.isReadable(path)	? 'r' : '-',
			Files.isWritable(path)	? 'w' : '-',
			Files.isExecutable(path)? 'x' : '-'
		));
		
		/* Second column */
		long size = Files.size(path);
		sb.append(!humanReadable ?
			String.format(" %10d" , size) :
			String.format(" %10s", Helper.humanReadableByteCount(size))
		);
		
		/* Third column */
		BasicFileAttributeView faView = Files.getFileAttributeView(
				path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS
		);
		BasicFileAttributes attributes = faView.readAttributes();
		
		FileTime fileTime = attributes.creationTime();
		String formattedDateTime = DATE_FORMAT.format(new Date(fileTime.toMillis()));
		
		sb.append(" " + formattedDateTime);
		
		/* Fourth column */
		sb.append(" " + path.getFileName());
		
		return sb.toString();
	}

}
