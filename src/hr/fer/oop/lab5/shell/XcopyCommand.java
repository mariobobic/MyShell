package hr.fer.oop.lab5.shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A command that is used for copying one directory and one directory only, to
 * another location. The given location may be absolute or relative to the
 * current working directory. The destination directory may or may not be
 * existent. If the last name in the pathname's name sequence is an existing
 * directory, the original directory will be copied into that directory, with
 * all its contents. Else if the last name in the pathname's name sequence is a
 * non-existing directory, a new directory will be created and the contents of
 * the original directory will be copied into the newly created directory.
 *
 * @author Mario Bobic
 */
public class XcopyCommand extends AbstractCommand implements CopyFeatures {

	/**
	 * Constructs a new command object of type {@code XcopyCommand}.
	 */
	public XcopyCommand() {
		super("XCOPY", "Copies one directory to another location, recursively.");
	}

	@Override
	public CommandStatus execute(Environment env, String s) {
		if (s == null || !s.contains(" ")) {
			printSyntaxError(env, "xcopy <arg1> <arg2>");
			return CommandStatus.CONTINUE;
		}
		
		String args[] = extractArguments(s);
		
		Path path1 = Helper.resolveAbsolutePath(env, args[0]);
		Path path2 = Helper.resolveAbsolutePath(env, args[1]);
		if (path1 == null || path2 == null) {
			env.writeln("Invalid path!");
			return CommandStatus.CONTINUE;
		}
		
		File dir1 = path1.toFile();
		File dir2 = path2.toFile();
		
		copyDirectory(dir1, dir2, env);
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Validates both directories and copies {@code dir1} to {@code dir2}.
	 * 
	 * @param dir1 the directory to be copied
	 * @param dir2 the destination directory
	 * @param env an environment
	 */
	private void copyDirectory(File dir1, File dir2, Environment env) {
		if (!dir1.exists()) {
			env.writeln("The system cannot find the directory specified.");
			return;
		}
		if (dir1.isFile()) {
			env.writeln("Cannot copy files using the xcopy command. Use copy instead.");
			return;
		}
		if (dir2.isFile()) {
			env.writeln("Cannot copy directory onto a file.");
			return;
		}
		
		XcopyFileVisitor xcopyVisitor;
		if (dir2.exists()) {
			/* Copy dir1 to dir2, leaving the name of dir1 */
			File newDir = new File(dir2, dir1.getName());
			xcopyVisitor = new XcopyFileVisitor(dir1, newDir, env);
		} else {
			/* Copy dir1 to dir2 parent, renaming dir1 to dir2 */
			File newDir = new File(dir2.getParentFile(), dir2.getName());
			xcopyVisitor = new XcopyFileVisitor(dir1, newDir, env);
		}
		
		try {
			Files.walkFileTree(dir1.toPath(), xcopyVisitor);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
