package geoling.maps.util;

import geoling.maps.projection.MapProjection;
import geoling.util.sim.grain.Polytope;

import java.util.LinkedList;

/**
 * Class for caching of grids for maps, because we want to avoid recomputing
 * them all the time.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class RectangularGridCache {
	
	/** Maximal number of grids that are held in the cache. */
	private static final int MAX_SIZE = 10;
	
	/** The cached grids. */
	private static LinkedList<RectangularGrid> cachedGrids = new LinkedList<RectangularGrid>();
	
	/**
	 * Returns a grid for the given border polygon with the specified
	 * map projection method (and default resolution).
	 * 
	 * @param border        the border polygon of the map
	 * @param mapProjection the map projection method to use
	 * @return the grid
	 */
	public static synchronized RectangularGrid getGrid(Polytope border, MapProjection mapProjection) {
		// we look for a grid that has:
		// - the same border polygon, currently only checked by area and circumference, which should be sufficient
		// - the same map projection type
		for (RectangularGrid grid : cachedGrids) {
			if (!MapBorder.bordersAreEqual(grid.getBorder(), border)) {
				continue;
			}
			
			if (!mapProjection.isSimilar(grid.getMapProjection())) {
				continue;
			}
			
			return grid;
		}
		
		if (cachedGrids.size() >= MAX_SIZE) {
			cachedGrids.removeLast();
		}
		
		RectangularGrid result = new RectangularGrid(border, mapProjection);
		cachedGrids.addFirst(result);
		return result;
	}
	
}