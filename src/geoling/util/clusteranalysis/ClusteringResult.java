package geoling.util.clusteranalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Class which stores the result of a cluster analysis.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class ClusteringResult {
	
	/** The list of clusters, their order has no meaning. */
	private ArrayList<Cluster> clusters;
	
	/** Parameter which will be <code>true</code> when the analysis is fuzzy, <code>false</code> otherwise. */
	private boolean isFuzzy;
	
	/**
	 * Constructs a new empty result object of a clustering.
	 */
	public ClusteringResult() {
		this.clusters = new ArrayList<Cluster>();
	}
	
	/**
	 * Constructs a new result object of a clustering with the given
	 * clusters.
	 * 
	 * @param clusters  the clusters which are part of the cluster result
	 * @param isFuzzy   determines whether it is fuzzy or not
	 * */
	public ClusteringResult(Collection<Cluster> clusters, boolean isFuzzy) {
		this();
		this.clusters.ensureCapacity(clusters.size());
		for (Cluster cluster : clusters) {
			if ((cluster != null) && (cluster.size() > 0)) {
				this.clusters.add(cluster);
			}
		}
		this.isFuzzy = isFuzzy;
	}
	
	/**
	 * Returns a copy of the list of clusters.
	 * 
	 * @return the list of clusters
	 */
	public ArrayList<Cluster> getClusters() {
		return new ArrayList<Cluster>(this.clusters);
	}
	
	/**
	 * Returns the number of clusters.
	 * 
	 * @return the number of clusters
	 */
	public int getNumberOfClusters() {
		return this.clusters.size();
	}
	
	/**
	 * Returns whether this clustering result is fuzzy or not.
	 * 
	 * @return <code>true</code> when the clustering result is fuzzy, <code>false</code> otherwise
	 */
	public boolean isFuzzy() {
		return this.isFuzzy;
	}
	
	/**
	 * Converts the fuzzy clusters to hard clusters. That means every object
	 * will belong to the cluster where the object has the highest probability.
	 * 
	 * @return the hard clustering result (this object itself if it is already
	 *         a hard result)
	 */
	public ClusteringResult getHardResult() {
		if (this.isFuzzy == false) {
			return this;
		} else {
			ArrayList<Cluster> hardClusters = new ArrayList<Cluster>();
			for (int i = 0; i < clusters.size(); i++) {
				hardClusters.add(new Cluster());
			}
			
			HashSet<ClusterObject> allObjects = new HashSet<ClusterObject>();
			for (Cluster cluster : clusters) {
				allObjects.addAll(cluster.getObjects());
			}
			
			for (ClusterObject object : allObjects) {
				double largestProbability = 0.0;
				int clusterIndex = -1;
				for (int j = 0; j < clusters.size(); j++) {
					double probability = clusters.get(j).getProbability(object);
					if (largestProbability < probability) {
						largestProbability = probability;
						clusterIndex = j;
					}
				}
				hardClusters.get(clusterIndex).put(object, 1.0);
			}
			
			return new ClusteringResult(hardClusters, false);
		}
	}
	
	/**
	 * Prints the fuzzy or the hard clustering result on <code>System.out</code>.
	 */
	public void print() {
		if (isFuzzy == true) {
			for (int i = 0; i < clusters.size(); i++) {
				System.out.println("Cluster: " + (i + 1));
				for (ClusterObject obj : clusters.get(i).getObjects()) {
					System.out.println(obj.toString() + " with probability " + clusters.get(i).getProbability(obj));
				}
			}
		} else {
			for (int i = 0; i < clusters.size(); i++) {
				System.out.println("Cluster: " + (i + 1));
				for (ClusterObject obj : clusters.get(i).getObjects()) {
					System.out.println(obj.toString());
				}
			}
		}
	}
	
}
