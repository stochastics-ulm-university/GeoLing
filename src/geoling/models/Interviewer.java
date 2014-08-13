package geoling.models;

import geoling.util.vendor.HumaneStringComparator;

/**
 * An interviewer who conducted interviews.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Interviewer extends ExtendedModel implements Comparable<Interviewer> {
	
	static {
		validatePresenceOf("name");
	}
	
	public int compareTo(Interviewer other) {
		return HumaneStringComparator.DEFAULT.compare(this.getString("name"), other.getString("name"));
	}
	
}