package geoling.gui.util;

import geoling.gui.GeoLingGUI;

import java.io.File;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * A custom variant of <code>JFileChooser</code> that automatically asks the user
 * whether an existing file should be overwritten. 
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class JFileChooserConfirmOverwrite extends JFileChooser {
	private static final long serialVersionUID = 7829683022698515502L;
	
	private ResourceBundle rb;
	
	public JFileChooserConfirmOverwrite(String defaultFileName) {
		super();
		setDialogType(SAVE_DIALOG);
		if (defaultFileName != null) {
			setSelectedFile(new File(defaultFileName));
		}
		rb = ResourceBundle.getBundle("GeoLingGUI", GeoLingGUI.LANGUAGE);
	}
	
	@Override
	public void approveSelection() {
		if (getSelectedFile().exists()) {
			if (JOptionPane.showConfirmDialog(this, rb.getString("text_popupOverwriteFile"), rb.getString("title_popupOverwriteFile"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
				super.approveSelection();
			}
		} else {
			super.approveSelection();
		}
	}
	
}