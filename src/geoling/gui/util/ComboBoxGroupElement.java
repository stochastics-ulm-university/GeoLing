package geoling.gui.util;

import geoling.models.Group;

/**
 * This class contains a <code>Group</code> object and 
 * offers a useful <code>toString()</code> method, which is used for <code>JComboBox</code> elements.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class ComboBoxGroupElement {

	private Group group;

	public ComboBoxGroupElement(Group group) {
		this.group = group;
	}

	public Group getGroup() {
		return this.group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public String toString() {
		if (this.group == null) {
			// prototype display value
			return "Name of some group";
		}
		
		String name = group.getString("name");
		if (name.length() > 62) {
			return name.substring(0, 60) + "...";
		} else {
			return name;
		}
	}

}