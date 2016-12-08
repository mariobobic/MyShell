package hr.fer.zemris.java.tecaj.hw07.shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import hr.fer.zemris.java.tecaj.hw07.shell.commands.*;
import hr.fer.zemris.java.tecaj.hw07.shell.extracommands.*;

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
 */
public class MyShell {
	
	/** A map of commands. */
	private static Map<String, ShellCommand> commands;
	
	static {
		commands = new HashMap<>();
		ShellCommand[] cc = {
				new SymbolCommand(),
				new CharsetsCommand(),
				new CatCommand(),
				new LsCommand(),
				new TreeCommand(),
				new CopyCommand(),
				new MkdirCommand(),
				new HexdumpCommand(),
				new HelpCommand(),
				new ExitCommand(),
				
				new DirCommand(),
				new RmdirCommand(),
				new RmCommand(),
				new LargestCommand(),
				new ByteShuffleCommand(),
				new NameShuffleCommand(),
				
				new HostCommand(),
				new ConnectCommand()
				
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
		environment.writeln("Welcome to MyShell v1.0! You may enter commands.");
		
		while (true) {
			environment.write(environment.promptSymbol + " ");
			
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
				executionStatus = shellCommand.execute(environment, arg);
			} catch (RuntimeException critical) {
				System.err.println("A critical error occured: " + critical.getMessage());
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
		String line = environment.readLine().trim();
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
		
		/** A reader that reads from the standard input. */
		private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		/** A writer that writes on the standard output. */
		private BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
		
		/** Prompt symbol to indicate the environment is ready. */
		private Character promptSymbol = '>';
		/** More lines symbol that is used for separating commands in multiple lines. */
		private Character morelinesSymbol = '\\';
		/** Multi-line symbol for writing commands in multiple lines. */
		private Character multilineSymbol = '|';
		
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
	}
	
	/**
	 * Redirects the input and output stream to the client to establish a
	 * connection with the host. This method does not close the previously
	 * opened reader and writer.
	 * <p>
	 * This method is primarily used by the {@linkplain HostCommand} of this
	 * MyShell.
	 * 
	 * @param in new online reader
	 * @param out new online writer
	 */
	public static void connectStreams(BufferedReader in, BufferedWriter out) {
		environment.reader = in;
		environment.writer = out;
	}
	
	/**
	 * Redirects the input and output stream to standard input and output.
	 * This method does not close the previously opened reader and writer.
	 * <p>
	 * This method is primarily used by the {@linkplain HostCommand} of this
	 * MyShell.
	 */
	public static void disconnectStreams() {
		environment.reader = new BufferedReader(new InputStreamReader(System.in));
		environment.writer = new BufferedWriter(new OutputStreamWriter(System.out));
	}
	
}
