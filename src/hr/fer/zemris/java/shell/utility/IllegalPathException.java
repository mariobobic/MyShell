package hr.fer.zemris.java.shell.utility;

/**
 * Exception that is thrown when an illegal path is used. For example, if a
 * directory was required, but the given path does not exist or is a file.
 *
 * @author Mario Bobic
 */
public class IllegalPathException extends IllegalArgumentException {
	/** Serialization UID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a {@code IllegalPathException} with no detail message and no cause.
	 */
	public IllegalPathException() {
		super();
	}

	/**
	 * Constructs a {@code IllegalPathException} with the specified detail message.
	 *
	 * @param message the detail message.
	 */
	public IllegalPathException(String message) {
		super(message);
	}

	/**
	 * Constructs a {@code IllegalPathException} with the specified cause.
	 * 
	 * @param cause the cause
	 */
	public IllegalPathException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a {@code IllegalPathException} with the specified detail message
	 * and cause.
	 * 
	 * @param message the detail message
	 * @param cause the cause
	 */
	public IllegalPathException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
