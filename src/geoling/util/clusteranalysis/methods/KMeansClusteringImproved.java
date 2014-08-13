package geoling.util.clusteranalysis.methods;

import geoling.util.clusteranalysis.Cluster;
import geoling.util.clusteranalysis.ClusterAnalysis;
import geoling.util.clusteranalysis.ClusterObject;
import geoling.util.clusteranalysis.ClusteringResult;
import geoling.util.sim.grain.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class is an improved implementation of the k-means cluster algorithm.
 * This means that this class realizes the KMC algorithm with the same
 * parameters several times. After the algorithm has been done several times,
 * the optimal result will be chosen and returned. Due to the fact that the
 * result of a KMC depends on the random initial configuration of the cluster
 * centers the described strategy is useful.
 * 
 * @author student assistant, Institute of Stochastics, Ulm University
 */
public class KMeansClusteringImproved implements ClusterAnalysis {
	
	/** The object for obtaining a single result of the cluster analysis, without repetitions etc. */
	private KMeansClustering singleAnalysis;
	
	/** The cluster analysis will be performed several times, this variable determines how often the analysis will be repeated. */
	private int repeat;
	
	/** The algorithm allows empty clusters, this parameter determines how often the occurrence of empty clusters is allowed before the algorithm throws an exception. */
	private int retries;
	
	/**
	 * Constructs a new k-means cluster analysis object.
	 * 
	 * @param c        the number of clusters
	 * @param repeat   determines how often the cluster analysis will be done
	 * @param retries  determines how often the occurrence of empty clusters is
	 *                 allowed before the algorithm throws an exception
	 */
	public KMeansClusteringImproved(int c, int repeat, int retries) {
		this.singleAnalysis = new KMeansClustering(c);
		this.repeat  = repeat;
		this.retries = retries;
	}
	
	/**
	 * Computes the resulting clusters according to this method.
	 * 
	 * @param objects  the set of objects that shall be clustered
	 * @return the set of clusters
	 */
	public ClusteringResult clusterAnalysis(Collection<? extends ClusterObject> objects) {
		ArrayList<ClusteringResult> allResults = new ArrayList<ClusteringResult>();
		
		for (int i = 0; i < repeat; i++) {
			ClusteringResult temporaryClusteringResult = null;
			
			for (int j = 0; j <= retries; j++) {
				temporaryClusteringResult = singleAnalysis.clusterAnalysis(objects);
				if (temporaryClusteringResult != null) {
					break;
				}
			}
			
			if (temporaryClusteringResult == null) {
				throw new RuntimeException("Cannot compute clustering result, "+retries+" tries yielded empty clusters!");
			}
			
			allResults.add(temporaryClusteringResult);
		}
		
		/**
		 * Finding the best clustering. That means the clustering where the sum
		 * of all euclidean distances between every point in a cluster to every
		 * other point of the same cluster, is minimal.
		 */
		/**
		 * Does the Fuzzy cluster analysis for each possible value of c and
		 * transforms it to a hard cluster analysis.
		 */
		int indexOfTheBestCluster = 0;
		double minimalNorm = Double.POSITIVE_INFINITY;
		for (int l = 0; l < repeat; l++) {
			ClusteringResult currentClusteringResult = allResults.get(l);
			ArrayList<Cluster> currentClusters = currentClusteringResult.getClusters();
			double sumOfNorms = 0.0;
			
			for (int i = 0; i < currentClusters.size(); i++) {
				Cluster currentCluster = currentClusters.get(i);
				List<ClusterObject> currentClusterObjects = currentCluster.getObjects();
				double tmp = 0.0;
				for (int j = 0; j < currentCluster.size(); j++) {
					for (int k = j + 1; k < currentCluster.size(); k++) {
						tmp = tmp + Point.distance(currentClusterObjects.get(j).getCoordinates(), currentClusterObjects.get(k).getCoordinates());
					}
				}
				sumOfNorms = sumOfNorms + tmp;
			}
			sumOfNorms = sumOfNorms / currentClusters.size();
			
			if (sumOfNorms <= minimalNorm) {
				minimalNorm = sumOfNorms;
				indexOfTheBestCluster = l;
			}
		}
		
		return allResults.get(indexOfTheBestCluster);
	}
	
}