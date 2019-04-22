package hr.fer.zemris.java.shell.commands.system;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Utility;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A command that is used for changing the current working directory. The given
 * path may be absolute or relative to the current working directory. It can
 * also be entered with quotation marks, which is widely used to interpret files
 * and directories containing whitespaces.
 *
 * @author Mario Bobic
 */
public class CdCommand extends AbstractCommand {

    /**
     * Constructs a new command object of type {@code CdCommand}.
     */
    public CdCommand() {
        super("CD", createCommandDescription());
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
        desc.add("Changes the current directory.");
        desc.add("Use cd <path> to navigate to a certain path.");
        desc.add("Use cd to navigate to MyShell home directory.");
        desc.add("Use cd ~ to navigate to user home directory.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        Path path = (s == null) ?
            env.getStartPath() :
            Utility.resolveAbsolutePath(env, s);

        Utility.requireDirectory(path);

        path = path.toRealPath();
        env.setCurrentPath(path);
        env.writeln("Current directory is now set to " + path);

        return ShellStatus.CONTINUE;
    }

}
