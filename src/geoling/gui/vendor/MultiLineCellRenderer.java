package geoling.gui.vendor;


import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.border.*;

import java.awt.Component;
import java.awt.Color;

import java.io.Serializable;

/**
* MultiLineCellRenderer.java<br><br>
*
* This class provides a Renderer for JMultilineTable.<br>
* (c) 2006 EduMIPS64 project - Rizzo Vanni G.<br>
*
* Special Thanks to Thomas Wernitz (thomas_wernitz@clear.net.nz)
* for his source code.<br>
*
* This file is part of the EduMIPS64 project, and is released under the GNU
* General Public License.<br>
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.<br>
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.<br>
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
*/
public class MultiLineCellRenderer extends JTextArea implements TableCellRenderer, Serializable {


	private static final long serialVersionUID = 1L;

	protected static Border noFocusBorder; 

	private Color unselectedForeground; 
	private Color unselectedBackground; 

	public MultiLineCellRenderer() {
		super();
		noFocusBorder = new EmptyBorder(1, 2, 1, 2);
		setLineWrap(true);
		setWrapStyleWord(true);
		setOpaque(true);
		setBorder(noFocusBorder);
	}

	public void setForeground(Color c) {
		super.setForeground(c); 
		unselectedForeground = c; 
	}

	public void setBackground(Color c) {
		super.setBackground(c); 
		unselectedBackground = c; 
	}

	public void updateUI() {
		super.updateUI(); 
		setForeground(null);
		setBackground(null);
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, 
			int row, int column) {

		if (isSelected) {
			super.setForeground(table.getSelectionForeground());
			super.setBackground(table.getSelectionBackground());
		}
		else {
			super.setForeground((unselectedForeground != null) ? unselectedForeground 
					: table.getForeground());
			super.setBackground((unselectedBackground != null) ? unselectedBackground 
					: table.getBackground());
		}

		setFont(table.getFont());

		if (hasFocus) {
			setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
			if (table.isCellEditable(row, column)) {
				super.setForeground( UIManager.getColor("Table.focusCellForeground") );
				super.setBackground( UIManager.getColor("Table.focusCellBackground") );
			}
		} else {
			setBorder(noFocusBorder);
		}

		setValue(value); 

		return this;
	}

	protected void setValue(Object value) {
		setText((value == null) ? "" : value.toString());
	}


	public static class UIResource extends MultiLineCellRenderer implements javax.swing.plaf.UIResource {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	}

}


