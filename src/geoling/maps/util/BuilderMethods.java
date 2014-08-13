package geoling.maps.util;

import java.math.BigDecimal;

import geoling.maps.density.bandwidth.BandwidthEstimator;
import geoling.maps.density.bandwidth.LeastSquaresCrossValidation;
import geoling.maps.density.bandwidth.LikelihoodCrossValidation;
import geoling.maps.density.bandwidth.MinComplexityMaxFidelity;
import geoling.maps.density.kernels.EpanechnikovKernel;
import geoling.maps.density.kernels.GaussianKernel;
import geoling.maps.density.kernels.K3Kernel;
import geoling.maps.density.kernels.Kernel;
import geoling.maps.distances.DistanceMeasure;
import geoling.maps.distances.GeographicalDistance;
import geoling.maps.distances.PrecomputedDistance;
import geoling.models.Distance;

/**
 * Helper class providing some methods to obtain objects specified by their
 * identification strings.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class BuilderMethods {
	
	/**
	 * Returns the corresponding distance measure object for the given
	 * identification string.
	 * 
	 * @param distanceIdStr  the identification string of the distance measure
	 * @return the distance measure object
	 */
	public static DistanceMeasure getDistanceMeasureObj(String distanceIdStr) {
		DistanceMeasure distanceMeasure;
		if (GeographicalDistance.getStaticIdentificationString().equals(distanceIdStr)) {
			distanceMeasure = new GeographicalDistance();
		} else {
			Distance distance = Distance.findFirst("type = 'precomputed' AND identification = ?", distanceIdStr);
			if (distance != null) {
				distanceMeasure = new PrecomputedDistance(distance, true);
			} else {
				throw new RuntimeException("Unknown distance measure identification string: "+distanceIdStr);
			}
		}
		return distanceMeasure;
	}
	
	/**
	 * Returns the corresponding kernel object for the given identification string.
	 * 
	 * @param distanceMeasure the existing distance measure
	 * @param bandwidth       the bandwidth to use for the kernel, you can use <code>null</code>
	 *                        if the generated object should be used for bandwidth estimation
	 * @param kernelIdStr     the identification string of the kernel
	 * @return the kernel object
	 */
	public static Kernel getKernelObj(DistanceMeasure distanceMeasure, BigDecimal bandwidth, String kernelIdStr) {
		Kernel estimator;
		if (EpanechnikovKernel.getStaticIdentificationString().equals(kernelIdStr)) {
			estimator = new EpanechnikovKernel(distanceMeasure, bandwidth);
		} else if (GaussianKernel.getStaticIdentificationString().equals(kernelIdStr)) {
			estimator = new GaussianKernel(distanceMeasure, bandwidth);
		} else if (K3Kernel.getStaticIdentificationString().equals(kernelIdStr)) {
			estimator = new K3Kernel(distanceMeasure, bandwidth);
		} else {
			throw new RuntimeException("Unknown kernel identification string: "+kernelIdStr);
		}
		return estimator;
	}
	
	/**
	 * Returns the corresponding bandwidth estimator object for the given identification string.
	 * 
	 * @param kernel          the existing kernel object
	 * @param estimatorIdStr  the identification string of the estimator
	 * @return the bandwidth estimator object
	 */
	public static BandwidthEstimator getBandwidthEstimatorObj(Kernel kernel, String estimatorIdStr) {
		BandwidthEstimator estimator;
		if (LikelihoodCrossValidation.getStaticIdentificationString().equals(estimatorIdStr)) {
			estimator = new LikelihoodCrossValidation(kernel);
		} else if (LeastSquaresCrossValidation.getStaticIdentificationString().equals(estimatorIdStr)) {
			estimator = new LeastSquaresCrossValidation(kernel);
		} else if (MinComplexityMaxFidelity.getStaticIdentificationString().equals(estimatorIdStr)) {
			estimator = new MinComplexityMaxFidelity(kernel);
		} else {
			throw new RuntimeException("Unknown bandwidth estimator identification string: "+estimatorIdStr);
		}
		return estimator;
	}
	
}