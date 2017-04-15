package hr.fer.zemris.java.shell.utility.exceptions;

import java.io.IOException;

/**
 * Exception that is thrown when there is not enough free space on the disk.
 *
 * @author Mario Bobic
 */
public class NotEnoughDiskSpaceException extends IOException {
	/** Serialization UID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a {@code NotEnoughDiskSpaceException} with no detail message
	 * and no cause.
	 */
	public NotEnoughDiskSpaceException() {
		super();
	}

	/**
	 * Constructs a {@code NotEnoughDiskSpaceException} with the specified
	 * detail message.
	 *
	 * @param message the detail message.
	 */
	public NotEnoughDiskSpaceException(String message) {
		super(message);
	}

	/**
	 * Constructs a {@code NotEnoughDiskSpaceException} with the specified
	 * cause.
	 * 
	 * @param cause the cause
	 */
	public NotEnoughDiskSpaceException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a {@code NotEnoughDiskSpaceException} with the specified
	 * detail message and cause.
	 * 
	 * @param message the detail message
	 * @param cause the cause
	 */
	public NotEnoughDiskSpaceException(String message, Throwable cause) {
		super(message, cause);
	}

}
