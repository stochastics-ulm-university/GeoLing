package geoling.maps.clustering.distances;

import geoling.util.clusteranalysis.distance.ClusterObjectDistance;
import geoling.util.clusteranalysis.linkage.CompleteLinkage;

/**
 * This class calculates the distance of 2 clusters by using complete linkage.
 * 
 * @author student assistant, Institute of Stochastics, Ulm University
 * @see "Jonas Rumpf, Simon Pickl, Stephan Elspaß, Werner König, Volker Schmidt,
 *      Quantification and Statistical Analysis of Structural Similarities in
 *      Dialectological Area-Class Maps, page 82 (Section: 3.2)"
 */
public class ClusterDistanceByCompleteLinkage extends CompleteLinkage implements ClusterDistance {
	
	/**
	 * 
	 * @param m  any MapDistance which will define with which method the
	 *           distance between any two AreaClassMaps will be calculated
	 */
	public ClusterDistanceByCompleteLinkage(ClusterObjectDistance m) {
		super(m);
	}
	
	public String getIdentificationString() {
		return getStaticIdentificationString();
	}
	
	/**
	 * Returns an identification string for this cluster distance type.
	 * 
	 * @return the identification string
	 */
	public static String getStaticIdentificationString() {
		return "complete_linkage";
	}
	
}
