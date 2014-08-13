package geoling.maps.density.kernels;

import geoling.maps.distances.DistanceMeasure;

import java.math.BigDecimal;

/**
 * Abstract class for kernels used for density estimation in variant maps.
 * The kernel classes support different distance measures and arbitrary bandwidths.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public abstract class Kernel {
	
	/** The distance measure that will be used. */
	protected DistanceMeasure distanceMeasure;
	
	/**
	 * The bandwidth that will be used, stored as a <code>BigDecimal</code>,
	 * therefore without rounding errors, and normed by the method
	 * <code>stripTrailingZeros</code>.
	 */
	protected BigDecimal bandwidth;
	
	/** The bandwidth as a double value, used for computations. */
	protected double bandwidthAsDouble;
	
	/**
	 * Constructs the kernel object with the most important parameters.
	 * Note that we require a <code>BigDecimal</code> as a bandwidth, because
	 * we need the precision for identification purposes (i.e., conversion
	 * from/to String has to be unproblematic, no rounding errors allowed).
	 * 
	 * @param distanceMeasure  the distance measure
	 * @param bandwidth        the bandwidth
	 */
	public Kernel(DistanceMeasure distanceMeasure, BigDecimal bandwidth) {
		this.distanceMeasure = distanceMeasure;
		
		// instead of using "setBandwidth(bandwidth);" we set the internal
		// bandwidth parameters directly, because subclasses may throw
		// an exception in "setBandwidth" (if bandwidth is not supported)
		if (bandwidth == null) {
			bandwidth = BigDecimal.ZERO;
		}
		this.bandwidth         = bandwidth.stripTrailingZeros();
		this.bandwidthAsDouble = bandwidth.doubleValue();
	}
	
	/**
	 * Returns an identification string for this kernel, does not include parameters
	 * as e.g. the bandwidth.
	 * 
	 * @return the identification string
	 */
	public abstract String getIdentificationStringWithoutParameters();
	
	/**
	 * Returns an identification string for this kernel, includes the parameters,
	 * but ignores the bandwidth.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationStringWithoutBandwidth() {
		return this.getIdentificationStringWithoutParameters()+":distances="+this.distanceMeasure.getIdentificationString();
	}
	
	/**
	 * Returns an identification string for this kernel, includes the parameters
	 * as e.g. the bandwidth.
	 * This method takes care that the conversion of the bandwidth to/from string is
	 * unproblematic, i.e., uses <code>toPlainString</code> of the <code>bandwidth</code>
	 * stored as normalized <code>BigDecimal</code> (<code>stripTrailingZeros</code>
	 * was applied).
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		return this.getIdentificationStringWithoutBandwidth()+":bandwidth="+this.bandwidth.toPlainString();
	}
	
	/**
	 * Returns the distance measure.
	 * 
	 * @return the distance measure
	 */
	public DistanceMeasure getDistanceMeasure() {
		return this.distanceMeasure;
	}
	
	/**
	 * Returns the bandwidth.
	 * 
	 * @return the bandwidth
	 */
	public BigDecimal getBandwidth() {
		return this.bandwidth;
	}
	
	/**
	 * Returns the maximum distance for relevant locations w.r.t. kernel
	 * density estimation (kernel and bandwidth).
	 * 
	 * @return the distance w.r.t. the current distance measure or positive
	 *         infinity if it could not be computed/estimated
	 */
	public abstract double getMaxRelevantDistanceForEstimation();
	
	/**
	 * Evaluates the kernel for the given distance and the defined bandwidth.
	 * 
	 * @param distance  the distance
	 * @return the value obtained from the kernel function
	 */
	public abstract double evaluateKernel(double distance);
	
	/**
	 * Generates a new kernel object of the same type, but with another bandwidth.
	 * 
	 * @param bandwidth  the bandwidth
	 * @return the kernel object of the same type and new bandwidth
	 */
	public abstract Kernel copyOfKernelWithBandwidth(BigDecimal bandwidth);
	
}