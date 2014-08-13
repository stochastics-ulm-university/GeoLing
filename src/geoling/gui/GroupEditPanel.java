package geoling.gui;

import geoling.gui.util.JNameTree;
import geoling.gui.vendor.CheckTreeManager;
import geoling.models.Group;
import geoling.models.GroupsMaps;
import geoling.models.Map;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.tree.TreeModel;

import org.javalite.activejdbc.LazyList;

/**
 * Panel for editing a single group in dialectometry GUI.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class GroupEditPanel {
	
	/**
	 * <code>JPanel</code> for <code>JNameTree</code> with checkbox and a two
	 * <code>JButton</code>. <code>JNameTree</code> is embedded in a
	 * <code>JScrollPane</code>.
	 */
	private JPanel panelGroupEdit;
	/** <code>JScrollPane</code> for the <code>JNameTree</code> treeGroup */
	private JScrollPane scrollPaneCheckTree;
	/**
	 * Every node in this <code>JNameTree</code> has a checkbox. This tree shows
	 * all maps of <code>Group</code> selectedGroup. New maps can be added to
	 * the group or can be removed from the group. You can get selected
	 * <code>TreePath</code> by using <code>TreeSelectionModel</code> of
	 * <code>CheckTreeManager</code> checkTreeManager.
	 */
	private JNameTree treeGroup;
	/**
	 * This <code>CheckTreeManager</code> delivers a
	 * <code>TreeSelectionModel</code> for getting selected
	 * <code>TreePath</code>
	 */
	private CheckTreeManager checkTreeManager;
	/**
	 * This <code>JButton</code> saves changes in mapping between a group and
	 * maps.
	 */
	private JButton buttonSaveGroup;
	/**
	 * This <code>JButton</code> removes <code>JPanel</code> panel_GroupEdit
	 * from <code>JTabbedPane</code> tabbedPane.
	 */
	private JButton buttonCloseTab;
	
	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;
	
	/**
	 * Create a panel for adding maps to a group in a <code>CheckBoxTree</code>.
	 * 
	 * @param tabbedPane     The new panel is added to this <code>JTabbedPane</code>.
	 * @param selectedGroup  This group can be changed.
	 * @param treeModel      This model describes the belonging of maps to categories.
	 */
	public GroupEditPanel(final JTabbedPane tabbedPane, final Group selectedGroup, final TreeModel treeModel) {
		
		rb = ResourceBundle.getBundle("GroupEditPanel", GeoLingGUI.LANGUAGE);
		
		panelGroupEdit = new JPanel();
		tabbedPane.addTab(selectedGroup.getString("name"), null, panelGroupEdit, null);
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
		
		GridBagLayout gbl_panel_GroupEdit = new GridBagLayout();
		gbl_panel_GroupEdit.columnWidths = new int[] { 420, 80, 0 };
		gbl_panel_GroupEdit.rowHeights = new int[] { 30, 30, 440, 0 };
		gbl_panel_GroupEdit.columnWeights = new double[] { 0.8, 0.1, 0.1, Double.MIN_VALUE };
		gbl_panel_GroupEdit.rowWeights = new double[] { 0.1, 0.9, Double.MIN_VALUE };
		panelGroupEdit.setLayout(gbl_panel_GroupEdit);
		
		scrollPaneCheckTree = new JScrollPane();
		GridBagConstraints gbc_scrollPane_GroupEdit = new GridBagConstraints();
		gbc_scrollPane_GroupEdit.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_GroupEdit.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_GroupEdit.gridx = 0;
		gbc_scrollPane_GroupEdit.gridy = 0;
		gbc_scrollPane_GroupEdit.gridheight = 3;
		panelGroupEdit.add(scrollPaneCheckTree, gbc_scrollPane_GroupEdit);
		
		treeGroup = new JNameTree();
		// treeGroup.setModel(treeCategory.getModel());
		treeGroup.setModel(treeModel);
		checkTreeManager = new CheckTreeManager(treeGroup);
		treeGroup.setCheckTreeSelected(selectedGroup, checkTreeManager);
		scrollPaneCheckTree.setViewportView(treeGroup);
		
		buttonSaveGroup = new JButton(rb.getString("text_buttonSaveGroup"));
		buttonSaveGroup.addActionListener(new ActionListener() {
			
			/**
			 * @param arg0
			 *            Press Button
			 * @return Saves changes of this group in the database. Tab is
			 *         removed.
			 */
			public void actionPerformed(ActionEvent arg0) {
				Vector<Map> maps = treeGroup.getCheckTreeSelected(checkTreeManager);
				LazyList<GroupsMaps> groupsMaps = selectedGroup.getAll(GroupsMaps.class);
				for (GroupsMaps groupsMap : groupsMaps) {
					int groupsMapId = groupsMap.getLong("map_id").intValue();
					boolean hit = false;
					for (int i = 0; i < maps.size(); i++) {
						if (maps.get(i).getLongId().intValue() == groupsMapId) {
							hit = true; // Map is already in selected group
							maps.removeElementAt(i);
							break;
						}
					}
					if (hit == false) { // Map in GroupsMaps is not in the
										// selection in treeGroup -> delete it
						groupsMap.delete();
					}
				}
				while (maps.size() > 0) { // Map in selection in treeGroup is
											// not already in database -> create
											// it
					GroupsMaps newEntry = new GroupsMaps();
					newEntry.set("group_id", selectedGroup.getId());
					newEntry.set("map_id", maps.firstElement().getId());
					newEntry.saveIt();
					maps.removeElementAt(0); // remove first element
				}
				tabbedPane.remove(panelGroupEdit);
				
				// removes old tab of panelGroup and constructs a new one
				tabbedPane.remove(GeoLingGUI.TAB_INDEX_GROUP);
				new GroupPanel(tabbedPane, treeModel);
				tabbedPane.setSelectedIndex(GeoLingGUI.TAB_INDEX_GROUP);
			}
		});
		buttonSaveGroup.setToolTipText(rb.getString("tooltip_buttonSaveGroup"));
		GridBagConstraints gbc_buttonSaveGroup = new GridBagConstraints();
		gbc_buttonSaveGroup.insets = new Insets(0, 0, 5, 0);
		gbc_buttonSaveGroup.gridx = 1;
		gbc_buttonSaveGroup.gridy = 0;
		panelGroupEdit.add(buttonSaveGroup, gbc_buttonSaveGroup);
		
		buttonCloseTab = new JButton(rb.getString("text_buttonCloseTab"));
		buttonCloseTab.addActionListener(new ActionListener() {
			
			/**
			 * @param arg0
			 *            Press Button
			 * @return Removes <code>JPanel</code> panel_GroupEdit from
			 *         <code>JTabbedPane</code> tabbedPane without saving
			 *         changes in database.
			 */
			public void actionPerformed(ActionEvent arg0) {
				tabbedPane.remove(panelGroupEdit);
				tabbedPane.setSelectedIndex(GeoLingGUI.TAB_INDEX_GROUP);
			}
		});
		buttonCloseTab.setToolTipText(rb.getString("tooltip_buttonCloseTab"));
		GridBagConstraints gbc_buttonCloseTab = new GridBagConstraints();
		gbc_buttonCloseTab.insets = new Insets(0, 0, 5, 0);
		gbc_buttonCloseTab.gridx = 1;
		gbc_buttonCloseTab.gridy = 1;
		panelGroupEdit.add(buttonCloseTab, gbc_buttonCloseTab);
	}
}
