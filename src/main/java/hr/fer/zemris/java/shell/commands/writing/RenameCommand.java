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
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hr.fer.zemris.java.shell.utility.CommandUtility.formatln;

/**
 * A command for renaming files in the desired way using special attributes.
 *
 * @author Mario Bobic
 */
public class RenameCommand extends AbstractCommand {

    /** Pattern used in formatting file names. */
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile("^\\d{8}_\\d{6}");

    /** The file date-time formatter. */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

    /* Flags */
    /** Indicates that the file last modified timestamp should be used. */
    private boolean useTimestamp;

    /**
     * Constructs a new command object of type {@code RenameCommand}.
     */
    public RenameCommand() {
        super("RENAME", createCommandDescription(), createFlagDescriptions());
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
        desc.add("Renames files using special file attributes.");
        desc.add("For example, a file can be renamed to its last modified timestamp with the original name appended.");
        desc.add("If a file is specified, it is simply renamed. If a directory is specified, all files inside will be renamed.");
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
        desc.add(new FlagDescription("t", "use-timestamp", null, "Use last modified timestamp as name."));
        return desc;
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        useTimestamp = false;

        /* Compile! */
        s = commandArguments.compile(s);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("t", "use-timestamp")) {
            useTimestamp = true;
        }

        return super.compileFlags(env, s);
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            throw new SyntaxException();
        }

        String[] args = StringUtility.extractArguments(s, 1);
        if (args.length < 1) {
            throw new SyntaxException();
        }

        Path path = Utility.resolveAbsolutePath(env, args[0]);
        Utility.requireExists(path);

        if (useTimestamp) {
            if (Files.isRegularFile(path)) {
                renameFileUsingLastModified(env, path);
            } else if (Files.isDirectory(path)) {
                Files.list(path).filter(Files::isRegularFile).forEach(file -> {
                    renameFileUsingLastModified(env, file);
                });
            }
        }

        return ShellStatus.CONTINUE;
    }

    private void renameFileUsingLastModified(Environment env, Path file) {
        // Obtain file name
        String filename = file.getFileName().toString();

        // Check if date and time already exist in filename
        Matcher matcher = DATE_TIME_PATTERN.matcher(filename);
        boolean found = matcher.find();
        if (found) {
            env.writeln("Skipped " + file);
            return;
        }

        try {
            // Obtain last modified timestamp and convert to local date and time
            FileTime fileTime = Files.getLastModifiedTime(file);
            long millis = fileTime.toMillis(); // millis from epoch UTC
            Instant instant = Instant.ofEpochMilli(millis);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

            // Format the timestamp and create full file name
            String format = FORMATTER.format(localDateTime);
            String newName = format + " " + filename;

            Path newFile = file.getParent().resolve(newName);
            Files.move(file, newFile, StandardCopyOption.ATOMIC_MOVE);
            formatln(env, "Renamed %s to %s (located in %s)", file.getFileName(), newFile.getFileName(), file.getParent());
        } catch (IOException e) {
            env.writeln("Error occurred while renaming " + file + ": " + e.getMessage());
        }
    }

}
