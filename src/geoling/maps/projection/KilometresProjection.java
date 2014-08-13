package geoling.maps.projection;

import geoling.maps.util.MapBorder;
import geoling.models.Location;
import geoling.util.sim.grain.ConvexPolytope;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;

import java.util.Collection;

/**
 * Projection of latitude/longitude to a plane where the units
 * correspond (approximately) to kilometres.
 * This class is used e.g. for the <code>LocationGrid</code>.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class KilometresProjection extends MapProjection {
	
	/** Kilometres one degree of latitude corresponds to. */
	private static final double LAT_KM = 111.3;
	
	/** Radius of earth. */
	private static final double EARTH_RADIUS = 6371.0;
	
	/** 
	 * The centre of the map (in geographical coordinates), it influences the distortion
	 * of areas farther away from the centre.
	 */
	private double[] centre;
	
	/**
	 * Constructs a new projection object for the given map centre.
	 * 
	 * @param centre  the centre of the map (in geographical coordinates), it influences
	 *                the distortion of areas farther away from the centre
	 */
	public KilometresProjection(double[] centre) {
		this.centre = centre;
	}
	
	/**
	 * Constructs a new projection object for the given map set of locations.
	 * 
	 * @param locations  the locations
	 */
	public KilometresProjection(Collection<Location> locations) {
		this(MapBorder.detectCentreForMapProjection(locations));
	}
	
	/**
	 * Constructs a new projection object for the given border polygon.
	 * 
	 * @param border  the border polygon
	 */
	public KilometresProjection(ConvexPolytope border) {
		this(MapBorder.detectCentreForMapProjection(border));
	}
	
	/**
	 * Constructs a new projection object for the given border polygon.
	 * 
	 * @param border  the border polygon
	 */
	public KilometresProjection(Polytope border) {
		this(MapBorder.detectCentreForMapProjection(border));
	}
	
	/**
	 * Projects the given geographical coordinates to the plane with units
	 * in kilometres.
	 * 
	 * @param latLong  the geographical coordinates
	 * @return the x- and y-coordinates in kilometres of the projected point
	 */
	public double[] projectLatLong(double[] latLong) {
		double y = (latLong[0]-centre[0]) * LAT_KM;
		double x = (latLong[1]-centre[1]) * (Math.cos(Math.toRadians(latLong[0])) * 2.0 * Math.PI * EARTH_RADIUS / 360.0);
		return new double[] { x, y };
	}
	
	/**
	 * Projects the given coordinates back to geographical coordinates.
	 * 
	 * @param xyCoord  the x- and y-coordinate in kilometres
	 * @return the geographical coordinates of the projected point
	 */
	public double[] revertProjection(double[] xyCoord) {
		double x = xyCoord[1] / LAT_KM + centre[0];
		double y = xyCoord[0] / (Math.cos(Math.toRadians(x)) * 2.0 * Math.PI * EARTH_RADIUS / 360.0) + centre[1];
		return new double[] { x, y };
	}
	
	/**
	 * Implements a check for equality of two map projection objects.
	 * 
	 * @param other  the second object
	 * @return <code>true</code> if the two objects are equal
	 */
	public boolean isSimilar(MapProjection other) {
		return (this == other) || (other instanceof KilometresProjection) && (new Point(centre).equals(new Point(((KilometresProjection)other).centre)));
	}
	
}
