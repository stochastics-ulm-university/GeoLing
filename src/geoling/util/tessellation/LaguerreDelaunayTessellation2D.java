package geoling.util.tessellation;

import geoling.util.DoubleBox;
import geoling.util.Utilities;
import geoling.util.geom.Plane3D;
import geoling.util.geom.Point3D;
import geoling.util.geom.convexhull.QuickHull3D;
import geoling.util.sim.grain.ConvexPolytope;
import geoling.util.sim.grain.MarkedPoint;
import geoling.util.sim.grain.Point;
import geoling.util.sim.random.Deterministic;
import geoling.util.sim.random.RandomVariable;
import geoling.util.sim.random.Uniform;
import geoling.util.sim.util.RandomSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Laguerre-Delaunay tessellation in 2D. Note that the Laguerre tessellation is
 * a weighted version of the Voronoi diagram (use constant weights to obtain a
 * Voronoi diagram).
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class LaguerreDelaunayTessellation2D {
	
	/**
	 * Computes the Laguerre-Delaunay tessellation in 2D, for a given set
	 * of marked points.
	 * 
	 * @param markedPoints      the 2D points, with a numeric mark for the radius of the sphere around the point
	 * @param scale3rdCoord     determines whether the third constructed coordinate is scaled to the range of the
	 *                          other real coordinates (useful for numeric precision)
	 * @param juggle3rdCoordEps the maximum small value to be added to the third coordinate, if values
	 *                          are in range <code>[0,1]</code> (will be scaled automatically for larger boxes),
	 *                          e.g., <code>0.0</code> or <code>1E-9</code>
	 * @return a random set containing the convex polygons as <code>ConvexPolytope</code> objects
	 */
	public static RandomSet computeLaguerreDelaunayTessellation(Collection<MarkedPoint> markedPoints, boolean scale3rdCoord, double juggle3rdCoordEps) {
		double thirdCoordScale = 1.0;
		
		// detect maximum absolute values of coordinates and third constructed coordinate,
		// e.g. used to scale the third coordinate to the same range as the real coordinates
		double coordAbsMax = 0.0;
		double thirdCoordMax = 0.0;
		for (MarkedPoint markedPoint : markedPoints) {
			if (markedPoint.getDimension() != 2) {
				throw new IllegalArgumentException("Expected marked points in dimension 2!");
			}
			double[] coord = markedPoint.getCoordinates();
			double radius = markedPoint.getValue();
			
			double tmp = Math.max(Math.abs(coord[0]), Math.abs(coord[1]));
			if (tmp > coordAbsMax) {
				coordAbsMax = tmp;
			}
			
			tmp = Math.abs(coord[0]*coord[0] + coord[1]*coord[1] - radius*radius);
			if (tmp > thirdCoordMax) {
				thirdCoordMax = tmp;
			}
		}
		
		// scale third coordinate if necessary
		if (scale3rdCoord && (thirdCoordMax > coordAbsMax)) {
			thirdCoordScale = coordAbsMax/thirdCoordMax;
			thirdCoordMax *= thirdCoordScale;
		}
		
		// initialize random variable for third coordinate juggle
		RandomVariable juggleRandomVar = new Deterministic(0.0);
		if (juggle3rdCoordEps > 0.0) {
			double range = juggle3rdCoordEps;
			if (thirdCoordMax > 1.0) {
				range *= thirdCoordMax;
			}
			juggleRandomVar = new Uniform(0.0, range);
		}
		
		// convert 2D points with radius into 3D for using the convex hull algorithm in 3D
		// to generate the 3D Delaunay cells, see page 74, Dissertation Claudia Redenbach
		ArrayList<Point3D> points3D = new ArrayList<Point3D>(markedPoints.size());
		for (MarkedPoint markedPoint : markedPoints) {
			double[] coord = markedPoint.getCoordinates();
			double radius = markedPoint.getValue();
			double forthCoord = coord[0]*coord[0] + coord[1]*coord[1] - radius*radius;
			points3D.add(new Point3D(new double[] { coord[0], coord[1], forthCoord*thirdCoordScale + juggleRandomVar.realise() }));
		}
		
		// compute convex hull in 3D
		QuickHull3D.Polyhedron polytope3D = QuickHull3D.getConvexHull(points3D);
		
		// now use the lower faces, remove the 3rd coordinate, and reconstruct the polygon
		// for every cell by its three vertices
		LinkedList<ConvexPolytope> polygons = new LinkedList<ConvexPolytope>();
		double[] min = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
		double[] max = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
		
		// compute centroid, we just need a point inside the convex hull to determine
		// normal vectors below
		Point3D centroid = Point3D.ORIGIN;
		for (Point3D vertex : polytope3D.vertices) {
			centroid = centroid.translateBy(vertex);
		}
		centroid = centroid.scaleBy(1.0 / polytope3D.vertices.size());
		
		for (QuickHull3D.Face face : polytope3D.faces) {
			if (face.vertices.size() != 3) {
				throw new RuntimeException("In 2D, a Delaunay cell has always 3 vertices!");
			}
			
			// compute normal vector
			Plane3D plane = new Plane3D(face.vertices.get(0), face.vertices.get(1), face.vertices.get(2));
			Point3D normal = plane.getNormalVec();
			if (normal.getScalarProduct(centroid.getVectorTo(face.vertices.get(0))) > 0.0) {
				normal = normal.reflectOrigin();
			}
			
			// use normal vector to determine whether this is a lower face
			if (normal.getCoordinates()[2] < 0.0) {
				ArrayList<Point> vertices = new ArrayList<Point>(face.vertices.size());
				for (Point3D point : face.vertices) {
					double[] coord3D = point.getCoordinates();
					vertices.add(new Point(new double[] { coord3D[0], coord3D[1] }));
					if (min[0] > coord3D[0]) min[0] = coord3D[0];
					if (min[1] > coord3D[1]) min[1] = coord3D[1];
					if (max[0] < coord3D[0]) max[0] = coord3D[0];
					if (max[1] < coord3D[1]) max[1] = coord3D[1];
				}
				polygons.add(new ConvexPolytope(Utilities.ensureCounterClockwise(vertices.toArray(new Point[0]))));
			}
		}
		RandomSet result = new RandomSet(new DoubleBox(min, max));
		for (ConvexPolytope polygon : polygons) {
			result.add(polygon);
		}
		
		return result;
	}
	
}
