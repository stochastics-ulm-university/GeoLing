package geoling.util.geom.convexhull;

import java.util.Collection;
import java.util.Vector;

import geoling.util.Utilities;
import geoling.util.geom.Geometry2D;

/**
 * Class for computing convex hulls in 2D using the "gift wrapping" algorithm.
 * 
 * @author Institute of Stochastics, Ulm University
 * @see <a href="http://en.wikipedia.org/wiki/Gift_wrapping_algorithm">Wikipedia: Gift wrapping algorithm</a>
 * @version 1.0, 15.03.2012
 */
public class GiftWrapping2D {
	
	/**
	 * Computes the convex hull of the given points in 2D.
	 * Uses the gift-wrapping algorithm and also deals with collinearity, i.e., only
	 * the extreme points of the convex hull are returned.
	 * 
	 * @param points  the array of points
	 * @return the convex hull as a convex polygon
	 */
	public static Geometry2D.ConvexPolygon getConvexHull(Geometry2D.Point[] points) {
		return getConvexHull(points, true);
	}
	
	/**
	 * Computes the convex hull of the given points in 2D.
	 * Uses the gift-wrapping algorithm and also deals with collinearity, i.e., only
	 * the extreme points of the convex hull are returned.
	 * 
	 * @param points  the list of points
	 * @return the convex hull as a convex polygon
	 */
	public static Geometry2D.ConvexPolygon getConvexHull(Collection<Geometry2D.Point> points) {
		return getConvexHull(points.toArray(new Geometry2D.Point[0]), true);
	}

	/**
	 * Computes the convex hull of the given points in 2D.
	 * Uses the gift-wrapping algorithm and also deals with collinearity, i.e., only
	 * the extreme points of the convex hull are returned.
	 * 
	 * @param points              the list of points
	 * @param cloneExtremalPoints determines whether the resulting polygon
	 *                            should contain copies of the extremal points or not
	 *                            (no cloning is useful if you use a subclass of
	 *                             <code>Geometry2D.Point</code>, where points have
	 *                             some additional information)
	 * @return the convex hull as a convex polygon
	 */
	public static Geometry2D.ConvexPolygon getConvexHull(Collection<Geometry2D.Point> points, boolean cloneExtremalPoints) {
		return getConvexHull(points.toArray(new Geometry2D.Point[0]), cloneExtremalPoints);
	}

	/**
	 * Computes the convex hull of the given points in 2D.
	 * Uses the gift-wrapping algorithm and also deals with collinearity, i.e., only
	 * the extreme points of the convex hull are returned.
	 * 
	 * @param points              the array of points
	 * @param cloneExtremalPoints determines whether the resulting polygon
	 *                            should contain copies of the extremal points or not
	 *                            (no cloning is useful if you use a subclass of
	 *                             <code>Geometry2D.Point</code>, where points have
	 *                             some additional information)
	 * @return the convex hull as a convex polygon
	 */
	public static Geometry2D.ConvexPolygon getConvexHull(Geometry2D.Point[] points, boolean cloneExtremalPoints) {
		if (points.length < 3) {
			throw new IllegalArgumentException("At least three (not collinear) points are required to compute the convex hull!");
		}

		boolean finished = false;

		// the vector of the vertices of the convex hull
		Vector<Geometry2D.Point> vectorofvertices = new Vector<Geometry2D.Point>();

		// get the lowest (left) point
		// (don't use Utilities.getMinPoint, because it generates a copy of the point)
		Geometry2D.Point minpoint = getMinPoint(points);
		vectorofvertices.add(minpoint);
		
		// is there another point with exactly the same y-coordinate?
		// (if yes, then take the one with the highest x-coordinate as next hull point, otherwise it isn't detected)
		Geometry2D.Point minpoint2 = minpoint;
		for (int i = 0; i < points.length; i++) {
			if (points[i].y == minpoint2.y && points[i].x > minpoint2.x) {
				minpoint2 = points[i];
			}
		}
		if (minpoint != minpoint2) {
			vectorofvertices.add(minpoint2);
		}

		// construction of another point to perform computations
		Geometry2D.Point helppoint = new Geometry2D.Point(minpoint.x + 10, minpoint.y);
		Geometry2D.Point lasthullpoint = minpoint;

		// while initial point is not reached again, get the next vertex
		// using the last vertex and the constructed help point
		while (!finished) {
			Geometry2D.Point nextpoint = findNextHullPoint(lasthullpoint, helppoint, points);
			if (minpoint.equals(nextpoint))
				finished = true;
			else {
				vectorofvertices.add(nextpoint);
				helppoint = new Geometry2D.Point(2 * nextpoint.x - lasthullpoint.x, 2 * nextpoint.y - lasthullpoint.y);
				lasthullpoint = nextpoint;
			}
		}

		// construction of the convex hull polygon
		Geometry2D.Point[] array = vectorofvertices.toArray(new Geometry2D.Point[0]);
		Geometry2D.ConvexPolygon polygon = new Geometry2D.ConvexPolygon(array);
		if (cloneExtremalPoints) {
			// the constructor of Geometry2D.ConvexPolygon creates copies of the vertices by default
		} else {
			// ensure that the polygon uses exactly the same objects, not a copy!
			polygon.p = array;
		}
		return polygon; 
	}
	
	/**
	 * Returns the lowest point of an array of points.
	 * 
	 * @param points  the array of points
	 * @return the lowest point, the most left if there is a tie
	 */
	private static Geometry2D.Point getMinPoint(Geometry2D.Point[] points) {
		Geometry2D.Point minpoint = new Geometry2D.Point(Double.MAX_VALUE, Double.MAX_VALUE);
		boolean found = false;
		for (int i = 0; i < points.length; i++) {
			if (points[i].y < minpoint.y) {
				minpoint = points[i];
				found = true;
			} else if (points[i].y == minpoint.y && points[i].x < minpoint.x) {
				minpoint = points[i];
				found = true;
			}
		}
		if (found) {
			return minpoint;
		} else {
			throw new IllegalArgumentException("No lowest point detected!");
		}
	}

	/**
	 * Gets the next vertex of the convex hull.
	 * 
	 * @param lasthullpoint  the last vertex of the convex hull
	 * @param helppoint      a help point on the line through the last vertex and the last
	 *                       but one vertex
	 * @param points         the array of points
	 * @return the next vertex of the convex hull
	 */
	private static Geometry2D.Point findNextHullPoint(Geometry2D.Point lasthullpoint, Geometry2D.Point helppoint, Geometry2D.Point[] points) {
		Geometry2D.Point minpoint = null;
		double minangle = Double.MAX_VALUE;
		double angle = 0;
		double angle1 = 0;
		double angle2 = 0;

		// translating the origin to lasthullpoint
		helppoint.translateBy(-lasthullpoint.x, -lasthullpoint.y);

		// checking the angles between helppoint and all other points in
		// lasthullpoint. The point with the smallest angle is chosen.
		Geometry2D.Point origin = new Geometry2D.Point(0, 0);
		for (int i = 0; i < points.length; i++) {
			Geometry2D.Point candidate = new Geometry2D.Point(points[i]);
			candidate.translateBy(-lasthullpoint.x, -lasthullpoint.y);
			if (Utilities.collinear(candidate, helppoint, origin))
				continue;
			angle1 = candidate.getAngle();
			angle2 = helppoint.getAngle();

			// getAngle() returns negative values for angles greater than Pi.
			// Therefore...
			if (angle1 < 0)
				angle1 = 2 * Math.PI + angle1;
			if (angle2 < 0)
				angle2 = 2 * Math.PI + angle2;

			angle = angle1 - angle2;
			if (angle < 0)
				continue;

			// checking for linear cases, intermediate points are neglected!
			if (Utilities.isEqual(angle, minangle)) {
				if (points[i].distanceTo(lasthullpoint) > minpoint.distanceTo(lasthullpoint)) {
					minpoint = points[i];
				}
			} else if (angle < minangle) {
				minangle = angle;
				minpoint = points[i];
			}
		}
		if (minpoint == null) {
			throw new RuntimeException("Next hull point not found! All points collinear?");
		}
		return minpoint;
	}

}