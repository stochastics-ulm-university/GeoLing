package geoling.util.clusteranalysis;

/**
 * Interface for objects that shall be clustered.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public interface ClusterObject {
	
	/**
	 * Returns whether this object has coordinates (which is required
	 * for e.g. computation of Euclidean distances between objects).
	 * 
	 * @return <code>true</code> if the object has coordinates
	 */
	public boolean hasCoordinates();
	
	/**
	 * Returns the coordinates of this object.
	 * 
	 * @return the coordinates
	 * @throws UnsupportedOperationException if this object has no coordinates
	 */
	public double[] getCoordinates();
	
	/**
	 * Returns an optional weight for this object, the default
	 * weight is <code>1.0</code>.
	 * Depending on e.g. the distance measure, this weight may
	 * be used or not.
	 * 
	 * @return the weight
	 */
	public double getWeight();
	
}
