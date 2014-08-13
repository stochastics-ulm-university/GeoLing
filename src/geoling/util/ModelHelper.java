package geoling.util;

import geoling.locations.util.AggregatedLocation;
import geoling.models.Location;
import geoling.models.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;


/**
 * Convenience methods for working with model classes.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class ModelHelper {

	/**
	 * Takes a collection of model objects and inserts them into a <code>HashMap</code>,
	 * where their ID is used as the key.
	 *  
	 * @param modelObjects  the collection of model objects
	 * @return the <code>HashMap</code> object
	 * @throws IllegalArgumentException if an object has no ID, i.e., if it is not saved to the database
	 */
	public static <E extends Model> HashMap<Object,E> toHashMap(Collection<E> modelObjects) {
		HashMap<Object,E> map = new HashMap<Object,E>();
		for (E obj : modelObjects) {
			Object id = obj.getId();
			if (id == null) {
				throw new IllegalArgumentException("ModelHelper.toHashMap: all model objects are required to have IDs!"); 
			}
			map.put(id, obj);
		}
		return map;
	}
	
	/**
	 * Takes a collection of <code>AggregatedLocation</code> objects which may consist of
	 * several locations each and maps every location ID to the <code>AggregatedLocation</code>
	 * object.
	 * 
	 * @param aggregatedLocations  the collection of aggregated locations
	 * @return the <code>HashMap</code> object
	 * @throws IllegalArgumentException if a location has no ID, i.e., if it is not saved to the database
	 */
	public static HashMap<Object,AggregatedLocation> toAggregatedLocationsHashMap(Collection<AggregatedLocation> aggregatedLocations) {
		HashMap<Object,AggregatedLocation> map = new HashMap<Object,AggregatedLocation>();
		for (AggregatedLocation aggregatedLocation : aggregatedLocations) {
			for (Location location : aggregatedLocation.getLocations()) {
				Object id = location.getId();
				if (id == null) {
					throw new IllegalArgumentException("ModelHelper.toAggregatedLocationsHashMap: all Location objects are required to have IDs!"); 
				}
				map.put(id, aggregatedLocation);
			}
		}
		return map;
	}
	
	/**
	 * Finds all tags for the given class with the given attributes.
	 * 
	 * @param clazz    the required class
	 * @param tagName  the name of the tag
	 * @param tagValue the value of the tag
	 * @return the list of tag objects
	 */
	public static <T> LazyList<Tag> findTags(Class<T> clazz, String tagName, String tagValue) {
		String parentType = clazz.getCanonicalName().substring(clazz.getCanonicalName().lastIndexOf(".")+1);
		return Tag.find("parent_type = ? AND name = ? AND value = ?", parentType, tagName, tagValue);
	}
	
	/**
	 * Returns an object that has the specified tag.
	 * Note that this method just returns one of several matching objects
	 * if present.
	 * 
	 * @param clazz    the required class
	 * @param tagName  the name of the tag
	 * @param tagValue the value of the tag
	 * @return an object with the specified tag or <code>null</code> if it doesn't exist
	 */
	public static <T extends Model> T findByTag(Class<T> clazz, String tagName, String tagValue) {
		for (Tag tag : findTags(clazz, tagName, tagValue)) {
			return (T)tag.parent(clazz);
		}
		return null;
	}
	
	/**
	 * Returns a list of objects that have the specified tag.
	 * 
	 * @param clazz    the required class
	 * @param tagName  the name of the tag
	 * @param tagValue the value of the tag
	 * @return the list of objects
	 */
	public static <T extends Model> ArrayList<T> findAllByTag(Class<T> clazz, String tagName, String tagValue) {
		ArrayList<T> result = new ArrayList<T>();
		for (Tag tag : findTags(clazz, tagName, tagValue)) {
			result.add((T)(tag.parent(clazz)));
		}
		return result;
	}
	
	/**
	 * Returns an object that has the specified tags.
	 * Note that this method just returns one of several matching objects
	 * if present.
	 * 
	 * @param clazz     the required class
	 * @param tagName1  the name of the first tag
	 * @param tagValue1 the value of the first tag
	 * @param tagName2  the name of the second tag
	 * @param tagValue2 the value of the second tag
	 * @return an object with the specified tag or <code>null</code> if it doesn't exist
	 */
	public static <T extends Model> T findByTag(Class<T> clazz, String tagName1, String tagValue1, String tagName2, String tagValue2) {
		String parentType = clazz.getCanonicalName().substring(clazz.getCanonicalName().lastIndexOf(".")+1);
		Tag tag = Tag.findFirst("parent_type = ? AND name = ? AND value = ? AND "+
		                        "parent_id IN (SELECT parent_id FROM tags AS t WHERE t.parent_type = ? AND t.name = ? AND t.value = ?)",
		                        parentType, tagName1, tagValue1, parentType, tagName2, tagValue2);
		if (tag != null) {
			return (T)tag.parent(clazz);
		} else {
			return null;
		}
	}
	
	/**
	 * Returns a list of objects that have the specified tags.
	 * 
	 * @param clazz     the required class
	 * @param tagName1  the name of the first tag
	 * @param tagValue1 the value of the first tag
	 * @param tagName2  the name of the second tag
	 * @param tagValue2 the value of the second tag
	 * @return the list of objects
	 */
	public static <T extends Model> ArrayList<T> findAllByTags(Class<T> clazz, String tagName1, String tagValue1, String tagName2, String tagValue2) {
		String parentType = clazz.getCanonicalName().substring(clazz.getCanonicalName().lastIndexOf(".")+1);
		LazyList<Tag> tags = Tag.find("parent_type = ? AND name = ? AND value = ? AND "+
		                              "parent_id IN (SELECT parent_id FROM tags AS t WHERE t.parent_type = ? AND t.name = ? AND t.value = ?)",
		                              parentType, tagName1, tagValue1, parentType, tagName2, tagValue2);
		ArrayList<T> result = new ArrayList<T>();
		for (Tag tag : tags) {
			result.add((T)tag.parent(clazz));
		}
		return result;
	}
	
}
