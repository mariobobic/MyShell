package hr.fer.zemris.java.shell.interfaces;

import java.io.IOException;
import java.nio.file.Path;

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
	String readLine() throws IOException;

	/**
	 * Writes the given string using the writer.
	 * 
	 * @param s string to be written
	 * @throws IOException if an I/O exception occurs
	 */
	void write(String s) throws IOException;
	
	/**
	 * Writes the given array of characters using the writer.
	 * 
	 * @param cbuf array of characters to be written
	 * @param off offset
	 * @param len length to be written
	 */
	void write(char cbuf[], int off, int len);

	/**
	 * Writes the given string using the writer, inputting a new line at the
	 * end.
	 * 
	 * @param s string to be written
	 * @throws IOException if an I/O exception occurs
	 */
	void writeln(String s) throws IOException;

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
	 * Returns the path of a directory where this program was run.
	 * 
	 * @return the path of a directory where this program was run
	 */
	Path getHomePath();
	
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

	/**
	 * Marks the specified file <tt>path</tt> by associating the path object
	 * with its ID. This is used in association with {@link #getMarked(int)} to
	 * get the marked path.
	 * 
	 * @param path path to be marked
	 * @return ID associated with the marked path
	 */
	int mark(Path path);

	/**
	 * Clears all paths marked by the {@link #mark(Path)} method. This generally
	 * means emptying the internal collection of ID associated with paths.
	 */
	void clearMarks();
	
	/**
	 * Returns a path marked with the specified ID number. The path must be
	 * previously marked with the {@link #mark(Path)} method.
	 * 
	 * @param num ID number of the path to be returned
	 * @return path marked with the specified ID number
	 */
	Path getMarked(int num);
	
	/**
	 * Returns the connection object of this environment.
	 * 
	 * @return the connection object of this environment
	 */
	Connection getConnection();
	
	/**
	 * Returns true if this Environment has connected different input and
	 * output streams. False otherwise.
	 * 
	 * @return true if this Environment has connected different input and
	 *         output streams
	 */
	boolean isConnected();

}
