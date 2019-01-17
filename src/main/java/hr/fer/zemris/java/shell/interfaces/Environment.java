package hr.fer.zemris.java.shell.interfaces;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;

/**
 * This interface represents an environment where the whole program works. It is
 * used for claiming the current user's path, working with commands and writing
 * out informational messages to the user.
 * <p>
 * This environment is an upgrade of the {@code BasicEnvironment} that provides
 * basic environment functionality.
 *
 * @author Mario Bobic
 */
public interface Environment extends BasicEnvironment {
	
	/**
	 * Writes the given object as a string using a writer.
	 * 
	 * @param obj object to be written as a string
	 */
	default void write(Object obj) {
		write(String.valueOf(obj));
	}
	
	/**
	 * Writes the given object as a string using a writer, followed by a new
	 * line separator.
	 * 
	 * @param obj object to be written as a string
	 */
	default void writeln(Object obj) {
		writeln(String.valueOf(obj));
	}
	
	/**
	 * Pushes the specified buffered reader and writer to the environment stack
	 * and uses them as current reader and writer. At all times, <strong>the
	 * number of readers and writers on the stack will be equal</strong>.
	 * <p>
	 * If any of the specified arguments is <tt>null</tt>, the previous or
	 * default reader/writer is pushed onto the stack. This is convenient if one
	 * simply wants to push either a reader or writer to be used temporarily,
	 * and leave the other one as is.
	 * <p>
	 * If both reader and writer are <tt>null</tt>, an
	 * {@code IllegalArgumentException} is thrown.
	 * <p>
	 * <em><strong>Caution: DO NOT create your own reader or writer of the
	 * standard input or output</strong>, as readers and writers are closed when
	 * popping them. <strong>Use {@link BasicEnvironment#stdIn} and
	 * {@link BasicEnvironment#stdOut} instead.</strong><em>
	 * 
	 * @param in reader
	 * @param out writer
	 * @throws IllegalArgumentException if both reader and writer are null
	 */
	void push(Reader in, Writer out);

	/**
	 * Removes the previously pushed {@code Reader} and {@code Writer} from the
	 * stack, and optionally closes them if the specified flag is <tt>true</tt>.
	 * <p>
	 * This method should be called after reader and writer on the stack have
	 * served their purpose.
	 * <p>
	 * If there are currently no reader and writer on the stack, this method
	 * has no effect and trivially returns.
	 * 
	 * @param close indicates if streams should be closed after popping
	 */
	void pop(boolean close);

	/**
	 * Returns the path of a directory where this program was run.
	 * 
	 * @return the path of a directory where this program was run
	 */
	Path getHomePath();
	
	/**
	 * Gets the value of an environment, system or shell variable with the
	 * specified <tt>name</tt>.
	 * 
	 * @param name name of the variable
	 * @return variable's value
	 */
	String getVariable(String name);
	
	/**
	 * Sets the value of an environment variable with the specified
	 * <tt>name</tt> to the specified <tt>value</tt>.
	 * 
	 * @param name name of the variable whose value is to be set
	 * @param value value to be set
	 */
	void setVariable(String name, String value);
	
	/**
	 * Returns the history of inputs entered by the user.
	 * 
	 * @return the history of inputs entered by the user
	 */
	List<String> getHistory();
	
	/**
	 * Adds the specified <tt>input</tt> to the history.
	 * 
	 * @param input input to be added to history
	 */
	void addToHistory(String input);
	
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
