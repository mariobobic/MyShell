package hr.fer.zemris.java.shell.commands.network;

import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Connection;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.NetworkTransfer;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

/**
 * This command is paired with {@link HostCommand} and {@link ConnectCommand}
 * and is used for downloading content from the host computer.
 *
 * @author Mario Bobic
 */
public class DownloadCommand extends AbstractCommand {

    /**
     * Constructs a new command object of type {@code DownloadCommand}.
     */
    public DownloadCommand() {
        super("DOWNLOAD", createCommandDescription());
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
        desc.add("Downloads content from the host computer.");
        desc.add("This command can only be run when connected to a MyShell host.");
        desc.add("Both files and directories can be downloaded.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (!env.isConnected()) {
            env.writeln("You must be connected to a host to run this command!");
            return ShellStatus.CONTINUE;
        }

        if (s == null) {
            throw new SyntaxException();
        }

        Path path = Utility.resolveAbsolutePath(env, s);
        Utility.requireExists(path);

        // Passed all checks, good to go
        try {
            // Upload from MyShell HOST to MyShell CLIENT
            Connection con = env.getConnection();
            NetworkTransfer.upload(path, con.getInFromClient(), con.getOutToClient(), con.getEncrypto());
            if (Files.isDirectory(path)) {
                System.out.println("Finished uploading " + path);
            }
        } catch (SocketException e) {
            // Connection has ended
            return ShellStatus.TERMINATE;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return ShellStatus.CONTINUE;
    }

}
