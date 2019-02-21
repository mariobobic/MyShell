package hr.fer.zemris.java.shell.commands.system;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lists names of supported charsets for the Java platform where it is executed.
 * This command takes no arguments and a single charset name is written per
 * line.
 *
 * @author Mario Bobic
 */
public class CharsetsCommand extends AbstractCommand {

    /**
     * Constructs a new command object of type {@code CharsetsCommand}.
     */
    public CharsetsCommand() {
        super("CHARSETS", createCommandDescription());
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
        desc.add("Lists names of supported charsets for the Java platform where it is executed.");
        desc.add("This command takes no arguments and a single charset name is written per line.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) {
        Set<String> charsetNames = Charset.availableCharsets().keySet();

        for (String name : charsetNames) {
            env.writeln(name);
        }

        return ShellStatus.CONTINUE;
    }

}
