package hr.fer.zemris.java.shell.commands.system;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple command that is used for clearing the console screen.
 *
 * @author Mario Bobic
 */
public class ClearCommand extends AbstractCommand {

    /** Number of new lines inserted to clear the screen. */
    private static final int COUNT = 100;

    /**
     * Constructs a new command object of type {@code ClearCommand}.
     */
    public ClearCommand() {
        super("CLEAR", createCommandDescription());
    }

    @Override
    public String getCommandSyntax() {
        return "";
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
        desc.add("Clears the console screen.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) {
        for (int i = 0; i < COUNT; i++) {
            env.writeln("");
        }

        return ShellStatus.CONTINUE;
    }

}
