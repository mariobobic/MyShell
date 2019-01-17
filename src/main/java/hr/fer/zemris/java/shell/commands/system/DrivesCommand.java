package hr.fer.zemris.java.shell.commands.system;

import static hr.fer.zemris.java.shell.utility.CommandUtility.*;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Utility;

/**
 * Lists names of available disk drives for the Java Virtual Machine. A single
 * drive name and its information is written per line.
 *
 * @author Mario Bobic
 */
public class DrivesCommand extends AbstractCommand {
	
	/* Flags */
	/** Indicates if file sizes should be printed in human readable byte count. */
	private boolean humanReadable;
	
	/**
	 * Constructs a new command object of type {@code DrivesCommand}.
	 */
	public DrivesCommand() {
		super("DRIVES", createCommandDescription(), createFlagDescriptions());
	}

	@Override
	public String getCommandSyntax() {
		return "";
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
		desc.add(new FlagDescription("h", null, null, "Print human readable sizes (e.g. 1kiB, 256MiB)."));
		return desc;
	}
	
	@Override
	protected String compileFlags(Environment env, String s) {
		/* Initialize default values. */
		humanReadable = false;

		/* Compile! */
		s = commandArguments.compile(s);
		
		/* Replace default values with flag values, if any. */
		if (commandArguments.containsFlag("h")) {
			humanReadable = true;
		}

		return super.compileFlags(env, s);
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
		desc.add("Lists names of available disk drives.");
		desc.add("A single drive name and its information is written per line.");
		return desc;
	}

	@Override
	protected ShellStatus execute0(Environment env, String s) throws IOException {
		for (Path root : FileSystems.getDefault().getRootDirectories()) {
			try {
		        FileStore store = Files.getFileStore(root);
		        
		        formatln(env, "%s - available=%s, total=%s", root,
		        	humanReadable ? Utility.humanReadableByteCount(store.getUsableSpace()) : store.getUsableSpace(),
		        	humanReadable ? Utility.humanReadableByteCount(store.getTotalSpace())  : store.getTotalSpace()
		        );
			} catch (IOException e) {
				env.writeln(root + " - fetching information failed");
			}
		}
		
		return ShellStatus.CONTINUE;
	}

}
