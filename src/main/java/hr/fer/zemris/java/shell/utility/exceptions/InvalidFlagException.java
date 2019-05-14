package hr.fer.zemris.java.shell.utility.exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Signals that program flags were used in an illegal or inappropriate way.
 *
 * @author Mario Bobic
 */
public class InvalidFlagException extends RuntimeException {
    /** Serialization UID. */
    private static final long serialVersionUID = 1L;

    /** A collection of flags that caused this exception. */
    private final Collection<String> flags = new ArrayList<>();

    /**
     * Constructs an {@code CommandFlagException} with the specified flag that
     * caused the exception.
     *
     * @param message the detail message
     */
    public InvalidFlagException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code CommandFlagException} with the specified detail
     * message and a flag that caused the exception.
     *
     * @param message the detail message
     * @param flag flag that caused the exception
     */
    public InvalidFlagException(String message, String flag) {
        super(message);
        flags.add(flag);
    }

    /**
     * Constructs an {@code CommandFlagException} with the specified collection
     * of flags.
     *
     * @param flags collection of flags that caused the exception
     */
    public InvalidFlagException(Collection<String> flags) {
        this(null, flags);
    }

    /**
     * Constructs an {@code CommandFlagException} with the specified detail
     * message and collection of flags.
     *
     * @param message the detail message
     * @param flags collection of flags that caused the exception
     */
    public InvalidFlagException(String message, Collection<String> flags) {
        super(message);
        this.flags.addAll(flags);
    }

    /**
     * Returns an unmodifiable collection of flags that caused this exception.
     *
     * @return an unmodifiable collection of flags that caused this exception.
     */
    public Collection<String> getFlags() {
        return Collections.unmodifiableCollection(flags);
    }

}
