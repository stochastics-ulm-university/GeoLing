package geoling.maps.util;

import geoling.util.sim.grain.Polytope;

import java.util.LinkedList;

/**
 * Class for caching of border intersection helper objects, because we want to avoid
 * recomputing them all the time.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class MapBorderIntersectionCache {
	
	/** Maximal number of helper objects that are held in the cache. */
	private static final int MAX_SIZE = 10;
	
	/** The cached helper objects. */
	private static LinkedList<MapBorderIntersection> cachedObjects = new LinkedList<MapBorderIntersection>();
	
	/**
	 * Returns a map border intersection helper object.
	 * 
	 * @param border  the border polygon of the map
	 * @return the helper object
	 */
	public static synchronized MapBorderIntersection getHelperObject(Polytope border) {
		for (MapBorderIntersection obj : cachedObjects) {
			if (MapBorder.bordersAreEqual(obj.getBorder(), border)) {
				return obj;
			}
		}
		
		if (cachedObjects.size() >= MAX_SIZE) {
			cachedObjects.removeLast();
		}
		
		MapBorderIntersection result = new MapBorderIntersection(border);
		cachedObjects.addFirst(result);
		return result;
	}
	
}