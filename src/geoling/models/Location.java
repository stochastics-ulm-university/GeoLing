package geoling.models;

import geoling.util.LatLong;
import geoling.util.vendor.HumaneStringComparator;

/**
 * A location where informants live, i.e., a city.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Location extends ExtendedModel implements Comparable<Location> {
	
	/** Cached <code>LatLong</code> object. */
	protected LatLong latLong = null;
	
	static {
		validatePresenceOf("name");
		validateNumericalityOf("latitude", "longitude");
	}
	
	/**
	 * Returns the geographic coordinates of this location.
	 * 
	 * @return the geographic coordinates
	 */
	public LatLong getLatLong() {
		if (latLong == null) {
			latLong = new LatLong(this.getDouble("latitude"), this.getDouble("longitude"));
		}
		return latLong;
	}
	
	public int compareTo(Location other) {
		return HumaneStringComparator.DEFAULT.compare(this.getString("name"), other.getString("name"));
	}

}