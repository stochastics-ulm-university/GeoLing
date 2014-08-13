package geoling.models;

import geoling.maps.util.MapBorder;
import geoling.util.sim.grain.Polytope;
import geoling.util.vendor.HumaneStringComparator;

import org.javalite.activejdbc.LazyList;

/**
 * A border object defines the border of the map used for plots.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Border extends ExtendedModel implements Comparable<Border> {
	
	static {
		validatePresenceOf("name");
	}
	
	/**
	 * Fetches the default border from the database, uses the corresponding
	 * configuration option (or, if only one border is present, it returns that one).
	 * 
	 * @return the border object
	 */
	public static Border getDefaultBorder() {
		int borderId = ConfigurationOption.getOption("defaultBorderId", 0);
		Border border;
		if (borderId > 0) {
			border = Border.findById(borderId);
			if (border == null) {
				throw new RuntimeException("Default border not found. Please check the \"defaultBorderId\" configuration option!");
			}
		} else {
			LazyList<Border> list = Border.findAll();
			if (list.size() == 1) {
				border = list.get(0);
			} else {
				if (list.isEmpty()) {
					throw new RuntimeException("No border present!");
				} else {
					throw new RuntimeException("More than one border present, please set the \"defaultBorderId\" configuration option!");
				}
			}
		}
		return border;
	}
	
	/**
	 * Constructs a polygon object for this border.
	 * 
	 * @return a (not necessarily convex) polygon
	 */
	public Polytope toPolygon() {
		return MapBorder.loadFromDatabase(this);
	}
	
	public int compareTo(Border other) {
		return HumaneStringComparator.DEFAULT.compare(this.getString("name"), other.getString("name"));
	}
	
}