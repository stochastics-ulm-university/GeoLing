package geoling.util.sim.random;

/**
 * This class extends the Java class <code>java.util.Random</code>.
 * The advantage of this class is that exactly as much random bits as are
 * needed are consumed (in contrary to <code>java.util.Random</code>).
 * This class also offers the possibility to use an own random number
 * generator which then replaces the one used by <code>java.util.Random</code>.
 * 
 * @author Institute of Stochastics, Ulm University
 * @version 1.0, 2001-08-22
 */
public class Random extends java.util.Random {
	private static final long serialVersionUID = 1L;
	
	/** The own random number generator, if used. */
	private Generator g = null;
	
	/** The random bits which have not yet been consumed. */
	private int savedBits = 0, bitCount = 0;
	
	/**
	 * Constructs an instance of this class.
	 */
	public Random() {
		super();
	}
	
	/**
	 * Constructs an instance of this class with the given seed.
	 * 
	 * @param seed
	 *            the seed for the random number generator.
	 */
	public Random(long seed) {
		super(seed);
	}
	
	/**
	 * Sets the seed of the random number generator to the given value.
	 * 
	 * @param seed
	 *            the seed for the random number generator.
	 */
	synchronized public void setSeed(long seed) {
		super.setSeed(seed);
		
		if (g != null)
			try {
				g.setSeed(seed);
			} catch (UnsupportedOperationException e) {
			}
		
	}
	
	/**
	 * Get next <code>bits</code> bits from the random number generator.
	 * This method is only for internal use by the super class.
	 * 
	 * @param bits
	 *            the number of random bits needed.
	 * @return the random bits generated.
	 */
	synchronized protected int next(int bits) {
		// if there are still enough random bits from the last call
		if (bitCount >= bits) {
			int value = savedBits & (int) ((1L << bits) - 1);
			bitCount -= bits;
			savedBits >>>= bits;
			return value;
		} else {
			int randomInt = (g == null) ? super.next(32) : g.nextInt();
			long value = ((long) randomInt << bitCount) | savedBits;
			bitCount = (32 + bitCount) - bits;
			savedBits = (int) ((value >>> bits) & ((1L << bitCount) - 1));
			return (int) (value & ((1L << bits) - 1));
		}
	}
	
	/**
	 * Sets an other random number generator.
	 * 
	 * @param gen
	 *            the random number generator.
	 */
	synchronized public void setGenerator(Generator gen) {
		if (gen != null) {
			g = gen;
			bitCount = 0;
		}
	}
	
}
