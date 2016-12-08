package hr.fer.zemris.java.tecaj.hw07.shell.commands;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.tecaj.hw07.shell.CommandStatus;
import hr.fer.zemris.java.tecaj.hw07.shell.Environment;
import hr.fer.zemris.java.tecaj.hw07.shell.Helper;

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
public class CopyCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command */
	private static final String SYNTAX = "copy <filename1> <filename2>";

	/**
	 * Constructs a new command object of type {@code CopyCommand}.
	 */
	public CopyCommand() {
		super("COPY", createCommandDescription());
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
	public CommandStatus execute(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		String[] args = Helper.extractArguments(s);
		if (args.length != 2) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		Path source = Helper.resolvePath(args[0]);
		Path dest = Helper.resolvePath(args[1]);
		if (source == null || dest == null) {
			writeln(env, "Invalid path!");
			return CommandStatus.CONTINUE;
		}
		
		copyFile(source, dest, env);
		
		return CommandStatus.CONTINUE;
	}

	/**
	 * Validates both paths and copies {@code src} to {@code dst}.
	 * 
	 * @param src the path to file to be copied
	 * @param dst the path to destination file or directory
	 * @param env an environment
	 */
	private void copyFile(Path src, Path dst, Environment env) {
		if (!Files.exists(src)) {
			writeln(env, "The system cannot find the file specified.");
			return;
		}
		if (Files.isDirectory(src)) {
			writeln(env, "Cannot copy directories using this command.");
			return;
		}
		if (src.equals(dst)) {
			writeln(env, "The file cannot be copied onto itself.");
		}
		if (Files.isDirectory(dst)) {
			createNewFile(src, dst.resolve(src.getFileName()), env);
		} else {
			createNewFile(src, dst, env);
		}
	}
	
	/**
	 * A file creator method. It copies the exact same contents from the
	 * <tt>src</tt> to the <tt>dst</tt>, creating a new file. This method also
	 * writes out the full path to the newly created file upon succeeding.
	 * <p>
	 * Implementation note: creates files using binary streams.
	 * 
	 * @param src an original file to be copied
	 * @param dst the destination directory
	 * @param env an environment
	 */
	private void createNewFile(Path src, Path dst, Environment env) {
		if (Files.exists(dst)) {
			writeln(env, "File " + dst + " already exists. Overwrite? Y/N");
			if (!overwrite(env)) {
				writeln(env, "Cancelled.");
				return;
			}
		}
		dst.getParent().toFile().mkdirs();
		
		try (
				BufferedInputStream in = new BufferedInputStream(Files.newInputStream(src));
				BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(dst));
		) {
			
			int len;
			byte[] buff = new byte[1024];
			while ((len = in.read(buff)) > 0) {
				out.write(buff, 0, len);
			}
			writeln(env, "Copied: " + dst);
			
		} catch (IOException e) {
			writeln(env, "An error occured during the copying of file " + src);
			writeln(env, e.getMessage());
		}
	}

	/**
	 * Prompts the user if he wants to overwrite a file. The user is expected to
	 * input <tt>Y</tt> as a <i>yes</i> or <tt>N</tt> as a <i>no</i>. Returns
	 * true if the user answered yes, false if no.
	 * 
	 * @param env an environment
	 * @return true if the user answered yes, false if no
	 */
	private boolean overwrite(Environment env) {
		while (true) {
			String line = readLine(env);
			
			if (line.equalsIgnoreCase("Y")) {
				return true;
			} else if (line.equalsIgnoreCase("N")) {
				return false;
			} else {
				write(env, "Please answer Y / N: ");
				continue;
			}
		}
	}

}
