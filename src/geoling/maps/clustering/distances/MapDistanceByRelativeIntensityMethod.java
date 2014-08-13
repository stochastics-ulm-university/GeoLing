package geoling.maps.clustering.distances;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.AreaClassMap;
import geoling.maps.clustering.data.MapClusterObject;
import geoling.util.SetComparison;
import geoling.util.clusteranalysis.ClusterObject;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class calculates the distance of 2 area-class-map by comparing the
 * relative intensities of all locations.
 * 
 * @author student assistant, Institute of Stochastics, Ulm University
 * @see "Jonas Rumpf, Simon Pickl, Stephan Elspaß, Werner König, Volker Schmidt,
 *      Quantification and Statistical Analysis of Structural Similarities in
 *      Dialectological Area-Class Maps, page 81 (Section: 3.1.1)"
 */
public class MapDistanceByRelativeIntensityMethod implements MapDistance {
	
	private ConcurrentHashMap<AreaClassMap, ConcurrentHashMap<AreaClassMap, Double>> cachedDistances;
	
	/**
	 * Constructs a new object for computing distances between area-class-maps with
	 * the relative-intensity method.
	 */
	public MapDistanceByRelativeIntensityMethod() {
		this.cachedDistances = new ConcurrentHashMap<AreaClassMap, ConcurrentHashMap<AreaClassMap, Double>>();
	}
	
	public double distance(double[] p, double[] q) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * The method calculates the distance between two area-class-maps.
	 * 
	 * @param object1  any area-class-map given as a <code>MapClusterObject</code>
	 * @param object2  any area-class-map given as a <code>MapClusterObject</code>
	 * @return the distance between the two area-class-maps
	 */
	public double distance(ClusterObject object1, ClusterObject object2) {
		MapClusterObject mapObj1 = (MapClusterObject)object1;
		MapClusterObject mapObj2 = (MapClusterObject)object2;
		AreaClassMap map1 = mapObj1.getAreaClassMap();
		AreaClassMap map2 = mapObj2.getAreaClassMap();
		
		ConcurrentHashMap<AreaClassMap, Double> hashMap = this.cachedDistances.get(map1);
		Double cachedDistance = (hashMap != null) ? hashMap.get(map2) : null;
		
		if (cachedDistance != null) {
			return cachedDistance;
		} else {
			map1.buildLocationDensityCache();
			map2.buildLocationDensityCache();
			
			if (!SetComparison.equalSets(map1.getLocations(), map2.getLocations())) {
				throw new RuntimeException("Both maps need to have the same locations!");
			}
			
			ArrayList<Double> distanceMap1 = new ArrayList<Double>();
			ArrayList<Double> distanceMap2 = new ArrayList<Double>();
			
			for (int i = 0; i < map1.getLocations().size(); i++) {
				AggregatedLocation location1 = map1.getLocations().get(i);
				AreaClassMap.VariantDensityResult map1loc1 = map1.getDominantVariantAndDensity(location1);
				AreaClassMap.VariantDensityResult map2loc1 = map2.getDominantVariantAndDensity(location1);
				
				for (int j = i + 1; j < map1.getLocations().size(); j++) {
					AggregatedLocation location2 = map1.getLocations().get(j);
					AreaClassMap.VariantDensityResult map1loc2 = map1.getDominantVariantAndDensity(location2);
					AreaClassMap.VariantDensityResult map2loc2 = map2.getDominantVariantAndDensity(location2);
					
					if (map1loc1 == null && map1loc2 == null) {
						distanceMap1.add(0.0);
					} else if (map1loc1 == null) {
						distanceMap1.add(Math.abs(map1loc2.density - map1.getVariantDensity(map1loc2.variant, location1)) / 2.0);
					} else if (map1loc2 == null) {
						distanceMap1.add(Math.abs(map1loc1.density - map1.getVariantDensity(map1loc1.variant, location2)) / 2.0);
					} else if (map1loc1.variant.equals(map1loc2.variant)) {
						distanceMap1.add(Math.abs(map1loc1.density - map1loc2.density));
					} else {
						distanceMap1.add(Math.abs(map1loc1.density - map1.getVariantDensity(map1loc1.variant, location2)) / 2.0
								+ Math.abs(map1loc2.density - map1.getVariantDensity(map1loc2.variant, location1)) / 2.0);
					}
					
					if (map2loc1 == null && map2loc2 == null) {
						distanceMap1.add(0.0);
					} else if (map2loc1 == null) {
						distanceMap1.add(Math.abs(map2loc2.density - map2.getVariantDensity(map2loc2.variant, location1)) / 2.0);
					} else if (map2loc2 == null) {
						distanceMap1.add(Math.abs(map2loc1.density - map2.getVariantDensity(map2loc1.variant, location2)) / 2.0);
					} else if (map2loc1.variant.equals(map2loc2.variant)) {
						distanceMap2.add(Math.abs(map2loc1.density - map2loc2.density));
					} else {
						distanceMap2.add(Math.abs(map2loc1.density - map2.getVariantDensity(map2loc1.variant, location2)) / 2.0
								+ Math.abs(map2loc2.density - map2.getVariantDensity(map2loc2.variant, location1)) / 2.0);
					}
					
				}
				
			}
			double result = 0;
			for (int i = 0; i < distanceMap1.size(); i++) {
				result = result + Math.abs(distanceMap1.get(i) - distanceMap2.get(i));
			}
			
			synchronized (this) {
				ConcurrentHashMap<AreaClassMap, Double> hashMap1 = this.cachedDistances.get(map1);
				ConcurrentHashMap<AreaClassMap, Double> hashMap2 = this.cachedDistances.get(map2);
				if (hashMap1 == null) {
					hashMap1 = new ConcurrentHashMap<AreaClassMap, Double>();
					this.cachedDistances.put(map1, hashMap1);
				}
				if (hashMap2 == null) {
					hashMap2 = new ConcurrentHashMap<AreaClassMap, Double>();
					this.cachedDistances.put(map2, hashMap2);
				}
				hashMap1.put(map2, result);
				hashMap2.put(map1, result);
			}
			
			return result;
		}
	}
	
	public String getIdentificationString() {
		return getStaticIdentificationString();
	}
	
	/**
	 * Returns an identification string for this map distance type.
	 * 
	 * @return the identification string
	 */
	public static String getStaticIdentificationString() {
		return "relative_intensities";
	}
	
}