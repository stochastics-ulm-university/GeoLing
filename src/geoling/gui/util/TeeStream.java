package geoling.gui.util;

import java.io.PrintStream;

/**
 * A simple extension of <code>PrintStream</code> to duplicate
 * all written data to a second stream.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class TeeStream extends PrintStream {
	
	/** The second stream. */
	private PrintStream secondaryOut;
	
	/**
	 * Constructs a new <code>PrintStream</code> object that duplicates
	 * all written data by writing to two streams at once.
	 * 
	 * @param out1  the first stream to write to
	 * @param out2  the second stream to write to
	 */
	public TeeStream(PrintStream out1, PrintStream out2) {
		super(out1);
		this.secondaryOut = out2;
	}
	
	@Override
	public void write(byte buf[], int off, int len) {
		super.write(buf, off, len);
		secondaryOut.write(buf, off, len);
	}
	
	@Override
	public void flush() {
		super.flush();
		secondaryOut.flush();
	}
	
}