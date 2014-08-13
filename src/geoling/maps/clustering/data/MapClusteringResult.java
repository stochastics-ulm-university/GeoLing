package geoling.maps.clustering.data;

import geoling.util.clusteranalysis.Cluster;
import geoling.util.clusteranalysis.ClusterObject;
import geoling.util.clusteranalysis.ClusteringResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Result of a clustering of maps.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class MapClusteringResult {
	
	public ClusteringResult clusterResult;
	
	public MapClusteringResult(ClusteringResult clusters) {
		this.clusterResult = clusters;
	}

	/**
	 * Returns the total number of clusters.
	 * 
	 * @return the total number of clusters
	 */
	public int getClusterCount() {
		return this.clusterResult.getNumberOfClusters();
	}
	
	/**
	 * Returns whether this clustering result is fuzzy, i.e., are there probabilities
	 * instead of (hard) assignments of maps to clusters.
	 * 
	 * @return <code>true</code> if the result is fuzzy, <code>false</code> otherwise
	 */
	public boolean isFuzzy() {
		return this.clusterResult.isFuzzy();
	}
	
	/**
	 * Returns the cluster with the given index as a sorted list of <code>MapClusterObject</code> objects.
	 * 
	 * @param clusterIndex  the index (starting at zero)
	 * @return the cluster for the given index
	 */
	public ArrayList<MapClusterObject> getCluster(int clusterIndex) {
		final Cluster cluster = this.clusterResult.getClusters().get(clusterIndex);
		
		ArrayList<MapClusterObject> list = new ArrayList<MapClusterObject>(cluster.size());
		for (ClusterObject obj : cluster.getObjects()) {
			list.add((MapClusterObject)obj);
		}
		Collections.sort(list, new Comparator<MapClusterObject>() {
			public int compare(MapClusterObject o1, MapClusterObject o2) {
				int c = (int)Math.signum(cluster.getProbability(o2)-cluster.getProbability(o1));
				if (c == 0) {
					c = o1.getAreaClassMap().getMap().compareTo(o2.getAreaClassMap().getMap());
				}
				return c;
			}
		});
		
		return list;
	}
	
	public double getObjectInClusterProbability(int clusterIndex, MapClusterObject object) {
		return this.clusterResult.getClusters().get(clusterIndex).getProbability(object);
	}
	
	public int getClusterSize(int clusterIndex) {
		return this.clusterResult.getClusters().get(clusterIndex).size();
	}

}