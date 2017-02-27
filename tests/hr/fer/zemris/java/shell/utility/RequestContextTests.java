package hr.fer.zemris.java.shell.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import hr.fer.zemris.java.shell.utility.RequestContext.RCCookie;

/**
 * Tests the functionality of {@link RequestContext} utility class.
 *
 * @author Mario Bobic
 */
@SuppressWarnings("javadoc")
public class RequestContextTests {
	
	/** Output stream to be used for primitive testing. */
	private static final OutputStream os = new ByteArrayOutputStream();

	//
	// Test constructors with basic arguments
	//
	
	@Test(expected=NullPointerException.class)
	public void testConstructorAllNull() {
		// must throw
		new RequestContext(null, null, null, null);
	}
	
	@Test
	public void testConstructorOutputStreamNotNull() {
		// must NOT throw
		new RequestContext(os, null, null, null);
	}
	
	
	//
	// Test public setter methods
	//
	
	/** Request context for regular testing of public setter methods. */
	private final RequestContext rc = new RequestContext(os, null, null, null);
	
	@Test
	public void testSetEncoding() {
		rc.setEncoding("ISO-8859-1");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testSetEncodingException() {
		// must throw
		rc.setEncoding("Štefica");
	}
	
	@Test
	public void testSetStatusCode() {
		rc.setStatusCode(505);
	}
	
	@Test
	public void testSetStatusText() {
		rc.setStatusText("505 s crtom.");
	}
	
	@Test
	public void testSetMimeType() {
		rc.setStatusText("text/css");
	}
	
	@Test
	public void testSetContentLength() {
		rc.setContentLength(1024);
	}
	
	@Test
	public void testAddRCCookie() {
		rc.addRCCookie(new RCCookie("Kuki", "chocolate", "localhost", "/", null));
	}
	
	
	//
	// All header setting methods should throw RuntimeException if header is already generated
	//
	
	/** Request context with header already generated. Test public setter methods again. */
	private final RequestContext rcGenerated;
	/** Generate header using the rc.write() method. */
	public RequestContextTests() {
		rcGenerated = new RequestContext(os, null, null, null);
		try { rcGenerated.write(""); } catch (IOException ignorable) {}
	}
	
	@Test(expected=RuntimeException.class)
	public void testSetEncodingRuntimeException() {
		rcGenerated.setEncoding("ISO-8859-1");
	}

	@Test(expected=RuntimeException.class)
	public void testSetStatusCodeRuntimeException() {
		rcGenerated.setStatusCode(505);
	}

	@Test(expected=RuntimeException.class)
	public void testSetStatusTextRuntimeException() {
		rcGenerated.setStatusText("505 s crtom.");
	}

	@Test(expected=RuntimeException.class)
	public void testSetMimeTypeRuntimeException() {
		rcGenerated.setStatusText("text/css");
	}

	@Test(expected=RuntimeException.class)
	public void testSetContentLengthRuntimeException() {
		rcGenerated.setContentLength(1024);
	}

	@Test(expected=RuntimeException.class)
	public void testAddRCCookieRuntimeException() {
		rcGenerated.addRCCookie(new RCCookie("Kuki", "chocolate", "localhost", "/", null));
	}
	
	
	//
	// Test maps of parameters
	//
	
	/* Parameters */
	
	@Test
	public void testGetParameter() {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("key", "value");
		
		RequestContext rc = new RequestContext(os, parameters, null, null);
		
		assertEquals("value", rc.getParameter("key"));
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testGetParameterNames() {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("name", "Mario");
		parameters.put("surname", "Bobic");
		parameters.put("age", "20");
		parameters.put("jmbag", "0036484942");

		RequestContext rc = new RequestContext(os, parameters, null, null);
		
		Set<String> parameterNames = rc.getParameterNames();
		// must throw
		parameterNames.remove("name");
	}
	
	
	/* Persistent parameters */
	
	@Test
	public void testGetPersistentParameter() {
		Map<String, String> persistentParameters = new HashMap<>();
		persistentParameters.put("key", "value");
		
		RequestContext rc = new RequestContext(os, null, persistentParameters, null);
		
		assertEquals("value", rc.getPersistentParameter("key"));
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testGetPersistentParameterNames() {
		Map<String, String> persistentParameters = new HashMap<>();
		persistentParameters.put("global", "parameter");

		RequestContext rc = new RequestContext(os, null, persistentParameters, null);
		
		Set<String> persistentParameterNames = rc.getPersistentParameterNames();
		// must throw
		persistentParameterNames.add("persistent");
	}
	
	@Test
	public void testSetPersistentParameter() {
		RequestContext rc = new RequestContext(os, null, null, null);
		
		rc.setPersistentParameter("global", "parameter");
		rc.setPersistentParameter("persistent", "parameter");
		
		assertEquals("parameter", rc.getPersistentParameter("global"));
		assertEquals("parameter", rc.getPersistentParameter("persistent"));
	}
	
	@Test
	public void testRemovePersistentParameter() {
		RequestContext rc = new RequestContext(os, null, null, null);
		rc.setPersistentParameter("global", "parameter");
		
		rc.removePersistentParameter("global");
		
		assertEquals(null, rc.getPersistentParameter("global"));
	}
	
	
	/* Temporary parameters */
	
	@Test
	public void testSetTemporaryParameter() {
		RequestContext rc = new RequestContext(os, null, null, null);
		
		rc.setTemporaryParameter("a", "0");
		rc.setTemporaryParameter("b", "1");
		
		assertEquals("0", rc.getTemporaryParameter("a"));
		assertEquals("1", rc.getTemporaryParameter("b"));
	}
	
	@Test
	public void testRemoveTemporaryParameter() {
		RequestContext rc = new RequestContext(os, null, null, null);
		
		rc.setTemporaryParameter("a", "0");
		rc.setTemporaryParameter("b", "1");
		
		rc.removeTemporaryParameter("a");
		rc.removeTemporaryParameter("b");
		
		assertTrue(rc.getTemporaryParameterNames().isEmpty());
	}
	
	
	//
	// Writing method tests
	//
	
	/** Output stream for write methods testing. */
	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	
	@Test
	public void testWriteByte() throws IOException {
		RequestContext rc = new RequestContext(outputStream, null, null, null);
		rc.setEncoding("ISO-8859-1");
		rc.setMimeType("text/plain");
		rc.setStatusCode(404);
		
		byte[] data = "Error 404 Page Not Found".getBytes("ISO-8859-1");
		rc.write(data);
		
		String actual = new String(outputStream.toByteArray(), "ISO-8859-1");
		
		assertTrue(actual.endsWith("Error 404 Page Not Found"));
		assertTrue(actual.contains("Content-Type: text/plain; charset=ISO-8859-1"));
		assertTrue(actual.contains("404 OK")); // lol
	}
	
	@Test
	public void testWriteByteOffLen() throws IOException {
		RequestContext rc = new RequestContext(outputStream, null, null, null);
		
		String html = "<html><body>Test</body></html>";
		byte[] data = html.getBytes("UTF-8");
		
		rc.setContentLength(data.length);
		rc.write(data, 0, 12); // <html><body>
		rc.write(data, 16, 14); // </body></html>
		
		String actual = new String(outputStream.toByteArray(), "UTF-8");
		
		assertEquals(html.length(), data.length); // this MUST be true
		assertTrue(actual.endsWith("<html><body></body></html>"));
		assertTrue(actual.contains("Content-Length: " + data.length));
	}
	
	@Test
	public void testWriteByteString() throws IOException {
		RequestContext rc = new RequestContext(outputStream, null, null, null);
		rc.setEncoding("US-ASCII");
		
		rc.addRCCookie(new RCCookie("Kolacic", "Njami", null, "/", null));
		rc.addRCCookie(new RCCookie("Chocolate", "Chip", null, null, null));
		
		String usa = "United States of America";
		rc.write(usa);
		
		String actual = new String(outputStream.toByteArray(), "US-ASCII");
		
		assertTrue(actual.endsWith(usa));
		assertTrue(actual.contains("charset=US-ASCII"));
		assertTrue(actual.contains("Set-Cookie: Kolacic=\"Njami\"; Path=/"));
		assertTrue(actual.contains("Set-Cookie: Chocolate=\"Chip\""));
	}
	
	
	@Test(expected=NullPointerException.class)
	public void testWriteByteNull() throws IOException {
		rc.write((byte[]) null);
	}
	
	@Test(expected=NullPointerException.class)
	public void testWriteByteOffLenNull() throws IOException {
		rc.write((byte[]) null, 0, 0);
	}
	
	@Test(expected=NullPointerException.class)
	public void testWriteStringNull() throws IOException {
		rc.write((String) null);
	}
	
	
	
	/* -------------------------------- Utility --------------------------------- */
	
}
