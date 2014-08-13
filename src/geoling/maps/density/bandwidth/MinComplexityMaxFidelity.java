package geoling.maps.density.bandwidth;

import geoling.maps.AreaClassMap;
import geoling.maps.density.KernelDensityEstimation;
import geoling.maps.density.kernels.Kernel;
import geoling.maps.projection.KilometresProjection;
import geoling.maps.util.MapBorder;
import geoling.maps.util.VoronoiMap;
import geoling.maps.util.VoronoiMapCache;
import geoling.maps.weights.VariantWeights;
import geoling.util.ThreadedTodoWorker;
import geoling.util.sim.grain.Polytope;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Bandwidth estimation method that tries to achieve a low complexity together
 * with a high area compactness.
 * 
 * @author student assistant (based on existing code), Institute of Stochastics, Ulm University
 * @see "Julius Vogelbacher, Statistische Analyse von Wortvarianten des Sprachatlas von
 *       Bayerisch-Schwaben, Universität Ulm, Diplomarbeit, Section 2.8.2"
 */
public class MinComplexityMaxFidelity extends BandwidthEstimator {
	
	/**
	 * Constructor using the kernel that will be used for bandwidth estimation.
	 * 
	 * @param kernel  the kernel
	 */
	public MinComplexityMaxFidelity(Kernel kernel) {
		super(kernel);
	}
	
	/**
	 * Detects a suitable bandwidth from the given candidates, trade-off
	 * between minimal complexity and maximal area compactness.
	 * 
	 * @param variantWeights       the weights for all variants at all locations
	 * @param bandwidthCandidates  the bandwidths that should be tested
	 * @return the detected bandwidth, <code>null</code> if no best bandwidth could be detected
	 *         (e.g., if <code>bandwidthCandidates</code> is empty)
	 */
	public BigDecimal findBandwidth(final VariantWeights variantWeights, Collection<BigDecimal> bandwidthCandidates) {
		// prepare the objects that are used in the loop below (whose worker threads have no
		// database connection)
		HashMap<BigDecimal,AreaClassMap> areaClassMaps = new HashMap<BigDecimal,AreaClassMap>();
		VoronoiMap voronoiMap = null;
		for (BigDecimal bandwidth : bandwidthCandidates) {
			AreaClassMap map = new AreaClassMap(variantWeights, new KernelDensityEstimation(kernel.copyOfKernelWithBandwidth(bandwidth)));
			areaClassMaps.put(bandwidth, map);
			
			if (voronoiMap == null) {
				// note that:
				// - we use map.getLocations() because the map object may return another
				//   set of locations (e.g. all locations instead of only those that we
				//   have weights for)
				// - we generate the VoronoiMap object only once, because it is the same
				//   for all bandwidths
				Polytope border = new Polytope(MapBorder.getAggregatedLocationsConvexHull(map.getLocations()).getVertices(), true);
				KilometresProjection kilometresProjection = new KilometresProjection(border);
				voronoiMap = VoronoiMapCache.getVoronoiMap(map.getLocations(), border, kilometresProjection);
			}
		}
		final VoronoiMap voronoiMapFinal = voronoiMap;
		
		final Map<BigDecimal,Double> totalBorderLengthMap = Collections.synchronizedMap(new HashMap<BigDecimal,Double>());
		final Map<BigDecimal,Double> overallAreaCompactnessMap = Collections.synchronizedMap(new HashMap<BigDecimal,Double>());
		
		// compute the total border length (-> complexity) and the overall
		// area compactness (-> fidelity) for every bandwidth and remember
		// them in the hash maps
		ThreadedTodoWorker.workOnTodoList(areaClassMaps.entrySet(), new ThreadedTodoWorker.SimpleTodoWorker<Entry<BigDecimal,AreaClassMap>>() {
			public void processTodoItem(Entry<BigDecimal,AreaClassMap> entry) {
				BigDecimal bandwidth = entry.getKey();
				AreaClassMap map = entry.getValue();
				
				// compute the map information
				map.buildAreas(voronoiMapFinal);
				
				// evaluate characteristics of this map and store the values
				totalBorderLengthMap.put(bandwidth, map.computeTotalBorderLength());
				overallAreaCompactnessMap.put(bandwidth, map.computeOverallAreaCompactness());
			}
		});
		
		double totalBorderLengthMax = Double.NEGATIVE_INFINITY;
		for (Entry<BigDecimal,Double> entry : totalBorderLengthMap.entrySet()) {
			if (totalBorderLengthMax < entry.getValue()) {
				totalBorderLengthMax = entry.getValue();
			}
		}
		
		double overallAreaCompactnessMin = Double.POSITIVE_INFINITY;
		for (Entry<BigDecimal,Double> entry : overallAreaCompactnessMap.entrySet()) {
			if (overallAreaCompactnessMin > entry.getValue()) {
				overallAreaCompactnessMin = entry.getValue();
			}
		}
		
		// now we only have to find the "best" bandwidth...
		
		double maxValue = Double.NEGATIVE_INFINITY;
		BigDecimal maxValueBandwidth = null;
		
		for (BigDecimal bandwidth : bandwidthCandidates) {
			double diffC = totalBorderLengthMax - totalBorderLengthMap.get(bandwidth).doubleValue();
			double diffL = overallAreaCompactnessMap.get(bandwidth).doubleValue() - overallAreaCompactnessMin;
			double value = diffC * Math.sqrt(diffL);
			
			if (value > maxValue) {
				maxValue = value;
				maxValueBandwidth = bandwidth;
			}
		}
		
		return maxValueBandwidth;
	}
	
	/**
	 * Returns an identification string for this bandwidth estimation method.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationStringWithoutParameters() {
		return getStaticIdentificationString();
	}
	
	/**
	 * Returns an identification string for this bandwidth estimation method.
	 * 
	 * @return the identification string
	 */
	public static String getStaticIdentificationString() {
		return "min_complexity_max_area_compactness";
	}
	
}
