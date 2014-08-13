package geoling.maps.clustering.distances;

import geoling.util.clusteranalysis.linkage.LinkageMethod;

/**
 * Common interface for distance measures to be computed between
 * (hard) clusters of area-class-maps.
 * For example, it can be used to compute a distance between clusters
 * by evaluating distances for pairs of area-class-maps and combining
 * them via complete linkage, average linkage or single linkage.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public interface ClusterDistance extends LinkageMethod {
	
	/**
	 * Returns an identification string for this cluster distance measure.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString();
	
}