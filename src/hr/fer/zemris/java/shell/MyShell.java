package hr.fer.zemris.java.shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.commands.listing.CountCommand;
import hr.fer.zemris.java.shell.commands.listing.DiffCommand;
import hr.fer.zemris.java.shell.commands.listing.FilterCommand;
import hr.fer.zemris.java.shell.commands.listing.FindCommand;
import hr.fer.zemris.java.shell.commands.listing.ShowCommand;
import hr.fer.zemris.java.shell.commands.listing.TreeCommand;
import hr.fer.zemris.java.shell.commands.network.ConnectCommand;
import hr.fer.zemris.java.shell.commands.network.DownloadCommand;
import hr.fer.zemris.java.shell.commands.network.HostCommand;
import hr.fer.zemris.java.shell.commands.network.HttpServerCommand;
import hr.fer.zemris.java.shell.commands.network.PingCommand;
import hr.fer.zemris.java.shell.commands.reading.CatCommand;
import hr.fer.zemris.java.shell.commands.reading.HexdumpCommand;
import hr.fer.zemris.java.shell.commands.system.CdCommand;
import hr.fer.zemris.java.shell.commands.system.CharsetsCommand;
import hr.fer.zemris.java.shell.commands.system.ClearCommand;
import hr.fer.zemris.java.shell.commands.system.DateCommand;
import hr.fer.zemris.java.shell.commands.system.DirCommand;
import hr.fer.zemris.java.shell.commands.system.EditCommand;
import hr.fer.zemris.java.shell.commands.system.ExitCommand;
import hr.fer.zemris.java.shell.commands.system.HelpCommand;
import hr.fer.zemris.java.shell.commands.system.LsCommand;
import hr.fer.zemris.java.shell.commands.system.OpenCommand;
import hr.fer.zemris.java.shell.commands.system.PwdCommand;
import hr.fer.zemris.java.shell.commands.system.SymbolCommand;
import hr.fer.zemris.java.shell.commands.writing.ByteShuffleCommand;
import hr.fer.zemris.java.shell.commands.writing.CopyCommand;
import hr.fer.zemris.java.shell.commands.writing.DecryptCommand;
import hr.fer.zemris.java.shell.commands.writing.DumpCommand;
import hr.fer.zemris.java.shell.commands.writing.EncryptCommand;
import hr.fer.zemris.java.shell.commands.writing.ExtractCommand;
import hr.fer.zemris.java.shell.commands.writing.MkdirCommand;
import hr.fer.zemris.java.shell.commands.writing.MoveCommand;
import hr.fer.zemris.java.shell.commands.writing.NameShuffleCommand;
import hr.fer.zemris.java.shell.commands.writing.RenameAllCommand;
import hr.fer.zemris.java.shell.commands.writing.ReplaceCommand;
import hr.fer.zemris.java.shell.commands.writing.RmCommand;
import hr.fer.zemris.java.shell.commands.writing.RmdirCommand;
import hr.fer.zemris.java.shell.commands.writing.TouchCommand;
import hr.fer.zemris.java.shell.commands.writing.ZipCommand;
import hr.fer.zemris.java.shell.interfaces.Connection;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.interfaces.ShellCommand;
import hr.fer.zemris.java.shell.utility.Crypto;
import hr.fer.zemris.java.shell.utility.Helper;

/**
 * MyShell, this is where the magic happens. Scans the user's input and searches
 * for a matching command. Some commands require arguments, so the user must
 * input them as well. If the input command is found, the command is executed,
 * otherwise an error message is displayed. The program stops and prints out a
 * goodbye message if the input command is {@linkplain ExitCommand}. The program
 * also prompts an input symbol while waiting for a command to be inputed. If a
 * critical error occurs, an error message is printed out onto the <b>standard
 * error</b> with a detail message specifying what went wrong and the program
 * terminates.
 *
 * @author Mario Bobic
 * @author Marko Čupić
 * @version PC
 */
public class MyShell {
	
	/** A map of commands. */
	private static Map<String, ShellCommand> commands;
	
	static {
		commands = new HashMap<>();
		ShellCommand[] cc = {
				/* Listing */
				new CountCommand(),
				new DiffCommand(),
				new FilterCommand(),
				new FindCommand(),
				new ShowCommand(),
				new TreeCommand(),
				
				/* Network */
				new ConnectCommand(),
				new DownloadCommand(),
				new HostCommand(),
				new HttpServerCommand(),
				new PingCommand(),
				
				/* Reading */
				new CatCommand(),
				new HexdumpCommand(),
				
				/* System */
				new CdCommand(),
				new CharsetsCommand(),
				new ClearCommand(),
				new DateCommand(),
				new DirCommand(),
				new EditCommand(),
				new ExitCommand(),
				new HelpCommand(),
				new LsCommand(),
				new OpenCommand(),
				new PwdCommand(),
				new SymbolCommand(),
				
				/* Writing */
				new ByteShuffleCommand(),
				new CopyCommand(),
				new DecryptCommand(),
				new DumpCommand(),
				new EncryptCommand(),
				new ExtractCommand(),
				new MkdirCommand(),
				new MoveCommand(),
				new NameShuffleCommand(),
				new RenameAllCommand(),
				new ReplaceCommand(),
				new RmCommand(),
				new RmdirCommand(),
				new TouchCommand(),
				new ZipCommand(),
		};
		
		for (ShellCommand c : cc) {
			commands.put(c.getCommandName(), c);
		}
	}
	
	/** An environment used by MyShell. */
	private static EnvironmentImpl environment = new EnvironmentImpl();

	/** Writer saved before redirecting output to a file. */
	private static BufferedWriter lastWriter;
	
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
			Path pathName = Helper.getFileName(path);
			environment.write("$" + pathName + environment.promptSymbol + " ");
			
			String line = readInput();
			
			String cmd;
			String arg;
			int splitter = Helper.indexOfWhitespace(line);
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
			
			// If this machine is a host, write the command to its standard output
			if (environment.isConnected()) {
				System.out.println(""+path + environment.promptSymbol + " " + line);
			}
			
			try {
				arg = checkEnvironment(arg);
				CommandStatus executionStatus = shellCommand.execute(environment, arg);
				restoreEnvironment();
				if (executionStatus == CommandStatus.TERMINATE) {
					break;
				}
			} catch (RuntimeException critical) {
				System.err.println("A critical error occured: " + critical.getMessage());
				System.err.println("Stack trace:");
				critical.printStackTrace();
				return;
			}

			// Increase readability
			environment.writeln("");
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
		}
		
		line = line.trim();
		while (line.endsWith(" " + environment.morelinesSymbol)) {
			line = line.substring(0, line.length()-1); // remove the symbol
			
			environment.write(environment.multilineSymbol + " ");
			line += environment.readLine().trim();
		}
		
		return line;
	}
	
	/**
	 * Checks if the <tt>input</tt> string contains an output redirect to a
	 * file. If there is no &gt; symbol in the input, the same input is
	 * returned. Else the file path is parsed and it is attempted to redirect
	 * the shell output stream to the file. This method fails if the specified
	 * path is already a directory. If a file already exists on the specified
	 * path, it is prompted to overwrite it.
	 * 
	 * @param input input to be checked for file redirection
	 * @return input without the file redirection substring
	 * @throws IOException if an I/O error occurs
	 */
	private static String checkEnvironment(String input) throws IOException {
		if (input == null) {
			return input;
		}
		
		// TODO BUG this may occur in some pattern or string argument
		int index = input.lastIndexOf("> ");
		if (index == -1) {
			return input;
		}
		
		/* Extract the output file from the given input string. */
		String output = input.substring(0, index).trim();
		String argument = input.substring(index+1).trim();
		Path outputFile = Helper.resolveAbsolutePath(environment, argument);
		
		/* Check conditions. */
		if (Files.isDirectory(outputFile)) {
			outputFile = Helper.firstAvailable(outputFile.resolve("output.txt"));
		}
		
		if (Files.exists(outputFile)) {
			boolean overwrite = AbstractCommand.promptConfirm(environment, "File " + outputFile + " already exists. Overwrite?");
			if (!overwrite) {
				environment.writeln("Cancelled.");
				return output;
			}
		}
		
		/* Save the out writer and redirect the output stream. */
		lastWriter = environment.writer;
		environment.writer = Files.newBufferedWriter(outputFile);
		
		return output;
	}

	/**
	 * Restores the environment to the state before output stream to file
	 * redirection.
	 */
	private static void restoreEnvironment() {
		if (lastWriter != null) {
			environment.writer = lastWriter;
			lastWriter = null;
		}
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
		
		/** Current ID number for marked paths. */
		private int markNum = 0;
		/** Map that associates paths with an ID number. */
		private Map<Integer, Path> markedMap = new HashMap<>();
		
		/** Connection object that manages client-host connections. */
		private ConnectionImpl connection = new ConnectionImpl();
		
		@Override
		public String readLine() throws IOException {
			return reader.readLine();
		}
		
		@Override
		public synchronized void write(String s) throws IOException {
			if (s == null) {
				s = "null";
			}
			writer.write(s);
			writer.flush();
		}
		
		@Override
		public synchronized void write(char cbuf[], int off, int len){
			try {
				writer.write(cbuf, off, len);
				writer.flush();
			} catch (IOException e) {}
		}
		
		@Override
		public synchronized void writeln(String s) throws IOException {
			write(s);
			writer.newLine();
			writer.flush();
		}
		
		@Override
		public Iterable<ShellCommand> commands() {
			return commands.values().stream()
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
		public int mark(Path path) {
			markedMap.put(++markNum, path);
			return markNum;
		}

		@Override
		public void clearMarks() {
			markNum = 0;
			markedMap.clear();
		}
		
		@Override
		public Path getMarked(int num) {
			return markedMap.get(num);
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
		/** Cryptographic cipher for encrypted connection. */
		private Crypto crypto;
		
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
			try { inFromClient.close(); } catch (Exception e) {}
			try { outToClient.close();  } catch (Exception e) {}
			try { environment.reader.close(); } catch (Exception e) {}
			try { environment.writer.close(); } catch (Exception e) {}
			
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
		public Crypto getCrypto() {
			return crypto;
		}

		@Override
		public void setCrypto(Crypto crypto) {
			this.crypto = crypto;
		}
		
	}
	
}
