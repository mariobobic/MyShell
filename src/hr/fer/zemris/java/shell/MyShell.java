package hr.fer.zemris.java.shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import hr.fer.zemris.java.shell.commands.*;
import hr.fer.zemris.java.shell.extracommands.*;
import hr.fer.zemris.java.shell.interfaces.Connection;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.interfaces.ShellCommand;

/**
 * MyShell, this is where the magic happens. Scans the user's input and searches
 * for a matching command. Some commands require arguments, so the user must
 * input them as well. If the inputed command is found, the command is executed,
 * otherwise an error message is displayed. The program stops and prints out a
 * goodbye message if the inputed command is {@linkplain ExitCommand}. The
 * program also prompts an input symbol while waiting for a command to be
 * inputed. If a critical error occurs, an error message is printed out onto the
 * <b>standard error</b> with a detail message specifying what went wrong and
 * the program terminates.
 *
 * @author Mario Bobic
 * @author Marko Čupić
 * @version 2016-11-26 16:30
 */
public class MyShell {
	
	/** A map of commands. */
	private static Map<String, ShellCommand> commands;
	
	static {
		commands = new HashMap<>();
		ShellCommand[] cc = {
				new CatCommand(),
				new CharsetsCommand(),
				new CopyCommand(),
				new ExitCommand(),
				new HelpCommand(),
				new HexdumpCommand(),
				new LsCommand(),
				new MkdirCommand(),
				new SymbolCommand(),
				new TreeCommand(),

				new ByteShuffleCommand(),
				new CdCommand(),
				new DateCommand(),
				new DiffCommand(),
				new DirCommand(),
				new DumpCommand(),
				new ExtractCommand(),
				new FilterCommand(),
				new FindCommand(),
				new LargestCommand(),
				new NameShuffleCommand(),
				new PwdCommand(),
				new RenameAllCommand(),
				new RmCommand(),
				new RmdirCommand(),
				
				new HostCommand(),
				new ConnectCommand(),
				new DownloadCommand()
		};
		for (ShellCommand c : cc) {
			commands.put(c.getCommandName(), c);
		}
	}
	
	/** An environment used by MyShell. */
	private static EnvironmentImpl environment = new EnvironmentImpl();
	
	/**
	 * Program entry point.
	 * 
	 * @param args not used in this program
	 * @throws IOException
	 *             if an IO exception occurs while writing or reading the input.
	 *             This is a critical exception which terminates the program
	 *             violently.
	 */
	public static void main(String[] args) throws IOException {
		environment.writeln("Welcome to MyShell! You may enter commands.");
		
		while (true) {
			Path path = environment.getCurrentPath();
			Path pathName = path.equals(path.getRoot()) ? path : path.getFileName();
			environment.write("$" + pathName + environment.promptSymbol + " ");
			
			String line = readInput();
			
			String cmd;
			String arg;
			int splitter = indexOfWhitespace(line);
			if (splitter != -1) {
				cmd = line.substring(0, splitter).toUpperCase();
				arg = line.substring(splitter+1).trim();
			} else {
				cmd = line.toUpperCase();
				arg = null;
			}
			
			ShellCommand shellCommand = commands.get(cmd);
			if (shellCommand == null) {
				environment.writeln("Unknown command!");
				continue;
			}
			
			CommandStatus executionStatus;
			try {
				// If this machine is a host, write the command to its standard output
				if (environment.isConnected()) {
					System.out.println(""+path + environment.promptSymbol + " " + line);
				}
				executionStatus = shellCommand.execute(environment, arg);
			} catch (RuntimeException critical) {
				System.err.println("A critical error occured: " + critical.getMessage());
				System.err.println("Stack trace:");
				critical.printStackTrace();
				return;
			}
			
			if (executionStatus == CommandStatus.TERMINATE) {
				break;
			} else {
				environment.writeln("");
			}
		}
		
		environment.writeln("Thank you for using this shell. Goodbye!");
	}
	
	/**
	 * Reads the input from the current {@link EnvironmentImpl#reader reader}
	 * considering the {@link EnvironmentImpl#morelinesSymbol morelinesSymbol}
	 * as a line breaker for commands. The user may enter some command and its
	 * arguments followed by a space character and the <tt>morelinesSymbol</tt>,
	 * to which the Shell will respond with an appropriate
	 * {@link EnvironmentImpl#multilineSymbol multilineSymbol} as a multi-line
	 * prompt, and the whole input will be stored as a simple one-lined string.
	 * 
	 * @return the string input from the user
	 * @throws IOException if an I/O error occurs
	 */
	private static String readInput() throws IOException {
		String line = environment.readLine();
		if (line == null) {
			environment.writeln("Input unavailable. Closing MyShell...");
			System.exit(-1);
		} else {
			line = line.trim();
		}
		
		while (line.endsWith(" " + environment.morelinesSymbol)) {
			line = line.substring(0, line.length()-1); // remove the symbol
			
			environment.write(environment.multilineSymbol + " ");
			line += environment.readLine().trim();
		}
		
		return line;
	}
	
	/**
	 * Returns the index within the specified string <tt>str</tt> of the first
	 * occurrence of a whitespace character determined by the
	 * {@linkplain Character#isWhitespace(char)} method.
	 * 
	 * @param str string whose index of the first whitespace is to be returned
	 * @return the index of the first occurrence of a whitespace character
	 */
	private static int indexOfWhitespace(String str) {
		for (int i = 0, n = str.length(); i < n; i++) {
			if (Character.isWhitespace(str.charAt(i))) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * An environment implemented. Both reader and writer are implemented to
	 * work with the standard input and output.
	 *
	 * @author Mario Bobic
	 */
	public static class EnvironmentImpl implements Environment {
		
		/** A default reader that reads from the standard input. */
		private BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
		/** A default writer that writes on the standard output. */
		private BufferedWriter stdOut = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
		
		
		/** Reader of this environment. May be connected to a remote machine. */
		private BufferedReader reader = stdIn;
		/** Writer of this environment. May be connected to a remote machine. */
		private BufferedWriter writer = stdOut;
		
		/** Path where the program was ran */
		private Path homePath = Paths.get(".").normalize().toAbsolutePath();
		/** Current path of the user positioning */
		private Path currentPath = homePath;
		
		/** Prompt symbol to indicate the environment is ready. */
		private Character promptSymbol = '>';
		/** More lines symbol that is used for separating commands in multiple lines. */
		private Character morelinesSymbol = '\\';
		/** Multi-line symbol for writing commands in multiple lines. */
		private Character multilineSymbol = '|';
		
		/** Connection object that manages client-host connections. */
		private ConnectionImpl connection = new ConnectionImpl();
		
		@Override
		public String readLine() throws IOException {
			return reader.readLine();
		}
		
		@Override
		public void write(String s) throws IOException {
			writer.write(s);
			writer.flush();
		}
		
		@Override
		public void write(char cbuf[], int off, int len){
			try {
				writer.write(cbuf, off, len);
				writer.flush();
			} catch (IOException e) {}
		}
		
		@Override
		public void writeln(String s) throws IOException {
			write(s);
			writer.newLine();
			writer.flush();
		}
		
		@Override
		public Iterable<ShellCommand> commands() {
			return commands.values()
				.stream()
				.sorted((cmd1, cmd2) -> cmd1.getCommandName().compareTo(cmd2.getCommandName()))
				.collect(Collectors.toList());
		}
		
		@Override
		public Path getHomePath() {
			return homePath;
		}
		
		@Override
		public Path getCurrentPath() {
			return currentPath.toAbsolutePath();
		}
		
		@Override
		public void setCurrentPath(Path path) {
			currentPath = path;
			try {
				writeln("Current directory is now set to " + currentPath);
			} catch (IOException e) {}
		}
		
		@Override
		public Character getPromptSymbol() {
			return promptSymbol;
		}
		
		@Override
		public void setPromptSymbol(Character symbol) {
			promptSymbol = symbol;
		}
		
		@Override
		public Character getMorelinesSymbol() {
			return morelinesSymbol;
		}
		
		@Override
		public void setMorelinesSymbol(Character symbol) {
			morelinesSymbol = symbol;
		}
		
		@Override
		public Character getMultilineSymbol() {
			return multilineSymbol;
		}
		
		@Override
		public void setMultilineSymbol(Character symbol) {
			multilineSymbol = symbol;
		}
		
		@Override
		public Connection getConnection() {
			return connection;
		}
		
		@Override
		public boolean isConnected() {
			return connection.isConnected();
		}
		
	}
	
	/**
	 * A connection implemented. Contains {@code InputStream} and
	 * {@code OutputStream} objects that can be reassigned from standard input
	 * and output to the client. Contains a map that stores paths that were
	 * marked for download.
	 *
	 * @author Mario Bobic
	 */
	public static class ConnectionImpl implements Connection {
		
		/** Indicates if this machine has an active remote connection. */
		private boolean connected = false;
		/** Input stream to read from the client. */
		private InputStream inFromClient;
		/** Output stream to write to the client. */
		private OutputStream outToClient;
		
		/** Current ID number for marked paths. */
		private int markNum = 0;
		/** Map that associates paths ready for download with an ID number. */
		private Map<Integer, Path> markedForDownload = new HashMap<>();
		
		@Override
		public void connectStreams(InputStream in, OutputStream out) {
			inFromClient = in;
			outToClient = out;
			
			environment.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			environment.writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
			
			connected = true;
		}
		
		@Override
		public void disconnectStreams() {
			inFromClient = null;
			outToClient = null;
			
			environment.reader = environment.stdIn;
			environment.writer = environment.stdOut;

			connected = false;
		}
		
		@Override
		public boolean isConnected() {
			return connected;
		}
		
		@Override
		public InputStream getInFromClient() {
			return inFromClient;
		}
		
		@Override
		public OutputStream getOutToClient() {
			return outToClient;
		}

		@Override
		public int markForDownload(Path path) {
			markedForDownload.put(++markNum, path);
			return markNum;
		}

		@Override
		public void clearDownloadMarks() {
			markNum = 0;
			markedForDownload.clear();
		}
		
		@Override
		public Path getMarked(int num) {
			return markedForDownload.get(num);
		}
		
	}
	
}
