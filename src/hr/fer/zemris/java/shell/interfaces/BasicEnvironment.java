package hr.fer.zemris.java.shell.interfaces;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import hr.fer.zemris.java.shell.utility.exceptions.ShellIOException;

/**
 * This interface represents an environment where the whole program works. It is
 * used for claiming the current user's path, working with commands and writing
 * out informational messages to the user.
 *
 * @author Mario Bobic
 * @author Marko Cupic
 */
public interface BasicEnvironment {
	
	/** A default reader that reads from the standard input. */
	BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
	/** A default writer that writes on the standard output. */
	BufferedWriter stdOut = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));

	/**
	 * Reads the user's input and returns it as a string.
	 * 
	 * @return the user's input
	 * @throws ShellIOException if an I/O exception occurs
	 */
	String readLine() throws ShellIOException;

	/**
	 * Writes the given string using a writer.
	 * 
	 * @param s string to be written
	 * @throws ShellIOException if an I/O exception occurs
	 */
	void write(String s) throws ShellIOException;
	
	/**
	 * Writes the given array of characters using a writer.
	 * 
	 * @param cbuf array of characters to be written
	 * @param off offset
	 * @param len length to be written
	 * @throws ShellIOException if an I/O exception occurs
	 */
	void write(char cbuf[], int off, int len) throws ShellIOException;

	/**
	 * Writes the given string using a writer, followed by a new line separator.
	 * 
	 * @param s string to be written
	 * @throws ShellIOException if an I/O exception occurs
	 */
	void writeln(String s) throws ShellIOException;

	/**
	 * Returns an iterable object containing this Shell's commands.
	 * 
	 * @return an iterable object containing this Shell's commands
	 */
	Iterable<ShellCommand> commands();

	/**
	 * Returns the current working directory path.
	 * 
	 * @return the current working directory path
	 */
	Path getCurrentPath();

	/**
	 * Sets the current working directory path.
	 * 
	 * @param path path to be set
	 */
	void setCurrentPath(Path path);
	
	/**
	 * Returns the prompt symbol that is used by MyShell.
	 * 
	 * @return the prompt symbol that is used by MyShell
	 */
	Character getPromptSymbol();
	
	/**
	 * Sets the prompt symbol to be used by MyShell.
	 * 
	 * @param symbol the prompt symbol to be used by MyShell
	 */
	void setPromptSymbol(Character symbol);
	
	/**
	 * Returns the morelines symbol that is used by MyShell.
	 * 
	 * @return the morelines symbol that is used by MyShell
	 */
	Character getMorelinesSymbol();
	
	/**
	 * Sets the morelines symbol to be used by MyShell.
	 * 
	 * @param symbol the morelines symbol to be used by MyShell
	 */
	void setMorelinesSymbol(Character symbol);
	
	/**
	 * Returns the multiline symbol that is used by MyShell.
	 * 
	 * @return the multiline symbol that is used by MyShell
	 */
	Character getMultilineSymbol();
	
	/**
	 * Sets the multiline symbol to be used by MyShell.
	 * 
	 * @param symbol the multiline symbol to be used by MyShell
	 */
	void setMultilineSymbol(Character symbol);

}
