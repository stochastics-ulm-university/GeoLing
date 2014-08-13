package geoling.util.clusteranalysis.termination;

import geoling.util.clusteranalysis.ClusteringResult;
import geoling.util.clusteranalysis.linkage.LinkageMethod;

/**
 * This interface provides a general method for clustering algorithms
 * to decide whether they should terminate (e.g. based on the number of
 * clusters in hierarchical clustering).
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public interface TerminationCriterion {
	
	/**
	 * Determines whether we should terminate the iteration process
	 * of the clustering algorithm.
	 * 
	 * @param currentResult  the current clusters
	 * @param linkage        the linkage-method used to determine distances between clusters
	 * @return <code>true</code> if the clustering should terminate
	 *         (and the current result should be the final result)
	 */
	public boolean shouldTerminate(ClusteringResult currentResult, LinkageMethod linkage);
	
}
