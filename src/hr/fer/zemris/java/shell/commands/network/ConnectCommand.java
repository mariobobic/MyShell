package hr.fer.zemris.java.shell.commands.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.crypto.BadPaddingException;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Crypto;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.Progress;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

/**
 * A command that is used for connecting to another computer running MyShell.
 * The other computer must have executed the {@linkplain HostCommand} in order
 * for this computer to connect to it.
 *
 * @author Mario Bobic
 */
public class ConnectCommand extends AbstractCommand {
	
	/* Flags */
	/** Password hash for encrypted connection. */
	private String hash;

	/**
	 * Constructs a new command object of type {@code ConnectCommand}.
	 */
	public ConnectCommand() {
		super("CONNECT", createCommandDescription(), createFlagDescriptions());
		commandArguments.addFlagDefinition("p", "pass", true);
	}
	
	@Override
	protected String getCommandSyntax() {
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

		return super.compileFlags(env, s);
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) {
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
		
		Crypto crypto = new Crypto(hash, Crypto.DECRYPT);
		
		/* Do connect. */
		Thread readingThread = null;
		try (
				Socket clientSocket = new Socket(host, port);
				OutputStream outToServer = clientSocket.getOutputStream();
				InputStream inFromServer = clientSocket.getInputStream();
				BufferedWriter serverWriter = new BufferedWriter(new OutputStreamWriter(outToServer));
				BufferedReader serverReader = new BufferedReader(new InputStreamReader(inFromServer));
		) {
			/* Be careful not to close the System.in stream. */
			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
			
			String serverAddress = clientSocket.getRemoteSocketAddress().toString();
			env.writeln("Connected to " + serverAddress);

			/* Start a thread that reads from server. */
			readingThread = new Thread(() -> {
				while (!Thread.interrupted()) {
					try {
						char[] cbuf = new char[1024];
						int len;
						while ((len = serverReader.read(cbuf)) != -1) {
							if (isAHint(cbuf, Helper.DOWNLOAD_KEYWORD)) {
								startDownload(env, inFromServer, outToServer, crypto);
							} else {
								env.write(cbuf, 0, len);
							}
						}
					} catch (IOException e) {
						/* Do nothing here, it usually means the connection is closed. */
					} catch (Exception e) {
						writeln(env, e.getMessage());
						e.printStackTrace();
					}
				}
			}, "Reading thread");
			readingThread.start();
			
			/* Accept input and write to server. */
			while (true) {
				String userLine = inFromUser.readLine();
				serverWriter.write(userLine+"\n");
				serverWriter.flush();
				if ("exit".equalsIgnoreCase(userLine)) {
					env.writeln("Disconnected from " + serverAddress);
					break;
				}
			}
		} catch (Exception e) {
			writeln(env, e.getMessage());
		} finally {
			if (readingThread != null) readingThread.interrupt();
		}

		return CommandStatus.CONTINUE;
	}

	/**
	 * Returns true if contents of the specified char array <tt>buffer</tt>
	 * match a hint specified by the <tt>keyword</tt>.
	 * <p>
	 * The <tt>buffer</tt> array is trimmed to the size of the keyword.
	 * 
	 * @param buffer a char array buffer
	 * @param keyword keyword to match
	 * @return true if buffer contains the keyword
	 */
	private static boolean isAHint(char[] buffer, char[] keyword) {
		char[] cbuf2 = Arrays.copyOf(buffer, keyword.length);
		return Arrays.equals(cbuf2, keyword);
	}

	/**
	 * Starts the download process. This method blocks until the download is
	 * finished or an error occurs. The file is downloaded into user's
	 * <tt>home</tt> directory into the Downloads folder.
	 * 
	 * @param env an environment
	 * @param inFromServer input stream from the server
	 * @param outToServer output stream to the server
	 * @param crypto cryptographic cipher for decrypting files
	 * @throws IOException if an I/O error occurs
	 */
	private static void startDownload(Environment env, InputStream inFromServer, OutputStream outToServer, Crypto crypto) throws IOException {
		//////////////////////////////////////////////////
		///////////////// File parameters ////////////////
		//////////////////////////////////////////////////
		
		// Accept download
		byte[] bytes = new byte[1024];
		outToServer.write(1); // send a signal: accepted download
		
		// Read file name
		inFromServer.read(bytes);
		outToServer.write(1); // send a signal: received file name
		
		String filename = new String(bytes).trim();
		bytes = new byte[1024]; // reset array
		
		// Read file size
		inFromServer.read(bytes);
		outToServer.write(1); // send a signal: received file size
		
		String filesize = new String(bytes).trim();
		long size = Long.parseLong(filesize);
		bytes = new byte[1024]; // reset array
		
		//////////////////////////////////////////////////
		////////////////////// Paths /////////////////////
		//////////////////////////////////////////////////
		
		// Create an appropriate directory structure
		Path path = Paths.get(System.getProperty("user.home"), "Downloads", filename);
		try {
			Files.createDirectories(path.getParent());
			outToServer.write(1); // send a signal: ready
		} catch (IOException e) {
			writeln(env, "Unable to create directory structure "+path+" because a file exists with the same name as one of the directories.");
			outToServer.write(0); // send a signal: not ready
			return;
		}
		
		// Do not overwrite, find first available file name
		path = Helper.firstAvailable(path);
		
		Path relativeName = Paths.get(filename).resolveSibling(path.getFileName());
		String nameAndSize = relativeName + " (" + Helper.humanReadableByteCount(size) + ")";
		writeln(env, "Downloading " + nameAndSize);
		
		//////////////////////////////////////////////////
		/////////////////// Downloading //////////////////
		//////////////////////////////////////////////////
		
		// Prepare download
		BufferedInputStream fileInput = new BufferedInputStream(inFromServer);
		bytes = new byte[1024];
		long totalLen = 0;
		
		// Start download
		Progress progress = new Progress(env, size, true);
		try (BufferedOutputStream fileOutput = new BufferedOutputStream(Files.newOutputStream(path))) {
			while (totalLen < size) {
				int len = fileInput.read(bytes);
				totalLen += len;
				if (totalLen > size) {
					len -= (totalLen - size);
					totalLen = size;
				}
				
				byte[] decrypted = crypto.update(bytes, 0, len);
				fileOutput.write(decrypted);
				progress.add(len);
			}
			fileOutput.write(crypto.doFinal());
		} catch (BadPaddingException e) {
			writeln(env, "An error occured while downloading " + path);
			writeln(env, "This is probably due to incorrect password.");
			// send a signal: download failed
			try { outToServer.write(0); } catch (Exception ex) {}
			throw new IOException(e);
		} catch (IOException e) {
			writeln(env, "An unexpected error occurred while downloading " + path);
			// send a signal: download failed
			try { outToServer.write(0); } catch (Exception ex) {}
			throw e;
		} finally {
			progress.stop();
		}
		
		outToServer.write(1); // send a signal: download done
		writeln(env, "Finished downloading " + nameAndSize);
	}
	
}
