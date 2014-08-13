package geoling.util;

import java.util.ArrayList;
import java.util.Collection;

import geoling.locations.util.AggregatedLocation;
import geoling.models.Location;
import geoling.util.geom.Geometry2D;
import geoling.util.sim.grain.Point;

/**
 * Class for object conversions required for plotting maps.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Conversion {
	
	/**
	 * Converts the given <code>LatLong</code> to a <code>geoling.util.sim.grain.Point</code>.
	 * 
	 * @param latLong  the coordinates
	 * @return the converted point
	 */
	public static Point toSimGrainPoint(LatLong latLong) {
		return new Point(new double[] { latLong.getLatitude(), latLong.getLongitude() });
	}
	
	/**
	 * Converts the given <code>LatLong</code> coordinates to a <code>Geometry2D.Point</code>.
	 * 
	 * @param latLong  the coordinates
	 * @return the converted point
	 */
	public static Geometry2D.Point toGeom2DPoint(LatLong latLong) {
		return new Geometry2D.Point(latLong.getLatitude(), latLong.getLongitude());
	}
	
	/**
	 * Returns a list of the locations contained in the given aggregated locations.
	 * 
	 * @param aggregatedLocations  the (aggregated) locations
	 * @return the list of contained locations
	 */
	public static ArrayList<Location> toLocations(Collection<AggregatedLocation> aggregatedLocations) {
		ArrayList<Location> locations = new ArrayList<Location>(aggregatedLocations.size());
		for (AggregatedLocation location : aggregatedLocations) {
			locations.addAll(location.getLocations());
		}
		return locations;
	}
	
}
