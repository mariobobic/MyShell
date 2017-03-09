package hr.fer.zemris.java.shell.commands.system;

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
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Helper;

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
	
	/** Date format used for formatting file date attribute. */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/* Flags */
	/** Indicates if file sizes should be printed in human readable byte count. */
	private boolean humanReadable;
	/** Indicates if directory sizes should be calculated. */
	private boolean directorySize;
	
	/**
	 * Constructs a new command object of type {@code LsCommand}.
	 */
	public LsCommand() {
		super("LS", createCommandDescription(), createFlagDescriptions());
		commandArguments.addFlagDefinition("h", false);
		commandArguments.addFlagDefinition("d", false);
	}
	
	@Override
	protected String getCommandSyntax() {
		return "(<path>)";
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
		desc.add(new FlagDescription("h", null, null, "Print human readable sizes (e.g. 1kiB, 256MiB)."));
		desc.add(new FlagDescription("d", null, null, "Calculate size of directories (sum all file sizes)."));
		return desc;
	}
	
	@Override
	protected String compileFlags(Environment env, String s) {
		/* Initialize default values. */
		humanReadable = false;
		directorySize = false;

		/* Compile! */
		s = commandArguments.compile(s);
		
		/* Replace default values with flag values, if any. */
		if (commandArguments.containsFlag("h")) {
			humanReadable = true;
		}
		
		if (commandArguments.containsFlag("d")) {
			directorySize = true;
		}

		return super.compileFlags(env, s);
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		Path dir = s == null ?
			env.getCurrentPath() : Helper.resolveAbsolutePath(env, s);
		
		Helper.requireDirectory(dir);
		
		/* Clear previously marked paths. */
		env.clearMarks();
		
		/* Passed all checks, start working. */
		try (Stream<Path> pathStream = Files.list(dir)) {
			pathStream.forEachOrdered(file -> {
				printFile(env, file);
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
	 */
	private void printFile(Environment env, Path path) {
		try {
			write(env, getFileString(path, humanReadable, directorySize));
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
	 * @param directorySize if directory size should be calculated
	 * @return a string representation of a single file or directory
	 * @throws IOException if an I/O error occurs when reading the path
	 */
	public static String getFileString(Path path, boolean humanReadable, boolean directorySize) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		/* First column */
		sb.append(String.format("%c%c%c%c",
			Files.isDirectory(path)	? 'd' : '-',
			Files.isReadable(path)	? 'r' : '-',
			Files.isWritable(path)	? 'w' : '-',
			Files.isExecutable(path)? 'x' : '-'
		));
		
		/* Second column */
		long size = directorySize ? calculateSize(path) : Files.size(path);
		sb.append(!humanReadable ?
			String.format(" %11d" , size) :
			String.format(" %11s", Helper.humanReadableByteCount(size))
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
	
	/**
	 * Returns the size, in bytes, of the specified <tt>path</tt>. If the given
	 * path is a regular file, trivially its size is returned. Else the path is
	 * a directory and its contents are recursively explored, returning the
	 * total sum of all files within the directory.
	 * <p>
	 * If an I/O exception occurs, it is suppressed within this method and
	 * <tt>0</tt> is returned as the size of the specified <tt>path</tt>.
	 * 
	 * @param path path whose size is to be returned
	 * @return size of the specified path
	 */
	public static long calculateSize(Path path) {
		try {
			if (Files.isRegularFile(path)) {
				return Files.size(path);
			}
			
			return Files.list(path).mapToLong(LsCommand::calculateSize).sum();
		} catch (IOException e) {
			return 0L;
		}
	}

}
