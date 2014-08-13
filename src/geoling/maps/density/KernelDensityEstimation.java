package geoling.maps.density;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.density.kernels.Kernel;
import geoling.maps.distances.GeographicalDistance;
import geoling.maps.distances.DistanceMeasure.LatLongNotSupportedException;
import geoling.maps.distances.PrecomputedDistance.PrecomputedDistanceNotFoundException;
import geoling.maps.weights.VariantWeights;
import geoling.models.ConfigurationOption;
import geoling.models.Location;
import geoling.models.Variant;
import geoling.util.LatLong;
import geoling.util.Utilities;

/**
 * Kernel density estimation (with fixed bandwidth) for estimation of density values
 * in variant maps.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class KernelDensityEstimation implements DensityEstimation {
	
	/** The number of locations required such that we will use the location grid instead of simple iterations. */
	public static final int LOCATION_COUNT_FOR_GRID = 1000;
	
	/** The kernel that will be used. */
	protected Kernel kernel;
	
	/** Determines whether the (absolute) number of answers at a location is ignored, i.e., whether only the weight itself is relevant. */
	protected boolean ignoreFrequencies;
	
	/**
	 * Constructs the kernel object with the most important parameters.
	 * Note that we require a <code>BigDecimal</code> as a bandwidth, because
	 * we need the precision for identification purposes (i.e., conversion
	 * from/to String has to be unproblematic, no rounding errors allowed).
	 * 
	 * @param kernel            the kernel
	 * @param ignoreFrequencies determines whether the (absolute) number of answers at a
	 *                          location is ignored, i.e., whether only the weight
	 *                          itself is relevant
	 */
	public KernelDensityEstimation(Kernel kernel, boolean ignoreFrequencies) {
		this.kernel = kernel;
		this.ignoreFrequencies = ignoreFrequencies;
	}
	
	/**
	 * Constructs the kernel object with the most important parameters.
	 * Note that we require a <code>BigDecimal</code> as a bandwidth, because
	 * we need the precision for identification purposes (i.e., conversion
	 * from/to String has to be unproblematic, no rounding errors allowed).
	 * 
	 * @param kernel  the kernel
	 */
	public KernelDensityEstimation(Kernel kernel) {
		this(kernel, ConfigurationOption.getOption("ignoreFrequenciesInDensityEstimation", false));
	}
	
	/**
	 * Returns an identification string, includes the kernel identification.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		return getStaticIdentificationString()+":"+this.kernel.getIdentificationString();
	}
	
	/**
	 * Returns an identification string for this density estimation type.
	 * 
	 * @return the identification string
	 */
	public static String getStaticIdentificationString() {
		return "kde";
	}
	
	/**
	 * Returns the kernel.
	 * 
	 * @return the kernel
	 */
	public Kernel getKernel() {
		return this.kernel;
	}
	
	/**
	 * Returns the relevant locations for a density estimation at given coordinates.
	 * 
	 * @param variantWeights  the weights of the variants at all locations
	 * @param latLong         the geographic coordinates
	 * @return the set of relevant locations
	 */
	private Collection<Location> getRelevantLocations(VariantWeights variantWeights, LatLong latLong) {
		return getRelevantLocations(variantWeights, latLong, this.kernel.getMaxRelevantDistanceForEstimation());
	}
	
	/**
	 * Returns the relevant locations for a density estimation at given coordinates.
	 * 
	 * @param variantWeights  the weights of the variants at all locations
	 * @param latLong         the geographic coordinates
	 * @param maxDist         the maximal distance in kilometres of the returned locations
	 *                        to the given coordinates
	 * @return the set of relevant locations
	 */
	private Collection<Location> getRelevantLocations(VariantWeights variantWeights, LatLong latLong, double maxDist) {
		if (maxDist <= 0.0) {
			throw new IllegalArgumentException("maxDist must be positive!");
		}
		Set<Location> allLocations = variantWeights.getLocations();
		
		if ((this.kernel.getDistanceMeasure() instanceof GeographicalDistance) && !Double.isInfinite(maxDist)) {
			// distances are only relevant if correct distance measure and maxDist is not infinite
			
			ArrayList<Location> locations;
			
			if ((allLocations.size() >= LOCATION_COUNT_FOR_GRID)) {
				// use only the grid the number of locations is large
				locations = variantWeights.getLocationGrid().findLocationsInDistance(latLong, maxDist, true);
			} else {
				// not lots of locations, filter them simply by computing distances
				locations = new ArrayList<Location>(allLocations.size());
				for (Location location : allLocations) {
					double distance = this.kernel.getDistanceMeasure().getDistance(location.getLatLong(), latLong);
					if (distance < maxDist+Utilities.EPS) {
						locations.add(location);
					}
				}
			}
			
			if (locations.isEmpty()) {
				// no locations? then use larger ranges to get some data... (may happen when using small bandwidths)
				return getRelevantLocations(variantWeights, latLong, maxDist*2.0);
			} else {
				return locations;
			}
		} else {
			return allLocations;
		}
	}
	
	/**
	 * Estimates the density value of the given variant at the given geographic coordinates.
	 * 
	 * @param variantWeights  the weights of the variants at all locations
	 * @param variant         the variant
	 * @param latLong         the geographic coordinates
	 * @return the density value
	 * @throws LatLongNotSupportedException if the distance measure only supports the distance between locations
	 */
	public double estimate(VariantWeights variantWeights, Variant variant, LatLong latLong) throws LatLongNotSupportedException {
		double result = 0.0;
		double sum = 0.0;
		
		for (Location otherLocation : getRelevantLocations(variantWeights, latLong)) {
			try {
				int total = variantWeights.getTotalNumberOfVariantOccurencesAtLocation(otherLocation);
				if (total == 0) {
					// in this map, no answers were given at the current location, i.e.,
					// we have no information at this location, therefore just ignore it...
					continue;
				}
				
				double distance    = this.kernel.getDistanceMeasure().getDistance(otherLocation.getLatLong(), latLong);
				double kernelValue = this.kernel.evaluateKernel(distance);
				if (this.ignoreFrequencies) {
					result += variantWeights.getWeight(variant, otherLocation) * kernelValue;
					sum    += kernelValue;
				} else {
					result += variantWeights.getNumberOfVariantOccurencesAtLocation(variant, otherLocation) * kernelValue;
					sum    += total * kernelValue;
				}
			} catch (PrecomputedDistanceNotFoundException e) {
				// ignore the location if the distance is unknown (if we have no information
				// about the distance, then handling it as if the other location wasn't even
				// there is probably the best solution...)
			}
		}
		
		if (sum > 0.0) {
			return result/sum;
		} else {
			return 0.0;
		}
	}
	
	/**
	 * Estimates the density value of the given variant at the given location,
	 * supports an additional parameter of a location that should be left out
	 * in the estimation.
	 * 
	 * @param variantWeights  the weights of the variants at all locations
	 * @param variant         the variant
	 * @param location        the location
	 * @param ignoreLocation  a location that should be left out, this parameter
	 *                        may be <code>null</code>
	 * @return the density value
	 */
	public double estimate(VariantWeights variantWeights, Variant variant, Location location, Location ignoreLocation) {
		double result = 0.0;
		double sum = 0.0;
		
		for (Location otherLocation : getRelevantLocations(variantWeights, location.getLatLong())) {
			try {
				if ((ignoreLocation != null) &&
				    (otherLocation.equals(ignoreLocation) || otherLocation.getLatLong().equals(location.getLatLong()))) {
					// for bandwidth estimation we need to ignore one location (and we also ignore locations with
					// the same coordinates)
					continue;
				}
				
				int total = variantWeights.getTotalNumberOfVariantOccurencesAtLocation(otherLocation);
				if (total == 0) {
					// in this map, no answers were given at the current location, i.e.,
					// we have no information at this location, therefore just ignore it...
					continue;
				}
				
				double distance    = this.kernel.getDistanceMeasure().getDistance(otherLocation, location);
				double kernelValue = this.kernel.evaluateKernel(distance);
				if (this.ignoreFrequencies) {
					result += variantWeights.getWeight(variant, otherLocation) * kernelValue;
					sum    += kernelValue;
				} else {
					result += variantWeights.getNumberOfVariantOccurencesAtLocation(variant, otherLocation) * kernelValue;
					sum    += total * kernelValue;
				}
			} catch (PrecomputedDistanceNotFoundException e) {
				// ignore the location if the distance is unknown (if we have no information
				// about the distance, then handling it as if the other location wasn't even
				// there is probably the best solution...)
			}
		}
		
		if (sum > 0.0) {
			return result/sum;
		} else {
			return 0.0;
		}
	}
	
	/**
	 * Estimates the density value of the given variant at the given location.
	 * 
	 * @param variantWeights  the weights of the variants at all locations
	 * @param variant         the variant
	 * @param location        the location
	 * @return the density value
	 */
	public double estimate(VariantWeights variantWeights, Variant variant, Location location) {
		return estimate(variantWeights, variant, location, null);
	}
	
	/**
	 * Estimates the density value of the given variant at the given (aggregated) location.
	 * Note that the densities are estimated for all locations that are aggregated, then
	 * the weighted mean of them will be returned. This behaviour is chosen somewhat
	 * arbitrarily, because it is not clear what would be best.
	 * 
	 * @param variantWeights     the weights of the variants at all locations
	 * @param variant            the variant
	 * @param aggregatedLocation the (aggregated) location
	 * @return the density value
	 */
	public double estimate(VariantWeights variantWeights, Variant variant, AggregatedLocation aggregatedLocation) {
		double sum = 0;
		int total  = 0;
		for (Location location : aggregatedLocation.getLocations()) {
			sum += this.estimate(variantWeights, variant, location);
			total++;
		}
		return sum / total;
	}
	
}