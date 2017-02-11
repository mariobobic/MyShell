package hr.fer.zemris.java.shell.utility;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class defines useful HTML context like encoding, status code, status
 * text, mime type, cookies, header etc. If no context is explicitly set, all
 * values are set to default as following:
 * <ul>
 * <li>Encoding is set to <tt>UTF-8</tt>.
 * <li>Status code is set to <tt>200</tt>.
 * <li>Status text is set to <tt>OK</tt>.
 * <li>Mime type is set to <tt>text/html</tt>.
 * </ul>
 * <p>
 * When creating an instance of this class, by using the class constructor
 * {@linkplain #RequestContext(OutputStream, Map, Map, List)} one must define at
 * least the output stream to where the request context will be written.
 * <p>
 * To set cookies, use instances of the {@linkplain RCCookie} class.
 *
 * @author Mario Bobic
 */
public class RequestContext {
	
	/** Default context encoding. */
	private static final String DEF_ENCODING = "UTF-8";
	/** Default response HTTP status code. */
	private static final int DEF_STATUS_CODE = 200;
	/** Default response HTTP status message text. */
	private static final String DEF_STATUS_TEXT = "OK";
	/** Default HTTP mime type. */
	private static final String DEF_MIME_TYPE = "text/html";

	/** Output stream of the context. */
	private OutputStream outputStream;
	/** Context charset. */
	private Charset charset = Charset.forName(DEF_ENCODING);

	/** Context encoding. */
	private String encoding = DEF_ENCODING;
	/** Response HTTP status code. */
	private int statusCode = DEF_STATUS_CODE;
	/** Response HTTP status message text. */
	private String statusText = DEF_STATUS_TEXT;
	/** Response HTTP mime type. */
	private String mimeType = DEF_MIME_TYPE;
	/** Optional content length to be added to the header. */
	private int contentLength = -1;
	
	/** Parameters stored within this context. */
	private final Map<String, String> parameters;
	/** Temporary parameters stored within this context. */
	private Map<String, String> temporaryParameters;
	/** Persistent parameters stored within this context. */
	private Map<String, String> persistentParameters;
	/** Cookies which will be sent out in response header. */
	private List<RCCookie> outputCookies;
	
	/** Indicates that the header has been generated. */
	private boolean headerGenerated;
	
	/**
	 * Constructs an instance of {@code RequestContext} with the specified parameters.
	 * 
	 * @param outputStream context output stream, must not be <tt>null</tt>
	 * @param parameters parameters stored within this context
	 * @param persistentParameters persistent parameters stored within this context
	 * @param outputCookies cookies which will be sent out in response header
	 */
	public RequestContext(OutputStream outputStream, Map<String, String> parameters,
			Map<String, String> persistentParameters, List<RCCookie> outputCookies) {
		Objects.requireNonNull(outputStream, "Output stream must not be null.");
		
		this.outputStream = outputStream;
		this.parameters = Collections.unmodifiableMap(parameters == null ? new HashMap<>() : parameters);
		this.persistentParameters = persistentParameters == null ? new HashMap<>() : persistentParameters;
		this.outputCookies = outputCookies == null ? new ArrayList<>() : outputCookies;
		
		temporaryParameters = new HashMap<>();
	}
	
	//
	// Setter methods
	//
	
	/**
	 * Sets the context encoding to the specified <tt>encoding</tt>. If the
	 * specified <tt>encoding</tt> is <tt>null</tt>, default value is set.
	 * 
	 * @param encoding encoding to be set
	 * @throws RuntimeException if header has already been generated
	 * @throws IllegalCharsetNameException if the given encoding name is illegal
	 * @throws UnsupportedCharsetException if no support for the named encoding
	 * is available in this instance of the Java virtual machine
	 */
	public void setEncoding(String encoding) {
		checkHeaderGenerated();
		this.encoding = encoding != null ? encoding : DEF_ENCODING;
		
		charset = Charset.forName(encoding);
	}

	/**
	 * Sets the response HTTP status code to the specified <tt>statusCode</tt>.
	 * If the specified <tt>statusCode</tt> is less than <tt>0</tt>, default
	 * value is set.
	 * 
	 * @param statusCode status code to be set
	 * @throws RuntimeException if header has already been generated
	 */
	public void setStatusCode(int statusCode) {
		checkHeaderGenerated();
		this.statusCode = statusCode >= 0 ? statusCode : DEF_STATUS_CODE;
	}

	/**
	 * Sets the response HTTP status message text to the specified
	 * <tt>statusText</tt>. If the specified <tt>statusText</tt> is
	 * <tt>null</tt>, default value is set.
	 * 
	 * @param statusText status text to be set
	 * @throws RuntimeException if header has already been generated
	 */
	public void setStatusText(String statusText) {
		checkHeaderGenerated();
		this.statusText = statusText != null ? statusText : DEF_STATUS_TEXT;
	}

	/**
	 * Sets the response HTTP mime type to the specified <tt>mimeType</tt>. If
	 * the specified <tt>mimeType</tt> is <tt>null</tt>, default value is set.
	 * 
	 * @param mimeType mime type to be set
	 * @throws RuntimeException if header has already been generated
	 */
	public void setMimeType(String mimeType) {
		checkHeaderGenerated();
		this.mimeType = mimeType != null ? mimeType : DEF_MIME_TYPE;
	}
	
	/**
	 * Sets the content length to the specified <tt>contentLength</tt>. If the
	 * specified <tt>contentLength</tt> is less than <tt>0</tt>, content length
	 * will not be added to the header.
	 * 
	 * @param contentLength content length to be set
	 * @throws RuntimeException if header has already been generated
	 */
	public void setContentLength(int contentLength) {
		checkHeaderGenerated();
		this.contentLength = contentLength >= 0 ? contentLength : -1;
	}
	
	/**
	 * Adds the specified RC <tt>cookie</tt> to a collection of output cookies.
	 * 
	 * @param cookie cookie to be added
	 * @throws RuntimeException if header has already been generated
	 * @throws NullPointerException if <tt>cookie</tt> is <tt>null</tt>
	 */
	public void addRCCookie(RCCookie cookie) {
		checkHeaderGenerated();
		Objects.requireNonNull(cookie);
		outputCookies.add(cookie);
	}
	
	/**
	 * Checks if the header has already been generated and throws
	 * {@linkplain RuntimeException} if it has.
	 * 
	 * @throws RuntimeException if header has already been generated
	 */
	private void checkHeaderGenerated() {
		if (headerGenerated) {
			throw new RuntimeException("Header already generated.");
		}
	}
	
	//
	// Collection manipulation methods
	//
	
	/**
	 * Returns the parameter associated with the specified <tt>name</tt>.
	 * 
	 * @param name name of the parameter to be returned
	 * @return the parameter associated with the specified <tt>name</tt>
	 */
	public String getParameter(String name) {
		return parameters.get(name);
	}
	
	/**
	 * Returns an <strong>unmodifiable</strong> <tt>Set</tt> of parameter names.
	 * 
	 * @return a Set of parameter names
	 */
	public Set<String> getParameterNames() {
		return Collections.unmodifiableSet(parameters.keySet());
	}
	
	/**
	 * Returns the persistent parameter associated with the specified
	 * <tt>name</tt>.
	 * 
	 * @param name name of the persistent parameter to be returned
	 * @return the persistent parameter associated with the specified name
	 */
	public String getPersistentParameter(String name) {
		return persistentParameters.get(name);
	}
	
	/**
	 * Returns an <strong>unmodifiable</strong> <tt>Set</tt> of persistent
	 * parameter names.
	 * 
	 * @return a Set of persistent parameter names
	 */
	public Set<String> getPersistentParameterNames() {
		return Collections.unmodifiableSet(persistentParameters.keySet());
	}
	
	/**
	 * Stores the specified <tt>value</tt> to the persistent parameters map and
	 * associates it with the specified <tt>name</tt>. If the map previously
	 * contained a mapping for the key, the old value is replaced by the
	 * specified value.
	 * 
	 * @param name persistent parameter name
	 * @param value value to be stored
	 */
	public void setPersistentParameter(String name, String value) {
		persistentParameters.put(name, value);
	}
	
	/**
	 * Removes value associated with the specified <tt>name</tt> from the
	 * persistent parameters map.
	 * 
	 * @param name name whose value is to be removed
	 */
	public void removePersistentParameter(String name) {
		persistentParameters.remove(name);
	}
	
	/**
	 * Returns the temporary parameter associated with the specified
	 * <tt>name</tt>.
	 * 
	 * @param name name of the temporary parameter to be returned
	 * @return the temporary parameter associated with the specified name
	 */
	public String getTemporaryParameter(String name) {
		return temporaryParameters.get(name);
	}
	
	/**
	 * Returns an <strong>unmodifiable</strong> <tt>Set</tt> of temporary
	 * parameter names.
	 * 
	 * @return a Set of temporary parameter names
	 */
	public Set<String> getTemporaryParameterNames() {
		return Collections.unmodifiableSet(temporaryParameters.keySet());
	}
	
	/**
	 * Stores the specified <tt>value</tt> to the temporary parameters map and
	 * associates it with the specified <tt>name</tt>. If the map previously
	 * contained a mapping for the key, the old value is replaced by the
	 * specified value.
	 * 
	 * @param name temporary parameter name
	 * @param value value to be stored
	 */
	public void setTemporaryParameter(String name, String value) {
		temporaryParameters.put(name, value);
	}
	
	/**
	 * Removes value associated with the specified <tt>name</tt> from the
	 * temporary parameters map.
	 * 
	 * @param name name whose value is to be removed
	 */
	public void removeTemporaryParameter(String name) {
		temporaryParameters.remove(name);
	}
	
	//
	// Byte operation methods
	//
	
	/**
	 * Writes the specified <tt>data</tt> array of bytes to the context output
	 * stream. Generates the header if it hasn't been previously generated.
	 * 
	 * @param data data to be written to the output stream
	 * @return this RequestContext object
	 * @throws IOException if an I/O exception occurs
	 */
	public RequestContext write(byte[] data) throws IOException {
		Objects.requireNonNull(data);
		return write(data, 0, data.length);
	}
	
	/**
	 * Writes <code>len</code> bytes from the specified byte array <tt>data</tt>
	 * starting at offset <code>off</code> to the context output stream.
	 * Generates the header if it hasn't been previously generated.
	 * 
	 * @param data data to be written to the output stream
	 * @param off the start offset in the data.
     * @param len the number of bytes to write.
	 * @return this RequestContext object
	 * @throws IOException if an I/O exception occurs
	 */
	public RequestContext write(byte[] data, int off, int len) throws IOException {
		Objects.requireNonNull(data);
		if (!headerGenerated) {
			byte[] header = generateHeader();
			outputStream.write(header);
		}
		
		outputStream.write(data, off, len);
		return this;
	}
	
	/**
	 * Writes the specified <tt>text</tt> string to the context output stream
	 * encoded with the previously set encoding or the default one if no
	 * encoding was manually set. Generates the header if it hasn't been
	 * previously generated.
	 * 
	 * @param text text to be written to the output stream
	 * @return this RequestContext object
	 * @throws IOException if an I/O exception occurs
	 * @throws NullPointerException if <tt>text</tt> is <tt>null</tt>
	 */
	public RequestContext write(String text) throws IOException {
		Objects.requireNonNull(text);
		return write(text.getBytes(charset));
	}
	
	/**
	 * Generates the header based on <tt>statusCode</tt>, <tt>statusText</tt>,
	 * <tt>mimeType</tt>, <tt>encoding</tt> and <tt>outputCookies</tt>
	 * properties.
	 * <p>
	 * After the header has been generated, the <tt>headerGenerated</tt> flag is
	 * set to <tt>true</tt> and the bytes of the header are returned encoded
	 * into a sequence of bytes using the {@linkplain StandardCharsets#ISO_8859_1}
	 * charset.
	 * 
	 * @return bytes of generated header encoded with ISO-8859-1
	 */
	private byte[] generateHeader() {
		final String NEW_LINE = "\r\n";
		StringBuilder header = new StringBuilder();
		
		// HTTP/1.1 statusCode statusMessage
		header.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText);
		header.append(NEW_LINE);
		
		// Content-Type: mimeType (; charset=encoding)
		header.append("Content-Type: ").append(mimeType);
		if (mimeType.startsWith("text/")) {
			header.append("; charset=").append(encoding);
		}
		header.append(NEW_LINE);
		
		// Content-Length: contentLength
		if (contentLength >= 0) {
			header.append("Content-Length: ").append(contentLength);
			header.append(NEW_LINE);
		}
		
		// Set-Cookie: name="value"; Domain=domain; Path=path; Max-Age=maxAge
		for (RCCookie cookie : outputCookies) {
			appendCookie(header, cookie);
			header.append(NEW_LINE);
		}
		
		// Signal the end of headers
		header.append(NEW_LINE);
		
		headerGenerated = true;
		return header.toString().getBytes(StandardCharsets.ISO_8859_1);
	}
	
	/**
	 * Appends the specified <tt>cookie</tt> to the specified StringBuilder
	 * <tt>header</tt>. The cookie's <tt>name</tt> and <tt>value</tt> are always
	 * present, while attributes <tt>domain</tt>, <tt>path</tt> and
	 * <tt>maxAge</tt> may be <tt>null</tt>.
	 * <p>
	 * The cookie is also checked to be HTTP-only, and if true, this attribute
	 * is appended to the end of the Set-Cookie line.
	 * 
	 * @param header the string builder
	 * @param cookie cookie to be appended
	 */
	private static void appendCookie(StringBuilder header, RCCookie cookie) {
		header.append("Set-Cookie: ").append(cookie.name).append("=\"").append(cookie.value).append("\";");
		if (cookie.domain != null) header.append(" Domain=").append(cookie.domain).append(";");
		if (cookie.path != null) header.append(" Path=").append(cookie.path).append(";");
		if (cookie.maxAge != null) header.append(" Max-Age=").append(cookie.maxAge).append(";");
		if (cookie.httpOnly) header.append(" HttpOnly;");
		header.deleteCharAt(header.length()-1);
	}
	
	//
	// Utility
	//
	
	/**
	 * This class represents a Request Context cookie. It defines basic
	 * parameters to be included in a request context when sending a
	 * <tt>Set-Cookie</tt> directive.
	 *
	 * @author Mario Bobic
	 */
	public static class RCCookie {
		/** Cookie name. */
		public final String name;
		/** Cookie value. */
		public final String value;
		/** Cookie domain. */
		public final String domain;
		/** Cookie path. */
		public final String path;
		/** Cookie maximum age. */
		public final Integer maxAge;
		
		/** Is cookie exposable to any other channel except HTTP (and HTTPS)? */
		private boolean httpOnly;
		
		/**
		 * Constructs an instance of {@code RCCookie} with the specified parameters
		 * 
		 * @param name name of the cookie, must not be <tt>null</tt>
		 * @param value value of the cookie, must not be <tt>null</tt>
		 * @param domain domain of the cookie
		 * @param path path of the cookie
		 * @param maxAge maximum age of the cookie
		 * @throws NullPointerException if name or value is <tt>null</tt>
		 */
		public RCCookie(String name, String value, String domain, String path, Integer maxAge) {
			this.name = Objects.requireNonNull(name);
			this.value = Objects.requireNonNull(value);;
			this.domain = domain;
			this.path = path;
			this.maxAge = maxAge;
		}

		/**
		 * Sets the HTTP-only property of this cookie. For cookies that must not
		 * expose to any other channel except HTTP (and HTTPS), server can
		 * request that a cookie must be treated as HTTP-only cookie.
		 * 
		 * @param b value to be set
		 */
		public void setHttpOnly(boolean b) {
			httpOnly = b;
		}
		
	}
	
}
