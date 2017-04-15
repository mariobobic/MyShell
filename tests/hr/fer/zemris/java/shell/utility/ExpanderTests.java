package hr.fer.zemris.java.shell.utility;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import hr.fer.zemris.java.shell.MyShell.EnvironmentImpl;

/**
 * Tests the functionality of {@link Expander} utility class.
 *
 * @author Mario Bobic
 */
@SuppressWarnings("javadoc")
public class ExpanderTests {
	
	/** Environment used by some tests. */
	private EnvironmentImpl environment = new EnvironmentImpl();

	
	/* ---------------------------- Brace expansion ----------------------------- */
	
	@Test
	public void testBraceExpansion1() {
		String input = "{1..5}";
		
		List<String> expected = Arrays.asList("1", "2", "3", "4", "5");
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansion2() {
		String input = "{old,new,dist,bugs}";
		
		List<String> expected = Arrays.asList("old", "new", "dist", "bugs");
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansion3() {
		String input = "{A..E}";
		
		List<String> expected = Arrays.asList("A", "B", "C", "D", "E");
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansion4() {
		String input = "{1..3,A..C,d..f}";
		
		List<String> expected = Arrays.asList("1", "2", "3", "A", "B", "C", "d", "e", "f");
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansion5() {
		String input = "file{1..2}.txt";
		
		List<String> expected = Arrays.asList("file1.txt", "file2.txt");
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansion6() {
		String input = "folder{1,A,Two}/file{1..2}.txt";
		
		// order is optional
		List<String> expected = Arrays.asList(
				"folder1/file1.txt", "folder1/file2.txt",
				"folderA/file1.txt", "folderA/file2.txt",
				"folderTwo/file1.txt", "folderTwo/file2.txt"
		);
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansion7() {
		String input = "folder{1..2}/file{1..2}.txt";
		
		// order is optional
		List<String> expected = Arrays.asList(
				"folder1/file1.txt", "folder1/file2.txt",
				"folder2/file1.txt", "folder2/file2.txt"
		);
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansion8() {
		String input = "{1,2,3}{A,B}";
		
		// order is optional
		List<String> expected = Arrays.asList(
				"1A", "1B",
				"2A", "2B",
				"3A", "3B"
		);
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansion9() {
		String input = "{10..12}{a..c}";
		
		// order is optional
		List<String> expected = Arrays.asList(
				"10a", "10b", "10c",
				"11a", "11b", "11c",
				"12a", "12b", "12c"
		);
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansion10() {
		String input = "{5..1}{ab,yz}";
		
		// order is optional
		List<String> expected = Arrays.asList(
				"5ab", "5yz",
				"4ab", "4yz",
				"3ab", "3yz",
				"2ab", "2yz",
				"1ab", "1yz"
		);
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansion11() {
		String input = "{e..a}";
		
		// order is optional
		List<String> expected = Arrays.asList("e", "d", "c", "b", "a");
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansion12() {
		String input = "{arg,arg,arg,arg}";
		
		List<String> expected = Arrays.asList("arg", "arg", "arg", "arg");
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansionWrongInput1() {
		String input = "{1,A..Two}";
		
		List<String> expected = Arrays.asList(input);
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansionWrongInput2() {
		String input = "text{1..2";
		
		List<String> expected = Arrays.asList(input);
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansionWrongInput3() {
		String input = "folder{1..2}/file{1..}.txt";
		
		List<String> expected = Arrays.asList("folder1/file{1..}.txt", "folder2/file{1..}.txt");
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansionEscaped1() {
		// plain:       file\{1..2}.txt
		String input = "file\\{1..2}.txt";
		
		List<String> expected = Arrays.asList("file{1..2}.txt");
		List<String> actual = Expander.expand(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansionEscaped2() {
		// plain:       folder{1..2}/file\{1..2}.txt
		String input = "folder{1..2}/file\\{1..2}.txt";
		
		List<String> expected = Arrays.asList("folder1/file{1..2}.txt", "folder2/file{1..2}.txt");
		List<String> actual = Expander.expand(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansionEscaped3() {
		// plain:       folder\{1..2}/file{1..2}.txt
		String input = "folder\\{1..2}/file{1..2}.txt";
		
		List<String> expected = Arrays.asList("folder{1..2}/file1.txt", "folder{1..2}/file2.txt");
		List<String> actual = Expander.expand(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansionEscaped4() {
		// plain:       folder\{1,2}/folder{1,2}/file\{1,2}.txt
		String input = "folder\\{1,2}/folder{1,2}/file\\{1,2}.txt";
		
		List<String> expected = Arrays.asList("folder{1,2}/folder1/file{1,2}.txt", "folder{1,2}/folder2/file{1,2}.txt");
		List<String> actual = Expander.expand(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansionEscaped5() {
		// plain:       file\\\{1..2}.txt
		String input = "file\\\\\\{1..2}.txt";

		// plain:                              file\{1..2}.txt
		List<String> expected = Arrays.asList("file\\{1..2}.txt");
		List<String> actual = Expander.expand(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansionUnescaped1() {
		// plain:       \\{arg1,arg2}
		String input = "\\\\{arg1,arg2}";

		// plain:                               \arg1     \arg2
		List<String> expected = Arrays.asList("\\arg1", "\\arg2");
		List<String> actual = Expander.expand(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	public void testBraceExpansionUnescaped2() {
		// plain:       \\{}
		String input = "\\\\{}";

		// plain:                               \arg1
		List<String> expected = Arrays.asList("\\{}");
		List<String> actual = Expander.expand(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	@Test
	@Ignore // TODO support nested expressions
	public void testBraceExpansionNested() {
		String input = "folder{1{old,new},2{stale,fresh}}";

		List<String> expected = Arrays.asList("folder1old", "folder1new", "folder2stale", "folder2fresh");
		List<String> actual = Expander.braceExpansion(environment, input);
		
		assertEqualsList(expected, actual);
	}
	
	

	/* ---------------------- Special character expansion ----------------------- */
	
	@Test
	public void testSpecialCharacterExpansion1() {
		environment.addToHistory("historical string");
		String input = "!!";

		String expected = "historical string";
		String actual = Expander.specialCharacterExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testSpecialCharacterExpansion2() {
		environment.addToHistory("sparta");
		String input = "This is !!!";

		String expected = "This is sparta!";
		String actual = Expander.specialCharacterExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testSpecialCharacterExpansion3() {
		environment.addToHistory("of");
		String input = "Middle !! text";

		String expected = "Middle of text";
		String actual = Expander.specialCharacterExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testSpecialCharacterExpansion4() {
		environment.addToHistory("Beginning");
		String input = "!! of text";

		String expected = "Beginning of text";
		String actual = Expander.specialCharacterExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testSpecialCharacterExpansionEscaped1() {
		environment.addToHistory("history");
		String input = "This is escaped\\!!";

		String expected = "This is escaped!!";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testSpecialCharacterExpansionEscaped2() {
		environment.addToHistory("history");
		String input = "\\!!\\!!";

		String expected = "!!!!";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testSpecialCharacterExpansionEscaped3() {
		environment.addToHistory("history");
		String input = "\\!!!";

		String expected = "\\!history"; // bash related behavior
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testSpecialCharacterExpansionUnescaped() {
		environment.addToHistory("history");
		String input = "A \\\\!! class.";

		String expected = "A \\history class.";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	
	
	
	/* --------------------------- Variable expansion --------------------------- */
	
	@Test
	public void testVariableExpansion1() {
		environment.setVariable("var", "value");
		String input = "${var}";
		
		String expected = "value";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansion2() {
		environment.setVariable("var", "value");
		String input = "$var";
		
		String expected = "value";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansion3() {
		environment.setVariable("var1", "value1");
		environment.setVariable("var2", "value2");
		String input = "$var1 $var2";
		
		String expected = "value1 value2";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansion4() {
		environment.setVariable("a", "5");
		environment.setVariable("b", "3");
		environment.setVariable("c", "1");
		environment.setVariable("d", "0");
		String input = "$a + ${b} * ${c}${d} = 35";
		
		String expected = "5 + 3 * 10 = 35";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansion5() {
		environment.setVariable("a", "5");
		environment.setVariable("b", "3");
		String input = "${a}${b}";
		
		String expected = "53";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansion6() {
		environment.setVariable("var1", "value1");
		environment.setVariable("var2", "value2");
		environment.setVariable("var3", "value3");
		String input = ".$var1.$var2$var3";
		
		String expected = ".value1.value2value3";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansion7() {
		environment.setVariable("var1", "value1");
		environment.setVariable("var2", "value2");
		environment.setVariable("var3", "value3");
		String input = "$var1$var2${var3}";
		
		String expected = "value1value2value3";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansion8() {
		environment.setVariable("var1", "1");
		environment.setVariable("var2", "2");
		environment.setVariable("var21", "error");
		String input = "$var1$var2$var1";
		
		String expected = "121";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionWrongInput1() {
		String input = "$";
		
		String expected = input;
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionWrongInput2() {
		String input = "${var";
		
		String expected = input;
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionEscaped1() {
		environment.setVariable("var1", "value1");
		String input = "\\$var1";
		
		String expected = "$var1";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionEscaped2() {
		environment.setVariable("var1", "value1");
		environment.setVariable("var2", "value2");
		String input = "\\$var1$var2";
		
		String expected = "$var1value2";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionEscaped3() {
		environment.setVariable("var1", "value1");
		String input = "\\${var1}";
		
		String expected = "${var1}";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionEscaped4() {
		environment.setVariable("var1", "value1");
		String input = "\\$\\{var1}";
		
		String expected = "${var1}";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionUnescaped1() {
		// plain:       \\$var
		String input = "\\\\$var";

		// plain:          \$var
		String expected = "\\$var";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionUnescaped2() {
		environment.setVariable("var", "value");
		String input = "\\\\$var";
		
		String expected = "\\value";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultTemp1() {
		String input = "${var:-default}";
		
		String expected = "default";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultTemp2() {
		environment.setVariable("var", "value");
		String input = "${var:-default}";
		
		String expected = "value";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultTemp3() {
		String input = "${var:-default}!";
		Expander.variableExpansion(environment, input);
		assertNull(environment.getVariable("var"));

		// var should not have been stored
		input = "${var}!";
		
		String expected = "${var}!";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultTemp4() {
		String input = "1. ${var:-default}, 2. $var";

		String expected = "1. default, 2. $var";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultTempInvalidName() {
		String input = "${123:-number}";

		String expected = "${123:-number}";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultTempEmpty() {
		String input = "${var:-}";

		String expected = "";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultTempEscaped() {
		String input = "\\${var:-default}";

		String expected = "${var:-default}";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultPerm1() {
		String input = "${var:=default}";
		
		String expected = "default";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultPerm2() {
		environment.setVariable("var", "value");
		String input = "${var:=default}";
		
		String expected = "value";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultPerm3() {
		String input = "${var:=default}!";
		Expander.variableExpansion(environment, input);
		assertEquals("default", environment.getVariable("var"));

		// var should have been stored
		input = "${var}!";
		
		String expected = "default!";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultPerm4() {
		String input = "1. ${var:=default}, 2. $var";

		String expected = "1. default, 2. default";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultPermInvalidName() {
		String input = "${123:=number}";

		String expected = "${123:=number}";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultPermEmpty() {
		String input = "${var:=}";

		String expected = "";
		String actual = Expander.variableExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionDefaultPermEscaped() {
		String input = "\\${var:=default}";

		String expected = "${var:=default}";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVariableExpansionReservedVariableName() {
		environment.setVariable("reserved?!#$", "success");
		
		String input = "${reserved?!#$}";

		String expected = "success";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	
	
	/* -------------------------- Arithmetic expansion -------------------------- */
	
	@Test
	public void testArithmeticExpansion1() {
		String input = "$((2+2))";
		
		String expected = "4";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansion2() {
		String input = "$((2 * 2))";
		
		String expected = "4";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansion3() {
		String input = "$((2 + 2*2/2))"; // 2 + 4/2 = 2 + 2
		
		String expected = "4";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansion4() {
		String input = "$((2^2))";
		
		String expected = "4";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansion5() {
		String input = "$((1 + 2^2^2 - 1))"; // 1 + 2^4 - 1 = 2^4
		
		String expected = "16";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansion6() {
		String input = "$((0))";
		
		String expected = "0";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansion7() {
		String input = "$((17 % 7))";
		
		String expected = "3";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionDouble1() {
		String input = "$((1 + 2*3 - 4/5))"; // 1 + 6 - 0.8
		
		String expected = "6.2";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionDouble2() {
		String input = "$((0.1 - 0.2 + 0.3 - 0.4 + 0.5))";
		
		String expected = "0.3";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionDouble3() {
		String input = "$((1/5 + 2/5 + 1/10 + 3/10))"; // 0.2 + 0.4 + 0.1 + 0.3
		
		String expected = "1";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionDouble4() {
		String input = "$((3.0^3 % $((2^2.0))))"; // 27 % 4
		
		String expected = "3";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionDouble5() {
		String input = "$((2^31)) $((2^32))";
		
		String expected = "2147483648 4294967296";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionMultiple1() {
		String input = "$((1-1))$((100^0))$((-2+4))$((3^1))$((1+1+2))";
		
		String expected = "01234";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionMultiple2() {
		String input = "$((1-1)) $((1-1))";
		
		String expected = "0 0";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionNested1() {
		String input = "$((1 - 0*$((1-2))))"; // 1 - 0*(1-2) = 1 - 0
		
		String expected = "1";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionNested2() {
		// 1 * (1-2) - (-2*(1+1)) + 1 = 1 * (-1) - (-4) + 1 = -1 + 4 + 1
		String input = "$((1 * $((1-2)) - $((-2*$((1+1)))) + 1))";
		
		String expected = "4";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionMultipleNested() {
		// 1 * (1-2) + 1, 2 * (1+1) = 1*(-1)+1, 2*2 = 0, 4
		String input = "$((1 * $((1-2)) + 1)), $((2 * $((1+1))))";
		
		String expected = "0, 4";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionVariables1() {
		environment.setVariable("a", "4");
		environment.setVariable("b", "3");
		environment.setVariable("c", "7");
		// (a + 4)*b % c = (4 + 4)*3 % 7 = 24 % 7
		String input = "$(($(($a + 4))*$b % $c))";
		
		String expected = "3";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionWrongInput1() {
		String input = "$((";
		
		String expected = "$((";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionWrongInput2() {
		String input = "$((1+1";
		
		String expected = "$((1+1";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionWrongInput3() {
		String input = "$((1+))";
		
		String expected = "$((1+))";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionWrongInput4() {
		String input = "$((1+unparsable))";
		
		String expected = "$((1+unparsable))";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionWrongInput5() {
		String input = "$(())";
		
		String expected = "$(())";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionWrongInput6() {
		String input = "$(()), $((2+2)), $(())";
		
		String expected = "$(()), 4, $(())";
		String actual = Expander.arithmeticExpansion(environment, input);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionEscaped() {
		String input = "\\$((1))";
		
		String expected = "$((1))";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testArithmeticExpansionUnescaped() {
		String input = "\\\\$((1+1))";
		
		String expected = "\\2";
		String actual = Helper.firstElement(Expander.expand(environment, input));
		
		assertEquals(expected, actual);
	}
	
	
	
	
	/* -------------------------------- Utility --------------------------------- */
	
	private static <T> void assertEqualsList(List<T> expected, List<T> actual) {
		assertEquals("Lists are not of same size.", expected.size(), actual.size());
//		assertEquals("Lists do not have same contents.", expected, actual);
		for (T item : expected) {
			assertTrue("List does not contain " + item, actual.contains(item));
		}
	}
	
}
