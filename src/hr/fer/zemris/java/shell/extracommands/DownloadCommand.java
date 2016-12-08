package hr.fer.zemris.java.shell.extracommands;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * This command is paired with {@link HostCommand} and {@link ConnectCommand}
 * and is used for downloading content from the host computer.
 *
 * @author Mario Bobic
 */
public class DownloadCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "download <filename> | download -<num>";
	
	/**
	 * Constructs a new command object of type {@code DownloadCommand}.
	 */
	public DownloadCommand() {
		super("DOWNLOAD", createCommandDescription());
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
		desc.add("Downloads content from the host's computer.");
		desc.add("This command can only be run when connected to a MyShell host.");
		desc.add("This command can be used in association with file-printing commands, "
				+ "which mark files with numbers to be used as a download reference.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		if (!env.isConnected()) {
			writeln(env, "You must be connected to a host to run this command!");
			return CommandStatus.CONTINUE;
		}
		
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		/* If the entered argument is parsable as an integer,
		 * see if a file is marked with that number. */
		Path path = null;
		if (Helper.isInteger(s)) {
			int num = Integer.parseInt(s);
			path = env.getConnection().getMarked(num);
		}
		
		/* If not, regularly check for the file. */
		if (path == null) {
			path = Helper.resolveAbsolutePath(env, s);
			if (path == null) {
				writeln(env, "Invalid path!");
				return CommandStatus.CONTINUE;
			}
		}

		if (!Files.exists(path)) {
			writeln(env, "The system cannot find the file specified.");
			return CommandStatus.CONTINUE;
		}
		
		if (Files.isDirectory(path)) {
			writeln(env, "Can not download directories (yet).");
			return CommandStatus.CONTINUE;
		}
		
		// Passed all checks, good to go
		OutputStream outToClient = env.getConnection().getOutToClient();
		InputStream inFromClient = env.getConnection().getInFromClient();
		
		try {
			// Send a hint that download has started
			byte[] start = new String(Helper.DOWNLOAD_KEYWORD).getBytes();
			outToClient.write(start);
			inFromClient.read(); // wait for signal
			
			// Send file name
			byte[] filename = path.getFileName().toString().getBytes();
			outToClient.write(filename);
			inFromClient.read(); // wait for signal
			
			// Send file size
			byte[] filesize = Long.toString(Files.size(path)).getBytes();
			outToClient.write(filesize);
			inFromClient.read(); // wait for signal
			
			// Start streaming file
			BufferedInputStream fileStream = new BufferedInputStream(Files.newInputStream(path));
			byte[] bytes = new byte[1024];
			int len;
			while ((len = fileStream.read(bytes)) != -1) {
				outToClient.write(bytes, 0, len);
			}
		} catch (IOException e) {
			writeln(env, "An error occured while downloading " + path);
			writeln(env, e.getMessage());
		}

		writeln(env, "Host ended uploading " + path);
		
		return CommandStatus.CONTINUE;
	}

}
