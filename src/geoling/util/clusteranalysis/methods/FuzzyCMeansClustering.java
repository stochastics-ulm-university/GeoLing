package geoling.util.clusteranalysis.methods;

import geoling.util.clusteranalysis.Cluster;
import geoling.util.clusteranalysis.ClusterAnalysis;
import geoling.util.clusteranalysis.ClusterObject;
import geoling.util.clusteranalysis.ClusteringResult;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class is an implementation of the fuzzy c-means clustering (FCMC) algorithm.
 * 
 * @author student assistant, Institute of Stochastics, Ulm University
 * @see "Robert L Cannon, Jitendra V Dave, James C Bezdek, Efficient Implementation
 *       of the Fuzzy c-Means Clustering Algorithm, 1986"
 */
public class FuzzyCMeansClustering implements ClusterAnalysis {
	
	/** The number of clusters. */
	private int c;
	
	/** Parameter of the algorithm, typically between <code>1.1</code> and <code>5.0</code>. */
	private double m;
	
	/** The algorithm will terminate if and only if the Frobenius norm between <code>U_b</code> and <code>U_(b+1)</code> is less than <code>epsilon</code>. */
	private double epsilon;
	
	/**
	 * Constructs a new fuzzy c-means cluster analysis object.
	 * 
	 * @param c        the number of clusters
	 * @param m        parameter of the algorithm, typically between <code>1.1</code> and
	 *                 <code>5.0</code>, see also the paper of Cannon et al.
	 * @param epsilon  defines when the algorithm will terminated (the algorithm will
	 *                 terminate if and only if the Frobenius norm between <code>U_b</code>
	 *                 and <code>U_(b+1)</code> is less than <code>epsilon</code>)
	 */
	public FuzzyCMeansClustering(int c, double m, double epsilon) {
		if (c <= 0) {
			throw new IllegalArgumentException("The number of clusters must be positive!");
		}
		if (m <= 0.0) {
			throw new IllegalArgumentException("The parameter m must be positive!");
		}
		if (epsilon <= 0.0) {
			throw new IllegalArgumentException("Epsilon must be positive!");
		}
		
		this.c = c;
		this.m = m;
		this.epsilon = epsilon;
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
		
		if (c > objectsList.size()) {
			throw new IllegalArgumentException("The number of clusters can't be greater than the number of objects which shall be clustered!");
		}
		int p = objectsList.get(0).getCoordinates().length - 1;
				
		/**
		 * The value U[i][j] is the probability with which the j-th cluster
		 * Object belongs to Cluster i.
		 */
		
		double[][] U = new double[c][objectsList.size()];
		
		/** Initialize the entries of U randomly. */
		for (int j = 0; j < objectsList.size(); j++) {
			double sum = 0.0;
			for (int i = 0; i < c; i++) {
				U[i][j] = Math.random();
				sum = sum + U[i][j];
			}
			
			for (int i = 0; i < c; i++) {
				U[i][j] = U[i][j] / sum;
			}
			
		}
		
		boolean doWeContinue;
		do {
			/**
			 * Calculates the c Cluster centers, where clusterCenters.get(i) is
			 * the center of the i-th cluster.
			 */
			double[][] clusterCenters = new double[c][p + 1];
			
			for (int i = 0; i < c; i++) {
				double tmp;
				for (int l = 0; l < p + 1; l++) {
					tmp = 0;
					double divide = 0;
					for (int k = 0; k < objectsList.size(); k++) {
						tmp = tmp + Math.pow(U[i][k], ((double) m)) * allCoordinates.get(k)[l];
						divide = divide + Math.pow(U[i][k], ((double) m));
					}
					tmp = tmp / divide;
					clusterCenters[i][l] = tmp;
				}
			}
			
			/** Compute the new values of U */
			double[][] Utmp = new double[c][objectsList.size()];
			for (int i = 0; i < c; i++) {
				for (int k = 0; k < objectsList.size(); k++) {
					double tmp = 0;
					for (int j = 0; j < c; j++) {
						tmp = tmp + Math.pow((norm(allCoordinates, clusterCenters, i, k) / norm(allCoordinates, clusterCenters, j, k)), (2.0) / (((double) m) - 1.0));
					}
					Utmp[i][k] = 1.0 / tmp;
				}
			}
			
			doWeContinue = isMatrixNormLessThanEpsilon(U, Utmp);
			U = Utmp;
			
		} while (doWeContinue);
		
		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		for (int i = 0; i < c; i++) {
			Cluster tmpCluster = new Cluster();
			for (int j = 0; j < objectsList.size(); j++) {
				
				tmpCluster.put(objectsList.get(j), U[i][j]);
			}
			clusters.add(tmpCluster);
		}
		
		return new ClusteringResult(clusters, true);
	}
	
	/**
	 * Calculates the Frobenius norm between <code>U</code> and <code>Utmp</code>.
	 * 
	 * @param U     a matrix
	 * @param Utmp  a matrix with the same dimensions as <code>U</code>
	 * @return <code>false</code> when the Frobenius norm is less than
	 *         <code>epsilon</code>, <code>true</code> otherwise.
	 */
	private boolean isMatrixNormLessThanEpsilon(double[][] U, double[][] Utmp) {
		double frobNorm = 0;
		for (int i = 0; i < U.length; i++) {
			for (int j = 0; j < U[i].length; j++) {
				frobNorm = frobNorm + Math.pow((U[i][j] - Utmp[i][j]), 2.0);
			}
		}
		
		if (frobNorm < epsilon) {
			return false;
		} else {
			return true;
		}		
	}
	
	/**
	 * Calculates the Euclidean norm between the <code>k</code>-th point and the
	 * <code>i</code>-th cluster center.
	 * 
	 * @param allCoordinates  the coordinates of the cluster objects
	 * @param clusterCenters  the centers of the current clusters
	 * @param i               any integer between 1 and the number of clusters
	 * @param k               any integer between 1 and the number of objects
	 * @return the Euclidean norm
	 */
	private double norm(ArrayList<double[]> allCoordinates, double[][] clusterCenters, int i, int k) {
		double result = 0;
		
		for (int j = 0; j < clusterCenters[i].length; j++) {
			double diff = allCoordinates.get(k)[j] - clusterCenters[i][j];
			result += diff * diff;
		}
		
		result = Math.sqrt(result);
		if (result == 0) {
			throw new RuntimeException("Result of function 'norm' is zero, but this is probably a problem, at least a special case was implemented here. For now, it is safer to let the program crash...");
		}
		
		return result;
	}
	
}