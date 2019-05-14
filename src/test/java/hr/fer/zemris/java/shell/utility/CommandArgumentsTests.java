package hr.fer.zemris.java.shell.utility;

import hr.fer.zemris.java.shell.utility.CommandArguments.Flag;
import hr.fer.zemris.java.shell.utility.exceptions.InvalidFlagException;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the functionality of {@link CommandArguments} utility class.
 *
 * @author Mario Bobic
 */
public class CommandArgumentsTests {

	/* ------------------------- CommandArguments tests ------------------------- */
	
	@Test
	public void testConstructorEmptyFlags() {
		// must not throw
		new CommandArguments("");
	}
	
	@Test(expected = NullPointerException.class)
	public void testConstructorNullFlags() {
		new CommandArguments(null);
	}
	
	@Test(expected = NullPointerException.class)
	public void testConstructorNullHasArguments() {
		new CommandArguments("flags", null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorUnmatchedNumberOfArguments() {
		new CommandArguments("abc", true, false);
	}
	
	@Test
	public void testAddFlagDefinition1() {
		CommandArguments cmdArgs = new CommandArguments();
		cmdArgs.addFlagDefinition("r", false);
		
		cmdArgs.compile("command -r other-arguments");
		assertEquals(1, cmdArgs.getFlags().size());
	}
	
	@Test
	public void testAddFlagDefinition2() {
		CommandArguments cmdArgs = new CommandArguments();
		cmdArgs.addFlagDefinition("e", "exclude", true);
		
		cmdArgs.compile("command --exclude flag-argument other-arguments");
		assertEquals(1, cmdArgs.getFlags().size());
	}
	
	@Test
	public void testStaticCompile1() {
		CommandArguments cmdArgs = CommandArguments.compile("ls -h", "h");
		assertEquals(1, cmdArgs.getFlags().size());
	}
	
	@Test
	public void testStaticCompile2() {
		CommandArguments cmdArgs =
			CommandArguments.compile("cmd -e flag-arg", "e", true);
		assertEquals(1, cmdArgs.getFlags().size());
	}
	
	@Test
	public void testCompile1() {
		CommandArguments rmArgs = new CommandArguments();
		rmArgs.addFlagDefinition("r", false);
		rmArgs.addFlagDefinition("f", false);
		rmArgs.addFlagDefinition("no-preserve-root", false);
		
		rmArgs.compile("rm -rf --no-preserve-root C:/");
		
		assertEquals(3, rmArgs.getFlags().size());
		assertEquals("rm C:/", rmArgs.getCleanArgument());
	}
	
	@Test
	public void testCompile2() {
		CommandArguments bsArgs = new CommandArguments();
		bsArgs.addFlagDefinition("o", true);
		bsArgs.addFlagDefinition("l", true);
		
		bsArgs.compile("byteshuffle -o 1024 -l 2048 \"/home/space name.ext\"");

		assertEquals(2, bsArgs.getFlags().size());
		assertEquals("byteshuffle \"/home/space name.ext\"", bsArgs.getCleanArgument());
	}
	
	@Test
	public void testCompileEscapedDash() {
		CommandArguments cmdArgs = new CommandArguments();
		cmdArgs.addFlagDefinition("flag", true);
		
		// escape symbol is not removed
		cmdArgs.compile("cmd --flag arg \\-not-flag path");

		assertEquals(1, cmdArgs.getFlags().size());
		assertEquals("cmd \\-not-flag path", cmdArgs.getCleanArgument());
	}
	
	@Test
	public void testCompileMultipleFlags() {
		CommandArguments findArgs = new CommandArguments();
		findArgs.addFlagDefinition("e", true);
		
		findArgs.compile("find -e path1 -e path2 path");

		List<String> expected = Arrays.asList("path1", "path2");
		List<String> actual = findArgs.getFlag("e").getArguments();
		
		assertEquals(expected, actual);
		assertEquals("find path", findArgs.getCleanArgument());
	}
	
	@Test(expected = InvalidFlagException.class)
	public void testCompileException1() {
		CommandArguments cmdArgs = new CommandArguments();
		cmdArgs.addFlagDefinition("o", true);
		cmdArgs.addFlagDefinition("l", true);
		
		// must throw
		cmdArgs.compile("cmd -o 1024 --undefined -wtf C:/");
	}

	@Test(expected = InvalidFlagException.class)
	public void testCompileException2() {
		CommandArguments cmdArgs = new CommandArguments();
		cmdArgs.addFlagDefinition("o", true);
		cmdArgs.addFlagDefinition("l", true);
		
		// must throw
		cmdArgs.compile("cmd -ol 1024 2048 C:/file.ext");
	}
	
	@Test(expected = InvalidFlagException.class)
	public void testCompileException3() {
		CommandArguments cmdArgs = new CommandArguments();
		cmdArgs.addFlagDefinition("l", true);
		
		// must throw
		cmdArgs.compile("cmd C:/file.ext -l");
	}
	
	@Test
	public void testContainsFlag() {
		CommandArguments cmdArgs = new CommandArguments();
		cmdArgs.addFlagDefinition("f", "force", false);
		
		cmdArgs.compile("cmd -f /");

		assertTrue(cmdArgs.containsFlag("f"));
		assertTrue(cmdArgs.containsFlag("f", "force"));
		assertFalse(cmdArgs.containsFlag("r"));
	}
	
	@Test
	public void testGetFlag() {
		CommandArguments cmdArgs = new CommandArguments();
		cmdArgs.addFlagDefinition("e", "exclude", true);
		
		cmdArgs.compile("cmd -e /usr --exclude /etc /");
		
		Flag eFlag = cmdArgs.getFlag("e");
		assertEquals("/usr", eFlag.getArgument());
		
		Flag excludeFlag = cmdArgs.getFlag("exclude");
		assertEquals("/etc", excludeFlag.getArgument());
	}
	
	@Test
	public void testGetMultipleFlags() {
		CommandArguments cmdArgs = new CommandArguments();
		cmdArgs.addFlagDefinition("e", "exclude", true);
		
		cmdArgs.compile("cmd -e /usr --exclude /etc /");
		
		Flag bothFlags = cmdArgs.getFlag("e", "exclude");
		assertTrue(bothFlags.getArguments().contains("/usr"));
		assertTrue(bothFlags.getArguments().contains("/etc"));
	}
	
	@Test(expected = IllegalStateException.class)
	public void testGetCleanArgumentException() {
		new CommandArguments().getCleanArgument();
	}
	
	@Test
	public void testClearFlags() {
		CommandArguments cmdArgs = CommandArguments.compile("cmd -f argument", "f");
		cmdArgs.clearFlags();
		
		assertTrue(cmdArgs.getFlags().isEmpty());
	}
	

	/* ------------------------------ Flag tests -------------------------------- */
	
	@Test
	public void testFlagConstructor1() {
		Flag flag = new Flag("name");
		assertFalse(flag.hasArgument());
		assertNull(flag.getArgument());
	}
	
	@Test
	public void testFlagConstructor2() {
		Flag flag = new Flag("name", "argument");
		assertTrue(flag.hasArgument());
		assertEquals("argument", flag.getArgument());
	}
	
	
	@Test
	public void testGetArguments() {
		Flag flag = new Flag("name", "argument");
		assertTrue(flag.getArguments().contains("argument"));
	}
	
	@Test
	public void testAddArgument1() {
		Flag flag = new Flag("name");
		flag.addArgument("arg1");
		flag.addArgument("arg2");

		assertTrue(flag.getArguments().contains("arg1"));
		assertTrue(flag.getArguments().contains("arg2"));
	}
	
	@Test
	public void testAddArgument2() {
		Flag flag = new Flag("name", "arg0");
		flag.addArgument("arg1");
		flag.addArgument("arg2");

		assertTrue(flag.getArguments().contains("arg0"));
		assertTrue(flag.getArguments().contains("arg1"));
		assertTrue(flag.getArguments().contains("arg2"));
	}
	
	@Test
	public void testAddArguments() {
		Flag flag = new Flag("name");
		
		List<String> args = Arrays.asList("arg1", "arg2");
		flag.addArguments(args);

		assertTrue(flag.getArguments().contains("arg1"));
		assertTrue(flag.getArguments().contains("arg2"));
	}
	
	
	@Test
	public void testGetIntArgument() {
		Flag flag = new Flag("negative-one", "-1");
		assertEquals(-1, flag.getIntArgument());
	}
	
	@Test
	public void testGetLongArgument() {
		Flag flag = new Flag("speed-limit", "299792458");
		assertEquals(299792458L, flag.getLongArgument());
	}
	
	@Test
	public void testGetDoubleArgument() {
		Flag flag = new Flag("pi", "3.141592653589793");
		assertEquals(Math.PI, flag.getDoubleArgument(), 1E-6);
	}
	
	@Test
	public void testGetSizeArgument() {
		Flag flag = new Flag("fat16-limit", "4 GiB");
		assertEquals(4L*1024L*1024L*1024L, flag.getSizeArgument());
	}
	
	
	@Test(expected = InvalidFlagException.class)
	public void testGetIntArgumentException() {
		new Flag("second", "9192631770").getIntArgument();
	}
	
	@Test(expected = InvalidFlagException.class)
	public void testGetLongArgumentException() {
		new Flag("name", "random string").getLongArgument();
	}

	@Test(expected = InvalidFlagException.class)
	public void testGetDoubleArgumentException() {
		new Flag("name", "random string").getDoubleArgument();
	}

	@Test(expected = InvalidFlagException.class)
	public void testGetSizeArgumentException() {
		new Flag("name", "random string").getSizeArgument();
	}
	
	
	@Test
	public void testGetPositiveIntArgument1() {
		Flag flag = new Flag("positive-one", "1");
		assertEquals(1, flag.getPositiveIntArgument(false));
	}
	
	@Test
	public void testGetPositiveIntArgument2() {
		Flag flag = new Flag("zero", "0");
		assertEquals(0, flag.getPositiveIntArgument(true));
	}
	
	@Test(expected = InvalidFlagException.class)
	public void testGetPositiveIntArgumentException1() {
		Flag flag = new Flag("negative-one", "-1");
		flag.getPositiveIntArgument(false);
	}
	
	@Test(expected = InvalidFlagException.class)
	public void testGetPositiveIntArgumentException2() {
		Flag flag = new Flag("zero", "0");
		flag.getPositiveIntArgument(false);
	}
	
	@Test
	public void testGetPositiveLongArgument1() {
		Flag flag = new Flag("second", "9192631770");
		assertEquals(9192631770L, flag.getPositiveLongArgument(false));
	}

	@Test
	public void testGetPositiveLongArgument2() {
		Flag flag = new Flag("zero", "0");
		assertEquals(0L, flag.getPositiveLongArgument(true));
	}

	@Test(expected = InvalidFlagException.class)
	public void testGetPositiveLongArgumentException1() {
		Flag flag = new Flag("negative-one", "-1");
		flag.getPositiveLongArgument(false);
	}

	@Test(expected = InvalidFlagException.class)
	public void testGetPositiveLongArgumentException2() {
		Flag flag = new Flag("zero", "0");
		flag.getPositiveLongArgument(false);
	}
	
	/* -------------------------------- Utility --------------------------------- */

	
}
