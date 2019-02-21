package hr.fer.zemris.java.shell.commands.writing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A command that is used for removing empty directories. In order to remove a
 * non-empty directory, use {@linkplain RmCommand}.
 *
 * @author Mario Bobic
 */
public class RmdirCommand extends AbstractCommand {

    /**
     * Constructs a new command object of type {@code RmdirCommand}.
     */
    public RmdirCommand() {
        super("RMDIR", createCommandDescription());
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
        desc.add("Removes a directory.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) {
        if (s == null) {
            throw new SyntaxException();
        }

        Path dir = Utility.resolveAbsolutePath(env, s);
        if (!Files.isDirectory(dir)){
            env.writeln(dir.getFileName() + " is not a directory.");
        } else {
            try {
                Files.delete(dir);
                env.writeln("Removed " + dir);
            } catch (IOException e) {
                env.writeln("The directory must be empty in order to be removed. Use RM instead.");
            }
        }

        return ShellStatus.CONTINUE;
    }

}
