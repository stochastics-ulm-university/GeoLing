package geoling.util.tessellation;

import geoling.util.DoubleBox;
import geoling.util.Utilities;
import geoling.util.geom.Geometry2D;
import geoling.util.sim.grain.MarkedPoint;
import geoling.util.sim.util.RandomSet;

import java.util.Collection;

/**
 * Laguerre tessellation in 2D. Note that the Laguerre tessellation is a
 * weighted version of the Voronoi diagram (use constant weights to obtain a
 * Voronoi diagram).
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class LaguerreTessellation2D {
	
	/**
	 * Computes the Laguerre tessellation in 2D, for a given set of marked points.
	 * 
	 * @param markedPoints  the 2D points, with a numeric mark for the radius of the sphere around the point
	 * @param boundingBox   the bounding box for the generated cells, all points should be contained
	 * @return a random set with the convex polygons as <code>ConvexPolytope</code> objects
	 */
	public static RandomSet computeLaguerreTessellation(Collection<MarkedPoint> markedPoints, DoubleBox boundingBox) {
		Geometry2D.ConvexPolygon boxPolygon = Utilities.transform(Utilities.boxToPolytope(boundingBox));
		
		RandomSet result = new RandomSet(boundingBox);
		for (MarkedPoint markedPoint : markedPoints) {
			Geometry2D.ConvexPolygon cell = constructLaguerreCell(markedPoint, boxPolygon, markedPoints);
			
			if (cell != null) {
				result.add(cell.toPolytope(false));
			}
		}
		return result;
	}
	
	/**
	 * Constructs a single 2D Laguerre cell, for a given point, an initial cell (usually the
	 * simulation window) and the other points, all marked with radii.
	 * 
	 * @param point        the point to compute the cell for
	 * @param initialCell  the initial cell
	 * @param otherPoints  the other (marked) points, used to construct the cell by
	 *                     intersection of half-planes
	 * @return the cell or <code>null</code> if this point generates no cell
	 */
	public static Geometry2D.ConvexPolygon constructLaguerreCell(MarkedPoint point, Geometry2D.ConvexPolygon initialCell, Collection<MarkedPoint> otherPoints) {
		Geometry2D.ConvexPolygon cell = new Geometry2D.ConvexPolygon(initialCell);
		
		double[] pointCoord = point.getCoordinates();
		double pointSquaredLength = pointCoord[0]*pointCoord[0] + pointCoord[1]*pointCoord[1];
		
		for (MarkedPoint otherPoint : otherPoints) {
			if (otherPoint == point) {
				continue;
			}
			if (otherPoint.equals(point)) {
				throw new IllegalArgumentException("Cannot construct Laguerre cell because there are other points that have the same coordinates!");
			}
			
			double[] otherPointCoord = otherPoint.getCoordinates();
			double otherPointSquaredLength = otherPointCoord[0]*otherPointCoord[0] +
			                                 otherPointCoord[1]*otherPointCoord[1];

			// reference for normal vector and offset of the half-plane:
			// see page 22, Dissertation Claudia Redenbach
			// (adapted to "n*z + b <= 0" instead of "n*z >= b")
			double[] normalVectorCoord = new double[] { (otherPointCoord[0]-pointCoord[0])*2.0,
			                                            (otherPointCoord[1]-pointCoord[1])*2.0 };
			double offset = pointSquaredLength - otherPointSquaredLength
			                + otherPoint.getValue()*otherPoint.getValue()
			                - point.getValue()*point.getValue();
			
			// scale normal vector to length 1
			double normalVectorLength = Math.sqrt(normalVectorCoord[0]*normalVectorCoord[0] +
			                                      normalVectorCoord[1]*normalVectorCoord[1]);
			normalVectorCoord[0] /= normalVectorLength;
			normalVectorCoord[1] /= normalVectorLength;
			offset /= normalVectorLength;
			normalVectorLength = 1.0;
			
			// compute perpendicular point on line to the origin
			Geometry2D.Point perpendicularPoint = new Geometry2D.Point(normalVectorCoord[0], normalVectorCoord[1]);
			perpendicularPoint.scaleWith(-offset);
			
			// direction of the line is orthogonal to the normal vector, left side of the line defines the half-plane
			double dx = -normalVectorCoord[1];
			double dy = normalVectorCoord[0];
			
			Geometry2D.Line line = new Geometry2D.Line(perpendicularPoint, dx, dy);
			cell = Geometry2D.intersectWithHalfplane(cell, line);
			if (cell == null) {
				break;
			}
		}
		
		return cell;
	}
	
}
