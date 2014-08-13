package geoling.util.clusteranalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A single hard or fuzzy cluster.
 * In a hard cluster, every contained object has probability one, whereas
 * in a fuzzy cluster, every contained objects has a positive probability
 * (giving the likelihood that it is contained in this cluster).
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Cluster {
	
	/** The set of objects and their assigned probabilities. */
	private HashMap<ClusterObject,Double> objects;
	
	/**
	 * Constructs a new empty cluster.
	 */
	public Cluster() {
		this.objects = new HashMap<ClusterObject,Double>();
	}
	
	/**
	 * Returns all objects in this cluster as a list.
	 * 
	 * @return a list of all objects in this cluster
	 */
	public List<ClusterObject> getObjects() {
		return Collections.unmodifiableList(new ArrayList<ClusterObject>(this.objects.keySet()));
	}
	
	/**
	 * Returns the probability of an object belonging to this
	 * cluster.
	 * 
	 * @param obj  the object
	 * @return the probability or <code>0.0</code> if the object
	 *         is not contained in this cluster
	 */
	public double getProbability(ClusterObject obj) {
		return this.objects.get(obj);
	}
	
	/**
	 * Inserts a new object into this cluster.
	 * If the given object is already contained, its probability
	 * is updated.
	 *  
	 * @param obj          the object
	 * @param probability  the probability; if <code>0.0</code>, the
	 *                     object is removed from this cluster
	 */
	public void put(ClusterObject obj, double probability) {
		if (probability > 0.0) {
			this.objects.put(obj, probability);
		} else {
			this.objects.remove(obj);
		}
	}
	
	/**
	 * Removes all objects from this cluster.
	 */
	public void clear() {
		this.objects.clear();
	}
	
	/**
	 * Returns the number of objects in this cluster.
	 * 
	 * @return the number of objects
	 */
	public int size() {
		return this.objects.size();
	}
	
}