package geoling.util.geom;

import java.lang.reflect.InvocationTargetException;

/**
 * Helper methods for working with different point classes.
 * Currently used by <code>PointsGrid3D</code>. Warning: This class
 * may be removed in the future, given a better solution is found
 * and implemented (for example a common interface for all point
 * classes).
 *
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @version 0.2.2, 09.06.2013
 */
public class PointsHelper {
	
	/**
	 * Fetches the coordinates of the given (point) object.
	 * Required because there is no interface for a generic point:
	 * This method just checks the class of the object and uses
	 * the existing methods of this class - if it is known.
	 * Otherwise a generic approach is tried using the Java reflection API,
	 * but this is slow.
	 * Throws a runtime exception if the coordinates can't be determined.
	 *
	 * @param point  the point
	 * @return the coordinates as an array
	 */
	public static double[] getPointCoordinates(Object point) {
		if (point instanceof geoling.util.sim.grain.Point) {
			return ((geoling.util.sim.grain.Point)point).getCoordinates();
		} else if (point instanceof geoling.util.geom.Geometry2D.Point) {
			return new double[] { ((geoling.util.geom.Geometry2D.Point)point).x,
			                      ((geoling.util.geom.Geometry2D.Point)point).y };
		} else {
			try {
				// try to get the coordinates in a generic way (using the Java reflection API)
				final java.lang.Class<?>[] parameterType = null;
				java.lang.reflect.Method method = point.getClass().getMethod("getCoordinates", parameterType);
				Object[] argument = null;
				return (double[])method.invoke(point, argument);
			} catch (NoSuchMethodException e) {
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
			throw new RuntimeException("Coordinates of the object couldn't be determined!");
		}
	}
	
	/**
	 * Calculates the distance between the given two points, defined by
	 * their coordinates.
	 *
	 * @param coord1  the coordinates of the first point
	 * @param coord2  the coordinates of the second point
	 * @return the distance
	 */
	public static double getDistance(double[] coord1, double[] coord2) {
		return geoling.util.sim.grain.Point.distance(coord1, coord2);
	}
	
	/**
	 * Calculates the distance between the given two points.
	 *
	 * @param point1  the first point
	 * @param point2  the second point
	 * @return the distance
	 */
	public static double getDistance(Object point1, Object point2) {
		return getDistance(getPointCoordinates(point1), getPointCoordinates(point2));
	}
	
	/**
	 * Returns the (numerical) mark of the point.
	 * Throws a runtime exception if there is no (numerical) mark.
	 * 
	 * @param point  the point object
	 * @return the mark as a <code>double</code>
	 */
	public static double getMark(Object point) {
		Object mark = null;
		if (point instanceof geoling.util.sim.grain.MarkedPoint) {
			return ((geoling.util.sim.grain.MarkedPoint)point).getValue();
		} else if (point instanceof geoling.util.sim.grain.ObjectMarkedPoint) {
			mark = ((geoling.util.sim.grain.ObjectMarkedPoint)point).getValue();
		}
		
		if (mark == null) {
			throw new RuntimeException("The point has no mark or the type of the marked point is unknown!");
		} else {
			if (mark instanceof Number) {
				return ((Number)mark).doubleValue();
			} else {
				throw new RuntimeException("The point does not have a numerical mark!");
			}
		}
	}
	
}
