package geoling.maps.weights;

import geoling.maps.util.LocationGrid;
import geoling.models.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.javalite.activejdbc.LazyList;

/**
 * Abstract class for objects that compute the weights of variants at locations.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public abstract class VariantWeights {
	
	/** The map the weights are computed for. */
	protected Map map;
	
	/** 
	 * Counter: the answers given at the location for each variant.
	 * Note that the number of answers per variant must be greater than zero, i.e.,
	 * don't create entries in the hash map where the value zero is assigned
	 * to a variant.
	 */
	protected HashMap<Location,HashMap<Variant,Integer>> variantCounter;
	
	/** Counter: the answers given at the location. */
	protected HashMap<Location,Integer> totalCounter;
	
	/** Cached <code>LocationGrid</code> object, only initialized when required. */
	protected LocationGrid locationGrid;
	
	/**
	 * Constructor that initializes the internal data structure.
	 * 
	 * @param map               the map the weights should be computed for
	 * @param initWithLocations determines whether the internal maps should
	 *                          be initialized with the locations
	 */
	public VariantWeights(Map map, boolean initWithLocations) {
		this.map          = map;
		this.locationGrid = null;
		
		variantCounter = new HashMap<Location,HashMap<Variant,Integer>>();
		totalCounter   = new HashMap<Location,Integer>();
		
		if (initWithLocations) {
			LazyList<Location> locations = Location.findAll();
			Integer zero = new Integer(0);
			for (Location location : locations) {
				variantCounter.put(location, new HashMap<Variant,Integer>());
				totalCounter.put(location, zero);
			}
		}
	}
	
	/**
	 * Constructor that initializes the internal data structure.
	 * 
	 * @param map  the map the weights should be computed for
	 */
	public VariantWeights(Map map) {
		this(map, true);
	}
	
	/**
	 * Returns an identification string for the weights computation.
	 * 
	 * @return the identification string
	 */
	public abstract String getIdentificationString();
	
	/**
	 * Returns the map belonging to this object.
	 * 
	 * @return the map
	 */
	public Map getMap() {
		return this.map;
	}
	
	/**
	 * Returns the set of all locations.
	 * 
	 * @return a (read-only) set of all locations
	 */
	public Set<Location> getLocations() {
		return Collections.unmodifiableSet(totalCounter.keySet());
	}
	
	/**
	 * Returns a <code>LocationGrid</code> object for the locations.
	 * 
	 * @return a <code>LocationGrid</code> object
	 */
	public synchronized LocationGrid getLocationGrid() {
		if (this.locationGrid == null) {
			this.locationGrid = new LocationGrid(totalCounter.keySet());
		}
		return this.locationGrid;
	}
	
	/**
	 * Makes sure that the given location exists in this object.
	 * This method is useful to synchronize the locations of several variant
	 * weights objects (and thus their area-class-maps, which causes them to
	 * have the same Voronoi cells).
	 * 
	 * @param location  the location which should be added, if it is not
	 *                  already contained
	 * @return <code>true</code> if the location was not present before
	 */
	public synchronized boolean enforceLocation(Location location) {
		if (!variantCounter.containsKey(location)) {
			variantCounter.put(location, new HashMap<Variant,Integer>());
			totalCounter.put(location, new Integer(0));
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Makes sure that the given locations exists in this object.
	 * This method is useful to synchronize the locations of several variant
	 * weights objects (and thus their area-class-maps, which causes them to
	 * have the same Voronoi cells).
	 * 
	 * @param locations  the list of locations which should be added, if they
	 *                   are not already contained
	 * @return <code>true</code> if at least one location was not present before
	 */
	public synchronized boolean enforceLocations(Collection<Location> locations) {
		boolean result = false;
		for (Location location : locations) {
			if (enforceLocation(location)) {
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Returns the list of all variants with positive weights.
	 * 
	 * @return the set of variants
	 */
	public HashSet<Variant> getVariants() {
		HashSet<Variant> result = new HashSet<Variant>();
		for (HashMap<Variant,Integer> variantCounterAtLoc : variantCounter.values()) {
			result.addAll(variantCounterAtLoc.keySet());
		}
		return result;
	}
	
	/**
	 * Returns the set of variants with positive weights at the given location.
	 * 
	 * @param location  the location
	 * @return the set of variants at the given location
	 */
	public HashSet<Variant> getVariantsAtLocation(Location location) {
		HashSet<Variant> result = new HashSet<Variant>();
		HashMap<Variant, Integer> variantCounterAtLoc = variantCounter.get(location);
		if (variantCounterAtLoc != null) {
			result.addAll(variantCounterAtLoc.keySet());
		}
		return result;
	}
	
	/**
	 * Returns the number of occurrences of a single variant at the given location.
	 * 
	 * @param location  the location
	 * @return the number of occurrences of a single variant at the given location
	 */
	public int getNumberOfVariantOccurencesAtLocation(Variant variant, Location location) {
		HashMap<Variant, Integer> variantCounterAtLoc = variantCounter.get(location);
		if (variantCounterAtLoc == null) {
			return 0;
		} else {
			Integer n = variantCounterAtLoc.get(variant);
			if (n == null) {
				return 0;
			} else {
				return n.intValue();
			}
		}
	}
	
	/**
	 * Returns the total number of answers available at the given location.
	 * 
	 * @param location  the location
	 * @return the number of answers at the given location
	 */
	public int getTotalNumberOfVariantOccurencesAtLocation(Location location) {
		Integer n = totalCounter.get(location);
		if (n == null) {
			return 0;
		} else {
			return n.intValue();
		}
	}
	
	/**
	 * Returns the computed weight for the given variant and location.
	 * Note that this method returns zero for all variants that don't appear
	 * at the given location, no exception is raised.
	 * 
	 * @param variant   the variant
	 * @param location  the location
	 * @return the weight
	 */
	public double getWeight(Variant variant, Location location) {
		int n = this.getNumberOfVariantOccurencesAtLocation(variant, location);
		if (n == 0) {
			return 0;
		} else {
			return (double)n / this.getTotalNumberOfVariantOccurencesAtLocation(location);
		}
	}
	
}
