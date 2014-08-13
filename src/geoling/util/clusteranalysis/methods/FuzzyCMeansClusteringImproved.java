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
 * This class is an improved implementation of the fuzzy c-means clustering
 * (FCMC) algorithm. This means that this class determines the clusters with the
 * same parameters several times. After the algorithm has been done several
 * times, the optimal result will be chosen and returned. Due to the fact that
 * the result depends on a random matrix, the described strategy is
 * useful.
 * 
 * @author student assistant, Institute of Stochastics, Ulm University
 * @see "Robert L Cannon, Jitendra V Dave, James C Bezdek, Efficient Implementation
 *       of the Fuzzy c-Means Clustering Algorithm, 1986"
 */
public class FuzzyCMeansClusteringImproved implements ClusterAnalysis {
	
	/** The object for obtaining a single result of the cluster analysis, without repetitions etc. */
	private FuzzyCMeansClustering singleAnalysis;
	
	/** The cluster analysis will be performed several times, this variable determines how often the analysis will be repeated. */
	private int repeat;
	
	/**
	 * Constructs a new fuzzy c-means cluster analysis object.
	 * 
	 * @param c        the number of clusters
	 * @param m        parameter of the algorithm, typically between <code>1.1</code> and
	 *                 <code>5.0</code>, see also the paper of Cannon et al.
	 * @param epsilon  defines when the algorithm will terminated (the algorithm will
	 *                 terminate if and only if the Frobenius norm between <code>U_b</code>
	 *                 and <code>U_(b+1)</code> is less than <code>epsilon</code>)
	 * @param repeat   determines how often the cluster analysis will be done
	 */
	public FuzzyCMeansClusteringImproved(int c, double m, double epsilon, int repeat) {
		this.singleAnalysis = new FuzzyCMeansClustering(c, m, epsilon);
		this.repeat = repeat;
	}
	
	/**
	 * Computes the resulting clusters according to this method.
	 * 
	 * @param objects  the set of objects that shall be clustered
	 * @return the set of clusters
	 */
	public ClusteringResult clusterAnalysis(Collection<? extends ClusterObject> objects) {
		ArrayList<ClusteringResult> allResults = new ArrayList<ClusteringResult>();
		
		/** Doing the cluster analysis as often as intended. */
		for (int i = 0; i < repeat; i++) {
			allResults.add(singleAnalysis.clusterAnalysis(objects));
		}
		
		/**
		 * Finding the best clustering. That means the clustering where the sum
		 * of all euclidean distances between every point in a cluster to every
		 * other point of the same cluster, is minimal.
		 */
		int indexOfTheBestCluster = 0;
		double minimalNorm = Double.POSITIVE_INFINITY;
		for (int l = 0; l < repeat; l++) {
			// transform "fuzzy" results to "hard" results
			ClusteringResult currentClusteringResultHard = allResults.get(l).getHardResult();
			ArrayList<Cluster> currentClusters = currentClusteringResultHard.getClusters();
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