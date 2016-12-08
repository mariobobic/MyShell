package hr.fer.zemris.java.shell.extracommands;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.Helper;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * Analyzes line by line and displays a list of changes between two files. The
 * command requires 2 arguments - path to first and second file.
 * <p>
 * Third, optional argument is a charset. If the charset argument isn't
 * specified, UTF-8 is used as default.
 * <p>
 * If a fourth argument is specified, it is expected to be 'ALL'. The ALL
 * argument means that the whole document will be printed, with differences
 * highlighted.
 *
 * @author Mario Bobic
 */
public class DiffCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command. */
	private static final String SYNTAX = "diff <filename1> <filename2> (optional: <charset> all)";

	/**
	 * Constructs a new command object of type {@code DiffCommand}.
	 */
	public DiffCommand() {
		super("DIFF", createCommandDescription());
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
		desc.add("Analyzes line by line and displays a list of changes between two files.");
		desc.add("The command requires 2 arguments - path to first and second file.");
		desc.add("Third, optional argument is a charset. If the charset argument isn't specified, "
				+ "UTF-8 is used as default.");
		desc.add("If a fourth argument is specified, it is expected to be \"all\".");
		desc.add("The ALL argument means that the whole document will be printed, "
				+ "with differences highlighted.");
		desc.add("Syntax: " + SYNTAX);
		return desc;
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		/* Extract arguments and check the array length. */
		String[] args = Helper.extractArguments(s);
		if (args.length < 2 || args.length > 4) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		/* Resolve file paths. */
		Path file1 = Helper.resolveAbsolutePath(env, args[0]);
		Path file2 = Helper.resolveAbsolutePath(env, args[1]);
		if (!Files.isRegularFile(file1)) {
			writeln(env, "The system cannot find the file " + file1);
			return CommandStatus.CONTINUE;
		}
		if (!Files.isRegularFile(file2)) {
			writeln(env, "The system cannot find the file " + file2);
			return CommandStatus.CONTINUE;
		}
		
		/* Resolve charset, if any is given and the ALL argument. */
		Charset charset = StandardCharsets.UTF_8;
		boolean all = false;
		
		/* First try if 'all' is given. If not, it is a charset or invalid. */
		if (args.length == 3) {
			if ("all".equalsIgnoreCase(args[2])) {
				all = true;
			} else {
				charset = resolveCharset(args[2]);
			}
		/* Charset must be given first, then the 'all' argument. */
		} else if (args.length == 4) {
			charset = resolveCharset(args[2]);
			if ("all".equalsIgnoreCase(args[3])) {
				all = true;
			} else {
				writeln(env, "With four arguments specified, the fourth must be 'ALL'.");
				return CommandStatus.CONTINUE;
			}
		}
		
		/* If the above procedure didn't give a valid charset, scream! */
		if (charset == null) {
			writeln(env, "Invalid charset: " + args[2]);
			writeln(env, "Check available charsets with charsets command.");
			return CommandStatus.CONTINUE;
		}
		
		/* Passed all checks, start working. */
		try {
			Iterator<String> iter1 = Files.lines(file1, charset).iterator();
			Iterator<String> iter2 = Files.lines(file2, charset).iterator();
			
			int counter = 0;
			while (true) {
				counter++;
				
				/* Setup */
				String line1 = null; String line2 = null;
				if (iter1.hasNext()) line1 = iter1.next();
				if (iter2.hasNext()) line2 = iter2.next();

				/* Break conditions */
				if (line1 == null && line2 == null) break;
				if (!all && (line1 == null || line2 == null)) break;
				
				writeDifferences(env, line1, line2, counter, all);
			}
			
		} catch (Exception e) {
			writeln(env, "Cannot decode files using " + charset + " encoding. Try a different encoding.");
		}
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Returns a charset object for the named charset, or <tt>null</tt> if a
	 * charset with the specified <tt>name<tt> can not be resolved.
	 * 
	 * @param name name of the requested charset; may be either a canonical name
	 *        or an alias
	 * @return a charset object for the named charset
	 */
	private static Charset resolveCharset(String name) {
		try {
			return Charset.forName(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	/**
	 * Writes out differences of the two specified lines <tt>line1</tt> and
	 * <tt>line2</tt>, with line number included in front. If the boolean
	 * <tt>all</tt> is <tt>true</tt> and if the specified lines do not differ,
	 * the line is written out.
	 * 
	 * @param env an environment
	 * @param line1 first line for comparison
	 * @param line2 second line for comparison
	 * @param lineNum line number
	 * @param all indicates if the line should be written out even if there are
	 *        no differences between the two lines
	 */
	private static void writeDifferences(Environment env, String line1, String line2, int lineNum, boolean all) {
		if (line1 == null) line1 = "";
		if (line2 == null) line2 = "";
		
		if (!Objects.equals(line1, line2)) {
			writeln(env, lineNum + ": " + line1 + " --> " + line2);
		} else if (all) {
			writeln(env, lineNum + ": " + line1);
		}
	}

}
