package geoling.models;

/**
 * Category<->Map many-to-many association.
 * Maps may belong to one or more categories (or theoretically, even none).
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class CategoriesMaps extends ExtendedModel {
	
	static {
		validatePresenceOf("category_id", "map_id");
	}
	
}