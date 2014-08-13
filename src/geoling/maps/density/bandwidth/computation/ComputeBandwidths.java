package geoling.maps.density.bandwidth.computation;

import geoling.config.Database;
import geoling.config.Settings;
import geoling.maps.density.bandwidth.*;
import geoling.maps.density.kernels.*;
import geoling.maps.distances.*;
import geoling.maps.weights.*;
import geoling.models.*;
import geoling.util.ProgressOutput;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import org.javalite.activejdbc.DBException;
import org.javalite.activejdbc.LazyList;

/**
 * This class provides a method to estimate (and save) bandwidths for maps
 * in the database.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class ComputeBandwidths {
	
	/** Determines whether <b>all</b> sensible combinations of estimators, distance measures etc. will be used. */
	public static boolean ALL_ESTIMATORS = false;
	
	/**
	 * Computes the bandwidth for the given map, using the given estimator.
	 * 
	 * @param variantWeights  the map given by its variant weights object
	 * @param estimator       the bandwidth estimator object
	 * @return the detected bandwidth
	 */
	public static BigDecimal computeBandwidth(VariantWeights variantWeights, BandwidthEstimator estimator) {
		BigDecimal bw = estimator.findBandwidth(variantWeights);
		// bw may be null, e.g., if there is only one variant on this level for all
		// locations, which means the maximal linguistic distance is zero (implying
		// that we cannot detect a bandwidth)
		
		// we set the bandwidth to 1.0 if it couldn't be detected, because 0.0 is bad for
		// density estimation and 1.0 is valid for geographic and linguistic distances
		// (we want to save something, because we don't want to get "no bandwidth" errors)
		return ((bw != null) ? bw : BigDecimal.ONE);
	}
	
	/**
	 * Computes the bandwidth for the given map, using the given estimator,
	 * and saves the bandwidth to the database.
	 * Note that this method does not throw an exception if saving was not possible,
	 * in that case it only prints the stack-trace to <code>System.err</code>.
	 * 
	 * @param variantWeights  the map given by its variant weights object
	 * @param estimator       the bandwidth estimator object
	 * @param bandwidth       the existing bandwidth object from the database that
	 *                        will be updated (if <code>null</code>, it will be created)
	 * @return the detected bandwidth
	 */
	public static BigDecimal computeAndSaveBandwidth(VariantWeights variantWeights, BandwidthEstimator estimator, Bandwidth bandwidth) {
		BigDecimal bw = computeBandwidth(variantWeights, estimator);
		
		try {
			if (bandwidth == null) {
				bandwidth = new Bandwidth();
				bandwidth.set("map_id", variantWeights.getMap().getId());
				bandwidth.set("weights_identification", variantWeights.getIdentificationString());
				bandwidth.set("kernel_identification", estimator.getKernel().getIdentificationStringWithoutParameters());
				bandwidth.set("distance_identification", estimator.getDistanceMeasure().getIdentificationString());
				bandwidth.set("estimator_identification", estimator.getIdentificationStringWithoutParameters());
			}
			bandwidth.set("bandwidth", bw.toPlainString());
			bandwidth.saveIt();
		} catch (DBException e) {
			// even if saving was not possible (because e.g. we may have only read-access to the database),
			// we want to return the computed bandwidth, therefore we do not raise an exception
			e.printStackTrace();
		}
		
		return bw;
	}
	
	/**
	 * Computes the bandwidth for the given map, using the given estimator,
	 * and saves the bandwidth to the database.
	 * Note that this method does not throw an exception if saving was not possible,
	 * in that case it only prints the stack-trace to <code>System.err</code>.
	 * 
	 * @param variantWeights  the map given by its variant weights object
	 * @param estimator       the bandwidth estimator object
	 * @param recompute       determines whether the bandwidth should be recomputed
	 *                        if it is already present
	 * @param progress        the <code>ProgressOutput</code> (may be <code>null</code>)
	 * @return the detected bandwidth
	 */
	public static BigDecimal findOrComputeAndSaveBandwidth(VariantWeights variantWeights, BandwidthEstimator estimator, boolean recompute, ProgressOutput progress) {
		if (progress != null) progress.customMessage("Map: "+variantWeights.getMap().getString("name"));
		if (progress != null) progress.customMessage("Weights: "+variantWeights.getIdentificationString());
		if (progress != null) progress.customMessage("Estimator: "+estimator.getIdentificationString());
		
		Bandwidth bandwidth = Bandwidth.findByIdentificationObj(variantWeights, estimator);
		BigDecimal bw;
		if ((bandwidth == null) || recompute) {
			long start = System.currentTimeMillis();
			bw = computeAndSaveBandwidth(variantWeights, estimator, bandwidth);
			long end = System.currentTimeMillis();
			
			if (progress != null) progress.customMessage("Result: "+bw.toPlainString()+" (time in ms: "+(end-start)+")");
		} else {
			bw = bandwidth.getBigDecimal("bandwidth");
			if (progress != null) progress.customMessage("Result: (already in database, skipping)");
		}
		
		return bw;
	}
	
	/**
	 * Computes the bandwidths for the given maps, for the given levels and estimators.
	 * 
	 * @param maps          the maps for which we want to compute bandwidths,
	 *                      may be <code>null</code>, then all maps are used
	 * @param levels        the list of level objects for which we want to compute bandwidths,
	 *                      may be <code>null</code>, then all levels are used
	 * @param estimators    the list of estimator objects that should be used
	 * @param onlyContained if set, then for the linguistic distance only groups will be
	 *                      considered where the current map is contained
	 * @param recompute     determines whether the bandwidth should be recomputed
	 *                      if it is already present
	 */
	public static void computeBandwidths(List<Map> maps, List<Level> levels, List<BandwidthEstimator> estimators, boolean onlyContained, boolean recompute) {
		if (maps == null) {
			maps = Map.findAll();
		}
		if (levels == null) {
			levels = Level.findAll();
		}
		
		ProgressOutput progress = new ProgressOutput(System.out);
		
		for (Map map : maps) {
			VariantWeights variantWeightsBase = new VariantWeightsNoLevel(map);
			
			for (Level level : levels) {
				VariantWeights variantWeights = new VariantWeightsWithLevel(variantWeightsBase, level);
				
				for (BandwidthEstimator estimator : estimators) {
					if (estimator.getDistanceMeasure() instanceof LinguisticDistance) {
						// if linguistic distance, then perform the estimation only if the level
						// matches and this map is contained in the group
						LinguisticDistance d = (LinguisticDistance)estimator.getDistanceMeasure();
						if ((d.getLevel() != null) && !d.getLevel().equals(level)) {
							continue;
						}
						if (onlyContained && (d.getGroup() != null) && d.getGroup().get(Map.class, "map_id = ?", map.getId()).isEmpty()) {
							continue;
						}
					}
					
					try {
						findOrComputeAndSaveBandwidth(variantWeights, estimator, recompute, progress);
					} catch (Exception e) {
						e.printStackTrace();
					}
					progress.customMessage("");
				}
				
				progress.customMessage("");
			}
		}
	}
	
	/**
	 * Returns a list of bandwidth estimator objects, using the usual combinations
	 * of estimator type, distance measure and kernel.
	 * 
	 * @param groups  the groups for which linguistic distances exist
	 * @return the list of bandwidth estimator objects
	 */
	public static List<BandwidthEstimator> getDefaultEstimators(List<Group> groups) {
		GeographicalDistance geographicalDistance = new GeographicalDistance();
		
		GaussianKernel gaussianKernelWithGeographicalDistance = new GaussianKernel(geographicalDistance, null);
		EpanechnikovKernel epanechnikovKernelWithGeographicalDistance = new EpanechnikovKernel(geographicalDistance, null);
		K3Kernel k3KernelWithGeographicalDistance = new K3Kernel(geographicalDistance, null);
		
		if (groups == null) {
			groups = Group.findAll();
		}
		LazyList<Level> levels = Level.findAll();
		LinkedList<GaussianKernel> gaussianKernelsWithLinguisticDistance = new LinkedList<GaussianKernel>();
		LinkedList<EpanechnikovKernel> epanechnikovKernelsWithLinguisticDistance = new LinkedList<EpanechnikovKernel>();
		LinkedList<K3Kernel> k3KernelsWithLinguisticDistance = new LinkedList<K3Kernel>();
		for (Group group : groups) {
			for (Level level : levels) {
				LinguisticDistance lingDistance = new LinguisticDistance(level, group, true);
				gaussianKernelsWithLinguisticDistance.add(new GaussianKernel(lingDistance, null));
				epanechnikovKernelsWithLinguisticDistance.add(new EpanechnikovKernel(lingDistance, null));
				k3KernelsWithLinguisticDistance.add(new K3Kernel(lingDistance, null));
			}
		}
		
		LinkedList<BandwidthEstimator> estimators = new LinkedList<BandwidthEstimator>();
		
		// least-squares cross validation
		estimators.add(new LeastSquaresCrossValidation(gaussianKernelWithGeographicalDistance));
		
		// likelihood cross validation
		estimators.add(new LikelihoodCrossValidation(gaussianKernelWithGeographicalDistance));
		if (ALL_ESTIMATORS) estimators.add(new LikelihoodCrossValidation(epanechnikovKernelWithGeographicalDistance));
		estimators.add(new LikelihoodCrossValidation(k3KernelWithGeographicalDistance));
		if (ALL_ESTIMATORS) {
			for (GaussianKernel kernel : gaussianKernelsWithLinguisticDistance) {
				estimators.add(new LikelihoodCrossValidation(kernel));
			}
			for (EpanechnikovKernel kernel : epanechnikovKernelsWithLinguisticDistance) {
				estimators.add(new LikelihoodCrossValidation(kernel));
			}
		}
		for (K3Kernel kernel : k3KernelsWithLinguisticDistance) {
			estimators.add(new LikelihoodCrossValidation(kernel));
		}
		
		// min-complexity-max-fidelity
		estimators.add(new MinComplexityMaxFidelity(gaussianKernelWithGeographicalDistance));
		if (ALL_ESTIMATORS) {
			estimators.add(new MinComplexityMaxFidelity(epanechnikovKernelWithGeographicalDistance));
			estimators.add(new MinComplexityMaxFidelity(k3KernelWithGeographicalDistance));
			for (GaussianKernel kernel : gaussianKernelsWithLinguisticDistance) {
				estimators.add(new MinComplexityMaxFidelity(kernel));
			}
			for (EpanechnikovKernel kernel : epanechnikovKernelsWithLinguisticDistance) {
				estimators.add(new MinComplexityMaxFidelity(kernel));
			}
		}
		for (K3Kernel kernel : k3KernelsWithLinguisticDistance) {
			estimators.add(new MinComplexityMaxFidelity(kernel));
		}
		
		return estimators;
	}
	
	/**
	 * Main method, allows to execute this class to compute the bandwidths for all
	 * maps found in the database.
	 * 
	 * @param args  command line parameters (not used)
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Settings.load();
		Database.connect(Settings.getDatabaseIdentifier());
		
		computeBandwidths(null, null, getDefaultEstimators(null), true, false);
	}
	
}
