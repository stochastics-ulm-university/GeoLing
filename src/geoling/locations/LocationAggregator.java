package geoling.locations;

import geoling.locations.util.AggregatedLocation;
import geoling.models.Location;

import java.util.Collection;
import java.util.List;

/**
 * An interface for aggregating locations, which may be useful depending on
 * the geographical proximity of locations (in comparison to the size of the
 * complete map).
 * <p>
 * The requirement for classes implementing this interface is simple:
 * Every location is contained in exactly one aggregated location.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public interface LocationAggregator {
	
	/**
	 * Returns the set of aggregated locations, i.e., one object may stand for
	 * one or more locations in the database.
	 * <p>
	 * Note that you should not call this method more than necessary, you should
	 * reuse the "old" aggregated locations whenever possible. (Here, the aggregated
	 * locations are usually reconstructed, i.e., they are new objects.)
	 * 
	 * @param locations  the set of the locations that should be aggregated
	 * @return the set of aggregated locations
	 */
	public List<AggregatedLocation> getAggregatedLocations(Collection<Location> locations);
	
	/**
	 * Returns an identification string for this type of location aggregation,
	 * may be used to store values depending on (aggregated) locations and their
	 * IDs to the database.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString();
	
}