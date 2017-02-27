package hr.fer.zemris.java.shell.commands.reading;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * A command that is used for writing out the contents of a file. This command
 * requires an argument. If the given argument is a directory, an error message
 * is written. This command can write out contents of all kinds of files, but
 * the content is not guaranteed to make any sense for non-text files. A charset
 * may also be provided to this command.
 *
 * @author Mario Bobic
 */
public class CatCommand extends AbstractCommand {

	/**
	 * Constructs a new command object of type {@code CatCommand}.
	 */
	public CatCommand() {
		super("CAT", createCommandDescription());
	}

	@Override
	protected String getCommandSyntax() {
		return "<filename> (<charset>)";
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
		desc.add("Displays the contents of a file.");
		desc.add("The command expects one or two arguments.");
		desc.add("The first argument must be a path to a file, "
				+ "while the second argument is optional but must be "
				+ "a charset name that is used to interpret chars from bytes.");
		desc.add("If the second argument is not provided, a default platform charset is used.");
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) {
		if (s == null) {
			throw new SyntaxException();
		}
		
		String[] args = Helper.extractArguments(s);
		
		Path path = Helper.resolveAbsolutePath(env, args[0]);
		
		Charset charset = Charset.defaultCharset();
		if (args.length > 1) {
			if (args.length != 2) {
				throw new SyntaxException();
			}
			
			try {
				charset = Charset.forName(args[1]);
			} catch (IllegalArgumentException e) {
				writeln(env, "Invalid charset: " + args[1]);
				writeln(env, "Check available charsets with charsets command.");
				return CommandStatus.CONTINUE;
			}
		}
		
		if (!Files.exists(path)) {
			writeln(env, "The system cannot find the file specified.");
			return CommandStatus.CONTINUE;
		}
		if (!Files.isRegularFile(path)) {
			writeln(env, "The specified path must be a file.");
			return CommandStatus.CONTINUE;
		}
		
		/* Passed all checks, start working. */
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(
					new BufferedInputStream(
						Files.newInputStream(path)), charset))
		) {
			
			String line;
			while ((line = br.readLine()) != null) {
				writeln(env, line);
			}
			
		} catch (IOException e) {
			/* This could happen if the file is protected. */
			writeln(env, "Access is denied: " + e.getMessage());
		}
		
		return CommandStatus.CONTINUE;
	}

}
