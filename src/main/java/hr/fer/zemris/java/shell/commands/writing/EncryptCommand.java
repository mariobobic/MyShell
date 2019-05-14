package hr.fer.zemris.java.shell.commands.writing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Crypto;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import javax.crypto.BadPaddingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static hr.fer.zemris.java.shell.utility.CommandUtility.promptConfirm;

/**
 * A command that is used for encrypting files with AES cryptoalgorithm. The
 * first argument for this command is a password in plain text, which is hashed
 * and turned into a key. The second argument is the file to be encrypted. The
 * file name may be given without quotation marks even if it contains
 * whitespaces because it is the last argument.
 *
 * @author Mario Bobic
 */
public class EncryptCommand extends AbstractCommand {

    /**
     * Constructs a new command object of type {@code EncryptCommand}.
     */
    public EncryptCommand() {
        super("ENCRYPT", createCommandDescription());
    }

    @Override
    public String getCommandSyntax() {
        return "<password> <filename>";
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
        desc.add("Encrypts a file with AES cryptoalgorithm.");
        desc.add("The first argument is a password given in plain text, "
                + "which is hashed and turned into a key.");
        desc.add("The second argument is the file to be encrypted.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        String[] args = StringUtility.extractArguments(s, 2);
        if (args.length != 2) {
            throw new SyntaxException();
        }

        Path srcFile = Utility.resolveAbsolutePath(env, args[1]);
        Utility.requireFile(srcFile);

        Path dstFile = Paths.get(srcFile + Utility.CRYPT_FILE_EXT);
        Utility.requireDiskSpace(Crypto.postSize(srcFile), dstFile);

        if (Files.exists(dstFile)) {
            if (!promptConfirm(env, "File " + dstFile + " already exists. Overwrite?")) {
                env.writeln("Cancelled.");
                return ShellStatus.CONTINUE;
            }
        }

        String hash = Utility.generatePasswordHash(args[0]);
        Crypto crypto = new Crypto(hash, Crypto.ENCRYPT);

        try {
            crypto.execute(srcFile, dstFile, env);
        } catch (BadPaddingException ignorable) {
            // ignored, since crypto is in encryption mode
        }

        return ShellStatus.CONTINUE;
    }

}
