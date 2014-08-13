package geoling.gui;

import geoling.models.Group;
import geoling.models.GroupsMaps;
import geoling.models.Map;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;

import org.javalite.activejdbc.LazyList;

/**
 * Panel for group overview in dialectometry GUI.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class GroupPanel {
	
	private JTabbedPane tabbedPane;
	private TreeModel treeModel;
	
	/**
	 * <code>JPanel</code> for <code>JTable</code>, <code>JList</code> and two
	 * <code>JButton</code>. <code>JTable</code> and <code>JList</code> are
	 * embedded in a <code>JScrollPane</code>.
	 */
	private JPanel panelGroup;
	/** <code>JScrollPane</code> for the <code>JTable</code> tableGroup */
	private JScrollPane scrollPaneGroupTable = new JScrollPane();
	/** <code>JScrollPane</code> for the <code>JList</code> listElements */
	private JScrollPane scrollPaneMapList = new JScrollPane();
	/**
	 * This <code>Group</code> is the currently selected group in
	 * <code>JTable</code> tableGroup.
	 */
	private Group selectedGroup = null;
	/** This <code>JTable</code> shows all groups in the database. */
	private JTable tableGroup;
	/**
	 * This <code>JList</code> shows all maps which belongs to the currently
	 * selected group in <code>JTable</code> tableGroup
	 */
	private JList<String> listGroupsMaps = new JList<String>();
	/** This <code>JButton</code> creates an empty group in the database */
	private JButton buttonNewGroup;
	/**
	 * This <code>JButton</code> deletes the selected group in the
	 * <code>JTable</code> from the database.
	 */
	private JButton buttonDeleteGroup;
	
	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;
	
	/**
	 * Create a panel for editing groups in the database.
	 * 
	 * @param tabbedPane
	 *            The new panel is added to this <code>JTabbedPane</code>.
	 * @param treeModel
	 *            This model describes the belonging of maps to categories.
	 */
	public GroupPanel(JTabbedPane tabbedPane, TreeModel treeModel) {
		this.tabbedPane = tabbedPane;
		this.treeModel = treeModel;
		rb = ResourceBundle.getBundle("GroupPanel", GeoLingGUI.LANGUAGE);
		
		panelGroup = new JPanel();
		tabbedPane.insertTab(rb.getString("title_GroupPanel"), null, panelGroup, null, GeoLingGUI.TAB_INDEX_GROUP);
		GridBagLayout gbl_panelGroup = new GridBagLayout();
		gbl_panelGroup.columnWidths = new int[] { 125, 125, 250, 0 };
		gbl_panelGroup.rowHeights = new int[] { 400, 50, 50, 0 };
		gbl_panelGroup.columnWeights = new double[] { 1.0, 0.25, 0.5, Double.MIN_VALUE };
		gbl_panelGroup.rowWeights = new double[] { 0.8, 0.1, 0.1, Double.MIN_VALUE };
		panelGroup.setLayout(gbl_panelGroup);
		
		GridBagConstraints gbc_scrollPaneMapList = new GridBagConstraints();
		gbc_scrollPaneMapList.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneMapList.gridx = 2;
		gbc_scrollPaneMapList.gridy = 0;
		gbc_scrollPaneMapList.gridheight = 3;
		scrollPaneMapList.setViewportView(listGroupsMaps);
		panelGroup.add(scrollPaneMapList, gbc_scrollPaneMapList);
		
		tableGroup = new JTable();
		createTableGroup();
		
		buttonNewGroup = new JButton(rb.getString("text_buttonNewGroup"));
		buttonNewGroup.addActionListener(new ActionListener() {
			
			/**
			 * @param arg0
			 *            Press Button
			 * @return Creates new group in the database. <code>JPanel</code>
			 *         panel_GroupEdit is removed from <code>JTabbedPane</code>
			 *         scrollPane_GroupEdit.
			 */
			public void actionPerformed(ActionEvent arg0) {
				Group newGroup = new Group();
				String eingabe = JOptionPane.showInputDialog(null, rb.getString("message_dialogCreateGroup"), rb.getString("title_dialogCreateGroup"),
						JOptionPane.PLAIN_MESSAGE);
				if (eingabe != null) {
					newGroup.set("name", eingabe);
					newGroup.saveIt();
					
					// refresh tableGroup
					panelGroup.remove(scrollPaneGroupTable);
					createTableGroup();
				}
			}
		});
		buttonNewGroup.setToolTipText(rb.getString("tooltip_buttonNewGroup"));
		GridBagConstraints gbc_buttonNewGroup = new GridBagConstraints();
		gbc_buttonNewGroup.insets = new Insets(0, 0, 5, 5);
		gbc_buttonNewGroup.gridx = 0;
		gbc_buttonNewGroup.gridy = 1;
		panelGroup.add(buttonNewGroup, gbc_buttonNewGroup);
		
		buttonDeleteGroup = new JButton(rb.getString("text_buttonDeleteGroup"));
		buttonDeleteGroup.addActionListener(new ActionListener() {
			
			/**
			 * @param arg0
			 *            Press Button
			 * @return Deletes group from the database.
			 */
			public void actionPerformed(ActionEvent arg0) {
				if (selectedGroup != null) {
					String[] options = { rb.getString("option1_dialogDeleteGroup"), rb.getString("option2_dialogDeleteGroup") };
					int selected = JOptionPane.showOptionDialog(null, rb.getString("message_dialogDeleteGroup"), rb.getString("title_dialogDeleteGroup"),
							JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[1]);
					if (selected == 0) { // Answer: Yes
						GroupsMaps.delete("group_id = ?", selectedGroup.getId());
						selectedGroup.delete();
						selectedGroup = null;
						// refresh tableGroup
						panelGroup.remove(scrollPaneGroupTable);
						createTableGroup();
					}
				}
			}
		});
		buttonDeleteGroup.setToolTipText(rb.getString("tooltip_buttonDeleteGroup"));
		GridBagConstraints gbc_buttonDeleteGroup = new GridBagConstraints();
		gbc_buttonDeleteGroup.insets = new Insets(0, 0, 5, 5);
		gbc_buttonDeleteGroup.gridx = 1;
		gbc_buttonDeleteGroup.gridy = 1;
		panelGroup.add(buttonDeleteGroup, gbc_buttonDeleteGroup);
	}
	
	/**
	 * Creates a new <code>JTable</code> and assigns it to tableGroup. This
	 * <code>JTable</code> gets a new model and a <code>MouseListener</code>. In
	 * this <code>MouseListener</code> the <code>JList</code> listGroupsMaps
	 * gets model. After every change of data from tableGroup it's necessary to
	 * call createTableGroup, e.g. after adding, editing or deleting of group by
	 * pressing a <code>JButton</code>.
	 */
	private void createTableGroup() {
		final LazyList<Group> groups = Group.findAll();
		Object[][] tableContentsGroup = new Object[groups.size()][2];
		for (int i = 0; i < groups.size(); i++) {
			tableContentsGroup[i][0] = groups.get(i).getString("name");
			tableContentsGroup[i][1] = GroupsMaps.count("group_id = ?", groups.get(i).getId());
		}
		tableGroup = new JTable();
		tableGroup.addKeyListener(new KeyAdapter() {
			
			/**
			 * ActionListener for press button inside of <code>JTable</code>
			 * tableGroup
			 * 
			 * @param arg0
			 *            Press up-key or down-key on keyboard
			 * @return selectedGroup is setted and <code>JList</code>
			 *         listGroupsMaps is updated
			 */
			public void keyPressed(KeyEvent arg0) {
				if ((arg0.getKeyCode() == KeyEvent.VK_UP)) {
					int select = tableGroup.getSelectedRow() - 1;
					if (select < 0) {
						select = 0;
					}
					selectedGroup = groups.get(select);
				}
				if ((arg0.getKeyCode() == KeyEvent.VK_DOWN)) {
					int select = tableGroup.getSelectedRow() + 1;
					if (select >= tableGroup.getRowCount()) {
						select = tableGroup.getRowCount() - 1;
					}
					selectedGroup = groups.get(select);
				}
				if ((arg0.getKeyCode() == KeyEvent.VK_F2)) {
					selectedGroup = groups.get(tableGroup.getSelectedRow());
					String eingabe = JOptionPane.showInputDialog(null, rb.getString("message_dialogGroupName"), rb.getString("title_dialogGroupName"),
							JOptionPane.PLAIN_MESSAGE);
					if (eingabe != null) {
						selectedGroup.set("name", eingabe);
						selectedGroup.saveIt();
						// refresh tableGroup
						panelGroup.remove(scrollPaneGroupTable);
						createTableGroup();
						return;
					}
				}
				
				if (selectedGroup != null) {
					// listGroupsMaps aktualisieren
					LazyList<GroupsMaps> gms = GroupsMaps.find("group_id = ?", selectedGroup.getId());
					final String[] values_new = new String[gms.size()];
					for (int i = 0; i < gms.size(); i++) {
						Object mapId_gms = gms.get(i).get("map_id");
						values_new[i] = Map.findById(mapId_gms).getString("name");
					}
					
					listGroupsMaps = new JList<String>();
					listGroupsMaps.setModel(new AbstractListModel<String>() {
						
						private static final long serialVersionUID = 1L;
						String[] values = values_new;
						
						public int getSize() {
							return values.length;
						}
						
						public String getElementAt(int index) {
							return values[index];
						}
					});
					
					GridBagConstraints gbc_scrollPane_2_2 = new GridBagConstraints();
					gbc_scrollPane_2_2.fill = GridBagConstraints.BOTH;
					gbc_scrollPane_2_2.gridx = 2;
					gbc_scrollPane_2_2.gridy = 0;
					gbc_scrollPane_2_2.gridheight = 3;
					scrollPaneMapList.setViewportView(listGroupsMaps);
					panelGroup.add(scrollPaneMapList, gbc_scrollPane_2_2);
				}
			}
			// }
		});
		
		tableGroup.setModel(new DefaultTableModel(tableContentsGroup, new String[] { rb.getString("columnName1_tableGroup"),
				rb.getString("columnName2_tableGroup") }) {
			
			private static final long serialVersionUID = 1L;
			
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
		});
		tableGroup.addMouseListener(new MouseAdapter() {
			
			/**
			 * ActionListener for mouse click on the group table
			 * 
			 * @param arg0
			 *            mouse click or double click
			 * @return groupId is set and new tab is added to tabbedPane
			 */
			public void mouseClicked(MouseEvent arg0) {
				selectedGroup = groups.get(tableGroup.getSelectedRow());
				if (selectedGroup != null) {
					// listGroupsMaps aktualisieren
					LazyList<GroupsMaps> gms = GroupsMaps.find("group_id = ?", selectedGroup.getId());
					final String[] values_new = new String[gms.size()];
					for (int i = 0; i < gms.size(); i++) {
						Object mapId_gms = gms.get(i).get("map_id");
						values_new[i] = Map.findById(mapId_gms).getString("name");
					}
					
					listGroupsMaps = new JList<String>();
					listGroupsMaps.setModel(new AbstractListModel<String>() {
						
						private static final long serialVersionUID = 1L;
						String[] values = values_new;
						
						public int getSize() {
							return values.length;
						}
						
						public String getElementAt(int index) {
							return values[index];
						}
					});
					
					GridBagConstraints gbc_scrollPaneMapList = new GridBagConstraints();
					gbc_scrollPaneMapList.fill = GridBagConstraints.BOTH;
					gbc_scrollPaneMapList.gridx = 2;
					gbc_scrollPaneMapList.gridy = 0;
					gbc_scrollPaneMapList.gridheight = 3;
					scrollPaneMapList.setViewportView(listGroupsMaps);
					panelGroup.add(scrollPaneMapList, gbc_scrollPaneMapList);
				}
				if (arg0.getClickCount() == 2 && (selectedGroup != null)) { 
					// open new Tab GroupEditPanel
					new GroupEditPanel(tabbedPane, selectedGroup, treeModel);
				}
			}
		});
		
		scrollPaneGroupTable.setViewportView(tableGroup);
		GridBagConstraints gbc_scrollPaneGroupTable = new GridBagConstraints();
		gbc_scrollPaneGroupTable.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneGroupTable.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPaneGroupTable.gridx = 0;
		gbc_scrollPaneGroupTable.gridy = 0;
		gbc_scrollPaneGroupTable.gridwidth = 2;
		panelGroup.add(scrollPaneGroupTable, gbc_scrollPaneGroupTable);
		
	}
	
}
