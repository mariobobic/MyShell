package hr.fer.zemris.java.shell.commands.writing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.CommandUtility;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Used for cleaning unnecessary files like broken symbolic links.
 *
 * @author Mario Bobic
 */
public class CleanCommand extends VisitorCommand {

    /** A function that eats shortcuts. */
    private static final Function<Path, Boolean> FUNC_SHORTCUTS = (path) -> {
        if (Files.isSymbolicLink(path) || Utility.isWindowsShortcut(path)) {
            Path realPathOrLink = Utility.getRealPathFromLink(path);
            if (path.equals(realPathOrLink) || !Files.exists(realPathOrLink)) {
                // Link is broken
                try {
                    Files.delete(path);
                return true;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }

        return false;
    };

    /**
     * Constructs a new command object of type {@code CleanCommand}.
     */
    public CleanCommand() {
        super("CLEAN", createCommandDescription());
    }

    @Override
    public String getCommandSyntax() {
        return "shortcuts (<path>)";
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
        desc.add("Cleans unnecessary files like broken symbolic links.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            throw new SyntaxException();
        }

        String[] args = StringUtility.extractArguments(s, 2);

        /* Resolve path from the second argument, if present. */
        Path dir;
        if (args.length == 1) {
            dir = env.getCurrentPath();
        } else {
            dir = Utility.resolveAbsolutePath(env, args[1]);
        }

        String attribute = args[0];
        Function<Path, Boolean> function = getFunction(attribute);
        if (function == null) {
            env.writeln("Unknown argument: " + attribute);
            return ShellStatus.CONTINUE;
        }

        Utility.requireDirectory(dir);

        CleanFileVisitor cleanVisitor = new CleanFileVisitor(env, function);
        walkFileTree(dir, cleanVisitor);

        int cleanCount = cleanVisitor.getCleanCount();
        int failCount = cleanVisitor.getFailCount();

        CommandUtility.formatln(env, "Cleaned %d %s.", cleanCount, attribute);
        if (failCount > 0) {
            CommandUtility.formatln(env, "Failed to clean %d %s.", failCount, attribute);
        }

        return ShellStatus.CONTINUE;
    }

    /**
     * Returns an appropriate function for the specified attribute.
     *
     * @param attr function attribute
     * @return a function that matches the specified attribute
     */
    private static Function<Path, Boolean> getFunction(String attr) {
        if ("shortcuts".equalsIgnoreCase(attr)) {
            return FUNC_SHORTCUTS;
        }

        return null;
    }

    /**
     * A {@link SimpleFileVisitor} extended and used to serve the {@link CleanCommand}.
     *
     * @author Mario Bobic
     */
    private static class CleanFileVisitor extends SimpleFileVisitor<Path> {

        /** An environment. */
        private Environment environment;
        /** A path consumer function. */
        private Function<Path, Boolean> function;

        /** Number of files this visitor has encountered. */
        private int cleanCount = 0;
        /** Number of files or directories that could not be removed. */
        private int failCount = 0;

        /**
         * Constructs an instance of this class with the specified environment and function.
         *
         * @param environment an environment
         * @param function path consumer function
         */
        public CleanFileVisitor(Environment environment, Function<Path, Boolean> function) {
            this.environment = environment;
            this.function = function;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            try {
                boolean applied = function.apply(file);
                if (applied) {
                    cleanCount++;
                    environment.writeln("Cleaned " + file);
                }
            } catch (UncheckedIOException e) {
                failCount++;
                environment.writeln("Failed to clean " + file);
            }

            return FileVisitResult.CONTINUE;
        }

        /**
         * Returns the number of files this visitor has cleaned.
         *
         * @return the number of files this visitor has cleaned
         */
        public int getCleanCount() {
            return cleanCount;
        }

        /**
         * Returns the number of files that could not be cleaned.
         *
         * @return the number of files that could not be cleaned
         */
        public int getFailCount() {
            return failCount;
        }

    }

}
