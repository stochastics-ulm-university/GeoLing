package geoling.util.clusteranalysis.termination;

import geoling.util.clusteranalysis.ClusteringResult;
import geoling.util.clusteranalysis.linkage.LinkageMethod;

/**
 * Termination criterion for clustering: a predefined number of
 * clusters is reached.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class NumberOfClusters implements TerminationCriterion {
	
	/** The predefined number of clusters we want to have. */
	private int targetNumber;
	
	/**
	 * Constructs a new termination criterion object.
	 * 
	 * @param targetNumber  the number of clusters we want to have
	 */
	public NumberOfClusters(int targetNumber) {
		this.targetNumber = targetNumber;
	}
	
	/**
	 * Determines whether we should terminate the iteration process
	 * of the clustering algorithm.
	 * 
	 * @param currentResult  the current clusters
	 * @param linkage        the linkage-method used to determine distances between clusters
	 * @return <code>true</code> if the clustering should terminate
	 *         (and the current result should be the final result)
	 */
	public boolean shouldTerminate(ClusteringResult currentResult, LinkageMethod linkage) {
		return (currentResult.getNumberOfClusters() == this.targetNumber);
	}
	
}