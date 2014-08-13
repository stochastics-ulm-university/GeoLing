package geoling.models;

import org.javalite.activejdbc.annotations.BelongsToPolymorphic;

/**
 * A tag for an arbitrary record of the other tables, used to store
 * additional information that should not be lost.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
@BelongsToPolymorphic(parents    = { Category.class, Informant.class, InterviewAnswer.class, Interviewer.class,
                                     Level.class, Location.class, Map.class, Variant.class }, 
                      typeLabels = { "Category", "Informant", "InterviewAnswer", "Interviewer",
                                     "Level", "Location", "Map", "Variant" } )
public class Tag extends ExtendedModel {
	
	static {
		validatePresenceOf("parent_type", "parent_id", "name");
	}

}