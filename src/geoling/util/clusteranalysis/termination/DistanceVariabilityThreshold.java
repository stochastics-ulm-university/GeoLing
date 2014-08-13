package geoling.util.clusteranalysis.termination;

import geoling.util.clusteranalysis.ClusterObject;
import geoling.util.clusteranalysis.ClusteringResult;
import geoling.util.clusteranalysis.distance.ClusterObjectDistance;
import geoling.util.clusteranalysis.linkage.CachedLinkage;
import geoling.util.clusteranalysis.linkage.LinkageMethod;

import java.util.List;

/**
 * Termination criterion for clustering: the variability between clusters is taken
 * into account, i.e., the clustering is terminated when the smallest distance
 * between two clusters is larger than their average distance plus the standard
 * deviation of their objects' distances scaled with a factor <code>k</code>.
 * <p>
 * The parameter <code>k</code> influences how long clustering is performed.
 * It controls the effect of the variability of distances on the termination
 * threshold, which is given by <code>m+k*s</code>, where <code>m</code> is
 * the average of all distances and <code>s</code> the empirical standard deviation
 * of the distances between the two clusters' object distances.
 * Clustering is aborted when the smallest distance between clusters exceeds this
 * threshold <code>k</code>. Larger values lead to bigger clusters, smaller values
 * to smaller clusters.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class DistanceVariabilityThreshold implements TerminationCriterion {
	
	/** The epsilon-value used for the threshold-inequality. */
	public static double EPS = 1E-8;
	
	/** The predefined number of clusters we want to have. */
	private double k;
	
	/**
	 * Constructs a new termination criterion object.
	 * 
	 * @param k  the parameter controlling when the clustering should end,
	 *           a typical value is <code>1.7</code>, larger values lead to
	 *           bigger clusters, smaller values to smaller clusters
	 */
	public DistanceVariabilityThreshold(double k) {
		this.k = k;
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
		if (linkage instanceof CachedLinkage) {
			CachedLinkage cachedLinkage = (CachedLinkage)linkage;
			CachedLinkage.ClusterPair pair = cachedLinkage.getSmallestDistancePair();
			if (pair == null) {
				// no pair found => only one cluster in total, abort
				return true;
			}
			
			// note: we use explicitly only float-precision in the following, this allows us to
			//       use float-storage for cached distances
			float smallestDistance  = (float)cachedLinkage.distance(pair.cluster1, pair.cluster2);
			float meanOfDistances   = (float)meanOfDistances(pair, linkage.getObjectDistance());
			float stddevOfDistances = (float)stddevOfDistances(pair, meanOfDistances, linkage.getObjectDistance());
			
			return (smallestDistance > meanOfDistances + k*stddevOfDistances + EPS);
		} else {
			throw new IllegalArgumentException("CachedLinkage expected!");
		}
	}
	
	/**
	 * Returns the average distance between the objects in the two clusters.
	 * 
	 * @param pair           the pair of clusters
	 * @param objectDistance the distance measure
	 * @return the average distance
	 */
	private double meanOfDistances(CachedLinkage.ClusterPair pair, ClusterObjectDistance objectDistance) {
		List<ClusterObject> objects1 = pair.cluster1.getObjects();
		List<ClusterObject> objects2 = pair.cluster2.getObjects();
		
		double sum = 0.0;
		int count = 0;
		for (ClusterObject object1 : objects1) {
			for (ClusterObject object2 : objects2) {
				sum += objectDistance.distance(object1, object2);
				count++;
			}
		}
		
		return sum / count;
	}
	
	/**
	 * Returns the standard deviation of the distances between the objects in the two clusters.
	 * 
	 * @param pair            the pair of clusters
	 * @param meanOfDistances the average distance, which has been already computed
	 * @param objectDistance  the distance measure
	 * @return the standard deviation of the distances
	 */
	private double stddevOfDistances(CachedLinkage.ClusterPair pair, double meanOfDistances, ClusterObjectDistance objectDistance) {
		if (pair.cluster1.size()*pair.cluster2.size() <= 1) {
			return 0.0;
		}
		
		List<ClusterObject> objects1 = pair.cluster1.getObjects();
		List<ClusterObject> objects2 = pair.cluster2.getObjects();
		
		double sum = 0.0;
		int count = 0;
		for (ClusterObject object1 : objects1) {
			for (ClusterObject object2 : objects2) {
				double distance = objectDistance.distance(object1, object2);
				sum += (distance-meanOfDistances)*(distance-meanOfDistances);
				count++;
			}
		}
		
		return Math.sqrt(sum / (count-1));
	}
	
}