package hr.fer.zemris.java.tecaj.hw07.shell.extracommands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hr.fer.zemris.java.tecaj.hw07.shell.CommandStatus;
import hr.fer.zemris.java.tecaj.hw07.shell.Environment;
import hr.fer.zemris.java.tecaj.hw07.shell.Helper;
import hr.fer.zemris.java.tecaj.hw07.shell.commands.AbstractCommand;

/**
 * A command that is used for shuffling bytes of a file. The user can specify
 * the range of byte shuffling. This command creates a new file with the same
 * name as the original file, but with an index at the end and keeps the
 * original file.
 *
 * @author Mario Bobic
 */
public class ByteShuffleCommand extends AbstractCommand {
	
	/** Defines the proper syntax for using this command */
	private static final String SYNTAX = "byteshuffle <filename> (optional: <offset> <length>)";

	/** Name used for temporarily renaming files */
	private static final String TEMP_NAME = "__temp";
	
	/**
	 * Constructs a new command object of type {@code ByteShuffleCommand}.
	 */
	public ByteShuffleCommand() {
		super("BYTESHUFFLE", createCommandDescription());
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
		desc.add("Shuffles bytes of the specified file creating a new file and keeping the original file.");
		desc.add("Optional offset and length may be included.");
		desc.add("The expected syntax: " + SYNTAX);
		return desc;
	}
	
	@Override
	public CommandStatus execute(Environment env, String s) {
		if (s == null) {
			printSyntaxError(env, SYNTAX);
			return CommandStatus.CONTINUE;
		}
		
		String[] args = Helper.extractArguments(s);
		
		Path path = Helper.resolvePath(args[0]);
		if (path == null) {
			writeln(env, "Invalid path!");
			return CommandStatus.CONTINUE;
		}
		File file = path.toFile();
		
		if (!file.isFile()) {
			writeln(env, "The system cannot find the file specified.");
			return CommandStatus.CONTINUE;
		}
		
		int offset;
		long length;
		try {
			offset = Integer.parseInt(args[1]);
			length = Integer.parseInt(args[2]);
		} catch (Exception e) {
			offset = 0;
			length = file.length();
			writeln(env, "Offset: " + offset + ", length: " + length); 
		}
		
		long fileEndPoint = offset + length;
		if (fileEndPoint > file.length()) {
			writeln(env, "The given offset and length are too big for file " + file.getName());
			writeln(env, "The given file has the length of " + file.length() + " bytes.");
			return CommandStatus.CONTINUE;
		}

		File parent = file.getParentFile();
		File newFile = new File(parent, TEMP_NAME);
		
		try (
				FileInputStream in = new FileInputStream(file);
				FileOutputStream out = new FileOutputStream(newFile);
		) {

			/* First copy entire file */
			int len;
			byte[] bytes = new byte[1024];
			while ((len = in.read(bytes)) > 0) {
				out.write(bytes, 0, len);
			}

			/* Rewind both streams */
			in.getChannel().position(offset);
			out.getChannel().position(offset);
			
			/* Then read with the offset */
			len = (int) length;
			bytes = new byte[len];
			in.read(bytes, 0, len);

			/* Shuffle the bytes and write to a new file with offset */
			byte[] shuffledBytes = shuffleBytes(bytes);
			out.write(shuffledBytes, 0, len);

		} catch (Exception e) {
			writeln(env, "An I/O error has occured!");
			writeln(env, e.toString());
		}

		/* The process of renaming a file */
		int namingIndex = 0;
		String name = file.getName();
		String extension = "";
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex != -1) {
			extension = name.substring(dotIndex);
			name = name.replace(extension, "-");
		}
		File renamingFile;
		while ((renamingFile = new File(parent, name + namingIndex + extension)).exists()) {
			namingIndex++;
		}
		newFile.renameTo(renamingFile);
		
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
