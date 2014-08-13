package geoling.gui.util;

import geoling.models.Map;

/**
 * This class contains a <code>Map</code> object and 
 * offers a useful <code>toString()</code> method, which is used for <code>JComboBox</code> elements.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class ComboBoxMapElement {

	private Map map;

	public ComboBoxMapElement(Map group) {
		this.map = group;
	}

	public Map getMap() {
		return this.map;
	}

	public void setMap(Map group) {
		this.map = group;
	}

	public String toString() {
		if (this.map == null) {
			// prototype display value
			return "Name of some map";
		}
		
		String name = map.getString("name");
		if (name.length() > 62) {
			return name.substring(0, 60) + "...";
		} else {
			return name;
		}
	}

}