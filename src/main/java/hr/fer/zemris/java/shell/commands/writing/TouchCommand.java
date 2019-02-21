package hr.fer.zemris.java.shell.commands.writing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A command that is used to update the access date and modification date of a
 * file or directory. In its default usage, it is the equivalent of creating or
 * opening a file and saving it without any change to the file contents.
 * <p>
 * Touch eliminates the unnecessary steps of opening the file, saving the file,
 * and closing the file again. Instead it simply updates the dates associated
 * with the file or directory. An updated access or modification date can be
 * important for a variety of other programs such as backup utilities or the
 * make command-line interface programming utility. Typically these types of
 * programs are only concerned with files which have been created or modified
 * after the program was last run.
 * <p>
 * Touch can also be useful for quickly creating files for programs or scripts
 * that require a file with a specific name to exist for successful operation of
 * the program, but do not require the file to have any specific content.
 *
 * @author Mario Bobic
 */
public class TouchCommand extends AbstractCommand {

    /**
     * Constructs a new command object of type {@code TouchCommand}.
     */
    public TouchCommand() {
        super("TOUCH", createCommandDescription());
    }

    @Override
    public String getCommandSyntax() {
        return "<filename>";
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
        desc.add("Updates the access date and modification date of a file or directory.");
        desc.add("In its default usage, it is the equivalent of creating or opening "
                + "a file and saving it without any change to the file contents.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            throw new SyntaxException();
        }

        Path path = Utility.resolveAbsolutePath(env, s);
        if (!Files.exists(path)) {
            Files.createDirectories(Utility.getParent(path));
            Files.createFile(path);
        } else {
            FileTime now = FileTime.fromMillis(System.currentTimeMillis());
            Files.setLastModifiedTime(path, now);
        }

        return ShellStatus.CONTINUE;
    }

}
