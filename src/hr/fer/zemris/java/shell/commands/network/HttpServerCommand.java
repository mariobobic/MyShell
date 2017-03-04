package hr.fer.zemris.java.shell.commands.network;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.VisitorCommand;
import hr.fer.zemris.java.shell.commands.system.LsCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.Helper;
import hr.fer.zemris.java.shell.utility.RequestContext;

/**
 * A command that is used to host a HTTP server. This command accepts a
 * functional argument, which may be either <strong>start</strong> or
 * <strong>stop</strong>, and other optional arguments for defined as follows:
 * <p>
 * <table border="1">
 * <tr>
 *   <th>Command</th>
 *   <th>Description</th>
 * </tr>
 * <tr>
 *   <td>httpserver start (&lt;port&gt;)</td>
 *   <td>Starts a HTTP server on the current path and port number if specified. If
 * the port is not specified, <tt>8080</tt> is used.</td>
 * </tr>
 * <tr>
 *   <td>httpserver stop (&lt;path&gt;)</td>
 *   <td>Stops the HTTP server that are running on the specified paths, or current
 * path if no path is specified.</td>
 * </tr>
 * <tr>
 *   <td>httpserver stop all</td>
 *   <td>Stops all running HTTP servers.</td>
 * </tr>
 * </table>
 * 
 * @author Mario Bobic
 */
public class HttpServerCommand extends VisitorCommand {
	
	/** Map of running server threads. */
	private static Map<Path, ServerThread> serversMap = new LinkedHashMap<>();
	
	/** The date-time formatter used for tracking client sockets. */
	private static final DateTimeFormatter FORMATTER =
			DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	
	/** Default address which the server listens. */
	private static final String ADDRESS = "0.0.0.0";
	/** Default port number which the server listens. */
	private static final int DEFAULT_PORT = 8080;
	
	/** Amount of threads used by the thread pool. */
	private static final int NUM_THREADS = 5;
	/** Thread pool of client workers. */
	private ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);

	/**
	 * Constructs a new command object of type {@code HttpServerCommand}.
	 */
	public HttpServerCommand() {
		super("HTTPSERVER", createCommandDescription());
	}
	
	@Override
	protected String getCommandSyntax() {
		return "| start (<port>) | stop (<path>|all)";
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
		desc.add("Hosts a HTTP server of the current directory.");
		desc.add("One mandatory argument must be specified, which is either start or stop.");
		desc.add("If start is chosen, one optional argument may be specified, which is a port.");
		desc.add("If the port number is not provided, 8080 is used.");
		desc.add("If stop is chosen, it must be followed by a server path to stop or 'all' to stop all.");
		desc.add("If no arguments are specified, this command prints out server paths.");
		return desc;
	}
	
	@Override
	protected CommandStatus execute0(Environment env, String s) {
		String[] args = Helper.extractArguments(s, 2);
		if (args.length == 0) {
			if (serversMap.isEmpty()) {
				writeln(env, "There are no HTTP servers hosted on this machine.");
			} else {
				writeln(env, "HTTP servers running on: ");
				serversMap.keySet().forEach(serverPath -> writeln(env, "  "+serverPath));
			}
			return CommandStatus.CONTINUE;
		}
		
		if ("start".equals(args[0])) {
			start(args, env);
		} else if ("stop".equals(args[0])) {
			stop(args, env);
		} else {
			writeln(env, "Unknown argument: " + args[0]);
		}

		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Tries to start a HTTP server on the current path of the environment.
	 * <p>
	 * Cases in which the HTTP server will fail to start:
	 * <ol>
	 * <li>if a server on the current path is already up and running,
	 * <li>if a port number is given, but is an invalid integer,
	 * <li>if the given or default port number is already in use by another HTTP
	 * server and
	 * <li>if the given or default port number is in use by another application.
	 * </ol>
	 * Returns <tt>true</tt> if a HTTP server is successfully started,
	 * <tt>false</tt> otherwise.
	 * 
	 * @param args arguments for the start method, including 'start' on the
	 *        first position
	 * @param env an environment
	 * @return true if a HTTP server is successfully started, false otherwise
	 */
	private boolean start(String[] args, Environment env) {
		Path path = env.getCurrentPath();
		
		if (serversMap.containsKey(path)) {
			writeln(env, "HTTP server for path " + path + " is already up and running!");
			return false;
		}

		/* Parse the port number, if any. */
		int port = DEFAULT_PORT;
		if (args.length == 2) {
			try {
				port = Integer.parseInt(args[1]);
			} catch (Exception e) {
				writeln(env, "Invalid port number: " + args[1]);
				return false;
			}
		}
		
		// First check if any server thread is using this port
		for (Map.Entry<Path, ServerThread> entry : serversMap.entrySet()) {
			if (port == entry.getValue().port) {
				writeln(env, "Port " + port + " is already in use by " + entry.getKey());
				return false;
			}
		}

		// Then check if OS uses this port
		if (!available(port)) {
			writeln(env, "Port " + port + " is already in use by another application.");
			return false;
		}

		/* Passed all checks, start working. */
		ServerThread serverThread = new ServerThread(ADDRESS, port, path);
		serversMap.put(path, serverThread);
		serverThread.start();
		
		writeln(env, "Started HTTP server in " + path + " ("+FORMATTER.format(LocalDateTime.now())+")");
		
		/* Print out a message that the connection is ready. */
		write(env, "Connect to " + Helper.getLocalIP() + ":" + port);
		writeln(env, " / " + Helper.getPublicIP() + ":" + port);
		return true;
	}
	
	/**
	 * Tries to stop HTTP servers on the specified paths in the arguments array,
	 * or the current path of the environment if no arguments were given.
	 * <p>
	 * Cases in which a HTTP server will fail to stop:
	 * <ol>
	 * <li>if an additional argument is given but it is neither a path or 'all',
	 * <li>if a path is given, but is not a directory and
	 * <li>if the selected path is directory, but is not a HTTP server.
	 * </ol>
	 * Returns <tt>true</tt> if a HTTP server is successfully stopped,
	 * <tt>false</tt> otherwise.
	 * 
	 * @param args arguments for the stop method, including 'stop' on the
	 *        first position
	 * @param env an environment
	 * @return true if a HTTP server is successfully stopped, false otherwise
	 */
	private boolean stop(String[] args, Environment env) {
		Path path = env.getCurrentPath();
		
		if (args.length == 2) {
			// If this argument is "all", stop all threads and return.
			if ("all".equals(args[1])) {
				serversMap.forEach((serverPath, thread) -> {
					thread.stopThread();
					writeln(env, "Stopped HTTP server at " + serverPath);
				});
				serversMap.clear();
				return true;
			}
			
			// Else this argument should be a path
			path = Helper.resolveAbsolutePath(env, args[1]);
		}
		
		Helper.requireDirectory(path);
		if (!serversMap.containsKey(path)) {
			writeln(env, "There is no HTTP server running for path " + path);
			return false;
		}

		/* Passed all checks, start working. */
		ServerThread thread = serversMap.remove(path);
		thread.stopThread();
		
		writeln(env, "Stopped HTTP server at " + path);
		return true;
	}
	
	/**
	 * Returns <tt>true</tt> if the specified <tt>port</tt> is available for TCP
	 * use. <tt>False</tt> otherwise.
	 * 
	 * @param port port to be checked
	 * @return true if the specified port is available for TCP connection
	 */
	private static boolean available(int port) {
	    try (ServerSocket serverSocket = new ServerSocket(port)) {
	    	return true;
	    } catch (IOException ignored) {
	        return false;
	    }
	}
	
	/**
	 * This class represents a server-running thread. Its {@linkplain #run()}
	 * method is overridden to create a {@code ServerSocket} that
	 * {@linkplain ServerSocket#accept() accepts} connections, creates instances
	 * of {@linkplain ClientWorker} jobs and submits them to the thread pool in
	 * order for the clients to be served.
	 *
	 * @author Mario Bobic
	 */
	private class ServerThread extends Thread {
		
		/** Indicates if the server should be active or not. */
		private boolean active = true;
		
		/** Address which the server listens. */
		private String address;
		/** Port number which the server listens. */
		private int port;
		
		/** Path to the hosted directory. */
		private Path documentRoot;
		
		/**
		 * Constructs an instance of {@code ServerThread} with the net address
		 * and port number.
		 *
		 * @param address address which the server listens
		 * @param port port number which the server listens
		 * @param documentRoot path to a directory to be hosted
		 */
		public ServerThread(String address, int port, Path documentRoot) {
			this.address = address;
			this.port = port;
			this.documentRoot = documentRoot;
			
			this.setName(documentRoot.toString());
		}
		
		@Override
		public void run() {
			try {
				ServerSocket serverSocket = new ServerSocket();
				serverSocket.bind(new InetSocketAddress(address, port));
				serverSocket.setSoTimeout(5000); // timeout for server shutdown
				
				while (active) {
					try {
						acceptClient(serverSocket);
					} catch (SocketTimeoutException e) {
						continue;
					}
				}
				
				serverSocket.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		/**
		 * Accepts the client connection.
		 * <p>
		 * This method <strong>blocks</strong> the thread while waiting for a
		 * client socket, or throws a {@linkplain SocketTimeoutException} if the
		 * specified <tt>serverSocket</tt> has set a timeout.
		 * 
		 * @param serverSocket server socket that accepts clients
		 * @throws SocketTimeoutException if the server socket has timed out
		 * @throws IOException if an I/O or other socket exception occurs
		 */
		private void acceptClient(ServerSocket serverSocket) throws IOException {
			Socket clientSocket = serverSocket.accept();
			ClientWorker cw = new ClientWorker(clientSocket, documentRoot);
			threadPool.submit(cw);
		}
		
		/**
		 * Stops the server.
		 */
		public void stopThread() {
			active = false;
		}
	}
	
	/**
	 * This class is a {@linkplain Runnable runnable} job that serves clients
	 * their requests.
	 *
	 * @author Mario Bobic
	 */
	private class ClientWorker implements Runnable {
		/** The client socket. */
		private Socket csocket;
		/** Input stream from where the request is read. */
		private PushbackInputStream istream;
		/** Output stream for serving the client. */
		private OutputStream ostream;
		
		/** HTTP version. Replaced with client's request version. */
		private String version = "HTTP/1.1";
		/** HTTP method. */
		private String method;
		
		/** Parameters fetched from the request. */
		private Map<String, String> params = new HashMap<>();
		/** Persistent parameters. */
		private Map<String, String> permParams = null;

		/** Path to the hosted directory. */
		private Path documentRoot;

		/**
		 * Constructs an instance of {@code ClientWorker} with the specified
		 * client socket.
		 * 
		 * @param csocket client socket to be used to serve the client
		 * @param documentRoot document root of the server
		 */
		public ClientWorker(Socket csocket, Path documentRoot) {
			this.csocket = csocket;
			this.documentRoot = documentRoot;
		}

		@Override
		public void run() {
			try {
				
				// Obtain input stream and output stream
				istream = new PushbackInputStream(csocket.getInputStream());
				ostream = csocket.getOutputStream();
			
				// Read complete request header from client
				List<String> request = readRequest();
				if (request.isEmpty()) {
					// TODO java.net.SocketException: Software caused connection abort: socket write error
					// an empty request just randomly pops up and causes socket write error
					sendError(400, "Bad request");
					return;
				}
				
				String firstLine = request.get(0);
				
				// Extract method, requestedPath, version from firstLine
				String[] args = firstLine.split(" ");
				if (args.length != 3) {
					sendError(400, "Bad request");
					return;
				}
				
				method = args[0].toUpperCase();
				if (!method.equals("GET")) {
					sendError(405, "Method Not Allowed");
					return;
				}
				
				version = args[2].toUpperCase();
				if (!version.equals("HTTP/1.0") && !version.equals("HTTP/1.1")) {
					sendError(505, "HTTP Version Not Supported");
					return;
				}
				
//				checkSession(request);
				
				
				// Process the requested path
				String requestedPathStr = args[1];
				
				// Extract path and paramString
				String[] pathArgs = requestedPathStr.split("\\?");
				if (pathArgs.length == 2) {
					String paramString = pathArgs[1];
					parseParameters(paramString);
				} else if (pathArgs.length != 1) {
					sendError(400, "Bad request");
					return;
				}
				
				String path = URLDecoder.decode(pathArgs[0], "UTF-8");

				// Resolve path with respect to documentRoot
				Path requestedPath = Paths.get(documentRoot.toString(), path).toAbsolutePath().normalize();
				// If requestedPath is not below documentRoot, return response status 403 forbidden
				if (!requestedPath.startsWith(documentRoot)) {
					sendError(403, "Forbidden");
					return;
				}
				
				System.out.format("%s accessed %s at %s%n",
					csocket, requestedPath, FORMATTER.format(LocalDateTime.now())
				);
				
				// Create context
				RequestContext rc = new RequestContext(ostream, params, permParams, null);
				
				// Check if requestedPath exists and is not hidden
				if (!Files.exists(requestedPath) || Helper.isHidden(requestedPath)) {
					sendError(404, "Not found");
					return;
				}
				
				// Check if requestedPath is accessible
				if (!Files.isReadable(requestedPath) || isExcluded(requestedPath)) {
					sendError(403, "Forbidden");
					return;
				}
				
				// If requestedPath is a directory, write its contents
				if (Files.isDirectory(requestedPath)) {
					String ls = lsHtml(requestedPath, true);
					
					rc.setMimeType("text/html");
					rc.setContentLength(ls.length());
					rc.write(ls);
					return;
				}
				
				// Find the appropriate mimeType for current file
				String mimeType = Files.probeContentType(requestedPath);
				// If no mime type found, assume application/octet-stream
				if (mimeType == null) {
					mimeType = "application/octet-stream";
				}
				
				// Set mime-type; set status to 200; later set content-length
				rc.setMimeType(mimeType);
				rc.setStatusCode(200);
				
				// This will generate header and send file bytes to client
				BufferedInputStream in = new BufferedInputStream(Files.newInputStream(requestedPath));
				rc.setContentLength(in.available());
				
				int len = 0;
				byte[] bytes = new byte[1024];
				while ((len = in.read(bytes)) > 0) {
					rc.write(bytes, 0, len);
				}
				in.close();
				
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("\tat " + FORMATTER.format(LocalDateTime.now()));
				throw new RuntimeException(e);
			} finally {
				try { if (!csocket.isClosed()) csocket.close(); } catch (IOException ignorable) {}
			}
		}

		/**
		 * Reads request from the client scanning for next lines until an empty
		 * line. Returns lines that were read as a list of strings.
		 * 
		 * @return list of strings containing lines that were read
		 */
		private List<String> readRequest() {
			List<String> lines = new ArrayList<>();
			
			@SuppressWarnings("resource")
			Scanner sc = new Scanner(istream, "UTF-8");
			
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.isEmpty()) break;
				lines.add(line);
			}
			
			return lines;
		}
		
		/**
		 * Parses the parameters and puts them into the <tt>params</tt> map.
		 * 
		 * @param paramString string of parameters
		 * @throws RuntimeException if an invalid parameter is given
		 */
		private void parseParameters(String paramString) {
			String[] parameters = paramString.split("\\&");
			
			for (String parameter : parameters) {
				String[] keyValue = parameter.split("=");
				if (keyValue.length == 1) {
					params.put(keyValue[0], null);
				} else {
					params.put(keyValue[0], keyValue[1]);
				}
			}
		}
		
		/**
		 * Sends an error with the specified <tt>statusCode</tt> and
		 * <tt>statusText</tt> to the client output stream.
		 * 
		 * @param statusCode error status code
		 * @param statusText error status text
		 * @throws IOException if an I/O error occurs
		 */
		private void sendError(int statusCode, String statusText) throws IOException {
			ostream.write(
				(version+" "+statusCode+" "+statusText+"\r\n"+
				"Server: MyShell\r\n"+
				"Content-Type: text/html; charset=UTF-8\r\n"+
				"Connection: close\r\n"+
				"\r\n").getBytes(StandardCharsets.US_ASCII)
			);
			
			// Write message to user
			ostream.write(
				("<html>\r\n"+
				 "  <head><title>"+statusCode+" "+statusText+"</title></head>\r\n"+
				 "  <body bgcolor=\"#cc9999\">\r\n"+
				 "    <p align='center'>\r\n"+
				 "      <font size='16'><b>"+statusCode+"</b> "+statusText+"</b></font>\r\n"+
				 "    </p>\r\n"+
				 "    <hr/>\r\n"+
				 "  </body>\r\n"+
				 "</html>\r\n").getBytes(StandardCharsets.UTF_8)
			);
			
			ostream.flush();
		}
		
		/**
		 * Defines the CSS styling of a contents HTML page.
		 */
		private static final String CSS =
			"<style>\r\n"+
			"  body   {background-color: #e6e6fa;}\r\n"+
			"  .title {font-size: 1.5em; font-weight: bold; text-align: center}\r\n"+
			"  a      {text-decoration: none;}\r\n"+
			"</style>";
		
		/**
		 * Returns a HTML representation of the specified directory <tt>dir</tt>
		 * the specified environment <tt>env</tt>.
		 * <p>
		 * Each path inside the directory is written according to the rules of the
		 * {@link LsCommand#getFileString(Path, boolean)} method.
		 * 
		 * @param dir directory whose string representation is to be returned
		 * @param humanReadable if file sizes should be in human readable byte count
		 * @return a HTML representation of the specified directory <tt>dir</tt>
		 * @throws IllegalArgumentException if <tt>dir</tt> is not a directory
		 * @throws IOException if an I/O error occurs when reading the path
		 */
		public String lsHtml(Path dir, boolean humanReadable) throws IOException {
			if (!Files.isDirectory(dir)) {
				throw new IllegalArgumentException("Path must be a directory: " + dir);
			}
			
			// List files from this directory and include 'back' if possible
			List<Path> children = Files.list(dir).collect(Collectors.toList());
			Path back = dir.resolve(".."); // this stays as ".."
			if (!dir.equals(documentRoot)) {
				children.add(0, back);
			}
			
			// TODO diacritics break the HTML?
			StringBuilder sb = new StringBuilder("\r\n");
			for (Path child : children) {
				if (Helper.isHidden(child) || isExcluded(child)) continue;
				
				String url = encodePath(documentRoot.relativize(child).normalize());
				String str = LsCommand.getFileString(child, humanReadable);
				sb	.append("<a href='/").append(url).append("'>")
					.append(str).append("</a>").append("\r\n");
			}
			
			Path root = Helper.getFileName(dir);
			return (
			 "<html>\r\n"+
			 "  <head><title>Contents of "+root+"</title>"+CSS+"</head>\r\n"+
			 "  <body>\r\n"+
			 "    <p class='title'>Contents of directory "+root+"</p>\r\n"+
			 "    <hr/>\r\n"+
			 "    <pre>"+sb+"</pre>\r\n"+
			 "  </body>\r\n"+
			 "</html>\r\n");
		}
		
		/**
		 * Translates the specified path into application/x-www-form-urlencoded
		 * format using the <tt>UTF-8</tt> encoding. For the specified
		 * <tt>path</tt>, every segment of the path is encoded with the help of
		 * the {@link URLEncoder#encode(String, String)} method.
		 * <p>
		 * Independently of the platform, the path separator is set to <tt>/</tt>
		 * to be properly displayed as a URL.
		 * 
		 * @param path path to be encoded with a URL encoder
		 * @return the encoded string
		 * @throws IOException if an I/O error occurs
		 */
		private String encodePath(Path path) throws IOException {
			StringJoiner sj = new StringJoiner("/");
			
			for (Path segment : path) {
				sj.add(URLEncoder.encode(segment.toString(), "UTF-8"));
			}
			
			return sj.toString();
		}
	}

}
