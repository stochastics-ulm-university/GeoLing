package geoling.maps.clustering.distances;

import geoling.util.clusteranalysis.distance.ClusterObjectDistance;
import geoling.util.clusteranalysis.linkage.AverageLinkage;

/**
 * This class calculates the distance of 2 clusters by using average linkage.
 * 
 * @author student assistant, Institute of Stochastics, Ulm University
 * @see "Jonas Rumpf, Simon Pickl, Stephan Elspaß, Werner König, Volker Schmidt,
 *      Quantification and Statistical Analysis of Structural Similarities in
 *      Dialectological Area-Class Maps, page 82 (Section: 3.2)"
 */
public class ClusterDistanceByAverageLinkage extends AverageLinkage implements ClusterDistance {
	
	/**
	 * 
	 * @param m  any MapDistance which will define with which method the
	 *           distance between any two AreaClassMaps will be calculated
	 */
	public ClusterDistanceByAverageLinkage(ClusterObjectDistance m) {
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
		return "average_linkage";
	}
	
}
