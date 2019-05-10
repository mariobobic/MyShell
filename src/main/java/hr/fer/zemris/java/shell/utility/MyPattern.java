package hr.fer.zemris.java.shell.utility;

import java.util.regex.Pattern;

/**
 * Represents a pattern that can be given either as a {@code String} or a
 * {@code Pattern}. If a string is given, it is decompiled to pattern parts
 * using the {@link StringUtility#splitPattern(String)} method. Else the
 * pattern is already a compiled representation of a regular expression.
 * <p>
 * This class contains a {@link #matches(String)} method that matches the
 * specified input string to the argument given in the constructor.
 *
 * @author Mario Bobic
 */
public class MyPattern {

    /** Parts of the pattern to be matched against. */
    private String[] patternParts;
    /** Indicates if the pattern parts are case sensitive. */
    private boolean caseSensitive;

    /** Regular expression pattern to be matched against. */
    private Pattern regexPattern;

    /**
     * Constructs an instance of {@code MyPattern} with the specified string
     * pattern.
     *
     * @param pattern a string pattern possibly containing asterisks
     */
    public MyPattern(String pattern) {
        this(pattern, false);
    }

    /**
     * Constructs an instance of {@code MyPattern} with the specified string
     * pattern.
     *
     * @param pattern a string pattern possibly containing asterisks
     */
    public MyPattern(String pattern, boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        this.patternParts = StringUtility.splitPattern(
                caseSensitive ? pattern : pattern.toLowerCase());
    }

    /**
     * Constructs an instance of {@code MyPattern} with the specified
     * regular expression pattern.
     *
     * @param regex a compiled representation of a regular expression
     */
    public MyPattern(Pattern regex) {
        this.regexPattern = regex;
    }

    /**
     * Returns true if the specified <tt>input</tt> matches this pattern.
     *
     * @param input input to be matched against
     * @return true if the specified input matches this pattern
     */
    public boolean matches(String input) {
        if (patternParts != null) {
            return StringUtility.matches(caseSensitive ? input : input.toLowerCase(), patternParts);
        } else {
            return regexPattern.matcher(input).matches();
        }
    }
}
