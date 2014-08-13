package geoling.gui.util;

import geoling.models.Distance;

/**
 * This class contains a <code>Distance</code> object and 
 * offers a useful <code>toString()</code> method, which is used for <code>JTable</code> elements.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class TableDistanceElement {

	private Distance distance;

	public TableDistanceElement(Distance distance) {
		this.distance = distance;
	}
	
	public Distance getDistance() {
		return distance;
	}
	
	public void setDistance(Distance distance) {
		this.distance = distance;
	}
	
	public String toString() {
		String name = distance.getString("name");
		if (name.length() > 40) {
			return name.substring(0, 40) + "...";
		} else {
			return name;
		}
	}

}