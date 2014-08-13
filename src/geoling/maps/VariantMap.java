package geoling.maps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.density.DensityEstimation;
import geoling.maps.distances.DistanceMeasure.LatLongNotSupportedException;
import geoling.maps.util.RectangularGrid;
import geoling.maps.weights.VariantWeights;
import geoling.models.Variant;
import geoling.util.LatLong;
import geoling.util.ThreadedTodoWorker;

/**
 * A variant-occurrence map, provides density values for every location.
 * Note: The <code>getDensity(LatLong)</code> method may raise an exception
 * if the distance measure doesn't support distances between arbitrary
 * geographical coordinates.
 * 
 * @author Aaron Spettl (partially based on previous work), Institute of Stochastics, Ulm University
 */
public class VariantMap {
	
	/** The variant this map is for. */
	protected Variant variant;
	
	/** The density estimation object that is used to build this map. */
	protected DensityEstimation densityEstimation;
	
	/** The area class map for the map of this variant. */
	protected AreaClassMap areaClassMap;
	
	/** The weight of all variants at all locations. */
	protected VariantWeights variantWeights;
	
	/** Density cache for all locations, <code>null</code> if not initialized. */
	protected HashMap<AggregatedLocation,Double> locationDensityCache;
	
	/** Rectangular grid for which the grid density cache has been computed. */
	protected RectangularGrid grid;
	
	/** Density cache for all grid points, <code>null</code> if not initialized. */
	protected HashMap<RectangularGrid.GridPoint,Double> gridDensityCache;
	
	/**
	 * Constructs an occurrence map for a single variant with an existing area-class-map.
	 * 
	 * @param areaClassMap       the existing area-class-map
	 * @param variant            the variant this map is for
	 * @param densityEstimation  the density estimation object to use
	 */
	public VariantMap(AreaClassMap areaClassMap, Variant variant, DensityEstimation densityEstimation) {
		this.areaClassMap      = areaClassMap;
		this.variantWeights    = (areaClassMap != null) ? areaClassMap.getVariantWeights() : null;
		this.variant           = variant;
		this.densityEstimation = densityEstimation;
		clearLocationDensityCache();
		clearGridDensityCache();
	}
	
	/**
	 * Returns the area-class-map for the map of this variant.
	 * 
	 * @return the area-class-map
	 */
	public AreaClassMap getAreaClassMap() {
		return areaClassMap;
	}
	
	/**
	 * Returns the variant of this variant-occurrence map.
	 * 
	 * @return the variant
	 */
	public Variant getVariant() {
		return variant;
	}
	
	/**
	 * Returns the density estimation object of this variant-occurrence map.
	 * 
	 * @return the density estimation object
	 */
	public DensityEstimation getDensityEstimation() {
		return densityEstimation;
	}
	
	/**
	 * Returns the locations of this variant-occurrence map.
	 * 
	 * @return the locations
	 */
	public List<AggregatedLocation> getLocations() {
		return areaClassMap.getLocations();
	}
	
	/**
	 * Checks for the density cache.
	 * 
	 * @return <code>true</code> if the density cache is initialized
	 */
	public synchronized boolean hasLocationDensityCache() {
		return (locationDensityCache != null);
	}
	
	/**
	 * Builds the density cache for all locations, throws an exception if already initialized.
	 * Note that this method uses multiple threads.
	 */
	public synchronized void buildLocationDensityCache() {
		if (locationDensityCache != null) {
			throw new RuntimeException("buildLocationDensityCache: density cache is already initialized.");
		}
		
		// result object is locationDensityCacheLocal, copy to this.locationDensityCache at the end of the method
		HashMap<AggregatedLocation,Double> locationDensityCacheLocal = new HashMap<AggregatedLocation,Double>(areaClassMap.getLocations().size()*4/3+1);
		// synchronized list of locations and result map
		final Map<AggregatedLocation,Double> locationDensityCacheSynchronized = Collections.synchronizedMap(locationDensityCacheLocal);
		
		// process all locations in threads
		ThreadedTodoWorker.workOnTodoList(areaClassMap.getLocations(), new ThreadedTodoWorker.SimpleTodoWorker<AggregatedLocation>() {
			public void processTodoItem(AggregatedLocation location) {
				double value = densityEstimation.estimate(variantWeights, variant, location);
				locationDensityCacheSynchronized.put(location, new Double(value));
			}
		});
		
		// now copy the final result object to this.locationDensityCache
		this.locationDensityCache = locationDensityCacheLocal;
	}
	
	/**
	 * Builds the density cache for all locations, never throws an exception.
	 * 
	 * @param force  if <code>true</code>, then we force the rebuild of the cache,
	 *               otherwise, the cache is only initialized if necessary
	 */
	public synchronized void buildLocationDensityCache(boolean force) {
		if (force && hasLocationDensityCache()) {
			clearLocationDensityCache();
		}
		if (!hasLocationDensityCache()) {
			buildLocationDensityCache();
		}
	}
	
	/**
	 * Clears the density cache for all locations.
	 */
	public synchronized void clearLocationDensityCache() {
		locationDensityCache = null;
	}
	
	/**
	 * Returns the grid used for the grid density cache.
	 * 
	 * @return the grid or <code>null</code> if not assigned
	 */
	public synchronized RectangularGrid getGrid() {
		return grid;
	}
	
	/**
	 * Checks for the grid density cache.
	 * 
	 * @param grid  the grid containing all the grid points
	 * @return <code>true</code> if the density cache is initialized
	 */
	public synchronized boolean hasGridDensityCache(RectangularGrid grid) {
		return (gridDensityCache != null) && (this.grid == grid);
	}
	
	/**
	 * Builds the grid density cache, throws an exception if already initialized.
	 * Note that this method uses multiple threads.
	 * 
	 * @param grid  the grid containing all the grid points
	 */
	public synchronized void buildGridDensityCache(RectangularGrid grid) {
		if (gridDensityCache != null) {
			throw new RuntimeException("buildGridDensityCache: density cache is already initialized.");
		}
		this.grid = grid;
		
		// result object is gridDensityCacheLocal, copy to this.gridDensityCache at the end of the method
		HashMap<RectangularGrid.GridPoint,Double> gridDensityCacheLocal = new HashMap<RectangularGrid.GridPoint,Double>(grid.getGridPoints().size()*4/3+1);
		// synchronized list of grid points and result map
		final Map<RectangularGrid.GridPoint,Double> gridDensityCacheSynchronized = Collections.synchronizedMap(gridDensityCacheLocal);
		
		// process all grid points in threads
		ThreadedTodoWorker.workOnTodoList(grid.getGridPoints(), new ThreadedTodoWorker.SimpleTodoWorker<RectangularGrid.GridPoint>() {
			public void processTodoItem(RectangularGrid.GridPoint gridPoint) {
				double value = densityEstimation.estimate(variantWeights, variant, gridPoint.getLatLong());
				gridDensityCacheSynchronized.put(gridPoint, new Double(value));
			}
		});
		
		// now copy the final result object to this.gridDensityCache
		this.gridDensityCache = gridDensityCacheLocal;
	}
	
	/**
	 * Builds the grid density cache, never throws an exception.
	 * 
	 * @param grid   the grid containing all the grid points
	 * @param force  if <code>true</code>, then we force the rebuild of the cache,
	 *               otherwise, the cache is only initialized if necessary
	 */
	public synchronized void buildGridDensityCache(RectangularGrid grid, boolean force) {
		if (force && hasGridDensityCache(grid)) {
			clearGridDensityCache();
		}
		if (!hasGridDensityCache(grid)) {
			clearGridDensityCache();
			buildGridDensityCache(grid);
		}
	}
	
	/**
	 * Clears the grid density cache.
	 */
	public synchronized void clearGridDensityCache() {
		grid = null;
		gridDensityCache = null;
	}
	
	/**
	 * Returns the minimal density evaluated at the locations.
	 * This method requires the density cache, which has to be initialized with <code>buildLocationDensityCache</code> previously.
	 * 
	 * @return the minimal density
	 */
	public double getMinDensity() {
		if (locationDensityCache == null) {
			throw new RuntimeException("getMinDensity: density cache is not initialized, use buildLocationDensityCache method.");
		}
		double result = Double.POSITIVE_INFINITY;
		for (Entry<AggregatedLocation,Double> entry : locationDensityCache.entrySet()) {
			result = Math.min(result, entry.getValue().doubleValue());
		}
		return result;
	}
	
	/**
	 * Returns the maximal density evaluated at the locations.
	 * This method requires the density cache, which has to be initialized with <code>buildLocationDensityCache</code> previously.
	 * 
	 * @return the maximal density
	 */
	public double getMaxDensity() {
		if (locationDensityCache == null) {
			throw new RuntimeException("getMaxDensity: density cache is not initialized, use buildLocationDensityCache method.");
		}
		double result = Double.NEGATIVE_INFINITY;
		for (Entry<AggregatedLocation,Double> entry : locationDensityCache.entrySet()) {
			result = Math.max(result, entry.getValue().doubleValue());
		}
		return result;
	}
	
	/**
	 * Estimates the density value of this variant at the given location.
	 * This method uses the density cache built by <code>buildLocationDensityCache</code>,
	 * if available.
	 * 
	 * @param location  the (aggregated) location
	 * @return the density value
	 */
	public double getDensity(AggregatedLocation location) {
		if (locationDensityCache == null) {
			return densityEstimation.estimate(variantWeights, variant, location);
		} else {
			Double val = locationDensityCache.get(location);
			if (val == null) {
				throw new IllegalArgumentException("Location density cache is initialized, but the given location is not known!");
			}
			return val.doubleValue();
		}
	}
	
	/**
	 * Estimates the density value of this variant at the given point of the grid.
	 * This method uses the density cache built by <code>buildGridDensityCache</code>,
	 * if available.
	 * 
	 * @param gridPoint  the point on the grid
	 * @return the density value
	 */
	public double getDensity(RectangularGrid.GridPoint gridPoint) {
		if (gridDensityCache == null) {
			return densityEstimation.estimate(variantWeights, variant, gridPoint.getLatLong());
		} else {
			Double val = gridDensityCache.get(gridPoint);
			if (val == null) {
				throw new IllegalArgumentException("Grid points density cache is initialized, but the given grid point is not known!");
			}
			return val.doubleValue();
		}
	}
	
	/**
	 * Estimates the density value of this variant at the given geographic coordinates.
	 * 
	 * @param latLong  the geographic coordinates
	 * @return the density value
	 * @throws LatLongNotSupportedException if the distance measure only supports the distance between locations
	 */
	public double getDensity(LatLong latLong) throws LatLongNotSupportedException {
		return densityEstimation.estimate(variantWeights, variant, latLong);
	}
	
}
