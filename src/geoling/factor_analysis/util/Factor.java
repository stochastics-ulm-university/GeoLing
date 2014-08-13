package geoling.factor_analysis.util;

import geoling.models.Variant;

import java.text.DecimalFormat;

/**
 * Object used for identification of a single factor as a result of the factor
 * analysis.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @see FactorLoadings
 */
public class Factor extends Variant {
	
	/** The number of the factor. */
	private int factorNumber;
	
	/** Determines whether this is the positive or negative part of the factor. */
	private boolean negative;
	
	/**
	 * Constructs a new object for the identification of a single factor.
	 * 
	 * @param factorNumber       the number of the factor
	 * @param negative           determines whether this is the positive or negative part of
	 *                           the factor
	 * @param explainedVariance  the variance explained by this positive or negative
	 *                           part of the factor
	 */
	public Factor(int factorNumber, boolean negative, double explainedVariance) {
		this.factorNumber = factorNumber;
		this.negative = negative;
		
		DecimalFormat f = new DecimalFormat("0.00%");
		this.set("name", "Factor " + factorNumber + ", " + (negative ? "negative part" : "positive part") + " (expl. var.: " + f.format(explainedVariance) + ")");
	}
	
	/**
	 * Returns a hash code for this object.
	 */
	@Override
	public int hashCode() {
		return factorNumber * 2 + (negative ? 1 : 0);
	}
	
	/**
	 * Implements a check for equality of two objects.
	 * 
	 * @param obj  the other object
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Factor) && (factorNumber == ((Factor) obj).factorNumber) && (this.negative == ((Factor) obj).negative);
	}
	
}