package hr.fer.zemris.java.shell.commands.system;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;

/**
 * Writes arguments to the standard output.
 * <p>
 * Displays the argument, separated by a single space character and followed by
 * a newline, on the standard output.
 * <p>
 * This is convenient for showing environment and system variables.
 *
 * @author Mario Bobic
 */
public class EchoCommand extends AbstractCommand {


    /* Flags */
    /** Indicates if a newline symbol should not be appended. */
    private boolean noNewline;
    /** Indicates if a space should be appended instead of newline. */
    private boolean appendSpace;

    /**
     * Constructs a new command object of type {@code EchoCommand}.
     */
    public EchoCommand() {
        super("ECHO", createCommandDescription(), createFlagDescriptions());
    }

    @Override
    public String getCommandSyntax() {
        return "(<argument>)";
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
        desc.add("Writes arguments to the standard output.");
        desc.add("Display the argument, separated by a single space character "
                + "and followed by a newline, on the standard output.");
        desc.add("This is convenient for showing environment and system variables.");
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
        desc.add(new FlagDescription("n", null, null, "Do not append a newline."));
        desc.add(new FlagDescription("s", null, null, "Append space instead of newline."));
        return desc;
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        noNewline = false;

        /* Compile! */
        s = commandArguments.compile(s, false);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("n")) {
            noNewline = true;
        }

        if (commandArguments.containsFlag("s")) {
            appendSpace = true;
        }

        return super.compileFlags(env, s);
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            s = "";
        }

        if (appendSpace) {
            s = s.concat(" ");
        }

        if (noNewline || appendSpace) {
            env.write(s);
        } else {
            env.writeln(s);
        }

        return ShellStatus.CONTINUE;
    }

}
