package geoling.locations;

import geoling.locations.util.AggregatedLocation;
import geoling.models.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A <code>LocationAggregator</code> object which does not aggregate any locations,
 * they are just passed through.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class LocationPassthrough implements LocationAggregator {
	
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
		ArrayList<AggregatedLocation> result = new ArrayList<AggregatedLocation>(locations.size());
		for (Location location : locations) {
			result.add(new AggregatedLocation(this, Arrays.asList(new Location[] { location })));
		}
		return result;
	}
	
	/**
	 * Returns an identification string for this type of location aggregation.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		return "location_passthrough";
	}
	
}
