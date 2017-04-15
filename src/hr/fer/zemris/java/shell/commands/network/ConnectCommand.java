package hr.fer.zemris.java.shell.commands.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import hr.fer.zemris.java.shell.MyShell;
import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.commands.system.ExitCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Crypto;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.NetworkTransfer;
import hr.fer.zemris.java.shell.utility.StringHelper;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

/**
 * A command that is used for connecting to another computer running MyShell.
 * The other computer must have executed the {@linkplain HostCommand} in order
 * for this computer to connect to it.
 *
 * @author Mario Bobic
 */
public class ConnectCommand extends AbstractCommand {
	
	/** Keyword used to execute commands locally. */
	private static final String LOCAL = "LOCAL:";
	
	/* Flags */
	/** Password hash for encrypted connection. */
	private String hash;
	/** Indicates if host-client connection should be reversed. */
	private boolean reverse;

	/**
	 * Constructs a new command object of type {@code ConnectCommand}.
	 */
	public ConnectCommand() {
		super("CONNECT", createCommandDescription(), createFlagDescriptions());
	}
	
	@Override
	public String getCommandSyntax() {
		return "<host> <port>";
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
		desc.add("To connect to server as a host, use the -r flag.");
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
		
		/* Read host and port. */
		String host;
		int port;
		try (Scanner sc = new Scanner(s)) {
			host = sc.next();
			port = sc.nextInt();
		} catch (Exception e) {
			throw new SyntaxException();
		}
		
		/* Do connect. */
		try (Socket clientSocket = new Socket(host, port)) {
			if (!reverse) {
				connect(env, clientSocket, s);
			} else {
				HostCommand.host(env, clientSocket, hash);
			}
		} catch (Exception e) {
			env.writeln(e.getMessage());
		}

		return ShellStatus.CONTINUE;
	}
	
	
	/**
	 * Connects this shell to a shell at the specified <tt>clientSocket</tt>.
	 * The <tt>hash</tt> parameter is used for encryption and decryption, and
	 * must be the same as host's in order to work.
	 * 
	 * @param env an environment
	 * @param clientSocket socket of the client
	 * @param hash encryption and decryption hash
	 * @throws IOException if an I/O error occurs
	 */
	static void connect(Environment env, Socket clientSocket, String hash) throws IOException {
		Thread readingThread = null;
		try (
			OutputStream outToServer = clientSocket.getOutputStream();
			InputStream inFromServer = clientSocket.getInputStream();
			BufferedWriter serverWriter = new BufferedWriter(new OutputStreamWriter(outToServer));
			BufferedReader serverReader = new BufferedReader(new InputStreamReader(inFromServer));
		) {
			/* System.in should not be closed. */
			BufferedReader inFromUser = Environment.stdIn;
			
			String serverAddress = clientSocket.getRemoteSocketAddress().toString();
			env.writeln("Connected to " + serverAddress);

			Crypto decrypto = new Crypto(hash, Crypto.DECRYPT);
			Crypto encrypto = new Crypto(hash, Crypto.ENCRYPT);
	
			/* Start a thread that reads from server. */
			readingThread = new Thread(() -> {
				while (!Thread.interrupted()) {
					try {
						int len;
						char[] cbuf = new char[1024];
						while ((len = serverReader.read(cbuf)) != -1) {
							if (NetworkTransfer.isAHint(cbuf, NetworkTransfer.DOWNLOAD_KEYWORD)) {
								NetworkTransfer.download(inFromServer, outToServer, decrypto);
							} else if (NetworkTransfer.isAHint(cbuf, NetworkTransfer.UPLOAD_KEYWORD)) {
								NetworkTransfer.processUploadRequest(inFromServer, outToServer, encrypto);
							} else {
								env.write(cbuf, 0, len);
							}
						}
					} catch (SocketException e) {
						// The connection has ended
						Thread.currentThread().interrupt();
						env.writeln("Host " + serverAddress + " has terminated connection.");
					} catch (IOException e) {
						env.writeln(e.getMessage());
					} catch (Exception e) {
						env.writeln(e.getMessage());
						e.printStackTrace();
					}
				}
			}, "Reading thread");
			readingThread.start();
			
			/* Accept input and write to server. */
			while (true) {
				String userLine = inFromUser.readLine();
				if (!readingThread.isAlive()) {
					break;
				}
				
				if (userLine.startsWith(LOCAL)) {
					localCommand(env, userLine.substring(6));
					continue;
				}
				
				serverWriter.write(userLine+"\n");
				serverWriter.flush();
				if (getStaticName(ExitCommand.class).equalsIgnoreCase(userLine)) {
					env.writeln("Disconnected from " + serverAddress);
					break;
				}
			}
		} finally {
			if (readingThread != null) readingThread.interrupt();
		}
	}
	
	/**
	 * Utility method to run commands locally on the client computer, instead of host's.
	 * While some Shell features won't work this way, most of them will.
	 * 
	 * @param env an environment
	 * @param line line containing the command name and arguments
	 */
	private static void localCommand(Environment env, String line) {
		line = line.trim();
		if (line.isEmpty()) {
			env.writeln("To execute a command locally, write the LOCAL keyword followed by a command.");
			return;
		}
		
		String cmd;
		String arg;
		int splitter = StringHelper.indexOfWhitespace(line);
		if (splitter != -1) {
			cmd = line.substring(0, splitter).toUpperCase();
			arg = line.substring(splitter+1).trim();
		} else {
			cmd = line.toUpperCase();
			arg = null;
		}

		try {
			MyShell.getCommand(cmd).execute(env, arg);
		} catch (Exception e) {
			env.writeln("Error occurred while executing local command:");
			env.writeln(e.getMessage());
		}
	}
	
}
