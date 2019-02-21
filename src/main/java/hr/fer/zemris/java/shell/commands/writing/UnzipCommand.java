package hr.fer.zemris.java.shell.commands.writing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Progress;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.SyntaxException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static hr.fer.zemris.java.shell.utility.CommandUtility.promptConfirm;

/**
 * Many sources of information contain redundant data or data that adds little
 * to the stored information. This results in tremendous amounts of data being
 * transferred between client and server applications or computers in general.
 * <p>
 * This command is used for decompressing ZIP archive files into regular files
 * and directories.
 *
 * @author Mario Bobic
 */
public class UnzipCommand extends AbstractCommand {

    /**
     * Constructs a new command object of type {@code UnzipCommand}.
     */
    public UnzipCommand() {
        super("UNZIP", createCommandDescription());
    }

    @Override
    public String getCommandSyntax() {
        return "<source_file> (<target_directory>)";
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
        desc.add("Decompresses ZIP archive files into regular files and directories.");
        desc.add("If the target path is not specified, the extraction will happen "
                + "in a directory with the first available name as the source file.");
        desc.add("If the target path is specified, the target files will be extracted "
                + "inside of that directory.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        if (s == null) {
            throw new SyntaxException();
        }

        String[] args = StringUtility.extractArguments(s);
        if (args.length > 2) {
            throw new SyntaxException();
        }

        Path source = Utility.resolveAbsolutePath(env, args[0]);
        Path target;
        try {
            target = Utility.resolveAbsolutePath(env, args[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
            String newName = Utility.getFileName(source).toString().replaceFirst(Utility.ZIP_FILE_EXT+"$", "");
            target = source.resolveSibling(newName);
            target = Utility.firstAvailable(target);
        }

        Utility.requireExists(source);
        if (Files.isRegularFile(target)) {
            env.writeln("The target path must not be a file.");
            return ShellStatus.CONTINUE;
        }

        /* Passed all checks, start working. */
        long size = getUncompressedSize(source);

        Progress progress = new Progress(env, size, true);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(source))) {
            ZipEntry zipEntry;

            while ((zipEntry = zis.getNextEntry()) != null) {
                Path file = target.resolve(zipEntry.getName());
                if (Files.exists(file)) {
                    if (!promptConfirm(env, "File " + file + " already exists. Overwrite?")) {
                        continue;
                    }
                }

                Files.createDirectories(file.getParent());

                try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(file))) {
                    int len;
                    byte[] buff = new byte[1024];
                    while ((len = zis.read(buff)) > 0) {
                        out.write(buff, 0, len);
                        progress.add(len);
                    }
                } finally {
                    progress.stop();
                    zis.closeEntry();
                }
            }
        }

        return ShellStatus.CONTINUE;
    }

    /**
     * Returns the total uncompressed size of all entries in the zip file of the
     * specified <tt>path</tt>.
     *
     * @param path the source zip file
     * @return total uncompressed size of all entries in the stream
     * @throws IOException if an I/O error occurs
     */
    private static long getUncompressedSize(Path path) throws IOException {
        ZipFile zipFile = new ZipFile(path.toFile());

        long size = 0;
        @SuppressWarnings("unchecked")
        Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) zipFile.entries();
        while (e.hasMoreElements()) {
            size += e.nextElement().getSize();
        }

        zipFile.close();
        return size;
    }

}
