package hr.fer.zemris.java.shell.commands;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Helper;

/**
 * Used as a superclass for Shell commands that implement the Visitor pattern.
 * 
 * @author Mario Bobic
 */
public abstract class VisitorCommand extends AbstractCommand {

	/** Set of paths to be excluded while searching. */
	protected Set<Path> excludes;
	/** Indicates if error printing should be suppressed. */
	protected boolean silent;

	/**
	 * Constructs a new command of a type extending {@code VisitorCommand}, with
	 * some pre-defined flag definitions.
	 * <ul>
	 * <li><strong>exclude</strong> - accepts a path to be excluded from the
	 * visitor process. Multiple flags can be given to exclude all specified
	 * paths from process.
	 * <li><strong>silent</strong> - a boolean flag to suppress errors
	 * </ul>
	 * 
	 * @param commandName name of the Shell command
	 * @param commandDescription description of the Shell command
	 */
	protected VisitorCommand(String commandName, List<String> commandDescription) {
		this(commandName, commandDescription, new ArrayList<>());
	}
	
	/**
	 * Constructs a new command of a type extending {@code VisitorCommand}, with
	 * some pre-defined flag definitions.
	 * <ul>
	 * <li><strong>exclude</strong> - accepts a path to be excluded from the
	 * visitor process. Multiple flags can be given to exclude all specified
	 * paths from process.
	 * <li><strong>silent</strong> - a boolean flag to suppress errors
	 * </ul>
	 * 
	 * @param commandName name of this Shell command
	 * @param commandDescription description of this Shell command
	 * @param flagDescriptions flag descriptions of this Shell command
	 */
	protected VisitorCommand(String commandName, List<String> commandDescription, List<FlagDescription> flagDescriptions) {
		super(commandName, commandDescription, addFlagDescriptions(flagDescriptions));
		commandArguments.addFlagDefinition("e", "exclude", true);
		commandArguments.addFlagDefinition("s", "silent", false);
	}
	
	/**
	 * Adds the visitor flag descriptions to the specified list of
	 * {@code FlagDescription} objects.
	 * 
	 * @param flagDescriptions list of flag descriptions to be updated
	 * @return the updated list of flag descriptions
	 */
	private static List<FlagDescription> addFlagDescriptions(List<FlagDescription> flagDescriptions) {
		flagDescriptions.add(new FlagDescription("e", "exclude", "path", "Exclude a file or directory from the visitor process. May be used multiple times."));
		flagDescriptions.add(new FlagDescription("s", "silent", null, "Suppress error printing on command execution."));
		return flagDescriptions;
	}

	@Override
	protected String compileFlags(Environment env, String s) {
		/* Initialize default values. */
		silent = false;
		excludes = new HashSet<>();

		/* Compile! */
		s = commandArguments.compile(s);
		
		/* Replace default values with flag values, if any. */
		if (commandArguments.containsFlag("s", "silent")) {
			silent = true;
		}
		
		if (commandArguments.containsFlag("e", "exclude")) {
			List<String> args = commandArguments.getFlag("e", "exclude").getArguments();
			excludes = args.stream()
				.map(str -> Helper.resolveAbsolutePath(env, str))
				.collect(Collectors.toSet());
		}
		
		return super.compileFlags(env, s);
	}
	
	/**
	 * Returns true if the specified <tt>path</tt> should be excluded from the
	 * visitor process.
	 * 
	 * @param path path to be checked
	 * @return true if path is excluded from the visitor process
	 */
	protected boolean isExcluded(Path path) {
		return excludes.contains(path);
	}
	
	/**
	 * Returns true if this command should run in silent mode, or in other words
	 * if error printing should be suppressed.
	 * 
	 * @return true if this command should run in silent mode
	 */
	protected boolean isSilent() {
		return silent;
	}
	
}
