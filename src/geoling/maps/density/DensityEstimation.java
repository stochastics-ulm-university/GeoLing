package geoling.maps.density;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.distances.DistanceMeasure.LatLongNotSupportedException;
import geoling.maps.weights.VariantWeights;
import geoling.models.Location;
import geoling.models.Variant;
import geoling.util.LatLong;

/**
 * Interface for density estimation in variant maps.
 * Note that the density estimation itself does not use aggregated locations. It
 * always works on the complete set of locations, and it only provides a method
 * that returns the (weighted) density of the locations contained in an aggregation.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public interface DensityEstimation {
	
	/**
	 * Returns an identification string for this density estimation object.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString();
	
	/**
	 * Estimates the density value of the given variant at the given geographic coordinates.
	 * <p>
	 * Warning: This method has to be implemented thread-safe!
	 * 
	 * @param variantWeights  the weights of the variants at all locations
	 * @param variant         the variant
	 * @param latLong         the geographic coordinates
	 * @return the density value
	 * @throws LatLongNotSupportedException if the distance measure only supports the distance between locations
	 */
	public double estimate(VariantWeights variantWeights, Variant variant, LatLong latLong) throws LatLongNotSupportedException;
	
	/**
	 * Estimates the density value of the given variant at the given location.
	 * <p>
	 * Warning: This method has to be implemented thread-safe!
	 * 
	 * @param variantWeights  the weights of the variants at all locations
	 * @param variant         the variant
	 * @param location        the location
	 * @return the density value
	 */
	public double estimate(VariantWeights variantWeights, Variant variant, Location location);
	
	/**
	 * Estimates the density value of the given variant at the given (aggregated) location.
	 * <p>
	 * Warning: This method has to be implemented thread-safe!
	 * 
	 * @param variantWeights     the weights of the variants at all locations
	 * @param variant            the variant
	 * @param aggregatedLocation the (aggregated) location
	 * @return the density value
	 */
	public double estimate(VariantWeights variantWeights, Variant variant, AggregatedLocation aggregatedLocation);
	
}