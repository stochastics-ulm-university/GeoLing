package geoling.util.sim.random;

/**
 * The general interface for an random number generator.
 * 
 * @author Institute of Stochastics, Ulm University
 * @version 1.0, 2001-08-22
 */
public interface Generator {
	
	/**
	 * Sets the seed of the random number generator. This method is optionally.
	 * 
	 * @param seed
	 *            the seed for the random number generator.
	 * @throws UnsupportedOperationException
	 *             if this method is not supported.
	 */
	public void setSeed(long seed);
	
	/**
	 * Returns the next random integer, i.e. the next 32 random bits.
	 * 
	 * @return the next 32 random bits (= integer) generated.
	 */
	public int nextInt();
	
}