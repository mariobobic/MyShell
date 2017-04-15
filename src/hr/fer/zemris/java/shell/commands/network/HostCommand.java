package hr.fer.zemris.java.shell.commands.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.MyShell;
import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Crypto;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

/**
 * A command that is used for connecting to another computer running MyShell. In
 * order for another computer to connect to this host, the
 * {@linkplain ConnectCommand} must be executed on another computer with host's
 * IP address and port number.
 *
 * @author Mario Bobic
 */
public class HostCommand extends AbstractCommand {
	
	/* Flags */
	/** Password hash for encrypted connection. */
	private String hash;
	/** Indicates if host-client connection should be reversed. */
	private boolean reverse;

	/**
	 * Constructs a new command object of type {@code HostCommand}.
	 */
	public HostCommand() {
		super("HOST", createCommandDescription(), createFlagDescriptions());
	}
	
	@Override
	public String getCommandSyntax() {
		return "<port>";
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
		desc.add("Hosts this session with the given port number.");
		desc.add("In order for another computer to connect to host, it must be running MyShell. ");
		desc.add("The other computer must execute the CONNECT command with host IP and port.");
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
		desc.add(new FlagDescription("p", "pass", "pass", "Specify a connection password."));
		desc.add(new FlagDescription("r", "reverse", null, "Reverse host-client connection."));
		return desc;
	}
	
	@Override
	protected String compileFlags(Environment env, String s) {
		/* Initialize default values. */
		hash = Helper.generatePasswordHash("");
		reverse = false;

		/* Compile! */
		s = commandArguments.compile(s);
		
		/* Replace default values with flag values, if any. */
		if (commandArguments.containsFlag("p", "pass")) {
			hash = Helper.generatePasswordHash(
				commandArguments.getFlag("p", "pass").getArgument());
		}
		
		if (commandArguments.containsFlag("r", "reverse")) {
			reverse = true;
		}

		return super.compileFlags(env, s);
	}
	
	@Override
	protected ShellStatus execute0(Environment env, String s) {
		if (s == null) {
			throw new SyntaxException();
		}
		
		/* Parse the port number. */
		int port;
		try {
			port = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new SyntaxException();
		}
		
		/* Print out a message that the connection is ready. */
		env.write("Hosting server... connect to " + Helper.getLocalIP());
		env.writeln(" / " + Helper.getPublicIP());
		
		/* Create a host access point and redirect the input and output stream. */
		try (ServerSocket serverSocket = new ServerSocket(port);
			 Socket connectionSocket = serverSocket.accept()) {
			if (!reverse) {
				host(env, connectionSocket, hash);
			} else {
				ConnectCommand.connect(env, connectionSocket, hash);
			}
		} catch (Exception e) {
			env.writeln(e.getMessage());
		}
		
		return ShellStatus.CONTINUE;
	}

	/**
	 * Hosts this shell to a shell at the specified <tt>hostSocket</tt>. The
	 * <tt>hash</tt> parameter is used for encryption and decryption, and must
	 * be the same as client's in order to work.
	 * 
	 * @param env an environment
	 * @param hostSocket socket of the host
	 * @param hash encryption and decryption hash
	 * @throws IOException if an I/O error occurs
	 */
	static void host(Environment env, Socket hostSocket, String hash) throws IOException {
		String clientAddress = hostSocket.getRemoteSocketAddress().toString();
		env.writeln(clientAddress + " connected.");

		/* Redirect the streams to client. */
		Crypto encrypto = new Crypto(hash, Crypto.ENCRYPT);
		Crypto decrypto = new Crypto(hash, Crypto.DECRYPT);
		InputStream inFromClient = hostSocket.getInputStream();
		OutputStream outToClient = hostSocket.getOutputStream();
		env.getConnection().connectStreams(inFromClient, outToClient, encrypto, decrypto);

		/* Go to the main program and wait for the client to disconnect. */
		try {
			MyShell.main(null);
		} catch (Exception e) {}

		/* Redirect the streams back. */
		env.getConnection().disconnectStreams();
		env.writeln(clientAddress + " disconnected.");
	}

}
