package geoling.util.clusteranalysis.linkage;

import geoling.util.clusteranalysis.Cluster;
import geoling.util.clusteranalysis.distance.ClusterObjectDistance;

/**
 * Interface for distances between clusters, used in e.g. hierarchical
 * clustering.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public interface LinkageMethod {
	
	/**
	 * Returns the object used to compute distances between individual objects.
	 * 
	 * @return the distance measure between individual objects
	 */
	public ClusterObjectDistance getObjectDistance();
	
	/**
	 * The distance between the given two clusters.
	 * 
	 * @param cluster1  the first cluster
	 * @param cluster2  the second cluster
	 * @return the distance
	 */
	public double distance(Cluster cluster1, Cluster cluster2);
	
}