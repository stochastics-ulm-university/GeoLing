package geoling.gui.management_dialog;

import geoling.config.Database;
import geoling.gui.GeoLingGUI;
import geoling.maps.distances.computation.LinguisticDistanceComputation;
import geoling.models.Group;
import geoling.models.Map;
import geoling.util.ProgressOutput;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ProgressMonitor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * A panel to compute linguistic distances for groups that can be selected in a table.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class DatabaseDistancePanel {

	/** The panel to which contents are added. */
	private JPanel panel = new JPanel();

	private JTable table;
	/** The <code>JScrollPane</code> for the <code>JTable</code>. */
	private JScrollPane scrollPane = new JScrollPane();

	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;

	public DatabaseDistancePanel(final JTabbedPane tabbedPane) {

		rb = ResourceBundle.getBundle("DatabaseDistancePanel", GeoLingGUI.LANGUAGE);

		tabbedPane.insertTab(rb.getString("title_DatabaseDistancePanel"), null, panel, null, DatabaseManagementDialog.TAB_DISTANCE);
		tabbedPane.addChangeListener(new ChangeListener() {

			// This method is called whenever the selected tab changes
			public void stateChanged(ChangeEvent evt) {
				JTabbedPane pane = (JTabbedPane) evt.getSource();
				// if database distance tab is selected
				if (pane.getSelectedIndex() == DatabaseManagementDialog.TAB_DISTANCE) { 
					table = createGroupTable();
					scrollPane.getViewport().setView(table);
				}

			}
		});

		// set layout
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 200, 200, 10, 0 };
		gridBagLayout.rowHeights = new int[] { 200, 50, 10, 0 };
		gridBagLayout.columnWeights = new double[] { 0.4, 0.4, 0.2,  Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.9, 0, 0.1, Double.MIN_VALUE };
		panel.setLayout(gridBagLayout);


		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		gbc_scrollPane.gridwidth = 2;
		panel.add(scrollPane, gbc_scrollPane);

		table = createGroupTable();
		scrollPane.setViewportView(table);
		scrollPane.setToolTipText(rb.getString("tooltip_scrollPane"));


		// button to start computation
		JButton buttonStartComputation = new JButton(rb.getString("text_buttonStartComputation"));
		GridBagConstraints gbc_buttonStartComputation = new GridBagConstraints();
		gbc_buttonStartComputation.fill = GridBagConstraints.BOTH;
		gbc_buttonStartComputation.insets = new Insets(5, 5, 5, 5);
		gbc_buttonStartComputation.gridx = 0;
		gbc_buttonStartComputation.gridy = 1;
		panel.add(buttonStartComputation, gbc_buttonStartComputation);
		buttonStartComputation.addActionListener(new ActionListener() {
			/** Button start computation is pressed. */
			public void actionPerformed(ActionEvent arg0) {
				TableModel model = table.getModel();
				ArrayList<Group> selectedGroups = new ArrayList<Group>();
				for (int row=0; row<model.getRowCount(); row++) {
					boolean isSelected = (boolean) model.getValueAt(row, 3);
					if (isSelected) {
						Object groupId = model.getValueAt(row, 1);
						selectedGroups.add((Group) Group.findById(groupId));
					}
				}
				if (selectedGroups.size()>0) {
					for (final Group group : selectedGroups) {
						Thread thread = new Thread(new Runnable() {

							public void run() {
								Database.ensureConnection();
								
								ProgressMonitor monitor = new ProgressMonitor(panel, group.getString("name"), "", 0, 100);
								ProgressOutput progress = new ProgressOutput(null, monitor, 1, true, 100);
								LinguisticDistanceComputation.computeDistances(group, progress);
							}
						});
						thread.start();
					}
				}

			}
		});

	}



	/** Finds all groups in database and puts them with some additional information in a <code>JTable</code>
	 * with check boxes in the last column.
	 * @return a <code>JTable</code> showing all groups
	 */
	private JTable createGroupTable() {
		List<Group> groups = null;
		try {
			groups = Group.findAll();
			groups = new ArrayList<Group>(groups);
		} catch (Exception e) {
			// ignore missing database table etc.
			groups = new ArrayList<Group>();
		}
		
		if (groups.size()==0) {
			return new JTable();
		}
		else {
			Object[] columnNames = {rb.getString("columnName1_table"), rb.getString("columnName2_table"), rb.getString("columnName3_table"), rb.getString("columnName4_table")};
			// create table contents
			Object[][] data = new Object[groups.size()][columnNames.length];
			for (int i=0; i<groups.size(); i++) {
				data[i][0] = groups.get(i).getString("name");
				data[i][1] = groups.get(i).getId();
				data[i][2] = groups.get(i).getAll(Map.class).size();
				data[i][3] = false;
			}

			DefaultTableModel model = new DefaultTableModel(data, columnNames);
			JTable table = new JTable(model) {
				private static final long serialVersionUID = 1L;

				@Override
				public Class<?> getColumnClass(int column) {
					switch (column) {
					case 0: return String.class;
					case 1: return Object.class;
					case 2: return Integer.class;
					case 3:	return Boolean.class;
					default: return Object.class;                     
					}
				}
			};
			return table;
		}
	}

}

