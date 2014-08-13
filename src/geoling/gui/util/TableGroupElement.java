package geoling.gui.util;

import geoling.models.Group;

/**
 * This class contains a <code>Distance</code> object and 
 * offers a useful <code>toString()</code> method, which is used for <code>JTable</code> elements.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class TableGroupElement {

	private Group group;

	public TableGroupElement(Group group) {
		this.group = group;
	}
	
	public Group getGroup() {
		return group;
	}
	
	public void setGroup(Group group) {
		this.group = group;
	}
	
	public String toString() {
		String name = group.getString("name");
		if (name.length() > 40) {
			return name.substring(0, 40) + "...";
		} else {
			return name;
		}
	}

}