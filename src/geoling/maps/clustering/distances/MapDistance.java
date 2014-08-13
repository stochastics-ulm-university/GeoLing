package geoling.maps.clustering.distances;

import geoling.util.clusteranalysis.distance.ClusterObjectDistance;

/**
 * Common interface for distance measures to be computed between
 * area-class-maps.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public interface MapDistance extends ClusterObjectDistance {
	
	/**
	 * Returns an identification string for this map distance measure.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString();
	
}