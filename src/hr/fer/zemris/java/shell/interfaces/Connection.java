package hr.fer.zemris.java.shell.interfaces;

import java.io.InputStream;
import java.io.OutputStream;

import hr.fer.zemris.java.shell.utility.Crypto;

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
	 * Redirects the input and output stream to the client to establish a
	 * connection with the host. This method does not close the previously
	 * opened reader and writer.
	 * 
	 * @param in input stream from the client
	 * @param out output stream to the client
	 */
	void connectStreams(InputStream in, OutputStream out);
	
	/**
	 * Redirects the input and output stream to standard input and output.
	 * This method does not close the previously opened reader and writer.
	 */
	void disconnectStreams();

	/**
	 * Returns true if this machine has an active connection with another
	 * machine running MyShell. False otherwise.
	 * <p>
	 * In other words, returns true if this machine is a host.
	 * 
	 * @return true if this machine has an active connection with another
	 *         machine
	 */
	boolean isConnected();
	
	/**
	 * Returns an input stream for reading from the client.
	 * 
	 * @return an input stream for reading from the client
	 */
	InputStream getInFromClient();
	
	/**
	 * Returns an output stream for writing to the client.
	 * 
	 * @return an output stream for writing to the client
	 */
	OutputStream getOutToClient();

	/**
	 * Returns the cryptographic cipher for encrypted connection.
	 * 
	 * @return the cryptographic cipher for encrypted connection
	 */
	Crypto getCrypto();

	/**
	 * Sets the cryptographic cipher for encrypted connection to the specified
	 * <tt>crypto</tt>.
	 * 
	 * @param crypto cryptographic cipher to be set
	 */
	void setCrypto(Crypto crypto);
	
}
