package geoling.maps.density.bandwidth;

import geoling.maps.density.KernelDensityEstimation;
import geoling.maps.density.kernels.Kernel;
import geoling.maps.weights.VariantWeights;
import geoling.models.Location;
import geoling.models.Variant;
import geoling.util.ThreadedTodoWorker;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Likelihood-cross-validation for determining a bandwidth for kernel density estimation.
 * 
 * @author student assistant (based on existing code), Institute of Stochastics, Ulm University
 * @see "Dissertation Jonas Rumpf, Ulm University, Section 3.3.1 and 4.3.2"
 */
public class LikelihoodCrossValidation extends BandwidthEstimator {
	
	/**
	 * Minimum percentage of locations where a variant must occur, otherwise the variant will
	 * be ignored.
	 */
	public static double MIN_OCCURRENCE_AT_LOCATION = 0.1;
	
	/**
	 * Limit of consecutive bandwidths tested where the likelihood decreases strictly, used
	 * to speedup the bandwidth estimation, which is especially expensive for large bandwidths.
	 */
	public static int STRICTLY_DECREASING_BREAK = 20;
	
	/**
	 * Constructor using the kernel that will be used for bandwidth estimation.
	 * 
	 * @param kernel  the kernel
	 */
	public LikelihoodCrossValidation(Kernel kernel) {
		super(kernel);
	}
	
	/**
	 * Detects a suitable bandwidth from the given candidates,
	 * uses the likelihood-cross-validation.
	 * 
	 * @param variantWeights       the weights for all variants at all locations
	 * @param bandwidthCandidates  the bandwidths that should be tested
	 * @return the detected bandwidth, <code>null</code> if no best bandwidth could be detected
	 *         (e.g., if <code>bandwidthCandidates</code> is empty)
	 */
	public BigDecimal findBandwidth(final VariantWeights variantWeights, Collection<BigDecimal> bandwidthCandidates) {
		double maxValue = Double.NEGATIVE_INFINITY;
		double prevValue = Double.NEGATIVE_INFINITY;
		BigDecimal maxValueBandwidth = null;
		final ArrayList<Location> locations = new ArrayList<Location>(variantWeights.getLocations());
		int strictlyDecreasingCounter = 0;
		for (BigDecimal bandwidth : bandwidthCandidates) {
			final KernelDensityEstimation kde = new KernelDensityEstimation(kernel.copyOfKernelWithBandwidth(bandwidth));
			
			final Map<Variant,Double> values = Collections.synchronizedMap(new HashMap<Variant,Double>());
			
			// evaluation in parallel for different variants, store the sum per variant into "values",
			// which is then summed up later
			ThreadedTodoWorker.workOnTodoList(variantWeights.getVariants(), new ThreadedTodoWorker.SimpleTodoWorker<Variant>() {
				public void processTodoItem(Variant variant) {
					// count the locations where this variant has a positive weight
					int count = 0;
					for (Location location : locations) {
						if (variantWeights.getNumberOfVariantOccurencesAtLocation(variant, location) > 0) {
							count++;
						}
					}
					// if this variant occurs often enough, then perform the computations for the likelihood-cross-validation itself
					double value = 0.0;
					if (count > locations.size() * MIN_OCCURRENCE_AT_LOCATION) {
						for (Location location : locations) {
							double weight = variantWeights.getWeight(variant, location);
							if (weight > 0.0) {
								double est = kde.estimate(variantWeights, variant, location, location);
								value += weight * Math.log(est);
							}
						}
					}
					values.put(variant, value);
				}
			});
			
			double value = 0.0;
			for (Entry<Variant,Double> entry : values.entrySet()) {
				value += entry.getValue();
			}
			
			if (value > maxValue) {
				maxValue = value;
				maxValueBandwidth = bandwidth;
			}
			if (value < prevValue) {
				strictlyDecreasingCounter++;
				if (strictlyDecreasingCounter >= STRICTLY_DECREASING_BREAK) {
					break;
				}
			} else {
				strictlyDecreasingCounter = 0;
			}
			prevValue = value;
		}
		
		return maxValueBandwidth;
	}
	
	/**
	 * Returns an identification string for the likelihood-cross validation.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationStringWithoutParameters() {
		return getStaticIdentificationString();
	}
	
	/**
	 * Returns an identification string for the likelihood-cross validation.
	 * 
	 * @return the identification string
	 */
	public static String getStaticIdentificationString() {
		return "likelihood_cross_validation";
	}
	
}