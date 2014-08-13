package geoling.tools;

import geoling.config.Database;
import geoling.config.Settings;
import geoling.models.Category;

import java.io.IOException;

/**
 * Program for rebuilding the "lft" and "rgt" attributes in the categories
 * table, which are used to avoid (slow) recursions in the categories tree.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class RebuildCategoriesLftRgtAttributes {

	/**
	 * Rebuilds the "lft" and "rgt" attributes in the categories table.
	 * 
	 * @param args  command line parameters (not required)
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Settings.load();
		Database.connect(Settings.getDatabaseIdentifier());
		
		Category.rebuildLftRgtAttributes();
	}

}