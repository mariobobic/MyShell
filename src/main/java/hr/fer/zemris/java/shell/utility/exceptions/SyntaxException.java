package hr.fer.zemris.java.shell.utility.exceptions;

/**
 * Exception that is thrown when an invalid syntax is used.
 *
 * @author Mario Bobic
 */
public class SyntaxException extends IllegalArgumentException {
    /** Serialization UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@code SyntaxException} with no detail message and no cause.
     */
    public SyntaxException() {
        super();
    }

    /**
     * Constructs a {@code SyntaxException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public SyntaxException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code SyntaxException} with the specified cause.
     *
     * @param cause the cause
     */
    public SyntaxException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a {@code SyntaxException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public SyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

}
