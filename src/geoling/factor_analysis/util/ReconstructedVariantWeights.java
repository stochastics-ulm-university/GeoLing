package geoling.factor_analysis.util;

import geoling.maps.weights.VariantWeights;
import geoling.models.Location;
import geoling.models.Map;
import geoling.models.Variant;

import java.util.HashMap;

/**
 * Object used to handle reconstructed maps from a factor analysis.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class ReconstructedVariantWeights extends VariantWeights {
	
	/** The scale value used to convert the weights to integers. */
	private static int SCALE_FACTOR = 1000;
	
	/**
	 * Constructs a new variant weights object for reconstructed maps,
	 * which has to be filled with <code>putInformation</code>.
	 * 
	 * @param map  the map the weights should be computed for
	 */
	public ReconstructedVariantWeights(Map map) {
		super(map, false);
	}
	
	/**
	 * Returns an identification string for the weights computation.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		return "factor_analysis_reconstruction";
	}
	
	/**
	 * Updates the information for a variant at the given location.
	 * 
	 * @param location the location
	 * @param variant  the variant
	 * @param weight   the weight of the variant at the location
	 */
	public void putInformation(Location location, Variant variant, Double weight) {
		// weight may be negative due to reconstruction with only
		// some factors, how should we handle this case?
		// => for now, just make sure it's in [0, 1]
		if (weight.doubleValue() < 0.0) weight = 0.0;
		if (weight.doubleValue() > 1.0) weight = 1.0;
		
		HashMap<Variant, Integer> scaledWeights = variantCounter.get(location);
		if (scaledWeights == null) {
			scaledWeights = new HashMap<Variant, Integer>();
			variantCounter.put(location, scaledWeights);
		}
		
		Integer oldWeight = scaledWeights.get(variant);
		if (oldWeight != null) {
			throw new RuntimeException("This should not happen: existing weight for this variant and location.");
		}
		Integer newWeight = (int)Math.round(SCALE_FACTOR * weight);
		scaledWeights.put(variant, newWeight);
		
		Integer oldTotal = totalCounter.get(location);
		if (oldTotal == null) {
			oldTotal = 0;
		}
		totalCounter.put(location, oldTotal + newWeight);
	}
	
}
