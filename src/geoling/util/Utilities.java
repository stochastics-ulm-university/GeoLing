package geoling.util;

import geoling.util.geom.Geometry2D;
import geoling.util.sim.grain.ConvexPolytope;
import geoling.util.sim.grain.LineSegment;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.grain.Sphere;
import geoling.util.sim.util.RandomSet;
import geoling.util.sim.util.RandomSetElement;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Helper methods for Geometry2D and package <code>geoling.util.sim</code> elements.
 * 
 * @author Institute of Stochastics, Ulm University
 * @version 2014-02-13
 */
public class Utilities {
	
	/** The value of epsilon used for correction of numerical errors. */
	public static double EPS = 1e-10;
	
	/**
	 * This method is a shorter command for the intersection of two convex polytopes.
	 *
	 * @param poly1  the first convex polytope to intersect
	 * @param poly2  the second convex polytope to intersect
	 * @return convex polytope which is obtained by the intersection
	 */
	public static ConvexPolytope intersect(ConvexPolytope poly1, ConvexPolytope poly2) {
		return Utilities.transform(Geometry2D.intersect(transform(poly1),transform(poly2)),false);
	}
	
	/**
	 * This method computes the intersection of two polygons, which are not necessarily convex.
	 * Important: it is necessary that the polygons have their vertices in the correct order,
	 * i.e., counter-clockwise.
	 *
	 * @param poly1  the first polygon to intersect
	 * @param poly2  the second polygon to intersect
	 * @return a random set with the polygons obtained by the intersection, i.e., may contain
	 *         none, one or several polygons
	 */
	public static RandomSet intersect(Polytope poly1, Polytope poly2) {
		return intersectPolygonsByTriangulation(poly1.triangulate(), poly2.triangulate(), poly1.isFilled() && poly2.isFilled());
	}
	
	/**
	 * This method computes the intersection of two polygons, which are not necessarily convex,
	 * and given by their triangulation.
	 *
	 * @param triangles1  the set of triangles of the first polygon
	 * @param triangles2  the set of triangles of the second polygon
	 * @param filled      determines whether the returned polygons are filled
	 * @return	a random set with the polygons obtained by the intersection, i.e., may contain
	 *          none, one or several polygons
	 */
	public static RandomSet intersectPolygonsByTriangulation(RandomSet triangles1, RandomSet triangles2, boolean filled) {
		if ((triangles1.getDimension() != 2) || (triangles2.getDimension() != 2)) {
			throw new IllegalArgumentException("Polytopes must have dimension 2!");
		}
		
		// construct bounding box for both polygons
		double[] min1 = triangles1.getBoundingBox().getMin();
		double[] min2 = triangles2.getBoundingBox().getMin();
		double[] max1 = triangles1.getBoundingBox().getMax();
		double[] max2 = triangles2.getBoundingBox().getMax();
		DoubleBox box = new DoubleBox(new double[] { Math.min(min1[0], min2[0]), Math.min(min1[1], min2[1]) },
		                              new double[] { Math.max(max1[0], max2[0]), Math.max(max1[1], max2[1]) });
		
		// compare all the triangles from poly1 pairwise with all the triangles from poly2 to detect intersections
		RandomSet intersectionTriangles = new RandomSet(box);
		for (Iterator<?> it1 = triangles1.iterator(); it1.hasNext(); ) {
			ConvexPolytope triangle1 = (ConvexPolytope)it1.next();
			for (Iterator<?> it2 = triangles2.iterator(); it2.hasNext(); ) {
				ConvexPolytope triangle2 = (ConvexPolytope)it2.next();
				
				ConvexPolytope intersection;
				try {
					intersection = intersect(triangle1, triangle2);
				} catch (RuntimeException e) {
					// ignore
					intersection = null;
				}
				
				if (intersection != null) {
					if (intersection.getNumberOfVertices() <= 2) {
						// ignore
					} else  if (intersection.getNumberOfVertices() == 3) {
						intersectionTriangles.add(intersection);
					} else {
						// in the following, we want to work with triangles, therefore
						// triangulate polygons with more than three vertices
						Polytope tmp = new Polytope(intersection.getVertices());
						for (Iterator<?> it = tmp.triangulate().iterator(); it.hasNext(); ) {
							intersectionTriangles.add((ConvexPolytope)it.next());
						}
					}
				}
			}
		}
		
		return trianglesToPolygons(intersectionTriangles, filled);
	}
	
	/**
	 * This method aggregates a set of triangles to one or several polygons.
	 *
	 * @param triangles  the set of triangles
	 * @param filled     determines whether the returned polygons are filled
	 * @return	a random set with the polygons, i.e., may contain none, one or several polygons
	 */
	public static RandomSet trianglesToPolygons(RandomSet triangles, boolean filled) {
		int i;
		
		// construct line segments for the edges of all triangles, always in both directions
		LineSegment[][][] edges = new LineSegment[triangles.size()][3][2];
		i = 0;
		for (Iterator<?> it = triangles.iterator(); it.hasNext(); ) {
			ConvexPolytope triangle = (ConvexPolytope)it.next();
			Point[] vertices = triangle.getVertices();
			for (int i1 = 0; i1 < vertices.length; i1++) {
				int i2 = i1+1;
				if (i2 == vertices.length) {
					i2 = 0;
				}
				edges[i][i1][0] = new LineSegment(vertices[i1].getCoordinates()[0], vertices[i1].getCoordinates()[1],
				                                  vertices[i2].getCoordinates()[0], vertices[i2].getCoordinates()[1], false);
				edges[i][i1][1] = new LineSegment(vertices[i2].getCoordinates()[0], vertices[i2].getCoordinates()[1],
				                                  vertices[i1].getCoordinates()[0], vertices[i1].getCoordinates()[1], false);
			}
			i++;
		}
		
		// compute the neighbors of each triangle, by comparing them pairwise,
		// and build an adjacency graph. This graph will contain one connected
		// subgraph for each polygon in the intersection of the two polygons
		boolean[][][][] adjacentEdgePairs = new boolean[triangles.size()][triangles.size()][3][3];
		boolean[][] hasAdjacentEdge = new boolean[triangles.size()][3];
		boolean[][] adjacent = new boolean[triangles.size()][triangles.size()];
		short[] cluster = new short[triangles.size()];
		short clusterCounter = 0;
		for (i = 0; i < cluster.length; i++) {
			for (int j = 0; j < cluster.length; j++) {
				if (i <= j) {
					for (int ii = 0; ii < 3; ii++) {
						for (int jj = 0; jj < 3; jj++) {
							if (edges[i][ii][0].equals(edges[j][jj][0]) || edges[i][ii][0].equals(edges[j][jj][1]) || 
							    edges[i][ii][1].equals(edges[j][jj][0]) || edges[i][ii][1].equals(edges[j][jj][1])) {
								adjacentEdgePairs[i][j][ii][jj] = true;
								adjacentEdgePairs[j][i][jj][ii] = true;
								if (i != j) {
									hasAdjacentEdge[i][ii] = true;
									hasAdjacentEdge[j][jj] = true;
								}
								adjacent[i][j] = true;
								adjacent[j][i] = true;
								
								// build cluster array
								if ((cluster[i] > 0) && (cluster[j] > 0)) {
									if (cluster[i] != cluster[j]) {
										// join clusters
										short tmp1 = cluster[i];
										short tmp2 = cluster[j];
										for (int k = 0; k < cluster.length; k++) {
											if (cluster[k] == tmp2) {
												cluster[k] = tmp1;
											}
											if (cluster[k] > tmp2) {
												cluster[k]--;
											}
										}
										clusterCounter--;
									}
								} else if (cluster[i] > 0) {
									cluster[j] = cluster[i];
								} else if (cluster[j] > 0) {
									cluster[i] = cluster[j];
								} else {
									clusterCounter++;
									cluster[i] = clusterCounter;
									cluster[j] = clusterCounter;
								}
							}
						}
					}
				}
			}
		}
		
		RandomSet result = new RandomSet(triangles.getBoundingBox());
		
		// for each such subgraph, pick a triangle, walk to the edge, and then
		// walk around the edge, producing the segments bounding the corresponding
		// output polygon
		for (int clusterNumber = 1; clusterNumber <= clusterCounter; clusterNumber++) {
			// detect one edge element given by (i, ii) of the resulting polygon
			boolean edge_detected = false;
			int ii = -1;
			for (i = 0; i < adjacent.length; i++) {
				if (cluster[i] == clusterNumber) {
					for (ii = 0; ii < 3; ii++) {
						if (!hasAdjacentEdge[i][ii]) {
							edge_detected = true;
							break;
						}
					}
					if (edge_detected) {
						break;
					}
				}
			}
			if (!edge_detected) {
				throw new RuntimeException("No edge detected, but there should be a cluster with number "+clusterNumber+"!");
			}
			
			LinkedList<Point> points = new LinkedList<Point>();
			points.add(edges[i][ii][0].getEndPoint());
			
			// now walk along the other edge elements of the resulting polygon
			int j = i;
			int jj = ii;
			while (true) {
				// process the next edge element
				jj = (jj+1) % 3;
				if (hasAdjacentEdge[j][jj]) {
					// if it is also the edge of another triangle, then proceed with the next edge of that triangle
					int k, kk = -1;
					for (k = 0; k < adjacent.length; k++) {
						if (k == j) {
							continue;
						}
						for (kk = 0; kk < 3; kk++) {
							if (adjacentEdgePairs[j][k][jj][kk]) {
								break;
							}
						}
						if (kk < 3) {
							break;
						}
					}
					j = k;
					jj = kk;
				}
				
				// if we are at the edge of the desired polygon, then add the new vertex (or break if finished)
				if (!hasAdjacentEdge[j][jj]) {
					if ((i == j) && (ii == jj)) {
						break;
					} else {
						points.add(edges[j][jj][0].getEndPoint());
					}
				}
			}
			
			// remove unnecessary vertices from the list
			ListIterator<Point> it = points.listIterator();
			while (it.hasNext()) {
				// fetch three points: previous point, this point, next point
				Point prevPoint;
				if (it.hasPrevious()) {
					prevPoint = it.previous();
					it.next();
				} else {
					prevPoint = points.getLast();
				}
				
				Point thisPoint = it.next();
				
				Point nextPoint;
				if (it.hasNext()) {
					nextPoint = it.next();
					it.previous();
				} else {
					nextPoint = points.getFirst();
				}
				
				// remove this point if it is unnecessary
				if (Utilities.collinear(prevPoint.toGeo2DPoint(), thisPoint.toGeo2DPoint(), nextPoint.toGeo2DPoint())) {
					it.previous();
					it.remove();
				}
			}
			
			// build result polygon with the previously built vertices
			Point[] pointsArray = new Point[points.size()];
			result.add(new Polytope(points.toArray(pointsArray), filled));
		}
		
		return result;
	}
	
	/**
	 * Converts a convex polytope from <code>sim</code> to convex polygon
	 * from <code>Geometry2D</code>.
	 *
	 * @param cp  convex polytope from <code>sim</code>.
	 * @return convex polygon from <code>Geometry2D</code>.
	 */
	public static Geometry2D.ConvexPolygon transform(ConvexPolytope cp) throws RuntimeException {
		if (cp.getDimension() != 2)
			throw new RuntimeException("Poly must have dimension 2");
		
		Point[] vertices = cp.getVertices();
		Geometry2D.Point[] p = new Geometry2D.Point[vertices.length];
		
		for (int i = 0; i < p.length; i++)
			p[i] = transform(vertices[i]);
		
		return new Geometry2D.ConvexPolygon(p);
	}
	
	/**
	 * Converts a convex polygon from <code>Geometry2D</code> to convex
	 * polytope from <code>sim</code>.
	 *
	 * @param cp     convex polygon from <code>Geometry2D</code>.
	 * @param filled specifies whether convex polygon should be filled or not.
	 * @return convex polytope from <code>sim</code>.
	 */
	public static ConvexPolytope transform(Geometry2D.ConvexPolygon cp, boolean filled) {
		if (cp == null)
			return null;
		
		Point[] p = new Point[cp.p.length];
		
		for (int i = 0; i < p.length; i++)
			p[i] = transform(cp.p[i]);
		
		return new ConvexPolytope(p, filled);
	}
	
	/**
	 * Converts a point from <code>Geometry2D</code> to point
	 * from <code>sim</code>.
	 *
	 * @param p  point from <code>Geometry2D</code>.
	 * @return point from <code>sim</code>.
	 */
	public static Point transform(Geometry2D.Point p) {
		return new Point(new double[] {p.x,p.y});
	} 
	
	/**
	 * Converts a point from <code>sim</code> to point
	 * from <code>Geometry2D</code>.
	 *
	 * @param p  point from <code>sim</code>.
	 * @return point from <code>Geometry2D</code>.
	 */
	public static Geometry2D.Point transform(Point p) {
		double[] c = p.getCoordinates();
		return new Geometry2D.Point(c[0],c[1]);
	}
	
	/**
	 * Calculates the radius of a line from <code>Geometry2D</code>.
	 *
	 * @param l   the line.
	 * @return the radius.
	 */
	public static double getRadius(Geometry2D.Line l) {
		Geometry2D.Point p = l.getPerpendicularPoint();
		return Math.sqrt((p.x)*(p.x) + (p.y)*(p.y));
	}
	
	/**
	 * Calculates the angle of a line from <code>Geometry2D</code>.
	 *
	 * @param l   the line.
	 * @return the angle.
	 */
	public static double getAngle(Geometry2D.Line l) throws RuntimeException {
		return Math.atan2(l.dy, l.dx);
	}
	
	/**
	 * Tests two double values for equality and
	 * deals with numerical errors.
	 *
	 * @param x  one double value to be compared.
	 * @param y  the other double value to be compared.
	 * @return <code>true</code> if and only if <code>x</code>
	 *         and <code>y</code> are equally.
	 */
	public static boolean isEqual(double x, double y) {
		return (x == y) || (Math.abs(x - y) < EPS * maxabsval(1, x, y));
	}
	
	/**
	 * Returns the maximum absolute value of three
	 * real numbers.
	 *
	 * @param x  the first number.
	 * @param y  the second number.
	 * @param z  the third number.
	 * @return the maximum of the absolute values.
	 */
	public static double maxabsval(double x, double y, double z) {
		x = (x < 0) ? -x : x;
		y = (y < 0) ? -y : y;
		z = (z < 0) ? -z : z;
		return (x > y) ? (x > z) ? x : z
				: (y > z) ? y : z;
	}
	
	/**
	 * Converts a line segment from <code>sim</code> to line segment
	 * from <code>Geometry2D</code>.
	 *
	 * @param ls     line segment from <code>sim</code>.
	 * @return line  segment from <code>Geometry2D</code>.
	 * @throws IllegalArgumentException
	 * @throws RuntimeException
	 */
	public static Geometry2D.LineSegment transform(LineSegment ls) throws IllegalArgumentException, RuntimeException {
		try {
			return new Geometry2D.LineSegment(transform(ls.getStartPoint()), transform(ls.getEndPoint()));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch (RuntimeException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	/**
	 * Converts a line segment from <code>Geometry2D</code> to line segment
	 * from <code>sim</code>.
	 *
	 * @param ls  line segment from <code>Geometry2D</code>.
	 * @return    line segment from <code>sim</code>.
	 */
	public static LineSegment transform(Geometry2D.LineSegment ls) {
		return new LineSegment(ls.p1.x,ls.p1.y,getLength(ls),getAngle(ls));
	}
	
	
	/**
	 * Calculates the length of a linesegment from <code>Geometry2D</code>.
	 *
	 * @param ls   the linesegment.
	 * @return the length.
	 */
	public static double getLength (Geometry2D.LineSegment ls) {
		return Math.sqrt((ls.p2.x-ls.p1.x)*(ls.p2.x-ls.p1.x) +
		                 (ls.p2.y-ls.p1.y)*(ls.p2.y-ls.p1.y));
	}
	
	/**
	 * Calculates the angle of a linesegment from <code>Geometry2D</code>.
	 *
	 * @param ls   the linesegment.  
	 * @return the angle.
	 */
	public static double getAngle (Geometry2D.LineSegment ls) 
			throws RuntimeException {
		double dx = ls.p2.x - ls.p1.x;
		double dy = ls.p2.y - ls.p1.y;
		return Math.atan2(dy, dx);
	}
	
	/**
	 * This method transforms a DoubleBox into a ConvexPolytope.
	 *
	 * @param	box	object of type DoubleBox
	 * @return	transformed object
	 */
	public static ConvexPolytope boxToPolytope(DoubleBox box) {
		double xmin = box.getMin(0);
		double ymin = box.getMin(1);
		double xmax = box.getMax(0);
		double ymax = box.getMax(1);
		
		ConvexPolytope win = new ConvexPolytope(new geoling.util.sim.grain.Point[] {
				new Point(new double[] {xmin, ymin}),
				new Point(new double[] {xmax, ymin}),
				new Point(new double[] {xmax, ymax}),
				new Point(new double[] {xmin, ymax}),
		});
		return win;
	}
	
	/**
	 * Checks whether or not two points out of <code>sim</code>
	 * are equal.
	 *
	 * @param       p1       the first point.
	 * @param       p2       the second point.
	 * @return      <code>True</code>, if both points are equal. 
	 *              <code>False</code>, otherwise.
	 */
	public static boolean equals(Point p1, Point p2) {
		Geometry2D.Point geop1 = transform(p1);
		Geometry2D.Point geop2 = transform(p2);
		if (geop1.equals(geop2)) return true;
		else return false;
	}
	
	/**
	 * Tests if a given point is on a given line segment.
	 *
	 * @param   ls   the linesegment to be tested.
	 * @param   p    point to be tested.
	 * @throws IllegalArgumentException
	 */
	public static boolean onSegment(Geometry2D.LineSegment ls, Geometry2D.Point p) throws IllegalArgumentException {
		Geometry2D.Point a = ls.p1;
		Geometry2D.Point b = ls.p2;
		try {
			Geometry2D.Line ab = new Geometry2D.Line(a,b);
			if (Geometry2D.side(ab,p) != Geometry2D.SIDE_ON) return false;
			if (isLess(a,b)) 
				return ((isLess(a,p) || p.equals(a)) && (isLess(p,b) || p.equals(b)));
			else 
				return ((isLess(p,a) || p.equals(a)) && (isLess(b,p) || p.equals(b)));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	/**
	 * Returns <code>true</code> iff the first point is smaller 
	 * than the second point
	 *
	 * @param   p   first point.
	 * @param   q   second point.
	 * @return  <code>True</code> iff p is smaller than q.
	 */
	public static boolean isLess(Geometry2D.Point p, Geometry2D.Point q) {
		return (compare(p, q) == -1);
	}
	
	/**
	 * Compares two points in a lexicographically way.
	 * Returns 1 if first point is greater, 0 if both are equal and
	 * -1 if first point is smaller than second point.
	 * 
	 * @param   p   first point.
	 * @param   q   second point.
	 * @return  -1 if smaller, 0 if equal, 1 if greater. 
	 */
	public static int compare(Geometry2D.Point p, Geometry2D.Point q) {
		if (p.equals(q)) return 0;
		if (geoling.util.Utilities.isEqual(p.x,q.x))
			if (p.y > q.y) return 1;
			else return -1;
		else if (p.x > q.x) return 1;
		else return -1;
	}
	
	/**
	 * Returns a <code>LineSegment</code>, a <code>Point</code> or
	 * <code>null</code> depending on the type of the intersection
	 * of the two line segment. A dummy <code>Line</code> object is 
	 * returned if an error occurs.
	 *
	 * @param   s1  the first line segment.
	 * @param   s2  the second line segment.
	 * @return  the intersection of <code>s1</code> and <code>s2</code>.
	 * @throws IllegalArgumentException
	 */
	public static Geometry2D.GeometricObject intersectSegments(Geometry2D.LineSegment s1, Geometry2D.LineSegment s2) throws IllegalArgumentException {
		Geometry2D.Line ls1 = s1.toLine();
		Geometry2D.Line ls2 = s2.toLine();
		Geometry2D.GeometricObject go = Geometry2D.intersect(ls1,ls2);
		
		try {
			// intersection is empty ...
			if (go == null) { 
				return null;
				// ... or the line segment ...
			} else if (go instanceof Geometry2D.Line) { 
				Geometry2D.Line l = (Geometry2D.Line) go;
				// two segments are equal ...
				if (equals(s1,s2)) {
					return new Geometry2D.LineSegment(s1);
					// ... segment s2 contained within s1 
				} else if (onSegment(s1,s2.p1) && onSegment(s1,s2.p2)) {
					return new Geometry2D.LineSegment(s2);
					// ... segment s1 contained within s2
				} else if (onSegment(s2,s1.p1) && onSegment(s2,s1.p2)) {
					return new Geometry2D.LineSegment(s1);
					// ... different cases when segments overlap partially
				} else if (onSegment(s1,s2.p1)) {
					if (onSegment(s2,s1.p1)) {
						if (isLess(s1.p1,s2.p1)) {
							return new Geometry2D.LineSegment(s1.p1,s2.p1);
						} else {
							return new Geometry2D.LineSegment(s2.p1,s1.p1);
						}
					} else if (onSegment(s2,s1.p2)) {
						if (isLess(s1.p2,s2.p1)) {
							return new Geometry2D.LineSegment(s1.p2,s2.p1);
						} else {
							return new Geometry2D.LineSegment(s2.p1,s1.p2);
						}
					} else {
						return new Geometry2D.Line(l); // dummy line return
					}
				} else if (onSegment(s1,s2.p2)) {
					if (onSegment(s2,s1.p1)) {
						if (isLess(s1.p1,s2.p2)) {
							return new Geometry2D.LineSegment(s1.p1,s2.p2);
						} else {
							return new Geometry2D.LineSegment(s2.p2,s1.p1);
						}
					} else if (onSegment(s2,s1.p2)) {
						if (isLess(s1.p2,s2.p2)) {
							return new Geometry2D.LineSegment(s1.p2,s2.p2);
						} else {
							return new Geometry2D.LineSegment(s2.p2,s1.p2);
						}
					} else {
						return new Geometry2D.Line(l); // dummy line return
					}
				} else {
					return null; // no intersection, but on same line
				}
				// ... or a point ...
			} else if (go instanceof Geometry2D.Point) {
				Geometry2D.Point p = (Geometry2D.Point) go;
				// point on both segments
				if (onSegment(s1,p) && onSegment(s2,p)) {
					return new Geometry2D.Point(p);
				} else {
					return null; // no intersection
				}
				// ... should never occur
			} else {
				return new Geometry2D.Line(s1.p1,s1.p2); // dummy line return
			}
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	/**
	 * Checks whether or not two line segments are equal.
	 *
	 * @param       s1       the first line segment.
	 * @param       s2       the second line segment.
	 * @return      <code>True</code>, if both linesegments are equal. 
	 *              <code>False</code>, otherwise.
	 */
	public static boolean equals(Geometry2D.LineSegment s1, Geometry2D.LineSegment s2) {
		if (s1.p1.equals(s2.p1) && s1.p2.equals(s2.p2)) return true;
		else return false;
	}
	
	/**
	 * Transforms a two dimensional point out of <code>sim.grain</code>
	 * into a two dimensional sphere. The radius and the indication of
	 * whether the sphere is filled or not can be chosen freely.
	 *
	 * @param  p         the point to be transformed.
	 * @param  r         the radius to be chosen.
	 * @param  filled    the indicator for filled.
	 * @return           the sphere.
	 */
	public static Sphere transform(Point p, double r, boolean filled) {
		
		return new Sphere(p.getCoordinates(),r,filled);
	}
	
	/**
	 * Returns the depth of a random set, i.e. returns the maximal number
	 * of iterated random sets within a certain random set. Hence the method allows
	 * for determination of the depth of an iterated tessellation
	 * 
	 * @param    rs the random set under consideration.
	 * @param   depth  it has to initialized with 0 
	 * @param   max    
	 * @return   the maximal iteration number.
	 */
	public static int getMaxIteration (RandomSet rs, int depth, int max) {
		max = Math.max(depth,max);  
		for (Iterator<?> i = rs.iterator(); i.hasNext();) {
			RandomSetElement element = (RandomSetElement) i.next();
			if (element instanceof RandomSet) {
				max = getMaxIteration((RandomSet) element, depth+1,max);
			}
		}
		return max;
	}
	
	/**
	 * Returns the distance between a linesegment and a point.
	 *
	 * @param ls     the line segment.
	 * @param p      the point.
	 * @return       the distance or <code>null</code>.
	 */
	public static double distance(Geometry2D.LineSegment ls, Geometry2D.Point p) {
		
		Geometry2D.LineSegment lstrans = translateLS(ls, -p.x, -p.y);
		Geometry2D.Line l = lstrans.toLine();
		Geometry2D.Point pt = l.getPerpendicularPoint();
		if (onSegment(lstrans, pt)) {
			return pt.getRadius();
		} else {
			return Math.min(p.distanceTo(ls.p1),p.distanceTo(ls.p2));
		}
	}
	
	/**
	 * Translates a linesegment from <code>Geometry2D</code> by a vector (dx,dy).
	 * 
	 * @param ls   the linesegment to be translated.
	 * @param dx    
	 * @param dy
	 * @return   the translated linesegment.
	 */
	public static Geometry2D.LineSegment translateLS(Geometry2D.LineSegment ls, double dx, double dy) {
		
		Geometry2D.Point p1 = new Geometry2D.Point(ls.p1.x + dx, ls.p1.y + dy);
		Geometry2D.Point p2 = new Geometry2D.Point(ls.p2.x + dx, ls.p2.y + dy);
		
		return new Geometry2D.LineSegment(p1,p2);
	}
	
	/**
	 * Projects a point onto a linesegment.
	 * Returns the projected point
	 *
	 * @param ls   the line segment.
	 * @param p   the point.
	 * @return     the projected point.
	 */
	public static Geometry2D.Point project(Geometry2D.LineSegment ls, Geometry2D.Point p) {
		Geometry2D.LineSegment etrans = translateLS(ls,-p.x,-p.y);
		Geometry2D.LineSegment etrans1 = new Geometry2D.LineSegment(etrans);
		Geometry2D.Line eline = etrans1.toLine();
		Geometry2D.Point ppt = eline.getPerpendicularPoint();
		if (Utilities.onSegment(etrans,ppt)) {
			Geometry2D.Point newpt = new Geometry2D.Point(ppt);
			newpt.translateBy(p.x,p.y);
			return(newpt);
		} else {
			double d1 = p.distanceTo(ls.p1);
			double d2 = p.distanceTo(ls.p2);
			if (d1 < d2) {
				return(ls.p1);
			} else {
				return(ls.p2);
			}
		}
	}
	
	/**
	 * Returns the polygon with contains the point origin and is split by the bisector
	 * of origin and nodes.
	 * 
	 * @param polygon the polygon
	 * @param origin  the point, which the polygon should be contain
	 * @param nodes   the other point, by which the polygon is to be splited
	 *
	 */
	public static Geometry2D.ConvexPolygon splitPolybyBisector(Geometry2D.ConvexPolygon polygon, Geometry2D.Point origin, Geometry2D.Point nodes) {
		
		Geometry2D.ConvexPolygon[] polys;
		Geometry2D.Line bisec = Geometry2D.bisector(origin, nodes);
		polys = Geometry2D.split(polygon, bisec);
		
		if (polys.length == 2) {
			if (polys[0].contains(origin)) {
				return polys[0];
			}
			
			if (polys[1].contains(origin)) {
				return polys[1];
			}
			else {
				throw new RuntimeException("split1");
			}
		}
		
		else //length =1
			if (polys[0].contains(origin)) {
				return polys[0];
			}
			else {
				throw new RuntimeException("split2");
			}
	}
	
	/**
	 * Returns the lowest point of an array of points.
	 * Note that this method always returns a point, even if the given
	 * array is empty; then it has <code>Double.MAX_VALUE</code> as coordinates.
	 * 
	 * @param points  the array of points.
	 * @return the lowest point, the most left if there is a tie.
	 */
	public static Geometry2D.Point getMinPoint(Geometry2D.Point[] points) {
		Geometry2D.Point minpoint = new Geometry2D.Point(Double.MAX_VALUE, Double.MAX_VALUE);
		for (int i = 0; i < points.length; i++) {
			if (points[i].y < minpoint.y) {
				minpoint = new Geometry2D.Point(points[i]);
			} else if (points[i].y == minpoint.y && points[i].x < minpoint.x) {
				minpoint = new Geometry2D.Point(points[i]);
			}
		}
		return minpoint;
	}
	
	/***************************************************************************
	 * Returns <code>true</code> iff the three points are collinear.
	 * 
	 * @param a  first point.
	 * @param b  second point.
	 * @param c  third point.
	 * @return <code>true</code> iff the three points are collinear.
	 ***************************************************************************/
	public static boolean collinear(Geometry2D.Point a, Geometry2D.Point b, Geometry2D.Point c) {
		if (a.equals(b) || b.equals(c) || a.equals(c)) {
			return true;
		} else {
			// old implementation (has some problems, probably due to test for equality of lines, which is not really clear):
			//Geometry2D.Line line1 = new Geometry2D.Line(a, b);
			//Geometry2D.Line line2 = new Geometry2D.Line(b, c);
			//return line1.equals(line2);
			if (a.distanceTo(b) >= Math.max(a.distanceTo(c), b.distanceTo(c))) {
				return (new Geometry2D.LineSegment(a, b)).contains(c);
			} else if (a.distanceTo(c) >= Math.max(a.distanceTo(b), b.distanceTo(c))) {
				return (new Geometry2D.LineSegment(a, c)).contains(b);
			} else {
				return (new Geometry2D.LineSegment(b, c)).contains(a);
			}
		}
	}
	
	/**
	 * Converts Circle from <code>Geometry2D</code> to Sphere from
	 * <code>geoling.util.sim.grain</code>.
	 * 
	 * @param circ  Circle from <code>Geometry2D</code>.
	 * @return Sphere from <code>geoling.util.sim.grain</code>.
	 */
	public static Sphere transform(Geometry2D.Circle circ) {
		return new Sphere(circ.center.toPoint().getCoordinates(), circ.r);
	}
	
	/**
	 * Checks if the given vertices of a polygon in 2D are ordered
	 * counter-clockwise, reverses the order if necessary.
	 * 
	 * @param points  the points ordered clockwise or counter-clockwise
	 * @return the points ordered counter-clockwise
	 */
	public static Point[] ensureCounterClockwise(Point[] points) {
		double sum = 0.0;
		for (int i = 0; i < points.length; i++) {
			int j = (i+1) % points.length;
			if (points[i].getDimension() != 2 || points[j].getDimension() != 2) {
				throw new IllegalArgumentException("2D points required to order them counter-clockwise!");
			}
			double[] coord1 = points[i].getCoordinates();
			double[] coord2 = points[j].getCoordinates();
			sum += (coord2[0]-coord1[0])*(coord2[1]+coord1[1]);
		}
		
		if (sum > 0.0) {
			Point[] result = new Point[points.length];
			for (int i = 0; i < points.length; i++) {
				result[points.length-i-1] = points[i];
			}
			return result;
		} else {
			return points;
		}
	}
	
}