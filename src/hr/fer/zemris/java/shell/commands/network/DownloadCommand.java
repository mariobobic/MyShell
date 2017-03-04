package hr.fer.zemris.java.shell.commands.network;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Crypto;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * This command is paired with {@link HostCommand} and {@link ConnectCommand}
 * and is used for downloading content from the host computer.
 *
 * @author Mario Bobic
 */
public class DownloadCommand extends VisitorCommand {
	
	/**
	 * Constructs a new command object of type {@code DownloadCommand}.
	 */
	public DownloadCommand() {
		super("DOWNLOAD", createCommandDescription());
	}
	
	@Override
	protected String getCommandSyntax() {
		return "<filename>";
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
		desc.add("Downloads content from the host's computer.");
		desc.add("This command can only be run when connected to a MyShell host.");
		desc.add("Both files and directories can be downloaded.");
		return desc;
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (!env.isConnected()) {
			writeln(env, "You must be connected to a host to run this command!");
			return CommandStatus.CONTINUE;
		}
		
		if (s == null) {
			throw new SyntaxException();
		}
		
		Path path = Helper.resolveAbsolutePath(env, s);
		Helper.requireExists(path);
		
		// Passed all checks, good to go
		try {
			DownloadFileVisitor downloadVisitor = new DownloadFileVisitor(env, path);
			walkFileTree(path, downloadVisitor);
			System.out.println("Host finished uploading " + path);
		} catch (SocketException e) {
			// Connection has ended
			return CommandStatus.TERMINATE;
		}
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Uploads the specified <tt>path</tt> to the client whose input and output
	 * streams are fetched through the environment <tt>env</tt>. Path
	 * <tt>root</tt> is used to create a relative file name of the path, which
	 * is then sent to the client to create an appropriate directory structure
	 * before downloading.
	 * 
	 * @param env an environment
	 * @param root root path from which files are being uploaded
	 * @param path path to be uploaded
	 * @throws SocketException if a connection error occurs between host and
	 *         client (typically the client ends the connection)
	 */
	private void upload(Environment env, Path root, Path path) throws SocketException {
		OutputStream outToClient = env.getConnection().getOutToClient();
		InputStream inFromClient = env.getConnection().getInFromClient();
		
		try {
			// Send a hint that download has started
			byte[] start = new String(Helper.DOWNLOAD_KEYWORD).getBytes();
			outToClient.write(start);
			inFromClient.read(); // wait for signal: accepted download
			
			// Send file name
			byte[] filename = root.relativize(path).toString().replace('\\', '/').getBytes();
			outToClient.write(filename);
			inFromClient.read(); // wait for signal: received file name
			
			// Send file type
			int filetype = Files.isDirectory(path) ? 1 : 0;
			outToClient.write(filetype);
			inFromClient.read(); // wait for signal: received file type
			
			if (Files.isDirectory(path)) {
				return;
			}
			
			// Send file size
			byte[] filesize = Long.toString(Crypto.postSize(path)).getBytes();
			outToClient.write(filesize);
			inFromClient.read(); // wait for signal: received file size
			
			// Start streaming file
			BufferedInputStream fileStream = new BufferedInputStream(Files.newInputStream(path));
			Crypto crypto = env.getConnection().getCrypto();
			byte[] bytes = new byte[1024];
			int len;
			while ((len = fileStream.read(bytes)) != -1) {
				byte[] encrypted = crypto.update(bytes, 0, len);
				outToClient.write(encrypted);
			}
			outToClient.write(crypto.doFinal());
		} catch (SocketException e) {
			throw e; // client has ended connection
		} catch (IOException e) {
			writeln(env, "An error occured while downloading " + path);
			writeln(env, e.getMessage());
		} catch (BadPaddingException ignorable) {
			// ignored, since crypto is in encryption mode
		} finally {
			try { outToClient.flush(); } catch (IOException e) {}
		}
	}
	
	/**
	 * A {@linkplain SimpleFileVisitor} extended and used to serve the
	 * {@linkplain DownloadCommand}.
	 *
	 * @author Mario Bobic
	 */
	private class DownloadFileVisitor extends SimpleFileVisitor<Path> {
		
		/** An environment. */
		private Environment environment;
		/** Starting file. */
		private Path root;
		
		/**
		 * Constructs an instance of {@code DownloadFileVisitor} with the
		 * specified environment.
		 * <p>
		 * The starting file is converted to a root directory. This is
		 * convenient for relativizing file names
		 *
		 * @param environment an environment
		 * @param start starting file
		 */
		public DownloadFileVisitor(Environment environment, Path start) {
			this.environment = environment;
			this.root = start.getParent();
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			process(dir);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			process(file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			writeln(environment, "Failed to access " + file);
			return FileVisitResult.CONTINUE;
		}
		
		/**
		 * Uploads the specified <tt>path</tt> to the client and waits for the
		 * signal of download completion. The received signal may be <tt>1</tt>
		 * for successful download or <tt>0</tt> for failed download.
		 * 
		 * @param path path to be uploaded and processed
		 * @throws IOException if the download fails on the client side
		 */
		private void process(Path path) throws IOException {
			upload(environment, root, path);
			int state = environment.getConnection().getInFromClient().read(); // wait for signal: download done
			
			if (state == 0) {
				throw new IOException("Download failed on client side: " + path);
			}
		}
	}

}
