package hr.fer.zemris.java.shell.interfaces;

import hr.fer.zemris.java.shell.ShellStatus;

import java.util.List;

/**
 * An interface that is used as a contract for implementing Shell commands.
 *
 * @author Mario Bobic
 */
public interface ShellCommand {

    /**
     * Returns the name of this Shell command.
     *
     * @return the name of this Shell command
     */
    public String getCommandName();

    /**
     * Returns the description of this Shell command.
     *
     * @return the description of this Shell command
     */
    public List<String> getCommandDescription();

    /**
     * Executes the given Shell command. Every shell command has its own unique
     * way of executing. Most Shell commands write out their steps of executing,
     * as error and information messages onto the {@code Environment}.
     * <p>
     * Arguments may or may not exist (String has a valid value or
     * {@code null}). The user is advised to check the implementing class
     * documentation in order to see what this command does.
     *
     * @param env an environment
     * @param s arguments
     * @return the status of this command
     */
    public ShellStatus execute(Environment env, String s);
}
