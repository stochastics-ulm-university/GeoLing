package geoling.models;

import org.javalite.activejdbc.annotations.BelongsTo;

/**
 * Aggregation of variants for different levels.
 * Note that the field "to_variant_id" may be <code>null</code>, e.g.,
 * if an answer is invalid.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
@BelongsTo(parent = Variant.class, foreignKeyName = "to_variant_id")
public class VariantsMapping extends ExtendedModel {
	
	static {
		validatePresenceOf("variant_id", "level_id");
	}
	
}