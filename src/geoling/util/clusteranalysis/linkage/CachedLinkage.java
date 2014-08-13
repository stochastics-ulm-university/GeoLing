package geoling.util.clusteranalysis.linkage;

import geoling.util.clusteranalysis.Cluster;
import geoling.util.clusteranalysis.distance.ClusterObjectDistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This class provides a distance cache for clusters, based on
 * a set of clusters and a linkage method.
 * There are methods provided such that the cache can be updated
 * partially (and you can easily obtain the pair of clusters with
 * the smallest distance).
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class CachedLinkage implements LinkageMethod {
	
	/** A pair of two clusters. */
	public static class ClusterPair {
		public Cluster cluster1;
		public Cluster cluster2;
	}
	
	/** The original linkage-method, without cache. */
	private LinkageMethod linkage;
	
	/** The list of clusters, with the same index as in the distance matrix. */
	private ArrayList<Cluster> clusters;
	
	/** The map of objects to their index in the distance matrix. */
	private HashMap<Cluster,Integer> clusterToIndex;
	
	/** The cached distances. */
	private float[][] distances;
	
	/**
	 * Constructs a new cache for distances between clusters.
	 * 
	 * @param linkage  the (non-cached) linkage-method
	 */
	public CachedLinkage(ArrayList<Cluster> clusters, LinkageMethod linkage) {
		this.clusters = new ArrayList<Cluster>(clusters);
		this.linkage  = linkage;
		
		clusterToIndex = new HashMap<Cluster,Integer>(clusters.size()*4/3);
		for (int i = 0; i < clusters.size(); i++) {
			clusterToIndex.put(clusters.get(i), i);
		}
		
		// initialize distance matrix
		distances = new float[clusters.size()][];
		for (int i = 0; i < clusters.size(); i++) {
			distances[i] = new float[i];
			for (int j = 0; j < distances[i].length; j++) {
				distances[i][j] = (float)this.linkage.distance(clusters.get(i), clusters.get(j));
			}
		}
	}
	
	/**
	 * Returns the list of all current clusters in this cache.
	 * 
	 * @return the list of clusters
	 */
	public List<Cluster> getClusters() {
		return Collections.unmodifiableList(clusters);
	}
	
	/**
	 * Returns the object used to compute distances between individual objects.
	 * 
	 * @return the distance measure between individual objects
	 */
	public ClusterObjectDistance getObjectDistance() {
		return this.linkage.getObjectDistance();
	}
	
	/**
	 * The distance between the given two clusters.
	 * 
	 * @param cluster1  the first cluster
	 * @param cluster2  the second cluster
	 * @return the distance
	 */
	public double distance(Cluster cluster1, Cluster cluster2) {
		Integer iObj = clusterToIndex.get(cluster1);
		Integer jObj = clusterToIndex.get(cluster2);
		if (iObj == null || jObj == null) {
			throw new IllegalArgumentException("At least one cluster is not known!");
		}
		int i = iObj.intValue();
		int j = jObj.intValue();
		return distances[Math.max(i, j)][Math.min(i, j)];
	}
	
	/**
	 * Detects the pair of clusters with minimal distance.
	 *  
	 * @return the pair of clusters with minimal distance or <code>null</code> if not possible
	 */
	public ClusterPair getSmallestDistancePair() {
		float minDist = Float.POSITIVE_INFINITY;
		int clusterIndex1 = -1;
		int clusterIndex2 = -1;
		for (int i = 0; i < distances.length; i++) {
			if (clusters.get(i) == null) {
				continue;
			}
			for (int j = 0; j < distances[i].length; j++) {
				if (minDist > distances[i][j]) {
					minDist = distances[i][j];
					clusterIndex1 = j;
					clusterIndex2 = i;
				}
			}
		}
		
		if (clusterIndex1 < 0) {
			return null;
		} else {
			ClusterPair result = new ClusterPair();
			result.cluster1 = clusters.get(clusterIndex1);
			result.cluster2 = clusters.get(clusterIndex2);
			return result;
		}
	}
	
	/**
	 * Updates all distances to a (modified) cluster.
	 * 
	 * @param cluster  the (modified) cluster
	 */
	public void recomputeDistancesToCluster(Cluster cluster) {
		int k = clusterToIndex.get(cluster);
		for (int i = 0; i < distances.length; i++) {
			if (i == k) {
				Cluster iCluster = clusters.get(i);
				for (int j = 0; j < distances[i].length; j++) {
					Cluster jCluster = clusters.get(j);
					if ((iCluster == null) || (jCluster == null)) {
						distances[i][j] = Float.NaN;
					} else {
						distances[i][j] = (float)this.linkage.distance(iCluster, jCluster);
					}
				}
			} else if (k < distances[i].length) {
				int j = k;
				Cluster iCluster = clusters.get(i);
				Cluster jCluster = clusters.get(j);
				if ((iCluster == null) || (jCluster == null)) {
					distances[i][j] = Float.NaN;
				} else {
					distances[i][j] = (float)this.linkage.distance(iCluster, jCluster);
				}
			}
		}
	}
	
	/**
	 * Removes a (modified, now empty) cluster.
	 * 
	 * @param cluster  the (modified, now empty) cluster
	 */
	public void removeCluster(Cluster cluster) {
		if (cluster.size() > 0) {
			throw new IllegalArgumentException("Cluster has to be empty.");
		}
		int k = clusterToIndex.get(cluster);
		for (int i = 0; i < distances.length; i++) {
			if (i == k) {
				for (int j = 0; j < distances[i].length; j++) {
					distances[i][j] = Float.NaN;
				}
			} else if (k < distances[i].length) {
				distances[i][k] = Float.NaN;
			}
		}
		clusterToIndex.remove(cluster);
		clusters.set(k, null);
	}
	
}
