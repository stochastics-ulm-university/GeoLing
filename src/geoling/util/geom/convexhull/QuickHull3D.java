package geoling.util.geom.convexhull;

import java.util.ArrayList;
import java.util.Collection;

import geoling.util.geom.Point3D;

/**
 * Class for computing convex hulls in 3D using the quick-hull algorithm.
 * This class provides a simple wrapper method for the JAR file "quickhull3d.jar".
 * <p>
 * Description from the "quickhull3d.jar" website:
 * QuickHull3D: A Robust 3D Convex Hull Algorithm in Java. This is a 3D implementation
 * of QuickHull for Java, based on the original paper by Barber, Dobkin, and Huhdanpaa
 * and the <code>C</code> implementation known as <code>qhull</code>. The algorithm has
 * <code>O(n log(n))</code> complexity, works with double precision numbers, is fairly
 * robust with respect to degenerate situations, and allows the merging of co-planar faces.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @see "Barber, C. Bradford; Dobkin, David P.; Huhdanpaa, Hannu (1996),
 *       The quickhull algorithm for convex hulls,
 *       ACM Transactions on Mathematical Software 22 (4): 469-483,
 *       doi:10.1145/235815.235821"
 * @see <a href="http://en.wikipedia.org/wiki/QuickHull">Wikipedia: QuickHull</a>
 * @see <a href="http://www.cs.ubc.ca/~lloyd/java/quickhull3d.html">Homepage: QuickHull3D</a>
 */
public class QuickHull3D {
	
	/** A (convex) polyhedron, which is given by its faces. */
	public static class Polyhedron {
		/** List of vertices of this polyhedron. */
		public ArrayList<Point3D> vertices;
		/** List of faces of this polyhedron. */
		public ArrayList<Face> faces;
	}
	
	/** A single face of the polyhedron. */
	public static class Face {
		/** The list of vertices of this facet. */
		public ArrayList<Point3D> vertices;
	}
	
	/**
	 * Computes the convex hull of the given points in 3D.
	 * Note that the vertices in the resulting polyhedron are objects
	 * from the parameter <code>points</code>.
	 * 
	 * @param points  the list of points
	 * @return the polyhedron 
	 */
	public static Polyhedron getConvexHull(Collection<Point3D> points) {
		ArrayList<Point3D> pointsList = new ArrayList<Point3D>(points); 
		quickhull3d.Point3d[] inputPoints = new quickhull3d.Point3d[pointsList.size()];
		for (int i = 0; i < pointsList.size(); i++) {
			double[] coord = pointsList.get(i).getCoordinates();
			inputPoints[i] = new quickhull3d.Point3d(coord[0], coord[1], coord[2]);
		}
		
		quickhull3d.QuickHull3D hull = new quickhull3d.QuickHull3D();
		hull.build(inputPoints);
		
		Polyhedron polyhedron = new Polyhedron();

		polyhedron.vertices = new ArrayList<Point3D>(hull.getNumVertices());
		for (int index : hull.getVertexPointIndices()) {
			polyhedron.vertices.add(pointsList.get(index));
		}
		
		polyhedron.faces = new ArrayList<Face>(hull.getNumFaces());
		for (int[] indices : hull.getFaces(quickhull3d.QuickHull3D.POINT_RELATIVE)) {
			Face face = new Face();
			face.vertices = new ArrayList<Point3D>();
			for (int index : indices) {
				face.vertices.add(pointsList.get(index));
			}
			polyhedron.faces.add(face);
		}
		
		return polyhedron;
	}

}