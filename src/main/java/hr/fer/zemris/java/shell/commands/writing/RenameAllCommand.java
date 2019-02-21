package hr.fer.zemris.java.shell.commands.writing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A command that is used for renaming all files and directories with the path
 * and new name specified as command arguments.
 * <p>
 * If the new name is <tt>example</tt> the and there are 101 files to be
 * renamed, the renamed files would be:
 * <blockquote><pre>
 *    example000
 *    example001
 *    ...
 *    example099
 *    example100
 * </pre></blockquote>
 * A start index can also be specified for this command, as well as the position
 * of file index in its file name.
 *
 * @author Mario Bobic
 */
public class RenameAllCommand extends AbstractCommand {

    /** Default numbering start index. */
    private static final int DEFAULT_START_INDEX = 0;

    /* Flags */
    /** Numbering start index. */
    private int startIndex;

    /**
     * Constructs a new command object of type {@code RenameAllCommand}.
     */
    public RenameAllCommand() {
        super("RENAMEALL", createCommandDescription(), createFlagDescriptions());
    }

    @Override
    public String getCommandSyntax() {
        return "<path> <newname>";
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
        desc.add("Renames all files and directories to the new name.");
        desc.add("Optional start index may be included.");
        desc.add("Wildcards can be used to be substituted with some elements in file names:");
        desc.add("Use the {i} sequence to substitute it with a file index.");
        desc.add("Use the {n} sequence to substitute it with the last index.");
        desc.add("Use the {ext} sequence to substitue it with a file extension. "
                + "The extension includes a period symbol.");
        desc.add("USE THIS COMMAND WITH CAUTION!");
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
        desc.add(new FlagDescription("i", "start", "index", "Numbering start index."));
        return desc;
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        startIndex = DEFAULT_START_INDEX;

        /* Compile! */
        s = commandArguments.compile(s);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("i", "start")) {
            startIndex = commandArguments.getFlag("i", "start").getIntArgument();
        }

        return super.compileFlags(env, s);
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            throw new SyntaxException();
        }

        String[] args = StringUtility.extractArguments(s, 2);
        if (args.length < 2) {
            throw new SyntaxException();
        }

        Path path = Utility.resolveAbsolutePath(env, args[0]);
        Utility.requireDirectory(path);

        if (startIndex < 0) {
            env.writeln("The start index must be positive: " + startIndex);
            return ShellStatus.CONTINUE;
        }

        /* Create a sorted list of files in the specified directory. */
        List<Path> listOfFiles = Files.list(path).collect(Collectors.toList());
        Collections.sort(listOfFiles);

        /* Check if the directory was empty. */
        int n = listOfFiles.size();
        if (n == 0) {
            env.writeln("There are no files in the specified directory.");
            return ShellStatus.CONTINUE;
        }

        /* Substitute values. */
        String name = args[1];
        name = name.replace("{n}", Integer.toString(n+startIndex-1));
        boolean containsIndex = name.contains("{i}");

        /* Rename all files. */
        for (int i = 0; i < n; i++) {
            int index = i + startIndex;
            String number = getLeadingZeros(n, startIndex, index) + index;
            String newName = containsIndex ? name.replace("{i}", number) : name+number;

            Path originalFile = listOfFiles.get(i);
            newName = newName.replace("{ext}", Utility.extension(originalFile));

            Path renamingFile = path.resolve(newName);

            try {
                Files.move(originalFile, renamingFile, StandardCopyOption.ATOMIC_MOVE);
                env.writeln(originalFile.getFileName() + " renamed to " + renamingFile.getFileName());
            } catch (Exception e) {
                env.writeln(originalFile.getFileName() + " cannot be renamed to " + newName);
            }
        }

        return ShellStatus.CONTINUE;
    }

    /**
     * Returns a string of zeroes that should be leading the
     * <tt>currentIndex</tt> in relation to <tt>total</tt>.
     *
     * @param total total number of items
     * @param offset index offset
     * @param currentIndex index of the current processing item
     * @return a string of leading zeroes
     */
    private static String getLeadingZeros(int total, int offset, int currentIndex) {
        int decimalPlaces = Integer.toString(total+offset-1).length();
        int numZeroes = decimalPlaces - (Integer.toString(currentIndex).length() % 10);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numZeroes; i++) {
            sb.append('0');
        }
        return sb.toString();
    }

}
