package geoling.maps.projection;

import geoling.util.LatLong;
import geoling.util.Utilities;
import geoling.util.geom.Geometry2D;
import geoling.util.sim.grain.ConvexPolytope;
import geoling.util.sim.grain.LineSegment;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;

/**
 * An abstract class for the projection of geographical coordinates to
 * a plane and vice versa.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Map_projection">Wikipedia: Map projection</a>
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public abstract class MapProjection {
	
	/**
	 * Projects the given geographical coordinates to the plane according
	 * to the principle of this map projection type.
	 * 
	 * @param latLong  the geographical coordinates
	 * @return the x- and y-coordinate of the projected point
	 */
	public abstract double[] projectLatLong(double[] latLong);
	
	/**
	 * Projects the given x-y-coordinates back to geographical coordinates.
	 * 
	 * @param xyCoord  the x- and y-coordinate
	 * @return the geographical coordinates of the projected point
	 */
	public abstract double[] revertProjection(double[] xyCoord);
	
	/**
	 * Implements a check for equality of two map projection objects.
	 * 
	 * @param other  the second object
	 * @return <code>true</code> if the two objects are equal
	 */
	public abstract boolean isSimilar(MapProjection other);
	
	/**
	 * Projects the given geographical coordinates to the
	 * plane according to the principle of this map projection type.
	 * 
	 * @param latLong  the geographical coordinates
	 * @return the x-y-coordinates
	 */
	public double[] projectLatLong(LatLong latLong) {
		return projectLatLong(new double[] { latLong.getLatitude(), latLong.getLongitude() });
	}
	
	/**
	 * Projects the object with geographical coordinates to the
	 * plane according to the principle of this map projection type.
	 * 
	 * @param point  the point with geographical coordinates
	 * @return the point with x-y-coordinates
	 */
	public Point projectLatLong(Point point) {
		return new Point(projectLatLong(point.getCoordinates()));
	}
	
	/**
	 * Projects the object with geographical coordinates to the
	 * plane according to the principle of this map projection type.
	 * 
	 * @param point  the point with geographical coordinates
	 * @return the point with x-y coordinates
	 */
	public Geometry2D.Point projectLatLong(Geometry2D.Point point) {
		double[] coord = projectLatLong(new double[] { point.x, point.y });
		return new Geometry2D.Point(coord[0], coord[1]);
	}
	
	/**
	 * Projects the object with geographical coordinates to the
	 * plane according to the principle of this map projection type.
	 * 
	 * @param polytope  the polytope with geographical coordinates
	 * @return the polytope with x-y coordinates (kilometres)
	 */
	public ConvexPolytope projectLatLong(ConvexPolytope polytope) {
		Point[] pointsGeo = polytope.getVertices();
		Point[] result = new Point[pointsGeo.length];
		for (int i = 0; i < pointsGeo.length; i++) {
			result[i] = projectLatLong(pointsGeo[i]);
		}
		return new ConvexPolytope(Utilities.ensureCounterClockwise(result), polytope.isFilled());
	}
	
	/**
	 * Projects the object with geographical coordinates to the
	 * plane according to the principle of this map projection type.
	 * 
	 * @param polytope  the polytope with geographical coordinates
	 * @return the polytope with x-y coordinates
	 */
	public Polytope projectLatLong(Polytope polytope) {
		Point[] pointsGeo = polytope.getVertices();
		Point[] result = new Point[pointsGeo.length];
		for (int i = 0; i < pointsGeo.length; i++) {
			result[i] = projectLatLong(pointsGeo[i]);
		}
		return new Polytope(Utilities.ensureCounterClockwise(result), polytope.isFilled());
	}
	
	/**
	 * Projects the object with geographical coordinates to the
	 * plane according to the principle of this map projection type.
	 * 
	 * @param ls  the line segment with geographical coordinates
	 * @return the line segment with x-y coordinates
	 */
	public LineSegment projectLatLong(LineSegment ls) {
		double[] p1 = projectLatLong(ls.getStartPoint()).getCoordinates();
		double[] p2 = projectLatLong(ls.getEndPoint()).getCoordinates();
		return new LineSegment(p1[0], p1[1], p2[0], p2[1], true);
	}
	
	/**
	 * Projects the object with x-y-coordinates back to geographical coordinates.
	 * 
	 * @param point  the point with x-y coordinates
	 * @return the point with geographical coordinates
	 */
	public Point revertProjection(Point point) {
		return new Point(revertProjection(point.getCoordinates()));
	}
	
	/**
	 * Projects the object with x-y-coordinates back to geographical coordinates.
	 * 
	 * @param point  the point with x-y coordinates
	 * @return the point with geographical coordinates
	 */
	public Geometry2D.Point revertProjection(Geometry2D.Point point) {
		double[] coord = revertProjection(new double[] { point.x, point.y });
		return new Geometry2D.Point(coord[0], coord[1]);
	}
	
	/**
	 * Projects the object with x-y-coordinates back to geographical coordinates.
	 * 
	 * @param polytope  the polytope with x-y coordinates
	 * @return the polytope with geographical coordinates
	 */
	public ConvexPolytope revertProjection(ConvexPolytope polytope) {
		Point[] pointsXY = polytope.getVertices();
		Point[] result = new Point[pointsXY.length];
		for (int i = 0; i < pointsXY.length; i++) {
			result[i] = revertProjection(pointsXY[i]);
		}
		return new ConvexPolytope(Utilities.ensureCounterClockwise(result), polytope.isFilled());
	}
	
	/**
	 * Projects the object with x-y-coordinates back to geographical coordinates.
	 * 
	 * @param polytope  the polytope with x-y coordinates
	 * @return the polytope with geographical coordinates
	 */
	public Polytope revertProjection(Polytope polytope) {
		Point[] pointsXY = polytope.getVertices();
		Point[] result = new Point[pointsXY.length];
		for (int i = 0; i < pointsXY.length; i++) {
			result[i] = revertProjection(pointsXY[i]);
		}
		return new Polytope(Utilities.ensureCounterClockwise(result), polytope.isFilled());
	}
	
	/**
	 * Projects the object with x-y-coordinates back to geographical coordinates.
	 * 
	 * @param ls  the line segment with x-y coordinates
	 * @return the line segment with geographical coordinates
	 */
	public LineSegment revertProjection(LineSegment ls) {
		double[] p1 = revertProjection(ls.getStartPoint()).getCoordinates();
		double[] p2 = revertProjection(ls.getEndPoint()).getCoordinates();
		return new LineSegment(p1[0], p1[1], p2[0], p2[1], true);
	}
	
}
