package geoling.maps.util;

import java.util.Collection;

import org.javalite.activejdbc.LazyList;

import geoling.locations.util.AggregatedLocation;
import geoling.models.*;
import geoling.util.Conversion;
import geoling.util.DoubleBox;
import geoling.util.Utilities;
import geoling.util.geom.Geometry2D;
import geoling.util.geom.convexhull.GiftWrapping2D;
import geoling.util.sim.grain.ConvexPolytope;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;

/**
 * Manual/automatic determination of the border of maps.
 *  
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class MapBorder {
	
	/**
	 * Constructs a polygon object for the given border from the database.
	 * 
	 * @param border  the border object from the database
	 * @return a (not necessarily convex) polygon
	 */
	public static Polytope loadFromDatabase(Border border) {
		LazyList<BorderCoordinate> coordinates = border.getAll(BorderCoordinate.class).orderBy("order_index");
		Point[] pointArray = new Point[coordinates.size()];
		int i = 0;
		for (BorderCoordinate coord : coordinates) {
			pointArray[i] = Conversion.toSimGrainPoint(coord.getLatLong());
			i++;
		}
		return new Polytope(pointArray);
	}
	
	/**
	 * Computes the convex hull of the given polytope.
	 * 
	 * @param polytope  the (not necessarily convex) polytope
	 * @return a convex polygon
	 */
	public static ConvexPolytope getConvexHull(Polytope polytope) {
		Point[] vertices = polytope.getVertices();
		Geometry2D.Point[] pointArray = new Geometry2D.Point[vertices.length];
		for (int i = 0; i < vertices.length; i++) {
			pointArray[i] = vertices[i].toGeo2DPoint();
		}
		return GiftWrapping2D.getConvexHull(pointArray, true).toPolytope(true);
	}
	
	/**
	 * Computes the convex hull of all geographic coordinates of the given locations.
	 * 
	 * @param locations  the locations
	 * @return a convex polygon
	 */
	public static ConvexPolytope getConvexHull(Collection<Location> locations) {
		Geometry2D.Point[] pointArray = new Geometry2D.Point[locations.size()];
		int i = 0;
		for (Location location : locations) {
			pointArray[i] = Conversion.toGeom2DPoint(location.getLatLong());
			i++;
		}
		return GiftWrapping2D.getConvexHull(pointArray, true).toPolytope(true);
	}
	
	/**
	 * Computes the convex hull of all geographic coordinates of the given (aggregated) locations.
	 * 
	 * @param aggregatedLocations  the (aggregated) locations
	 * @return a convex polygon
	 */
	public static ConvexPolytope getAggregatedLocationsConvexHull(Collection<AggregatedLocation> aggregatedLocations) {
		Geometry2D.Point[] pointArray = new Geometry2D.Point[aggregatedLocations.size()];
		int i = 0;
		for (AggregatedLocation location : aggregatedLocations) {
			pointArray[i] = Conversion.toGeom2DPoint(location.getLatLong());
			i++;
		}
		return GiftWrapping2D.getConvexHull(pointArray, true).toPolytope(true);
	}
	
	/**
	 * Computes the geographical centre of the given locations.
	 * 
	 * @param locations  the locations
	 * @return the coordinates of the centre
	 */
	public static double[] detectCentreForMapProjection(Collection<Location> locations) {
		return detectCentreForMapProjection(getConvexHull(locations));
	}
	
	/**
	 * Computes the geographical centre of the given border polygon.
	 * 
	 * @param border  the border polygon
	 * @return the coordinates of the centre
	 */
	public static double[] detectCentreForMapProjection(ConvexPolytope border) {
		return border.getCenterOfGravity();
	}
	
	/**
	 * Computes the geographical centre of the given border polygon.
	 * 
	 * @param border  the border polygon
	 * @return the coordinates of the centre
	 */
	public static double[] detectCentreForMapProjection(Polytope border) {
		Geometry2D.Point centre = border.getCenterofGravity();
		return new double[] { centre.x, centre.y };
	}
	
	/**
	 * Computes the min/max values of the vertices of the given border polygon.
	 * Note that in most cases this method should be applied to a polygon
	 * already transformed to kilometres, not in geographical coordinates.
	 * (This is because the latitude influences the transformation ratio to
	 *  kilometres of the longitude.)
	 * 
	 * @param borderPolygon  the border polygon
	 * @return the window given as a <code>DoubleBox</code>
	 */
	public static DoubleBox getWindow(Polytope borderPolygon) {
		double xMin = Double.POSITIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		
		for (Point point : borderPolygon.getVertices()) {
			xMin = Math.min(xMin, point.getCoordinates()[0]);
			yMin = Math.min(yMin, point.getCoordinates()[1]);
			xMax = Math.max(xMax, point.getCoordinates()[0]);
			yMax = Math.max(yMax, point.getCoordinates()[1]);
		}
		
		return new DoubleBox(new double[] { xMin - Utilities.EPS, yMin - Utilities.EPS },
		                     new double[] { xMax + Utilities.EPS, yMax + Utilities.EPS });
	}
	
	/**
	 * Checks whether two borders are the same.
	 * Note that currently we only compare borders by their area and their circumference,
	 * which should be sufficient for our cases.
	 * 
	 * @param border1  the first border
	 * @param border2  the second border
	 * @return <code>true</code> if the two borders are the same
	 */
	public static boolean bordersAreEqual(Polytope border1, Polytope border2) {
		if (border1 == border2) {
			return true;
		}
		if (Utilities.isEqual(border1.getArea(), border2.getArea()) &&
		    Utilities.isEqual(border1.getCircumference(), border2.getCircumference())) {
			return true;
		} else {
			return false;
		}
	}
	
}
