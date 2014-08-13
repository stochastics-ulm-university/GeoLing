package geoling.maps.util;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.projection.MapProjection;
import geoling.util.sim.grain.Polytope;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Class for caching of Voronoi maps, because we want to avoid recomputing
 * them all the time.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class VoronoiMapCache {
	
	/** Maximal number of Voronoi maps that are held in the cache. */
	private static final int MAX_SIZE = 10;
	
	/** The cached Voronoi maps. */
	private static LinkedList<VoronoiMap> cachedMaps = new LinkedList<VoronoiMap>();
	
	/**
	 * Returns a Voronoi map for the given locations, uses the given border.
	 * 
	 * @param locations     the set of (aggregated) locations
	 * @param border        the border polygon
	 * @param mapProjection the projection method for the coordinates
	 * @return the Voronoi map
	 */
	public static synchronized VoronoiMap getVoronoiMap(Collection<AggregatedLocation> locations, Polytope border, MapProjection mapProjection) {
		HashSet<AggregatedLocation> locationsSet = new HashSet<AggregatedLocation>(locations);
		
		// we look for a Voronoi map that has:
		// - the same set of locations
		// - the same border polygon, currently only checked by area and circumference, which should be sufficient
		// - the same map projection type
		mapLoop: for (VoronoiMap voronoiMap : cachedMaps) {
			if (voronoiMap.getLocations().size() != locationsSet.size()) {
				continue;
			}
			for (AggregatedLocation location : voronoiMap.getLocations()) {
				if (!locationsSet.contains(location)) {
					continue mapLoop;
				}
			}
			
			if (!MapBorder.bordersAreEqual(voronoiMap.getBorder(), border)) {
				continue;
			}
			
			if (!mapProjection.isSimilar(voronoiMap.getMapProjection())) {
				continue;
			}
			
			return voronoiMap;
		}
		
		if (cachedMaps.size() >= MAX_SIZE) {
			cachedMaps.removeLast();
		}
		
		VoronoiMap result = new VoronoiMap(locations, border, mapProjection);
		cachedMaps.addFirst(result);
		return result;
	}
	
}