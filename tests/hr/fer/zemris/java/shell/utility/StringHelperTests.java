package hr.fer.zemris.java.shell.utility;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests the functionality of {@link StringHelper} utility class.
 *
 * @author Mario Bobic
 */
@SuppressWarnings("javadoc")
public class StringHelperTests {
	
	/* --------------------------- StringHelper tests --------------------------- */
	
	@Test
	public void testExtractArguments1() {
		String str = "This string should contain \"exactly 5 arguments\"";
		
		String[] args = StringHelper.extractArguments(str);
		int expectedLength = 5;
		int actualLength = args.length;
		
		assertEquals(expectedLength, actualLength);
	}
	
	@Test
	public void testExtractArguments2() {
		String str = "This\t   string \nshould  contain \"exactly 3 arguments\" for sure";
		
		String[] args = StringHelper.extractArguments(str, 3);
		int expectedLength = 3;
		int actualLength = args.length;
		
		assertEquals(expectedLength, actualLength);
	}
	
	@Test
	public void testExtractArguments3() {
		String str = " .";
		
		String[] args = StringHelper.extractArguments(str, 1);
		int expectedLength = 1;
		int actualLength = args.length;
		
		assertEquals(expectedLength, actualLength);
	}
	
	@Test
	public void testExtractArgumentsEmptyString() {
		String str = "";
		
		String[] args = StringHelper.extractArguments(str);
		int expectedLength = 0;
		int actualLength = args.length;
		
		assertEquals(expectedLength, actualLength);
	}
	
	@Test
	public void testExtractArgumentsNull() {
		String str = null;
		
		String[] args = StringHelper.extractArguments(str);
		int expectedLength = 0;
		int actualLength = args.length;
		
		assertEquals(expectedLength, actualLength);
	}
	
	@Test
	public void testExtractArgumentsKeepQuots() {
		String str = "Keep these \"quotes\" please.";
		
		String[] args = StringHelper.extractArguments(str, 0, true);
		
		int expectedLength = 4;
		int actualLength = args.length;
		assertEquals(expectedLength, actualLength);
		
		String[] expected = {"Keep", "these", "\"quotes\"", "please."};
		assertArrayEquals(expected, args);
	}
	
	@Test
	public void testSplitPattern() {
		String pattern = "three*pattern*parts";
		
		String[] expected = {"three", "pattern", "parts"};
		String[] actual = StringHelper.splitPattern(pattern);
		
		assertArrayEquals(expected, actual);
	}
	
	@Test
	public void testSplitPatternEscapedAsterisk() {
		String pattern = "escaped\\*asterisk";
		
		String[] expected = {"escaped*asterisk"};
		String[] actual = StringHelper.splitPattern(pattern);
		
		assertArrayEquals(expected, actual);
	}
	
	@Test
	public void testMatches() {
		String pattern = "M*.java";

		assertTrue(StringHelper.matches("MyShell.java", pattern));
		assertTrue(StringHelper.matches("MyShellTests.java", pattern));

		assertFalse(StringHelper.matches("StringHelper.java", pattern));
		assertFalse(StringHelper.matches("MyShell.class", pattern));
	}
	
	@Test
	public void testMatchesPatternParts() {
		String[] pattern = {"M", ".java"};

		assertTrue(StringHelper.matches("MyShell.java", pattern));
		assertTrue(StringHelper.matches("MyShellTests.java", pattern));

		assertFalse(StringHelper.matches("StringHelper.java", pattern));
		assertFalse(StringHelper.matches("MyShell.class", pattern));
	}
	
	@Test
	public void testReplaceFirst() {
		String var1 = "value1";
		String str = "Value of variable var1 is $var1";
		
		String expected = str.replace("$var1", var1);
		String actual = StringHelper.replaceFirst(str, "$var1", var1);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testReplaceFirstFromIndex() {
		String str = "folder{1..5}/file{1..5}";
		int fromIndex = str.indexOf("/");
		
		String expected = "folder{1..5}/fileXXX";
		String actual = StringHelper.replaceFirst(str, "{1..5}", "XXX", fromIndex);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testReplaceUnescaped() {
		// plain:     file\folder
		String str = "file-\\folder";
		
		String expected = "mile-\\folder";
		String actual = StringHelper.replaceUnescaped(str, "f", "m");
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testReplaceUnescapedEscapeSymbol() {
		// plain:     \\
		String str = "\\\\";
		
		String expected = str;
		String actual = StringHelper.replaceUnescaped(str, "\\", "/");
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testReplaceUnescapedEscapeSymbolLast() {
		// plain:     \\
		String str = "This is text.\\";
		
		String expected = "This is text./";
		String actual = StringHelper.replaceUnescaped(str, "\\", "/");
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testReplaceUnescapedMultipleEscapes() {
		// plain:     \\\
		String str = "\\\\\\";
		
		// plain:          \\.
		String expected = "\\\\.";
		String actual = StringHelper.replaceUnescaped(str, "\\", ".");
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testReplaceUnescapedFromIndex() {
		// plain:     This\ should not be\ replaced. This should.
		String str = "This\\ should not be\\ replaced. This should.";
		
		String expected = "This\\ should not be\\ replaced._This_should.";
		String actual = StringHelper.replaceUnescaped(str, " ", "_", 17); // be
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testRemoveCharAt() {
		String str = "window";
		
		String expected = "widow";
		String actual = StringHelper.removeCharAt(str, 2);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testRemoveSubstring1() {
		String str = "monitor";
		
		String expected = "motor";
		String actual = StringHelper.removeSubstring(str, 2, 4);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testRemoveSubstring2() {
		String str = "wallet";
		
		String expected = "wet";
		String actual = StringHelper.removeSubstring(str, 1, 4);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testCountOccurrencesOf1() {
		String str = "Test count occurrences.";
		
		int expected = 2;
		int actual = StringHelper.countOccurrencesOf(str, ' ');
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testCountOccurrencesOf2() {
		String str = "A\\whole\\\\lot\\\\\\of//backslashes";
		
		int expected = 6;
		int actual = StringHelper.countOccurrencesOf(str, '\\');
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testIndexOfWhitespace() {
		String str = "There.is-only\rone_whitespace";
		
		int expected = str.indexOf('\r');
		int actual = StringHelper.indexOfWhitespace(str);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testIndexOfWhitespaceFromIndex() {
		String str = "There are_two whitespaces";
		
		int expected = str.indexOf(' ', 6);
		int actual = StringHelper.indexOfWhitespace(str, 6);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testIndexOfWhitespaceNotFound() {
		String str = "There!are?no.whitespaces";
		
		int expected = str.indexOf(' ');
		int actual = StringHelper.indexOfWhitespace(str);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testIndexOfNonIdentifier1() {
		String str = "variableName{something}";
		
		int expected = str.indexOf("{");
		int actual = StringHelper.indexOfNonIdentifier(str, 0);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testIndexOfNonIdentifier2() {
		String str = "This is normal text. This is a $variable1.";
		int fromIndex = str.indexOf('$')+1;
		
		int expected = str.indexOf('.', fromIndex);
		int actual = StringHelper.indexOfNonIdentifier(str, fromIndex);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testIndexOfNonIdentifierNotFound() {
		String str = "$variable";
		
		int expected = -1;
		int actual = StringHelper.indexOfNonIdentifier(str, 1);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testCharAtEquals() {
		String str = "String";
		assertTrue(StringHelper.charAtEquals(str, 0, 'S'));
		assertTrue(StringHelper.charAtEquals(str, 1, 't'));
		assertTrue(StringHelper.charAtEquals(str, 2, 'r'));
		assertTrue(StringHelper.charAtEquals(str, 3, 'i'));
		assertTrue(StringHelper.charAtEquals(str, 4, 'n'));
		assertTrue(StringHelper.charAtEquals(str, 5, 'g'));
	}
	
	@Test
	public void testCharAtEqualsIndexTooBig() {
		String str = "String";
		// must not throw
		assertFalse(StringHelper.charAtEquals(str, 9, '?'));
	}
	
	@Test
	public void testCharsAtEquals1() {
		String str = "Mississippi";
		assertTrue(StringHelper.charsAtEquals(str, 0, 0, 'M'));
		assertTrue(StringHelper.charsAtEquals(str, 2, 3, 's'));
		assertTrue(StringHelper.charsAtEquals(str, 5, 6, 's'));
		assertTrue(StringHelper.charsAtEquals(str, 8, 9, 'p'));
	}
	
	@Test
	public void testCharsAtEquals2() {
		String str = "aaaa";
		assertTrue(StringHelper.charsAtEquals(str, 0, 0, 'a'));
		assertTrue(StringHelper.charsAtEquals(str, 0, 1, 'a'));
		assertTrue(StringHelper.charsAtEquals(str, 0, 2, 'a'));
		assertTrue(StringHelper.charsAtEquals(str, 0, 3, 'a'));
	}
	
	@Test
	public void testCharsAtIndexTooBig() {
		String str = "aaaa";
		assertFalse(StringHelper.charsAtEquals(str, 0, 9, 'a'));
		assertFalse(StringHelper.charsAtEquals(str, 3, 4, 'a'));
		assertFalse(StringHelper.charsAtEquals(str, 4, 5, 'a'));
	}
	
	@Test
	public void testIsEscaped1() {
		String str = "\\$variable"; // $ is escaped
		assertTrue(StringHelper.isEscaped(str, 1));
	}
	
	@Test
	public void testIsEscaped2() {
		String str = "\\\\"; // second \ is escaped
		assertTrue(StringHelper.isEscaped(str, 1));
		assertFalse(StringHelper.isEscaped(str, 0));
	}
	
	@Test
	public void testIsEscaped3() {
		String str = "Escape at the end.\\"; // last character \ is unescaped
		assertFalse(StringHelper.isEscaped(str, 18));
	}
	
	@Test
	public void testIsValidIdentifierName() {
		String name1 = "identifier1";
		String name2 = "_privateMember";
		String name3 = "variable123__";
		String name4 = "__";
		assertTrue(StringHelper.isValidIdentifierName(name1));
		assertTrue(StringHelper.isValidIdentifierName(name2));
		assertTrue(StringHelper.isValidIdentifierName(name3));
		assertTrue(StringHelper.isValidIdentifierName(name4));
	}
	
	@Test
	public void testIsInvalidIdentifierName4() {
		String name1 = "1startingWithDigit";
		String name2 = "having#hashtag";
		String name3 = "var!";
		String name4 = "var?";
		String name5 = "var/";
		String name6 = "var\\";
		String name7 = "var(";
		String name8 = "var*";
		String name9 = "";
		assertFalse(StringHelper.isValidIdentifierName(name1));
		assertFalse(StringHelper.isValidIdentifierName(name2));
		assertFalse(StringHelper.isValidIdentifierName(name3));
		assertFalse(StringHelper.isValidIdentifierName(name4));
		assertFalse(StringHelper.isValidIdentifierName(name5));
		assertFalse(StringHelper.isValidIdentifierName(name6));
		assertFalse(StringHelper.isValidIdentifierName(name7));
		assertFalse(StringHelper.isValidIdentifierName(name8));
		assertFalse(StringHelper.isValidIdentifierName(name9));
	}
	
	@Test
	public void testQuote() {
		final String Q = "\"";
		final String Qs = "\" \"";
		
		String str = "String";
		Integer i  = 1;
		Long l     = 2L;
		Float f    = 3.0f;
		Double d   = 4.0;
		Byte b     = (byte) 5;
		Short s    = (short) 6;
		
		String actual = StringHelper.quote(str, i, l, f, d, b, s);
		String expected = Q+str+Qs+i+Qs+l+Qs+f+Qs+d+Qs+b+Qs+s+Q;
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testQuoteContainingNull() {
		String actual = StringHelper.quote("is", null);
		String expected = "\"is\" \"null\"";
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testQuoteEmpty() {
		String actual = StringHelper.quote();
		String expected = "";
		
		assertEquals(expected, actual);
	}

}
