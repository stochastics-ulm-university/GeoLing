package geoling.util;

/**
 * This utility class implements a stop watch for profiling
 * purposes.
 *
 * @author Institute of Stochastics, Ulm University
 * @version 1.2, 2013-02-18
 */
public class StopWatch {
	
	/** The system time when the stop watch was started (in milliseconds). */
	private long startTime;
	
	/** The time difference of the last run of the stop watch (in milliseconds). */
	private long lastTime;
	
	/** The total time accumulated so far (in milliseconds). */
	private long totalTime;
	
	/** Indicates whether the stop watch is running or not. */
	private boolean running;
	
	/** Indicates whether there is a result from a previous run of the stop watch. */
	private boolean result;
	
	/**
	 * Creates a new stop watch.
	 */
	public StopWatch() {
		reset();
	}
	
	/**
	 * Starts the stop watch, i.e. the time measurement.
	 */
	public void start() {
		running = true;
		startTime = System.currentTimeMillis();
	}
	
	/**
	 * Stops the stop watch, i.e. the time measurement.
	 *
	 * @throws IllegalStateException if the stop watch is not running
	 */
	public void stop() {
		if (running) {
			lastTime = System.currentTimeMillis() - startTime;
			totalTime += lastTime;
			result = true;
			running = false;
		}
		else
			throw new IllegalStateException("stop() can only be called while stop watch is running");
	}
	
	/**
	 * Returns the stopped time (in seconds) of the last run
	 * of the stop watch.
	 *
	 * @return the stopped time of the last run of the stop watch
	 * @throws IllegalStateException if there is no previous run of this stop watch
	 */
	public double getLastTime() {
		if (result)
			return (double) lastTime / 1000.0;
		else
			throw new IllegalStateException("getLastTime() can only be called if the stopwatch has a result");
	}
	
	/**
	 * Returns the stopped time (in seconds) accumulated so far.
	 *
	 * @return the stopped time accumulated so far
	 */
	public double getTime() {
		return (double) totalTime / 1000.0;
	}
	
	/**
	 * Resets the stop watch such that the accumulated time is 0s
	 * and there is no last stopped time.
	 */
	public void reset() {
		running = false;
		result = false;
		totalTime = 0;
	}
	
	/**
	 * Converts the given number of seconds to a human-readable string.
	 * 
	 * @param seconds  the number of seconds
	 * @return a string like e.g. <code>1 h 5 min 10 sec</code>
	 */
	public static String secondsToString(double seconds) {
		int ms  = (int)((seconds-(int)seconds)*1000);
		int sec = (int)seconds;
		int min = sec / 60;  sec %= 60;
		int h   = min / 60;  min %= 60;
		
		// we won't output milliseconds if the time is at least 60 seconds
		boolean hide_ms = ((int)seconds >= 60);
		if (hide_ms) sec = (int)Math.round(sec + ms/1000.0);
		
		String tmp = "";
		if (h > 0)                             tmp += " "+h+" h";
		if ((h > 0) || (min > 0))              tmp += " "+min+" min";
		if ((h > 0) || (min > 0) || (sec > 0)) tmp += " "+sec+" sec";
		if (!hide_ms)                          tmp += " "+ms+" ms";
		return tmp.substring(1);
	}
	
}