package hr.fer.zemris.java.shell.commands.network;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Crypto;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * This command is paired with {@link HostCommand} and {@link ConnectCommand}
 * and is used for downloading content from the host computer.
 *
 * @author Mario Bobic
 */
public class DownloadCommand extends AbstractCommand {
	
	/**
	 * Constructs a new command object of type {@code DownloadCommand}.
	 */
	public DownloadCommand() {
		super("DOWNLOAD", createCommandDescription());
	}
	
	@Override
	protected String getCommandSyntax() {
		return "<filename>";
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
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) {
		if (!env.isConnected()) {
			writeln(env, "You must be connected to a host to run this command!");
			return CommandStatus.CONTINUE;
		}
		
		if (s == null) {
			throw new SyntaxException();
		}
		
		Path path = Helper.resolveAbsolutePath(env, s);

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
			byte[] filesize = Long.toString(Crypto.postSize(path)).getBytes();
			outToClient.write(filesize);
			inFromClient.read(); // wait for signal
			
			// Start streaming file
			BufferedInputStream fileStream = new BufferedInputStream(Files.newInputStream(path));
			Crypto crypto = env.getConnection().getCrypto();
			byte[] bytes = new byte[1024];
			int len;
			while ((len = fileStream.read(bytes)) != -1) {
				byte[] encrypted = crypto.update(bytes, 0, len);
				outToClient.write(encrypted);
			}
			outToClient.write(crypto.doFinal());

			writeln(env, "Host ended uploading " + path);
		} catch (SocketException e) {
			return CommandStatus.TERMINATE; // client has ended connection
		} catch (IOException e) {
			writeln(env, "An error occured while downloading " + path);
			writeln(env, e.getMessage());
		} catch (BadPaddingException ignorable) {
			// ignored, since crypto is in encryption mode
		}
		
		return CommandStatus.CONTINUE;
	}

}
