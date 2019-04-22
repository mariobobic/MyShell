package hr.fer.zemris.java.shell.utility;

import hr.fer.zemris.java.shell.commands.listing.CountCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.exceptions.IllegalPathException;
import hr.fer.zemris.java.shell.utility.exceptions.NotEnoughDiskSpaceException;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A helper class. Provides helper methods mostly for Path manipulation, but
 * with various other functionalities.
 *
 * @author Mario Bobic
 */
public abstract class Utility {

    /** Extension used for encrypted files. */
    public static final String CRYPT_FILE_EXT = ".crypt";
    /** Extension used for zipped files. */
    public static final String ZIP_FILE_EXT = ".zip";

    /** Path to user home directory. */
    public static final String USER_HOME = System.getProperty("user.home");
    /** Shorthand abbreviation for home directory. */
    private static final String HOME_DIR = "~";

    // Files.isSymbolicLink() returns false for shortcuts on Windows. To know
    // if a file is a shortcut on Windows, we need to check the extension.
    /** Extension for filesystem shortcuts (symbolic links) on Windows. */
    private static final String WINDOWS_SHORTCUT_SUFFIX = ".lnk";

    /** Most commonly used date format. */
    private static final DateFormat STANDARD_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Disable instantiation or inheritance.
     */
    private Utility() {
    }

    /**
     * Resolves the given path by checking if it's relative or absolute. If the
     * {@code str} path parameter is an absolute path then this method trivially
     * returns the given path. In the simplest case, the given path does not
     * have a root component, in which case this method joins the given path
     * with the root and returns the absolute path. If the given path has
     * invalid characters an {@code InvalidPathException} is thrown.
     * <p>
     * The specified string <tt>str</tt> may also be an integer, in which case
     * it is first checked if a file marked with that number exists.
     *
     * @param env an environment
     * @param str the given path string
     * @return the absolute path of the given path
     * @throws InvalidPathException if string cannot be converted into a Path
     */
    public static Path resolveAbsolutePath(Environment env, String str) {
        /* If the entered argument is parsable as an integer,
         * see if a path is marked with that number. */
        if (isInteger(str)) {
            int num = Integer.parseInt(str);
            Path path = env.getMarked(num);
            if (path != null) return path;
        }

        /* Paths.get() may throw an exception. */
        str = str.replace("\"", "");
        Path path = Paths.get(str);

        /* If it starts with a tilde, recurse back to this method with user.home. */
        if (path.startsWith(HOME_DIR)) {
            String home = System.getProperty("user.home");
            String rest = str.substring(HOME_DIR.length());
            return resolveAbsolutePath(env, home+rest);
        }

        if (!path.isAbsolute()) {
            path = env.getCurrentPath().resolve(path);
        }
        path = path.normalize();

        return path;
    }

    /**
     * Returns the name of the file or directory denoted by this path as a
     * {@code Path} object. The file name is the <em>farthest</em> element from
     * the root in the directory hierarchy.
     * <p>
     * This method is convenient when <tt>path</tt> is a root directory, because
     * it trivially returns the given path instead of {@code null}.
     *
     * @param path path whose file name is to be returned
     * @return a path representing the name of the file or directory, or
     *         {@code null} if this path has zero elements
     */
    public static Path getFileName(Path path) {
        return path.equals(path.getRoot()) ? path : path.getFileName();
    }

    /**
     * Returns the <em>parent path</em> of the specified <tt>path</tt>.
     * <p>
     * The parent of this path object consists of this path's root component, if
     * any, and each element in the path except for the <em>farthest</em> from
     * the root in the directory hierarchy. This method does not access the file
     * system; the path or its parent may not exist.
     * <p>
     * This method is convenient when <tt>path</tt> is a root directory, because
     * it trivially returns the given path instead of {@code null}.
     *
     * @param path path whose parent is to be returned
     * @return a path representing the path's parent
     */
    public static Path getParent(Path path) {
        return path.equals(path.getRoot()) ? path : path.getParent();
    }

    /**
     * Returns the user home directory path.
     * <p>
     * The user home directory is fetched using the
     * {@link System#getProperty(String)} method.
     *
     * @return the user home directory path
     */
    public static Path getUserHomeDirectory() {
        return Paths.get(USER_HOME);
    }

    /**
     * Returns the user Downloads directory path.
     *
     * @return the user Downloads directory path
     * @see #getUserHomeDirectory()
     */
    public static Path getUserDownloadsDirectory() {
        return getUserHomeDirectory().resolve("Downloads");
    }

    /**
     * Tells whether or not a file is considered <em>hidden</em>. The exact
     * definition of hidden is platform or provider dependent. On UNIX for
     * example a file is considered to be hidden if its name begins with a
     * period character ('.'). On Windows a file is considered hidden if the DOS
     * {@link DosFileAttributes#isHidden hidden} attribute is set.
     * <p>
     * Depending on the implementation this method may require to access the
     * file system to determine if the file is considered hidden.
     *
     * @param path the path to the file to be tested
     * @return {@code true} if the file is considered hidden
     * @throws SecurityException In the case of the default provider, and a
     *         security manager is installed, the
     *         {@link SecurityManager#checkRead(String) checkRead} method is
     *         invoked to check read access to the file.
     */
    public static boolean isHidden(Path path) {
        if (path.equals(path.getRoot())) {
            return false;
        }

        return path.toFile().isHidden();
    }

    /**
     * Returns <tt>true</tt> if the specified <tt>pathname</tt> string is a
     * valid {@code Path}. In other words, return true if the the specified
     * pathname can be obtained as a {@code Path} object using the
     * {@link Paths#get(String, String...) Paths.get(pathname)} method.
     *
     * @param pathname pathname to be tested
     * @return true if pathname is a valid path
     */
    public static boolean isValidPath(String pathname) {
        try {
            Paths.get(pathname);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lists all files in the specified dir, sorted by natural ordering and
     * returns the result as a list of {@link Path} objects.
     *
     * @param dir directory of which a list of files will be returned
     * @return list of all files and directories inside <tt>dir</tt>
     * @throws java.nio.file.NotDirectoryException if path is not a directory
     * @throws IOException if an I/O error occurs when opening the directory
     */
    public static List<Path> listFilesSorted(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.sorted().collect(Collectors.toList());
        }
    }

    /**
     * Returns the first available {@code Path} with a unique file name. The
     * first available path means that, if a file with the specified
     * <tt>path</tt> exists on disk, an index is appended to it. If a file with
     * that path still exists, index is incremented and so on, until a unique
     * path is generated. This path is then returned.
     * <p>
     * If a file with the specified <tt>path</tt> does not exist, this method
     * trivially returns <tt>path</tt>.
     * <p>
     * For an example, if the parent directory of the specified path already
     * contains <tt>file.txt</tt>, <tt>file-0.txt</tt> and <tt>file-1.txt</tt>,
     * and the file name of this path is <tt>file.txt</tt>, then a path with
     * file name <tt>file-2.txt</tt> is returned.
     *
     * @param path path from which the first available is returned
     * @return a path with a unique file name
     */
    public static Path firstAvailable(Path path) {
        if (!Files.exists(path))
            return path;

        int namingIndex = 0;
        String name = getFileName(path).toString();
        String extension = "";

        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = name.substring(dotIndex);
            name = name.substring(0, dotIndex);
        }
        name += "-";

        while (Files.exists(path)) {
            path = path.resolveSibling(name + namingIndex + extension);
            namingIndex++;
        }
        return path;
    }

    /**
     * Returns the file name extension of the specified <tt>path</tt>. Extension
     * is considered as the last period symbol in the file name that may be
     * followed by sequence of characters.
     * <p>
     * If the specified path does not have an extension, or in other words if
     * its file name does not contain a period symbol, the extension is
     * considered non-existent and an empty string is returned.
     * <p>
     * If the file name ends with a period symbol, the period is returned as an
     * extension.
     *
     * @param path path whose extension is to be determined
     * @return the file name extension of the specified path
     */
    public static String extension(Path path) {
        String name = path.getFileName().toString();
        String extension = "";

        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = name.substring(dotIndex);
        }

        return extension;
    }

    /**
     * Checks if the specified <tt>path</tt> exists . This method is designed
     * primarily for doing parameter validation in methods and constructors, as
     * demonstrated below:
     * <blockquote><pre>
     * public Foo(Path path) {
     *     this.path = Helper.requireExists(path);
     * }
     * </pre></blockquote>
     *
     * @param path path to be checked
     * @return <tt>path</tt> if it exists
     * @throws IllegalPathException if path does not exist
     */
    public static Path requireExists(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalPathException("The system cannot find the path specified: " + path);
        }

        return path;
    }

    /**
     * Checks if the specified <tt>path</tt> exists and is a directory. This
     * method is designed primarily for doing parameter validation in methods
     * and constructors, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Path path) {
     *     this.path = Helper.requireDirectory(path);
     * }
     * </pre></blockquote>
     *
     * @param path path to be checked
     * @return <tt>path</tt> if it exists and is a directory
     * @throws IllegalPathException if path does not exist or is not a directory
     */
    public static Path requireDirectory(Path path) {
        requireExists(path);
        if (!Files.isDirectory(path)) {
            throw new IllegalPathException("The specified path must be a directory: " + path);
        }

        return path;
    }

    /**
     * Checks if the specified <tt>path</tt> exists and is a regular file.
     * This method is designed primarily for doing parameter validation in
     * methods and constructors, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Path path) {
     *     this.path = Helper.requireFile(path);
     * }
     * </pre></blockquote>
     *
     * @param path path to be checked
     * @return <tt>path</tt> if it exists and is a regular file
     * @throws IllegalPathException if path does not exist or is not a file
     */
    public static Path requireFile(Path path) {
        requireExists(path);
        if (!Files.isRegularFile(path)) {
            throw new IllegalPathException("The specified path must be a file: " + path);
        }

        return path;
    }

    /**
     * Checks if there is enough space on disk to store the specified amount of
     * <tt>bytes</tt> to the file store where the specified <tt>path</tt> is
     * located.
     * <p>
     * It is not a guarantee that it is possible to use most or any of the
     * specified bytes even if this method does not throw an
     * {@code IOException}. The number of usable bytes is most likely to be
     * accurate immediately after the space attributes are obtained. It is
     * likely to be made inaccurate by any external I/O operations including
     * those made on the system outside of this Java virtual machine.
     *
     * @param bytes amount of free disk space that is required
     * @param path path to where the disk space is required
     * @throws NotEnoughDiskSpaceException if there is not enough disk space
     * @throws IOException if an I/O exception occurs on file store operations
     */
    public static void requireDiskSpace(long bytes, Path path) throws IOException {
        long usable = Files.getFileStore(path.getRoot()).getUsableSpace();
        if (usable < bytes) {
            String info = "Required: " + bytes + " bytes. Available: " + usable + " bytes.";
            throw new NotEnoughDiskSpaceException("There is not enough space on the disk. " + info);
        }
    }

    /**
     * Returns the real path of the specified symbolic link or Windows shortcut
     * file. If the specified path does not exist or is not a readable symbolic
     * link or shortcut, the given path is returned.
     *
     * @param link link of which the real path is to be returned
     * @return the real path of the given link, or that link if it is unreadable
     */
    public static Path getRealPathFromLink(Path link) {
        try {
            if (Files.isSymbolicLink(link)) {
                return Files.readSymbolicLink(link);
            } else if (isWindowsShortcut(link)) {
                return getWindowsShortcutRealPath(link);
            }
        } catch (IOException e) {
            // invalid link or shortcut
        }

        return link;
    }

    /**
     * Returns true if the file is a Windows shortcut, other known as Windows link.
     * The result is true if the file name ends with .lnk extension.
     *
     * @param file file to be checked
     * @return true if the file is a Windows shortcut
     */
    public static boolean isWindowsShortcut(Path file) {
        String filename = file.getFileName().toString();
        return filename.toLowerCase().endsWith(WINDOWS_SHORTCUT_SUFFIX);
    }

    /**
     * Consult {@link #isWindowsShortcut(Path)} first.
     *
     * Returns the real path of the specified Windows shortcut file. If the shortcut
     * file is not in valid format, the given file is simply returned. If the given
     * file can not be read, an IOException is thrown.
     *
     * @param shortcutFile file of which the real path is to be returned
     * @return the real path of the given Windows shortcut file, or this file
     * @throws IOException if the file does not exist or is not readable
     */
    public static Path getWindowsShortcutRealPath(Path shortcutFile) throws IOException {
        try {
            WindowsShortcut shortcut = new WindowsShortcut(shortcutFile.toFile());
            return Paths.get(shortcut.getRealFilename());
        } catch (ParseException e) {
            // not a valid Windows shortcut
            return shortcutFile;
        }
    }

    /**
     * Creates a directory hierarchy if the specified <tt>path</tt> ends with a slash or
     * the default system file separator, if the path doesn't exist. Returns true if
     * directories were created, false otherwise.
     *
     * @param path path to be checked, and directory to be created if it ends
     *             with a file separator and does not yet exist
     * @return true if directories were created, false otherwise
     * @throws IOException if an I/O error occurs while creating directories
     * @throws InvalidPathException if path ends with a file separator, but the
     *                              path string cannot be converted to a Path
     */
    public static boolean createDirectoriesIfPathEndsWithFileSeparator(String path, Path dir) throws IOException, InvalidPathException {
        if (path.endsWith("/") || path.endsWith(File.separator)) {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a string representation of a single file or directory specified
     * by the <tt>path</tt>. The path is written in four columns:
     * <ol>
     * <li>The first column indicates if current object is directory (d),
     * readable (r), writable (w) and executable (x).
     * <li>The second column contains object size in bytes that is right aligned
     * and occupies 10 characters.
     * <li>The third column shows file creation date and time, where the
     * date format is specified by the {@link DateFormat} class.
     * <li>The fourth column contains the name of the file or directory.
     * </ol>
     *
     * Example of a directory string if all arguments except <tt>cleanOutput</tt> are true:
     * <blockquote><pre>
     *     drwx    20.0 MiB 2019-04-03 20:49:50 testdir (1/0)
     * </pre></blockquote>
     *
     * Example of a couple of file string if only <tt>cleanOutput</tt> is true:
     * <blockquote><pre>
     *     testdir00
     *     testdir01
     *     testdir02
     *     testdir03
     *     testdir04
     * </pre></blockquote>
     *
     * @param path path to be written
     * @param humanReadable if file size should be in human readable byte count
     * @param directorySize if directory size should be calculated
     * @param cleanOutput if only file name should be included
     * @param countFiles if number of files and directories should be included
     * @return a string representation of a single file or directory
     * @throws IOException if an I/O error occurs when reading the path
     */
    public static String getFileString(Path path, DateFormat dateFormat, boolean humanReadable, boolean directorySize,
                                       boolean cleanOutput, boolean countFiles) throws IOException {
        StringBuilder sb = new StringBuilder();

        if (!cleanOutput) {
            /* First column */
            sb.append(String.format("%c%c%c%c",
                    Files.isDirectory(path) ? 'd' : '-',
                    Files.isReadable(path) ? 'r' : '-',
                    Files.isWritable(path) ? 'w' : '-',
                    Files.isExecutable(path) ? 'x' : '-'
            )).append(" ");

            /* Second column */
            long size = directorySize ? calculateSize(path) : Files.size(path);
            sb.append(!humanReadable ?
                    String.format("%11d", size) :
                    String.format("%11s", humanReadableByteCount(size))
            ).append(" ");

            /* Third column */
            BasicFileAttributeView faView = Files.getFileAttributeView(
                    path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS
            );
            BasicFileAttributes attributes = faView.readAttributes();

            FileTime fileTime = attributes.creationTime();
            String formattedDateTime = dateFormat.format(new Date(fileTime.toMillis()));

            sb.append(formattedDateTime).append(" ");
        }

        /* Fourth column */
        sb.append(path.getFileName());

        if (countFiles && Files.isDirectory(path)) {
            /* Fifth column */
            CountCommand.CountFileVisitor countVisitor = new CountCommand.CountFileVisitor();
            Files.walkFileTree(path, countVisitor);

            int files = countVisitor.getFileCount();
            int folders = countVisitor.getFolderCount();
            int fails = countVisitor.getFails();

            sb.append(" ");
            sb.append("(");
            sb.append(files).append("/").append(folders);
            if (fails > 0) {
                sb.append("/").append(fails);
            }
            sb.append(")");
        }

        return sb.toString();
    }

    /**
     * Returns a string representation of a single file or directory specified
     * by the <tt>path</tt>. The path is written in four columns:
     * <ol>
     * <li>The first column indicates if current object is directory (d),
     * readable (r), writable (w) and executable (x).
     * <li>The second column contains object size in bytes that is right aligned
     * and occupies 10 characters.
     * <li>The third column shows file creation date and time, where the
     * date format is specified by the {@link DateFormat} class.
     * <li>The fourth column contains the name of the file or directory.
     * </ol>
     *
     * @param path path to be written
     * @param humanReadable if file size should be in human readable byte count
     * @param directorySize if directory size should be calculated
     * @return a string representation of a single file or directory
     * @throws IOException if an I/O error occurs when reading the path
     * @see #getFileString(Path, DateFormat, boolean, boolean, boolean, boolean)
     */
    public static String getFileString(Path path, boolean humanReadable, boolean directorySize) throws IOException {
        return getFileString(path, STANDARD_DATE_FORMAT, humanReadable, directorySize, false, false);
    }

    /**
     * Converts the number of bytes to a human readable byte count with binary
     * prefixes.
     *
     * @param bytes number of bytes
     * @return human readable byte count with binary prefixes
     */
    public static String humanReadableByteCount(long bytes) {
        /* Use the natural 1024 units and binary prefixes. */
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "kMGTPE".charAt(exp - 1) + "i";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Returns the size, in bytes, of the specified <tt>path</tt>. If the given
     * path is a regular file, trivially its size is returned. Else the path is
     * a directory and its contents are recursively explored, returning the
     * total sum of all files within the directory.
     * <p>
     * If an I/O exception occurs, it is suppressed within this method and
     * <tt>0</tt> is returned as the size of the path it is processing (it may
     * or may not be the specified <tt>path</tt>).
     *
     * @param path path whose size is to be returned
     * @return size of the specified path
     */
    public static long calculateSize(Path path) {
        try {
            if (Files.isRegularFile(path)) {
                return Files.size(path);
            }

            try (Stream<Path> stream = Files.list(path)) {
                return stream.mapToLong(Utility::calculateSize).sum();
            }
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * Parses the specified string <tt>str</tt> as size and returns size in
     * bytes. The given argument may be a human readable byte count with units
     * specified, not necessarily separated by a space. If units are not
     * specified in the string, the parsed <tt>long</tt> value is returned.
     * Argument is case insensitive.
     * <p>
     * Examples of this method usage are the following:
     * <p>
     * <table border="1">
     * <tr><th>Method call</th><th>Result</th></tr>
     * <tr><td>parseSize("2 kiB")</td><td>2048 bytes</td></tr>
     * <tr><td>parseSize("10 MiB")</td><td>10485760 bytes</td></tr>
     * <tr><td>parseSize("1 GiB")</td><td>1073741824 bytes</td></tr>
     * <tr><td>parseSize("16 B")</td><td>16 bytes</td></tr>
     * <tr><td>parseSize("4096")</td><td>4096 bytes</td></tr>
     * <tr><td>parseSize("2.5 kiB")</td><td>2560 bytes</td></tr>
     * <tr><td>parseSize("1kiB")</td><td>1024 bytes</td></tr>
     * <tr><td>parseSize("1MIB")</td><td>1048576 bytes</td></tr>
     * <tr><td><br></td><td></td></tr>
     * <tr><td>parseSize("1 kB")</td><td>1000 bytes</td></tr>
     * <tr><td>parseSize("1 MB")</td><td>1000000 bytes</td></tr>
     * <tr><td>parseSize("1 gb")</td><td>1000000000 bytes</td></tr>
     * <tr><td>parseSize("1gB")</td><td>1000000000 bytes</td></tr>
     * <tr><td>parseSize("1.23456789 GB")</td><td>1234567890 bytes</td></tr>
     * <tr><td><br></td><td></td></tr>
     * <tr><td>parseSize("")</td><td>IllegalArgumentException</td></tr>
     * <tr><td>parseSize("-2 kB")</td><td>IllegalArgumentException</td></tr>
     * <tr><td>parseSize("2..5 kB")</td><td>IllegalArgumentException</td></tr>
     * <tr><td>parseSize("Foo")</td><td>IllegalArgumentException</td></tr>
     * </table>
     *
     * @param s string to be parsed as size
     * @return long value of the parsed size
     * @throws IllegalArgumentException if string can not be parsed as size
     */
    public static long parseSize(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse empty string.");
        }

        /* Trim and replace commas with dots. */
        s = s.trim().replace(",", ".");

        /* Check if it is only a number without a unit. */
        if (isDouble(s)) {
            return (long) Double.parseDouble(s);
        }

        /* Separate digits and alphas - number and unit. */
        StringBuilder numSb = new StringBuilder();
        String unitStr = "";
        for (int i = 0, n = s.length(); i < n; i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                numSb.append(c);
            } else {
                unitStr = s.substring(i).trim();
                break;
            }
        }

        /* Number is composed of digits. Parse it. */
        double num = Double.parseDouble(numSb.toString());

        /* If the unit is bytes, number can be returned immediately. */
        if (unitStr.equalsIgnoreCase("B")) {
            return (long) num;
        }

        /* Get the unit prefix (i.e. k, M, G, ...) and suffix (iB or B). */
        char unitPrefix = unitStr.toUpperCase().charAt(0);
        String unitSuffix = unitStr.substring(1);

        /* Set up the unit if suffix is valid. */
        int unit;
        if (unitSuffix.equalsIgnoreCase("iB")) {
            unit = 1024;
        } else if (unitSuffix.equalsIgnoreCase("B")) {
            unit = 1000;
        } else {
            throw new IllegalArgumentException("Invalid unit: " + unitStr);
        }

        /* Set up the exponent if prefix is valid. */
        int exp = "KMGTPE".indexOf(unitPrefix) + 1;
        if (exp == 0) {
            throw new IllegalArgumentException("Invalid unit: " + unitStr);
        }

        /* Final calculation. */
        return (long) (num * Math.pow(unit, exp));
    }

    /**
     * Converts nanoseconds to a human readable time unit, rounding it to the
     * nearest whole number.
     *
     * @param nanoseconds nanoseconds to be converted
     * @return human readable time unit
     */
    public static String humanReadableTimeUnit(long nanoseconds) {
        final long nanosecond = 1L;
        final long microsecond = nanosecond * 1000L;
        final long milisecond = microsecond * 1000L;
        final long second = milisecond * 1000L;
        final long minute = second * 60L;
        final long hour = minute * 60L;
        final long day = hour * 24L;

        String retVal;
        long remainder = 0L;
        if (nanoseconds < microsecond) {
            retVal = nanoseconds + " ns";
        } else if (nanoseconds < milisecond) {
            retVal = nanoseconds / microsecond + " us";
        } else if (nanoseconds < second) {
            retVal = nanoseconds / milisecond + " ms";
        } else if (nanoseconds < minute) {
            retVal = nanoseconds / second + " s";
        } else if (nanoseconds < hour) {
            retVal = nanoseconds / minute + " min";
            remainder = nanoseconds % minute;
        } else if (nanoseconds < day) {
            retVal = nanoseconds / hour + " hr";
            remainder = nanoseconds % hour;
        } else {
            retVal = nanoseconds / day + " days";
            remainder = nanoseconds % day;
        }

        return retVal + (remainder != 0L ? " "+humanReadableTimeUnit(remainder) : "");
    }

    /**
     * Generates the 40 character long SHA-1 password hash of the user's
     * password by converting the specified <tt>password</tt> to an array of
     * bytes decoded with the {@link StandardCharsets#UTF_8 UTF-8} charset and
     * digested with the hash-algorithm.
     *
     * @param password password to be hashed
     * @return the hash of the specified <tt>password</tt>
     */
    public static String generatePasswordHash(String password) {
        String pass = password.concat("peaches.*");
        byte[] passwordBytes = pass.getBytes(StandardCharsets.UTF_8);

        MessageDigest md;
        try {
            // TODO use a better algorithm (SHA-512?) http://stackoverflow.com/questions/2640566/why-use-sha1-for-hashing-secrets-when-sha-512-is-more-secure
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("Algorithm unavailable (SHA-1)", e);
        }

        return DatatypeConverter.printHexBinary(md.digest(passwordBytes));
    }

    /**
     * Returns a charset object for the named charset, or <tt>null</tt> if a
     * charset with the specified <tt>name</tt> can not be resolved.
     *
     * @param name name of the requested charset; may be either a canonical name
     *        or an alias
     * @return a charset object for the named charset
     */
    public static Charset resolveCharset(String name) {
        try {
            return Charset.forName(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns true if string <tt>s</tt> can be parsed as an <tt>Integer</tt>
     * using the {@linkplain Integer#parseInt(String)} method. False otherwise.
     *
     * @param s the user parameter entry
     * @return true if <tt>s</tt> can be parsed as Integer, false otherwise
     */
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Returns true if string <tt>s</tt> can be parsed as a <tt>Long</tt>
     * using the {@linkplain Long#parseLong(String)} method. False otherwise.
     *
     * @param s the user parameter entry
     * @return true if <tt>s</tt> can be parsed as Long, false otherwise
     */
    public static boolean isLong(String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Returns true if string <tt>s</tt> can be parsed as a <tt>Double</tt>
     * using the {@linkplain Double#parseDouble(String)} method. False otherwise.
     *
     * @param s the user parameter entry
     * @return true if <tt>s</tt> can be parsed as Double, false otherwise
     */
    public static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Returns the first element of the specified <tt>list</tt>. If the list is
     * empty, <tt>null</tt> is returned. This is convenient because it cuts out
     * exception handling.
     *
     * @param <T> type
     * @param list list from which first element is returned
     * @return the first element of the specified list
     */
    public static <T> T firstElement(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Returns the last element of the specified <tt>list</tt>. If the list is
     * empty, <tt>null</tt> is returned. This is convenient because it cuts out
     * exception handling.
     *
     * @param <T> type
     * @param list list from which last element is returned
     * @return the last element of the specified list
     */
    public static <T> T lastElement(List<T> list) {
        return list.isEmpty() ? null : list.get(list.size()-1);
    }

    /**
     * Returns the first argument if it is not <tt>null</tt>, or the second
     * argument if the first one is <tt>null</tt>.
     *
     * @param <T> argument type
     * @param nullable value that may be null
     * @param substitute substitute value, if the first one is null
     * @return <tt>nullable != null ? nullable : substitute</tt>
     */
    public static <T> T ifNull(T nullable, T substitute) {
        return nullable != null ? nullable : substitute;
    }

    /**
     * Returns this computer's local IP address or <tt>null</tt> if the IP
     * address is inaccessible.
     *
     * @return this computer's local IP address or <tt>null</tt>
     */
    // TODO Any better way of obtaining local IP address?
    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface n = en.nextElement();

                Enumeration<InetAddress> ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress addr = ee.nextElement();
                    if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {}
        return null;
    }

    /**
     * Returns the external IP address of the router this computer is connected
     * to. Returns {@code null} if the IP address is inaccessible.
     *
     * @return this computer's public IP address or <tt>null</tt>
     */
    // TODO Any better way of obtaining public IP address?
    public static String getPublicIP() {
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com/");

            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(whatismyip.openStream())
            )) {
                String ip = in.readLine();
                return ip;
            }
        } catch (IOException e) {
            return null;
        }
    }

}
