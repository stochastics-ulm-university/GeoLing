package geoling.maps.distances;

import geoling.models.Location;
import geoling.util.LatLong;

/**
 * Interface for different distance measures.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public interface DistanceMeasure {
	
	public static class LatLongNotSupportedException extends IllegalArgumentException {
		private static final long serialVersionUID = 4279173725683512844L;
		
		public LatLongNotSupportedException() {
			super("Arbitrary geographical coordinatinates not supported for this distance measure!");
		}
	}
	
	/**
	 * Returns an identification string for this distance measure.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString();
	
	/**
	 * Computes the distance between two geographical coordinates.
	 * 
	 * @param latLong1  the first geographical coordinate
	 * @param latLong2  the second geographical coordinate
	 * @return the distance
	 * @throws LatLongNotSupportedException if the distance measure only supports the distance between locations
	 */
	public double getDistance(LatLong latLong1, LatLong latLong2) throws LatLongNotSupportedException;
	
	/**
	 * Computes the distance between two locations.
	 * 
	 * @param location1  the first location
	 * @param location2  the second location
	 * @return the distance
	 */
	public double getDistance(Location location1, Location location2);
	
}
