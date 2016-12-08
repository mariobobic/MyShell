package hr.fer.zemris.java.tecaj.hw07.shell;

import java.io.IOException;

import hr.fer.zemris.java.tecaj.hw07.shell.commands.ShellCommand;

/**
 * This interface represents an environment where the whole program works. It is
 * used for claiming the current user's path, working with commands and writing
 * out informational messages to the user.
 *
 * @author Mario Bobic
 * @author Marko Cupic
 */
public interface Environment {

	/**
	 * Reads the user's input and returns it as a string.
	 * 
	 * @return the user's input
	 * @throws IOException if an I/O exception occurs
	 */
	public String readLine() throws IOException;

	/**
	 * Writes the given string using the writer.
	 * 
	 * @param s string to be written
	 * @throws IOException if an I/O exception occurs
	 */
	public void write(String s) throws IOException;
	
	/**
	 * Writes the given array of characters using the writer.
	 * 
	 * @param cbuf array of characters to be written
	 * @param off offset
	 * @param len length to be written
	 */
	public void write(char cbuf[], int off, int len);

	/**
	 * Writes the given string using the writer, inputting a new line at the
	 * end.
	 * 
	 * @param s string to be written
	 * @throws IOException if an I/O exception occurs
	 */
	public void writeln(String s) throws IOException;

	/**
	 * Returns an iterable object containing this Shell's commands.
	 * 
	 * @return an iterable object containing this Shell's commands
	 */
	public Iterable<ShellCommand> commands();
	
	/**
	 * Returns the prompt symbol that is used by MyShell.
	 * 
	 * @return the prompt symbol that is used by MyShell
	 */
	public Character getPromptSymbol();
	
	/**
	 * Sets the prompt symbol to be used by MyShell.
	 * 
	 * @param symbol the prompt symbol to be used by MyShell
	 */
	public void setPromptSymbol(Character symbol);
	
	/**
	 * Returns the morelines symbol that is used by MyShell.
	 * 
	 * @return the morelines symbol that is used by MyShell
	 */
	public Character getMorelinesSymbol();
	
	/**
	 * Sets the morelines symbol to be used by MyShell.
	 * 
	 * @param symbol the morelines symbol to be used by MyShell
	 */
	public void setMorelinesSymbol(Character symbol);
	
	/**
	 * Returns the multiline symbol that is used by MyShell.
	 * 
	 * @return the multiline symbol that is used by MyShell
	 */
	public Character getMultilineSymbol();
	
	/**
	 * Sets the multiline symbol to be used by MyShell.
	 * 
	 * @param symbol the multiline symbol to be used by MyShell
	 */
	public void setMultilineSymbol(Character symbol);
}
