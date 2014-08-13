package geoling.maps.distances;

import java.util.Arrays;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.RowListenerAdapter;

import geoling.models.Distance;
import geoling.models.Location;
import geoling.models.LocationDistance;
import geoling.util.LatLong;

/**
 * Database-based distance measure for locations, requires precomputed
 * distance values for every pair of locations.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class PrecomputedDistance implements DistanceMeasure {
	
	public static class PrecomputedDistanceNotFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
	
	public static class VirtualLocationHasNoPrecomputedDistanceException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
	
	/** The distance measure identification used in the database. */
	protected Distance distance;
	
	/**
	 * Internal cache that caches the distance for every pair of locations,
	 * uses the IDs of the location objects as indices (minus <code>minId</code>),
	 * it is a strictly lower triangular matrix.
	 * Note that this array may be large and will probably have many unused entries,
	 * but the performance is better.
	 */
	protected float[][] distancesCache;
	
	/** Smallest location ID, used for shifting of indices in <code>distancesCache</code>. */
	protected int minId;
	
	/**
	 * Constructs a distance measure object that fetches the precomputed
	 * distance values from the database.
	 * 
	 * @param distance  the distance measure
	 * @param useCache  if <code>true</code>, then all distances are loaded into an internal cache
	 */
	public PrecomputedDistance(Distance distance, boolean useCache) {
		this.distance = distance;
		
		if (useCache) {
			Location distanceWithMinId = (Location)Location.findAll().orderBy("id ASC").limit(1).get(0);
			minId = distanceWithMinId.getLongId().intValue();
			
			Location distanceWithMaxId = (Location)Location.findAll().orderBy("id DESC").limit(1).get(0);
			int maxId = distanceWithMaxId.getLongId().intValue();
			
			distancesCache = new float[maxId-minId+1][];
			for (int i = 0; i < distancesCache.length; i++) {
				distancesCache[i] = new float[i];
				Arrays.fill(distancesCache[i], Float.NaN);
			}
			
			// We do not use the Distance model because we have to avoid creating such a large number of objects.
			// In this simple case, we process the rows directly.
			Base.find("SELECT location_id1, location_id2, distance FROM location_distances"+
			          " WHERE distance_id = ?", distance.getId()).with(new RowListenerAdapter() {
				public void onNext(java.util.Map<String,Object> row) {
					int id1 = ((Number)row.get("location_id1")).intValue();
					int id2 = ((Number)row.get("location_id2")).intValue();
					float dist = ((Number)row.get("distance")).floatValue();
					if (id1 >= id2) {
						throw new RuntimeException("Table \"location_distances\" is required to have the smaller ID id location_id1, the larger ID in location_id2!");
					}
					distancesCache[id2-minId][id1-minId] = dist;
				}
			});
		} else {
			distancesCache = null;
			minId = 0;
		}
	}
	
	/**
	 * Returns an identification string for this distance measure.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		return this.distance.getString("identification");
	}
	
	/**
	 * Returns the distance object from the database for this distance measure.
	 * 
	 * @return the distance object from the database
	 */
	public Distance getDistance() {
		return this.distance;
	}
	
	/**
	 * Because there is no precomputed distance for two arbitrary coordinates,
	 * this method always throws an exception.
	 * 
	 * @param latLong1  the first geographical coordinate
	 * @param latLong2  the second geographical coordinate
	 * @return the distance
	 * @throws LatLongNotSupportedException if the distance measure only supports the distance between locations
	 */
	public double getDistance(LatLong latLong1, LatLong latLong2) {
		throw new LatLongNotSupportedException();
	}
	
	/**
	 * Fetches the precomputed distance between two locations.
	 * 
	 * @param location1  the first location
	 * @param location2  the second location
	 * @return the distance
	 * @throws PrecomputedDistanceNotFoundException if the distance was not found
	 * @throws VirtualLocationHasNoPrecomputedDistanceException if one of the locations is virtual
	 */
	public double getDistance(Location location1, Location location2) {
		double result;
		int id1 = location1.getLongId().intValue();
		int id2 = location2.getLongId().intValue();
		if (distancesCache == null) {
			Double obj = LocationDistance.getDistance(this.distance, location1, location2);
			if (obj == null) {
				result = Double.NaN;
			} else {
				result = obj.doubleValue();
			}
		} else {
			if (id1 == id2) {
				result = 0.0;
			} else if (id1 < id2) {
				result = distancesCache[id2-minId][id1-minId];
			} else {
				result = distancesCache[id1-minId][id2-minId];
			}
		}
		if (Double.isNaN(result)) {
			throw new PrecomputedDistanceNotFoundException();
		}
		return result;
	}
	
}
