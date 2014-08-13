package geoling.factor_analysis.util;

import geoling.maps.weights.VariantWeights;
import geoling.models.Location;
import geoling.models.Variant;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Object used to handle factor loadings which are calculated by the class
 * <code>FactorAnalysis</code>. This class is compatible to a
 * <code>VariantWeights</code> object (even though it has another meaning),
 * therefore the standard methods to plot a map can be used.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class FactorLoadings extends VariantWeights {
	
	/** The scale value used to convert the loadings to integers. */
	private static int SCALE_FACTOR = 1000;
	
	/**
	 * Constructs a new factor loadings object, which has to be filled
	 * with <code>putVariantCounter</code>.
	 */
	public FactorLoadings() {
		super(null, false);
	}
	
	/**
	 * Returns an identification string.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		return "factor_loadings";
	}
	
	/**
	 * Updates the information for each factor for the given location.
	 * 
	 * @param location  the location
	 * @param loadings  the mapping of factors to their (positive) loadings (negative
	 *                  parts of the loadings are mapped to a second
	 *                  <code>Factor</code> object)
	 */
	public void putVariantCounter(Location location, HashMap<Factor, Double> loadings) {
		HashMap<Variant, Integer> scaledLoadings = new HashMap<Variant, Integer>();
		for (Entry<Factor, Double> entry : loadings.entrySet()) {
			scaledLoadings.put(entry.getKey(), (int) Math.round(SCALE_FACTOR * entry.getValue().doubleValue()));
		}
		variantCounter.put(location, scaledLoadings);
		totalCounter.put(location, SCALE_FACTOR);
	}
	
}
