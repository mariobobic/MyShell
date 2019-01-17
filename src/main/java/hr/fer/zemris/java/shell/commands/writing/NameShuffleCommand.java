package hr.fer.zemris.java.shell.commands.writing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

/**
 * A command that is used for shuffling file names. Argument must be an existing
 * directory.
 *
 * @author Mario Bobic
 */
public class NameShuffleCommand extends AbstractCommand {

    /** Prefix used for temporarily renaming files. */
    private static final String RENAMING_PREFIX = "__temp-";

    /**
     * Constructs a new command object of type {@code NameShuffleCommand}.
     */
    public NameShuffleCommand() {
        super("NAMESHUFFLE", createCommandDescription());
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
        desc.add("Shuffles the file names of a specified directory.");
        desc.add("Argument must be an existing directory.");
        desc.add("USE THIS COMMAND WITH CAUTION!");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            throw new SyntaxException();
        }

        Path path = Utility.resolveAbsolutePath(env, s);
        Utility.requireDirectory(path);

        /* Create an original list of files and keep it for later printing. */
        List<Path> originalListOfFiles = Files.list(path).collect(Collectors.toList());

        /* Check if the directory was empty. */
        if (originalListOfFiles.size() == 0) {
            env.writeln("There are no files in the specified directory.");
            return ShellStatus.CONTINUE;
        }

        /* Create a list of file names and shuffle it. */
        List<String> listOfFileNames = originalListOfFiles.stream()
                .map(Path::getFileName)
                .map(String::valueOf)
                .collect(Collectors.toList());
        Collections.shuffle(listOfFileNames);

        /* Temporarily rename all files. */
        for (int i = 0, n = originalListOfFiles.size(); i < n; i++) {
            Path tempFile = path.resolve(RENAMING_PREFIX + Integer.toString(i) + ".tmp");
            Files.move(originalListOfFiles.get(i), tempFile);
        }

        /* Make a list of temporary files. */
        List<Path> listOfTempFiles = Files.list(path).collect(Collectors.toList());

        /* Start shuffle-renaming. */
        for (int i = 0, n = listOfTempFiles.size(); i < n; i++) {
            Path originalFile = originalListOfFiles.get(i);
            Path tempFile = listOfTempFiles.get(i);
            Path renamingFile = path.resolve(listOfFileNames.get(i));

            Files.move(tempFile, renamingFile);

            env.writeln(originalFile.getFileName() + " renamed to " + renamingFile.getFileName());
        }

        return ShellStatus.CONTINUE;
    }

}
