package hr.fer.zemris.java.shell.commands.listing;

import hr.fer.zemris.java.shell.ShellStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.Utility;
import hr.fer.zemris.java.shell.utility.exceptions.InvalidFlagException;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A command that recursively passes the directory tree, lists all extensions
 * and for each extension shows how many times it appeared.
 *
 * @author Mario Bobic
 */
public class FileTypesCommand extends VisitorCommand {

    /** A comparator that compares types by their count, smallest first. */
    private static final Comparator<TypeCount> COMP_COUNT = Comparator.comparingInt(TypeCount::getCount).thenComparing(TypeCount::getType);
    /** A comparator that compares types by their name, lexicographically smallest first. */
    private static final Comparator<TypeCount> COMP_TYPE = Comparator.comparing(TypeCount::getType);

    /** Delimiter used by the pretty flag. */
    private static final String PRETTY_DELIMITER = ";";

    /* Flags */
    /** Indicates if mime type should be used instead of extension. */
    private boolean useMimeType;
    /** Indicates if only mime type, without subtype should be shown. */
    private boolean useTypeOnly;
    /** Indicates if pretty view should be used. */
    private boolean usePretty;
    /** Indicates if different casing for same extensions means distinct types. */
    private boolean caseSensitive;
    /** Defines which ordering to use. */
    private Comparator<TypeCount> orderComparator;

    /**
     * Constructs a new command object of type {@code CountCommand}.
     */
    public FileTypesCommand() {
        super("FILETYPES", createCommandDescription(), createFlagDescriptions());
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
        desc.add("Recursively passed the directory tree and lists all file types using extension or mime type.");
        desc.add("For each file type, the command shows how many times it appeared.");
        desc.add("File types that are categorized in mime types are shown by type/subtype.");
        return desc;
    }

    /**
     * Creates a list of {@code FlagDescription} objects where each entry
     * describes the available flags of this command. This method is generates
     * description exclusively for the command that this class represents.
     *
     * @return a list of strings that represents description
     */
    private static List<FlagDescription> createFlagDescriptions() {
        List<FlagDescription> desc = new ArrayList<>();
        desc.add(new FlagDescription("m", "mime-type", null, "Show mime type instead of extension."));
        desc.add(new FlagDescription("t", "type-only", null, "Show just type without subtype."));
        desc.add(new FlagDescription("p", "pretty", null, "Show pretty view."));
        desc.add(new FlagDescription("c", "case-sensitive", null, "Different casing for same extensions means distinct types."));
        desc.add(new FlagDescription("o", "order", "[!]count|type", "Order by count or type (! means reversed)."));
        return desc;
    }

    @Override
    protected String compileFlags(Environment env, String s) {
        /* Initialize default values. */
        useMimeType = false;
        useTypeOnly = false;
        usePretty = false;
        caseSensitive = false;
        orderComparator = COMP_COUNT.reversed();

        /* Compile! */
        s = commandArguments.compile(s);

        /* Replace default values with flag values, if any. */
        if (commandArguments.containsFlag("m", "mime-type")) {
            useMimeType = true;
        }

        if (commandArguments.containsFlag("t", "type-only")) {
            useTypeOnly = true;
        }

        if (commandArguments.containsFlag("p", "pretty")) {
            usePretty = true;
        }

        if (commandArguments.containsFlag("c", "case-sensitive")) {
            caseSensitive = true;
        }

        if (commandArguments.containsFlag("o", "order")) {
            String order = commandArguments.getFlag("o", "order").getArgument();
            orderComparator = getOrderComparator(order);
        }

        return super.compileFlags(env, s);
    }

    @Override
    protected ShellStatus execute0(Environment env, String s) throws IOException {
        Path path = s == null ?
            env.getCurrentPath() : Utility.resolveAbsolutePath(env, s);

        Utility.requireDirectory(path);

        FileTypesFileVisitor filetypesVisitor = new FileTypesFileVisitor();
        walkFileTree(path, filetypesVisitor);

        Map<String, Integer> mimeTypes = filetypesVisitor.getFileTypes();
        int fails = filetypesVisitor.getFails();

        if (usePretty) {
            prettyPrint(env, mimeTypes);
        } else {
            mimeTypes.entrySet()
                    .stream()
                    .map(TypeCount::new)
                    .sorted(orderComparator)
                    .map(TypeCount::toString)
                    .forEach(env::writeln);
        }
        if (fails != 0) {
            env.writeln("Failed to access " + fails + " folders.");
        }

        return ShellStatus.CONTINUE;
    }

    /**
     * Prints detected file types in a pretty way.
     *
     * @param env an environment
     * @param mimeTypes mime types and their count
     */
    private void prettyPrint(Environment env, Map<String, Integer> mimeTypes) {
        Map<String, List<TypeCount>> typeMap = new HashMap<>();

        // Fill typeMap with key=mimeType, value=list<extension,count>
        mimeTypes.forEach((key, value) -> {
            int index = key.indexOf(PRETTY_DELIMITER);
            String mimeType = key.substring(0, index);
            if ("null".equals(mimeType)) {
                mimeType = null;
            }

            String extension = key.substring(index + 1);
            TypeCount tc = new TypeCount(extension, value);
            typeMap.compute(mimeType, (k, v) -> v == null ? new LinkedList<>() : v).add(tc);
        });

        // Get an easily-sortable TypeCount map
        Map<TypeCount, List<TypeCount>> typeCountMap = new HashMap<>();
        typeMap.forEach((type, list) -> {
            int totalCount = list.stream().mapToInt(TypeCount::getCount).sum();
            TypeCount typeCount = new TypeCount(type, totalCount);
            typeCountMap.put(typeCount, list);
        });

        typeCountMap.values().forEach(list -> list.sort(orderComparator));
        typeCountMap.entrySet()
                .stream()
                .sorted((e1, e2) -> orderComparator.compare(e1.getKey(), e2.getKey()))
                .forEach(e -> {
                    env.writeln(e.getKey());
                    e.getValue().forEach(tc -> env.writeln(" |- " + tc));
                });
    }

    /**
     * Returns an appropriate comparator for the specified <tt>order</tt>.
     *
     * @param order order
     * @return type and count comparator with the specified order
     * @throws InvalidFlagException if <tt>order</tt> is not a valid argument
     */
    private Comparator<TypeCount> getOrderComparator(String order) throws InvalidFlagException {
        if (order == null) {
            throw new InvalidFlagException("You must specify order, e.g. '!count' or 'type'.", "o");
        }

        boolean reverse = order.startsWith("!");
        if (reverse) {
            order = order.substring(1);
        }

        Comparator<TypeCount> comp;
        if (order.equalsIgnoreCase("count")) {
            comp = COMP_COUNT;
        } else if (order.equalsIgnoreCase("type")) {
            comp = COMP_TYPE;
        } else {
            throw new InvalidFlagException("Invalid order argument: " + order, "o");
        }

        return reverse ? comp.reversed() : comp;
    }

    /**
     * Type and count data structure.
     */
    private static class TypeCount {
        private final String type;
        private final int count;

        public TypeCount(String type, int count) {
            this.type = type;
            this.count = count;
        }

        public TypeCount(Map.Entry<String, Integer> entry) {
            this(entry.getKey(), entry.getValue());
        }

        public String getType() {
            if (type == null) {
                return "Unknown";
            } else if (type.isEmpty()) {
                return "No extension";
            }

            return type;
        }

        public int getCount() {
            return count;
        }

        @Override
        public String toString() {
            return getType() + " [" + getCount() + "]";
        }
    }

    /**
     * A {@linkplain SimpleFileVisitor} extended and used to serve the
     * {@linkplain FileTypesCommand}.
     *
     * @author Mario Bobic
     */
    public class FileTypesFileVisitor extends SimpleFileVisitor<Path> {

        /** Mime types this visitor has encountered. */
        private Map<String, Integer> fileTypes = new HashMap<>();

        /** Number of files and folders that failed to be accessed. */
        private int fails = 0;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String type;
            String extension = caseSensitive ? Utility.extension(file) : Utility.extension(file).toLowerCase();
            if (usePretty) {
                type = getMainType(Files.probeContentType(file)) + PRETTY_DELIMITER + extension;
            } else if (useMimeType || useTypeOnly) {
                type = useTypeOnly ? getMainType(Files.probeContentType(file)) : Files.probeContentType(file);
            } else {
                type = extension;
            }

            fileTypes.putIfAbsent(type, 0);
            fileTypes.computeIfPresent(type, (key, count) -> count + 1);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            fails++;
            return FileVisitResult.CONTINUE;
        }

        /**
         * Returns a map of mime types and their count.
         *
         * @return a map of mime types and their count
         */
        public Map<String, Integer> getFileTypes() {
            return fileTypes;
        }

        /**
         * Returns the number of folders that failed to be accessed.
         *
         * @return the number of folders that failed to be accessed
         */
        public int getFails() {
            return fails;
        }

        /**
         * Parses and returns just the type from mime type structure
         * <tt>type/subtype</tt>.
         *
         * @param mimeType mime type to be parsed
         * @return the main type, without subtype
         */
        private String getMainType(String mimeType) {
            if (mimeType == null) return null;

            int separatorIndex = mimeType.indexOf("/");
            return mimeType.substring(0, separatorIndex);
        }

    }

}
