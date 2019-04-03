package hr.fer.zemris.java.shell.commands.system;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static hr.fer.zemris.java.shell.utility.CommandUtility.markAndPrintNumber;

/**
 * A command that is used for writing out the current contents of a directory.
 * The specified argument must be an existing directory path.
 * <p>
 * While listing directory's contents, alphabetically, this command also writes
 * the attributes of the file it encountered. All attributes will be written in
 * four of the following columns:
 * <ol>
 * <li>The first column indicates if current object is directory (d), readable
 * (r), writable (w) and executable (x).
 * <li>The second column contains object size in bytes that is right aligned and
 * occupies 10 characters.
 * <li>The third column shows file creation date and time with, where the date
 * format is specified by the {@linkplain #DATE_FORMAT}.
 * <li>The fourth column contains the name of the file or directory.
 * </ol>
 *
 * @author Mario Bobic
 */
public class LsCommand extends AbstractCommand {

    /** Date format used for formatting file date attribute. */
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /* Flags */
    /** Indicates if file sizes should be printed in human readable byte count. */
    private boolean humanReadable;
    /** Indicates if directory sizes should be calculated. */
    private boolean directorySize;
    /** Indicates if the given output should be cleared of details. */
    private boolean cleanOutput;
    /** Indicates if number of files and directories should be counted. */
    private boolean countFiles;

    /**
     * Constructs a new command object of type {@code LsCommand}.
     */
    public LsCommand() {
        super("LS", createCommandDescription(), createFlagDescriptions());
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
        desc.add("Lists directory contents.");
        desc.add("While listing directory's contents, alphabetically, "
                + "this command also writes the attributes of the file it encountered.");
        desc.add("Use ls <path> to list contents of a certain directory.");
        desc.add("Use ls to list contents of the current directory.");
        desc.add("Clean output flag removes all details from line output, including file marks.");
        desc.add("Count files flag recursively counts number of files and directories below each directory "
               + "listed by this command. It does not include the listed directory in the count. Shown after "
               + "file name as (numFiles/numDirs), or as (numFiles/numDirs/numErrors) if there are reading errors.");
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
        desc.add(new FlagDescription("h", null, null, "Print human readable sizes (e.g. 1kiB, 256MiB)."));
        desc.add(new FlagDescription("d", null, null, "Calculate size of directories (sum all file sizes)."));
        desc.add(new FlagDescription("c", "clean", null, "Don't print details, use only file name."));
        desc.add(new FlagDescription("n", "count", null, "Count number of files and directories (recursively)."));
        return desc;
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        humanReadable = false;
        directorySize = false;
        cleanOutput = false;
        countFiles = false;

        /* Compile! */
        s = commandArguments.compile(s);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("h")) {
            humanReadable = true;
        }

        if (commandArguments.containsFlag("d")) {
            directorySize = true;
        }

        if (commandArguments.containsFlag("c", "clean")) {
            cleanOutput = true;
        }

        if (commandArguments.containsFlag("n", "count")) {
            countFiles = true;
        }

        return super.compileFlags(env, s);
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        Path path = s == null ?
            env.getCurrentPath() : Utility.resolveAbsolutePath(env, s);

        /* Clear previously marked paths. */
        env.clearMarks();

        /* Print directory contents or single file. */
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                stream.forEachOrdered(file -> {
                    printFile(env, file);
                });
            }
        } else {
            printFile(env, path);
        }

        return ShellStatus.CONTINUE;
    }

    /**
     * Prints out a single file or directory specified by the <tt>path</tt> to
     * the specified environment <tt>env</tt>. The path is printed in four
     * columns:
     * <ol>
     * <li>The first column indicates if current object is directory (d),
     * readable (r), writable (w) and executable (x).
     * <li>The second column contains object size in bytes that is right aligned
     * and occupies 10 characters.
     * <li>The third column shows file creation date and time with, where the
     * date format is specified by the {@linkplain #DATE_FORMAT}.
     * <li>The fourth column contains the name of the file or directory.
     * </ol>
     *
     * @param env environment to where the path attributes are printed
     * @param path path to be printed
     */
    private void printFile(Environment env, Path path) {
        try {
            String fileString = Utility.getFileString(path, DATE_FORMAT, humanReadable, directorySize, cleanOutput, countFiles);
            if (cleanOutput) {
                env.writeln(fileString);
            } else {
                env.write(fileString);
                markAndPrintNumber(env, path);
            }
        } catch (IOException e) {
            env.writeln("An I/O error has occured.");
            env.writeln(e.getMessage());
        }
    }

}
