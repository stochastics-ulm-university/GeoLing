package geoling.util.sim.random;

/**
 * This is a general interface for double valued random variables.
 * 
 * @author Institute of Stochastics, Ulm University
 * @version 1.0, 2001-08-22
 */
public interface RandomVariable {
	
	/**
	 * Returns the next realisation of the random variable according to
	 * the distribution of the random variable.
	 * 
	 * @return the next random double value according to the distribution.
	 */
	public double realise();
	
	/**
	 * Returns the smallest possible value for this random variable.
	 * If there is no such value, <code>Double.NEGATIVE_INFINITY</code> is
	 * returned.
	 * 
	 * @return the smallest possible value of the random variable
	 *         or <code>Double.NEGATIVE_INFINITY</code>.
	 */
	public double getInfimum();
	
	/**
	 * Returns the biggest possible value for this random variable.
	 * If there is no such value, <code>Double.POSITIVE_INFINITY</code> is
	 * returned.
	 * 
	 * @return the biggest possible value of the random variable
	 *         or <code>Double.POSITIVE_INFINITY</code>.
	 */
	public double getSupremum();
	
}