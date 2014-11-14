package geoling.maps.distances;

import geoling.models.Distance;
import geoling.models.Group;
import geoling.models.Level;
import geoling.models.Location;

import org.javalite.activejdbc.LazyList;

/**
 * Linguistic distance measure for locations, requires precomputed
 * distance values for every pair of locations.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @see "S. Pickl, A. Spettl, S. Pröll, S. Elspaß, W. König and V. Schmidt,
 *       Linguistic distances in dialectometric intensity estimation.
 *       Journal of Linguistic Geography 2 (2014), 25-40."
 */
public class LinguisticDistance extends PrecomputedDistance {
	
	/** The level used to compute the distances. */
	private Level level;
	
	/** The group with the maps used to compute the distances. */
	private Group group;
	
	/**
	 * Constructs a distance measure object that fetches the precomputed
	 * linguistic distance values from the database.
	 * 
	 * @param level    the level used to compute the distances
	 * @param group    the group with the maps used to compute the distances
	 * @param useCache if <code>true</code>, then all distances are loaded into an internal cache
	 */
	public LinguisticDistance(Level level, Group group, boolean useCache) {
		super(findDistanceByIdentificationString(getStaticIdentificationString(level, group)), useCache);
		this.level = level;
		this.group = group;
	}
	
	/**
	 * Returns the level used to compute the distances.
	 * 
	 * @return the level
	 */
	public Level getLevel() {
		return this.level;
	}
	
	/**
	 * Returns the group with the maps used to compute the distances.
	 * 
	 * @return the group
	 */
	public Group getGroup() {
		return this.group;
	}
	
	/**
	 * Fetches the (precomputed) linguistic distance between two locations.
	 * 
	 * @param location1  the first location
	 * @param location2  the second location
	 * @return the distance
	 * @throws PrecomputedDistanceNotFoundException if the distance was not found
	 */
	public double getDistance(Location location1, Location location2) {
		return super.getDistance(location1, location2);
	}
	
	/**
	 * Returns the identification string used in the database.
	 * 
	 * @param level  the level used to compute the distances
	 * @param group  the group with the maps used to compute the distances
	 * @return the identification string
	 */
	public static String getStaticIdentificationString(Level level, Group group) {
		String result = "linguistic";
		if (level != null) {
			result += ":level_id=" + level.getId();
		}
		if (group != null) {
			result += ":group_id=" + group.getId();
		}
		return result;
	}
	
	/**
	 * Returns the distance measure object from the database for the given identification.
	 * 
	 * @param identification the identification string
	 * @return the distance measure object from the database
	 */
	public static Distance findDistanceByIdentificationString(String identification) {
		LazyList<Distance> list = Distance.find("identification = ?", identification);
		if (list.size() == 1) {
			return list.get(0);
		} else {
			throw new IllegalArgumentException("Distance with identification \""+identification+"\" not found!");
		}
	}
	
}