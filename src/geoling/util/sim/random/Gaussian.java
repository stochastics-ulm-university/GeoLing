package geoling.util.sim.random;

/**
 * This class implements a Gaussianly distributed random variable.
 *
 * @author  Institute of Stochastics, Ulm University
 * @version 1.1, 05.08.2009
 */
public class Gaussian implements RandomVariable {
	/** The random number generator used for realisation. */
	private static Random g = new Random();
	/** Is it a standard Gaussian random variable? I.e. mean = 0.0, stddev = 1.0. */
	private boolean standard;
	/** The mean value and standard deviatation of the random variable. */
	private double mean, stdDeviatation;
	
	/**
	 * Constructs a standard Gaussian random variable, i.e. with mean value 0.0
	 * and standard deviatation 1.0.
	 */
	public Gaussian() {
		standard = true;
		mean = 0.0;
		stdDeviatation = 1.0;
	}
	
	/**
	 * Constructs a Gaussian random variable with the given mean value and
	 * variance.
	 *
	 * @param    mean    the mean value of the random variable.
	 * @param    variance    the variance of the random variable.
	 * @throws   IllegalArgumentException
	 *           if the <code>variance</code> is less than zero.
	 */
	public Gaussian(double mean, double variance) {
		if (!(variance >= 0.0))
			throw new IllegalArgumentException("Normal: variance" +
					" must be at least zero");
		
		if (mean == 0.0 && variance == 1.0) {
			standard = true;
			this.mean = 0.0;
			this.stdDeviatation = 1.0;
		}
		else {
			standard = false;
			this.mean = mean;
			this.stdDeviatation = Math.sqrt(variance);
		}
	}
	
	/**
	 * Returns a new realisation of this random variable.
	 *
	 * @return   a new realisation of this random variable.
	 */
	public double realise() {
		double value;
		
		value = g.nextGaussian();
		
		if (! standard)
			value = value * stdDeviatation + mean;
		
		return value;
	}
	
	/**
	 * Returns always <code>Double.NEGATIVE_INFINITY</code>, the smallest
	 * possible value of this random variable.
	 *
	 * @return   Double.NEGATIVE_INFINITY
	 */
	public double getInfimum() {
		return Double.NEGATIVE_INFINITY;
	}
	
	/**
	 * Returns always <code>Double.POSITIVE_INFINITY</code>, the biggest
	 * possible value of this random variable.
	 *
	 * @return   Double.POSITIVE_INFINITY
	 */
	public double getSupremum() {
		return Double.POSITIVE_INFINITY;
	}
	
	@Override
	public String toString() {
		return "Gaussian random variable with mean="+mean+", stddev="+stdDeviatation;
	}
	
}
