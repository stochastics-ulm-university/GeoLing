package geoling.models;

/**
 * Group<->Map many-to-many association.
 * A group is used to define a list of maps that we want to consider.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class GroupsMaps extends ExtendedModel {
	
	static {
		validatePresenceOf("group_id", "map_id");
	}
	
}