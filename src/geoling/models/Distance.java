package geoling.models;

import geoling.maps.distances.DistanceMeasure;
import geoling.maps.distances.GeographicalDistance;
import geoling.maps.distances.PrecomputedDistance;

/**
 * A distance object corresponds to a distance measure.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Distance extends ExtendedModel {
	
	static {
		validatePresenceOf("name");
		validateRegexpOf("type", "^(geographic|precomputed)$");
	}
	
	/**
	 * Constructs a distance measure object for this object from
	 * the database.
	 * 
	 * @param useCache  if <code>true</code>, then all distances are loaded into an internal cache
	 */
	public DistanceMeasure getDistanceMeasure(boolean useCache) {
		if (this.getString("type").equals("geographic")) {
			return new GeographicalDistance(useCache);
		} else if (this.getString("type").equals("precomputed")) {
			return new PrecomputedDistance(this, useCache);
		} else {
			throw new RuntimeException("Distance type \""+this.getString("type")+"\" is not known!");
		}
	}
	
}