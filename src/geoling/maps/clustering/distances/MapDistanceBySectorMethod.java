package geoling.maps.clustering.distances;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.AreaClassMap;
import geoling.maps.clustering.data.MapClusterObject;
import geoling.models.Variant;
import geoling.util.LatLong;
import geoling.util.SetComparison;
import geoling.util.clusteranalysis.ClusterObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class calculates the distance of 2 area-class-map using the sector
 * method.
 * 
 * @author student assistant, Institute of Stochastics, Ulm University
 * @see "Jonas Rumpf, Simon Pickl, Stephan Elspaß, Werner König, Volker Schmidt,
 *      Quantification and Statistical Analysis of Structural Similarities in
 *      Dialectological Area-Class Maps, page 80 (Section: 3.1.1)"
 */
public class MapDistanceBySectorMethod implements MapDistance {
	
	/** The number of sections. */
	private int d = -1;
	
	private ConcurrentHashMap<AreaClassMap, ConcurrentHashMap<AreaClassMap, Double>> cachedDistances;
	private ArrayList<LatLong> cachedDirectionsOfSectorBoundaries;
	private HashMap<AggregatedLocation,ArrayList<ArrayList<AggregatedLocation>>> cachedSectorsForLocations;
	private ArrayList<AggregatedLocation> cachedLocations;
	
	/**
	 * Constructs a new object for computing distances between area-class-maps with
	 * the sector method.
	 * 
	 * @param d  the number of sections.
	 */
	public MapDistanceBySectorMethod(int d) {
		this.d = d;
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
	 * @return the distance between the two area-class-maps in kilometers
	 */
	public double distance(ClusterObject object1, ClusterObject object2) {
		MapClusterObject mapObj1 = (MapClusterObject)object1;
		MapClusterObject mapObj2 = (MapClusterObject)object2;
		AreaClassMap map1 = mapObj1.getAreaClassMap();
		AreaClassMap map2 = mapObj2.getAreaClassMap();
		
		ConcurrentHashMap<AreaClassMap,Double> hashMap = this.cachedDistances.get(map1);
		Double cachedDistance = (hashMap != null) ? hashMap.get(map2) : null;
		
		if (cachedDistance != null) {
			return cachedDistance;
		} else {
			map1.buildLocationDensityCache();
			map2.buildLocationDensityCache();
			
			// check sets of locations for equality
			if (!SetComparison.equalSets(map1.getLocations(), map2.getLocations())) {
				throw new RuntimeException("Both maps need to have the same locations!");
			}
			
			// build sectors cache
			synchronized (this) {
				if (cachedLocations == null) {
					cachedLocations = new ArrayList<AggregatedLocation>(map1.getLocations());
					cachedDirectionsOfSectorBoundaries = getSectorBoundaries();
					cachedSectorsForLocations = new HashMap<AggregatedLocation,ArrayList<ArrayList<AggregatedLocation>>>(cachedLocations.size() * 4 / 3);
					for (int i = 0; i < cachedLocations.size(); i++) {
						AggregatedLocation currentLocation = cachedLocations.get(i);
						ArrayList<ArrayList<AggregatedLocation>> locationsInSectors = new ArrayList<ArrayList<AggregatedLocation>>(d);
						for (int j = 0; j < d; j++) {
							locationsInSectors.add(getLocationsInSector(cachedLocations, currentLocation, cachedDirectionsOfSectorBoundaries, j));
						}
						cachedSectorsForLocations.put(currentLocation, locationsInSectors);
					}
				}
			}
			
			// check sets of locations for equality (for cached locations)
			if (!SetComparison.equalSets(map1.getLocations(), cachedLocations)) {
				throw new RuntimeException("Cached locations are different!");
			}
			
			ArrayList<Double> distanceMap1 = new ArrayList<Double>();
			ArrayList<Double> distanceMap2 = new ArrayList<Double>();
			
			for (int i = 0; i < cachedLocations.size(); i++) {
				AggregatedLocation currentLocation = cachedLocations.get(i);
				
				for (int j = 0; j < d; j++) {
					ArrayList<AggregatedLocation> locationsInSector = cachedSectorsForLocations.get(currentLocation).get(j);
					ArrayList<Variant> variantOfPointM1 = new ArrayList<Variant>();
					ArrayList<Variant> variantOfPointM2 = new ArrayList<Variant>();
					boolean areThereSeveralVariants1 = false;
					boolean areThereSeveralVariants2 = false;
					
					for (int k = 0; k < locationsInSector.size(); k++) {
						AreaClassMap.VariantDensityResult dominant1 = map1.getDominantVariantAndDensity(locationsInSector.get(k));
						AreaClassMap.VariantDensityResult dominant2 = map2.getDominantVariantAndDensity(locationsInSector.get(k));
						
						variantOfPointM1.add((dominant1 != null) ? dominant1.variant : null);
						if (variantOfPointM1.get(k) != variantOfPointM1.get(0)) {
							areThereSeveralVariants1 = true;
						}
						
						variantOfPointM2.add((dominant2 != null) ? dominant2.variant : null);
						if (variantOfPointM2.get(k) != variantOfPointM2.get(0)) {
							areThereSeveralVariants2 = true;
						}
					}
					
					if (!areThereSeveralVariants1) {
						double currentDistance = 0;
						for (int k = 0; k < locationsInSector.size(); k++) {
							double tmp = currentLocation.getLatLong().calculateDistanceTo(locationsInSector.get(k).getLatLong());
							if (currentDistance <= tmp) {
								currentDistance = tmp;
							}
							
						}
						distanceMap1.add(currentDistance);
					} else {
						AreaClassMap.VariantDensityResult currentDominant = map1.getDominantVariantAndDensity(currentLocation);
						Variant currentVariant = (currentDominant != null) ? currentDominant.variant : null;
						double currentDistance = Double.POSITIVE_INFINITY;
						for (int k = 0; k < locationsInSector.size(); k++) {
							if (variantOfPointM1.get(k) == currentVariant || (variantOfPointM1.get(k) != null && variantOfPointM1.get(k).equals(currentVariant))) {
								// nothing
							} else {
								double tmp = currentLocation.getLatLong().calculateDistanceTo(locationsInSector.get(k).getLatLong());
								if (currentDistance > tmp) {
									currentDistance = tmp;
								}
							}
							
						}
						distanceMap1.add(currentDistance);
					}
					
					if (!areThereSeveralVariants2) {
						double currentDistance = 0;
						for (int k = 0; k < locationsInSector.size(); k++) {
							double tmp = currentLocation.getLatLong().calculateDistanceTo(locationsInSector.get(k).getLatLong());
							if (currentDistance <= tmp) {
								currentDistance = tmp;
							}
							
						}
						distanceMap2.add(currentDistance);
					} else {
						AreaClassMap.VariantDensityResult currentDominant = map2.getDominantVariantAndDensity(currentLocation);
						Variant currentVariant = (currentDominant != null) ? currentDominant.variant : null;
						double currentDistance = Double.POSITIVE_INFINITY;
						for (int k = 0; k < locationsInSector.size(); k++) {
							if (variantOfPointM2.get(k) == currentVariant || (variantOfPointM2.get(k) != null && variantOfPointM2.get(k).equals(currentVariant))) {
								// nothing
							} else {
								double tmp = currentLocation.getLatLong().calculateDistanceTo(locationsInSector.get(k).getLatLong());
								if (currentDistance > tmp) {
									currentDistance = tmp;
								}
							}
						}
						distanceMap2.add(currentDistance);
					}
				}
			}
			
			ArrayList<Double> absoluteDifference = new ArrayList<Double>();
			for (int i = 0; i < distanceMap1.size(); i++) {
				absoluteDifference.add(Math.abs(distanceMap1.get(i) - distanceMap2.get(i)));
			}
			
			double result = 0;
			for (int i = 0; i < absoluteDifference.size(); i++) {
				result = result + absoluteDifference.get(i);
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
	
	/**
	 * The method calculates all points which are in the <code>whichSector</code>'s
	 * sector. The first sector is always the first sector which is at the right of
	 * the line which has the direction latitude:1 longitute:0 and goes through the
	 * point <code>currentPoint</code>. The second sector is the sector which is
	 * clockwise beneath the first sector.
	 * 
	 * @param map              the considered AreaClassMap
	 * @param currentLocation  any location which will be the center of the sectors
	 * @param directionsOfSectorBoundaries a list which consists of all the sectors
	 *                         bounding lines; more precisely, the list contains only
	 *                         directions and the associated lines are considered to go
	 *                         through the point <code>currentPoint</code>
	 * @param whichSector      the index of the Sector
	 * @return a list which consists of all points inside of the <code>whichSector</code>'s sector
	 */
	private ArrayList<AggregatedLocation> getLocationsInSector(ArrayList<AggregatedLocation> map, AggregatedLocation currentLocation, ArrayList<LatLong> directionsOfSectorBoundaries,
			int whichSector) {
		ArrayList<AggregatedLocation> result = new ArrayList<AggregatedLocation>();
		
		// If there is only 1 sector, every point has to be inside of it.
		if (d == 1) {
			return map;
		}
		if (d == 2) {
			for (int i = 0; i < map.size(); i++) {
				if (whichSector == 0) {
					if (map.get(i).getLatLong().getLongitude() > currentLocation.getLatLong().getLongitude()) {
						result.add(map.get(i));
					}
				} else { // That means whichSector=1
					if (!(map.get(i).getLatLong().getLongitude() > currentLocation.getLatLong().getLongitude())) {
						result.add(map.get(i));
					}
				}
			}
			return result;
		}
		LatLong dir1;
		LatLong dir2;
		dir1 = directionsOfSectorBoundaries.get(whichSector);
		if (whichSector == d - 1) {
			dir2 = directionsOfSectorBoundaries.get(0);
		} else {
			dir2 = directionsOfSectorBoundaries.get(whichSector + 1);
		}
		
		double angle = 2.0 * Math.PI / ((double) d);
		
		for (int i = 0; i < map.size(); i++) {
			double currentNorm = Math.sqrt(Math.pow(map.get(i).getLatLong().getLatitude() - currentLocation.getLatLong().getLatitude(), 2.0)
					+ Math.pow(map.get(i).getLatLong().getLongitude() - currentLocation.getLatLong().getLongitude(), 2.0));
			LatLong currentDirection;
			if (currentNorm != 0) {
				currentDirection = new LatLong((map.get(i).getLatLong().getLatitude() - currentLocation.getLatLong().getLatitude()) / currentNorm, (map.get(i)
						.getLatLong().getLongitude() - currentLocation.getLatLong().getLongitude())
						/ currentNorm);
				if (Math.acos(currentDirection.getLatitude() * dir1.getLatitude() + currentDirection.getLongitude() * dir1.getLongitude()) < angle
						&& Math.acos(currentDirection.getLatitude() * dir2.getLatitude() + currentDirection.getLongitude() * dir2.getLongitude()) < angle) {
					
					result.add(map.get(i));
				}
			} else {
				result.add(map.get(i));
			}
			
		}
		
		return result;
	}
	
	/**
	 * The method calculates all directions which separate the earth for a given
	 * point into <code>d</code> equal-sized sectors.
	 * 
	 * @return a list containing every direction
	 */
	private ArrayList<LatLong> getSectorBoundaries() {
		ArrayList<LatLong> result = new ArrayList<LatLong>();
		
		// If d=2 there are only two Sectors
		if (d == 1) {
			return result;
		}
		if (d == 2) {
			result.add(new LatLong(1.0, 0.0));
			return result;
		} else {
			for (int i = 0; i < d; i++) {
				if (i <= ((double) ((double) d) / 2.0)) {
					result.add(new LatLong(Math.cos((2.0 * Math.PI / ((double) d)) * ((double) i)), Math.sqrt(1 - Math.pow(
							Math.cos((2.0 * Math.PI / ((double) d)) * ((double) i)), 2.0))));
				} else {
					result.add(new LatLong(Math.cos((2.0 * Math.PI / ((double) d)) * ((double) i)), -Math.sqrt(1 - Math.pow(
							Math.cos((2.0 * Math.PI / ((double) d)) * ((double) i)), 2.0))));
				}
				
			}
		}
		
		return result;
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
		return "sector_method";
	}
	
}
