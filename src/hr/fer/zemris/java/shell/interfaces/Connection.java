package hr.fer.zemris.java.shell.interfaces;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * This interface represents a remote connection between two machines. One
 * machine is the host while the other is a client that accesses the host.
 * <p>
 * The interface contains methods for connection manipulation, such as testing
 * if a connection exists, getting {@code InputStream} and {@code OutputStream}
 * objects from the client and other methods for remote download.
 *
 * @author Mario Bobic
 */
public interface Connection {

	/**
	 * Returns true if this machine has an active connection with another
	 * machine running MyShell. False otherwise.
	 * <p>
	 * In other words, returns true if this machine is a host.
	 * 
	 * @return true if this machine has an active connection with another
	 *         machine
	 */
	public boolean isConnected();
	
	/**
	 * Returns an input stream for reading from the client.
	 * 
	 * @return an input stream for reading from the client
	 */
	public InputStream getInFromClient();
	
	/**
	 * Returns an output stream for writing to the client.
	 * 
	 * @return an output stream for writing to the client
	 */
	public OutputStream getOutToClient();

	/**
	 * Marks the specified file <tt>path</tt> for download by associating the
	 * path object with its ID. This is used in association with
	 * {@link #getMarked(int)} to get the marked path.
	 * 
	 * @param path path to be marked
	 * @return ID associated with the marked path
	 */
	public int markForDownload(Path path);

	/**
	 * Clears all paths marked for download by the
	 * {@link #markForDownload(Path)} method. This generally means emptying the
	 * internal collection of ID associated with paths.
	 */
	public void clearDownloadMarks();
	
	/**
	 * Returns a path marked with the specified ID number. The path must be
	 * previously marked with the {@link #markForDownload(Path)} method.
	 * 
	 * @param num ID number of the path to be returned
	 * @return path marked with the specified ID number
	 */
	public Path getMarked(int num);
	
}
