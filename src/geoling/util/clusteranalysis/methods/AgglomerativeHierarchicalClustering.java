package geoling.util.clusteranalysis.methods;

import geoling.util.clusteranalysis.Cluster;
import geoling.util.clusteranalysis.ClusterAnalysis;
import geoling.util.clusteranalysis.ClusterObject;
import geoling.util.clusteranalysis.ClusteringResult;
import geoling.util.clusteranalysis.linkage.CachedLinkage;
import geoling.util.clusteranalysis.linkage.LinkageMethod;
import geoling.util.clusteranalysis.termination.TerminationCriterion;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Agglomerative hierarchical clustering.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @see <a href="http://en.wikipedia.org/wiki/Hierarchical_clustering">Wikipedia: Hierarchical clustering</a>
 */
public class AgglomerativeHierarchicalClustering implements ClusterAnalysis {
	
	/** The linkage-method to determine distances between clusters. */
	private LinkageMethod linkage;
	
	/** The termination-criterion to use. */
	private TerminationCriterion termination;
	
	/**
	 * Constructs a new object for agglomerative hierarchical clustering.
	 * 
	 * @param linkage     the linkage-method to determine distances between clusters
	 * @param termination the termination-criterion to use
	 */
	public AgglomerativeHierarchicalClustering(LinkageMethod linkage, TerminationCriterion termination) {
		this.linkage     = linkage;
		this.termination = termination;
	}
	
	/**
	 * Computes the resulting clusters according to this method.
	 * 
	 * @param objects  the set of objects that shall be clustered
	 * @return the set of clusters
	 */
	public ClusteringResult clusterAnalysis(Collection<? extends ClusterObject> objects) {
		ArrayList<ClusterObject> objectsList = new ArrayList<ClusterObject>(objects);
		
		CachedLinkage cachedLinkage = new CachedLinkage(this.getInitialClusters(objectsList), this.linkage);
		ClusteringResult result = new ClusteringResult(cachedLinkage.getClusters(), false);
		
		// aggregate clusters as long as we should not terminate
		while (!this.termination.shouldTerminate(result, cachedLinkage)) {
			CachedLinkage.ClusterPair pair = cachedLinkage.getSmallestDistancePair();
			
			// join clusters
			for (ClusterObject object : pair.cluster2.getObjects()) {
				pair.cluster1.put(object, 1.0);
			}
			pair.cluster2.clear();
			
			// remove empty cluster
			cachedLinkage.removeCluster(pair.cluster2);
			// recompute distances to modified cluster
			cachedLinkage.recomputeDistancesToCluster(pair.cluster1);
			
			result = new ClusteringResult(cachedLinkage.getClusters(), false);
		}
		
		return result;
	}
	
	/**
	 * Generates the initial clusters, i.e., one object per cluster.
	 * 
	 * @param objects  the list of objects
	 * @return the set of clusters
	 */
	private ArrayList<Cluster> getInitialClusters(ArrayList<ClusterObject> objects) {
		ArrayList<Cluster> singleObjectClusters = new ArrayList<Cluster>(objects.size());
		for (ClusterObject object : objects) {
			Cluster cluster = new Cluster();
			cluster.put(object, 1.0);
			singleObjectClusters.add(cluster);
		}
		return singleObjectClusters;
	}
	
}
