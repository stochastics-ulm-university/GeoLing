package geoling.maps.weights;

import geoling.models.*;
import geoling.util.ModelHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.RowListenerAdapter;

/**
 * Object that computes the weights of variants at locations,
 * does not support levels.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class VariantWeightsNoLevel extends VariantWeights {
	
	/**
	 * Collects the answers for the variants of the given map and constructs
	 * an object to fetch the weights easily.
	 * 
	 * @param map  the map the weights are computed for
	 */
	public VariantWeightsNoLevel(Map map) {
		super(map, true);
		
		// cache variants and locations to avoid queries in the loop below
		final HashMap<Object,Variant> variantsMap = ModelHelper.toHashMap(map.getAll(Variant.class));
		final HashMap<Object,Location> locationsMap = ModelHelper.toHashMap(getLocations());
		
		// use a simple SQL query to get all answers without allocating many InterviewAnswer objects
		Base.find("SELECT ia.variant_id, i.location_id FROM interview_answers AS ia"+
		          " JOIN informants AS i ON i.id=ia.informant_id"+
		          " JOIN variants AS v ON v.id=ia.variant_id"+
		          " WHERE v.map_id=?", map.getId()).with(new RowListenerAdapter() {
			public void onNext(java.util.Map<String,Object> row) {
        		Variant variant = variantsMap.get(row.get("variant_id"));
        		Location location = locationsMap.get(row.get("location_id"));

				Integer oldWeightObj = variantCounter.get(location).get(variant);
				int oldWeight = 0;
				if (oldWeightObj != null) {
					oldWeight = oldWeightObj.intValue();
				}
				variantCounter.get(location).put(variant, new Integer(oldWeight+1));
				
				totalCounter.put(location, new Integer(totalCounter.get(location).intValue()+1));
        	}
		});
		
		// remove unused locations
		for (Iterator<Entry<Location, HashMap<Variant,Integer>>> it = variantCounter.entrySet().iterator(); it.hasNext() ; ) {
			if (it.next().getValue().isEmpty()) {
				it.remove();
			}
		}
		for (Iterator<Entry<Location, Integer>> it = totalCounter.entrySet().iterator(); it.hasNext() ; ) {
			if (it.next().getValue().intValue() == 0) {
				it.remove();
			}
		}
	}

	/**
	 * Returns an identification string for the weights computation.
	 * 
	 * @return the identification string
	 */
	public String getIdentificationString() {
		return "default";
	}

}
