package geoling.maps.projection;

/**
 * Projection of latitude/longitude to a plane using the Mercator projection,
 * which is used by e.g. Google Maps.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Mercator_projection">Wikipedia: Mercator projection</a>
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class MercatorProjection extends MapProjection {
	
	/**
	 * Constructs a new projection object.
	 */
	public MercatorProjection() {
	}
	
	/**
	 * Projects the given geographical coordinates to the plane with units
	 * in kilometres.
	 * 
	 * @param latLong  the geographical coordinates
	 * @return the x- and y-coordinates in kilometres of the projected point
	 */
	public double[] projectLatLong(double[] latLong) {
		double x = Math.toRadians(latLong[1]);
		double y = Math.log(Math.tan(Math.PI/4.0 + 0.5*Math.toRadians(latLong[0])));
		return new double[] { x, y };
	}
	
	/**
	 * Projects the given coordinates back to geographical coordinates.
	 * 
	 * @param xyCoord  the x- and y-coordinate in kilometres
	 * @return the geographical coordinates of the projected point
	 */
	public double[] revertProjection(double[] xyCoord) {
		double latitude  = Math.toDegrees(Math.asin(Math.tanh(xyCoord[1])));
		double longitude = Math.toDegrees(xyCoord[0]);
		return new double[] { latitude, longitude };
	}
	
	/**
	 * Implements a check for equality of two map projection objects.
	 * 
	 * @param other  the second object
	 * @return <code>true</code> if the two objects are equal
	 */
	public boolean isSimilar(MapProjection other) {
		return (this == other) || (other instanceof MercatorProjection);
	}
	
}
