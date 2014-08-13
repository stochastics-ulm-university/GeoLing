package geoling.maps.util;

import geoling.maps.projection.KilometresProjection;
import geoling.models.Location;
import geoling.util.DoubleBox;
import geoling.util.LatLong;
import geoling.util.PointsGrid2D;
import geoling.util.Utilities;
import geoling.util.sim.grain.ObjectMarkedPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Class to find adjacent locations, inserts all locations into a
 * grid.
 * Because the grid uses the Euclidean distance, internally all geographical
 * coordinates are converted to kilometres (which is only an approximation).
 *  
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class LocationGrid {
	
	/** The grid with marked points, where the mark is the location object. */
	private PointsGrid2D<ObjectMarkedPoint> grid;
	
	/** The projection object for the coordinates. */
	private KilometresProjection kilometresProjection;
	
	/**
	 * Constructs a new location grid for the given locations.
	 * 
	 * @param locations  the locations
	 */
	public LocationGrid(Collection<Location> locations) {
		this.kilometresProjection = new KilometresProjection(locations);
		
		LinkedList<ObjectMarkedPoint> points = new LinkedList<ObjectMarkedPoint>();
		double[] lowCorner  = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
		double[] highCorner = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
		for (Location location : locations) {
			double[] coord = this.kilometresProjection.projectLatLong(location.getLatLong());
			ObjectMarkedPoint point = new ObjectMarkedPoint(coord, location);
			points.add(point);
			lowCorner[0] = Math.min(lowCorner[0], coord[0]);
			lowCorner[1] = Math.min(lowCorner[1], coord[1]);
			highCorner[0] = Math.max(highCorner[0], coord[0]);
			highCorner[1] = Math.max(highCorner[1], coord[1]);
		}
		lowCorner[0]  -= Utilities.EPS;
		lowCorner[1]  -= Utilities.EPS;
		highCorner[0] += Utilities.EPS;
		highCorner[1] += Utilities.EPS;
		this.grid = new PointsGrid2D<ObjectMarkedPoint>(points, new DoubleBox(lowCorner, highCorner));
	}
	
	/**
	 * Detects all locations in a given maximum distance.
	 * Note that the (unavoidable) distortion of the coordinates in the plane is
	 * corrected with a safety margin of 20% for the Euclidean distance, then the
	 * real geographical distance is used in addition. 
	 * 
	 * @param latLong    the geographical coordinates
	 * @param kilometres the maximum distance in kilometres
	 * @param fuzzy      determines whether it is allowed that locations with a larger
	 *                   distance are also contained in the result
	 * @return a list of detected locations
	 */
	public ArrayList<Location> findLocationsInDistance(LatLong latLong, double kilometres, boolean fuzzy) {
		ObjectMarkedPoint point = new ObjectMarkedPoint(this.kilometresProjection.projectLatLong(latLong), null);
		List<ObjectMarkedPoint> neighbours = this.grid.findNeighboursInDistance(point, kilometres*1.2, false);
		ArrayList<Location> result = new ArrayList<Location>(neighbours.size());
		for (ObjectMarkedPoint neighbour : neighbours) {
			Location other = (Location)neighbour.getValue();
			if (fuzzy || latLong.calculateDistanceTo(other.getLatLong()) < kilometres+Utilities.EPS) {
				result.add(other);
			}
		}
		return result;
	}
	
	/**
	 * Detects the location nearest to the given geographical coordinates.
	 * 
	 * @param latLong  the geographical coordinates
	 * @return the location
	 */
	public Location findNearestLocation(LatLong latLong) {
		ObjectMarkedPoint point = new ObjectMarkedPoint(this.kilometresProjection.projectLatLong(latLong), null);
		ObjectMarkedPoint obj = this.grid.findNearestNeighbour(point);
		if (obj == null) {
			return null;
		} else {
			return (Location)obj.getValue();
		}
	}
	
}
