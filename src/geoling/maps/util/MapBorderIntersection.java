package geoling.maps.util;

import geoling.util.Conversion;
import geoling.util.DoubleBox;
import geoling.util.LatLong;
import geoling.util.Utilities;
import geoling.util.geom.Geometry2D;
import geoling.util.sim.grain.ConvexPolytope;
import geoling.util.sim.grain.LineSegment;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.util.RandomSet;

import java.util.Iterator;

/**
 * Helper class for the intersection of cells with the (non-convex) border polygon.
 * Note that this helper class uses the polygon implementation supplied by
 * the package <code>java.awt</code> to determine whether a cell is completely
 * contained and can be returned without intersecting. This speeds up the process.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class MapBorderIntersection {
	
	/** Exception for the case of several polygons, which cannot be handled otherwise. */
	public static class IntersectionConsistsOfSeveralPolygons extends RuntimeException {
		private static final long serialVersionUID = 1L;
		
		public IntersectionConsistsOfSeveralPolygons() {
			super("The intersection consists of more than one polygon!");
		}
	}
	
	/**
	 * Scaling constant used to scale geographical coordinates to large values, because
	 * <code>java.awt.Polygon</code> only supports integer coordinates.
	 */
	private final static int JAVA_SHAPE_SCALING = 1000000;
	
	/** The border polygon of this Voronoi map. */
	private Polytope border;
	
	/** A set of triangles representing the (filled) border polygon. */
	private RandomSet borderTriangles;
	
	/** A (scaled) java.awt.Polygon object which is used to avoid unnecessary intersections. */
	private java.awt.Polygon borderJavaObj;
	
	/**
	 * Constructs a new map border intersection helper object.
	 */
	public MapBorderIntersection(Polytope border) {
		this.border = border;
		
		this.borderTriangles = this.border.triangulate();
		
		// construct a (scaled) java.awt.Polygon object which is used to avoid unnecessary intersections
		Point[] vertices = this.border.getVertices();
		int[] xpoints = new int[vertices.length];
		int[] ypoints = new int[vertices.length];
		for (int i = 0; i < vertices.length; i++) {
			xpoints[i] = (int)Math.round(vertices[i].getCoordinates()[0] * JAVA_SHAPE_SCALING);
			ypoints[i] = (int)Math.round(vertices[i].getCoordinates()[1] * JAVA_SHAPE_SCALING);
		}
		this.borderJavaObj = new java.awt.Polygon(xpoints, ypoints, vertices.length);
	}
	
	/**
	 * Returns the border polygon of this Voronoi map.
	 * 
	 * @return the border polygon
	 */
	public Polytope getBorder() {
		return this.border;
	}
	
	/**
	 * Checks if the given box is completely contained in the border polygon, uses the (scaled) java.awt.Polygon object.
	 * 
	 * @param box  the box to check
	 * @return <code>true</code> if the box is contained in the border polygon, <code>false</code> doesn't mean anything!
	 */
	private boolean javaBorderContainsBox(DoubleBox box) {
		if (this.borderJavaObj == null) {
			throw new RuntimeException("Call initJavaBorder() before javaBorderContainsBox.");
		}
		
		// construct bounding box of cell, which is scaled by JAVA_SHAPE_SCALING
		double[] min = box.getMin();
		double[] width = box.getWidth();
		min[0] = min[0] * JAVA_SHAPE_SCALING;
		min[1] = min[1] * JAVA_SHAPE_SCALING;
		width[0] = width[0] * JAVA_SHAPE_SCALING;
		width[1] = width[1] * JAVA_SHAPE_SCALING;
		
		// check if the box is completely contained in the border polygon, then
		// no intersection is necessary (uses the (scaled) java.awt.Polygon object)
		return this.borderJavaObj.contains((int)Math.floor(min[0]), (int)Math.floor(min[1]), (int)Math.ceil(width[0]), (int)Math.ceil(width[1]));
	}
	
	/**
	 * Intersects the given cell (a polygon) with the border polygon.
	 * 
	 * @param cell  the cell as a polygon
	 * @return the intersection of the cell with the border polygon,
	 *         a set containing one or several polytopes
	 */
	public RandomSet intersect(Polytope cell) {
		if (javaBorderContainsBox(cell.getBoundingBox())) {
			RandomSet result = new RandomSet(border.getBoundingBox());
			result.add(new Polytope(cell.getVertices(), true));
			return result;
		} else {
			return Utilities.intersectPolygonsByTriangulation(borderTriangles, cell.triangulate(), true);
		}
	}
	
	/**
	 * Intersects the given cell (a polygon) with the border polygon.
	 * 
	 * @param cell     the cell as a polygon
	 * @param latLong  used to determine the polygon to choose if the
	 *                 intersection consists of more than one polygon,
	 *                 may be <code>null</code>
	 * @return the intersection of the cell with the border polygon,
	 *         if the intersections consists of several polygons, only that
	 *         one containing the considered location will be returned
	 * @throws IntersectionConsistsOfSeveralPolygons if the intersection
	 *           is not a single polygon (or parameter <code>latLong</code>
	 *           is not sufficient to choose the required polygon)
	 */
	public Polytope intersect(Polytope cell, LatLong latLong) {
		if (javaBorderContainsBox(cell.getBoundingBox())) {
			return new Polytope(cell.getVertices(), true);
		} else {
			// intersect Voronoi cell with the correct border
			RandomSet intersection = Utilities.intersectPolygonsByTriangulation(borderTriangles, cell.triangulate(), true);
			if (intersection.isEmpty()) {
				return null;
			} else if (intersection.size() == 1) {
				return (Polytope)intersection.iterator().next();
			} else {
				// if the intersections consists of several polygons, then choose the one containing the location point, if given
				Polytope newVoronoiCell = null;
				if (latLong != null) {
					Point point = Conversion.toSimGrainPoint(latLong);
					for (Iterator<?> it = intersection.iterator(); it.hasNext(); ) {
						Polytope poly = (Polytope)it.next();
						if (poly.contains(point) || isPointOnEdge(poly, point)) {
							newVoronoiCell = poly;
							break;
						}
					}
				}
				if (newVoronoiCell == null) {
					throw new IntersectionConsistsOfSeveralPolygons();
				}
				return newVoronoiCell;
			}
		}
	}
	
	/**
	 * Intersects the given cell (a convex polygon) with the border polygon.
	 * 
	 * @param cell     the cell as a polygon
	 * @param latLong  used to determine the polygon to choose if the
	 *                 intersection consists of more than one polygon,
	 *                 may be <code>null</code>
	 * @return the intersection of the cell with the border polygon,
	 *         if the intersections consists of several polygons, only that
	 *         one containing the considered location will be returned
	 * @throws IntersectionConsistsOfSeveralPolygons if the intersection
	 *           is not a single polygon (or parameter <code>latLong</code>
	 *           is not sufficient to choose the required polygon)
	 */
	public Polytope intersect(ConvexPolytope cell, LatLong latLong) {
		return intersect(new Polytope(cell.getVertices()), latLong);
	}
	
	/**
	 * Checks whether the given point lies on the edge of the given polytope.
	 * 
	 * @param polytope the polytope
	 * @param point    the point
	 * @return true if the point lies on the edge of the polytope
	 */
	public static boolean isPointOnEdge(Polytope polytope, Point point) {
		Geometry2D.Point p = point.toGeo2DPoint();
		RandomSet edges = polytope.getEdges();
		for (Iterator<?> i = edges.iterator(); i.hasNext(); ) {
			LineSegment ls = (LineSegment)i.next();
			Geometry2D.LineSegment ls2 = new Geometry2D.LineSegment(ls.getStartPoint().toGeo2DPoint(), ls.getEndPoint().toGeo2DPoint());
			if (Utilities.onSegment(ls2, p)) {
				return true;
			}
		}
		return false;
	}
	
}
