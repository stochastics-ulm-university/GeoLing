package geoling.maps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.javalite.activejdbc.LazyList;

import geoling.locations.LocationAggregator;
import geoling.locations.LocationPassthrough;
import geoling.locations.SimilarCoordinatesAggregation;
import geoling.locations.util.AggregatedLocation;
import geoling.maps.density.DensityEstimation;
import geoling.maps.distances.DistanceMeasure.LatLongNotSupportedException;
import geoling.maps.projection.MapProjection;
import geoling.maps.util.RectangularGrid;
import geoling.maps.util.RectangularGridCache;
import geoling.maps.util.VoronoiMap;
import geoling.maps.util.VoronoiMapCache;
import geoling.maps.weights.VariantWeights;
import geoling.models.ConfigurationOption;
import geoling.models.Location;
import geoling.models.Map;
import geoling.models.Variant;
import geoling.util.LatLong;
import geoling.util.Utilities;
import geoling.util.XMLExport;
import geoling.util.geom.Geometry2D;
import geoling.util.sim.grain.LineSegment;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.util.RandomSet;

/**
 * An area-class-map, provides density values for every location, always
 * for the variant with the highest density.
 * Note: The <code>getVariantDensity(Variant,LatLong)</code> method may raise an
 * exception if the distance measure doesn't support distances between arbitrary
 * geographical coordinates.
 * 
 * @author Aaron Spettl (partially based on previous work), Institute of Stochastics, Ulm University
 */
public class AreaClassMap {
	
	/** Result object for <code>getDominantVariantAndDensity</code> methods. */
	public static class VariantDensityResult {
		public Variant variant = null;
		public double density = 0.0;
	}
	
	/** The density estimation object that is used to build this map. */
	protected DensityEstimation densityEstimation;
	
	/** The weight of all variants at all locations. */
	protected VariantWeights variantWeights;
	
	/** The location aggregator object that was used to generate the list of (aggregated) locations. */
	protected LocationAggregator locationAggregator;
	
	/** The list of all (aggregated) locations in this map. */
	protected ArrayList<AggregatedLocation> locations;
	
	/** The variant maps automatically constructed for all variants of the area-class-map. */
	protected HashMap<Variant,VariantMap> variantMaps;
	
	/** 
	 * The computed areas, for every variant the list of locations belonging to its area is saved.
	 * <code>null</code> until explicitly initialized with <code>buildAreas</code>.
	 * Note that variants with an empty location set are not allowed.
	 */
	protected HashMap<Variant,HashSet<AggregatedLocation>> areas;
	
	/**
	 * The Voronoi map of all locations.
	 * <code>null</code> until explicitly initialized with <code>buildAreas</code>.
	 */
	protected VoronoiMap voronoiMap;
	
	/** Rectangular grid for which the grid density cache has been computed. */
	protected RectangularGrid grid;
	
	/** Cached dominant variants including density for locations. */
	protected HashMap<AggregatedLocation,VariantDensityResult> dominantVariantLocationCache;
	
	/** Cached dominant variants including density for grid points. */
	protected HashMap<RectangularGrid.GridPoint,VariantDensityResult> dominantVariantGridCache;
	
	/**
	 * Constructs an area-class-map, i.e., at every coordinate the variant
	 * with the highest density is used.
	 * 
	 * @param variantWeights     the map, given by the weights for all variants at all locations
	 * @param locationAggregator specifies how the locations should be aggregated, i.e., represented
	 *                           as one Voronoi cell, may be <code>null</code> (then the default is
	 *                           used)
	 * @param densityEstimation  the density estimation object to use
	 */
	public AreaClassMap(VariantWeights variantWeights, LocationAggregator locationAggregator, DensityEstimation densityEstimation) {
		this.variantWeights = variantWeights;
		this.densityEstimation = densityEstimation;
		this.locationAggregator = locationAggregator;
		
		// fetch locations for which densities should be estimated:
		// normally, these are the locations having weights, but we also support
		// usage of all locations regardless of data present (configuration options table)
		ArrayList<Location> locationObjects;
		if (ConfigurationOption.getOption("useAllLocationsInDensityEstimation", false) || (variantWeights == null)) {
			LazyList<Location> tmp = Location.findAll();
			locationObjects = new ArrayList<Location>(tmp);
		} else {
			locationObjects = new ArrayList<Location>(variantWeights.getLocations());
		}
		
		// do not aggregate locations by default, but aggregate locations which have essentially the same coordinates
		// if this is configured
		if (this.locationAggregator == null) {
			if (ConfigurationOption.getOption("useLocationAggregation", false)) {
				this.locationAggregator = new SimilarCoordinatesAggregation();
			} else {
				this.locationAggregator = new LocationPassthrough();
			}
		}
		
		this.locations = new ArrayList<AggregatedLocation>(this.locationAggregator.getAggregatedLocations(locationObjects));
		
		// build variant maps necessary for this area-class-map
		this.variantMaps = new HashMap<Variant,VariantMap>();
		if (variantWeights != null) {
			for (Variant variant : variantWeights.getVariants()) {
				this.variantMaps.put(variant, new VariantMap(this, variant, densityEstimation));
			}
		}
		
		// areas are not yet computed
		this.areas = null;
		
		// grid not present by default
		this.grid = null;
		
		// dominant variant cache not initialized
		this.dominantVariantLocationCache = null;
		this.dominantVariantGridCache = null;
	}
	
	/**
	 * Constructs an area-class-map, i.e., at every coordinate the variant
	 * with the highest density is used.
	 * 
	 * @param variantWeights     the map, given by the weights for all variants at all locations
	 * @param densityEstimation  the density estimation object to use
	 */
	public AreaClassMap(VariantWeights variantWeights, DensityEstimation densityEstimation) {
		this(variantWeights, null, densityEstimation);
	}
	
	/**
	 * Returns the map of this area-class-map.
	 * 
	 * @return the map
	 */
	public Map getMap() {
		return variantWeights.getMap();
	}
	
	/**
	 * Returns the density estimation object of this area-class-map.
	 * 
	 * @return the density estimation object
	 */
	public DensityEstimation getDensityEstimation() {
		return densityEstimation;
	}
	
	/**
	 * Returns the location aggregator object that was used to generate the list of (aggregated) locations.
	 * 
	 * @return the location aggregator object
	 */
	public LocationAggregator getLocationAggregator() {
		return locationAggregator;
	}
	
	/**
	 * Returns the locations of this area-class-map.
	 * 
	 * @return the locations
	 */
	public List<AggregatedLocation> getLocations() {
		return Collections.unmodifiableList(locations);
	}
	
	/**
	 * Returns the variant maps.
	 * 
	 * @return the variant maps, the variant object itself is used as the key
	 */
	public HashMap<Variant,VariantMap> getVariantMaps() {
		return variantMaps;
	}
	
	/**
	 * Returns the variant weights object for this map.
	 * 
	 * @return the variant weights object
	 */
	public VariantWeights getVariantWeights() {
		return variantWeights;
	}
	
	/**
	 * Checks for the density cache in all variant maps.
	 * 
	 * @return <code>true</code> if all variant maps have an initialized density cache.
	 */
	public synchronized boolean hasLocationDensityCache() {
		if (this.dominantVariantLocationCache == null) {
			return false;
		}
		for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
			if (!entry.getValue().hasLocationDensityCache()) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Builds the density cache for all locations in all variant maps.
	 * If a variant map has an initialized density cache, then this cache remains
	 * (i.e., it is not rebuilt).
	 */
	public synchronized void buildLocationDensityCache() {
		for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
			entry.getValue().buildLocationDensityCache(false);
		}
		if (this.dominantVariantLocationCache == null) {
			HashMap<AggregatedLocation,VariantDensityResult> dominantVariants = new HashMap<AggregatedLocation,VariantDensityResult>(this.getLocations().size()*4/3);
			for (AggregatedLocation location : this.getLocations()) {
				dominantVariants.put(location, getDominantVariantAndDensity(location));
			}
			this.dominantVariantLocationCache = dominantVariants;
		}
	}

	/**
	 * Clears the density cache for all locations in all variant maps.
	 */
	public synchronized void clearLocationDensityCache() {
		for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
			entry.getValue().clearLocationDensityCache();
		}
		this.dominantVariantLocationCache = null;
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
	 * Returns the grid used for the grid density cache.
	 * 
	 * @param force  if <code>true</code>, then the grid will be generated
	 *               with border polygon and map projection method from the
	 *               Voronoi map, if necessary
	 * @return the grid or <code>null</code> if not assigned
	 */
	public synchronized RectangularGrid getGrid(boolean force) {
		if (force && (grid == null)) {
			if (this.voronoiMap == null) {
				throw new IllegalArgumentException("Grid can only be constructed automatically if Voronoi map is already present!");
			}
			grid = RectangularGridCache.getGrid(this.voronoiMap.getBorder(), this.voronoiMap.getMapProjection());
		}
		return grid;
	}
	
	/**
	 * Checks for the grid density cache in all variant maps.
	 * 
	 * @param grid  the grid containing all the grid points
	 * @return <code>true</code> if all variant maps have an initialized density cache.
	 */
	public synchronized boolean hasGridDensityCache(RectangularGrid grid) {
		if (this.dominantVariantGridCache == null) {
			return false;
		}
		for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
			if (!entry.getValue().hasGridDensityCache(grid)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Builds the grid density cache in all variant maps.
	 * If a variant map has an initialized density cache, then this cache remains
	 * (i.e., it is not rebuilt).
	 * 
	 * @param grid  the grid containing all the grid points
	 */
	public synchronized void buildGridDensityCache(RectangularGrid grid) {
		this.grid = grid;
		for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
			entry.getValue().buildGridDensityCache(grid, false);
		}
		if (this.dominantVariantGridCache == null) {
			HashMap<RectangularGrid.GridPoint,VariantDensityResult> dominantVariants = new HashMap<RectangularGrid.GridPoint,VariantDensityResult>(grid.getGridPoints().size()*4/3);
			for (RectangularGrid.GridPoint gridPoint : grid.getGridPoints()) {
				dominantVariants.put(gridPoint, getDominantVariantAndDensity(gridPoint));
			}
			this.dominantVariantGridCache = dominantVariants;
		}
	}
	
	/**
	 * Clears the grid density cache in all variant maps.
	 */
	public synchronized void clearGridDensityCache() {
		this.grid = null;
		for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
			entry.getValue().clearGridDensityCache();
		}
		this.dominantVariantGridCache = null;
	}
	
	/**
	 * Estimates the density values for the given variant at the given location.
	 * 
	 * @param variant   the variant
	 * @param location  the location
	 * @return the density value
	 */
	public double getVariantDensity(Variant variant, AggregatedLocation location) {
		VariantMap variantMap = this.variantMaps.get(variant);
		if (variantMap == null) {
			return 0.0;
		} else {
			return variantMap.getDensity(location);
		}
	}
	
	/**
	 * Estimates the density values for the given variant at the given grid point.
	 * 
	 * @param variant    the variant
	 * @param gridPoint  the grid point
	 * @return the density value
	 */
	public double getVariantDensity(Variant variant, RectangularGrid.GridPoint gridPoint) {
		VariantMap variantMap = this.variantMaps.get(variant);
		if (variantMap == null) {
			return 0.0;
		} else {
			return variantMap.getDensity(gridPoint);
		}
	}
	
	/**
	 * Estimates the density values for the given variant at the given geographic
	 * coordinates.
	 * 
	 * @param variant  the variant
	 * @param latLong  the geographic coordinates
	 * @return the density value
	 */
	public double getVariantDensity(Variant variant, LatLong latLong) {
		VariantMap variantMap = this.variantMaps.get(variant);
		if (variantMap == null) {
			return 0.0;
		} else {
			return variantMap.getDensity(latLong);
		}
	}
	
	/**
	 * Estimates the density values at the given location and returns the
	 * variant with the highest density value.
	 * 
	 * @param location  the location
	 * @return the dominant variant and its density value or <code>null</code>
	 *         if there is no variant with a positive density value
	 */
	public VariantDensityResult getDominantVariantAndDensity(AggregatedLocation location) {
		if (this.dominantVariantLocationCache == null) {
			VariantDensityResult result = new VariantDensityResult();
			
			int dominantCounter = 0;
			for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
				double density = entry.getValue().getDensity(location);
				if (Utilities.isEqual(density, result.density)) {
					dominantCounter++;
				} else if (density > result.density) {
					result.variant = entry.getKey();
					result.density = density;
					dominantCounter = 1;
				}
			}
			
			if (result.variant == null) {
				return null;
			} else {
				if (dominantCounter > 1) {
					System.err.println("Warning: No unique dominant variant at location with ID "+location.getId()+", just picking one.");
				}
				return result;
			}
		} else {
			VariantDensityResult result = this.dominantVariantLocationCache.get(location);
			if (result == null) {
				if (!this.dominantVariantLocationCache.containsKey(location)) {
					throw new IllegalArgumentException("Dominant variants cache is initialized, but the given location is not known!");
				}
			}
			return result;
		}
	}
	
	/**
	 * Estimates the density values at the given grid point and returns the
	 * variant with the highest density value.
	 * 
	 * @param gridPoint  the grid point
	 * @return the dominant variant and its density value or <code>null</code>
	 *         if there is no variant with a positive density value
	 */
	public VariantDensityResult getDominantVariantAndDensity(RectangularGrid.GridPoint gridPoint) {
		if (this.dominantVariantGridCache == null) {
			VariantDensityResult result = new VariantDensityResult();
			
			int dominantCounter = 0;
			for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
				double density = entry.getValue().getDensity(gridPoint);
				if (Utilities.isEqual(density, result.density)) {
					dominantCounter++;
				} else if (density > result.density) {
					result.variant = entry.getKey();
					result.density = density;
					dominantCounter = 1;
				}
			}
			
			if (result.variant == null) {
				return null;
			} else {
				if (dominantCounter > 1) {
					System.err.println("Warning: No unique dominant variant at grid point with geographical coordinates ("+gridPoint.getLatLong()+"), just picking one.");
				}
				return result;
			}
		} else {
			VariantDensityResult result = this.dominantVariantGridCache.get(gridPoint);
			if (result == null) {
				if (!this.dominantVariantGridCache.containsKey(gridPoint)) {
					throw new IllegalArgumentException("Dominant variants cache is initialized, but the given grid point is not known!");
				}
			}
			return result;
		}
	}
	
	/**
	 * Estimates the density values at the given geographic coordinates and returns the
	 * variant with the highest density value.
	 * 
	 * @param latLong  the geographic coordinates
	 * @return the dominant variant and its density value or <code>null</code>
	 *         if there is no variant with a positive density value
	 * @throws LatLongNotSupportedException if the distance measure only supports the distance between locations
	 */
	public VariantDensityResult getDominantVariantAndDensity(LatLong latLong) throws LatLongNotSupportedException {
		VariantDensityResult result = new VariantDensityResult();
		int dominantCounter = 0;
		for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
			double density = entry.getValue().getDensity(latLong);
			if (Utilities.isEqual(density, result.density)) {
				dominantCounter++;
			} else if (density > result.density) {
				result.variant = entry.getKey();
				result.density = density;
				dominantCounter = 1;
			}
		}
		if (result.variant == null) {
			return null;
		} else {
			if (dominantCounter > 1) {
				System.err.println("Warning: No unique dominant variant at geographical coordinates ("+latLong+"), just picking one.");
			}
			return result;
		}
	}
	
	/**
	 * Returns the minimal density in all variant maps evaluated at the locations.
	 * This method requires the density cache, which has to be initialized with <code>buildLocationDensityCache</code> previously.
	 * 
	 * @return the minimal density
	 */
	public double getMinDensity() {
		double result = Double.POSITIVE_INFINITY;
		for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
			result = Math.min(result, entry.getValue().getMinDensity());
		}
		return result;
	}
	
	/**
	 * Returns the maximal density in all variant maps evaluated at the locations.
	 * This method requires the density cache, which has to be initialized with
	 * <code>buildLocationDensityCache</code> previously.
	 * 
	 * @return the maximal density
	 */
	public double getMaxDensity() {
		double result = Double.NEGATIVE_INFINITY;
		for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
			result = Math.max(result, entry.getValue().getMaxDensity());
		}
		return result;
	}
	
	/**
	 * Initializes the areas of this area-class-map.
	 * Note that this method also initializes the density cache with
	 * <code>buildLocationDensityCache</code>, if necessary.
	 * 
	 * @param voronoiMap  the map with the Voronoi cells
	 */
	public synchronized void buildAreas(VoronoiMap voronoiMap) {
		this.voronoiMap = voronoiMap;
		
		if (!hasLocationDensityCache()) {
			buildLocationDensityCache();
		}
		
		HashMap<Variant,HashSet<AggregatedLocation>> areasLocal = new HashMap<Variant,HashSet<AggregatedLocation>>();
		
		for (AggregatedLocation location : voronoiMap.getLocationsWithCells()) {
			VariantDensityResult result = getDominantVariantAndDensity(location);
			if (result == null) {
				System.err.println("No dominant variant(s) for location with ID "+location.getId()+" found! This location is ignored, i.e., it does not belong to any area.");
				continue;
			}
			
			HashSet<AggregatedLocation> set = null;
			if (areasLocal.containsKey(result.variant)) {
				set = areasLocal.get(result.variant);
			} else {
				set = new HashSet<AggregatedLocation>();
				areasLocal.put(result.variant, set);
			}
			set.add(location);
		}
		
		this.areas = areasLocal;
	}
	
	/**
	 * Initializes the areas of this area-class-map.
	 * Note that this method also initializes the density cache with
	 * <code>buildLocationDensityCache</code>, if necessary.
	 * 
	 * @param border        the border polygon
	 * @param mapProjection the projection method for the coordinates
	 */
	public synchronized void buildAreas(Polytope border, MapProjection mapProjection) {
		this.buildAreas(VoronoiMapCache.getVoronoiMap(this.getLocations(), border, mapProjection));
	}
	
	/**
	 * Clears the areas of this area-class-map.
	 */
	public synchronized void clearAreas() {
		this.areas = null;
		this.voronoiMap = null;
	}
	
	/**
	 * Returns whether the areas of this area-class-map have been initialized.
	 * 
	 * @return <code>true</code> if the areas have been initialized with <code>buildAreas</code>
	 */
	public synchronized boolean hasAreas() {
		return (this.areas != null);
	}
	
	/**
	 * Returns the areas of this area-class-map.
	 * 
	 * @return a hash map with the variant as key and a list of locations as value
	 */
	public HashMap<Variant,HashSet<AggregatedLocation>> getAreas() {
		if (this.areas == null) {
			throw new RuntimeException("Areas not initialized, remember to use buildAreas!");
		}
		return this.areas;
	}
	
	/**
	 * Returns the number of areas of this area-class-map.
	 * 
	 * @return the number of areas
	 */
	public int getNumberOfAreas() {
		if (this.areas == null) {
			throw new RuntimeException("Areas not initialized, remember to use buildAreas!");
		}
		return this.areas.size();
	}
	
	/**
	 * Returns the Voronoi cells of the locations.
	 * 
	 * @return the Voronoi map
	 */
	public VoronoiMap getVoronoiMap() {
		if (this.voronoiMap == null) {
			throw new RuntimeException("Voronoi map not initialized, remember to use buildAreas!");
		}
		return this.voronoiMap;
	}
	
	/**
	 * Checks whether the given two areas have at least one common edge.
	 * 
	 * @param variant1  the first area, identified by the variant object
	 * @param variant2  the second area, identified by the variant object
	 * @return <code>true</code> if the two areas have at least one common edge
	 */
	public boolean areasAreNeighbours(Variant variant1, Variant variant2) {
		if (this.areas == null) {
			throw new RuntimeException("Areas not initialized, remember to use buildAreas!");
		}
		HashSet<AggregatedLocation> locations1 = this.areas.get(variant1);
		HashSet<AggregatedLocation> locations2 = this.areas.get(variant2);
		
		for (AggregatedLocation location1 : locations1) {
			for (AggregatedLocation location2 : locations2) {
				if (this.voronoiMap.cellsAreNeighbours(location1, location2)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Detects the border between two areas.
	 * 
	 * @param variant1  the first area, identified by the variant object
	 * @param variant2  the second area, identified by the variant object
	 * @return the border as a set of <code>Geometry2D.LineSegment</code> objects
	 */
	public RandomSet getBorderBetweenAreas(Variant variant1, Variant variant2) {
		if (this.areas == null) {
			throw new RuntimeException("Areas not initialized, remember to use buildAreas!");
		}
		
		HashSet<AggregatedLocation> locations1 = this.areas.get(variant1);
		HashSet<AggregatedLocation> locations2 = this.areas.get(variant2);
		
		RandomSet rs = new RandomSet(this.voronoiMap.getWindow());
		
		for (AggregatedLocation location1 : locations1) {
			for (AggregatedLocation location2 : locations2) {
				LineSegment ls = this.voronoiMap.getSeparatingEdge(location1, location2);
				if (ls != null) {
					rs.add(Utilities.transform(ls));
				}
			}
		}
		
		return rs;
	}
	
	/**
	 * Detects the border between all areas.
	 * 
	 * @return the border as a set of <code>Geometry2D.LineSegment</code> objects
	 */
	public RandomSet getBorderBetweenAllAreas() {
		if (this.areas == null) {
			throw new RuntimeException("Areas not initialized, remember to use buildAreas!");
		}
		
		List<AggregatedLocation> locationsWithCells = this.voronoiMap.getLocationsWithCells();
		
		HashSet<LineSegment> tmp = new HashSet<LineSegment>();
		
		// for all areas
		for (Entry<Variant,HashSet<AggregatedLocation>> entry : this.areas.entrySet()) {
			
			// check all pairs of locations ('location of this area', 'some other location')
			for (AggregatedLocation location1 : entry.getValue()) {
				for (AggregatedLocation location2 : locationsWithCells) {
					// if the locations are adjacent and the other location does not belong to this area, we have a border
					// (note: this way, we also find borders for adjacent cells that have no dominant variant)
					LineSegment ls = this.voronoiMap.getSeparatingEdge(location1, location2);
					if (ls != null && !entry.getValue().contains(location2)) {
						// note that "ls" does not depend on the order of (location1, location2),
						// therefore we can use a hash set to filter out duplicates
						tmp.add(ls);
					}
				}
			}
		}
		
		RandomSet rs = new RandomSet(this.voronoiMap.getWindow());
		for (LineSegment ls : tmp) {
			rs.add(Utilities.transform(ls));
		}
		return rs;
	}
	
	/**
	 * Computes the dominance of the variant with the highest density at the given location.
	 * 
	 * @return the dominance
	 * @see "Dissertation Jonas Rumpf, page 86"
	 */
	public double computeDominanceAtLocation(AggregatedLocation location) {
		double max_fvx = Double.NEGATIVE_INFINITY;
		double sum = 0.0;
		for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
			double fvx = entry.getValue().getDensity(location);
			if (fvx > max_fvx) {
				max_fvx = fvx;
			}
			sum += fvx;
		}
		double result = max_fvx / sum;
		if (Double.isNaN(result)) {
			return 0.0;
		} else {
			return result;
		}
	}
	
	/**
	 * Computes the dominance of the variant with the highest density at the given grid point.
	 * 
	 * @return the dominance
	 * @see "Dissertation Jonas Rumpf, page 86"
	 */
	public double computeDominanceAtGridPoint(RectangularGrid.GridPoint gridPoint) {
		double max_fvx = Double.NEGATIVE_INFINITY;
		double sum = 0.0;
		for (Entry<Variant,VariantMap> entry : this.variantMaps.entrySet()) {
			double fvx = entry.getValue().getDensity(gridPoint);
			if (fvx > max_fvx) {
				max_fvx = fvx;
			}
			sum += fvx;
		}
		double result = max_fvx / sum;
		if (Double.isNaN(result)) {
			return 0.0;
		} else {
			return result;
		}
	}
	
	/**
	 * Computes the length of all borders between the areas in kilometres.
	 * 
	 * @return the length of all borders between the areas in kilometres
	 * @see "Dissertation Jonas Rumpf, page 92"
	 */
	public double computeTotalBorderLength() {
		if (this.areas == null) {
			throw new RuntimeException("Areas not initialized, remember to use buildAreas!");
		}
		
		double totalBorderLength = 0.0;
		
		for (Iterator<?> it = getBorderBetweenAllAreas().iterator(); it.hasNext(); ) {
			Geometry2D.LineSegment ls = (Geometry2D.LineSegment)it.next();
			
			LatLong here = new LatLong(ls.p1.x, ls.p1.y);
			LatLong there = new LatLong(ls.p2.x, ls.p2.y);
			
	    	totalBorderLength += here.calculateDistanceTo(there);
		}
		
		return totalBorderLength;
	}
	
	/**
	 * Computes the "area compactness" for a single area of the map.
	 * 
	 * @param variant  the area, identified by the variant object
	 * @return the "area compactness" of the variant on this map
	 * @see "Dissertation Jonas Rumpf, page 92"
	 */
	public double computeAreaCompactness(Variant variant) {
		if (this.areas == null) {
			throw new RuntimeException("Areas not initialized, remember to use buildAreas!");
		}
		if (!this.areas.containsKey(variant)) {
			throw new RuntimeException("This variant is not dominant, therefore no area!");
		}
		
		double lv = 0.0;
		int n = 0;
		for (AggregatedLocation aggregatedLocation : this.areas.get(variant)) {
			double llv = 0.0;
			int m = 0;
			
			for (Location location : aggregatedLocation.getLocations()) {
				if (this.variantWeights.getTotalNumberOfVariantOccurencesAtLocation(location) > 0) {
					llv += this.variantWeights.getWeight(variant, location);
					m++;
				}
			}
			
			if (m > 0) {
				llv /= m;
				lv += llv;
				n++;
			}
		}
		lv /= n;
		return lv;
	}
	
	/**
	 * Computes the overall "area compactness" of the map.
	 * 
	 * @return the "area compactness" of the map
	 * @see "Dissertation Jonas Rumpf, page 92"
	 */
	public double computeOverallAreaCompactness() {
		if (this.areas == null) {
			throw new RuntimeException("Areas not initialized, remember to use buildAreas!");
		}
		
		double l = 0.0;
		int n = 0;
		for (Entry<Variant,HashSet<AggregatedLocation>> entry : this.areas.entrySet()) {
			for (AggregatedLocation aggregatedLocation : entry.getValue()) {
				double llv = 0.0;
				int m = 0;
				
				for (Location location : aggregatedLocation.getLocations()) {
					if (this.variantWeights.getTotalNumberOfVariantOccurencesAtLocation(location) > 0) {
						llv += this.variantWeights.getWeight(entry.getKey(), location);
						m++;
					}
				}
				
				if (m > 0) {
					llv /= m;
					l += llv;
					n++;
				}
			}
		}
		l /= n;
		return l;
	}
	
	/**
	 * Computes the homogeneity of an area.
	 * 
	 * @param variant  the area, identified by the variant object
	 * @return the homogeneity of the area
	 * @see "Dissertation Jonas Rumpf, page 93"
	 */
	public double computeAreaHomogeneity(Variant variant) {
		if (this.areas == null) {
			throw new RuntimeException("Areas not initialized, remember to use buildAreas!");
		}
		if (!this.areas.containsKey(variant)) {
			throw new RuntimeException("This variant is not dominant, therefore no area!");
		}
		
		double bv = 0.0;
		for (AggregatedLocation location : this.areas.get(variant)) {
			bv += computeDominanceAtLocation(location);
		}
		bv /= this.areas.get(variant).size();
		return bv;
	}
	
	/**
	 * Computes the overall homogeneity of the map.
	 * 
	 * @return the homogeneity of the map
	 * @see "Dissertation Jonas Rumpf, page 93"
	 */
	public double computeOverallHomogeneity() {
		if (this.areas == null) {
			throw new RuntimeException("Areas not initialized, remember to use buildAreas!");
		}
		
		double b = 0.0;
		for (Entry<Variant,HashSet<AggregatedLocation>> entry : this.areas.entrySet()) {
			double lb = 0.0;
			for (AggregatedLocation location : entry.getValue()) {
				lb += computeDominanceAtLocation(location);
			}
			lb /= entry.getValue().size();
			b += lb;
		}
		
		b /= this.areas.size();
		return b;
	}
	
	/**
	 * Computes the mean-value of the prevalence map, which consists
	 * only of the densities of the dominant variant for each location.
	 * 
	 * @param onGrid  determines whether the mean prevalence should be
	 *                computed on all grid points instead of all locations
	 * @return the mean prevalence
	 */
	public double computeMeanPrevalence(boolean onGrid) {
		double sum = 0.0;
		int counter = 0;
		
		if (onGrid) {
			for (RectangularGrid.GridPoint gridPoint : this.getGrid().getGridPoints()) {
				VariantDensityResult dominantVariantAndDensity = getDominantVariantAndDensity(gridPoint);
				if (dominantVariantAndDensity == null) {
					// no dominant variant means all variants have density values equal to zero
				} else {
					sum += dominantVariantAndDensity.density;
				}
				counter++;
			}
		} else {
			for (AggregatedLocation location : this.getVoronoiMap().getLocationsWithCells()) {
				VariantDensityResult dominantVariantAndDensity = getDominantVariantAndDensity(location);
				if (dominantVariantAndDensity == null) {
					// no dominant variant means all variants have density values equal to zero
				} else {
					sum += dominantVariantAndDensity.density;
				}
				counter++;
			}
		}
		
		return sum / counter;
	}
	
	/**
	 * Exports this area class map to a XML file.
	 * 
	 * @param fileName   the file name for the new XML file
	 * @param exportGrid determines whether the grid should be exported
	 * @throws IOException if an I/O error occurs
	 */
	public void toXML(String fileName, boolean exportGrid) throws IOException {
		try {
			XMLExport writer = new XMLExport(fileName);
			
			if (exportGrid) {
				writer.XML.writeStartElement("prevalencemap");
			} else {
				writer.XML.writeStartElement("areaclassmap");
			}
			if (this.getMap() != null) {
				writer.XML.writeAttribute("map_id", this.getMap().getId().toString());
				writer.XML.writeAttribute("map_name", this.getMap().getString("name"));
				writer.XML.writeAttribute("variant_weights", this.getVariantWeights().getIdentificationString());
				writer.XML.writeAttribute("densities", this.getDensityEstimation().getIdentificationString());
			}
			writer.newLine();
			
			writer.XML.writeStartElement("characteristics");
			writer.newLine();
			writer.XML.writeStartElement("characteristic");
			writer.XML.writeAttribute("name", "mean_prevalence");
			writer.XML.writeAttribute("value", String.format(Locale.ENGLISH, "%f", this.computeMeanPrevalence(false)));
			writer.XML.writeEndElement();
			writer.newLine();
			writer.XML.writeStartElement("characteristic");
			writer.XML.writeAttribute("name", "overall_area_compactness");
			writer.XML.writeAttribute("value", String.format(Locale.ENGLISH, "%f", this.computeOverallAreaCompactness()));
			writer.XML.writeEndElement();
			writer.newLine();
			writer.XML.writeStartElement("characteristic");
			writer.XML.writeAttribute("name", "overall_homogeneity");
			writer.XML.writeAttribute("value", String.format(Locale.ENGLISH, "%f", this.computeOverallHomogeneity()));
			writer.XML.writeEndElement();
			writer.newLine();
			writer.XML.writeStartElement("characteristic");
			writer.XML.writeAttribute("name", "total_border_length");
			writer.XML.writeAttribute("value", String.format(Locale.ENGLISH, "%f", this.computeTotalBorderLength()));
			writer.XML.writeEndElement();
			writer.newLine();
			writer.XML.writeEndElement(); // </characteristics>
			writer.newLine();
			
			writer.XML.writeStartElement("data");
			writer.newLine();
			
			if (exportGrid) {
				writer.XML.writeStartElement("densities");
				writer.newLine();
				for (Entry<Variant,VariantMap> entry : this.getVariantMaps().entrySet()) {
					writer.XML.writeStartElement("variant");
					writer.XML.writeAttribute("id", entry.getKey().getId().toString());
					writer.XML.writeAttribute("name", entry.getKey().getString("name"));
					writer.newLine();
					for (RectangularGrid.GridPoint gridPoint : this.getGrid().getGridPoints()) {
						writer.XML.writeStartElement("density");
						writer.XML.writeAttribute("latitude", String.format(Locale.ENGLISH, "%f", gridPoint.getLatLong().getLatitude()));
						writer.XML.writeAttribute("longitude", String.format(Locale.ENGLISH, "%f", gridPoint.getLatLong().getLongitude()));
						writer.XML.writeAttribute("value", String.format(Locale.ENGLISH, "%f", entry.getValue().getDensity(gridPoint)));
						writer.XML.writeEndElement();
						writer.newLine();
					}
					writer.XML.writeEndElement();
					writer.newLine();
				}
				writer.XML.writeEndElement(); // </densities>
				writer.newLine();
			} else {
				writer.XML.writeStartElement("locations");
				writer.newLine();
				for (AggregatedLocation location : this.getLocations()) {
					writer.XML.writeStartElement("location");
					writer.XML.writeAttribute("id", ""+location.getId());
					writer.XML.writeAttribute("name", location.getName());
					writer.XML.writeAttribute("latitude", String.format(Locale.ENGLISH, "%f", location.getLatLong().getLatitude()));
					writer.XML.writeAttribute("longitude", String.format(Locale.ENGLISH, "%f", location.getLatLong().getLongitude()));
					writer.XML.writeEndElement();
					writer.newLine();
				}
				writer.XML.writeEndElement(); // </locations>
				writer.newLine();
				
				writer.XML.writeStartElement("densities");
				writer.newLine();
				for (Entry<Variant,VariantMap> entry : this.getVariantMaps().entrySet()) {
					writer.XML.writeStartElement("variant");
					if (entry.getKey().getId() != null) {
						writer.XML.writeAttribute("id", entry.getKey().getId().toString());
					}
					writer.XML.writeAttribute("name", entry.getKey().getString("name"));
					writer.newLine();
					for (AggregatedLocation location : this.getLocations()) {
						writer.XML.writeStartElement("density");
						writer.XML.writeAttribute("location_id", ""+location.getId());
						writer.XML.writeAttribute("value", String.format(Locale.ENGLISH, "%f", entry.getValue().getDensity(location)));
						writer.XML.writeEndElement();
						writer.newLine();
					}
					writer.XML.writeEndElement();
					writer.newLine();
				}
				writer.XML.writeEndElement(); // </densities>
				writer.newLine();
			}
			
			writer.XML.writeEndElement(); // </data>
			writer.newLine();
			
			writer.XML.writeEndElement(); // </prevalencemap> / </areaclassmap>
			
			writer.close();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}
	
}