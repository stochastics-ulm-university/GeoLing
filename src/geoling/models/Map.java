package geoling.models;

import geoling.util.vendor.HumaneStringComparator;

/**
 * A single map that consists of many variants.
 * The map may belong to categories, see the join table model
 * <code>CategoriesMaps</code>.  
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Map extends ExtendedModel implements Comparable<Map> {
	
	static {
		validatePresenceOf("name");
	}
	
	public int compareTo(Map other) {
		return HumaneStringComparator.DEFAULT.compare(this.getString("name"), other.getString("name"));
	}

}