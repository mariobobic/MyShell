package hr.fer.zemris.java.shell.utility;

import hr.fer.zemris.java.shell.interfaces.Environment;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * This class provides the functionality of a cryptographic cipher for
 * encryption and decryption.
 * <p>
 * The class offers a method for automatic encryption or decryption of a source
 * file into a destination file ({@link #execute(Path, Path)}.<br>
 * It also offers two methods to manually encrypt or decrypt bytes
 * ({@link #update(byte[], int, int)} and {@link #doFinal()}).
 *
 * @author Mario Bobic
 */
public class Crypto {

    /** Encryption mode. */
    public static final boolean ENCRYPT = true;
    /** Decryption mode. */
    public static final boolean DECRYPT = false;

    /** Standard size for the loading byte buffer array. */
    private static final int STD_LOADER_SIZE = 4096;

    /** Minimal length of the hash. Hash is trimmed to this length. */
    private static final int HASH_LEN = 32;

    /** Hash to be used while encrypting or decrypting. */
    private String hash;

    /** The operation mode of this crypto, i.e. Crypto.ENCRYPT. */
    private boolean mode;

    /** Cipher used by this crypto. */
    Cipher cipher;

    /**
     * Constructs an instance of {@code Crypto} with the specified arguments.
     *
     * @param hash hash to be used while encrypting or decrypting
     * @param mode encryption or decryption mode, i.e. Crypto.ENCRYPT
     * @throws IllegalArgumentException if <tt>hash</tt> is invalid
     */
    public Crypto(String hash, boolean mode) {
        if (hash.length() < HASH_LEN) {
            throw new IllegalArgumentException("Hash length must not be smaller than " + HASH_LEN);
        }

        this.hash = hash.substring(0, HASH_LEN); // must be 16 bytes for SecretKeySpec
        this.mode = mode;
        initialize(mode);
    }

    /**
     * Initializes this Crypto by instantiating a cipher in encryption or
     * decryption mode.
     * <p>
     * To use the encryption mode, the <tt>encrypt</tt> value must be
     * <tt>true</tt>.<br>
     * To use the decryption mode, the <tt>encrypt</tt> value must be
     * <tt>false</tt>.
     *
     * @param encrypt specified encryption or decryption
     */
    private void initialize(boolean encrypt) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(DatatypeConverter.parseHexBinary(hash), "AES");
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(DatatypeConverter.parseHexBinary(hash));

            /* Create a cipher and start encrypting/decrypting. */
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec, paramSpec);
        } catch (GeneralSecurityException e) {
            throw new InternalError("Could not initialize.", e);
        }
    }

    /**
     * Calculates the new size of <tt>source</tt> after its
     * <strong>encryption</strong> considering the cryptographic padding used.
     * <p>
     * For an example, if the <tt>source</tt> size is 295 bytes, to
     * calculate its size after encryption simply use the following method
     * (which will return 304 bytes):
     *
     * <pre>
     * Crypto.postSize(source);
     * </pre>
     *
     * Note that size of decrypted file can not be known in advance because of
     * the algorithm padding.
     *
     * @param source file whose post size is to be calculated
     * @return new size of <tt>source</tt> after encryption
     * @throws IOException if an I/O error occurs
     */
    public static long postSize(Path source) throws IOException {
        return (Files.size(source)/16 + 1) * 16;
    }

    /**
     * <b>Encrypts</b> or <b>decrypts</b> the file specified by the
     * <tt>src</tt> and generates a file specified by the
     * <tt>dst</tt>. The <tt>hash</tt> that was given to the constructor is
     * used as a password. This method blocks until the execution is over.
     *
     * @param src file to be encrypted or decrypted
     * @param dst file to be created
     * @throws FileNotFoundException if the file does not exist, is a directory
     *         rather than a regular file, or for some other reason cannot be
     *         opened for reading
     * @throws IOException if any other I/O error occurs
     * @throws BadPaddingException if this crypto is in decryption mode, but a
     *         wrong key is given or the bytes were not properly padded
     */
    public void execute(Path src, Path dst) throws IOException, BadPaddingException {
        execute(src, dst, null);
    }

    /**
     * <b>Encrypts</b> or <b>decrypts</b> the file specified by the
     * <tt>src</tt> and generates a file specified by the
     * <tt>dst</tt>. The <tt>hash</tt> that was given to the constructor is
     * used as a password. This method blocks until the execution is over.
     * <p>
     * This exclusive method accepts an environment where it writes the progress
     * of execution. If the specified environment is <tt>null</tt>, progress
     * tracker is not used.
     *
     * @param src file to be encrypted or decrypted
     * @param dst file to be created
     * @param env an environment for writing out progress, may be <tt>null</tt>
     * @throws FileNotFoundException if the file does not exist, is a directory
     *         rather than a regular file, or for some other reason cannot be
     *         opened for reading
     * @throws IOException if any other I/O error occurs
     * @throws BadPaddingException if this crypto is in decryption mode, but a
     *         wrong key is given or the bytes were not properly padded
     */
    public void execute(Path src, Path dst, Environment env) throws IOException, BadPaddingException {
        Progress progress = env == null ? null : new Progress(env, Files.size(src), true);
        try (
            InputStream in = new BufferedInputStream(Files.newInputStream(src));
            OutputStream out = new BufferedOutputStream(Files.newOutputStream(dst))
        ) {
            int len;
            byte[] bytes = new byte[STD_LOADER_SIZE];
            while ((len = in.read(bytes)) != -1) {
                // Update until the very end
                byte[] processedBytes = update(bytes, 0, len);
                out.write(processedBytes);
                if (progress != null) progress.add(len);
            }
            // Do the final touch
            byte[] processedBytes = doFinal();
            out.write(processedBytes);
        } finally {
            if (progress != null) progress.stop();
        }
    }

    /**
     * Continues a multiple-part encryption or decryption operation (depending
     * on how this crypto was initialized), processing another data part.
     * <p>
     * The first <tt>len</tt> bytes in the <tt>input</tt> buffer, starting at
     * <tt>offset</tt> inclusive, are processed, and the result is stored in a
     * new buffer.
     * <p>
     * If <tt>len</tt> is zero, this method returns <tt>null</tt>.
     *
     * @param input the input buffer
     * @param offset the offset in input where the input starts
     * @param len the input length
     * @return the new buffer with the result
     */
    public byte[] update(byte[] input, int offset, int len) {
        return cipher.update(input, 0, len);
    }

    /**
     * Finishes a multiple-part encryption or decryption operation, depending on
     * how this crypto was initialized. The result is stored in a new buffer.
     * <p>
     * A {@code BadPaddingException} may be thrown if this crypto is in
     * <tt>decryption mode</tt> and its hash does not match the hash used to
     * encrypt bytes.
     *
     * @return the new buffer with the result
     * @throws IOException masked security exceptions. See {@link Cipher#doFinal()}
     * @throws BadPaddingException if this crypto is in decryption mode, but a
     *         wrong key is given or the bytes were not properly padded
     */
    public byte[] doFinal() throws IOException, BadPaddingException {
        try {
            return cipher.doFinal();
        } catch (BadPaddingException e) {
            throw e;
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    /**
     * Returns the operation mode of this crypto, {@link #ENCRYPT} or
     * {@link #DECRYPT}.
     *
     * @return the operation mode of this crypto
     */
    public boolean getMode() {
        return mode;
    }

}
