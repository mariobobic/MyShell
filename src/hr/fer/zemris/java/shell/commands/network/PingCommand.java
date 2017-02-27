package hr.fer.zemris.java.shell.commands.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import hr.fer.zemris.java.shell.CommandStatus;
import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;
import hr.fer.zemris.java.shell.utility.FlagDescription;
import hr.fer.zemris.java.shell.utility.SyntaxException;

/**
 * This command sends ICMP echo request packets to network hosts to get an ICMP
 * echo reply from a host or gateway.
 *
 * @author Mario Bobic
 */
public class PingCommand extends AbstractCommand {

	/** Amount of ping calls that should be executed. */
	private static final int DEFAULT_COUNT = 4;
	/** Default time to live. */
	private static final int DEFAULT_TTL = 0;
	/** Maximum amount of time, in milliseconds, the try should take. */
	private static final int DEFAULT_TIMEOUT = 5000;
	
	
	/* Flags */
	/** Number of echo requests to send. */
	private int count;
	/** Timeout, in milliseconds, to wait for each reply. */
	private int timeout;
	/** Time to live (maximum number of hops to try). */
	private int ttl;
	
	/**
	 * Constructs a new command object of type {@code PingCommand}.
	 */
	public PingCommand() {
		super("PING", createCommandDescription(), createFlagDescriptions());
		commandArguments.addFlagDefinition("n", "count", true);
		commandArguments.addFlagDefinition("t", "timeout", true);
		commandArguments.addFlagDefinition("T", "ttl", true);
	}
	
	@Override
	protected String getCommandSyntax() {
		return "<address>";
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
		desc.add("Sends ICMP echo request packets to network hosts.");
		desc.add("Uses the ICMP protocol's mandatory echo request datagram "
				+ "to elicit an ICMP echo response from a host or gateway.");
		return desc;
	}
	
	/**
	 * Creates a list of {@code FlagDescription} objects where each entry
	 * describes the available flags of this command. This method is generates
	 * description exclusively for the command that this class represents.
	 * 
	 * @return a list of strings that represents description
	 */
	private static List<FlagDescription> createFlagDescriptions() {
		List<FlagDescription> desc = new ArrayList<>();
		desc.add(new FlagDescription("n", "count", "count", "Number of echo requests to send."));
		desc.add(new FlagDescription("t", "timeout", "timeout", "Timeout in milliseconds to wait for each reply."));
		desc.add(new FlagDescription("T", "ttl", "ttl", "Time to live."));
		return desc;
	}
	
	@Override
	protected String compileFlags(Environment env, String s) {
		/* Initialize default values. */
		count = DEFAULT_COUNT;
		timeout = DEFAULT_TIMEOUT;
		ttl = DEFAULT_TTL;

		/* Compile! */
		s = commandArguments.compile(s);
		
		/* Replace default values with flag values, if any. */
		if (commandArguments.containsFlag("n", "count")) {
			count = commandArguments.getFlag("n", "count").getIntArgument();
		}
		
		if (commandArguments.containsFlag("t", "timeout")) {
			timeout = commandArguments.getFlag("t", "timeout").getIntArgument();
		}
		
		if (commandArguments.containsFlag("T", "ttl")) {
			ttl = commandArguments.getFlag("T", "ttl").getIntArgument();
		}

		return super.compileFlags(env, s);
	}

	@Override
	protected CommandStatus execute0(Environment env, String s) throws IOException {
		if (s == null) {
			throw new SyntaxException();
		}

		InetAddress inet;
		try {
			inet = InetAddress.getByName(s);
		} catch (UnknownHostException e) {
			formatln(env, "Ping request could not find host %s. Please check the name and try again.", s);
			return CommandStatus.CONTINUE;
		}

		formatln(env, "Pinging %s with ?? bytes of data:", inet.getHostAddress());

		int sent = 0;
		int received = 0;

		long min = Integer.MAX_VALUE;
		long max = Integer.MIN_VALUE;
		long totalTime = 0;

		for (int i = 1; i <= count; i++) {
			try {
				sent++;
				long nanoseconds = ping(inet, ttl, timeout);
				received++;
				min = Math.min(min, nanoseconds);
				max = Math.max(max, nanoseconds);
				totalTime += nanoseconds;
				
				// TODO bytes and TTL ?
				formatln(env, "  %d. Reply from %s: bytes=??, time=%dms, TTL=??",
					i, inet.getHostAddress(), nanoseconds/1_000_000);
			} catch (IOException e) {
				formatln(env, "  %d. No reply from %s.", i, inet.getHostAddress());
			}
			
			try {
				if (i < DEFAULT_COUNT)
					Thread.sleep(1000);
			} catch (InterruptedException ignorable) {}
		}
		
		int lost = sent - received;
		writeln(env, "");
		formatln(env, "Ping statistics for %s:", inet.getHostAddress());
		formatln(env, "  Packets: Sent = %d, Received = %d, Lost = %d (%d%% loss)",
			sent, received, lost, 100*lost/sent);
		formatln(env, "  Round trip times: Min = %dms, Max = %dms, Avg = %dms",
			min/1_000_000, max/1_000_000, (totalTime / received)/1_000_000);

		return CommandStatus.CONTINUE;
	}
	
	/**
	 * Test whether the <tt>inet</tt> address is reachable within the specified
	 * <tt>timeout</tt>, measuring the time it takes to get a response.
	 * <p>
	 * If host is reachable, the measured time is returned, else a
	 * {@code SocketException} is thrown.
	 * 
	 * @param inet the network address to ping
	 * @param ttl the maximum number of hops to try or 0 for the default
	 * @param timeout the time, in milliseconds, before the call aborts
	 * @return time it took for the round trip, in nanoseconds
	 * @throws SocketException if the host is unreachable
	 * @throws IOException if a network error occurs
	 * @throws IllegalArgumentException if either {@code timeout} or {@code ttl}
	 *         are negative
	 */
	private static long ping(InetAddress inet, int ttl, int timeout) throws SocketException, IOException {
		long t1 = System.nanoTime();
		
		boolean reachable = inet.isReachable(null, ttl, timeout);
		if (!reachable) {
			throw new SocketException("Host " + inet.getHostAddress() + " is unreachable.");
		}
		
		long t2 = System.nanoTime();
		return t2 - t1;
	}

}
