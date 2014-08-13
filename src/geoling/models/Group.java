package geoling.models;

import geoling.util.vendor.HumaneStringComparator;

/**
 * A group is used to define a list of maps that we want to consider.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Group extends ExtendedModel implements Comparable<Group> {
	
	static {
		validatePresenceOf("name");
	}
	
	public int compareTo(Group other) {
		return HumaneStringComparator.DEFAULT.compare(this.getString("name"), other.getString("name"));
	}
	
}