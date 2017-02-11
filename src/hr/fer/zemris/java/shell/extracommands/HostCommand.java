package hr.fer.zemris.java.shell.extracommands;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Crypto;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.MyShell;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * A command that is used for connecting to another computer running MyShell. In
 * order for another computer to connect to this host, the
 * {@linkplain ConnectCommand} must be executed on another computer with host's
 * IP address and port number.
 *
 * @author Mario Bobic
 */
public class HostCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "host <port>";

	/**
	 * Constructs a new command object of type {@code HostCommand}.
	 */
	public HostCommand() {
		super("HOST", createCommandDescription());
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
		desc.add("In order for another computer to connect to host, "
				+ "it must be running MyShell and must have executed "
				+ "the CONNECT command with the host IP address and port number.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}
	
	@Override
	protected CommandStatus execute0(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		/* Parse the port number. */
		int port;
		try {
			port = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		/* Set the cryptographic cipher. */
		write(env, "Enter encryption password: ");
		String hash = Helper.generatePasswordHash(readLine(env));
		Crypto crypto = new Crypto(hash, Crypto.ENCRYPT);
		env.getConnection().setCrypto(crypto);
		
		/* Print out a message that the connection is ready. */
		write(env, "Hosting server... connect to " + Helper.getLocalIP());
		writeln(env, " / " + Helper.getPublicIP());
		
		/* Create a host access point and redirect the input and output stream. */
		try (
				ServerSocket serverSocket = new ServerSocket(port);
				Socket connectionSocket = serverSocket.accept();
				InputStream inFromClient = connectionSocket.getInputStream();
				OutputStream outToClient = connectionSocket.getOutputStream();
		){
			String clientAddress = connectionSocket.getRemoteSocketAddress().toString();
			env.writeln(clientAddress + " connected.");

			/* Redirect the streams to client. */
			env.getConnection().connectStreams(inFromClient, outToClient);

			/* Go to the main program and wait for the client to disconnect. */
			try {
				MyShell.main(null);
			} catch (Exception e) {}

			/* Redirect the streams back. */
			env.getConnection().disconnectStreams();
			env.writeln(clientAddress + " disconnected.");
		} catch (Exception e) {
			writeln(env, e.getMessage());
		}
		
		return CommandStatus.CONTINUE;
	}

}
