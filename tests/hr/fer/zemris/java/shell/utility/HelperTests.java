package hr.fer.zemris.java.shell.utility;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import hr.fer.zemris.java.shell.MyShell.EnvironmentImpl;
import hr.fer.zemris.java.shell.utility.exceptions.IllegalPathException;
import hr.fer.zemris.java.shell.utility.exceptions.NotEnoughDiskSpaceException;

/**
 * Tests the functionality of {@link Helper} utility class.
 *
 * @author Mario Bobic
 */
@SuppressWarnings("javadoc")
public class HelperTests {
	
	/** Environment used by some tests. */
	private EnvironmentImpl environment = new EnvironmentImpl();


	/* ------------------------------ Helper tests ------------------------------ */
	
	@Test
	public void testResolveAbsolutePath() throws IOException {
		String str = ".";
		
		Path actual = Helper.resolveAbsolutePath(environment, str);
		Path expected = Paths.get(str);
		
		assertTrue(Files.isSameFile(expected, actual));
	}

	@Test
	public void testResolveAbsolutePathUserHome() throws IOException {
		Path home = Paths.get(System.getProperty("user.home"));
		
		Path expected = home.resolve("Downloads");
		Path actual = Helper.resolveAbsolutePath(environment, "~/Downloads");
		
		assertTrue(Files.isSameFile(expected, actual));
	}
	
	@Test
	public void testResolveAbsolutePathMarkedInteger() throws IOException {
		Path home = Paths.get(System.getProperty("user.home"));
		int id = environment.mark(home);
		
		Path actual = Helper.resolveAbsolutePath(environment, Integer.toString(id));
		Path expected = home;
		
		assertTrue(Files.isSameFile(expected, actual));
	}
	
	@Test
	public void testGetFileName1() {
		Path file = Paths.get("./file.txt").toAbsolutePath();
		
		Path expectedFile = file.getFileName();
		Path actualFile = Helper.getFileName(file);
		assertEquals(expectedFile, actualFile);
	}
	
	@Test
	public void testGetFileName2() {
		Path root = Paths.get("./file.txt").toAbsolutePath().getRoot();
		
		Path expectedRoot = root;
		Path actualRoot = Helper.getFileName(root);
		assertEquals(expectedRoot, actualRoot);
	}
	
	@Test
	public void testGetParent1() {
		Path file = Paths.get("./file.txt").toAbsolutePath();
		
		Path expectedParent = file.getParent();
		Path actualParent = Helper.getParent(file);
		assertEquals(expectedParent, actualParent);
	}
	
	@Test
	public void testGetParent2() {
		Path root = Paths.get("./file.txt").toAbsolutePath().getRoot();
		
		Path expectedRoot = root;
		Path actualRoot = Helper.getParent(root);
		assertEquals(expectedRoot, actualRoot);
	}
	
	@Test
	public void testIsHiddenFile() throws IOException {
		// Create it and set to hidden
		Path hiddenFile = Files.createTempFile(null, null);
		Files.setAttribute(hiddenFile, "dos:hidden", Boolean.TRUE);
		
		assertTrue(Helper.isHidden(hiddenFile));
		
		// Delete it afterwards
		Files.delete(hiddenFile);
	}
	
	@Test
	public void testIsHiddenDirectory() throws IOException {
		// Create it and set to hidden
		Path hiddenDirectory = Files.createTempDirectory(null);
		Files.setAttribute(hiddenDirectory, "dos:hidden", Boolean.TRUE);
		
		assertTrue(Helper.isHidden(hiddenDirectory));
		
		// Delete it afterwards
		Files.delete(hiddenDirectory);
	}
	
	@Test
	public void testIsNotHidden() throws IOException {
		// Create not hidden file and directory
		Path file = Files.createTempFile(null, null);
		Path dir = Files.createTempDirectory(null);

		assertFalse(Helper.isHidden(file));
		assertFalse(Helper.isHidden(dir));
		
		// Delete them afterwards
		Files.delete(file);
		Files.delete(dir);
	}
	
	@Test
	public void testIsHiddenRoot() throws IOException {
		// roots must not be hidden
		FileSystems.getDefault().getRootDirectories().forEach(root -> {
			assertFalse(Helper.isHidden(root));
		});
	}
	
	@Test
	public void testFirstAvailable1() throws IOException {
		// Create a file
		Path file = Files.createTempFile(null, ".txt");
		
		// Get the first available file (should not be 'file')
		Path firstAvailable = Helper.firstAvailable(file).toAbsolutePath();
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
		Path firstAvailable = Helper.firstAvailable(file).toAbsolutePath();
		assertNotEquals(file, firstAvailable);
		
		// Delete the file afterwards
		Files.delete(file);
	}
	
	@Test
	public void testFirstAvailable3() throws IOException {
		Path file = Paths.get("./file.txt").toAbsolutePath();
		
		// Get the first available file (should be exactly 'file')
		Path firstAvailable = Helper.firstAvailable(file).toAbsolutePath();
		assertEquals(file, firstAvailable);
	}
	
	@Test
	public void testExtension1() {
		Path file = Paths.get("./file.txt");
		assertEquals(".txt", Helper.extension(file));
	}
	
	@Test
	public void testExtension2() {
		Path file = Paths.get("./file.");
		assertEquals(".", Helper.extension(file));
	}
	
	@Test
	public void testExtension3() {
		Path file = Paths.get("./file");
		assertEquals("", Helper.extension(file));
	}
	
	@Test
	public void testRequireExists1() throws IOException {
		// Create a file
		Path file = Files.createTempFile(null, null);
		Helper.requireExists(file);
		
		// Delete the file afterwards
		Files.delete(file);
	}
	
	@Test(expected = IllegalPathException.class)
	public void testRequireExists2() throws IOException {
		Path file = Paths.get("./non-existent-file");
		Helper.requireExists(file);
	}
	
	@Test
	public void testRequireDirectory1() throws IOException {
		// Create a directory
		Path dir = Files.createTempDirectory(null);
		Helper.requireDirectory(dir);
		
		// Delete the directory afterwards
		Files.delete(dir);
	}
	
	@Test(expected = IllegalPathException.class)
	public void testRequireDirectory2() throws IOException {
		// Create a file
		Path file = Files.createTempFile(null, null);
		Helper.requireDirectory(file);
	}
	
	@Test
	public void testRequireFile1() throws IOException {
		// Create a file
		Path file = Files.createTempFile(null, null);
		Helper.requireFile(file);
		
		// Delete the file afterwards
		Files.delete(file);
	}
	
	@Test(expected = IllegalPathException.class)
	public void testRequireFile2() throws IOException {
		// Create a directory
		Path dir = Files.createTempDirectory(null);
		Helper.requireFile(dir);
	}
	
	@Test
	public void testRequireDiskSpace1() throws IOException {
		Helper.requireDiskSpace(0, environment.getHomePath());
	}
	
	@Test(expected = NotEnoughDiskSpaceException.class)
	public void testRequireDiskSpace2() throws IOException {
		long fiveTiB = 5L*1000*1000*1000*1000; // five terabytes
		Helper.requireDiskSpace(fiveTiB, environment.getHomePath());
	}

	@Test
	public void testHumanReadableByteCount1() {
		final long kB = 1024L;
		
		assertEquals("1 B",     Helper.humanReadableByteCount(1));
		assertEquals("1.0 kiB", Helper.humanReadableByteCount(kB));
		assertEquals("1.0 MiB", Helper.humanReadableByteCount(kB*kB));
		assertEquals("1.0 GiB", Helper.humanReadableByteCount(kB*kB*kB));
		assertEquals("1.0 TiB", Helper.humanReadableByteCount(kB*kB*kB*kB));
		assertEquals("1.0 PiB", Helper.humanReadableByteCount(kB*kB*kB*kB*kB));
		assertEquals("1.0 EiB", Helper.humanReadableByteCount(kB*kB*kB*kB*kB*kB));
	}
	
	@Test
	public void testHumanReadableByteCount2() {
		final long kB = 1024L;

		assertEquals("1000 B",  Helper.humanReadableByteCount(1000));
		assertEquals("2.5 kiB", Helper.humanReadableByteCount(2*kB + kB/2));
		assertEquals("1.5 MiB", Helper.humanReadableByteCount(1024*kB + 512*kB));
	}
	
	@Test
	public void testParseSize1() {
		assertEquals(2048L,       Helper.parseSize("2 kiB"));
		assertEquals(10485760L,   Helper.parseSize("10 MiB"));
		assertEquals(1073741824L, Helper.parseSize("1 GiB"));
		assertEquals(16L,         Helper.parseSize("16 B"));
		assertEquals(4096L,       Helper.parseSize("4096"));
		assertEquals(2560L,       Helper.parseSize("2.5 kiB"));
		assertEquals(2560L,       Helper.parseSize("2,5 kiB"));
		assertEquals(1024L,       Helper.parseSize("1kiB"));
		assertEquals(1048576L,    Helper.parseSize("1MIB"));
	}
	
	@Test
	public void testParseSize2() {
		assertEquals(1000L,       Helper.parseSize("1 kB"));
		assertEquals(1000000L,    Helper.parseSize("1 MB"));
		assertEquals(1000000000L, Helper.parseSize("1 gb"));
		assertEquals(1000000000L, Helper.parseSize("1gB"));
		assertEquals(1234567890L, Helper.parseSize("1.23456789 GB"));
		assertEquals(1234567890L, Helper.parseSize("1,23456789 GB"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeEmpty() {
		Helper.parseSize("");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeNull() {
		Helper.parseSize(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeNegative() {
		Helper.parseSize("-2 kB");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeDoublePeriod() {
		Helper.parseSize("2..5 kB");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeWord() {
		Helper.parseSize("Foo");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeWrongUnit1() {
		Helper.parseSize("1.21 GW");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testParseSizeWrongUnit2() {
		Helper.parseSize("14 jB");
	}
	
	@Test
	public void testHumanReadableTimeUnit1() {
		final long us = 1000L;
		
		assertEquals("1 ns",   Helper.humanReadableTimeUnit(1));
		assertEquals("1 us",   Helper.humanReadableTimeUnit(us));
		assertEquals("1 ms",   Helper.humanReadableTimeUnit(us*us));
		assertEquals("1 s",    Helper.humanReadableTimeUnit(us*us*us));
		assertEquals("1 min",  Helper.humanReadableTimeUnit(60*us*us*us));
		assertEquals("1 hr",   Helper.humanReadableTimeUnit(60*60*us*us*us));
		assertEquals("1 days", Helper.humanReadableTimeUnit(24*60*60*us*us*us));
	}
	
	@Test
	public void testHumanReadableTimeUnit2() {
		final long s = 1_000_000_000L;
		final long min = 60*s;
		final long hr = 60*min;
		final long day = 24*hr;
		
		assertEquals("123 ns",                   Helper.humanReadableTimeUnit(123));
		assertEquals("2 min 15 s",               Helper.humanReadableTimeUnit(2*min + 15*s));
		assertEquals("1 hr 15 min 20 s",         Helper.humanReadableTimeUnit(hr + 15*min + 20*s));
		assertEquals("2 days 23 hr 59 min 59 s", Helper.humanReadableTimeUnit(2*day + 23*hr + 59*min + 59*s));
		assertEquals("365 days",                 Helper.humanReadableTimeUnit(365*day));
	}
	
	@Test
	public void testGeneratePasswordHash() {
		String hash = Helper.generatePasswordHash("password");
		assertNotEquals("password", hash);
	}
	
	@Test
	public void testResolveCharset1() {
		Charset charset = Helper.resolveCharset("UTF-8");
		assertEquals(StandardCharsets.UTF_8, charset);
	}
	
	@Test
	public void testResolveCharset2() {
		Charset charset = Helper.resolveCharset("nothing");
		assertEquals(null, charset);
	}
	
	@Test
	public void testIsInteger() {
		assertTrue(Helper.isInteger("0"));
		assertTrue(Helper.isInteger("-1"));
		assertTrue(Helper.isInteger(Integer.toString(Integer.MAX_VALUE)));
		assertTrue(Helper.isInteger(Integer.toString(Integer.MIN_VALUE)));

		assertFalse(Helper.isInteger(""));
		assertFalse(Helper.isInteger("abc"));
		assertFalse(Helper.isInteger("2.25"));
		assertFalse(Helper.isInteger("2,50"));
		assertFalse(Helper.isInteger(Long.toString(Long.MAX_VALUE)));
	}
	
	@Test
	public void testIsLong() {
		assertTrue(Helper.isLong("0"));
		assertTrue(Helper.isLong("-1"));
		assertTrue(Helper.isLong(Long.toString(Long.MAX_VALUE)));
		assertTrue(Helper.isLong(Long.toString(Long.MIN_VALUE)));

		assertFalse(Helper.isLong(""));
		assertFalse(Helper.isLong("abc"));
		assertFalse(Helper.isLong("2.25"));
		assertFalse(Helper.isLong("2,50"));
		assertFalse(Helper.isLong(Double.toString(Double.MAX_VALUE)));
	}
	
	@Test
	public void testIsDouble() {
		assertTrue(Helper.isDouble("0"));
		assertTrue(Helper.isDouble("-1"));
		assertTrue(Helper.isDouble(".5"));
		assertTrue(Helper.isDouble("2.25"));
		assertTrue(Helper.isDouble("1E10"));
		assertTrue(Helper.isDouble(Double.toString(Double.NaN)));
		assertTrue(Helper.isDouble(Double.toString(Double.MAX_VALUE)));
		assertTrue(Helper.isDouble(Double.toString(Double.MIN_VALUE)));
		assertTrue(Helper.isDouble(Double.toString(Double.POSITIVE_INFINITY)));

		assertFalse(Helper.isDouble(""));
		assertFalse(Helper.isDouble("."));
		assertFalse(Helper.isDouble("abc"));
		assertFalse(Helper.isDouble("1.2.3"));
	}
	
	@Test
	public void testFirstElement() {
		List<Integer> list = new ArrayList<>();
		assertNull(Helper.firstElement(list));

		list.add(1);
		list.add(2);
		assertEquals(Integer.valueOf(1), Helper.firstElement(list));
	}
	
	@Test
	public void testLastElement() {
		List<Integer> list = new ArrayList<>();
		assertNull(Helper.lastElement(list));

		list.add(1);
		list.add(2);
		assertEquals(Integer.valueOf(2), Helper.lastElement(list));
	}
	
	@Test
	public void testIfNull() {
		String str = "custom";

		String expected = str;
		String actual = Helper.ifNull(str, "default");
		assertEquals(expected, actual);

		expected = "default";
		actual = Helper.ifNull(null, "default");
		assertEquals(expected, actual);
	}
	
	@Test
	public void testGetLocalIP() {
		String ip = Helper.getLocalIP();
		assertNotNull(ip);
	}
	
	@Test
	@Ignore
	public void testGetPublicIP() {
		String ip = Helper.getPublicIP();
		assertNotNull("Please check your internet connection.", ip);
	}

}
