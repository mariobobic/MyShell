package hr.fer.zemris.java.shell.utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import javax.crypto.BadPaddingException;

import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.exceptions.NotEnoughDiskSpaceException;

/**
 * Provides static methods for network transfers - upload and download requests,
 * as well as concrete uploading and downloading.
 *
 * @author Mario Bobic
 */
public abstract class NetworkTransfer {

    /** Keyword used for sending and detecting a download start. */
    public static final char[] DOWNLOAD_KEYWORD = "__DOWNLOAD_START".toCharArray();
    /** Keyword used for sending and detecting an upload start. */
    public static final char[] UPLOAD_KEYWORD = "__UPLOAD_START".toCharArray();

    /** Default connection timeout, 10 seconds. */
    public static final int DEFAULT_TIMEOUT = 1000*10;

    /**
     * Disable instantiation or inheritance.
     */
    private NetworkTransfer() {
    }

    /**
     * Returns true if contents of the specified char array <tt>buffer</tt>
     * match a hint specified by the <tt>keyword</tt>.
     * <p>
     * The <tt>buffer</tt> array is trimmed to the size of the keyword.
     *
     * @param buffer a char array buffer
     * @param keyword keyword to match
     * @return true if buffer contains the keyword
     */
    public static boolean isAHint(char[] buffer, char[] keyword) {
        char[] cbuf2 = Arrays.copyOf(buffer, keyword.length);
        return Arrays.equals(cbuf2, keyword);
    }

    /**
     * Asserts that the a network transfer was successful. In other words,
     * verifies that the specified <tt>status</tt> equals <tt>1</tt> and returns
     * the status, or if the verification fails throws an {@code IOException} with
     * the specified <tt>message</tt>
     *
     * @param status status to be verified
     * @param message message for the exception in case the assertion fails
     * @return the specified status if the assertion is true
     * @throws IOException if the assertion fails
     */
    private static int assertSuccessful(int status, String message) throws IOException {
        if (status != 1) {
            throw new IOException("Unsuccessful transfer: " + message);
        }

        return status;
    }

    /**
     * Sends a request for download of the specified <tt>path</tt> to a server
     * using the specified input and output streams.
     * <p>
     * This method <strong>proceeds</strong> with the download process by
     * calling the
     * {@link #download(Environment, InputStream, OutputStream, Crypto)} method
     * in a loop as long as the file server sends the {@link #DOWNLOAD_KEYWORD}
     * hint.
     *
     * @param env an environment
     * @param path path to be requested for download
     * @param inFromServer input stream of the file server
     * @param outToServer output stream of the file server
     * @param decrypto cryptographic cipher for decrypting files
     * @throws IOException if an I/O error occurs
     */
    public static void requestDownload(Environment env, String path, InputStream inFromServer, OutputStream outToServer, Crypto decrypto) throws IOException {
        // Send a hint that server should start upload
        byte[] start = new String(UPLOAD_KEYWORD).getBytes(StandardCharsets.UTF_8);
        outToServer.write(start);
        int status = inFromServer.read(); // wait for signal: accepted upload
        assertSuccessful(status, "Download request not accepted");

        // Send file name
        byte[] filename = path.getBytes(StandardCharsets.UTF_8);
        outToServer.write(filename);
        status = inFromServer.read(); // wait for signal: received file name
        assertSuccessful(status, "Server did not receive file name");

        BufferedReader clientReader = new BufferedReader(new InputStreamReader(inFromServer));
        char[] cbuf = new char[1024];

        while (clientReader.read(cbuf) != -1) {
            if (NetworkTransfer.isAHint(cbuf, DOWNLOAD_KEYWORD)) {
                NetworkTransfer.download(env, inFromServer, outToServer, decrypto);
            } else {
                break;
            }
        }
    }

    /**
     * Starts the download process. This method blocks until the download is
     * finished or an error occurs. The file is downloaded into user's
     * <tt>home</tt> directory into the Downloads folder.
     * <p>
     * Steps of starting a download process:
     * <ol>
     * <li>Accepts a download and sends a signal of success.
     * <li>Reads a file name and sends a signal of success.
     * <li>Reads the file size and sends a signal of success.
     * <li>Creates an appropriate directory structure in user's Downloads.
     * folder, sends a signal of success if folder creation was successful, or a
     * signal of failure if there is not enough space on disk or a file with
     * directory's name already exists.
     * <li>If a file with the specified name already exists, the new file is
     * saved with the {@link Utility#firstAvailable(Path) first available} name.
     * <li>The download starts and prints progress on standard output.
     * <li>After the download finishes, this method sends the last signal of
     * success or failure to the server.
     * </ol>
     *
     * @param env an environment
     * @param inFromServer input stream of the server
     * @param outToServer output stream of the server
     * @param decrypto cryptographic cipher for decrypting files
     * @throws IOException if an I/O error occurs
     */
    public static void download(Environment env, InputStream inFromServer, OutputStream outToServer, Crypto decrypto) throws IOException {
        if (decrypto.getMode() != Crypto.DECRYPT) {
            throw new IllegalArgumentException("Crypto must be in decryption mode.");
        }

        //////////////////////////////////////////////////
        ///////////////// File parameters ////////////////
        //////////////////////////////////////////////////

        // Accept download
        byte[] bytes = new byte[1024];
        outToServer.write(1); // send a signal: accepted download

        // Read file name
        inFromServer.read(bytes);
        outToServer.write(1); // send a signal: received file name

        String filename = new String(bytes).trim();
        bytes = new byte[1024]; // reset array

        // Read file size
        inFromServer.read(bytes);
        outToServer.write(1); // send a signal: received file size

        String filesize = new String(bytes).trim();
        long size = Long.parseLong(filesize);
        bytes = new byte[1024]; // reset array

        //////////////////////////////////////////////////
        ////////////////////// Paths /////////////////////
        //////////////////////////////////////////////////

        // Create an appropriate directory structure
        Path path = env.getConnection().getDownloadPath().resolve(filename);
        try {
            Utility.requireDiskSpace(size, path);
            Files.createDirectories(Utility.getParent(path));
            outToServer.write(1); // send a signal: ready
        } catch (NotEnoughDiskSpaceException e) {
            outToServer.write(0); // send a signal: not ready
            throw e;
        } catch (FileAlreadyExistsException e) {
            outToServer.write(0); // send a signal: not ready
            throw new IOException("Unable to create directory structure " + path
                + " because a file exists with the same name as one of the directories.", e);
        }

        // Do not overwrite, find first available file name
        path = Utility.firstAvailable(path);

        //////////////////////////////////////////////////
        /////////////////// Downloading //////////////////
        //////////////////////////////////////////////////

        Path relativeName = Paths.get(filename).resolveSibling(path.getFileName());
        String nameAndSize = relativeName + " (" + Utility.humanReadableByteCount(size) + ")";
        System.out.println("Downloading " + nameAndSize);

        // Prepare download
        BufferedInputStream fileInput = new BufferedInputStream(inFromServer);
        bytes = new byte[1024];
        long totalLen = 0;

        // Start download
        Progress progress = new Progress(null, size, true);
        try (BufferedOutputStream fileOutput = new BufferedOutputStream(Files.newOutputStream(path))) {
            while (totalLen < size) {
                int len = fileInput.read(bytes);
                totalLen += len;
                if (totalLen > size) {
                    len -= (totalLen - size);
                    totalLen = size;
                }

                byte[] decrypted = decrypto.update(bytes, 0, len);
                fileOutput.write(decrypted);
                progress.add(len);
            }
            fileOutput.write(decrypto.doFinal());
        } catch (BadPaddingException e) {
            try { outToServer.write(0); } catch (Exception ex) {} // send a signal: download failed
            throw new IOException("An error occured while downloading " + path
                + ". This is probably due to incorrect password.", e);
        } catch (IOException e) {
            try { outToServer.write(0); } catch (Exception ex) {} // send a signal: download failed
            throw new IOException("An unexpected error occurred while downloading " + path, e);
        } finally {
            progress.stop();
        }

        outToServer.write(1); // send a signal: download done
        System.out.println("Finished downloading " + nameAndSize);
    }

    /**
     * Receives a request for upload. The requested upload is accepted and a
     * file name of the requested path is read from the input stream.
     * <p>
     * This method <strong>proceeds</strong> with the upload process by calling
     * the {@link #upload(Path, InputStream, OutputStream, Crypto)} method.
     *
     * @param env an environment
     * @param inFromClient input stream of the file recipient
     * @param outToClient output stream of the file recipient
     * @param encrypto cryptographic cipher for encrypting files
     * @throws IOException if an I/O error occurs
     */
    public static void processUploadRequest(Environment env, InputStream inFromClient, OutputStream outToClient, Crypto encrypto) throws IOException {
        // Accept upload
        byte[] bytes = new byte[1024];
        outToClient.write(1); // send a signal: accepted download

        // Read file name
        inFromClient.read(bytes);
        outToClient.write(1); // send a signal: received file name

        String filename = new String(bytes).trim();
        Path path = Utility.resolveAbsolutePath(env, filename);

        // Continue upload
        upload(path, inFromClient, outToClient, encrypto);
    }

    /**
     * Starts the upload process. This method blocks until the upload is
     * finished or an error occurs. The path is uploaded using a file visitor by
     * streaming bytes of each file, if the specified <tt>path</tt> is a
     * directory.
     * <p>
     * Steps of starting an upload process:
     * <ol>
     * <li>Sends a {@link #DOWNLOAD_KEYWORD} hint and waits for a signal
     * of success.
     * <li>Sends the file name and waits for a signal of success.
     * <li>Sends the file size and waits for a signal of success.
     * <li>Waits for the client to be ready before the upload starts.
     * <li>Finally, starts the upload and prints progress on the standard
     * output.
     * </ol>
     *
     * @param path path that is being uploaded
     * @param inFromClient input stream of the recipient
     * @param outToClient output stream of the recipient
     * @param encrypto cryptographic cipher for encrypting files
     * @throws IOException if an I/O error occurs
     */
    public static void upload(Path path, InputStream inFromClient, OutputStream outToClient, Crypto encrypto) throws IOException {
        if (encrypto.getMode() != Crypto.ENCRYPT) {
            throw new IllegalArgumentException("Crypto must be in encryption mode.");
        }

        UploadFileVisitor visitor = new UploadFileVisitor(path, inFromClient, outToClient, encrypto);
        Files.walkFileTree(path, visitor);
    }

    /**
     * Uploads the specified <tt>file</tt> to the client using the specified
     * input and output streams. Path <tt>root</tt> is used to create a relative
     * file name of the file that is being uploaded, which is then sent to the
     * client to create an appropriate directory structure before downloading.
     * <p>
     * An {@code IOException} may be thrown if the client connection is not
     * ready to receive the file.
     *
     * @param root root path from which files are being uploaded
     * @param file file that is being uploaded
     * @param inFromClient input stream of the recipient
     * @param outToClient output stream of the recipient
     * @param encrypto cryptographic cipher for encrypting files
     * @throws IOException if an I/O error occurs
     * @throws SocketException if a connection error occurs between host and
     *         client (typically the client ends the connection)
     */
    private static void upload(Path root, Path file, InputStream inFromClient, OutputStream outToClient, Crypto encrypto) throws IOException, SocketException {
        try {
            //////////////////////////////////////////////////
            ///////////////// File parameters ////////////////
            //////////////////////////////////////////////////

            // Send a hint that client should start download
            byte[] start = new String(DOWNLOAD_KEYWORD).getBytes(StandardCharsets.UTF_8);
            outToClient.write(start);
            int status = inFromClient.read(); // wait for signal: accepted download
            assertSuccessful(status, "Upload request not accepted");

            // Send file name
            String filename = root.relativize(file).toString().replace('\\', '/');
            outToClient.write(filename.getBytes(StandardCharsets.UTF_8));
            status = inFromClient.read(); // wait for signal: received file name
            assertSuccessful(status, "Client did not receive file name");

            // Send file size
            byte[] filesize = Long.toString(Crypto.postSize(file)).getBytes(StandardCharsets.UTF_8);
            outToClient.write(filesize);
            status = inFromClient.read(); // wait for signal: received file size
            assertSuccessful(status, "Client did not receive file size");

            // Wait for client to be ready
            status = inFromClient.read(); // wait for signal: ready
            assertSuccessful(status, "Client not ready. Aborting upload of: " + file);

            //////////////////////////////////////////////////
            //////////////////// Uploading ///////////////////
            //////////////////////////////////////////////////

            String nameAndSize = filename + " (" + Utility.humanReadableByteCount(Files.size(file)) + ")";
            System.out.println("Uploading " + nameAndSize);

            // Start streaming file
            Progress progress = new Progress(null, Files.size(file), true);
            try (BufferedInputStream fileStream = new BufferedInputStream(Files.newInputStream(file))) {
                byte[] bytes = new byte[1024];
                int len;
                while ((len = fileStream.read(bytes)) != -1) {
                    byte[] encrypted = encrypto.update(bytes, 0, len);
                    outToClient.write(encrypted);
                    progress.add(len);
                }
                outToClient.write(encrypto.doFinal());
            } catch (BadPaddingException ignorable) {
                // ignored, since crypto is in encryption mode
            } finally {
                progress.stop();
            }

            System.out.println("Finished uploading " + nameAndSize);
        } finally {
            try { outToClient.flush(); } catch (IOException e) {}
        }
    }

    /**
     * A {@linkplain SimpleFileVisitor} extended and used to serve for
     * downloading files and directories.
     *
     * @author Mario Bobic
     */
    private static class UploadFileVisitor extends SimpleFileVisitor<Path> {

        /** Starting file. */
        private Path root;
        /** Input stream of the recipient. */
        private InputStream inFromClient;
        /** Output stream of the recipient. */
        private OutputStream outToClient;
        /** Cryptographic cipher for encrypting files. */
        private Crypto encrypto;

        /**
         * Constructs an instance of {@code UploadFileVisitor} with the
         * specified environment.
         * <p>
         * The starting file is converted to a root directory. This is
         * convenient for relativizing file names.
         *
         * @param start starting file
         * @param encrypto cryptographic cipher for encrypting files
         * @param inFromClient input stream of the recipient
         * @param outToClient output stream of the recipient
         */
        public UploadFileVisitor(Path start, InputStream inFromClient, OutputStream outToClient, Crypto encrypto) {
            this.root = Utility.getParent(start);
            this.inFromClient = inFromClient;
            this.outToClient = outToClient;
            this.encrypto = encrypto;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            process(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            System.out.println("Failed to access " + file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Uploads the specified <tt>file</tt> to the client and waits for the
         * signal of download completion. The received signal may be <tt>1</tt>
         * for successful download or <tt>0</tt> for failed download.
         *
         * @param file file to be uploaded and processed
         * @throws IOException if the download fails on the client side
         */
        private void process(Path file) throws IOException {
            upload(root, file, inFromClient, outToClient, encrypto);
            int status = inFromClient.read(); // wait for signal: download done

            if (status == 0) {
                throw new IOException("Download failed on client side for: " + file);
            }
        }
    }

}
