package geoling.util.clusteranalysis;

import java.util.Collection;

/**
 * Common interface for various clustering methods.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public interface ClusterAnalysis {
	
	/**
	 * Computes the resulting clusters according to this method.
	 * 
	 * @param objects  the set of objects that shall be clustered
	 * @return the set of (maybe fuzzy) clusters
	 */
	public ClusteringResult clusterAnalysis(Collection<? extends ClusterObject> objects);
	
}