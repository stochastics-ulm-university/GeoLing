package geoling.maps.density.bandwidth;

import geoling.maps.density.kernels.GaussianKernel;
import geoling.maps.density.kernels.Kernel;
import geoling.maps.distances.GeographicalDistance;
import geoling.maps.weights.VariantWeights;
import geoling.models.ConfigurationOption;
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
 * Least-squares-cross-validation for determining a bandwidth for kernel density estimation.
 * 
 * @author student assistant (based on existing code), Institute of Stochastics, Ulm University
 * @see "Dissertation Jonas Rumpf, Ulm University, Section 3.3.2 and 4.3.2"
 */
public class LeastSquaresCrossValidation extends BandwidthEstimator {
	
	public static class LeastSquaresCrossValidationNotSupportedException extends IllegalArgumentException {
		private static final long serialVersionUID = 1L;
		
		public LeastSquaresCrossValidationNotSupportedException(String string) {
			super(string);
		}
	}
	
	/**
	 * Limit of consecutive bandwidths tested where the value increases strictly, used
	 * to speedup the bandwidth estimation.
	 */
	public static int STRICTLY_INCREASING_BREAK = 20;
	
	/** Determines whether the (absolute) number of answers at a location is ignored, i.e., whether only the weight itself is relevant. */
	protected boolean ignoreFrequencies;
	
	/**
	 * Constructor using the kernel that will be used for bandwidth estimation.
	 * Note that this method requires a Gaussian kernel using the geographic distance.
	 * 
	 * @param kernel            the kernel
	 * @param ignoreFrequencies determines whether the (absolute) number of answers at a
	 *                          location is ignored, i.e., whether only the weight
	 *                          itself is relevant
	 * @throws LeastSquaresCrossValidationNotSupportedException if the given kernel is not supported
	 */
	public LeastSquaresCrossValidation(Kernel kernel, boolean ignoreFrequencies) {
		super(kernel);
		if (!((kernel instanceof GaussianKernel) && (kernel.getDistanceMeasure() instanceof GeographicalDistance))) {
			throw new LeastSquaresCrossValidationNotSupportedException("Only Gaussian kernel with geographic distance measure is allowed!");
		}
		this.ignoreFrequencies = ignoreFrequencies;
	}
	
	/**
	 * Constructor using the kernel that will be used for bandwidth estimation.
	 * Note that this method requires a Gaussian kernel using the geographic distance.
	 * 
	 * @param kernel  the kernel
	 * @throws LeastSquaresCrossValidationNotSupportedException if the given kernel is not supported
	 */
	public LeastSquaresCrossValidation(Kernel kernel) {
		this(kernel, ConfigurationOption.getOption("ignoreFrequenciesInDensityEstimation", false));
	}
	
	/**
	 * Detects a suitable bandwidth from the given candidates,
	 * uses the least-squares-cross-validation.
	 * 
	 * @param variantWeights       the weights for all variants at all locations
	 * @param bandwidthCandidates  the bandwidths that should be tested
	 * @return the detected bandwidth, <code>null</code> if no best bandwidth could be detected
	 *         (e.g., if <code>bandwidthCandidates</code> is empty)
	 */
	public BigDecimal findBandwidth(final VariantWeights variantWeights, Collection<BigDecimal> bandwidthCandidates) {
		final ArrayList<Location> locations = new ArrayList<Location>(variantWeights.getLocations());
		Double minValue = Double.POSITIVE_INFINITY;
		Double prevValue = Double.POSITIVE_INFINITY;
		BigDecimal minValueBandwidth = null;
		int strictlyIncreasingCounter = 0;
		for (BigDecimal bandwidth : bandwidthCandidates) {
			// Note: we don't need a kernel object with the correct bandwidth
			// (kernel.copyOfKernelWithBandwidth(bandwidth)), because we don't use it.
			// Least-squares-cross-validation is only implemented for the Gaussian case
			// with Euclidean/geographical distance, which is asserted by the constructor.
			
			final double h = bandwidth.doubleValue();
			final Map<Variant,Double> values = Collections.synchronizedMap(new HashMap<Variant,Double>());
			
			// evaluation in parallel for different variants, store the sum per variant into "values",
			// which is then summed up later
			ThreadedTodoWorker.workOnTodoList(variantWeights.getVariants(), new ThreadedTodoWorker.SimpleTodoWorker<Variant>() {
				public void processTodoItem(Variant variant) {
					int n = 0;
					double sum = 0.0;
					for (Location location1 : locations) {
						int number1;
						if (ignoreFrequencies) {
							number1 = (int)Math.round(variantWeights.getWeight(variant, location1)*100);
						} else {
							number1 = variantWeights.getNumberOfVariantOccurencesAtLocation(variant, location1);
						}
						
						if (number1 > 0) {
							for (Location location2 : locations) {
								if (!location1.equals(location2)) {
									int number2;
									if (ignoreFrequencies) {
										number2 = (int)Math.round(variantWeights.getWeight(variant, location2)*100);
									} else {
										number2 = variantWeights.getNumberOfVariantOccurencesAtLocation(variant, location2);
									}
									
									if (number2 > 0) {
										double d = kernel.getDistanceMeasure().getDistance(location1, location2);
										sum += (Math.exp(-(d/h)*(d/h)/4.0)/4.0 - Math.exp(-(d/h)*(d/h)/2.0)) * number1*number2;
									}
								}
							}
							n += number1;
						}
					}
					
					double weightSum = 0.0;
					for (Location location : variantWeights.getLocations()) {
						weightSum += variantWeights.getWeight(variant, location);
					}
					
					values.put(variant, weightSum * (sum / (n*n*h*h*Math.PI) + 1.0/(Math.PI*n*h*h)));
				}
			});
			
			double value = 0.0;
			for (Entry<Variant,Double> entry : values.entrySet()) {
				value += entry.getValue();
			}
			
			if (value < minValue) {
				minValue = value;
				minValueBandwidth = bandwidth;
			}
			
			if (value > prevValue) {
				strictlyIncreasingCounter++;
				if (strictlyIncreasingCounter >= STRICTLY_INCREASING_BREAK) {
					break;
				}
			} else {
				strictlyIncreasingCounter = 0;
			}
			prevValue = value;
		}
		
		return minValueBandwidth;
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
		return "least_squares_cross_validation";
	}
	
}