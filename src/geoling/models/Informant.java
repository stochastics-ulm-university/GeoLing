package geoling.models;

import geoling.util.vendor.HumaneStringComparator;

/**
 * An informant that has been interviewed.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Informant extends ExtendedModel implements Comparable<Informant> {
	
	static {
		validatePresenceOf("location_id", "name");
	}
	
	public int compareTo(Informant other) {
		return HumaneStringComparator.DEFAULT.compare(this.getString("name"), other.getString("name"));
	}
	
}