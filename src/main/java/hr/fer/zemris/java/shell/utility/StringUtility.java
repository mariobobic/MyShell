package hr.fer.zemris.java.shell.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A String helper class. Provides helper methods for string manipulation.
 *
 * @author Mario Bobic
 */
public abstract class StringUtility {

    /** The quote character. */
    private static final Character QUOT_CHR = '"';
    /** The quote string. */
    private static final String QUOT_STR = "\"";

    /** Group index pattern for target regex. */
    private static final Pattern REGEX_GROUP_INDEX_PATTERN = Pattern.compile("\\\\\\d+");

    /**
     * Disable instantiation or inheritance.
     */
    private StringUtility() {
    }

    /**
     * Extracts arguments from the specified string <tt>s</tt>. This method
     * supports an unlimited number of arguments, and can be entered either with
     * quotation marks or not. Returns an array of strings containing the
     * extracted arguments.
     * <p>
     * This is the same as calling the {@link #extractArguments(String, int)
     * extractArguments(s, 0)} method.
     *
     * @param s a string containing arguments
     * @return an array of strings containing extracted arguments.
     */
    public static String[] extractArguments(String s) {
        return extractArguments(s, 0);
    }

    /**
     * Extracts arguments from the specified string <tt>s</tt>. This method
     * splits arguments until it runs out of matches or reaches the specified
     * <tt>limit</tt>, which ever comes first. Arguments can be entered either
     * with quotation marks or not. Returns an array of strings containing the
     * extracted arguments.
     *
     * @param s a string containing arguments
     * @param limit limit of splitting the arguments, 0 for no limit
     * @return an array of strings containing extracted arguments.
     */
    public static String[] extractArguments(String s, int limit) {
        return extractArguments(s, limit, false);
    }

    /**
     * Extracts arguments from the specified string <tt>s</tt>. This method
     * splits arguments until it runs out of matches or reaches the specified
     * <tt>limit</tt>, which ever comes first. Arguments can be entered either
     * with quotation marks or not. If the <tt>keepQuots</tt> parameter is true,
     * quotation marks are kept for the quoted argument. Returns an array of
     * strings containing the extracted arguments.
     *
     * @param s a string containing arguments
     * @param limit limit of splitting the arguments, 0 for no limit
     * @param keepQuots true if quotation marks should not be removed
     * @return an array of strings containing extracted arguments.
     */
    public static String[] extractArguments(String s, int limit, boolean keepQuots) {
        if (s == null) return new String[0];
        if (!keepQuots && limit == 1 && s.startsWith(QUOT_STR) && s.endsWith(QUOT_STR)) {
            String noQuots = s.substring(1, s.length() - 1);
            return new String[] { noQuots };
        }

        List<String> list = new ArrayList<>();

        String regex = "\"([^\"]*)\"|(\\S+)";
        Matcher m = Pattern.compile(regex).matcher(s);
        while (m.find()) {
            if (--limit == 0 && !m.hitEnd()) {
                list.add(s.substring(m.start()).trim());
                break;
            }

            String arg;
            if (m.group(1) != null) {
                arg = keepQuots ? quote(m.group(1)) : m.group(1);
            } else {
                arg = m.group(2);
            }
            list.add(arg);
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Splits the specified <tt>pattern</tt> string around matches of asterisk
     * symbols.
     *
     * @param pattern pattern to be split
     * @return the array of strings computed by splitting this string around
     *         matches of asterisk symbols
     */
    public static String[] splitPattern(String pattern) {
        // (?<!\\)    Matches if the preceding character is not a backslash
        // (?:\\\\)*  Matches any number of occurrences of two backslashes
        // \*         Matches an asterisk
        String[] split = pattern.split("(?<!\\\\)(?:\\\\\\\\)*\\*");
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].replace("\\*", "*");
        }

        return split;
    }

    /**
     * Returns a name specified by <tt>regex</tt> with all available
     * groups replaced using <tt>regexMatcher</tt>.
     *
     * @param regex regex of which the target name is created
     * @param regexMatcher object containing text from regex groups
     * @return target name with all regex groups replaced
     */
    public static String getTargetNameFromRegex(String regex, Matcher regexMatcher) {
        Matcher matcher = REGEX_GROUP_INDEX_PATTERN.matcher(regex);

        String targetName = regex;
        while (matcher.find()) {
            String match = regex.substring(matcher.start(), matcher.end());
            int regexGroup = Integer.parseInt(match.substring(1));

            String replacement = regexMatcher.group(regexGroup);
            targetName = targetName.replace(match, replacement);
        }

        return targetName;
    }

    /**
     * Returns true if the given param {@code str} matches the {@code pattern}.
     * The {@code pattern} can contain asterisk characters ("*") that represent
     * 0 or more characters between the parts that are matching.
     * <p>
     * For an example, pattern &quot;*.java&quot; will list all files containing
     * the .java character sequence.
     *
     * @param str a string that is being examined
     * @param pattern a pattern that may contain the asterisk character
     * @return true if {@code str} matches the {@code pattern}. False otherwise
     */
    public static boolean matches(String str, String pattern) {
        return matches(str, splitPattern(pattern));
    }

    /**
     * Returns true if the given param {@code str} matches the
     * {@code patternParts}. The {@code patternParts} are parts of the pattern
     * which all must match the specified {@code str}. There may be 0 or more
     * characters between each part of the pattern, which will be ignored.
     * <p>
     * For an example, the pattern parts [&quot;M&quot;, &quot;.java&quot;] will
     * list all files containing the letter 'M' before the .java literal.
     *
     * @param str a string that is being examined
     * @param patternParts parts of the pattern to be matched against
     * @return true if {@code str} matches the {@code patternParts}. False otherwise
     */
    public static boolean matches(String str, String[] patternParts) {
        int lastIndex = -1;
        for (String part : patternParts) {
            int index = str.indexOf(part);
            if (index <= lastIndex) return false;
            else lastIndex = index;
        }

        return true;
    }

    /**
     * Replaces the first subsequence of the <tt>source</tt> string that matches
     * the literal target string with the specified literal replacement string.
     *
     * @param source source string on which the replacement is made
     * @param target the string to be replaced
     * @param replacement the replacement string
     * @return the resulting string
     */
    public static String replaceFirst(String source, String target, String replacement) {
        return replaceFirst(source, target, replacement, 0);
    }

    /**
     * Replaces the first subsequence of the <tt>source</tt> string that matches
     * the literal target string with the specified literal replacement string,
     * starting at the specified index.
     *
     * @param source source string on which the replacement is made
     * @param target the string to be replaced
     * @param replacement the replacement string
     * @param fromIndex the index from which to start the search
     * @return the resulting string
     */
    public static String replaceFirst(String source, String target, String replacement, int fromIndex) {
        int index = source.indexOf(target, fromIndex);
        if (index == -1) {
            return source;
        }

        return source.substring(0, index)
            .concat(replacement)
            .concat(source.substring(index+target.length()));
    }

    /**
     * Replaces all subsequences of the <tt>source</tt> string that matches the
     * literal target string with the specified literal replacement string, from
     * beginning to the end.
     *
     * @param source source string on which the replacement is made
     * @param target the string to be replaced
     * @param replacement the replacement string
     * @return the resulting string
     */
    public static String replaceUnescaped(String source, String target, String replacement) {
        return replaceUnescaped(source, target, replacement, 0);
    }

    /**
     * Replaces all subsequences of the <tt>source</tt> string that matches the
     * literal target string with the specified literal replacement string,
     * starting at the specified index.
     *
     * @param source source string on which the replacement is made
     * @param target the string to be replaced
     * @param replacement the replacement string
     * @param fromIndex the index from which to start the search
     * @return the resulting string
     */
    public static String replaceUnescaped(String source, String target, String replacement, int fromIndex) {
        int index = source.indexOf(target, fromIndex);
        if (index == -1) {
            return source;
        }

        // charsAtEquals -> if character at current index is \ and the next one is also \
        if (isEscaped(source, index) || charsAtEqual(source, index, index+1, '\\')) {
            return replaceUnescaped(source, target, replacement, index+1);
        }

        source = replaceFirst(source, target, replacement, index);
        return replaceUnescaped(source, target, replacement, index-lengthDifference(target, replacement));
    }

    /**
     * Returns a string that is a concatenation of two substrings. The first
     * substring begins at the beginning and extends to the character at index
     * {@code index - 1} and the second substring begins at {@code index+1} and
     * extends to the end. Thus the length of the substring is
     * {@code source.length()-1}.
     * <p>
     * In other words, this method <strong>removes</strong> a character from
     * <tt>source</tt> at the specified index.
     * <p>
     * Examples:
     * <blockquote><pre>
     * removeCharAt("window", 2) returns "widow"
     * removeCharAt("colour", 4) returns "color"
     * </pre></blockquote>
     *
     * @param source the source string
     * @param index index of a character to be removed
     * @return the specified string with removed substring
     * @throws IndexOutOfBoundsException if the {@code index} is negative or
     *         larger than the length of {@code source} string
     */
    public static String removeCharAt(String source, int index) {
        return removeSubstring(source, index, index+1);
    }

    /**
     * Returns a string that is a concatenation of two substrings. The first
     * substring begins at the beginning and extends to the character at index
     * {@code fromIndex - 1} and the second substring begins at {@code toIndex}
     * and extends to the end. Thus the length of the substring is
     * {@code source.length()-(toIndex-fromIndex)}.
     * <p>
     * In other words, this method <strong>removes</strong> a substring from
     * <tt>source</tt> between the specified indices, where {@code fromIndex} is
     * inclusive and {@code toIndex} is exclusive.
     * <p>
     * Examples:
     * <blockquote><pre>
     * removeSubstring("monitor", 2, 4) returns "motor"
     * removeSubstring("wallet", 1, 4) returns "wet"
     * </pre></blockquote>
     *
     * @param source the source string
     * @param fromIndex the beginning index, inclusive
     * @param toIndex the ending index, exclusive
     * @return the specified string with removed substring
     * @throws IndexOutOfBoundsException if the {@code fromIndex} is negative,
     *         or {@code toIndex} is larger than the length of {@code source}
     *         string, or {@code fromIndex} is larger than {@code toIndex}
     */
    public static String removeSubstring(String source, int fromIndex, int toIndex) {
        return source.substring(0, fromIndex).concat(source.substring(toIndex));
    }

    /**
     * Counts the number of occurrences of the specified char in a string.
     *
     * @param string string from which the occurrences are counted
     * @param c character whose occurrences are to be counted
     * @return the number of occurrences of the specified char in a string
     */
    public static int countOccurrencesOf(String string, char c) {
        int sum = 0;

        for (int i = 0, n = string.length(); i < n; i++) {
            if (string.charAt(i) == c) sum++;
        }

        return sum;
    }

    /**
     * Returns the difference between the lengths of string <tt>str1</tt> and
     * <tt>str2</tt>, or more formally {@code str1.length() - str2.length()}.
     *
     * @param str1 first string
     * @param str2 second string
     * @return length of str1 minus length of str2
     */
    public static int lengthDifference(String str1, String str2) {
        return str1.length() - str2.length();
    }

    /**
     * Returns the index within the specified string <tt>str</tt> of the first
     * occurrence of a whitespace character determined by the
     * {@link Character#isWhitespace(char)} method.
     *
     * @param str string whose index of the first whitespace is to be returned
     * @return the index of the first occurrence of a whitespace character
     */
    public static int indexOfWhitespace(String str) {
        return indexOfWhitespace(str, 0);
    }

    /**
     * Returns the index within the specified string <tt>str</tt> of the first
     * occurrence of a whitespace character determined by the
     * {@link Character#isWhitespace(char)} method, starting at the specified
     * index.
     *
     * @param str string whose index of the first whitespace is to be returned
     * @param fromIndex the index from which to start the search
     * @return the index of the first occurrence of a whitespace character
     */
    public static int indexOfWhitespace(String str, int fromIndex) {
        return indexOf(str, fromIndex, Character::isWhitespace);
    }

    /**
     * Returns the index within the specified string <tt>str</tt> of the first
     * occurrence of a non Unicode identifier character determined by the
     * !{@link Character#isUnicodeIdentifierPart(char)} method, starting at the
     * specified index.
     *
     * @param str string whose index of the first whitespace is to be returned
     * @param fromIndex the index from which to start the search
     * @return the index of the first occurrence of a whitespace character
     */
    public static int indexOfNonIdentifier(String str, int fromIndex) {
        return indexOf(str, fromIndex, not(Character::isUnicodeIdentifierPart));
    }

    /**
     * Returns the index within the specified string <tt>str</tt> of the first
     * occurrence of a character that satisfies the specified
     * <tt>predicate</tt>, starting at the specified index.
     *
     * @param str string whose index of the first character that satisfies the
     *        predicate is to be returned
     * @param fromIndex the index from which to start the search
     * @param predicate predicate to be satisfied
     * @return the index of the first occurrence of a character that satisfies
     *         the given predicate
     */
    public static int indexOf(String str, int fromIndex, Predicate<Character> predicate) {
        if (fromIndex < 0) {
            fromIndex = 0;
        }

        for (int i = fromIndex, n = str.length(); i < n; i++) {
            if (predicate.test(str.charAt(i))) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns the index within the specified string <tt>str</tt> of the first
     * occurrence of a character that is not under double quotation-marks and
     * satisfies the specified <tt>predicate</tt>, starting at the specified
     * index.
     *
     * @param str string whose index of the first character that satisfies the
     *        predicate is to be returned
     * @param fromIndex the index from which to start the search
     * @param predicate predicate to be satisfied
     * @return the index of the first occurrence of a character that satisfies
     *         the given predicate that is not under quotes
     */
    public static int indexOfUnquoted(String str, int fromIndex, Predicate<Character> predicate) {
        if (fromIndex < 0) {
            fromIndex = 0;
        }

        boolean underQuote = false;
        for (int i = fromIndex, n = str.length(); i < n; i++) {
            char c = str.charAt(i);
            if (c == '"' && !isEscaped(str, i)) {
                underQuote = !underQuote;
                continue;
            }

            if (!underQuote && predicate.test(c)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns the index within the specified string <tt>str</tt> of the last
     * occurrence of a character that is not under double quotation-marks and
     * satisfies the specified <tt>predicate</tt>, starting at the specified
     * index.
     *
     * @param str string whose index of the last character that satisfies the
     *        predicate is to be returned
     * @param fromIndex the index from which to start the search
     * @param predicate predicate to be satisfied
     * @return the index of the last occurrence of a character that satisfies
     *         the given predicate that is not under quotes
     */
    public static int lastIndexOfUnquoted(String str, int fromIndex, Predicate<Character> predicate) {
        if (fromIndex < 0) {
            fromIndex = 0;
        }

        boolean underQuote = false;
        for (int i = str.length()-1; i >= fromIndex; i--) {
            char c = str.charAt(i);
            if (c == '"' && !isEscaped(str, i)) {
                underQuote = !underQuote;
                continue;
            }

            if (!underQuote && predicate.test(c)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns the negated predicate of the specified <tt>predicate</tt>.
     *
     * @param <T> predicate type
     * @param predicate predicate to be negated
     * @return the negated predicate
     */
    private static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }

    /**
     * Returns true if the character at the specified <tt>index</tt> is equal to
     * the specified character <tt>c</tt>.
     * <p>
     * If the specified index is greater or equal to the length of the string,
     * this method returns false.
     * <p>
     * This method is convenient as it does not throw an
     * {@code IndexOutOfBoundsException} if index is out of bounds.
     *
     * @param str string to be tested
     * @param index index in the string
     * @param c character to be tested
     * @return true if char at <tt>index</tt> is equal to the specified
     *         character
     */
    public static boolean charAtEquals(String str, int index, char c) {
        return charsAtEqual(str, index, index, c);
    }

    /**
     * Returns true if characters from <tt>index0</tt> to <tt>index1</tt>, both
     * inclusive, are equal to the specified character <tt>c</tt>.
     * <p>
     * If <tt>index0</tt> is negative or <tt>index1</tt> is greater or equal to
     * the length of the string, this method returns false.
     * <p>
     * This method is convenient as it does not throw an
     * {@code IndexOutOfBoundsException} if indices are out of bounds.
     *
     * @param str string to be tested
     * @param index0 start index, <strong>inclusive</strong>
     * @param index1 end index, <strong>inclusive</strong>
     * @param c character to be tested
     * @return true if chars at indices index0 through index1 all equal the
     *         specified character
     */
    public static boolean charsAtEqual(String str, int index0, int index1, char c) {
        return charsAtSatisfy(str, index0, index1, Predicate.isEqual(c));
    }

    /**
     * Returns true if the character at the specified <tt>index</tt> satisfies
     * the specified <tt>predicate</tt>.
     * <p>
     * If the specified index is negative or greater or equal to the length of
     * the string, this method returns false.
     * <p>
     * This method is convenient as it does not throw an
     * {@code IndexOutOfBoundsException} if index is out of bounds.
     *
     * @param str string to be tested
     * @param index index in the string
     * @param predicate predicate to test
     * @return true if char at <tt>index</tt> satisfies the predicate
     */
    public static boolean charAtSatisfies(String str, int index, Predicate<Character> predicate) {
        return charsAtSatisfy(str, index, index, predicate);
    }

    /**
     * Returns true if characters from <tt>index0</tt> to <tt>index1</tt>, both
     * inclusive, satisfy the specified <tt>predicate</tt>.
     * <p>
     * If <tt>index0</tt> is negative or <tt>index1</tt> is greater or equal to
     * the length of the string, this method returns false.
     * <p>
     * This method is convenient as it does not throw an
     * {@code IndexOutOfBoundsException} if indices are out of bounds.
     *
     * @param str string to be tested
     * @param index0 start index, <strong>inclusive</strong>
     * @param index1 end index, <strong>inclusive</strong>
     * @param predicate predicate to test all characters from index0 to index1
     * @return true if char at <tt>index</tt> satisfies the predicate
     */
    public static boolean charsAtSatisfy(String str, int index0, int index1, Predicate<Character> predicate) {
        for (int i = index0; i <= index1; i++) {
            if (isIndexOutOfBounds(str, i))		return false;
            if (!predicate.test(str.charAt(i)))	return false;
        }

        return true;
    }

    /**
     * Returns <tt>true</tt> if the specified <tt>index</tt> is out of the
     * string's bounds. False otherwise
     *
     * @param str a string
     * @param index index of the string
     * @return true if index is out of bounds
     */
    public static boolean isIndexOutOfBounds(String str, int index) {
        return index < 0 || index >= str.length();
    }

    /**
     * Returns <tt>true</tt> if a character sequence starting at the specified
     * <tt>start</tt> index of the <tt>input</tt> is preceded by an escape
     * symbol.
     * <p>
     * The escape symbol itself <strong>must not be escaped</strong> in order
     * for this method to return true.
     *
     * @param input the whole input
     * @param start start of the character sequence
     * @return true if a character before <tt>start</tt> is an escape symbol
     */
    public static boolean isEscaped(String input, int start) {
        return start > 0 && input.charAt(start-1) == '\\'
            && !isEscaped(input, start-1);
    }

    /**
     * Determines if the specified <tt>name</tt> is a Unicode identifier.
     * <p>
     * If the specified name is an empty string, false is returned. Else the
     * result is determined by the
     * {@link Character#isUnicodeIdentifierStart(char)} and
     * {@link Character#isUnicodeIdentifierPart(char)} methods.
     *
     * @param name the name to be tested
     * @return true if the specified name is a Java identifier
     */
    public static boolean isValidIdentifierName(String name) {
        if (name.isEmpty()) {
            return false;
        }

        if (!Character.isUnicodeIdentifierStart(name.charAt(0)) && name.charAt(0) != '_') {
            return false;
        }

        for (int i = 1, n = name.length(); i < n; i++) {
            if (!Character.isUnicodeIdentifierPart(name.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Surrounds all the specified arguments with double quotes, delimiting each
     * of them with a space and returns the generated string. The specified
     * array of objects may contain <tt>null</tt> values.
     * <p>
     * If no parameters are specified, an empty string is returned.
     *
     * @param args arguments to be surrounded with quotes, not null
     * @return arguments wrapped in quotes, separated by a space
     */
    public static String quote(Object...args) {
        StringBuilder sb = new StringBuilder();

        for (Object arg : args) {
            sb.append(QUOT_CHR).append(arg).append(QUOT_CHR);
            sb.append(" ");
        }
        // delete last space
        if (sb.length() > 0) {
            sb.setLength(sb.length()-1);
        }

        return sb.toString();
    }

    /**
     * Returns the current index prefixed with a string of leading zeroes.
     *
     * The amount of leading zeros necessary for the current index is calculated
     * using the end index.
     *
     * @param currentIndex index of the current processing item
     * @param endIndex final index, inclusive
     * @return a string of leading zeroes
     */
    public static String getIndexWithLeadingZeros(long currentIndex, long endIndex) {
        int numZeroes = Long.toString(endIndex).length() - Long.toString(currentIndex).length();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numZeroes; i++) {
            sb.append('0');
        }

        sb.append(currentIndex);
        return sb.toString();
    }

}
