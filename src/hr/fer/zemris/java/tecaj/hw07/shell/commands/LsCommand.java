package hr.fer.zemris.java.tecaj.hw07.shell.commands;

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

import hr.fer.zemris.java.tecaj.hw07.shell.CommandStatus;
import hr.fer.zemris.java.tecaj.hw07.shell.Environment;
import hr.fer.zemris.java.tecaj.hw07.shell.Helper;

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

	/** Defines the proper syntax for using this command */
	private static final String SYNTAX = "ls <path>";
	
	/** Date format used for formatting file date attribute. */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
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
		try (Stream<Path> pathStream = Files.list(dir)) {
			pathStream.forEachOrdered((path) -> {
				printFile(env, path);
			});
		} catch (IOException e) {
			writeln(env, e.getMessage());
			return CommandStatus.CONTINUE;
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
	 */
	private static void printFile(Environment env, Path path) {
		try {
			/* First column */
			write(env, String.format("%c%c%c%c",
				Files.isDirectory(path)	? 'd' : '-',
				Files.isReadable(path)	? 'r' : '-',
				Files.isWritable(path)	? 'w' : '-',
				Files.isExecutable(path)? 'x' : '-'
			));
			
			/* Second column */
			write(env, String.format(" %10d" , Files.size(path)));
			
			/* Third column */
			BasicFileAttributeView faView = Files.getFileAttributeView(
					path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS
			);
			BasicFileAttributes attributes = faView.readAttributes();
			
			FileTime fileTime = attributes.creationTime();
			String formattedDateTime = DATE_FORMAT.format(new Date(fileTime.toMillis()));
			
			write(env, " " + formattedDateTime);
			
			/* Fourth column */
			writeln(env, " " + path.getFileName());
		} catch (IOException e) {
			writeln(env, "An I/O error has occured.");
			writeln(env, e.getMessage());
		}
	}

}
