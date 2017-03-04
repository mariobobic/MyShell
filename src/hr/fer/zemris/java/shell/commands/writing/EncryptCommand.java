package hr.fer.zemris.java.shell.commands.writing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Crypto;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

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
	protected String getCommandSyntax() {
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
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		String[] args = Helper.extractArguments(s, 2);
		if (args.length != 2) {
			throw new SyntaxException();
		}
		
		Path sourcefile = Helper.resolveAbsolutePath(env, args[1]);
		Helper.requireFile(sourcefile);
		
		Path destfile = Paths.get(sourcefile + Helper.CRYPT_FILE_EXT);
		if (Files.exists(destfile)) {
			if (!promptConfirm(env, "File " + destfile + " already exists. Overwrite?")) {
				writeln(env, "Cancelled.");
				return CommandStatus.CONTINUE;
			}
		}

		String hash = Helper.generatePasswordHash(args[0]);
		Crypto crypto = new Crypto(hash, Crypto.ENCRYPT);
		
		try {
			crypto.execute(sourcefile, destfile);
		} catch (BadPaddingException ignorable) {
			// ignored, since crypto is in encryption mode
		}

		return CommandStatus.CONTINUE;
	}

}