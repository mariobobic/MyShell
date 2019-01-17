package hr.fer.zemris.java.shell.commands.network;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Connection;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.NetworkTransfer;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

/**
 * This command is paired with {@link HostCommand} and {@link ConnectCommand}
 * and is used for uploading content from the client to the host computer.
 *
 * @author Mario Bobic
 */
public class UploadCommand extends AbstractCommand {
	
	/**
	 * Constructs a new command object of type {@code UploadCommand}.
	 */
	public UploadCommand() {
		super("UPLOAD", createCommandDescription());
	}
	
	@Override
	public String getCommandSyntax() {
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
		desc.add("Uploads content to the host computer.");
		desc.add("This command can only be run when connected to a MyShell host.");
		desc.add("Both files and directories can be uploaded.");
		return desc;
	}

	@Override
	protected ShellStatus execute0(Environment env, String s) throws IOException {
		if (!env.isConnected()) {
			env.writeln("You must be connected to a host to run this command!");
			return ShellStatus.CONTINUE;
		}
		
		if (s == null) {
			throw new SyntaxException();
		}
		
		/* Passed all checks, good to go. */
		try {
			// Upload from MyShell CLIENT to MyShell HOST
			Connection con = env.getConnection();
			NetworkTransfer.requestDownload(env, s, con.getInFromClient(), con.getOutToClient(), con.getDecrypto());
		} catch (SocketException e) {
			// Connection has ended
			return ShellStatus.TERMINATE;
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
		return ShellStatus.CONTINUE;
	}
	
}
