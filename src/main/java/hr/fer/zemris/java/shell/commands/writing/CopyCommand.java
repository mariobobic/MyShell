package hr.fer.zemris.java.shell.commands.writing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Progress;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.NotEnoughDiskSpaceException;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import static hr.fer.zemris.java.shell.utility.CommandUtility.formatln;
import static hr.fer.zemris.java.shell.utility.CommandUtility.promptConfirm;

/**
 * A command that is used for copying one file and only one file, to another
 * location. The given location must be an absolute path. The destination
 * directory may or may not exist before the copying is done. If the destination
 * directory does not exist, a corresponding directory structure is created. If
 * the last name in the pathname's name sequence is an existing directory, the
 * newly made file will be named as the original file. Else if the last name in
 * the pathname's name sequence is a non-existing directory (a file), the newly
 * made file will be named as it.
 *
 * @author Mario Bobic
 */
public class CopyCommand extends VisitorCommand {

    /* Flags */
    /** Indicates if real files should be copied instead of symbolic links. */
    private boolean followLinks;

    /**
     * Constructs a new command object of type {@code CopyCommand}.
     */
    public CopyCommand() {
        super("COPY", createCommandDescription(), createFlagDescriptions());
    }

    @Override
    public String getCommandSyntax() {
        return "<source_file> <target_path>";
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
        desc.add("Copies one file to another location.");
        desc.add("The first argument must be a source file to be copied, "
                + "whereas the second argument may be either a file or a directory.");
        desc.add("If the second argument is not a directory, it means it is a new file name.");
        desc.add("If the second argument is a directory, a file with the same name is copied into it.");
        desc.add("The destination directory may or may not exist before the copying is done.");
        desc.add("If the destination directory does not exist, a corresponding directory structure is created.");
        desc.add("Directory will be created, and the source copied into it if target ends with a file separator.");
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
        desc.add(new FlagDescription("l", "follow-links", null, "Copy real files instead of symbolic links."));
        return desc;
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        followLinks = false;

        /* Compile! */
        s = commandArguments.compile(s);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("l", "follow-links")) {
            followLinks = true;
        }

        return super.compileFlags(env, s);
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            throw new SyntaxException();
        }

        String[] args = StringUtility.extractArguments(s);
        if (args.length != 2) {
            throw new SyntaxException();
        }

        Path source = Utility.resolveAbsolutePath(env, args[0]);
        Path target = Utility.resolveAbsolutePath(env, args[1]);
        Utility.requireExists(source);

        /* Both paths must be of same type. */
        if (Files.isDirectory(source) && Files.isRegularFile(target)) {
            env.writeln("Can not copy directory onto a file.");
            return ShellStatus.CONTINUE;
        }

        /* If destination path ends with a system separator, assume it's a directory. */
        boolean created = Utility.createDirectoriesIfPathEndsWithFileSeparator(args[1], target);
        if (created) {
            env.writeln("Created directory: " + target);
        }

        /* Passed all checks, start working. */
        if (Files.isRegularFile(source)) {
            copyFile(env, followLinks ? Utility.getRealPathFromLink(source) : source, target);
        } else {
            if (Files.isDirectory(target)) {
                target = target.resolve(source.getFileName());
            }

            CopyFileVisitor copyVisitor = new CopyFileVisitor(env, source, target);
            walkFileTree(source, copyVisitor);
        }

        return ShellStatus.CONTINUE;
    }

    /**
     * Validates both paths and copies <tt>source</tt> to <tt>target</tt>. This
     * method also writes out the full path to the newly created file upon
     * succeeding.
     *
     * @param env an environment
     * @param source the path to file to be copied
     * @param target the path to destination file or directory
     * @throws IllegalArgumentException if <tt>source</tt> is a directory
     * @throws NotEnoughDiskSpaceException if there is not enough disk space
     * @throws IOException if an I/O error occurs
     */
    private void copyFile(Environment env, Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            env.writeln("Source file can not be a directory: " + source);
            return;
        }

        if (source.equals(target)) {
            env.writeln("File cannot be copied onto itself: " + source);
            return;
        }

        if (Files.isDirectory(target)) {
            target = target.resolve(source.getFileName());
        }

        if (Files.exists(target)) {
            if (!promptConfirm(env, "File " + target + " already exists. Overwrite?")) {
                env.writeln("Cancelled.");
                return;
            }
        }

        try {
            Files.createDirectories(target.getParent());
            createNewFile(env, source, target);
            if (!isSilent()) formatln(env, "Copied: %s -> %s", source, target);
        } catch (NotEnoughDiskSpaceException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException("Could not copy " + source + " to " + target + ": " + e.getMessage());
        }
    }

    /**
     * A file creator method. It copies the exact same contents from the
     * <tt>source</tt> to the <tt>target</tt>, creating a new file.
     * <p>
     * Implementation note: creates files using binary streams.
     *
     * @param env an environment
     * @param source an original file to be copied
     * @param target the destination directory
     * @throws IOException if an I/O error occurs
     */
    private static void createNewFile(Environment env, Path source, Path target) throws IOException {
        Utility.requireDiskSpace(Files.size(source), target);

        Progress progress = new Progress(env, Files.size(source), true);
        try (
            BufferedInputStream in = new BufferedInputStream(Files.newInputStream(source));
            BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(target))
        ) {
            int len;
            byte[] buff = new byte[1024];
            while ((len = in.read(buff)) > 0) {
                out.write(buff, 0, len);
                progress.add(len);
            }
        } finally {
            progress.stop();
        }

        // Copy 'Last Modified' timestamp from source to target
        FileTime timestamp = Files.getLastModifiedTime(source);
        Files.setLastModifiedTime(target, timestamp);
    }

    /**
     * A {@linkplain FileVisitor} implementation that is used to serve the
     * {@linkplain CopyCommand}.
     *
     * @author Mario Bobic
     */
    private class CopyFileVisitor extends SimpleFileVisitor<Path> {

        /** An environment. */
        private final Environment environment;

        /** This path's root directory. */
        private final Path root;
        /** Other path's root directory. */
        private final Path otherRoot;

        /**
         * Constructs an instance of {@code CopyFileVisitor} with the specified
         * arguments.
         *
         * @param environment an environment
         * @param root root directory of this file visitor
         * @param otherRoot other path's root directory
         */
        public CopyFileVisitor(Environment environment, Path root, Path otherRoot) {
            this.environment = environment;
            this.root = root;
            this.otherRoot = otherRoot;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path relative = root.relativize(file);
            if (followLinks) {
                file = Utility.getRealPathFromLink(file);
                relative = relative.resolveSibling(file.getFileName());
            }

            Path target = otherRoot.resolve(relative);
            copyFile(environment, file, target);

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            environment.writeln("Failed to access " + file);
            return FileVisitResult.CONTINUE;
        }
    }

}
