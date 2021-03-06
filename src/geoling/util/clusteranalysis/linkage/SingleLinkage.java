package geoling.util.clusteranalysis.linkage;

import geoling.util.clusteranalysis.Cluster;
import geoling.util.clusteranalysis.ClusterObject;
import geoling.util.clusteranalysis.distance.ClusterObjectDistance;

/**
 * Single-linkage for distances between clusters, i.e., the minimum
 * distance between pairs of cluster objects.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class SingleLinkage implements LinkageMethod {
	
	/** The distance measure between cluster objects. */
	private ClusterObjectDistance distance;
	
	/**
	 * Constructs a new single-linkage cluster distance.
	 * 
	 * @param distance  the distance between individual cluster objects
	 */
	public SingleLinkage(ClusterObjectDistance distance) {
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
	 */
	public double distance(Cluster cluster1, Cluster cluster2) {
		double min = Double.POSITIVE_INFINITY;
		for (ClusterObject point1 : cluster1.getObjects()) {
			for (ClusterObject point2 : cluster2.getObjects()) {
				double d = this.distance.distance(point1, point2);
				if (min > d) {
					min = d;
				}
			}
		}
		return min;
	}
	
}
