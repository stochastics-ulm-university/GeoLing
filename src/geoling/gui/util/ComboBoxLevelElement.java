package geoling.gui.util;

import geoling.models.Level;

/**
 * This class contains a <code>Level</code> object and 
 * offers a useful <code>toString()</code> method, which is used for <code>JComboBox</code> elements.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class ComboBoxLevelElement {

	private Level level;

	public ComboBoxLevelElement(Level level) {
		this.level = level;
	}

	public Level getLevel() {
		return this.level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	public String toString() {
		if (this.level == null) {
			// prototype display value
			return "No level";
		}
		
		String name = level.getString("name");
		if (name.length() > 62) {
			return name.substring(0, 60) + "...";
		} else {
			return name;
		}
	}
}