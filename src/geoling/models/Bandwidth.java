package geoling.models;

import geoling.maps.density.bandwidth.BandwidthEstimator;
import geoling.maps.weights.VariantWeights;

import java.math.BigDecimal;

/**
 * A bandwidth object holds a precomputed bandwidth for a map.
 * The identification attributes are used to distinguish the different
 * possibilities to estimate an optimal bandwidth (choice of weights
 * computation, kernel, distance measure and the estimation method itself).
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Bandwidth extends ExtendedModel {
	
	static {
		validatePresenceOf("map_id", "weights_identification", "kernel_identification", "distance_identification", "estimator_identification");
		validateNumericalityOf("bandwidth").greaterThan(0);
	}
	
	/**
	 * Returns the bandwidth object corresponding to the given identification
	 * strings of the weights, kernel, distance measure and estimator type to
	 * use.
	 * 
	 * @param map            the map for which we want to find a suitable bandwidth
	 * @param weightsIdStr   the identification string of the weights
	 * @param kernelIdStr    the identification string of the kernel
	 * @param distanceIdStr  the identification string of the distance measure
	 * @param estimatorIdStr the identification string of the bandwidth estimator type
	 * @return the bandwidth object or <code>null</code> if not available
	 */
	public static Bandwidth findByIdentificationStr(Map map, String weightsIdStr, String kernelIdStr, String distanceIdStr, String estimatorIdStr) {
		return (Bandwidth)Bandwidth.findFirst("map_id = ? AND weights_identification = ? AND kernel_identification = ? AND " +
		                                      "distance_identification = ? AND estimator_identification = ?", 
		                                      map.getId(), weightsIdStr, kernelIdStr, distanceIdStr, estimatorIdStr);
	}
	
	/**
	 * Returns the bandwidth (as a number) corresponding to the given identification
	 * strings of the weights, kernel, distance measure and estimator type to
	 * use.
	 * 
	 * @param map            the map for which we want to find a suitable bandwidth
	 * @param weightsIdStr   the identification string of the weights
	 * @param kernelIdStr    the identification string of the kernel
	 * @param distanceIdStr  the identification string of the distance measure
	 * @param estimatorIdStr the identification string of the bandwidth estimator type
	 * @return the bandwidth as a <code>BigDecimal</code> or <code>null</code> if not available
	 */
	public static BigDecimal findNumberByIdentificationStr(Map map, String weightsIdStr, String kernelIdStr, String distanceIdStr, String estimatorIdStr) {
		Bandwidth bandwidth = findByIdentificationStr(map, weightsIdStr, kernelIdStr, distanceIdStr, estimatorIdStr);
		if (bandwidth == null) {
			return null;
		} else {
			return bandwidth.getBigDecimal("bandwidth");
		}
	}
	
	/**
	 * Returns the bandwidth object corresponding to the given identification
	 * objects of the weights, kernel, distance measure and estimator type to
	 * use.
	 * 
	 * @param weights    the variant weights object of the map
	 * @param estimator  the bandwidth estimator object
	 * @return the bandwidth object or <code>null</code> if not available
	 */
	public static Bandwidth findByIdentificationObj(VariantWeights weights, BandwidthEstimator estimator) {
		return findByIdentificationStr(weights.getMap(), weights.getIdentificationString(),
		                               estimator.getKernel().getIdentificationStringWithoutParameters(),
		                               estimator.getDistanceMeasure().getIdentificationString(),
		                               estimator.getIdentificationStringWithoutParameters());
	}
	
	/**
	 * Returns the bandwidth (as a number) corresponding to the given identification
	 * objects of the weights, kernel, distance measure and estimator type to
	 * use.
	 * 
	 * @param weights    the variant weights object of the map
	 * @param estimator  the bandwidth estimator object
	 * @return the bandwidth as a <code>BigDecimal</code> or <code>null</code> if not available
	 */
	public static BigDecimal findNumberByIdentificationObj(VariantWeights weights, BandwidthEstimator estimator) {
		Bandwidth bandwidth = findByIdentificationObj(weights, estimator);
		if (bandwidth == null) {
			return null;
		} else {
			return bandwidth.getBigDecimal("bandwidth");
		}
	}
	
}