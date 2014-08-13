package geoling.gui.util;

import geoling.gui.GeoLingGUI;
import geoling.util.Directory;

import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * A custom variant of <code>JFileChooser</code> for selecting directories that
 * automatically asks the user whether the contents of a non-empty directory
 * should be overwritten. 
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class JDirectoryChooserConfirmNonempty extends JFileChooser {
	private static final long serialVersionUID = -4480858073414229519L;
	
	private ResourceBundle rb;
	
	public JDirectoryChooserConfirmNonempty(String currentDirectoryPath) {
		super(currentDirectoryPath);
		setDialogType(SAVE_DIALOG);
		setFileSelectionMode(DIRECTORIES_ONLY);
		rb = ResourceBundle.getBundle("GeoLingGUI", GeoLingGUI.LANGUAGE);
	}
	
	@Override
	public void approveSelection() {
		try {
			if (!Directory.isEmpty(getSelectedFile().getAbsolutePath())) {
				if (JOptionPane.showConfirmDialog(this, rb.getString("text_popupNonemptyDirectory"), rb.getString("title_popupNonemptyDirectory"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
					super.approveSelection();
				}
			} else {
				super.approveSelection();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}