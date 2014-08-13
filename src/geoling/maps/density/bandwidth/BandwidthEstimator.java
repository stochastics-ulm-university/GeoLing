package geoling.maps.density.bandwidth;

import geoling.maps.density.kernels.Kernel;
import geoling.maps.distances.DistanceMeasure;
import geoling.maps.weights.VariantWeights;
import geoling.models.Location;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstract class for determining a bandwidth for kernel density estimation,
 * which is required for variant maps.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public abstract class BandwidthEstimator {
	
	/** The number of automatically constructed bandwidth candidates. */
	public static int CANDIDATES_COUNT = 100;
	
	/** The factor applied to the auto-adjusted maximal distance. */
	public static BigDecimal MAX_DISTANCE_RATIO = BigDecimal.valueOf(1.0);
	
	/** The kernel object. */
	protected Kernel kernel;
	
	/**
	 * Constructor using the kernel that will be used for bandwidth
	 * estimation.
	 * 
	 * @param kernel  the kernel
	 */
	public BandwidthEstimator(Kernel kernel) {
		this.kernel = kernel;
	}
	
	/**
	 * Returns the kernel object.
	 * 
	 * @return the kernel object
	 */
	public Kernel getKernel() {
		return this.kernel;
	}
	
	/**
	 * Returns the distance measure object.
	 * 
	 * @return the distance measure object
	 */
	public DistanceMeasure getDistanceMeasure() {
		return this.kernel.getDistanceMeasure();
	}
	
	/**
	 * Returns an identification string for this kernel, does not include parameters
	 * as e.g. the bandwidth.
	 * 
	 * @return the identification string
	 */
	public abstract String getIdentificationStringWithoutParameters();
	
	/**
	 * Returns an identification string for this bandwidth estimator, includes the parameters
	 * as e.g. the kernel.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		return this.getIdentificationStringWithoutParameters()+":kernel="+this.kernel.getIdentificationStringWithoutBandwidth();
	}
	
	/**
	 * Detects bandwidth candidates for the given variants/weights.
	 * In this method, the maximal distance between two locations is used
	 * to construct a list of bandwidths with an equidistant step size.
	 * The number of steps is given by <code>AUTO_BANDWIDTH_CANDIDATES_COUNT</code>.
	 * Warning: this method is independent of the kernel!  
	 * 
	 * @param variantWeights  the weights for all variants at all locations
	 * @return the list of bandwidth candidates, will be empty if the maximal
	 *         distance between locations is zero
	 */
	public ArrayList<BigDecimal> getBandwidthCandidates(VariantWeights variantWeights) {
		double maxDistance = 0.0;
		ArrayList<Location> locations = new ArrayList<Location>(variantWeights.getLocations());
		for (Location location1 : locations) {
			for (Location location2 : locations) {
				if (location1.getLongId().longValue() < location2.getLongId().longValue()) {
					double distance = this.getDistanceMeasure().getDistance(location1, location2);
					if (distance > maxDistance) {
						maxDistance = distance;
					}
				}
			}
		}
		
		if (maxDistance == 0.0) {
			// detected max distance is zero, bandwidth is irrelevant - return an empty list
			return new ArrayList<BigDecimal>();
		}
		
		BigDecimal maxDistanceObj = BigDecimal.valueOf(maxDistance);
		// scale to one digit before the decimal point
		int scale = maxDistanceObj.precision()-maxDistanceObj.scale()-1;
		maxDistanceObj = maxDistanceObj.scaleByPowerOfTen(-scale);
		// round down
		maxDistanceObj = BigDecimal.valueOf(maxDistanceObj.toBigInteger().longValue());
		// revert scale
		maxDistanceObj = maxDistanceObj.scaleByPowerOfTen(scale);
		// e.g. linguistic distance: use 1.0 as max value to get a step size of 0.01
		if (maxDistanceObj.compareTo(BigDecimal.ONE) < 0) {
			maxDistanceObj = BigDecimal.ONE;
		}
		// apply the ratio defined as a global (usually constant) variable
		maxDistanceObj = maxDistanceObj.multiply(MAX_DISTANCE_RATIO);
		
		return getEquidistantSteps(BigDecimal.ZERO, maxDistanceObj, CANDIDATES_COUNT);
	}
	
	/**
	 * Constructs a list of equidistant steps for a given interval and step count.
	 * 
	 * @param exclusiveMin minimum value (not included in result)
	 * @param max          maximum value
	 * @param count        the number of steps
	 * @return the list with the values
	 */
	public static ArrayList<BigDecimal> getEquidistantSteps(BigDecimal exclusiveMin, BigDecimal max, int count) {
		ArrayList<BigDecimal> list = new ArrayList<BigDecimal>();
		
		BigDecimal widthObj = max.subtract(exclusiveMin);
		BigDecimal countObj = BigDecimal.valueOf(count);
		BigDecimal stepObj = widthObj.divide(countObj).stripTrailingZeros();
		for (int i = 1; i <= count; i++) {
			list.add(exclusiveMin.add(stepObj.multiply(BigDecimal.valueOf(i))).setScale(stepObj.scale(), RoundingMode.HALF_UP));
		}
		
		return list;
	}
	
	/**
	 * Detects a suitable bandwidth from the automatically detected candidates.
	 * 
	 * @param variantWeights  the weights for all variants at all locations
	 * @return the detected bandwidth, <code>null</code> if no best bandwidth could be detected
	 */
	public BigDecimal findBandwidth(VariantWeights variantWeights) {
		return findBandwidth(variantWeights, getBandwidthCandidates(variantWeights));
	}
	
	/**
	 * Detects a suitable bandwidth from the given candidates.
	 * 
	 * @param variantWeights       the weights for all variants at all locations
	 * @param bandwidthCandidates  the bandwidths that should be tested
	 * @return the detected bandwidth, <code>null</code> if no best bandwidth could be detected
	 *         (e.g., if <code>bandwidthCandidates</code> is empty)
	 */
	public abstract BigDecimal findBandwidth(VariantWeights variantWeights, Collection<BigDecimal> bandwidthCandidates);
	
}
