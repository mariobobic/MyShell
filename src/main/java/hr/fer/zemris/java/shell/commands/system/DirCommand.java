package hr.fer.zemris.java.shell.commands.system;

import static hr.fer.zemris.java.shell.utility.CommandUtility.*;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Utility;

/**
 * A command that is used for writing out the current contents of a directory in
 * a Windows CMD-like environment. The specified argument must be an existing
 * directory path.
 * <p>
 * While listing directory's contents, this command also writes out if it
 * stumbled upon a file or a directory. In case of a file, this command writes
 * out the file's size in bytes.
 *
 * @author Mario Bobic
 */
public class DirCommand extends AbstractCommand {

    /** Date format used for formatting file date attribute. */
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy.  HH:mm");
    /** Decimal format used for formatting file size attribute. */
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###,###.##");

    /**
     * Constructs a new command object of type {@code DirCommand}.
     */
    public DirCommand() {
        super("DIR", createCommandDescription());
    }

    @Override
    public String getCommandSyntax() {
        return "(<path>)";
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
        desc.add("Lists directory contents with a CMD-like environment.");
        desc.add("A directory path may be specified to list its contents.");
        return desc;
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) {
        Path dir = s == null ?
            env.getCurrentPath() : Utility.resolveAbsolutePath(env, s);

        Utility.requireDirectory(dir);

        /* Clear previously marked paths. */
        env.clearMarks();

        /* Passed all checks, start working. */
        env.writeln(" Directory of " + dir);
        env.writeln("");

        File[] files = dir.toFile().listFiles();
        int noFiles = 0;
        int noDirs = 0;
        long filesLength = 0;

        for (File file : files) {
            String name = file.getName();
            Date date = new Date(file.lastModified());

            env.write(DATE_FORMAT.format(date) + "");
            if (file.isFile()) {
                noFiles++;
                long size = file.length();
                filesLength += size;
                format(env, " %17s ", DECIMAL_FORMAT.format(size));
            } else {
                noDirs++;
                env.write("    <DIR>          ");
            }
            env.write(name);
            markAndPrintNumber(env, file.toPath());
        }
        formatln(env, "%15d File(s), %s bytes", noFiles, DECIMAL_FORMAT.format(filesLength));
        formatln(env, "%15d Dir(s)", noDirs);

        return ShellStatus.CONTINUE;
    }

}
