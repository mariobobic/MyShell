package hr.fer.zemris.java.shell.commands.listing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static hr.fer.zemris.java.shell.utility.CommandUtility.markAndPrintNumber;

/**
 * Walks the directory tree from the given path, or current path if no path is
 * entered. Prints out the given maximum quantity of files, or less if that many
 * files are not found, to the current environment. If no quantity is given, the
 * quantity is set to 10 by default.
 * <p>
 * Currently supported arguments for file querying are <strong>largest</strong>,
 * <strong>smallest</strong>, <strong>newest</strong>, <strong>oldest</strong>.
 *
 * @author Mario Bobic
 */
public class ShowCommand extends VisitorCommand {

    /** Default amount of files to be shown. */
    private static final int DEFAULT_QUANTITY = 10;

    /** The standard date-time formatter. */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /** A comparator that compares files by their size, smallest first. */
    private static final Comparator<Path> COMP_SMALLEST = Comparator.comparingLong(ShowCommand::size);

    /** A comparator that compares files by their modification date, oldest first. */
    private static final Comparator<Path> COMP_OLDEST = Comparator.comparing(ShowCommand::lastModified);

    /* Flags */
    /** Amount of files to be shown. */
    private int count;
    /** True if only directories should be matched against the pattern. */
    private boolean directoriesOnly;

    /**
     * Constructs a new command object of type {@code LargestCommand}.
     */
    public ShowCommand() {
        super("SHOW", createCommandDescription(), createFlagDescriptions());
    }

    @Override
    public String getCommandSyntax() {
        return "<largest|smallest|newest|oldest> (<path>)";
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
        desc.add("Displays files ordered by a specified attribute in the directory tree.");
        desc.add("There are four supported arguments for file querying: largest, smallest, newest and oldest.");
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
        desc.add(new FlagDescription("n", "count", "count", "Amount of files to be shown."));
        desc.add(new FlagDescription("d", null, null, "Compare directories instead of files."));
        return desc;
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        count = DEFAULT_QUANTITY;

        /* Compile! */
        s = commandArguments.compile(s);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("n", "count")) {
            count = commandArguments.getFlag("n", "count").getPositiveIntArgument(false);
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

        String[] args = StringUtility.extractArguments(s, 2);

        /* Resolve path from the second argument, if present. */
        Path dir;
        if (args.length == 1) {
            dir = env.getCurrentPath();
        } else {
            dir = Utility.resolveAbsolutePath(env, args[1]);
        }

        Comparator<Path> comparator = getComparator(args[0]);
        if (comparator == null) {
            env.writeln("Unknown argument: " + args[0]);
            return ShellStatus.CONTINUE;
        }

        /* Make necessary checks. */
        Utility.requireDirectory(dir);

        ShowFileVisitor largestVisitor = new ShowFileVisitor(comparator);
        walkFileTree(dir, largestVisitor);

        /* Clear previously marked paths. */
        env.clearMarks();

        List<Path> largestFiles = largestVisitor.getFiles();
        Map<Path, Long> directorySizes = largestVisitor.getDirectorySizes();
        for (Path f : largestFiles) {
            long size = directoriesOnly ? directorySizes.get(f) : size(f);
            String bytes = " (" + Utility.humanReadableByteCount(size) + ")";
            String modTime = " (" + FORMATTER.format(lastModified(f).toInstant()) + ")";
            env.write(f.normalize() + bytes + modTime);
            markAndPrintNumber(env, f);
        }

        return ShellStatus.CONTINUE;
    }

    /**
     * Returns a comparator that matches the specified attribute <tt>attr</tt>.
     *
     * @param attr comparison attribute
     * @return a comparator that matches the specified attribute
     */
    private static Comparator<Path> getComparator(String attr) {
        if ("largest".equalsIgnoreCase(attr)) {
            return COMP_SMALLEST.reversed();
        } else if ("smallest".equalsIgnoreCase(attr)) {
            return COMP_SMALLEST;
        } else if ("newest".equalsIgnoreCase(attr)) {
            return COMP_OLDEST.reversed();
        } else if ("oldest".equalsIgnoreCase(attr)) {
            return COMP_OLDEST;
        }

        return null;
    }

    /**
     * Returns the size of a file (in bytes). The size may differ from the
     * actual size on the file system due to compression, support for sparse
     * files, or other reasons. The size of directories is calculated as the
     * sum of sizes of all files that reside in that directory and in
     * subdirectories.
     *
     * @param file the path to the file
     * @return the file size, in bytes
     */
    private static long size(Path file) {
        return Utility.calculateSize(file);
    }

    /**
     * Returns a file's last modified time. If an I/O exception occurs while
     * trying to obtain the file's last modified time,
     * <tt>FileTime.fromMillis(0)</tt> is returned.
     *
     * @param file the path to the file
     * @return a {@code FileTime} representing the time the file was last
     *         modified, or an implementation specific default when a time stamp
     *         to indicate the time of last modification is not supported by the
     *         file system
     */
    private static FileTime lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    /**
     * A {@linkplain SimpleFileVisitor} extended and used to serve the
     * {@linkplain ShowCommand}.
     *
     * @author Mario Bobic
     */
    private class ShowFileVisitor extends SimpleFileVisitor<Path> {

        /** List of largest files in the given directory tree. */
        private final Set<Path> filteredFiles;
        /** Sizes of each directory that was visited, only filled if onlyDirectories == true. */
        private final Map<Path, Long> directorySizes = new HashMap<>();

        /**
         * Initializes a new instance of this class setting the quantity to the
         * desired value.
         *
         * @param comparator comparator used for comparing files
         */
        public ShowFileVisitor(Comparator<Path> comparator) {
            this.filteredFiles = new TreeSet<>(comparator);
        }

        /**
         * Calculates the directory size by accumulating file sizes under the visited directory.
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            if (directoriesOnly) {
                long currentDirSize = directorySizes.getOrDefault(dir, 0L);
                computeDirectorySizes(Utility.getParent(dir), currentDirSize);
                addCandidate(dir);
            }
            return FileVisitResult.CONTINUE;
        }

        /**
         * Adds the file to the list of candidates.
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (directoriesOnly) {
                computeDirectorySizes(Utility.getParent(file), attrs.size());
            } else {
                addCandidate(file);
            }
            return FileVisitResult.CONTINUE;
        }

        /**
         * Continues searching for files.
         */
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
//			environment.writeln("Failed to access " + file);
            return FileVisitResult.CONTINUE;
        }

        private void computeDirectorySizes(Path path, long size) {
            directorySizes.compute(path, (dir2, oldSize) -> oldSize == null ? size : size + oldSize);
        }

        /**
         * Adds candidate to a set of files or directories.
         *
         * @param path candidate file or directory
         */
        private void addCandidate(Path path) {
            filteredFiles.add(path);
        }

        /**
         * Returns the list of largest files in the directory tree.
         *
         * @return the list of largest files in the directory tree
         */
        public List<Path> getFiles() {
            ArrayList<Path> list = new ArrayList<>(count);

            int i = 0;
            for (Path file : filteredFiles) {
                if (i == count) break;
                list.add(file);
                i++;
            }

            return list;
        }

        /**
         * Returns a map of directories as keys and their sizes as values.
         *
         * Directories are stored in this map after a complete walk through
         * the file tree is done. All directories that are subdirectories of
         * the starting directory will be stored in this map, and their sizes
         * calculated based on file sizes that the directories contain.
         *
         * @return a map of directories as keys and their sizes as values
         */
        public Map<Path, Long> getDirectorySizes() {
            return directorySizes;
        }
    }

}
