package geoling.models;

import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.BelongsToParents;
import org.javalite.activejdbc.validation.ValidatorAdapter;

/**
 * A location distance object holds a precomputed distance between two locations.
 * Note that we store the distance always for a pair, by definition we
 * use the location with the smaller ID for the field "location_id1" and
 * the location with the bigger ID for the field "location_id2".
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
@BelongsToParents({
@BelongsTo(parent = Distance.class, foreignKeyName = "distance_id"),
@BelongsTo(parent = Location.class, foreignKeyName = "location_id1"),
@BelongsTo(parent = Location.class, foreignKeyName = "location_id2")
})
public class LocationDistance extends ExtendedModel {
	
	public static class LocationIdsValidator extends ValidatorAdapter<LocationDistance> {
		
		public void validate(LocationDistance m) {
			Integer id1 = m.getInteger("location_id1");
			Integer id2 = m.getInteger("location_id2");
			if ((id1 == null) || (id2 == null)) {
				m.addValidator(this, "location_id1 and location_id2 must not be null!");
			}
			
			int cmp = id1.compareTo(id2);
			if (cmp == 0) {
				m.addValidator(this, "location_id1 and location_id2 must not be equal!");
			} else if (cmp > 0) {
				m.addValidator(this, "By definition, location_id1 must contain a smaller ID than location_id2!");
			}
		}
		
	}
	
	static {
		validatePresenceOf("distance_id");
		validateWith(new LocationIdsValidator());
		validateNumericalityOf("distance");
	}
	
	/**
	 * Fetches the precomputed distance object for the two locations and the distance measure
	 * identification from the database.
	 * 
	 * @param distance   the distance measure
	 * @param location1  the first location
	 * @param location2  the second location
	 * @return the distance object or <code>null</code> if there is no precomputed distance object in the database
	 */
	public static LocationDistance getDistanceObject(Distance distance, Location location1, Location location2) {
		Long id1 = location1.getLongId();
		Long id2 = location2.getLongId();
		int cmp = id1.compareTo(id2);
		if (cmp == 0) {
			return null;
		} else if (cmp > 0) {
			Long tmp = id1;
			id1 = id2;
			id2 = tmp;
		}
		LazyList<LocationDistance> list = LocationDistance.find("distance_id = ? AND location_id1 = ? AND location_id2 = ?", distance.getId(), id1, id2);
		try {
			return list.get(0);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	/**
	 * Fetches the precomputed distance for the two locations and the distance measure identification
	 * from the database.
	 * 
	 * @param distance   the distance measure
	 * @param location1  the first location
	 * @param location2  the second location
	 * @return the distance or <code>null</code> if there is no precomputed distance,
	 *         but returns always zero if the two locations are equal
	 */
	public static Double getDistance(Distance distance, Location location1, Location location2) {
		if (location1.equals(location2)) {
			return new Double(0.0);
		} else {
			LocationDistance obj = getDistanceObject(distance, location1, location2);
			if (obj == null) {
				return null;
			} else {
				return obj.getDouble("distance");
			}
		}
	}
	
}