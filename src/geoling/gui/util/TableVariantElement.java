package geoling.gui.util;

import geoling.models.Variant;

/**
 * This class contains a <code>Variant</code> object and 
 * offers a useful <code>toString()</code> method, which is used for <code>JTable</code> elements.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class TableVariantElement {

	private Variant variant;
	
	private int stringLength = 40;

	public TableVariantElement(Variant variant) {
		this.variant = variant;
	}
	
	public TableVariantElement(Variant variant, int stringLength) {
		this.variant = variant;
		this.stringLength = stringLength;
	}

	public Variant getVariant() {
		return variant;
	}

	public void setVariant(Variant variant) {
		this.variant = variant;
	}

	public String toString() {
		String name = variant.getString("name");
		if (name.length() > stringLength) {
			return name.substring(0, stringLength) + "...";
		} else {
			return name;
		}
	}
}