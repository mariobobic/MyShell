package hr.fer.zemris.java.shell.commands;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.InvalidFlagException;

/**
 * Used as a superclass for Shell commands that implement the Visitor pattern.
 * 
 * @author Mario Bobic
 */
public abstract class VisitorCommand extends AbstractCommand {

    /** Set of paths to be excluded while searching. */
    private Set<Path> excludes;
    /** Indicates if error printing should be suppressed. */
    private boolean silent;

    /**
     * Constructs a new command of a type extending {@code VisitorCommand}, with
     * some pre-defined flag definitions.
     * <ul>
     * <li><strong>exclude</strong> - accepts a path to be excluded from the
     * visitor process. Multiple flags can be given to exclude all specified
     * paths from process.
     * <li><strong>silent</strong> - a boolean flag to suppress errors
     * </ul>
     *
     * @param commandName name of the Shell command
     * @param commandDescription description of the Shell command
     */
    public VisitorCommand(String commandName, List<String> commandDescription) {
        this(commandName, commandDescription, new ArrayList<>());
    }

    /**
     * Constructs a new command of a type extending {@code VisitorCommand}, with
     * some pre-defined flag definitions.
     * <ul>
     * <li><strong>exclude</strong> - accepts a path to be excluded from the
     * visitor process. Multiple flags can be given to exclude all specified
     * paths from process.
     * <li><strong>silent</strong> - a boolean flag to suppress errors
     * </ul>
     *
     * @param commandName name of this Shell command
     * @param commandDescription description of this Shell command
     * @param flagDescriptions flag descriptions of this Shell command
     */
    public VisitorCommand(String commandName, List<String> commandDescription, List<FlagDescription> flagDescriptions) {
        super(commandName, commandDescription, addFlagDescriptions(flagDescriptions));
    }

    /**
     * Adds the visitor flag descriptions to the specified list of
     * {@code FlagDescription} objects.
     *
     * @param flagDescriptions list of flag descriptions to be updated
     * @return the updated list of flag descriptions
     */
    private static List<FlagDescription> addFlagDescriptions(List<FlagDescription> flagDescriptions) {
        flagDescriptions.add(new FlagDescription("e", "exclude", "path", "Exclude a file or directory from the visitor process. May be used multiple times."));
        flagDescriptions.add(new FlagDescription("s", "silent", null, "Suppress error and information printing on command execution."));
        return flagDescriptions;
    }

    /**
     * Walks a file tree.
     * <p>
     * This method works as if invoking it were equivalent to evaluating the
     * expression:
     * <blockquote><pre>
     * walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, visitor)
     * </pre></blockquote>
     * In other words, it does not follow symbolic links, and visits all
     * levels of the file tree.
     * <p>
     * A new {@code FileVisitor} object is created which takes care of flag
     * implementations, like the silent function when a file visit fails and
     * excluded files and directories.
     *
     * @param start the starting
     * @param visitor the file visitor to invoke for each file
     * @return the starting file
     * @throws IOException if an I/O error is thrown by a visitor method
     */
    protected Path walkFileTree(Path start, FileVisitor<? super Path> visitor) throws IOException {
        return Files.walkFileTree(start, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isExcluded(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return visitor.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isExcluded(file)) {
                    return FileVisitResult.CONTINUE;
                }
                return visitor.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                if (isSilent()) {
                    return FileVisitResult.CONTINUE;
                }
                return visitor.visitFileFailed(file, exc);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return visitor.postVisitDirectory(dir, exc);
            }
        });
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        silent = false;
        excludes = new HashSet<>();

        /* Compile! */
        s = commandArguments.compile(s);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("s", "silent")) {
            silent = true;
        }

        if (commandArguments.containsFlag("e", "exclude")) {
            List<String> args = commandArguments.getFlag("e", "exclude").getArguments();
            excludes = args.stream()
                .map(str -> Utility.resolveAbsolutePath(env, str))
                .collect(Collectors.toSet());

            /* If excluding paths are set, but one does not exist. */
            boolean success = true;
            for (Path exclude : excludes) {
                if (!Files.exists(exclude)) {
                    env.writeln("Excluded path " + exclude + " does not exist.");
                    success = false;
                }
            }

            if (!success)
                throw new InvalidFlagException("");
        }

        return super.compileFlags(env, s);
    }

    /**
     * Returns true if the specified <tt>path</tt> should be excluded from the
     * visitor process.
     *
     * @param path path to be checked
     * @return true if path is excluded from the visitor process
     */
    protected boolean isExcluded(Path path) {
        return excludes.contains(path);
    }

    /**
     * Returns true if this command should run in silent mode, or in other words
     * if error printing should be suppressed.
     *
     * @return true if this command should run in silent mode
     */
    protected boolean isSilent() {
        return silent;
    }

}
