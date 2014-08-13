package geoling.util.clusteranalysis.linkage;

import geoling.util.clusteranalysis.Cluster;
import geoling.util.clusteranalysis.ClusterObject;
import geoling.util.clusteranalysis.distance.ClusterObjectDistance;

/**
 * Centroid-method for distances between clusters, i.e., the distance
 * between the centroids of two clusters.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class CentroidMethod implements LinkageMethod {
	
	/** The distance measure between cluster objects. */
	private ClusterObjectDistance distance;
	
	/**
	 * Constructs a new centroid-method cluster distance.
	 * 
	 * @param distance  the distance between individual cluster objects
	 */
	public CentroidMethod(ClusterObjectDistance distance) {
		this.distance = distance;
	}
	
	/**
	 * Returns the object used to compute distances between individual objects.
	 * 
	 * @return the distance measure between individual objects
	 */
	public ClusterObjectDistance getObjectDistance() {
		return this.distance;
	}
	
	/**
	 * The distance between the given two clusters.
	 * 
	 * @param cluster1  the first cluster
	 * @param cluster2  the second cluster
	 * @return the distance
	 * @throws UnsupportedOperationException if coordinates are not supported
	 */
	public double distance(Cluster cluster1, Cluster cluster2) {
		return this.distance.distance(this.getCentroid(cluster1), this.getCentroid(cluster2));
	}
	
	/**
	 * Computes the centroid of the given cluster.
	 * 
	 * @param cluster  the cluster
	 * @return the centroid
	 * @throws UnsupportedOperationException if coordinates are not supported
	 */
	private double[] getCentroid(Cluster cluster) {
		double[] centroid = null;
		double n = 0.0;
		for (ClusterObject point : cluster.getObjects()) {
			double[] coord = point.getCoordinates();
			double weight = point.getWeight();
			if (centroid == null) {
				centroid = new double[coord.length];
			}
			for (int i = 0; i < centroid.length; i++) {
				centroid[i] += coord[i]*weight;
			}
			n += weight;
		}
		if (centroid != null) {
			for (int i = 0; i < centroid.length; i++) {
				centroid[i] /= n;
			}
		}
		return centroid;
	}
	
}
