package hr.fer.zemris.java.shell.commands.writing;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * A command that is used for shuffling bytes of a file. The user can specify
 * the range of byte shuffling. This command creates a new file with the same
 * name as the original file, but with an index at the end and keeps the
 * original file.
 *
 * @author Mario Bobic
 */
public class ByteShuffleCommand extends AbstractCommand {
	
	/**
	 * Constructs a new command object of type {@code ByteShuffleCommand}.
	 */
	public ByteShuffleCommand() {
		super("BYTESHUFFLE", createCommandDescription());
	}
	
	@Override
	protected String getCommandSyntax() {
		return "<filename> (optional: <offset> <length>)";
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
		desc.add("Shuffles bytes of the specified file.");
		desc.add("Upon execution, a new file is created and the original file is kept.");
		desc.add("Optional offset and length may be included.");
		return desc;
	}
	
	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			throw new SyntaxException();
		}
		
		String[] args = Helper.extractArguments(s);
		
		Path file = Helper.resolveAbsolutePath(env, args[0]);
		Helper.requireFile(file);
		
		// TODO Flag this up!
		int offset;
		long length;
		try {
			offset = Integer.parseInt(args[1]);
			length = Integer.parseInt(args[2]);
		} catch (Exception e) {
			offset = 0;
			length = Files.size(file);
			writeln(env, "Offset: " + offset + ", length: " + length); 
		}
		
		long fileEndPoint = offset + length;
		if (fileEndPoint > Files.size(file)) {
			writeln(env, "The given offset and length are too big for file " + file.getFileName());
			writeln(env, "The given file has the length of " + Files.size(file) + " bytes.");
			return CommandStatus.CONTINUE;
		}

		Path tempFile = Files.createTempFile(file.getParent(), null, null);
		try (
				FileInputStream in = new FileInputStream(file.toFile());
				FileOutputStream out = new FileOutputStream(tempFile.toFile());
		) {

			/* First copy entire file. */
			int len;
			byte[] bytes = new byte[1024];
			while ((len = in.read(bytes)) > 0) {
				out.write(bytes, 0, len);
			}

			/* Rewind both streams. */
			in.getChannel().position(offset);
			out.getChannel().position(offset);
			
			/* Then read with the offset. */
			len = (int) length;
			bytes = new byte[len];
			in.read(bytes, 0, len);

			/* Shuffle the bytes and write to a new file with offset. */
			byte[] shuffledBytes = shuffleBytes(bytes);
			out.write(shuffledBytes, 0, len);

		}

		/* Rename the temp file. */
		Path newFile = Helper.firstAvailable(file);
		Files.move(tempFile, newFile);
		
		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Shuffles the given byte array using the Java&trade; utility methods. The
	 * byte array is loaded into a List, shuffled and returned as a new byte
	 * array.
	 * 
	 * @param bytes array of bytes to be shuffled
	 * @return shuffled array of bytes
	 */
	private static byte[] shuffleBytes(byte[] bytes) {
		List<Byte> list = new ArrayList<>();
		for (Byte b : bytes) {
			list.add(b);
		}
		
		Collections.shuffle(list);
		byte[] shuffledBytes = new byte[bytes.length];

		for (int i = 0; i < shuffledBytes.length; i++) {
			shuffledBytes[i] = list.get(i);
		}
		return shuffledBytes;
	}

}
