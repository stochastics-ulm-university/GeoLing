package geoling.maps.weights;

import geoling.models.*;
import geoling.util.ModelHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.RowListenerAdapter;

/**
 * Object that computes the weights of variants at locations, supports a level to
 * aggregate variants.
 * Note that chaining of several <code>VariantWeights</code> objects is explicitly allowed.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class VariantWeightsWithLevel extends VariantWeights {
	
	/** The level that was used to aggregate variants. */
	protected Level level;
	
	/** The identification string of the weights given to the constructor of this object. */
	protected String variantWeightsBaseIdentification;
		
	/**
	 * Collects the answers for the (aggregated) variants of the given map and constructs
	 * an object to fetch the weights easily.
	 * 
	 * @param map    the map the weights are computed for
	 * @param level  the level to use for variant aggregation
	 */
	public VariantWeightsWithLevel(Map map, Level level) {
		this(new VariantWeightsNoLevel(map), level);
	}
	
	/**
	 * Aggregates the answers given by an existing <code>VariantWeights</code> object and constructs
	 * a new object to fetch the weights easily.
	 * Note that chaining of several <code>VariantWeights</code> objects is explicitly allowed.
	 * 
	 * @param variantWeights the <code>VariantWeights</code> object whose variants will be aggregated
	 * @param level          the level to use for variant aggregation
	 */
	public VariantWeightsWithLevel(VariantWeights variantWeights, Level level) {
		super(variantWeights.getMap(), false);
		this.level = level;
		this.variantWeightsBaseIdentification = variantWeights.getIdentificationString();
		
		ArrayList<Location> locations = new ArrayList<Location>(variantWeights.getLocations());
		
		// cache locations to avoid queries in the loop below
		final HashMap<Object,Variant> variantsMap = ModelHelper.toHashMap(map.getAll(Variant.class));
		final HashMap<Object,LinkedList<Object>> allIdMappings = new HashMap<Object,LinkedList<Object>>(variantsMap.size()*4/3+1);
		
		// use a simple SQL query to get all mappings at once
		Base.find("SELECT vm.variant_id, vm.to_variant_id FROM variants_mappings AS vm"+
		          " JOIN variants AS v ON v.id=vm.variant_id"+
		          " WHERE v.map_id=? AND vm.level_id=?", map.getId(), level.getId()).with(new RowListenerAdapter() {
			public void onNext(java.util.Map<String,Object> row) {
				Object variantId = row.get("variant_id");
				Object toVariantId = row.get("to_variant_id");
				LinkedList<Object> list = null;
				if (allIdMappings.containsKey(variantId)) {
					list = allIdMappings.get(variantId);
				} else {
					list = new LinkedList<Object>();
					allIdMappings.put(variantId, list);
				}
				list.add(toVariantId);
			}
		});
		
		// iterate over all variants and check if there exist (one or more) mappings
		// to another variant for this level
		for (Variant variant : variantWeights.getVariants()) {
			LinkedList<Object> idMappings = null;
			boolean noMapping = false;
			if (allIdMappings.containsKey(variant.getId())) {
				idMappings = allIdMappings.get(variant.getId());
			} else {
				// if no mapping exists, then the existing variant is ok, create only
				// a dummy entry so that we enter the loop below
				idMappings = new LinkedList<Object>();
				idMappings.add(null);
				noMapping = true;
			}
			
			// now handle all mappings
			for (Object toVariantId : idMappings) {
				if (noMapping) {
					// there is no mapping, old variant is the new one
					toVariantId = variant.getId();
				}
				
				if (toVariantId == null) {
					// null for to_variant_id is allowed, e.g., if the answer is invalid and should be ignored
				} else {
					Variant toVariant;
					if (toVariantId.equals(variant.getId())) {
						toVariant = variant;
					} else if (variantsMap.containsKey(toVariantId)) {
						toVariant = variantsMap.get(toVariantId);
					} else {
						toVariant = (Variant)Variant.findById(toVariantId);
						variantsMap.put(toVariantId, toVariant);
					}
					
					for (Location location : locations) {
						Integer addWeightObj = variantWeights.variantCounter.get(location).get(variant);
						int addWeight = (addWeightObj != null) ? addWeightObj.intValue() : 0;
						if (addWeight == 0) {
							continue;
						}
						
						HashMap<Variant, Integer> variantCounterAtLoc = this.variantCounter.get(location);
						if (variantCounterAtLoc == null) {
							variantCounterAtLoc = new HashMap<Variant,Integer>();
							this.variantCounter.put(location, variantCounterAtLoc);
						}
						Integer oldWeightObj = variantCounterAtLoc.get(toVariant);
						int oldWeight = (oldWeightObj != null) ? oldWeightObj.intValue() : 0;
						variantCounterAtLoc.put(toVariant, new Integer(oldWeight+addWeight));
						
						Integer oldCountObj = this.totalCounter.get(location);
						int oldCount = (oldCountObj != null) ? oldCountObj.intValue() : 0;
						this.totalCounter.put(location, new Integer(oldCount+addWeight));
					}
				}
			}
		}
	}
	
	/**
	 * Returns an identification string for the weights computation.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		return this.variantWeightsBaseIdentification + ":level_id="+this.level.getId();
	}
	
	/**
	 * Returns the level used for variant aggregation.
	 * 
	 * @return the level
	 */
	public Level getLevel() {
		return this.level;
	}
	
}
