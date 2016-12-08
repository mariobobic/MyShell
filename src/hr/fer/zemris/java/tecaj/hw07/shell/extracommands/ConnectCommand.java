package hr.fer.zemris.java.tecaj.hw07.shell.extracommands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import hr.fer.zemris.java.tecaj.hw07.shell.CommandStatus;
import hr.fer.zemris.java.tecaj.hw07.shell.Environment;
import hr.fer.zemris.java.tecaj.hw07.shell.commands.AbstractCommand;

/**
 * A command that is used for connecting to another computer running MyShell.
 * The other computer must have executed the {@linkplain HostCommand} in order
 * for this computer to connect to it.
 * <p>
 * <b>Note</b>: This command currently only works on computers that are
 * connected to the same LAN.
 *
 * @author Mario Bobic
 */
public class ConnectCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command */
	private static final String SYNTAX = "connect <host> <port>";

	/**
	 * Constructs a new command object of type {@code ConnectCommand}.
	 */
	public ConnectCommand() {
		super("CONNECT", createCommandDescription());
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
		desc.add("Connects to another computer running MyShell.");
		desc.add("The other computer must have executed the HOST command "
				+ "in order for this computer to connect to it.");
		desc.add("The expected syntax: " + SYNTAX);
		return desc;
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		String host;
		int port;
		try (Scanner sc = new Scanner(s)) {
			host = sc.next();
			port = sc.nextInt();
		} catch (Exception e) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		try (
				Socket clientSocket = new Socket(host, port);
				BufferedWriter outToServer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		) {
			/* Be careful not to close the System.in stream. */
			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
			
			String serverAddress = clientSocket.getRemoteSocketAddress().toString();
			env.writeln("Connected to " + serverAddress);
			
			Thread readingThread = new Thread(() -> {
				while (!Thread.interrupted()) {
					try {
						char[] cbuf = new char[1024];
						int len;
						while ((len = inFromServer.read(cbuf)) != -1)
							env.write(cbuf, 0, len);
					} catch (IOException e) {
						/* Do nothing here, it means the connection is closed. */
					} catch (Exception e) {
						writeln(env, e.getMessage());
					}
				}
			}, "Reading thread");
			readingThread.start();
			
			/* Upon quitting, "stop" the reading thread and close the socket. */
			while (true) {
				String userLine = inFromUser.readLine() + "\n";
				outToServer.write(userLine);
				outToServer.flush();
				if ("exit\n".equalsIgnoreCase(userLine)) {
					readingThread.interrupt();
					clientSocket.close();
					env.writeln("Disconnected from " + serverAddress);
					break;
				}
			}
		} catch (Exception e) {
			writeln(env, e.getMessage());
		}

		return CommandStatus.CONTINUE;
	}

}
