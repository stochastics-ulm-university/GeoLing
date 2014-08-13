package geoling.locations;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.util.LocationGrid;
import geoling.models.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * A <code>LocationAggregator</code> object which does aggregate locations
 * according to their distance, i.e., their distance has to be below a
 * certain (small) distance.
 * <p>
 * Locations are aggregated if their distance is smaller than 1 metre, by
 * default. This distance has to be very small, because only geographical
 * coordinates essentially the same should be aggregated. For arbitrary
 * distances it is not clear whether the aggregation is uniquely determined.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class SimilarCoordinatesAggregation implements LocationAggregator {
	
	/** Default tolerance in kilometres for locations that should be aggregated. */
	private static final double DEFAULT_TOLERANCE = 0.001;
	
	/** Tolerance in kilometres for locations that should be aggregated. */
	private final double tolerance;
	
	/**
	 * Constructs a new object for aggregation of locations which have
	 * similar geographical coordinates, i.e., the locations have less than
	 * 1 metre distance.
	 */
	public SimilarCoordinatesAggregation() {
		this(DEFAULT_TOLERANCE);
	}
	
	/**
	 * Constructs a new object for aggregation of locations which have
	 * similar geographical coordinates, i.e., the locations are within
	 * a certain distance.
	 * 
	 * @param tolerance  the distance threshold, in kilometres
	 */
	public SimilarCoordinatesAggregation(double tolerance) {
		this.tolerance = tolerance;
	}
	
	/**
	 * Returns the set of aggregated locations where one object stand exactly for
	 * one location in the database.
	 * <p>
	 * Note that you should not call this method more than necessary, you should
	 * reuse the "old" aggregated locations whenever possible. (Here, the aggregated
	 * locations are usually reconstructed, i.e., they are new objects.)
	 * 
	 * @param locations  the set of the locations that should be aggregated
	 * @return the set of aggregated locations
	 */
	public List<AggregatedLocation> getAggregatedLocations(Collection<Location> locations) {
		LocationGrid grid = new LocationGrid(locations);
		
		ArrayList<AggregatedLocation> result = new ArrayList<AggregatedLocation>(locations.size());
		
		HashSet<Location> done = new HashSet<Location>();
		for (Location location : locations) {
			if (done.contains(location)) {
				continue;
			}
			
			ArrayList<Location> neighbours = grid.findLocationsInDistance(location.getLatLong(), this.tolerance, false);
			
			if (neighbours.isEmpty()) {
				throw new RuntimeException("Error: something went wrong, didn't even find the location that is known to be at the given coordinates!");
			} else if (neighbours.size() == 1) {
				result.add(new AggregatedLocation(this, Arrays.asList(new Location[] { location })));
			} else {
				ArrayList<Location> aggregateLocations = new ArrayList<Location>();
				for (Location neighbour : neighbours) {
					aggregateLocations.add(neighbour);
					if (!done.add(neighbour)) {
						throw new RuntimeException("Error: location aggregation by distance ("+this.tolerance+" km) is not uniquely determined in this case!");
					}
				}
				result.add(new AggregatedLocation(this, aggregateLocations));
			}
		}
		
		return result;
	}
	
	/**
	 * Returns an identification string for this type of location aggregation.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		if (this.tolerance == DEFAULT_TOLERANCE) {
			return "similar_coordinates";
		} else {
			return "similar_coordinates:tolerance="+this.tolerance;
		}
	}
	
}
