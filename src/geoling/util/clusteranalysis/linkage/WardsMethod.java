package geoling.util.clusteranalysis.linkage;

import geoling.util.clusteranalysis.Cluster;
import geoling.util.clusteranalysis.ClusterObject;
import geoling.util.clusteranalysis.distance.ClusterObjectDistance;

/**
 * Ward’s minimum variance method for distances between clusters.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class WardsMethod implements LinkageMethod {
	
	/** The centroid-method distance measure between clusters. */
	private CentroidMethod centroidMethod;
	
	/**
	 * Constructs a new Wards-method cluster distance.
	 * 
	 * @param distance  the distance between individual cluster objects
	 */
	public WardsMethod(ClusterObjectDistance distance) {
		this.centroidMethod = new CentroidMethod(distance);
	}
	
	/**
	 * Returns the object used to compute distances between individual objects.
	 * 
	 * @return the distance measure between individual objects
	 */
	public ClusterObjectDistance getObjectDistance() {
		return this.centroidMethod.getObjectDistance();
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
		double weights1 = 0.0;
		double weights2 = 0.0;
		for (ClusterObject point : cluster1.getObjects()) {
			weights1 += point.getWeight();
		}
		for (ClusterObject point : cluster2.getObjects()) {
			weights2 += point.getWeight();
		}
		return this.centroidMethod.distance(cluster1, cluster2) / (1.0/weights1 + 1.0/weights2);
	}
	
}
