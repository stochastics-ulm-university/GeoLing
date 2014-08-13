package geoling.util.clusteranalysis.distance;

import geoling.util.clusteranalysis.ClusterObject;

/**
 * Interface for classes that define a distance between objects
 * that shall be clustered.
 * The classes may use the coordinates of the objects that are
 * clustered, or other methods to compute a distance.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public interface ClusterObjectDistance {
	
	/**
	 * The distance between the two objects.
	 * 
	 * @param p  the first object
	 * @param q  the second object
	 * @return the distance
	 * @throws UnsupportedOperationException if the cluster objects are not supported
	 */
	public double distance(ClusterObject p, ClusterObject q);
	
	/**
	 * The distance between the coordinates of two objects (or e.g.
	 * the centroid of two clusters), when applicable.
	 * 
	 * @param p  the first object
	 * @param q  the second object
	 * @return the distance
	 * @throws UnsupportedOperationException if coordinates are not supported
	 */
	public double distance(double[] p, double[] q);
	
}