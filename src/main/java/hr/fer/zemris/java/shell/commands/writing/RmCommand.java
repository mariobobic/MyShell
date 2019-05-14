package hr.fer.zemris.java.shell.commands.writing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static hr.fer.zemris.java.shell.utility.CommandUtility.promptConfirm;

/**
 * A command that is used for removing files and non-empty directories.
 *
 * @author Mario Bobic
 */
public class RmCommand extends VisitorCommand {

    /** True if removing directories should be forced. */
    private boolean force;

    /**
     * Constructs a new command object of type {@code RmCommand}.
     */
    public RmCommand() {
        super("RM", createCommandDescription(), createFlagDescriptions());
    }

    @Override
    public String getCommandSyntax() {
        return "<path>";
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
        desc.add("Removes a file or a directory.");
        desc.add("THE REMOVE OPERATION IS IRREVERSIBLE.");
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
        desc.add(new FlagDescription("f", "force", null, "Never prompt before removing."));
        return desc;
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        force = false;

        /* Compile! */
        s = commandArguments.compile(s);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("f", "force")) {
            force = true;
        }

        return super.compileFlags(env, s);
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            throw new SyntaxException();
        }

        if (env.isConnected() && !force) {
            env.writeln("Access denied.");
            return ShellStatus.CONTINUE;
        }

        Path path = Utility.resolveAbsolutePath(env, s);
        Utility.requireExists(path);

        // Require confirmation for directories
        if (Files.isDirectory(path) && !force) {
            boolean confirmed = promptConfirm(env, "Remove directory " + path + "?");
            if (!confirmed) {
                env.writeln("Cancelled.");
                return ShellStatus.CONTINUE;
            }
        }

        RmFileVisitor rmVisitor = new RmFileVisitor(env, path);
        walkFileTree(path, rmVisitor);

        return ShellStatus.CONTINUE;
    }

    /**
     * A {@linkplain SimpleFileVisitor} extended and used to serve the {@linkplain
     * RmCommand}. This file visitor is mostly used to remove non-empty directories.
     *
     * @author Mario Bobic
     */
    private class RmFileVisitor extends SimpleFileVisitor<Path> {

        /** An environment. */
        private final Environment environment;
        /** Starting path. */
        private final Path root;

        /**
         * Constructs an instance of {@code RmFileVisitor} with the specified arguments.
         *
         * @param environment an environment
         * @param start starting file of the tree walker
         */
        public RmFileVisitor(Environment environment, Path start) {
            this.environment = environment;
            this.root = Utility.getParent(start);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            Path relative = root.relativize(file);
            try {
                Files.delete(file);
                print("Deleted file " + relative);
            } catch (IOException e) {
                print("Failed to delete file " + relative);
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            environment.writeln("Failed to access " + file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            Path relative = root.relativize(dir);

            try {
                Files.delete(dir);
                print("Removed directory " + relative);
            } catch (IOException e) {
                print("Failed to remove directory " + relative);
            }

            return FileVisitResult.CONTINUE;
        }

        /**
         * Prints the specified <tt>message</tt> followed by a new line to the
         * environment if the command is not silent.
         *
         * @param message message to be printed
         */
        private void print(String message) {
            if (!isSilent()) {
                environment.writeln(message);
            }
        }
    }

}
