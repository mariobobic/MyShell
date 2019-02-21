package hr.fer.zemris.java.shell.commands.listing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static hr.fer.zemris.java.shell.utility.CommandUtility.formatln;
import static hr.fer.zemris.java.shell.utility.CommandUtility.markAndPrintPath;

/**
 * A command that is used for filtering file names or contents of a file and
 * writing out the absolute path of files whose file names or content match the
 * given pattern interpreted as a regular expression. The search begins in the
 * directory provided, or current working directory if no arguments are provided
 * and goes through all of the subdirectories.
 *
 * @author Mario Bobic
 */
public class FindCommand extends VisitorCommand {

    /** Size limit of files that this command will search through. */
    private static final long DEFAULT_SIZE_LIMIT = 0;

    /* Flags */
    /** True if regular expression should be used for pattern matching. */
    private boolean useRegex;
    /** True if regular expression pattern should be case sensitive. */
    private boolean caseSensitive;
    /** True if lines should be trimmed before printing. */
    private boolean trim;
    /** Size limit of files that will be accessed and opened. */
    private long sizeLimit;

    /**
     * Constructs a new command object of type {@code FindCommand}.
     */
    public FindCommand() {
        super("FIND", createCommandDescription(), createFlagDescriptions());
    }

    @Override
    public String getCommandSyntax() {
        return "(<path>) <pattern>";
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
        desc.add("Searches for the pattern provided as argument, looking in file names and their content.");
        desc.add("Displays the absolute path of files whose file names or content match the given pattern.");
        desc.add("If needed to include spaces to the pattern, use double quotation marks on the argument.");
        desc.add("Files that are exceeding the size limit will be ignored. Default limit iz 5 MiB.");
        desc.add("The specified path may also be a file in which the pattern is searched for.");
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
        desc.add(new FlagDescription("l", "limit", "limit", "Maximum file size in bytes for content reading. Use -1 for unlimited."));
        desc.add(new FlagDescription("t", "trim", null, "Trim lines before printing."));
        return desc;
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        useRegex = false;
        caseSensitive = false;
        trim = false;
        sizeLimit = DEFAULT_SIZE_LIMIT;

        /* Compile! */
        s = commandArguments.compile(s);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("r")) {
            useRegex = true;
        }

        if (commandArguments.containsFlag("c")) {
            caseSensitive = true;
        }

        if (commandArguments.containsFlag("t", "trim")) {
            trim = true;
        }

        if (commandArguments.containsFlag("l", "limit")) {
            sizeLimit = commandArguments.getFlag("l", "limit").getSizeArgument();
            if (sizeLimit == -1) {
                sizeLimit = Integer.MAX_VALUE;
            }
        }

        return super.compileFlags(env, s);
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            throw new SyntaxException();
        }

        /* Possible 1 or 2 arguments, where the second is a pattern
         * that may contain spaces and quotation marks. */
        String[] args = StringUtility.extractArguments(s, 2);

        /* Set path and filter pattern. */
        Path path;
        String filter;

        if (args.length == 1) {
            path = env.getCurrentPath();
            filter = args[0];
        } else if (args.length == 2) {
            path = Utility.resolveAbsolutePath(env, args[0]);
            Utility.requireExists(path);
            filter = args[1];
        } else {
            throw new SyntaxException();
        }

        /* Compile the pattern. */
        MyPattern myPattern;
        try {
            if (useRegex) {
                myPattern = caseSensitive ?
                    new MyPattern(Pattern.compile(filter)) :
                    new MyPattern(Pattern.compile(filter, Pattern.CASE_INSENSITIVE));
            } else {
                myPattern = new MyPattern(filter);
            }
        } catch (PatternSyntaxException e) {
            env.writeln("Pattern error occurred:");
            env.writeln(e.getMessage());
            return ShellStatus.CONTINUE;
        }

        /* Clear previously marked paths. */
        env.clearMarks();

        /* Visit file or directory. */
        FindFileVisitor findVisitor = new FindFileVisitor(env, path, myPattern);
        walkFileTree(path, findVisitor);

        return ShellStatus.CONTINUE;
    }

    /**
     * This method writes to the Environment <tt>env</tt> only in case the
     * pattern matching with the specified <tt>pattern</tt> is satisfied.
     * <p>
     * Pattern matching is executed upon the specified <tt>file</tt>, and
     * searches for the following:
     * <ol>
     * <li>If contents of the specified file match the given pattern, the file
     * name and matched contents are printed out along with the line number of
     * the matched content.
     * <li>If there are no contents of the file that match the given pattern,
     * but the file name matches, only the file name is printed out.
     * <li>If neither contents of the file and file name match the given
     * pattern, nothing is printed out.
     * </ol>
     *
     * @param env an environment
     * @param myPattern pattern to be matched against
     * @param file file upon which pattern matching is executed
     * @throws IOException if an I/O exception occurs
     */
    private void printMatches(Environment env, MyPattern myPattern, Path file) throws IOException {
        if (!Files.isReadable(file)) {
            env.writeln("Failed to access " + file);
            return;
        }

        // Print file immediately if name matches given criteria
        boolean nameMatches = myPattern.matches(file.getFileName().toString());
        if (nameMatches) {
            markAndPrintPath(env, file);
        }

        if (Files.size(file) <= sizeLimit) {
            StringBuilder sb = new StringBuilder();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            new BufferedInputStream(
                                    Files.newInputStream(file)), StandardCharsets.UTF_8))) {

                int counter = 0;
                String line;
                while ((line = br.readLine()) != null) {
                    counter++;
                    line = line.replace((char) 7, (char) 0);
                    if (trim) line = line.trim();
                    if (myPattern.matches(line)) {
                        sb.append("   ")
                                .append(counter)
                                .append(": ")
                                .append(line)
                                .append("\n");
                    }
                }
            }

            // Print matching contents
            if (sb.length() != 0) {
                env.writeln(sb.toString());
            }
        }
    }

    /**
     * Represents a pattern that can be given either as a {@code String} or a
     * {@code Pattern}. If a string is given, it is decompiled to pattern parts
     * using the {@link StringUtility#splitPattern(String)} method. Else the
     * pattern is already a compiled representation of a regular expression.
     * <p>
     * This class contains a {@link #matches(String)} method that matches the
     * specified input string to the argument given in the constructor.
     *
     * @author Mario Bobic
     */
    private static class MyPattern {

        /** Parts of the pattern to be matched against. */
        private String[] patternParts;

        /** Regular expression pattern to be matched against. */
        private Pattern regexPattern;

        /**
         * Constructs an instance of {@code MyPattern} with the specified string
         * pattern.
         *
         * @param pattern a string pattern possibly containing asterisks
         */
        public MyPattern(String pattern) {
            patternParts = StringUtility.splitPattern(pattern.toUpperCase());
        }

        /**
         * Constructs an instance of {@code MyPattern} with the specified
         * regular expression pattern.
         *
         * @param regex a compiled representation of a regular expression
         */
        public MyPattern(Pattern regex) {
            regexPattern = regex;
        }

        /**
         * Returns true if the specified <tt>input</tt> matches this pattern.
         *
         * @param input input to be matched against
         * @return true if the specified input matches this pattern
         */
        public boolean matches(String input) {
            if (patternParts != null) {
                return StringUtility.matches(input.toUpperCase(), patternParts);
            } else {
                return regexPattern.matcher(input).matches();
            }
        }

    }

    /**
     * A {@linkplain SimpleFileVisitor} extended and used to serve the
     * {@linkplain FindCommand}. Pattern matching is case insensitive.
     *
     * @author Mario Bobic
     */
    private class FindFileVisitor extends SimpleFileVisitor<Path> {

        /** An environment. */
        private Environment environment;
        /** The starting file. */
        private Path start;
        /** The wanted pattern to be filtered out. */
        private MyPattern pattern;
        /** Size limit converted to human readable byte count. */
        private String limitStr;

        /**
         * Initializes a new instance of this class setting the desired pattern
         * and an environment used only for writing out messages.
         *
         * @param environment an environment
         * @param start the starting file
         * @param pattern the wanted pattern to be filtered out
         */
        public FindFileVisitor(Environment environment, Path start, MyPattern pattern) {
            this.environment = environment;
            this.start = start;
            this.pattern = pattern;
            this.limitStr = Utility.humanReadableByteCount(sizeLimit);
        }

        /**
         * Checks if the file name or content match the given
         * {@link FindFileVisitor#pattern pattern} and writes it out if it does.
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            try {
                printMatches(environment, pattern, file);
            } catch (IOException e) {
                visitFileFailed(file, e);
            }

            if (!isSilent() && Files.size(file) > sizeLimit) {
                Path relativeFile = start.relativize(file);
                formatln(environment, "Too big to process content: %s (%s)",
                        relativeFile,
                        Utility.humanReadableByteCount(Files.size(file))
                );
            }

            return FileVisitResult.CONTINUE;
        }

        /**
         * Continues searching for the filtering pattern, even though a certain
         * file failed to be visited.
         */
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            environment.writeln("Failed to access " + file);
            return FileVisitResult.CONTINUE;
        }

    }

}
