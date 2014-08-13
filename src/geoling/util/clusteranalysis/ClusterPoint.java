package geoling.util.clusteranalysis;

import java.util.Arrays;

/**
 * Point in arbitrary dimension with optional weight that is used as
 * an object in a clustering. 
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class ClusterPoint implements ClusterObject {
	
	/** The coordinates of this point, with arbitrary dimension. */
	private double[] coordinates;
	
	/** The weight of this point, by default <code>1.0</code>. */
	private double weight;
	
	/**
	 * Constructs a new (clustering) point with default weight.
	 * 
	 * @param coordinates  the coordinates
	 */
	public ClusterPoint(double[] coordinates) {
		this(coordinates, 1.0);
	}
	
	/**
	 * Constructs a new (clustering) point with the given weight.
	 * 
	 * @param coordinates  the coordinates
	 * @param weight       the weight of the point
	 */
	public ClusterPoint(double[] coordinates, double weight) {
		this.coordinates = coordinates;
		this.weight      = weight;
	}
	
	/**
	 * Returns whether this object has coordinates (which is required
	 * for e.g. computation of Euclidean distances between objects).
	 * 
	 * @return always <code>true</code>
	 */
	public boolean hasCoordinates(){
		return true;
	}
	
	/**
	 * Returns the coordinates of this point.
	 * 
	 * @return the coordinates
	 */
	public double[] getCoordinates(){
		return this.coordinates;
	}	
	
	/**
	 * Returns an optional weight for this object, the default
	 * weight is <code>1.0</code>.
	 * Depending on e.g. the distance measure, this weight may
	 * be used or not.
	 * 
	 * @return the weight
	 */
	public double getWeight(){
		return this.weight;
	}
	
	/**
	 * Returns a string representation of this point.
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString(){
		return "Point: "+Arrays.toString(this.coordinates);
	}
	
}