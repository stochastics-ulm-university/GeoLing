package geoling.gui.util;

import geoling.models.Distance;

/**
 * This class contains a <code>Distance</code> object and 
 * offers a useful <code>toString()</code> method, which is used for <code>JComboBox</code> elements.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class ComboBoxDistanceElement {

	private Distance distance;

	public ComboBoxDistanceElement(Distance distance) {
		this.distance = distance;
	}

	public Distance getDistance() {
		return this.distance;
	}

	public void setDistance(Distance distance) {
		this.distance = distance;
	}

	public String toString() {
		if (this.distance == null) {
			// prototype display value
			return "Name of some distance";
		}
		
		String name = distance.getString("name");
		if (name.length() > 62) {
			return name.substring(0, 60) + "...";
		} else {
			return name;
		}
	}

}