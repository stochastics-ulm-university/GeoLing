package geoling.gui.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;

/**
 * This class implements the interface <code>Thread.UncaughtExceptionHandler</code>.
 * All uncaught exceptions are catched and displayed in a message dialog.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
	
	/** Number of characters of the stack trace to use at most in the error message. */
	public static int MAX_STACK_TRACE_LENGTH = 255;
	
	/**
	 * Catches uncaught exception and show it in a message dialog with the option to continue application.<br>
	 * {@inheritDoc}
	 * @param t the <code>Thread</code>
	 * @param e the <code>Throwable</code>
	 */
	public void uncaughtException(Thread t, Throwable e) {
		// stack trace to stderr
		e.printStackTrace();
		
		// (partial) stack trace for error message
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String stackTrace = sw.toString();
		if (stackTrace.length() > MAX_STACK_TRACE_LENGTH) {
			stackTrace = stackTrace.substring(0, MAX_STACK_TRACE_LENGTH) + "...";
		}
		
		int returnVal = JOptionPane.showConfirmDialog(null, "The following uncaught exception occured:\n" +
		                                              (e.getMessage() != null ? e.getMessage() : e) + "\n\n" + stackTrace +
		                                              "\n\nDo you want to continue the application?\n\n"+
		                                              "Tip: This error message including full stack trace can also be found in the subfolder \"logs\".",
		                                              "Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
		// terminate application
		if (returnVal != JOptionPane.YES_OPTION) {
			System.exit(1);
		}
	}
	
}