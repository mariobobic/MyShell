package hr.fer.zemris.java.shell.commands.system;

import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * Lists the history of inputs entered by the user. A single user input is
 * written per line.
 *
 * @author Mario Bobic
 */
public class HistoryCommand extends AbstractCommand {

    /**
     * Constructs a new command object of type {@code HistoryCommand}.
     */
    public HistoryCommand() {
        super("HISTORY", createCommandDescription());
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
        desc.add("Lists the history of user inputs.");
        desc.add("A single user input is written per line.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) {
        env.getHistory().forEach(input -> env.writeln(input));

        return ShellStatus.CONTINUE;
    }

}
