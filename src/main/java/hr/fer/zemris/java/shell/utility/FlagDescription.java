package hr.fer.zemris.java.shell.utility;

import java.util.Objects;

/**
 * This class is a simple structure for providing flag descriptions.
 * Mandatory fields are flag's name and description, while other fields are
 * optional.
 *
 * @author Mario Bobic
 */
public class FlagDescription implements Comparable<FlagDescription> {

    /** Flag name, with dash or dashes included. */
    public final String name;
    /** Other flag name, with dash or dashes included. */
    public final String otherName;
    /** Flag argument name. */
    public final String argument;
    /** Flag description. */
    public final String description;

    /**
     * Constructs an instance of {@code FlagDescription} with the specified
     * arguments.
     *
     * @param name name of the flag, not <tt>null</tt>
     * @param otherName flag's other name, may be <tt>null</tt>
     * @param argument flag's argument name, may be <tt>null</tt>
     * @param description flag description, not <tt>null</tt>
     * @throws NullPointerException if either name or description is null
     */
    public FlagDescription(String name, String otherName, String argument, String description) {
        Objects.requireNonNull("Name must not be null.", name);
        Objects.requireNonNull("Description must not be null.", description);

        this.name = processName(name);
        this.otherName = processName(otherName);
        this.argument = argument;
        this.description = description;
    }

    /**
     * Processes the flag <tt>name</tt> by adding one dash to the beginning
     * of the string if name has length of 1 or two dashes otherwise.
     *
     * @param name name to be processed, may be <tt>null</tt>
     * @return name with one or two dashes added to the beginning
     */
    private static String processName(String name) {
        if (name == null) return null;
        return (name.length() == 1 ? "-" : "--") + name;
    }

    /**
     * Returns the clean name of the flag, without preceding dash or dashes.
     * This process is convenient for obtaining only the flag name, as flags of
     * this class are stored with dashes in front of their name.
     * <p>
     * If the given <tt>flag</tt> is a <tt>null</tt> reference, nothing is done
     * and <tt>null</tt> is returned.
     *
     * @param flag flag to be unflagged
     * @return the clean name of the flag, without preceding dashes
     */
    public static String unflag(String flag) {
        if (flag == null) return null;
        return flag.replaceFirst("^--?", "");
    }

    /**
     * Returns the concatenation of <tt>name</tt>, <tt>otherName</tt> and
     * <tt>argument</tt> fields of this flag description, considering only
     * non-null values and adding the spaces in-between fields. If other
     * name exists, a comma is added after the name, followed by a space and
     * the other name.
     * <p>
     * For an example, if this flag description contains header fields as
     * follows:
     *
     * <pre>
     * name:       e        len=1
     * otherName:  exclude  len=7
     * argument:   path     len=4
     * </pre>
     *
     * the returned header will be: <blockquote>-e, --exclude
     * path.</blockquote>
     *
     * Its length will be <tt>1+(2)+7+(1)+4 = 15</tt>.
     *
     * @return the total length of header fields
     */
    public String getHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);

        if (otherName != null) {
            sb.append(", ").append(otherName);
        }

        if (argument != null) {
            sb.append(" ").append(argument);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return name + (argument != null ? " "+argument : "");
    }

    @Override
    public int compareTo(FlagDescription other) {
        return name.compareToIgnoreCase(other.name);
    }

}