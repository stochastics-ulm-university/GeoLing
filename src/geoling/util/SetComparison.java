package geoling.util;

import java.util.Collection;

/**
 * Helper methods for comparing sets.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class SetComparison {
	
	/**
	 * Checks whether both sets contain the elements of the other one.
	 * 
	 * @param set1  the first set
	 * @param set2  the second set
	 * @return <code>true</code> if both sets contain the elements of the other one
	 */
	public static boolean equalSets(Collection<? extends Object> set1, Collection<? extends Object> set2) {
		return set2.containsAll(set1) && set1.containsAll(set2);
	}
	
}
