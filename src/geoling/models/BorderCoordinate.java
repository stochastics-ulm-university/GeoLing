package geoling.models;

import geoling.util.LatLong;

/**
 * A border coordinate object defines a geographical point on the border,
 * the border itself is given by a series of points in counterclockwise order.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class BorderCoordinate extends ExtendedModel {
	
	/** Cached ID object. */
	protected Object cachedId = null;
	
	static {
		validatePresenceOf("border_id");
		validateNumericalityOf("order_index").greaterThan(0);
		validateNumericalityOf("latitude", "longitude");
	}
	
	/**
	 * Returns the geographic coordinates of this border point.
	 * 
	 * @return the geographic coordinates
	 */
	public LatLong getLatLong() {
		return new LatLong(this.getDouble("latitude"), this.getDouble("longitude"));
	}
	
}