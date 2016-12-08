package hr.fer.oop.lab5.shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A {@linkplain SimpleFileVisitor} extended and used to serve the {@linkplain
 * RmCommand}. This file visitor is mostly used to remove non-empty directories.
 *
 * @author Mario Bobic
 */
public class RmFileVisitor extends SimpleFileVisitor<Path> {
	
	/** An environment */
	private Environment environment;

	/**
	 * Initializes a new instance of this class setting only an environment used
	 * only for writing out messages.
	 * 
	 * @param environment an environment
	 */
	public RmFileVisitor(Environment environment) {
		super();
		this.environment = environment;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		File f = file.toFile();
		f.delete();
		environment.writeln("Deleted file " + f.getName());
		
		return super.visitFile(file, attrs);
	}
	
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		System.out.println("Failed to remove " + file);
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		File d = dir.toFile();
		d.delete();
		environment.writeln("Directory " + d.getName() + " removed.");
		
		return super.postVisitDirectory(dir, exc);
	}
}
