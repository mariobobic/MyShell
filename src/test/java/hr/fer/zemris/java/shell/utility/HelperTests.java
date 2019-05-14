package hr.fer.zemris.java.shell.utility;

import hr.fer.zemris.java.shell.MyShell.EnvironmentImpl;
import hr.fer.zemris.java.shell.utility.exceptions.IllegalPathException;
import hr.fer.zemris.java.shell.utility.exceptions.NotEnoughDiskSpaceException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the functionality of {@link Utility} utility class.
 *
 * @author Mario Bobic
 */
public class HelperTests {
	
	/** Environment used by some tests. */
	private final EnvironmentImpl environment = new EnvironmentImpl();


	/* ------------------------------ Helper tests ------------------------------ */
	
	@Test
	public void testResolveAbsolutePath() throws IOException {
		String str = ".";
		
		Path actual = Utility.resolveAbsolutePath(environment, str);
		Path expected = Paths.get(str);
		
		assertTrue(Files.isSameFile(expected, actual));
	}

	@Test
	public void testResolveAbsolutePathUserHome() throws IOException {
		Path home = Paths.get(System.getProperty("user.home"));
		
		Path expected = home.resolve("Downloads");
		Path actual = Utility.resolveAbsolutePath(environment, "~/Downloads");
		
		assertTrue(Files.isSameFile(expected, actual));
	}
	
	@Test
	public void testResolveAbsolutePathMarkedInteger() throws IOException {
		Path home = Paths.get(System.getProperty("user.home"));
		int id = environment.mark(home);
		
		Path actual = Utility.resolveAbsolutePath(environment, Integer.toString(id));
		Path expected = home;
		
		assertTrue(Files.isSameFile(expected, actual));
	}
	
	@Test
	public void testGetFileName1() {
		Path file = Paths.get("./file.txt").toAbsolutePath();
		
		Path expectedFile = file.getFileName();
		Path actualFile = Utility.getFileName(file);
		assertEquals(expectedFile, actualFile);
	}
	
	@Test
	public void testGetFileName2() {
		Path root = Paths.get("./file.txt").toAbsolutePath().getRoot();
		
		Path expectedRoot = root;
		Path actualRoot = Utility.getFileName(root);
		assertEquals(expectedRoot, actualRoot);
	}
	
	@Test
	public void testGetParent1() {
		Path file = Paths.get("./file.txt").toAbsolutePath();
		
		Path expectedParent = file.getParent();
		Path actualParent = Utility.getParent(file);
		assertEquals(expectedParent, actualParent);
	}
	
	@Test
	public void testGetParent2() {
		Path root = Paths.get("./file.txt").toAbsolutePath().getRoot();
		
		Path expectedRoot = root;
		Path actualRoot = Utility.getParent(root);
		assertEquals(expectedRoot, actualRoot);
	}
	
	@Test
	public void testIsHiddenFile() throws IOException {
		// Create it and set to hidden
		Path hiddenFile = Files.createTempFile(null, null);
		Files.setAttribute(hiddenFile, "dos:hidden", Boolean.TRUE);
		
		assertTrue(Utility.isHidden(hiddenFile));
		
		// Delete it afterwards
		Files.delete(hiddenFile);
	}
	
	@Test
	public void testIsHiddenDirectory() throws IOException {
		// Create it and set to hidden
		Path hiddenDirectory = Files.createTempDirectory(null);
		Files.setAttribute(hiddenDirectory, "dos:hidden", Boolean.TRUE);
		
		assertTrue(Utility.isHidden(hiddenDirectory));
		
		// Delete it afterwards
		Files.delete(hiddenDirectory);
	}
	
	@Test
	public void testIsNotHidden() throws IOException {
		// Create not hidden file and directory
		Path file = Files.createTempFile(null, null);
		Path dir = Files.createTempDirectory(null);

		assertFalse(Utility.isHidden(file));
		assertFalse(Utility.isHidden(dir));
		
		// Delete them afterwards
		Files.delete(file);
		Files.delete(dir);
	}
	
	@Test
	public void testIsHiddenRoot() {
		// roots must not be hidden
		FileSystems.getDefault().getRootDirectories().forEach(root ->
			assertFalse(Utility.isHidden(root))
		);
	}
	
	@Test
	public void testFirstAvailable1() throws IOException {
		// Create a file
		Path file = Files.createTempFile(null, ".txt");
		
		// Get the first available file (should not be 'file')
		Path firstAvailable = Utility.firstAvailable(file).toAbsolutePath();
		assertNotEquals(file, firstAvailable);
		assertTrue(firstAvailable.toString().endsWith(".txt"));
		
		// Delete the file afterwards
		Files.delete(file);
	}
	
	@Test
	public void testFirstAvailable2() throws IOException {
		// Create a file
		Path file = Files.createTempFile(null, "");
		
		// Get the first available file (should not be 'file')
		Path firstAvailable = Utility.firstAvailable(file).toAbsolutePath();
		assertNotEquals(file, firstAvailable);
		
		// Delete the file afterwards
		Files.delete(file);
	}
	
	@Test
	public void testFirstAvailable3() {
		Path file = Paths.get("./file.txt").toAbsolutePath();
		
		// Get the first available file (should be exactly 'file')
		Path firstAvailable = Utility.firstAvailable(file).toAbsolutePath();
		assertEquals(file, firstAvailable);
	}
	
	@Test
	public void testExtension1() {
		Path file = Paths.get("./file.txt");
		assertEquals(".txt", Utility.extension(file));
	}
	
	@Test
	public void testExtension2() {
		Path file = Paths.get("./file.");
		assertEquals(".", Utility.extension(file));
	}
	
	@Test
	public void testExtension3() {
		Path file = Paths.get("./file");
		assertEquals("", Utility.extension(file));
	}
	
	@Test
	public void testRequireExists1() throws IOException {
		// Create a file
		Path file = Files.createTempFile(null, null);
		Utility.requireExists(file);
		
		// Delete the file afterwards
		Files.delete(file);
	}
	
	@Test(expected = IllegalPathException.class)
	public void testRequireExists2() {
		Path file = Paths.get("./non-existent-file");
		Utility.requireExists(file);
	}
	
	@Test
	public void testRequireDirectory1() throws IOException {
		// Create a directory
		Path dir = Files.createTempDirectory(null);
		Utility.requireDirectory(dir);
		
		// Delete the directory afterwards
		Files.delete(dir);
	}
	
	@Test(expected = IllegalPathException.class)
	public void testRequireDirectory2() throws IOException {
		// Create a file
		Path file = Files.createTempFile(null, null);
		Utility.requireDirectory(file);
	}
	
	@Test
	public void testRequireFile1() throws IOException {
		// Create a file
		Path file = Files.createTempFile(null, null);
		Utility.requireFile(file);
		
		// Delete the file afterwards
		Files.delete(file);
	}
	
	@Test(expected = IllegalPathException.class)
	public void testRequireFile2() throws IOException {
		// Create a directory
		Path dir = Files.createTempDirectory(null);
		Utility.requireFile(dir);
	}
	
	@Test
	public void testRequireDiskSpace1() throws IOException {
		Utility.requireDiskSpace(0, environment.getStartPath());
	}
	
	@Test(expected = NotEnoughDiskSpaceException.class)
	public void testRequireDiskSpace2() throws IOException {
		long fiveTiB = 5L*1000*1000*1000*1000; // five terabytes
		Utility.requireDiskSpace(fiveTiB, environment.getStartPath());
	}

	@Test
	public void testHumanReadableByteCount1() {
		final long kB = 1024L;
		
		assertEquals("1 B",     Utility.humanReadableByteCount(1));
		assertEquals("1.0 kiB", Utility.humanReadableByteCount(kB));
		assertEquals("1.0 MiB", Utility.humanReadableByteCount(kB*kB));
		assertEquals("1.0 GiB", Utility.humanReadableByteCount(kB*kB*kB));
		assertEquals("1.0 TiB", Utility.humanReadableByteCount(kB*kB*kB*kB));
		assertEquals("1.0 PiB", Utility.humanReadableByteCount(kB*kB*kB*kB*kB));
		assertEquals("1.0 EiB", Utility.humanReadableByteCount(kB*kB*kB*kB*kB*kB));
	}
	
	@Test
	public void testHumanReadableByteCount2() {
		final long kB = 1024L;

		assertEquals("1000 B",  Utility.humanReadableByteCount(1000));
		assertEquals("2.5 kiB", Utility.humanReadableByteCount(2*kB + kB/2));
		assertEquals("1.5 MiB", Utility.humanReadableByteCount(1024*kB + 512*kB));
	}
	
	@Test
	public void testParseSize1() {
		assertEquals(2048L,       Utility.parseSize("2 kiB"));
		assertEquals(10485760L,   Utility.parseSize("10 MiB"));
		assertEquals(1073741824L, Utility.parseSize("1 GiB"));
		assertEquals(16L,         Utility.parseSize("16 B"));
		assertEquals(4096L,       Utility.parseSize("4096"));
		assertEquals(2560L,       Utility.parseSize("2.5 kiB"));
		assertEquals(2560L,       Utility.parseSize("2,5 kiB"));
		assertEquals(1024L,       Utility.parseSize("1kiB"));
		assertEquals(1048576L,    Utility.parseSize("1MIB"));
	}
	
	@Test
	public void testParseSize2() {
		assertEquals(1000L,       Utility.parseSize("1 kB"));
		assertEquals(1000000L,    Utility.parseSize("1 MB"));
		assertEquals(1000000000L, Utility.parseSize("1 gb"));
		assertEquals(1000000000L, Utility.parseSize("1gB"));
		assertEquals(1234567890L, Utility.parseSize("1.23456789 GB"));
		assertEquals(1234567890L, Utility.parseSize("1,23456789 GB"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeEmpty() {
		Utility.parseSize("");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeNull() {
		Utility.parseSize(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeNegative() {
		Utility.parseSize("-2 kB");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeDoublePeriod() {
		Utility.parseSize("2..5 kB");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeWord() {
		Utility.parseSize("Foo");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeWrongUnit1() {
		Utility.parseSize("1.21 GW");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeWrongUnit2() {
		Utility.parseSize("14 jB");
	}
	
	@Test
	public void testHumanReadableTimeUnit1() {
		final long us = 1000L;
		
		assertEquals("1 ns",   Utility.humanReadableTimeUnit(1));
		assertEquals("1 us",   Utility.humanReadableTimeUnit(us));
		assertEquals("1 ms",   Utility.humanReadableTimeUnit(us*us));
		assertEquals("1 s",    Utility.humanReadableTimeUnit(us*us*us));
		assertEquals("1 min",  Utility.humanReadableTimeUnit(60*us*us*us));
		assertEquals("1 hr",   Utility.humanReadableTimeUnit(60*60*us*us*us));
		assertEquals("1 days", Utility.humanReadableTimeUnit(24*60*60*us*us*us));
	}
	
	@Test
	public void testHumanReadableTimeUnit2() {
		final long s = 1_000_000_000L;
		final long min = 60*s;
		final long hr = 60*min;
		final long day = 24*hr;
		
		assertEquals("123 ns",                   Utility.humanReadableTimeUnit(123));
		assertEquals("2 min 15 s",               Utility.humanReadableTimeUnit(2*min + 15*s));
		assertEquals("1 hr 15 min 20 s",         Utility.humanReadableTimeUnit(hr + 15*min + 20*s));
		assertEquals("2 days 23 hr 59 min 59 s", Utility.humanReadableTimeUnit(2*day + 23*hr + 59*min + 59*s));
		assertEquals("365 days",                 Utility.humanReadableTimeUnit(365*day));
	}
	
	@Test
	public void testGeneratePasswordHash() {
		String hash = Utility.generatePasswordHash("password");
		assertNotEquals("password", hash);
	}
	
	@Test
	public void testResolveCharset1() {
		Charset charset = Utility.resolveCharset("UTF-8");
		assertEquals(StandardCharsets.UTF_8, charset);
	}
	
	@Test
	public void testResolveCharset2() {
		Charset charset = Utility.resolveCharset("nothing");
		assertNull(charset);
	}
	
	@Test
	public void testIsInteger() {
		assertTrue(Utility.isInteger("0"));
		assertTrue(Utility.isInteger("-1"));
		assertTrue(Utility.isInteger(Integer.toString(Integer.MAX_VALUE)));
		assertTrue(Utility.isInteger(Integer.toString(Integer.MIN_VALUE)));

		assertFalse(Utility.isInteger(""));
		assertFalse(Utility.isInteger("abc"));
		assertFalse(Utility.isInteger("2.25"));
		assertFalse(Utility.isInteger("2,50"));
		assertFalse(Utility.isInteger(Long.toString(Long.MAX_VALUE)));
	}
	
	@Test
	public void testIsLong() {
		assertTrue(Utility.isLong("0"));
		assertTrue(Utility.isLong("-1"));
		assertTrue(Utility.isLong(Long.toString(Long.MAX_VALUE)));
		assertTrue(Utility.isLong(Long.toString(Long.MIN_VALUE)));

		assertFalse(Utility.isLong(""));
		assertFalse(Utility.isLong("abc"));
		assertFalse(Utility.isLong("2.25"));
		assertFalse(Utility.isLong("2,50"));
		assertFalse(Utility.isLong(Double.toString(Double.MAX_VALUE)));
	}
	
	@Test
	public void testIsDouble() {
		assertTrue(Utility.isDouble("0"));
		assertTrue(Utility.isDouble("-1"));
		assertTrue(Utility.isDouble(".5"));
		assertTrue(Utility.isDouble("2.25"));
		assertTrue(Utility.isDouble("1E10"));
		assertTrue(Utility.isDouble(Double.toString(Double.NaN)));
		assertTrue(Utility.isDouble(Double.toString(Double.MAX_VALUE)));
		assertTrue(Utility.isDouble(Double.toString(Double.MIN_VALUE)));
		assertTrue(Utility.isDouble(Double.toString(Double.POSITIVE_INFINITY)));

		assertFalse(Utility.isDouble(""));
		assertFalse(Utility.isDouble("."));
		assertFalse(Utility.isDouble("abc"));
		assertFalse(Utility.isDouble("1.2.3"));
	}
	
	@Test
	public void testFirstElement() {
		List<Integer> list = new ArrayList<>();
		assertNull(Utility.firstElement(list));

		list.add(1);
		list.add(2);
		assertEquals(Integer.valueOf(1), Utility.firstElement(list));
	}
	
	@Test
	public void testLastElement() {
		List<Integer> list = new ArrayList<>();
		assertNull(Utility.lastElement(list));

		list.add(1);
		list.add(2);
		assertEquals(Integer.valueOf(2), Utility.lastElement(list));
	}
	
	@Test
	public void testIfNull() {
		String str = "custom";

		String expected = str;
		String actual = Utility.ifNull(str, "default");
		assertEquals(expected, actual);

		expected = "default";
		actual = Utility.ifNull(null, "default");
		assertEquals(expected, actual);
	}
	
	@Test
	public void testGetLocalIP() {
		String ip = Utility.getLocalIP();
		assertNotNull(ip);
	}
	
	@Test
	@Ignore
	public void testGetPublicIP() {
		String ip = Utility.getPublicIP();
		assertNotNull("Please check your internet connection.", ip);
	}

}
