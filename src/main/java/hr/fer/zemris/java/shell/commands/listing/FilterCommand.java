package hr.fer.zemris.java.shell.commands.listing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.MyPattern;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static hr.fer.zemris.java.shell.utility.CommandUtility.markAndPrintPath;

/**
 * A command that is used for filtering and writing out the absolute path of
 * files that match the given pattern. This search begins in the current working
 * directory and is going through all of the subdirectories.
 *
 * @author Mario Bobic
 */
public class FilterCommand extends VisitorCommand {

    /* Flags */
    /** True if regular expression should be used for pattern matching. */
    private boolean useRegex;
    /** True if regular expression pattern should be case sensitive. */
    private boolean caseSensitive;
    /** True if full path should be used while matching file names. */
    private boolean useFullPath;
    /** True if only directories should be matched against the pattern. */
    private boolean directoriesOnly;

    /**
     * Constructs a new command object of type {@code FilterCommand}.
     */
    public FilterCommand() {
        super("FILTER", createCommandDescription(), createFlagDescriptions());
    }

    @Override
    public String getCommandSyntax() {
        return "<pattern>";
    }

    /**
     * Creates a list of strings where each string represents a new line of this
     * command's description. This method is generates description exclusively
     * for the command that this class represents.
     *
     * @return a list of strings that represents description
     */
    private static List<String> createCommandDescription() {
        List<String> desc = new ArrayList<>();
        desc.add("Searches the current directory and all its subdirectories.");
        desc.add("Displays the absolute path of files that match the given pattern.");
        desc.add("Pattern may be given with optional asterisk (*) symbols.");
        desc.add("Advanced regex search and full path search can be used by providing appropriate flags.");
        return desc;
    }

    /**
     * Creates a list of {@code FlagDescription} objects where each entry
     * describes the available flags of this command. This method is generates
     * description exclusively for the command that this class represents.
     *
     * @return a list of strings that represents description
     */
    private static List<FlagDescription> createFlagDescriptions() {
        List<FlagDescription> desc = new ArrayList<>();
        desc.add(new FlagDescription("r", null, null, "Use regex pattern matching."));
        desc.add(new FlagDescription("c", null, null, "Regex pattern is case sensitive."));
        desc.add(new FlagDescription("f", null, null, "Use full path matching instead of just file name."));
        desc.add(new FlagDescription("d", null, null, "Match only against directories."));
        return desc;
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        useRegex = false;
        caseSensitive = false;
        useFullPath = false;

        /* Compile! */
        s = commandArguments.compile(s);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("r")) {
            useRegex = true;
        }

        if (commandArguments.containsFlag("c")) {
            caseSensitive = true;
        }

        if (commandArguments.containsFlag("f")) {
            useFullPath = true;
        }

        if (commandArguments.containsFlag("d")) {
            directoriesOnly = true;
        }

        return super.compileFlags(env, s);
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            throw new SyntaxException();
        }

        // Get string pattern, this helps us avoid quotation marks
        String pattern = StringUtility.extractArguments(s, 1)[0];

        /* Compile the pattern. */
        MyPattern myPattern;
        try {
            if (useRegex) {
                myPattern = caseSensitive ?
                        new MyPattern(Pattern.compile(pattern)) :
                        new MyPattern(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } else {
                myPattern = new MyPattern(pattern);
            }
        } catch (PatternSyntaxException e) {
            env.writeln("Pattern error occurred:");
            env.writeln(e.getMessage());
            return ShellStatus.CONTINUE;
        }

        /* Clear previously marked paths. */
        env.clearMarks();

        FilterFileVisitor filterVisitor = new FilterFileVisitor(env, myPattern);
        Path path = env.getCurrentPath();

        walkFileTree(path, filterVisitor);
        int fails = filterVisitor.getFails();
        if (fails != 0) {
            env.writeln("Failed to access " + fails + " paths.");
        }

        return ShellStatus.CONTINUE;
    }

    /**
     * A {@linkplain SimpleFileVisitor} extended and used to serve the {@linkplain
     * FilterCommand}. Only the {@code visitFile} method is overridden.
     *
     * @author Mario Bobic
     */
    private class FilterFileVisitor extends SimpleFileVisitor<Path> {

        /** An environment. */
        private Environment environment;
        /** Pattern to be matched against. */
        private MyPattern pattern;

        /** Number of files that failed to be accessed. */
        private int fails;

        /**
         * Initializes a new instance of this class setting the desired pattern and
         * an environment used only for writing out messages.
         *
         * @param environment an environment
         * @param pattern pattern to be matched against
         */
        public FilterFileVisitor(Environment environment, MyPattern pattern) {
            this.environment = environment;
            this.pattern = pattern;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            printMatch(dir);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Checks if the file matches the given {@link #pattern} and writes
         * it out if it does.
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (!directoriesOnly) {
                printMatch(file);
            }
            return FileVisitResult.CONTINUE;
        }

        /**
         * Continues searching for the filtering pattern, even though a certain file
         * failed to be visited.
         */
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            environment.writeln("Failed to access " + file);
            fails++;
            return FileVisitResult.CONTINUE;
        }

        /**
         * Prints the specified path if it matches pattern parts.
         *
         * @param path path to be tested
         */
        private void printMatch(Path path) {
            String fileName = (useFullPath ?
                    path.toAbsolutePath() : Utility.getFileName(path)
            ).toString();

            boolean matches = pattern.matches(fileName);
            if (matches) {
                markAndPrintPath(environment, path);
            }
        }

        /**
         * Returns the number of files that failed to be accessed.
         *
         * @return the number of files that failed to be accessed
         */
        public int getFails() {
            return fails;
        }

        /**
         * Returns true if the given param {@code name} matches the
         * {@code pattern}. The {@code pattern} can contain an asterisk
         * character ("*") that represents 0 or more characters that should not
         * be considered while matching.
         *
         * @param name name that is being examined
         * @param pattern a pattern that may contain the asterisk character
         * @return true if {@code name} matches the {@code pattern}. False otherwise
         * @deprecated this method can have only 2 parts of the pattern. Use
         *             {@link StringUtility#matches(String, String)} instead
         */
        @Deprecated
        @SuppressWarnings("unused")
        private boolean matches(String name, String pattern) {
            if (pattern.contains("*")) {
                int r = pattern.indexOf("*");
                String start = pattern.substring(0, r);
                String end = pattern.substring(r+1);
                if (name.startsWith(start) && name.endsWith(end))
                    return true;
            } else if (name.equals(pattern)) {
                return true;
            }

            return false;
        }
    }

}
