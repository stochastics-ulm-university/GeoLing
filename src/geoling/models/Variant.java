package geoling.models;

import geoling.util.vendor.HumaneStringComparator;

/**
 * A variant, belongs to exactly one map.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Variant extends ExtendedModel implements Comparable<Variant> {
	
	static {
		validatePresenceOf("map_id", "name");
	}
	
	public int compareTo(Variant other) {
		return HumaneStringComparator.DEFAULT.compare(this.getString("name"), other.getString("name"));
	}
	
}