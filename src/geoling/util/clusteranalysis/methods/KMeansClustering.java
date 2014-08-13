package geoling.util.clusteranalysis.methods;

import geoling.util.clusteranalysis.Cluster;
import geoling.util.clusteranalysis.ClusterAnalysis;
import geoling.util.clusteranalysis.ClusterObject;
import geoling.util.clusteranalysis.ClusteringResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * K-means cluster algorithm (KMC).
 * 
 * @author student assistant, Institute of Stochastics, Ulm University
 */
public class KMeansClustering implements ClusterAnalysis {
	
	/** The number of clusters. */
	private int c;
	
	/**
	 * Constructs a new k-means cluster analysis object.
	 * 
	 * @param c  the number of clusters
	 */
	public KMeansClustering(int c) {
		this.c = c;
	}
	
	/**
	 * Computes the resulting clusters according to this method.
	 * 
	 * @param objects  the set of objects that shall be clustered
	 * @return the set of clusters or <code>null</code> if an empty cluster occured
	 */
	public ClusteringResult clusterAnalysis(Collection<? extends ClusterObject> objects) {
		ArrayList<ClusterObject> objectsList = new ArrayList<ClusterObject>(objects);
		
		ArrayList<double[]> allCoordinates = new ArrayList<double[]>();
		for (int i = 0; i < objectsList.size(); i++) {
			allCoordinates.add(objectsList.get(i).getCoordinates());
		}
		
		/**
		 * Every point belongs to one, and only one, cluster. U[i][j]=1 means
		 * that the j-th point belongs to the i-th cluster
		 */
		int[][] U = new int[c][objectsList.size()];
		
		/** Random initial configuration. */
		
		/**
		 * At first a random permutation is used to make sure that every cluster
		 * will contain at least one point.
		 */
		int[] clusterNumberPerm = randomPermut(c);
		int[] pointNumberPerm = randomPermut(objectsList.size());
		
		for (int i = 0; i < c; i++) {
			U[clusterNumberPerm[i]][pointNumberPerm[i]] = 1;
		}
		
		boolean doWeContinue = false;
		do {
			
			/** Calculates cluster centers */
			ArrayList<double[]> clusterCenters = new ArrayList<double[]>();
			for (int i = 0; i < c; i++) {
				double[] tmp = new double[allCoordinates.get(0).length];
				double counter = 0.0;
				
				for (int j = 0; j < objectsList.size(); j++) {
					if (U[i][j] == 1) {
						counter = counter + 1.0;
						for (int k = 0; k < tmp.length; k++) {
							tmp[k] = tmp[k] + allCoordinates.get(j)[k];
							
						}
						
					}
				}
				for (int l = 0; l < tmp.length; l++) {
					tmp[l] = tmp[l] / counter;
				}
				
				clusterCenters.add(tmp);
				
			}
			
			/** Calculates the new Matrix U */
			double[] distanceToOwnClusterCenter = new double[objectsList.size()];
			int[] argminOfMinDist = new int[objectsList.size()];
			for (int i = 0; i < objectsList.size(); i++) {
				
				double[] currentPoint = allCoordinates.get(i);
				
				/** Calculates the distance to every cluster center */
				double currentMinSquaredDistance = Double.POSITIVE_INFINITY;
				for (int j = 0; j < c; j++) {
					
					double[] currentCenter = clusterCenters.get(j);
					double currentSquaredDistance = 0.0;
					
					for (int k = 0; k < currentCenter.length; k++) {
						currentSquaredDistance = currentSquaredDistance + Math.pow(currentCenter[k] - currentPoint[k], 2.0);
					}
					
					if (U[j][i] == 1) {
						distanceToOwnClusterCenter[i] = currentSquaredDistance;
					}
					
					if (currentSquaredDistance < currentMinSquaredDistance) {
						currentMinSquaredDistance = currentSquaredDistance;
						argminOfMinDist[i] = j;
					}
					
				}
			}
			
			doWeContinue = false;
			int[][] temporaryU = new int[c][objectsList.size()];
			for (int i = 0; i < objectsList.size(); i++) {
				for (int j = 0; j < c; j++) {
					if (j == argminOfMinDist[i]) {
						temporaryU[j][i] = 1;
					}
				}
			}
			
			for (int i = 0; i < c; i++) {
				boolean illegalClustering = true;
				for (int j = 0; j < objectsList.size(); j++) {
					
					if (temporaryU[i][j] - U[i][j] != 0) {
						doWeContinue = true;
					}
					
					if (U[i][j] == 1) {
						illegalClustering = false;
					}
					
				}
				if (illegalClustering) {
					illegalClustering = true;
					
					return null;
				}
			}
			
			U = temporaryU;
		} while (doWeContinue);
		
		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		for (int j = 0; j < c; j++) {
			Cluster currentCluster = new Cluster();
			for (int i = 0; i < objectsList.size(); i++) {
				if (U[j][i] == 1) {
					currentCluster.put(objectsList.get(i), 1.0);
				}
			}
			clusters.add(currentCluster);
		}
		
		return new ClusteringResult(clusters, false);
	}
	
	/**
	 * Returns in an integer array a random permutation of the numbers <code>1,...,m</code>.
	 * 
	 * @param m  the largest number
	 * @return the random permutation
	 **/
	private static int[] randomPermut(int m) {
		Random random = new Random();
		
		int[] result = new int[m];
		for (int i = 0; i < m; i++) {
			result[i] = i;
		}
		
		for (int i = 0; i < m; i++) {
			int j;
			if (m - i - 1 != 0) {
				j = random.nextInt(m - i - 1);
			} else {
				j = 0;
			}
			int k = result[i];
			result[i] = result[j];
			result[j] = k;
		}
		
		return result;
	}
	
}