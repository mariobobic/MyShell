package hr.fer.zemris.java.shell.utility;

import hr.fer.zemris.java.shell.interfaces.Environment;

import java.nio.file.Path;

/**
 * Provides static utility methods for working with commands.
 *
 * @author Mario Bobic
 */
public abstract class CommandUtility {

    /**
     * Disable instantiation or inheritance.
     */
    private CommandUtility() {
    }

    /**
     * Writes a formatted string to the specified environment <tt>env</tt> using
     * the specified format string and arguments. If there are more arguments
     * than format specifiers, the extra arguments are ignored. The number of
     * arguments is variable and may be zero.
     * <p>
     * This method calls the {@link Environment#write(String) env.write(String)}
     * method with a string returned by the
     * {@link String#format(String, Object...)} method. Throws a
     * {@code RuntimeException} if an I/O exception occurs.
     *
     * @param env environment where to write
     * @param format format string to be written
     * @param args arguments referenced by the format specifiers in the format
     *        string
     * @throws RuntimeException if an I/O exception occurs
     */
    public static void format(Environment env, String format, Object... args) {
        env.write(String.format(format, args));
    }

    /**
     * Writes a formatted string to the specified environment <tt>env</tt> using
     * the specified format string and arguments <strong>followed by a newline
     * separator</strong>.
     *
     * @param env environment where to write
     * @param format format string to be written
     * @param args arguments referenced by the format specifiers in the format
     *        string
     * @throws RuntimeException if an I/O exception occurs
     * @see #format(Environment, String, Object...)
     */
    public static void formatln(Environment env, String format, Object... args) {
        format(env, format+"%n", args);
    }

    /**
     * Prompts the user to confirm an action. The user is expected to input
     * <tt>Y</tt> as a <i>yes</i> or <tt>N</tt> as a <i>no</i>. Returns true if
     * the user answered yes, false if no.
     * <p>
     * This method blocks until the user answers yes or no.
     *
     * @param env an environment
     * @param message message to be written out before prompting
     * @return true if the user answered yes, false if no
     */
    public static boolean promptConfirm(Environment env, String message) {
        env.write(message + " (Y/N) ");
        while (true) {
            String line = env.readLine();

            if (line.equalsIgnoreCase("Y")) {
                return true;
            } else if (line.equalsIgnoreCase("N")) {
                return false;
            } else {
                env.write("Please answer Y / N: ");
            }
        }
    }

    /**
     * Marks the specified path <tt>path</tt> and prints the full path name with
     * its ID number. The printed string is followed by a newline separator.
     *
     * @param env an environment
     * @param path path to be marked and printed out
     */
    public static void markAndPrintPath(Environment env, Path path) {
        env.write(path);
        markAndPrintNumber(env, path);
    }

    /**
     * Marks the specified path <tt>path</tt> and prints its ID number. The
     * printed string is followed by a newline separator.
     *
     * @param env an environment
     * @param path path to be marked
     */
    public static void markAndPrintNumber(Environment env, Path path) {
        int num = env.mark(path);
        env.writeln(" <" + num + ">");
    }

    public static class CmdArg {
        public final String cmd;
        public final String arg;

        public CmdArg(String line) {
            int splitter = StringUtility.indexOfWhitespace(line);
            if (splitter != -1) {
                cmd = line.substring(0, splitter).toUpperCase();
                arg = line.substring(splitter+1).trim();
            } else {
                cmd = line.toUpperCase();
                arg = null;
            }
        }
    }

}
