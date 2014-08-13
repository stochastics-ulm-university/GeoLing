package geoling.maps.distances;

import java.util.Arrays;

import org.javalite.activejdbc.Model;

import geoling.models.Location;
import geoling.util.LatLong;

/**
 * Geographical distance measure for coordinates/locations.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class GeographicalDistance implements DistanceMeasure {
	
	/**
	 * Internal cache that caches the distance for every pair of locations,
	 * used the IDs of the location objects as indices (minus <code>minId</code>),
	 * it is a strictly lower triangular matrix.
	 */
	protected float[][] distancesCache;
	
	/** Smallest location ID, used for shifting of indices in <code>distancesCache</code>. */
	protected int minId;
	
	/**
	 * Constructs a new object for the computation of the geographical distance.
	 * 
	 * @param useCache  if <code>true</code>, then all computed distances will be cached
	 */
	public GeographicalDistance(boolean useCache) {
		distancesCache = null;
		minId = 0;
		
		if (useCache) {
			Model obj = Location.findAll().orderBy("id DESC").limit(1).get(0);
			if (obj != null) {
				int maxId = obj.getLongId().intValue();
				
				Location distanceWithMinId = (Location)Location.findAll().orderBy("id ASC").limit(1).get(0);
				minId = distanceWithMinId.getLongId().intValue();
				
				distancesCache = new float[maxId-minId+1][];
				for (int i = 0; i < distancesCache.length; i++) {
					distancesCache[i] = new float[i];
					Arrays.fill(distancesCache[i], Float.NaN);
				}
			}
		}
	}
	
	/**
	 * Constructs a new object for the computation of the geographical distance.
	 */
	public GeographicalDistance() {
		this(true);
	}
	
	/**
	 * Returns an identification string for this distance measure.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		return getStaticIdentificationString();
	}
	
	/**
	 * Returns an identification string for this distance measure.
	 * 
	 * @return the identification string
	 */
	public static String getStaticIdentificationString() {
		return "geographic";
	}
	
	/**
	 * Computes the geographical distance between two geographical coordinates.
	 * 
	 * @param latLong1  the first geographical coordinate
	 * @param latLong2  the second geographical coordinate
	 * @return the distance in kilometres
	 */
	public double getDistance(LatLong latLong1, LatLong latLong2) {
		return latLong1.calculateDistanceTo(latLong2);
	}
	
	/**
	 * Computes the geographical distance between two locations.
	 * 
	 * @param location1  the first location
	 * @param location2  the second location
	 * @return the distance in kilometres
	 */
	public double getDistance(Location location1, Location location2) {
		int id1 = location1.getLongId().intValue();
		int id2 = location2.getLongId().intValue();
		if ((distancesCache == null) || (id1 < 1) || (id2 < 1)) {
			return getDistance(location1.getLatLong(), location2.getLatLong());
		} else {
			double result;
			if (id1 == id2) {
				result = 0.0;
			} else if (id1 < id2) {
				result = (float)distancesCache[id2-minId][id1-minId];
			} else {
				result = (float)distancesCache[id1-minId][id2-minId];
			}
			if (Double.isNaN(result)) {
				result = getDistance(location1.getLatLong(), location2.getLatLong());
				// Note that it is possible that several threads are using this method
				// at the same time, but nothing bad can happen: in the worst case,
				// we compute a distance two times. The assignment of float values
				// is atomic, therefore unsynchronized reading is no problem. 
				if (id1 < id2) {
					distancesCache[id2-minId][id1-minId] = (float)result;
				} else {
					distancesCache[id1-minId][id2-minId] = (float)result;
				}
			}
			return result;
		}
	}
	
}
