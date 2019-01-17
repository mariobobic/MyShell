package hr.fer.zemris.java.shell.interfaces;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import hr.fer.zemris.java.shell.utility.Crypto;
import hr.fer.zemris.java.shell.utility.exceptions.IllegalPathException;

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
     * @param encrypto cryptographic cipher in encryption mode to be set
     * @param decrypto cryptographic cipher in decryption mode to be set
     */
    void connectStreams(InputStream in, OutputStream out, Crypto encrypto, Crypto decrypto);

    /**
     * Redirects the input and output stream to the last input stream and output
     * stream before streams are connected. If there were no previous stacked
     * connections, redirects the streams to the standard input and standard
     * output.
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
     * Returns the cryptographic cipher for this connection that is in
     * encryption mode.
     *
     * @return the cryptographic cipher in encryption mode
     */
    Crypto getEncrypto();

    /**
     * Returns the cryptographic cipher for this connection that is in
     * decryption mode.
     *
     * @return the cryptographic cipher in decryption mode
     */
    Crypto getDecrypto();

    /**
     * Returns the download path of this connection.
     *
     * @return the download path of this connection
     */
    Path getDownloadPath();

    /**
     * Sets the download path of this connection to the specified <tt>path</tt>.
     *
     * @param path path to be set
     * @throws IllegalPathException if the specified path exists, but is a file
     *         or if the specified path does not exists and can not be created
     */
    void setDownloadPath(Path path);

}
