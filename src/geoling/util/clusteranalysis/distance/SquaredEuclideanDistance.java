package geoling.util.clusteranalysis.distance;

import geoling.util.clusteranalysis.ClusterObject;

/**
 * The squared Euclidean distance for cluster objects having coordinates.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class SquaredEuclideanDistance implements ClusterObjectDistance {
	
	/**
	 * The squared Euclidean distance between the two objects.
	 * 
	 * @param p  the first object
	 * @param q  the second object
	 * @return the distance
	 * @throws UnsupportedOperationException if coordinates are not supported
	 */
	public double distance(ClusterObject p, ClusterObject q) {
		return distance(p.getCoordinates(), q.getCoordinates());
	}
	
	/**
	 * The squared Euclidean distance between the coordinates of two objects (or e.g.
	 * the centroid of two clusters).
	 * 
	 * @param p  the first object
	 * @param q  the second object
	 * @return the distance
	 */
	public double distance(double[] p, double[] q) {
		if (p.length != q.length) {
			throw new IllegalArgumentException("p and q must have the same length");
		}
		double dist2 = 0.0;
		for (int i = 0; i < p.length; i++) {
			dist2 += (p[i] - q[i]) * (p[i] - q[i]);
		}
		return dist2;
	}
	
}
