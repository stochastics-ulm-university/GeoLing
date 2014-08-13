/**
* MySwing: Advanced Swing Utilites
* Copyright (C) 2005  Santhosh Kumar T
* <p/>
.* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
* <p/>
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*/

package geoling.gui.vendor;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.*;

/**
* @author Santhosh Kumar T
* @email  santhosh@in.fiorano.com
*/
public class CheckTreeCellRenderer extends JPanel implements TreeCellRenderer{ 
	//	 /** This is a type-safe enumerated type */
	//	  public static class State { private State() { } }
	//	  public static final State NOT_SELECTED = new State();
	//	  public static final State SELECTED = new State();
	//	  public static final State DONT_CARE = new State();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private CheckTreeSelectionModel selectionModel; 
	private TreeCellRenderer delegate; 
	private TristateCheckBox checkBox = new TristateCheckBox(); 

	public CheckTreeCellRenderer(TreeCellRenderer delegate, CheckTreeSelectionModel selectionModel){ 
		this.delegate = delegate; 
		this.selectionModel = selectionModel; 
		setLayout(new BorderLayout()); 
		setOpaque(false); 
		checkBox.setOpaque(false); 
	} 


	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus){ 
		Component renderer = delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus); 

		TreePath path = tree.getPathForRow(row); 
		if(path!=null){ 
			if(selectionModel.isPathSelected(path, true)) 
				//                checkBox.setState(Boolean.TRUE); 
			//            	checkBox.setSelected(Boolean.TRUE);
				checkBox.setState(TristateCheckBox.SELECTED);
			else {
				//                checkBox.setState(selectionModel.isPartiallySelected(path) ? null : Boolean.FALSE); 
				//            	checkBox.setSelected(selectionModel.isPartiallySelected(path) ? Boolean.TRUE : Boolean.FALSE);
				checkBox.setState(selectionModel.isPartiallySelected(path) ? TristateCheckBox.DONT_CARE : TristateCheckBox.NOT_SELECTED); 
			}
		} 
		removeAll(); 
		add(checkBox, BorderLayout.WEST); 
		add(renderer, BorderLayout.CENTER); 
		return this; 
	} 
} 