package geoling.models;

import geoling.util.vendor.HumaneStringComparator;

/**
 * A level, different levels are used to aggregate variants to certain criteria.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Level extends ExtendedModel implements Comparable<Level> {
	
	static {
		validatePresenceOf("name");
	}
	
	public int compareTo(Level other) {
		return HumaneStringComparator.DEFAULT.compare(this.getString("name"), other.getString("name"));
	}
	
}