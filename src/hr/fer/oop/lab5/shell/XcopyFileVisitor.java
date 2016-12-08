package hr.fer.oop.lab5.shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A {@linkplain SimpleFileVisitor} extended and used to serve the {@linkplain
 * XcopyCommand}. This file visitor is used to copy all contents of a source
 * directory to a destination directory.
 *
 * @author Mario Bobic
 */
public class XcopyFileVisitor extends SimpleFileVisitor<Path> implements CopyFeatures {
	
	/** Source directory */
	private File sourceDir;
	/** Destination directory */
	private File destDir;
	/** An environment */
	private Environment environment;

	/**
	 * Initializes a new instance of this class setting the source directory,
	 * destination directory and an environment used only for writing out
	 * messages.
	 * 
	 * @param sourceDir source directory
	 * @param destDir destination directory
	 * @param environment an environment
	 * 
	 */
	public XcopyFileVisitor(File sourceDir, File destDir, Environment environment) {
		super();
		this.sourceDir = sourceDir;
		this.destDir = destDir;
		this.environment = environment;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		Path subPath = getSubPath(sourceDir.toPath(), dir);
		File fullPath = combinePaths(destDir, subPath);
		
		environment.writeln("Mkdir: " + fullPath);
		fullPath.mkdir();
		
		return super.preVisitDirectory(dir, attrs);
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path subPath = getSubPath(sourceDir.toPath(), file);
		File fullPath = combinePaths(destDir, subPath);
		
		createNewFile(file.toFile(), fullPath.getParentFile(), fullPath.getName(), environment);
		
		return super.visitFile(file, attrs);
	}
	
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		System.out.println("Failed to access " + file);
		return FileVisitResult.CONTINUE;
	}
	
	/**
	 * Returns the subpath in relation to the {@code sourceDir} and {@code path}.
	 * For an example, if the {@code sourceDir} is {@code /a/b} and the
	 * {@code path} is {@code /a/b/c/d}, the returned value is {@code c/d}.
	 * 
	 * @param sourceDir source directory
	 * @param path a path relative to source directory
	 * @return relativized subpath
	 */
	private Path getSubPath(Path sourceDir, Path path) {
		return sourceDir.relativize(path);
	}
	
	/**
	 * Combines two paths into one. For an example if the {@code parent} is
	 * {@code /a/b}, and the {@code child} is {@code /c/d}, the combined path is
	 * {@code /a/b/c/d}.
	 * 
	 * @param parent parent file
	 * @param child child path
	 * @return a combined path
	 */
	private File combinePaths(File parent, Path child) {
		File fullPath = new File(parent, child.toString());
		return fullPath;
	}

}