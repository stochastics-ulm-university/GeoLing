package geoling.gui.util;

import geoling.util.Utilities;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * This class pigments cells of a <code>JTable</code> in the color of the passed
 * <code>JLabel</code>, the hue is used as a label, if present.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class ColorHueTableCellRenderer implements TableCellRenderer {

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		JLabel label = new JLabel();
		if (value instanceof Color) {
			Color color = (Color)value;
			float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
			if (Utilities.isEqual(hsb[1], 0.0)) {
				label.setText(""); // white/black/gray
			} else {
				label.setText("hue = " + (int)Math.round(hsb[0]*360) + "°");
			}
			label.setOpaque(true);
			label.setBackground(color);
		}
		return label;
	}

}