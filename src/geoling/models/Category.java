package geoling.models;

import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.annotations.BelongsTo;

/**
 * A single category, the model supports a (sorted) hierarchical category structure.
 * Note that maps may belong to one or more categories, see the join table
 * model <code>CategoriesMaps</code>.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @see <a href="http://www.sitepoint.com/hierarchical-data-database-2/">Storage of the tree in the database</a>
 */
@BelongsTo(parent = Category.class, foreignKeyName = "parent_id")
public class Category extends ExtendedModel implements Comparable<Category> {
	
	static {
		validatePresenceOf("name", "lft", "rgt");
	}
	
	/**
	 * Returns a list of the root categories, in their correct order.
	 * 
	 * @return a list of the root categories
	 */
	public static LazyList<Category> findRootCategories() {
		return Category.where("parent_id IS NULL").orderBy("lft");
	}
	
	/**
	 * Returns a list of all direct child categories of the given category,
	 * in their correct order.
	 * 
	 * @param category  the parent category
	 * @return a list of all direct child categories
	 */
	public static LazyList<Category> findDirectChildren(Category category) {
		return Category.where("parent_id = ?", category.getId()).orderBy("lft");
	}
	
	/**
	 * Rebuilds the "lft" and "rgt" attributes in the categories table, which
	 * are used to avoid (slow) recursions in the categories tree.
	 */
	public static void rebuildLftRgtAttributes() {
		Category.rebuildLftRgtAttributes(null, 0);
	}
	
	/**
	 * Rebuilds the "lft" and "rgt" attributes in the categories table beginning
	 * from a given category.
	 * The <code>category</code> parameter may be <code>null</code>, then
	 * all top-level categories are processed. In that case, the <code>lft</code>
	 * parameter should equal zero.
	 * 
	 * @param category  the category object whose "lft" and "rgt" attributes should
	 *                  be updated, calls this method also for all children
	 * @param lft       the left value that will be used
	 */
	private static int rebuildLftRgtAttributes(Category category, int lft) {
		int rgt = lft+1;
		
		LazyList<Category> subCategories;
		if (category == null) {
			subCategories = Category.findRootCategories();
		} else {
			subCategories = Category.findDirectChildren(category);
		}
		for (Category subCategory : subCategories) {
			rgt = rebuildLftRgtAttributes(subCategory, rgt);
		}
		
		if (category != null) {
			category.setInteger("lft", lft);
			category.setInteger("rgt", rgt);
			category.saveIt();
		}
		return rgt+1;
	}
	
	public int compareTo(Category other) {
		return this.getInteger("lft").compareTo(other.getInteger("lft"));
	}

}