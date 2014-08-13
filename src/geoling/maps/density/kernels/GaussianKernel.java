package geoling.maps.density.kernels;

import geoling.maps.distances.DistanceMeasure;
import geoling.maps.weights.VariantWeights;
import geoling.models.Bandwidth;

import java.math.BigDecimal;

/**
 * Gaussian kernel used for density estimation on variant maps.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class GaussianKernel extends Kernel {
	
	/**
	 * Constructs a Gaussian kernel with the given distance measure and
	 * bandwidth.
	 * 
	 * @param distanceMeasure  the distance measure
	 * @param bandwidth        the bandwidth
	 */
	public GaussianKernel(DistanceMeasure distanceMeasure, BigDecimal bandwidth) {
		super(distanceMeasure, bandwidth);
	}
	
	/**
	 * Constructs a Gaussian kernel with the given distance measure and
	 * a previously computed bandwidth from the database.
	 * 
	 * @param distanceMeasure  the distance measure
	 * @param variantWeights   the variant weights required to find the bandwidth
	 * @param estimatorIdStr   the identification string of the bandwidth estimator
	 */
	public GaussianKernel(DistanceMeasure distanceMeasure, VariantWeights variantWeights, String estimatorIdStr) {
		super(distanceMeasure, Bandwidth.findNumberByIdentificationStr(variantWeights.getMap(), variantWeights.getIdentificationString(), getStaticIdentificationString(), distanceMeasure.getIdentificationString(), estimatorIdStr));
		if (this.bandwidth.equals(BigDecimal.ZERO)) {
			throw new RuntimeException("No bandwidth found in database (or bandwidth is zero)!");
		}
	}
	
	/**
	 * Returns an identification string for this kernel, does not include parameters
	 * as e.g. the bandwidth.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationStringWithoutParameters() {
		return getStaticIdentificationString();
	}
	
	/**
	 * Returns an identification string for this kernel, does not include the bandwidth.
	 * 
	 * @return the identification string
	 */
	public static String getStaticIdentificationString() {
		return "gaussian";
	}
	
	/**
	 * Returns the maximum distance for relevant locations w.r.t. kernel
	 * density estimation (kernel and bandwidth).
	 * 
	 * @return the distance w.r.t. the current distance measure or positive
	 *         infinity if it could not be computed/estimated
	 */
	public double getMaxRelevantDistanceForEstimation() {
		return this.bandwidthAsDouble * 3.09;
	}
	
	/**
	 * Evaluates the Gaussian kernel for the given distance and the defined bandwidth.
	 * 
	 * @param distance  the distance
	 * @return the value obtained from the kernel function
	 */
	public double evaluateKernel(double distance) {
		double x = distance/this.bandwidthAsDouble;
		if (Double.isNaN(distance)) {
			return 0.0;
		}
		return Math.exp(-0.5*x*x) / Math.sqrt(2.0*Math.PI);
	}
	
	/**
	 * Generates a new kernel object of the same type, but with another bandwidth.
	 * 
	 * @param bandwidth  the bandwidth
	 * @return the kernel object of the same type and new bandwidth
	 */
	public GaussianKernel copyOfKernelWithBandwidth(BigDecimal bandwidth) {
		return new GaussianKernel(this.getDistanceMeasure(), bandwidth);
	}
	
}
