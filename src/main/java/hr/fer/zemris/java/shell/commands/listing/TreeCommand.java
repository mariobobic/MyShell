package hr.fer.zemris.java.shell.commands.listing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.CommandUtility;
import hr.fer.zemris.java.shell.utility.Utility;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Walks the directory tree from the specified path and prints a whole file tree
 * where each directory level shifts output two characters to the right.
 *
 * @author Mario Bobic
 */
public class TreeCommand extends VisitorCommand {

    /**
     * Constructs a new command object of type {@code TreeCommand}.
     */
    public TreeCommand() {
        super("TREE", createCommandDescription());
    }

    @Override
    public String getCommandSyntax() {
        return "(<path>)";
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
        desc.add("Walks the directory tree from the specified path.");
        desc.add("Prints a whole file tree where each directory level "
                + "shifts output two characters to the right.");
        desc.add("Use tree <path> to walk file tree of a certain directory.");
        desc.add("Use tree to walk file tree of the current directory.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        Path path = s == null ?
            env.getCurrentPath() : Utility.resolveAbsolutePath(env, s);

        Utility.requireDirectory(path);

        /* Passed all checks, start working. */
        TreeFileVisitor treeVisitor = new TreeFileVisitor(env);
        walkFileTree(path, treeVisitor);

        return ShellStatus.CONTINUE;
    }

    /**
     * A {@linkplain FileVisitor} implementation that is used to serve the
     * {@linkplain TreeCommand}. This method prints out the directory tree.
     *
     * @author Mario Bobic
     */
    private class TreeFileVisitor implements FileVisitor<Path> {

        /** The level in relation to root that is currently being visited. */
        private int level;

        /** An environment. */
        private Environment environment;

        /**
         * Constructs an instance of TreeFileVisitor with the specified
         * <tt>environment</tt>.
         *
         * @param environment an environment
         */
        public TreeFileVisitor(Environment environment) {
            this.environment = environment;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (level == 0) {
                environment.writeln(dir.normalize().toAbsolutePath());
            } else {
                print(dir);
            }

            level++;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            print(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            environment.writeln("Failed to access " + file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            level--;
            return FileVisitResult.CONTINUE;
        }

        /**
         * Prints out the specified <tt>path</tt> to the environment, with
         * leading spaces based on the current <tt>level</tt>.
         *
         * @param path path to be written out
         */
        private void print(Path path) {
            environment.write(spaces(level)+path.getFileName());
            CommandUtility.markAndPrintNumber(environment, path);
        }

        /**
         * Returns <tt>2*amount</tt> of spaces in a single line.
         *
         * @param amount the amount times two of spaces to be returned
         * @return the specified amount times two of spaces in a string
         */
        private String spaces(int amount) {
            char[] chars = new char[2*amount];
            Arrays.fill(chars, ' ');
            return new String(chars);
        }

    }

}
