package hr.fer.zemris.java.shell.commands.writing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Sorts files into directories matching a specified pattern.
 *
 * @author Mario Bobic
 */
public class SortCommand extends AbstractCommand {

    private static final Pattern GROUP_INDEX_PATTERN = Pattern.compile("\\\\\\d+");

    /* Flags */
    /** Indicates if no operations should be performed in this run. */
    private boolean dryRun;

    /**
     * Constructs a new command object of type {@code SortCommand}.
     */
    public SortCommand() {
        super("SORT", createCommandDescription(), createFlagDescriptions());
    }

    @Override
    public String getCommandSyntax() {
        return "(<path>) <regex> <dir_pattern>";
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
        desc.add("Sorts files into directories matching a specified regex pattern.");
        desc.add("Files matching the given regex pattern will be moved into one or multiple directories "
               + "specified by the second argument.");
        desc.add("The destination dir pattern can contain arguments from regex groups within the first argument.");
        desc.add("Say you have hundreds of photos named by pattern yyyyMMdd_HHmmss and you want to sort them by month, "
               + "use: sort . \"^(\\d{4})(\\d{2})\\d{2}_\\d{6}.*\" \"\\1-\\2\"");
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
        desc.add(new FlagDescription("d", "dry-run", null, "Perform no operations in this run."));
        return desc;
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        dryRun = false;

        /* Compile! */
        s = commandArguments.compile(s);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("d", "dry-run")) {
            dryRun = true;
        }

        return super.compileFlags(env, s);
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) {
        if (s == null) {
            throw new SyntaxException();
        }

        /* Possible 1 or 2 arguments, where the second is a pattern
         * that may contain spaces and quotation marks. */
        String[] args = StringUtility.extractArguments(s);

        /* Set source directory, regex for files to be matched and target directory pattern. */
        Path dir;
        String regex;
        String dirPattern;

        if (args.length == 2) {
            dir = env.getCurrentPath();
            regex = args[0];
            dirPattern = args[1];
        } else if (args.length == 3) {
            dir = Utility.resolveAbsolutePath(env, args[0]);
            Utility.requireDirectory(dir);
            regex = args[1];
            dirPattern = args[2];
        } else {
            throw new SyntaxException();
        }

        try {
            Pattern regexPattern = Pattern.compile(regex);
            try (Stream<Path> stream = Files.list(dir)) {
                stream
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            String filename = file.getFileName().toString();
                            Matcher regexMatcher = regexPattern.matcher(filename);
                            if (regexMatcher.matches()) {
                                String destDirName = getDestinationDirName(dirPattern, regexMatcher);
                                moveFile(env, file, destDirName);
                            }
                        });
            }
        } catch (Exception e) {
            env.writeln("Error: " + e.getMessage());
        }

        return ShellStatus.CONTINUE;
    }

    private void moveFile(Environment env, Path file, String destDirName) {
        String fileName = file.getFileName().toString();
        Path destDir = file.resolveSibling(destDirName);
        Path target = destDir.resolve(fileName);

        try {
            if (!dryRun) {
                Files.createDirectories(destDir);
                Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
            }
            env.writeln("Moved: " + file + " -> " + target);
        } catch (FileAlreadyExistsException e) {
            env.writeln("Could not create directory because a file with that name already exists: " + destDir);
        } catch (IOException e) {
            env.writeln("Error while moving file: " + e.getMessage());
        }
    }

    private String getDestinationDirName(String dirPattern, Matcher regexMatcher) {
        Matcher matcher = GROUP_INDEX_PATTERN.matcher(dirPattern);

        String dirName = dirPattern;
        while (matcher.find()) {
            String match = dirPattern.substring(matcher.start(), matcher.end());
            int regexGroup = Integer.parseInt(match.substring(1));

            String replacement = regexMatcher.group(regexGroup);
            dirName = dirName.replace(match, replacement);
        }

        return dirName;
    }

}
