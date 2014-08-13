package geoling.util.sim.random;

/**
 * This class implements a double valued uniformly distributed random variable.
 *
 * @author  Institute of Stochastics, Ulm University
 * @version 1.0, 2001-08-22
 */
public class Uniform implements RandomVariable {
	
	/** The source for the random numbers. */
	private static Random u = new Random();
	/** Is it a standard uniform distribution? */
	private boolean standard;
	/** Minimal and maximal value and the difference between these two values. */
	private double min, max, width;
	
	/**
	 * Constructs a new standard uniformly distributed random variable.
	 * (That means uniformly distributed between 0.0 and 1.0.)
	 */
	public Uniform() {
		standard = true;
		min = 0.0;
		max = 1.0;
	}
	
	/**
	 * Constructs a new uniformly between <code>minimum</code> and
	 * <code>maximum</code> distributed random variable.
	 *
	 * @param	minimum	the lower bound of the range for the uniform distribution.
	 * @param	maximum	the upper bound of the range for the uniform distribution.
	 * @throws	IllegalArgumentException
	 *			if <code>minimum</code> is greater than <code>maximum</code>.
	 */
	public Uniform(double minimum, double maximum) {
		if (!(minimum <= maximum))
			throw new IllegalArgumentException("Uniform: minimum must not be smaller than maximum");
		
		if (min == 0.0 && max == 1.0) {
			standard = true;
			min = 0.0;
			max = 1.0;
		}
		else {
			standard = false;
			min = minimum;
			max = maximum;
			width = maximum - minimum;
		}
	}
	
	/**
	 * Returns a new realisation of this random variable, i.e. a new
	 * uniformly distributed random value.
	 *
	 * @return	the realisation of the random variable.
	 */
	public double realise() {
		double value = u.nextDouble();
		
		if (! standard) {
			value = min + value * width;
		}
		
		return value;
	}
	
	/**
	 * Returns the lower bound of the range of the uniform distribution.
	 *
	 * @return	the minimal value for this random variable.
	 */
	public double getInfimum() {
		return min;
	}
	
	/**
	 * Returns the upper bound of the range of the uniform distribution.
	 *
	 * @return	the maximal value for this random variable.
	 */
	public double getSupremum() {
		return max;
	}
	
	@Override
	public String toString() {
		return "Uniformly distributed random variable on ["+min+", "+max+"]";
	}
	
}