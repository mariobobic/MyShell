package hr.fer.zemris.java.shell.extracommands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
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
		
		/* Print out a message that the connection is ready. */
		write(env, "Hosting server... connect to " + getLocalIP());
		writeln(env, " / " + getPublicIP());
		
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

	/**
	 * Returns this computer's local IP address to which another user on the
	 * same LAN network may connect using the {@linkplain ConnectCommand}.
	 * Returns {@code null} if the IP address is inaccessible.
	 * 
	 * @return this computer's local IP address
	 */
	private static String getLocalIP() {
		try {
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
			while (en.hasMoreElements()) {
				NetworkInterface n = (NetworkInterface) en.nextElement();
				
				Enumeration<InetAddress> ee = n.getInetAddresses();
			    while (ee.hasMoreElements()) {
			        InetAddress addr = (InetAddress) ee.nextElement();
			        if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
			        	return addr.getHostAddress();
			        }
			    }
			}
		} catch (SocketException e) {}
		return null;
	}
	
	/**
	 * Returns the external IP address to which another user on the
	 * internet may connect using the {@linkplain ConnectCommand}.
	 * Returns {@code null} if the IP address is inaccessible.
	 * 
	 * @return this computer's local IP address
	 */
	private static String getPublicIP() {
		try {
			URL whatismyip = new URL("http://checkip.amazonaws.com");
			
			BufferedReader in = null;
			try {
				in = new BufferedReader(
					new InputStreamReader(whatismyip.openStream())
				);
				String ip = in.readLine();
				return ip;
			} finally {
				if (in != null) {
					try { in.close(); } catch (IOException e) {}
				}
			}
		} catch (IOException e) {
			return null;
		}
    }

}
