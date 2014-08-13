package geoling.util.clusteranalysis.methods;

import geoling.util.clusteranalysis.Cluster;
import geoling.util.clusteranalysis.ClusterAnalysis;
import geoling.util.clusteranalysis.ClusterObject;
import geoling.util.clusteranalysis.ClusteringResult;
import geoling.util.sim.grain.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.ProgressMonitor;

/**
 * This class is an extension of the KMC algorithm. The extension is that this
 * algorithm does not need a specific number of clusters but that it finds the
 * optimal number of clusters. The algorithm works only if the number of cluster
 * objects is greater than 4. Due to numerical reasons the optimal number of
 * clusters can't be equal to one.
 * 
 * @author student assistant, Institute of Stochastics, Ulm University
 */
public class KMeansClusteringExtended implements ClusterAnalysis {
	
	/** The (improved) KMC analysis will be performed several times, this variable determines how often the analysis will be repeated. */
	private int repeat;
	
	/** The (improved) KMC algorithm allows empty clusters, this parameter determines how often the occurrence of empty clusters is allowed before the algorithm throws an exception. */
	private int retries;
	
	/**
	 * The cluster analysis will find the optimal value for the number of
	 * clusters in the interval <code>[minValue,maxValue]</code>. If either
	 * <code>minValue</code> or <code>maxValue</code> is negative or equals
	 * zero the algorithm will find the optimal value for every possible
	 * number of clusters which is less than the half of the number of
	 * cluster objects.
	 **/
	private int defaultMinValue, defaultMaxValue;
	
	/** Determines how often the (improved) k-means cluster analysis will be computed (again, for all numbers of clusters). */
	private int n;
	
	/** Progress monitor which visualises the progress if the analysis is used in an environment with GUI. */
	private ProgressMonitor pm;
	
	/**
	 * Constructs a new k-means cluster analysis object.
	 * 
	 * @param minValue the cluster analysis will find the optimal value for the
	 *                 number of clusters in the interval <code>[minValue,maxValue]</code>.
	 *                 If either <code>minValue</code> or <code>maxValue</code> is
	 *                 negative or equals zero the algorithm will find the optimal
	 *                 value for every possible number of clusters which is less
	 *                 than the half of the number of cluster objects
	 * @param maxValue the cluster analysis will find the optimal value for the
	 *                 number of clusters in the interval <code>[minValue,maxValue]</code>.
	 *                 If either <code>minValue</code> or <code>maxValue</code> is
	 *                 negative or equals zero the algorithm will find the optimal
	 *                 value for every possible number of clusters which is less
	 *                 than the half of the number of cluster objects
	 * @param repeat   determines how often the cluster analysis will be done
	 * @param retries  determines how often the occurrence of empty clusters is
	 *                 allowed before the algorithm throws an exception
	 * @param n        determines how often the (improved) k-means cluster analysis
	 *                 will be computed (again, for all numbers of clusters)
	 * @param pm       progress monitor which visualises the progress if the analysis is
	 *                 used in an environment with GUI
	 */
	public KMeansClusteringExtended(int minValue, int maxValue, int repeat, int retries, int n, ProgressMonitor pm) {
		this.defaultMinValue = minValue;
		this.defaultMaxValue = maxValue + 2;
		this.repeat          = repeat;
		this.retries         = retries;
		this.n               = n;
		this.pm              = pm;
	}
	
	/**
	 * Computes the resulting clusters according to this method.
	 * 
	 * @param objects  the set of objects that shall be clustered
	 * @return the set of clusters
	 */
	public ClusteringResult clusterAnalysis(Collection<? extends ClusterObject> objects) {
		/*
		 * This method runs the method <code>clusterAnalysisSingleSet</code> <code>n</code>
		 * -times. Due to this n optimal values for the cluster number have been calculated.
		 * This method determines the median of all these values. This median is the
		 * optimal number of clusters for the KMC algorithm. Finally this method
		 * does the KMC algorithm once more with the optimal number of Clusters. Due
		 * to the fact that this method calculates the median of a list, consisting
		 * of n elements, <code>n</code> should be an odd number.
		 */
		
		ArrayList<Integer> counter = new ArrayList<Integer>();
		
		if (pm != null) {
			pm.setProgress(0);
			pm.setMaximum(100);
		}
		
		for (int i = 0; i < n; i++) {
			if (pm != null) {
				if (pm.isCanceled()) {
					return null;
				}
				
				pm.setProgress((int) (100 * ((double) (i + 1)) / ((double) n)));
				pm.setNote("KMC Iteration: " + (i + 1) + " von: " + n);
				
			}
			ClusteringResult result = this.clusterAnalysisSingleSet(objects);
			counter.add(result.getNumberOfClusters());
		}
		int median = median(counter);
		
		KMeansClusteringImproved KMCA = new KMeansClusteringImproved(median, repeat, retries);
		return KMCA.clusterAnalysis(objects);
	}
	
	/**
	 * This method does the KMC analysis for every value in the interval
	 * <code>[minValue, maxValue]</code> for the number of clusters and
	 * returns the best result.
	 * 
	 * @param objects  the set of objects that shall be clustered
	 * @return the set of clusters
	 */
	private ClusteringResult clusterAnalysisSingleSet(Collection<? extends ClusterObject> objects) {
		ArrayList<ClusteringResult> results = new ArrayList<ClusteringResult>();
		ArrayList<Double> clusterIndexes = new ArrayList<Double>();
		
		int minValue = this.defaultMinValue;
		int maxValue = this.defaultMaxValue + 2;
		
		if (minValue <= 0 || maxValue <= 0) {
			minValue = 1;
			maxValue = objects.size() / 2 + 2;
		}
		if (maxValue > objects.size() / 2 + 2) {
			maxValue = objects.size() / 2 + 2;
		}
		if (minValue != 1) {
			minValue = minValue - 1;
		}
		
		if (minValue > maxValue) {
			throw new IllegalArgumentException("minValue cannot be greater than maxValue.");
		}
		if (maxValue > objects.size()) {
			throw new IllegalArgumentException("maxValue exceeds number of cluster objects.");
		}
		
		for (int c = minValue; c < maxValue; c++) {
			/**
			 * Does the Fuzzy cluster analysis for each possible value of c and
			 * transforms it to a hard cluster analysis.
			 */
			KMeansClusteringImproved KMCA = new KMeansClusteringImproved(c, repeat, retries);
			ClusteringResult currentClusteringResult = KMCA.clusterAnalysis(objects);
			
			if (currentClusteringResult == null) {
				throw new IllegalArgumentException("Unable to make cluster analysis. Please increase the parameter 'retries'. If this does not work please decrease the parameter maxValue, to a value which is less than: "+c);
			}
			
			/**
			 * Calculates the sum of all euclidean norms between every object to
			 * every other object in the same cluster.
			 */
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
			clusterIndexes.add(sumOfNorms / currentClusters.size());
			
			results.add(currentClusteringResult);
		}
		
		/** Calculates the differences between all neighbored clusterIndexes. */
		ArrayList<Double> slopes = new ArrayList<Double>();
		for (int i = 0; i < clusterIndexes.size() - 1; i++) {
			slopes.add(Math.abs(clusterIndexes.get(i + 1) - clusterIndexes.get(i)));
		}
		
		/** Calculates the ratio between the neighbored slopes. */
		ArrayList<Double> ratioBetweenSlopes = new ArrayList<Double>();
		
		for (int i = 0; i < slopes.size() - 1; i++) {
			ratioBetweenSlopes.add(slopes.get(i) / slopes.get(i + 1));
		}
		
		/** Calculates the optimal number of clusters. */
		double maximum = (double) Collections.max(ratioBetweenSlopes);
		int optimalNumberOfClusters = 0;
		
		for (int i = 0; i < ratioBetweenSlopes.size(); i++) {
			if (maximum == ratioBetweenSlopes.get(i)) {
				optimalNumberOfClusters = i + 2;
			}
		}
		
		return results.get(optimalNumberOfClusters - 1);
	}
	
	/**
	 * This method calculates the median of a given list (and truncates it to an
	 * integer, if necessary).
	 * 
	 * @param counter  the list of integer values
	 * @return the median of the integer values in the list
	 */
	private int median(ArrayList<Integer> counter) {
		Collections.sort(counter);
		
		int mid = counter.size() / 2;
		if (counter.size() % 2 == 1) {
			return counter.get(mid);
		} else {
			double resultValue = (counter.get(mid - 1) + counter.get(mid)) / 2.0;
			return (int)resultValue; // if not an integer, use only the integer part
		}
	}
	
}
