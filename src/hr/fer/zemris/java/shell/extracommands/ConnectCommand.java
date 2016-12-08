package hr.fer.zemris.java.shell.extracommands;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

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
	
	/** Defines the proper syntax for using this command. */
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
		desc.add("Syntax: " + SYNTAX);
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
				OutputStream outToServer = clientSocket.getOutputStream();
				InputStream inFromServer = clientSocket.getInputStream();
				BufferedWriter serverWriter = new BufferedWriter(new OutputStreamWriter(outToServer));
				BufferedReader serverReader = new BufferedReader(new InputStreamReader(inFromServer));
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
						while ((len = serverReader.read(cbuf)) != -1) {
							if (isDownloadHint(cbuf)) {
								startDownload(env, inFromServer, outToServer);
							} else {
								env.write(cbuf, 0, len);
							}
						}
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
				serverWriter.write(userLine);
				serverWriter.flush();
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

	/**
	 * Returns true if contents of the specified char array <tt>cbuf</tt> match
	 * a download hint specified by the {@link Helper#DOWNLOAD_KEYWORD}.
	 * <p>
	 * The <tt>cbuf</tt> array is trimmed to the size of the keyword.
	 * 
	 * @param cbuf a char array buffer
	 * @return true if <tt>cbuf</tt> contains a download hint
	 */
	private boolean isDownloadHint(char[] cbuf) {
		char[] cbuf2 = Arrays.copyOf(cbuf, Helper.DOWNLOAD_KEYWORD.length);
		return Arrays.equals(cbuf2, Helper.DOWNLOAD_KEYWORD);
	}

	/**
	 * Starts the download process. This method blocks until the download is
	 * finished or an error occurs. The file is downloaded into user's
	 * <tt>home</tt> directory into the Downloads folder.
	 * 
	 * @param env an environment
	 * @param inFromServer input stream from the server
	 * @param outToServer output stream to the server
	 * @throws IOException if an I/O error occurs
	 */
	private static void startDownload(Environment env, InputStream inFromServer, OutputStream outToServer) throws IOException {
		byte[] bytes = new byte[1024];
		outToServer.write(0); // send a signal
		
		inFromServer.read(bytes);
		outToServer.write(0); // send a signal
		
		String filename = new String(bytes).trim();
		bytes = new byte[1024]; // reset array

		inFromServer.read(bytes);
		outToServer.write(0); // send a signal
		
		String filesize = new String(bytes).trim();
		long size = Long.parseLong(filesize);
		bytes = new byte[1024];  // reset array
		
		writeln(env, "Downloading " + filename + " (" + Helper.humanReadableByteCount(size) + ")");
		
		// Start downloading file
		BufferedInputStream fileInput = new BufferedInputStream(inFromServer);
		bytes = new byte[1024];
		long totalLen = 0;
		
		Path file = Paths.get(System.getProperty("user.home"), "Downloads", filename);
		Files.createDirectories(file.getParent());
		
		DownloadStatisticsTeller percentageTeller = new DownloadStatisticsTeller(env, size);
		ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		scheduledExecutor.scheduleWithFixedDelay(percentageTeller, 1, 5, TimeUnit.SECONDS);
		
		try (BufferedOutputStream fileOutput = new BufferedOutputStream(Files.newOutputStream(file))) {
			while (totalLen < size) {
				int len = fileInput.read(bytes);
				totalLen += len;
				if (totalLen > size) {
					len -= (totalLen - size);
					totalLen = size;
				}
				
				fileOutput.write(bytes, 0, len);
				percentageTeller.add(len);
			}
		}

		scheduledExecutor.shutdown();
		writeln(env, "Finished downloading "+filename
			+ " ("+Helper.humanReadableByteCount(size)+")");
	}
	
	/**
	 * A runnable job that tells the current downloaded percentage, download
	 * speed, elapsed time and estimated time until completion.
	 *
	 * @author Mario Bobic
	 */
	private static class DownloadStatisticsTeller implements Runnable {
		
		/** Time this job was constructed. */
		private final long startTime = System.nanoTime();
		
		/** An environment. */
		private Environment environment;
		/** Total size to be downloaded. */
		private final long size;
		/** Currently downloaded length. */
		private long downloadedLength;
		
		/** Total size to be downloaded converted to a human readable string. */
		private final String sizeStr;
		
		/**
		 * Constructs an instance of {@code DownloadStatisticsTeller} with the
		 * specified arguments.
		 *
		 * @param environment an environment
		 * @param size total size to be downloaded
		 */
		public DownloadStatisticsTeller(Environment environment, long size) {
			this.environment = environment;
			this.size = size;
			this.downloadedLength = 0;
			
			sizeStr = Helper.humanReadableByteCount(size);
		}
		
		/**
		 * Adds the specified <tt>length</tt> to the total downloaded length for
		 * calculating percentage.
		 * 
		 * @param length length to be added to the total downloaded length
		 */
		public void add(long length) {
			downloadedLength += length;
		}

		@Override
		public void run() {
			try {
				int percent = (int) (100 * downloadedLength / size);
				String downloaded = Helper.humanReadableByteCount(downloadedLength);
				
				long elapsedTime = System.nanoTime() - startTime;
				String elapsedTimeStr = Helper.humanReadableTimeUnit(elapsedTime);
				
				long downloadSpeed = downloadedLength / (elapsedTime/1_000_000_000L);
				String downloadSpeedStr = Helper.humanReadableByteCount(downloadSpeed) + "/s";
				
				String estimatedTime = Helper.humanReadableTimeUnit(1_000_000_000L * (size - downloadedLength) / downloadSpeed);
				
				writeln(environment, String.format(
					"%d%% downloaded (%s/%s), Elapsed time: %s, Download speed: %s, Estimated time: %s",
					percent, downloaded, sizeStr, elapsedTimeStr, downloadSpeedStr, estimatedTime
				));
				
//				writeln(environment, String.format("%d%% downloaded (%s/%s)", percent, downloaded, sizeStr));
//				writeln(environment, "   Elapsed time: " + elapsedTimeStr);
//				writeln(environment, "   Download speed: " + downloadSpeedStr);
//				writeln(environment, "   Estimated completion time: " + estimatedTime);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

}
