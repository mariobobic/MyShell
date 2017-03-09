package hr.fer.zemris.java.shell.utility;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import hr.fer.zemris.java.shell.commands.AbstractCommand;
import hr.fer.zemris.java.shell.interfaces.Environment;

/**
 * This class is used for tracking progress and printing it to an environment
 * writer. The environment must be given to a constructor, along with the
 * <strong>total length</strong> to be processed.
 * <p>
 * There is an option for an object of this class to start tracking progress as
 * soon as it is constructed. It also stops progress tracking upon completion,
 * or in other words <em>when the <tt>current</tt> processed length reaches the
 * <tt>total</tt> processed length</em>. This option can be set through the
 * {@link #Progress(Environment, long, boolean)} constructor by setting the
 * <tt>auto</tt> flag to true.
 * <p>
 * The output of this class is a formatted string, showing the percentage of
 * completion and other statistics like elapsed time, current speed and
 * estimated time of completion.
 *
 * @author Mario Bobic
 */
public class Progress implements Runnable {
	
	/** Scheduled executor for running tasks. */
	private ScheduledExecutorService scheduledExecutor =
		Executors.newSingleThreadScheduledExecutor();
	
	/** Time this job was constructed. */
	private final long startTime = System.nanoTime();
	
	/** An environment. */
	private Environment environment;
	/** Total length to be processed. */
	private final long total;
	/** Currently processed length. */
	private long current;
	/** Total size to be processed converted to a human readable string. */
	private final String totalStr;

	/** Recently elapsed time. */
	private long recentStartTime = startTime;
	/** Recently processed length. */
	private long recent;
	
	/** Indicates if the progress tracker has been started. */
	private boolean started;
	/** Indicates if start and stop should be called automatically. */
	private boolean auto;
	
	/**
	 * Constructs an instance of {@code Progress} with the specified arguments.
	 *
	 * @param env an environment
	 * @param total total size to be processed
	 */
	public Progress(Environment env, long total) {
		this(env, total, false);
	}
	
	/**
	 * Constructs an instance of {@code Progress} with the specified arguments.
	 * <p>
	 * If <tt>auto</tt> is set to <strong>true</strong>, the {@link #start()}
	 * method is called automatically upon object construction and
	 * {@link #stop()} when the processing finishes.
	 *
	 * @param env an environment
	 * @param total total size to be processed
	 * @param auto indicates if start and stop should be called automatically
	 */
	public Progress(Environment env, long total, boolean auto) {
		this.environment = Objects.requireNonNull(env);
		this.total = total;
		this.auto = auto;
		
		totalStr = Helper.humanReadableByteCount(total);
		
		if (auto) {
			start();
		}
	}

	/**
	 * Creates and executes a periodic action that becomes enabled first after a
	 * second, and subsequently with 5 seconds between the termination of one
	 * printing execution and the commencement of the next. The task can be
	 * terminated by calling the {@link #stop()} method.
	 */
	public void start() {
		if (!started) {
			started = true;
			scheduledExecutor.scheduleWithFixedDelay(this, 1, 5, TimeUnit.SECONDS);
		}
	}
	
	/**
	 * Initiates an orderly shutdown in which the progress tracker is stopped.
	 * Invocation has no additional effect if already stopped.
	 */
	public void stop() {
		if (started) {
			scheduledExecutor.shutdown();
			started = false;
		}
	}
	
	/**
	 * Adds the specified <tt>amount</tt> to the currently processed length.
	 * <p>
	 * If <tt>auto</tt> was specified and this call results in current reaching
	 * the total length, progress tracking will automatically be stopped.
	 * 
	 * @param amount amount to be added to the current progress
	 */
	public void add(long amount) {
		current += amount;
		recent += amount;
		
		if (auto && current >= total) {
			stop();
		}
	}

	/**
	 * Returns the total length to be processed.
	 * 
	 * @return the total length to be processed
	 */
	public long getTotal() {
		return total;
	}

	/**
	 * Returns the currently processed length.
	 * 
	 * @return the currently processed length
	 */
	public long getCurrent() {
		return current;
	}

	/**
	 * Sets the currently processed length to the specified value.
	 * 
	 * @param current value to be set
	 */
	public void setCurrent(long current) {
		this.current = current;
	}
	
	@Override
	@SuppressWarnings("unused")
	public void run() {
		try {
			/* Percentage */
			int percent = (int) (100 * current / total);
			String currentStr = Helper.humanReadableByteCount(current);

			/* Time */
			long elapsedTime = System.nanoTime() - startTime;
			long recentElapsedTime = System.nanoTime() - recentStartTime;
			String elapsedTimeStr = Helper.humanReadableTimeUnit(elapsedTime);

			/* Speed */
			long averageSpeed = current / (elapsedTime/1_000_000_000L);
			long currentSpeed = recent / (recentElapsedTime/1_000_000_000L);
			String currentSpeedStr = Helper.humanReadableByteCount(currentSpeed) + "/s";

			/* Estimation */
			String estimatedTime = currentSpeed > 0 ?
				Helper.humanReadableTimeUnit(1_000_000_000L * ((total - current) / currentSpeed)) : "∞";
			
			
			/* Print it! */
			AbstractCommand.formatln(environment,
				"%2d%% processed (%s/%s), Elapsed time: %s, Current speed: %s, Estimated time: %s",
				percent, currentStr, totalStr, elapsedTimeStr, currentSpeedStr, estimatedTime
			);
			
			/* Reset 'recent' calculations. */
			recent = 0;
			recentStartTime = System.nanoTime();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
