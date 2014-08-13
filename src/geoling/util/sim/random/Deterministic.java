package geoling.util.sim.random;

/**
 * This class implements a deterministically distributed random variable
 * whose realisations have all the same value.
 *
 * @author  Institute of Stochastics, Ulm University
 * @version 1.0, 2001-08-22
 */
public class Deterministic implements RandomVariable {
	
	/** The value of all realisations of this random variable.*/
	private double value;
	
	/**
	 * Constructs a new deterministic random variable whose
	 * realisations are all equal to <code>value</code>.
	 *
	 * @param	value	the value of all realisations.
	 */
	public Deterministic(double value) {
		this.value = value;
	}
	
	/**
	 * Returns a new realisation of this random variable.
	 *
	 * @return	the deterministic value with which this object has been constructed.
	 */
	public double realise() {
		return value;
	}
	
	/**
	 * Returns the smallest possible value which is equal to the
	 * deterministic value of this random variable.
	 *
	 * @return	the deterministic value of this random variable.
	 */
	public double getInfimum() {
		return value;
	}
	
	/**
	 * Returns the biggest possible value which is equal to the
	 * deterministic value of this random variable.
	 *
	 * @return	the deterministic value of this random variable.
	 */
	public double getSupremum() {
		return value;
	}
	
	@Override
	public String toString() {
		return "Deterministic random variable with value "+value;
	}
	
}