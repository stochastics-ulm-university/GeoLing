package geoling.models;

/**
 * A single answer (variant) given in an interview of an informant.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class InterviewAnswer extends ExtendedModel {
	
	static {
		validatePresenceOf("interviewer_id", "informant_id", "variant_id");
	}
	
}