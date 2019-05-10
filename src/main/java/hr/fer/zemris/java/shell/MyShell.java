package hr.fer.zemris.java.shell;

import hr.fer.zemris.java.shell.commands.listing.CountCommand;
import hr.fer.zemris.java.shell.commands.listing.DiffCommand;
import hr.fer.zemris.java.shell.commands.listing.FileTypesCommand;
import hr.fer.zemris.java.shell.commands.listing.FilterCommand;
import hr.fer.zemris.java.shell.commands.listing.FindCommand;
import hr.fer.zemris.java.shell.commands.listing.ShowCommand;
import hr.fer.zemris.java.shell.commands.listing.TreeCommand;
import hr.fer.zemris.java.shell.commands.network.ConnectCommand;
import hr.fer.zemris.java.shell.commands.network.DownloadCommand;
import hr.fer.zemris.java.shell.commands.network.HostCommand;
import hr.fer.zemris.java.shell.commands.network.HttpServerCommand;
import hr.fer.zemris.java.shell.commands.network.PingCommand;
import hr.fer.zemris.java.shell.commands.network.UploadCommand;
import hr.fer.zemris.java.shell.commands.reading.CatCommand;
import hr.fer.zemris.java.shell.commands.reading.HexdumpCommand;
import hr.fer.zemris.java.shell.commands.system.CdCommand;
import hr.fer.zemris.java.shell.commands.system.CharsetsCommand;
import hr.fer.zemris.java.shell.commands.system.ClearCommand;
import hr.fer.zemris.java.shell.commands.system.DateCommand;
import hr.fer.zemris.java.shell.commands.system.DirCommand;
import hr.fer.zemris.java.shell.commands.system.DrivesCommand;
import hr.fer.zemris.java.shell.commands.system.EchoCommand;
import hr.fer.zemris.java.shell.commands.system.EditCommand;
import hr.fer.zemris.java.shell.commands.system.ExitCommand;
import hr.fer.zemris.java.shell.commands.system.HelpCommand;
import hr.fer.zemris.java.shell.commands.system.HistoryCommand;
import hr.fer.zemris.java.shell.commands.system.LsCommand;
import hr.fer.zemris.java.shell.commands.system.OpenCommand;
import hr.fer.zemris.java.shell.commands.system.PwdCommand;
import hr.fer.zemris.java.shell.commands.system.SymbolCommand;
import hr.fer.zemris.java.shell.commands.writing.ByteShuffleCommand;
import hr.fer.zemris.java.shell.commands.writing.CleanCommand;
import hr.fer.zemris.java.shell.commands.writing.CopyCommand;
import hr.fer.zemris.java.shell.commands.writing.DecryptCommand;
import hr.fer.zemris.java.shell.commands.writing.DumpCommand;
import hr.fer.zemris.java.shell.commands.writing.EncryptCommand;
import hr.fer.zemris.java.shell.commands.writing.ExtractCommand;
import hr.fer.zemris.java.shell.commands.writing.MkdirCommand;
import hr.fer.zemris.java.shell.commands.writing.MoveCommand;
import hr.fer.zemris.java.shell.commands.writing.NameShuffleCommand;
import hr.fer.zemris.java.shell.commands.writing.RenameAllCommand;
import hr.fer.zemris.java.shell.commands.writing.RenameCommand;
import hr.fer.zemris.java.shell.commands.writing.ReplaceCommand;
import hr.fer.zemris.java.shell.commands.writing.RmCommand;
import hr.fer.zemris.java.shell.commands.writing.RmdirCommand;
import hr.fer.zemris.java.shell.commands.writing.SortCommand;
import hr.fer.zemris.java.shell.commands.writing.TempRenameCommand;
import hr.fer.zemris.java.shell.commands.writing.TouchCommand;
import hr.fer.zemris.java.shell.commands.writing.UnzipCommand;
import hr.fer.zemris.java.shell.commands.writing.ZipCommand;
import hr.fer.zemris.java.shell.interfaces.Connection;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.interfaces.ShellCommand;
import hr.fer.zemris.java.shell.utility.CommandUtility;
import hr.fer.zemris.java.shell.utility.Crypto;
import hr.fer.zemris.java.shell.utility.Expander;
import hr.fer.zemris.java.shell.utility.StringUtility;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.IllegalPathException;
import hr.fer.zemris.java.shell.utility.exceptions.ShellIOException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * MyShell, this is where the magic happens. Scans the user's input and searches
 * for a matching command. Some commands require arguments, so the user must
 * enter them as well. If the input command is found, the command is executed,
 * otherwise the program checks if it is a variable assignment. If the given
 * input is neither a valid command name nor a variable assignment, an error
 * message is displayed. The program stops and prints out a goodbye message if
 * the input command is {@link ExitCommand}. The program also prompts an input
 * symbol while waiting for a command to be inputed. If a critical error occurs,
 * an error message is printed out onto the <b>standard error</b> with a detail
 * message specifying what went wrong before the program terminates.
 *
 * @author Mario Bobic
 * @author Marko Čupić
 * @version Laptop
 */
public class MyShell {

    /** A map of commands. */
    private static Map<String, ShellCommand> commands;

    static {
        commands = new HashMap<>();
        ShellCommand[] cc = {
                /* Listing */
                new CountCommand(),
                new DiffCommand(),
                new FileTypesCommand(),
                new FilterCommand(),
                new FindCommand(),
                new ShowCommand(),
                new TreeCommand(),

                /* Network */
                new ConnectCommand(),
                new DownloadCommand(),
                new HostCommand(),
                new HttpServerCommand(),
                new PingCommand(),
                new UploadCommand(),

                /* Reading */
                new CatCommand(),
                new HexdumpCommand(),

                /* System */
                new CdCommand(),
                new CharsetsCommand(),
                new ClearCommand(),
                new EchoCommand(),
                new DateCommand(),
                new DirCommand(),
                new DrivesCommand(),
                new EditCommand(),
                new ExitCommand(),
                new HelpCommand(),
                new HistoryCommand(),
                new LsCommand(),
                new OpenCommand(),
                new PwdCommand(),
                new SymbolCommand(),

                /* Writing */
                new ByteShuffleCommand(),
                new CleanCommand(),
                new CopyCommand(),
                new DecryptCommand(),
                new DumpCommand(),
                new EncryptCommand(),
                new ExtractCommand(),
                new MkdirCommand(),
                new MoveCommand(),
                new NameShuffleCommand(),
                new RenameAllCommand(),
                new RenameCommand(),
                new ReplaceCommand(),
                new RmCommand(),
                new RmdirCommand(),
                new SortCommand(),
                new TempRenameCommand(),
                new TouchCommand(),
                new UnzipCommand(),
                new ZipCommand(),
        };

        for (ShellCommand c : cc) {
            commands.put(c.getCommandName(), c);
        }
    }

    /** An environment used by MyShell. */
    private static EnvironmentImpl environment = new EnvironmentImpl();
    /** Indicates if the output stream has been redirected to a file. */
    private static boolean redirected = false;
    /** Execution status of the last executed command. */
    private static ShellStatus executionStatus;

    /**
     * Program entry point.
     *
     * @param args not used in this program
     */
    public static void main(String[] args) {
        environment.writeln("Welcome to MyShell! You may enter commands.");

l:		while (true) {
            Path path = environment.getCurrentPath();
            Path pathName = Utility.getFileName(path);
            environment.write("$" + pathName + environment.promptSymbol + " ");

            String input = readInput();
            List<String> lines = Expander.expand(environment, input);
            environment.addToHistory(input);

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue l;

                CommandUtility.CmdArg cmdArg = new CommandUtility.CmdArg(line);
                String cmd = cmdArg.cmd;
                String arg = cmdArg.arg;

                ShellCommand shellCommand = commands.get(cmd);
                if (shellCommand == null) {
                    boolean assigned = tryAssignVariable(line);
                    if (!assigned) {
                        environment.writeln("Unknown command: " + cmd);
                        continue l;
                    }
                    continue;
                }

                // If this machine is a host, write the command to its standard output
                if (environment.isConnected()) {
                    System.out.println(""+path + environment.promptSymbol + " " + line);
                }

                try {
                    arg = checkRedirect(arg);
                } catch (InvalidPathException e) {
                    environment.writeln(e.getMessage());
                    continue;
                }

                try {
                    executionStatus = shellCommand.execute(environment, arg);
                } catch (RuntimeException critical) {
                    System.err.println("A critical error occured: " + critical.getMessage());
                    System.err.println("Stack trace:");
                    critical.printStackTrace();
                    return;
                }

                restoreRedirect();
                if (executionStatus == ShellStatus.TERMINATE) {
                    break l;
                }
            }
        }

        environment.writeln("Thank you for using this shell. Goodbye!");
    }

    /**
     * Returns a {@code ShellCommand} with the specified name.
     *
     * @param name name of the shell command
     * @return a command with the specified name
     */
    public static ShellCommand getCommand(String name) {
        return commands.get(name);
    }

    /**
     * Reads the input from the current {@link EnvironmentImpl#readers reader}
     * considering the {@link EnvironmentImpl#morelinesSymbol morelinesSymbol}
     * as a line breaker for commands. The user may enter some command and its
     * arguments followed by a space character and the <tt>morelinesSymbol</tt>,
     * to which the Shell will respond with an appropriate
     * {@link EnvironmentImpl#multilineSymbol multilineSymbol} as a multi-line
     * prompt, and the whole input will be stored as a simple one-lined string.
     *
     * @return the string input from the user
     */
    private static String readInput() {
        String line = environment.readLine();
        if (line == null) {
            environment.writeln("Input unavailable. Closing MyShell...");
            System.exit(-1);
        }

        line = line.trim();
        while (line.endsWith(" " + environment.morelinesSymbol)) {
            line = line.substring(0, line.length()-1); // remove the symbol

            environment.write(environment.multilineSymbol + " ");
            line += environment.readLine().trim();
        }

        return line;
    }

    /**
     * Tries to assign a value to a variable, both parsed from the specified
     * <tt>input</tt>.
     * <p>
     * The input must be given according to the following rules:
     * <ul>
     * <li>Variable name must be on the left-hand side,
     * <li>Value must be on the right-hand side and
     * <li>There must be no spaces between variable name and value.
     * </ul>
     * <p>
     * When the input satisfies all of the above criteria, the specified
     * variable is assigned a value and <strong>true</strong> is returned.
     * <p>
     * If the input does not satisfy any of the following rules,
     * <strong>false</strong> is returned and no variable is assigned a value.
     *
     * @param input input to be processed
     * @return true if variable was assigned a value, false otherwise
     */
    private static boolean tryAssignVariable(String input) {
        int index = input.indexOf("=");
        if (index == -1) {
            return false;
        }

        String variable = input.substring(0, index);
        String value = input.substring(index+1);
        if (!StringUtility.isValidIdentifierName(variable) ||
            (!value.isEmpty() && Character.isWhitespace(value.charAt(0)))) {
            return false;
        }

        environment.setVariable(variable, value);
        return true;
    }

    /**
     * Checks if the <tt>input</tt> string contains an output redirect to a
     * file. If there is unquoted no &gt; symbol in the input, the same input is
     * returned. Else the file path is parsed and it is attempted to redirect
     * the shell output stream to the file. This method fails if the specified
     * path is already a directory. If a file already exists on the specified
     * path, user is prompted to overwrite it.
     *
     * @param input input to be checked for file redirection
     * @return input without the file redirection substring
     */
    private static String checkRedirect(String input) {
        if (input == null) {
            return input;
        }

        // Find the last unquoted '>' symbol
        int index = StringUtility.lastIndexOfUnquoted(input, 0, c -> c.equals('>'));
        if (index == -1) {
            return input;
        }

        // Find out if this is appending or creating/overwriting. Append is >>, create is >
        boolean doAppend = index > 0 && input.charAt(index - 1) == '>';

        /* Get output text and extract output file from the given input string. */
        String output = input.substring(0, index + (doAppend ? -1 : 0)).trim();
        String argument = input.substring(index+1).trim();
        Path outputFile = Utility.resolveAbsolutePath(environment, argument);

        /* Check conditions. */
        if (Files.isDirectory(outputFile)) {
            outputFile = Utility.firstAvailable(outputFile.resolve("output.txt"));
        }

        if (!doAppend && Files.exists(outputFile)) {
            boolean overwrite = CommandUtility.promptConfirm(environment, "File " + outputFile + " already exists. Overwrite?");
            if (!overwrite) {
                environment.writeln("Cancelled.");
                return output;
            }
        }

        /* Redirect the output stream. */
        try {
            BufferedWriter fileWriter = doAppend && Files.exists(outputFile) ?
                    Files.newBufferedWriter(outputFile, StandardOpenOption.APPEND) :
                    Files.newBufferedWriter(outputFile);
            environment.push(null, fileWriter);
            redirected = true;
        } catch (IOException e) {
            environment.writeln("Could not redirect stream to file: " + e.getMessage());
        }

        return output;
    }

    /**
     * Restores the environment to the state before output stream to file
     * redirection.
     */
    private static void restoreRedirect() {
        if (redirected) {
            environment.pop(true);
            redirected = false;
        }
    }

    /**
     * An environment implemented. Both reader and writer are implemented to
     * work with the standard input and output.
     *
     * @author Mario Bobic
     */
    public static class EnvironmentImpl implements Environment {

        /** Reader of this environment. May be connected to a remote machine. */
        private Stack<BufferedReader> readers = new Stack<>();
        /** Writer of this environment. May be connected to a remote machine. */
        private Stack<BufferedWriter> writers = new Stack<>();

        /** Path to user home directory. */
        private final Path homePath = Paths.get(Utility.USER_HOME);
        /** Path where the program was ran. */
        private final Path startPath = Paths.get(".").normalize().toAbsolutePath();
        /** Current path of the user positioning. */
        private Path currentPath = startPath;

        /** Map of environment variables. */
        private Map<String, String> variables = new HashMap<>();
        /** History of inputs entered by the user. */
        private List<String> history = new LinkedList<>();

        /** Prompt symbol to indicate the environment is ready. */
        private Character promptSymbol = '>';
        /** More lines symbol that is used for separating commands in multiple lines. */
        private Character morelinesSymbol = '\\';
        /** Multi-line symbol for writing commands in multiple lines. */
        private Character multilineSymbol = '|';

        /** Current ID number for marked paths. */
        private int markNum = 0;
        /** Map that associates paths with an ID number. */
        private Map<Integer, Path> markedMap = new HashMap<>();

        /** Connection object that manages client-host connections. */
        private ConnectionImpl connection = new ConnectionImpl();

        /**
         * Constructs an instance of {@code EnvironmentImpl} with default values.
         */
        public EnvironmentImpl() {
            // Call setters to store environment variables
            setPromptSymbol(promptSymbol);
            setMorelinesSymbol(morelinesSymbol);
            setMultilineSymbol(multilineSymbol);
            history.add("");
        }

        @Override
        public String readLine() throws ShellIOException {
            try {
                return reader().readLine();
            } catch (IOException e) {
                throw new ShellIOException(e);
            }
        }

        @Override
        public synchronized void write(String s) throws ShellIOException {
            if (s == null) {
                s = "null";
            }

            try {
                writer().write(s);
                writer().flush();
            } catch (IOException e) {
                throw new ShellIOException(e);
            }
        }

        @Override
        public synchronized void write(char cbuf[], int off, int len) throws ShellIOException {
            try {
                writer().write(cbuf, off, len);
                writer().flush();
            } catch (IOException e) {
                throw new ShellIOException(e);
            }
        }

        @Override
        public synchronized void writeln(String s) throws ShellIOException {
            try {
                write(s);
                writer().newLine();
                writer().flush();
            } catch (IOException e) {
                throw new ShellIOException(e);
            }
        }

        /**
         * Returns the environment reader.
         * <p>
         * The reader that is returned depends if there are any stacked readers;
         * if this environment is connected to a network or an outside member
         * has pushed a (temporary) reader, the last pushed reader is returned.
         * Else if there are no readers stacked and the standard input reader is
         * returned.
         *
         * @return the environment reader
         */
        private BufferedReader reader() {
            return readers.isEmpty() ? stdIn : readers.peek();
        }

        /**
         * Returns the environment writer.
         * <p>
         * The writer that is returned depends if there are any stacked writers;
         * if this environment is connected to a network or an outside member
         * has pushed a (temporary) writer, the last pushed writer is returned.
         * Else if there are no writers stacked and the standard output writer
         * is returned.
         *
         * @return the environment writer
         */
        private BufferedWriter writer() {
            return writers.isEmpty() ? stdOut : writers.peek();
        }

        @Override
        public void push(Reader in, Writer out) {
            if (in == null && out == null) {
                throw new IllegalArgumentException("Reader and writer cannot both be null.");
            }

            readers.push(in == null  ? reader() : new BufferedReader(in));
            writers.push(out == null ? writer() : new BufferedWriter(out));
        }

        @Override
        public void pop(boolean close) {
            if (!readers.isEmpty()) {
                BufferedReader in = readers.pop();
                BufferedWriter out = writers.pop();
                if (close) {
                    try {
                        if (!in.equals(reader())) in.close();
                        if (!out.equals(writer())) out.close();
                    } catch (IOException ignorable) {}
                }
            }
        }

        @Override
        public Iterable<ShellCommand> commands() {
            return commands.values().stream()
                .sorted((cmd1, cmd2) -> cmd1.getCommandName().compareTo(cmd2.getCommandName()))
                .collect(Collectors.toList());
        }

        @Override
        public Path getHomePath() {
            return homePath;
        }

        @Override
        public Path getStartPath() {
            return startPath;
        }

        @Override
        public Path getCurrentPath() {
            return currentPath.toAbsolutePath();
        }

        @Override
        public void setCurrentPath(Path path) {
            currentPath = path;
        }

        @Override
        public String getVariable(String name) {
            String value = variables.get(name);
            if (value == null) {
                value = System.getenv(name);
            }

            return value;
        }

        @Override
        public void setVariable(String name, String value) {
            variables.put(name, value);
        }

        @Override
        public List<String> getHistory() {
            return history;
        }

        @Override
        public void addToHistory(String input) {
            input = StringUtility.replaceUnescaped(input, Expander.LAST_INPUT, Utility.lastElement(history), 0);
            history.add(input);
        }

        @Override
        public Character getPromptSymbol() {
            return promptSymbol;
        }

        @Override
        public void setPromptSymbol(Character symbol) {
            promptSymbol = symbol;
            variables.put("PROMPT", String.valueOf(symbol));
        }

        @Override
        public Character getMorelinesSymbol() {
            return morelinesSymbol;
        }

        @Override
        public void setMorelinesSymbol(Character symbol) {
            morelinesSymbol = symbol;
            variables.put("MORELINES", String.valueOf(symbol));
        }

        @Override
        public Character getMultilineSymbol() {
            return multilineSymbol;
        }

        @Override
        public void setMultilineSymbol(Character symbol) {
            multilineSymbol = symbol;
            variables.put("MULTILINE", String.valueOf(symbol));
        }

        @Override
        public int mark(Path path) {
            markedMap.put(++markNum, path);
            setVariable(Integer.toString(markNum), path.toAbsolutePath().toString());
            return markNum;
        }

        @Override
        public void clearMarks() {
            markedMap.keySet().forEach(variables::remove);
            markedMap.clear();
            markNum = 0;
        }

        @Override
        public Path getMarked(int num) {
            return markedMap.get(num);
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public boolean isConnected() {
            return connection.isConnected();
        }

    }

    /**
     * A connection implemented. Contains {@code InputStream} and
     * {@code OutputStream} objects that can be reassigned from standard input
     * and output to the client. Contains a map that stores paths that were
     * marked for download.
     *
     * @author Mario Bobic
     */
    public static class ConnectionImpl implements Connection {

        /** Indicates if this machine has an active remote connection. */
        private boolean connected = false;
        /** Input streams to read from the client. */
        private Stack<InputStream> inFromClient = new Stack<>();
        /** Output streams to write to the client. */
        private Stack<OutputStream> outToClient = new Stack<>();

        /** Cryptographic ciphers in encryption mode. */
        private Stack<Crypto> encryptos = new Stack<>();
        /** Cryptographic ciphers in decryption mode. */
        private Stack<Crypto> decryptos = new Stack<>();

        /** Download path of this connection. */
        private Path downloadPath = Utility.getUserHomeDirectory().resolve("Downloads");

        @Override
        public void connectStreams(InputStream in, OutputStream out, Crypto encrypto, Crypto decrypto) {
            inFromClient.push(in);
            outToClient.push(out);
            assignReaderAndWriter();

            encryptos.push(requireMode(encrypto, Crypto.ENCRYPT));
            decryptos.push(requireMode(decrypto, Crypto.DECRYPT));

            connected = true;
        }

        @Override
        public void disconnectStreams() {
            try { inFromClient.pop().close(); } catch (Exception e) {}
            try { outToClient.pop().close();  } catch (Exception e) {}
            try { environment.pop(true); } catch (Exception e) {}

            if (!inFromClient.isEmpty()) {
                assignReaderAndWriter();
                encryptos.pop();
                decryptos.pop();
                return;
            }

            /* No more connections (in case of nested hosting). */
            connected = false;
        }

        /**
         * Creates reader and writer from this connection's current input and
         * output streams and pushes them to the environment.
         */
        private void assignReaderAndWriter() {
            BufferedReader in = new BufferedReader(new InputStreamReader(getInFromClient(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(getOutToClient(), StandardCharsets.UTF_8));
            environment.push(in, out);
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public InputStream getInFromClient() {
            return inFromClient.peek();
        }

        @Override
        public OutputStream getOutToClient() {
            return outToClient.peek();
        }

        @Override
        public Crypto getEncrypto() {
            return encryptos.peek();
        }

        @Override
        public Crypto getDecrypto() {
            return decryptos.peek();
        }

        /**
         * Throws an {@code IllegalArgumentException} if the specified
         * <tt>crypto</tt> is not in the specified <tt>mode</tt>.
         *
         * @param crypto crypto to be tested
         * @param mode the required mode
         * @return the given crypto
         */
        private static Crypto requireMode(Crypto crypto, boolean mode) {
            if (crypto.getMode() != mode) {
                String modeStr = (mode == Crypto.ENCRYPT ? "en" : "de") + "cryption";
                throw new IllegalArgumentException("Crypto must be in " + modeStr + " mode.");
            }

            return crypto;
        }

        @Override
        public Path getDownloadPath() {
            return downloadPath;
        }

        @Override
        public void setDownloadPath(Path path) {
            if (!Files.isDirectory(path)) {
                try {
                    Files.createDirectories(path);
                } catch (FileAlreadyExistsException e) {
                    String m = "A file by the name of " + path + " already exists.";
                    throw new IllegalPathException(m);
                } catch (IOException e) {
                    throw new IllegalPathException(e);
                }
            }

            this.downloadPath = path;
        }

    }

}
