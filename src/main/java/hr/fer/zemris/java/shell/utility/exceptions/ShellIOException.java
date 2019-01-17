package hr.fer.zemris.java.shell.utility.exceptions;

import java.io.IOException;

/**
 * Signals that an I/O exception of the shell has occurred. Usually used instead
 * of an {@link IOException} while reading and writing.
 *
 * @author Mario Bobic
 */
public class ShellIOException extends RuntimeException {
    /** Serialization UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@code ShellIOException} with no detail message and no
     * cause.
     */
    public ShellIOException() {
        super();
    }

    /**
     * Constructs a {@code ShellIOException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public ShellIOException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code ShellIOException} with the specified cause.
     *
     * @param cause the cause
     */
    public ShellIOException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a {@code ShellIOException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ShellIOException(String message, Throwable cause) {
        super(message, cause);
    }

}
