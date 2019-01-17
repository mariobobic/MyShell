package hr.fer.zemris.java.shell.utility;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("javadoc")
public class ConcurrentFileVisitor implements FileVisitor<Path> {
	
	private static final int NUM_PROCESSORS = Runtime.getRuntime().availableProcessors();
	private static final int NUM_THREADS = NUM_PROCESSORS;
	
	private ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
	
	private Set<Path> visited = new HashSet<>();
	
	public Path walkFileTree(Path start, FileVisitor<Path> visitor) throws IOException {
		Files.walkFileTree(start, visitor);
		executor.shutdown();
		return start;
	}
	
	@Override
	public synchronized FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if (visited.contains(dir)) {
			return FileVisitResult.SKIP_SUBTREE;
		}
		
		if (!visited.contains(dir)) {
			visited.add(dir);
			executor.execute(() -> {
//				System.out.println("Walking on " + dir);
				try {
					Files.walkFileTree(dir, this);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

}
