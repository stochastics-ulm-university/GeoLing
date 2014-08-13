package geoling.maps.density;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.weights.VariantWeights;
import geoling.models.Location;
import geoling.models.Variant;
import geoling.util.LatLong;

/**
 * Direct pass-through of the weight of a variant.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class WeightPassthrough implements DensityEstimation {
	
	/**
	 * Returns an identification string for this kernel.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		return getStaticIdentificationString();
	}
	
	/**
	 * Returns an identification string for this kernel.
	 * 
	 * @return the identification string
	 */
	public static String getStaticIdentificationString() {
		return "weight_passthrough";
	}
	
	/**
	 * Returns the weight of the given variant at the location nearest to the
	 * given geographic coordinates.
	 * 
	 * @param variantWeights  the weights of the variants at all locations
	 * @param variant         the variant
	 * @param latLong         the geographic coordinates
	 * @return the density value
	 */
	public double estimate(VariantWeights variantWeights, Variant variant, LatLong latLong) {
		return estimate(variantWeights, variant, variantWeights.getLocationGrid().findNearestLocation(latLong));
	}
	
	/**
	 * Returns the weight of the given variant at the given location.
	 * 
	 * @param variantWeights  the weights of the variants at all locations
	 * @param variant         the variant
	 * @param location        the location
	 * @return the weight
	 */
	public double estimate(VariantWeights variantWeights, Variant variant, Location location) {
		return variantWeights.getWeight(variant, location);
	}
	
	/**
	 * Returns the weight of the given variant at the given (aggregated) location.
	 * 
	 * @param variantWeights     the weights of the variants at all locations
	 * @param variant            the variant
	 * @param aggregatedLocation the (aggregated) location
	 * @return the weight
	 */
	public double estimate(VariantWeights variantWeights, Variant variant, AggregatedLocation aggregatedLocation) {
		int occurences = 0;
		int total      = 0;
		for (Location location : aggregatedLocation.getLocations()) {
			occurences += variantWeights.getNumberOfVariantOccurencesAtLocation(variant, location);
			total      += variantWeights.getTotalNumberOfVariantOccurencesAtLocation(location);
		}
		if (total == 0) {
			return 0.0;
		} else {
			return (double)occurences / total;
		}
	}
	
}