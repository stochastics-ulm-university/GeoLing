package geoling.util;

import java.io.PrintStream;

import javax.swing.ProgressMonitor;

/**
 * Helper class for status messages and progress bars written to a
 * <code>PrintStream</code> object (with dots, by default one dot for 2%), e.g.
 * <code>System.out</code>, but supports also a <code>ProgressMonitor</code>
 * object to visualize the progress in a UI window.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @version 1.0.4, 2013-06-03
 */
public class ProgressOutput {
	
	/** The default total number of steps, <code>100</code> denoting 100%. */
	protected final static int DEFAULT_TOTAL = 100;
	
	/** The number of dots that will be outputted in one line. */
	protected final static int DOTS_PER_LINE = 50;
	
	/** The output stream (may be <code>null</code>, then no output will be generated). */
	protected PrintStream output;
	
	/** The progress monitor (may be <code>null</code>) used to show the current progress in a UI window. */
	protected ProgressMonitor progressMonitor;
	
	/** The current position when using progress outputs. */
	protected int current;
	
	/** The maximum position when using progress outputs. */
	protected int total;
	
	/** The number of lines that should be used for the progress bar that is written with dots. */
	protected int lines;
	
	/** The number of dots already written to the output (a value of <code>-1</code> means no output yet). */
	protected int dots;
	
	/** Determines whether the required time will be shown at 100%. */
	protected boolean stopwatch;
	
	/** The stopwatch object itself that is used to stop the time. */
	protected StopWatch timer;
	
	/**
	 * Constructs the object for writing status messages and/or progress bars,
	 * also supports a <code>ProgressMonitor</code> object to visualize the progress
	 * in a UI window.
	 * 
	 * @param output          the output stream (may be <code>null</code>, then
	 *                        no output will be generated)
	 * @param progressMonitor progress monitor (may be <code>null</code>)
	 * @param lines           the number of lines that should be used for the progress bar
	 * @param stopwatch       determines whether the required time will be shown at 100%
	 * @param total           the maximum position
	 */
	public ProgressOutput(PrintStream output, ProgressMonitor progressMonitor, int lines, boolean stopwatch, int total) {
		if (lines < 1) {
			throw new IllegalArgumentException("Number of lines has to be a positive number!");
		}
		if (total < 1) {
			throw new IllegalArgumentException("Maximum position has to be a positive number!");
		}
		this.output          = output;
		this.progressMonitor = progressMonitor;
		this.timer           = new StopWatch();
		this.lines           = lines;
		reset(stopwatch, total);
	}
	
	/**
	 * Constructs the object for writing status messages and/or progress bars.
	 * 
	 * @param output    the output stream (may be <code>null</code>, then
	 *                  no output will be generated)
	 * @param stopwatch determines whether the required time will be shown at 100%
	 * @param total     the maximum position
	 */
	public ProgressOutput(PrintStream output, boolean stopwatch, int total) {
		this(output, null, 1, stopwatch, total);
	}
	
	/**
	 * Constructs the object for writing status messages and/or progress bars.
	 * 
	 * @param output    the output stream (may be <code>null</code>, then
	 *                  no output will be generated)
	 * @param stopwatch determines whether the required time will be shown at 100%
	 */
	public ProgressOutput(PrintStream output, boolean stopwatch) {
		this(output, stopwatch, DEFAULT_TOTAL);
	}
	
	/**
	 * Constructs the object for writing status messages and/or progress bars.
	 * 
	 * @param output  the output stream (may be <code>null</code>, then
	 *                no output will be generated)
	 * @param total   the maximum position
	 */
	public ProgressOutput(PrintStream output, int total) {
		this(output, true, total);
	}
	
	/**
	 * Constructs the object for writing status messages and/or progress bars.
	 * 
	 * @param output  the output stream (may be <code>null</code>, then
	 *                no output will be generated)
	 */
	public ProgressOutput(PrintStream output) {
		this(output, DEFAULT_TOTAL);
	}
	
	/**
	 * Constructs a dummy object for writing status messages and/or progress bars,
	 * no real output is generated.
	 */
	public ProgressOutput() {
		this((PrintStream)null);
	}
	
	/**
	 * Constructs the object for writing status messages and/or progress bars,
	 * uses the settings of an existing object.
	 * 
	 * @param obj  the existing <code>ProgressOutput</code> object,
	 *             may be <code>null</code> (then defaults are used,
	 *             no output is generated)
	 */
	public ProgressOutput(ProgressOutput obj) {
		this((obj != null) ? obj.output          : null,
		     (obj != null) ? obj.progressMonitor : null,
		     (obj != null) ? obj.lines           : 1,
		     (obj != null) ? obj.stopwatch       : true,
		     (obj != null) ? obj.total           : DEFAULT_TOTAL);
	}
	
	/**
	 * Returns the current position.
	 * 
	 * @return the current position
	 */
	public synchronized int getCurrent() {
		return this.current;
	}
	
	/**
	 * Returns the maximum position.
	 * 
	 * @return the maximum position
	 */
	public synchronized int getTotal() {
		return this.total;
	}

	/**
	 * Returns the current percentage.
	 * 
	 * @return the current percentage (an integer number between 0 and 100).
	 */
	public synchronized int getPercentage() {
		return (int)Math.round(100.0*this.current/this.total);
	}
	
	/**
	 * Writes the given message to the output stream and sets it as a note
	 * for the <code>ProgressMonitor</code>.
	 * 
	 * @param message  the message
	 */
	public synchronized void customMessage(String message) {
		this.customMessage(message, true);
	}
	
	/**
	 * Writes the given message to the output stream and sets it as a note
	 * for the <code>ProgressMonitor</code>, if <code>setNote</code> is set.
	 * 
	 * @param message  the message
	 * @param setNote  determines whether the note is set in the
	 *                 <code>ProgressMonitor</code> object
	 */
	public synchronized void customMessage(String message, boolean setNote) {
		if (this.output != null) {
			this.output.println(message);
		}
		if (setNote && (this.progressMonitor != null)) {
			this.progressMonitor.setNote(message);
		}
	}
	
	/**
	 * Writes the given message to the output stream, but without newline.
	 * 
	 * @param message  the message
	 */
	public synchronized void customMessageInline(String message) {
		if (this.output != null) {
			this.output.print(message);
		}
	}
	
	/**
	 * Resets the internal variables for progress messages.
	 * 
	 * @param stopwatch determines whether the required time will be shown at 100%
	 * @param total     the new maximum position
	 */
	public synchronized void reset(boolean stopwatch, int total) {
		this.stopwatch = stopwatch;
		reset(total);
	}
	
	/**
	 * Resets the internal variables for progress messages, but preserves
	 * the <code>stopwatch</code> setting.
	 * 
	 * @param total  the new maximum position
	 */
	public synchronized void reset(int total) {
		this.current = 0;
		this.total   = total;
		this.dots    = -1;
		
		this.timer.reset();
		
		if (this.progressMonitor != null) {
			this.progressMonitor.setProgress(0);
			this.progressMonitor.setMinimum(0);
			this.progressMonitor.setMaximum(total);
		}
	}
	
	/**
	 * Sets the current position and generates/updates a progress bar.
	 * 
	 * @param current  the new position
	 */
	public synchronized void setCurrent(int current) {
		this.current = Math.min(current, this.total);
		if (this.progressMonitor != null) {
			this.progressMonitor.setProgress(current);
		}
		
		int newDots = (int)((double)DOTS_PER_LINE * this.lines * this.current / this.total); // double calculation, because integer overflow possible
		if (this.dots < newDots) {
			while (this.dots < newDots) {
				this.dots++;
				
				if (this.dots == 0) {
					// start progress bar and stopwatch
					this.timer.start();
					this.customMessageInline("[");
				} else {
					this.customMessageInline(".");
					
					// several lines: do we need a line break?
					if ((this.dots % DOTS_PER_LINE == 0) && (this.dots < DOTS_PER_LINE * this.lines)) {
						this.customMessage("  done: "+(int)Math.round(100.0 * this.dots / (DOTS_PER_LINE * this.lines))+"%", false);
						this.customMessageInline(" ");
					}
				}
			};
			
			if (this.current == this.total) {
				// finished: stop timer and output end of progress bar
				this.timer.stop();
				
				this.customMessageInline("]");
				if (this.stopwatch) {
					this.customMessageInline(" time: "+StopWatch.secondsToString(timer.getLastTime()));
				}
				this.customMessage("", false);
			}
		}
	}
	
	/**
	 * Initializes the current position (if not already started).
	 */
	public synchronized void initCurrent() {
		setCurrent(0);
	}
	
	/**
	 * Increments the current position.
	 */
	public synchronized void incrementCurrent() {
		setCurrent(this.current+1);
	}
	
}