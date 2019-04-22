package hr.fer.zemris.java.shell.commands.writing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.CommandUtility;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * A command that is used for temporary renaming files. Argument must be
 * an existing directory.
 *
 * @author Mario Bobic
 */
public class TempRenameCommand extends AbstractCommand {

    /** Prefix used for temporarily renaming files. */
    private static final String RENAMING_PREFIX = "__temp-";

    /** Name of the files list backup file. */
    private static final String BACKUP_FILE_NAME = "file-list-backup.txt";

    /**
     * Constructs a new command object of type {@code NameShuffleCommand}.
     */
    public TempRenameCommand() {
        super("TEMPRENAME", createCommandDescription());
    }

    @Override
    public String getCommandSyntax() {
        return "<dir>";
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
        desc.add("Temporarily renames all files of a specified directory.");
        desc.add("Useful when there are a lot of files or directories with accents "
               + "which cannot be processed by some programs.");
        desc.add("Argument must be an existing directory.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            throw new SyntaxException();
        }

        Path path = Utility.resolveAbsolutePath(env, s);
        Utility.requireDirectory(path);

        /* Create an original list of files. */
        List<Path> originalListOfFiles = Utility.listFilesSorted(path);

        /* Check if the directory is empty. */
        int numItems = originalListOfFiles.size();
        if (numItems == 0) {
            env.writeln("There are no files in the specified directory.");
            return ShellStatus.CONTINUE;
        }

        /* Ask for permission to be sure! */
        String message = "Temporarily rename " + numItems + " items in root of " + path + "?";
        boolean agreed = CommandUtility.promptConfirm(env, message);
        if (!agreed) {
            env.writeln("Aborting...");
            return ShellStatus.CONTINUE;
        }

        /* Store a temp backup copy of renames in home directory. */
        Path backupPath = Utility.firstAvailable(env.getHomePath().resolve(BACKUP_FILE_NAME));
        PrintWriter backupWriter = createPrintWriter(backupPath);

        /* Rename root dir content and map tmp-to-original paths. */
        Map<Path, Path> map = new HashMap<>();
        for (Path p : originalListOfFiles) {
            String tmpName = RENAMING_PREFIX + UUID.randomUUID();

            Path parent = Utility.getParent(p);
            Path tmpPath = Utility.firstAvailable(parent.resolve(tmpName));

            try {
                Files.move(p, tmpPath, StandardCopyOption.ATOMIC_MOVE);
                map.put(tmpPath, p);
                env.writeln("  " + p + " -> " + tmpPath);
                if (backupWriter != null)
                    backupWriter.println(p + " -> " + tmpPath);
            } catch (IOException e) {
                env.writeln("  Failed to rename " + p + " to " + tmpPath);
            }
        }
        env.writeln(map.size() + " items temporarily renamed. Please do not rename back manually!");
        if (backupWriter != null)
            backupWriter.close();

        /* Prompt the user to rename back. Give him a few chances. */
        message = "Restore files and dirs to original names?";
        agreed = CommandUtility.promptConfirm(env, message);
        if (!agreed) {
            message = "This is a temporary rename command... Restore back to original names?";
            agreed = CommandUtility.promptConfirm(env, message);
            if (!agreed) {
                message = "Last chance. Restore back to original names?";
                agreed = CommandUtility.promptConfirm(env, message);
                if (!agreed) {
                    env.writeln("Old file names are backed up at " + backupPath);
                    return ShellStatus.CONTINUE;
                }
            }
        }

        /* Rename everything back to original. */
        try (Stream<Path> stream = Files.list(path)) {
            stream.forEach(tmpPath -> {
                Path original = map.get(tmpPath);
                if (original == null) {
                    env.writeln("  No previous mapping for: " + tmpPath);
                    return;
                }

                try {
                    Files.move(tmpPath, original, StandardCopyOption.ATOMIC_MOVE);
                    env.writeln("  " + tmpPath + " -> " + original);
                } catch (IOException e) {
                    env.writeln("  Failed to rename " + tmpPath + " to " + original);
                }
            });
        }

        Files.delete(backupPath);
        return ShellStatus.CONTINUE;
    }

    /**
     * Returns a {@link PrintWriter} object for the given path, or <tt>null</tt>
     * if an I/O error occurs.
     *
     * @param file destination file
     * @return a print writer for the given file
     */
    private static PrintWriter createPrintWriter(Path file) {
        try {
            return new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
    }

}
