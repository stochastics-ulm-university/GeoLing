package geoling.gui.management_dialog;

import geoling.gui.GeoLingGUI;
import geoling.models.Border;
import geoling.models.ConfigurationOption;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Database configuration panel.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class DatabaseConfigurationPanel {

	/** The panel to which contents are added. */
	private JPanel panel = new JPanel();

	/** All border names. */
	private String[] borderNames;
	/** All borders. */
	private Border[] borders;
	/** <code>JComboBox</code> that shows all borders. */
	private JComboBox<String> comboBoxDefaultBorder = new JComboBox<String>();

	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;

	public DatabaseConfigurationPanel(final JTabbedPane tabbedPane) {

		rb = ResourceBundle.getBundle("DatabaseConfigurationPanel", GeoLingGUI.LANGUAGE);

		tabbedPane.insertTab(rb.getString("title_DatabaseConfigurationPanel"), null, panel, null, DatabaseManagementDialog.TAB_CONFIGURATION);
		tabbedPane.addChangeListener(new ChangeListener() {

			// This method is called whenever the selected tab changes
			public void stateChanged(ChangeEvent evt) {
				JTabbedPane pane = (JTabbedPane) evt.getSource();
				// if database configuration tab is selected
				if (pane.getSelectedIndex() == DatabaseManagementDialog.TAB_CONFIGURATION) { 
					// Refreshes border names
					List<Border> allBorders = null;
					try {
						allBorders = Border.findAll();
						allBorders = new ArrayList<Border>(allBorders);
					} catch (Exception e) {
						// ignore missing database table etc.
						allBorders = new ArrayList<Border>();
					}
					borderNames = new String[allBorders.size()];
					borders = new Border[allBorders.size()];
					for (int i=0; i<allBorders.size(); i++) {
						borderNames[i] = allBorders.get(i).getString("name");
						borders[i] = allBorders.get(i);
					}
					comboBoxDefaultBorder.setModel((new DefaultComboBoxModel<String>(borderNames)));
				}

			}
		});

		// get all available borders
		List<Border> allBorders = null;
		try {
			allBorders = Border.findAll();
			allBorders = new ArrayList<Border>(allBorders);
		} catch (Exception e) {
			// ignore missing database table etc.
			allBorders = new ArrayList<Border>();
		}
		borderNames = new String[allBorders.size()];
		borders = new Border[allBorders.size()];
		for (int i=0; i<allBorders.size(); i++) {
			borderNames[i] = allBorders.get(i).getString("name");
			borders[i] = allBorders.get(i);
		}


		final Boolean[] trueFalseAnswer = new Boolean[] {Boolean.TRUE, Boolean.FALSE};

		// set layout
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 200, 100, 10, 0 };
		gridBagLayout.rowHeights = new int[] { 40, 40, 40, 40, 40, 40, 60, 10, 0 };
		gridBagLayout.columnWeights = new double[] { 0.2, 0.2, 0.6, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0, 0, 0, 0, 0, 0, 0, 1, Double.MIN_VALUE };
		panel.setLayout(gridBagLayout);

		JLabel lblConfig = new JLabel(rb.getString("text_lblConfig"));
		GridBagConstraints gbc_lblConfig = new GridBagConstraints();
		gbc_lblConfig.fill = GridBagConstraints.BOTH;
		gbc_lblConfig.insets = new Insets(5, 5, 5, 5);
		gbc_lblConfig.gridx = 0;
		gbc_lblConfig.gridy = 0;
		panel.add(lblConfig, gbc_lblConfig);

		// plotLocationCodes
		JLabel lblLocationCodes = new JLabel("plotLocationCodes");
		lblLocationCodes.setToolTipText(rb.getString("tooltip_lblLocationCodes"));
		GridBagConstraints gbc_lblLocationCodes = new GridBagConstraints();
		gbc_lblLocationCodes.fill = GridBagConstraints.BOTH;
		gbc_lblLocationCodes.insets = new Insets(5, 5, 5, 5);
		gbc_lblLocationCodes.gridx = 0;
		gbc_lblLocationCodes.gridy = 1;
		panel.add(lblLocationCodes, gbc_lblLocationCodes);


		final JComboBox<Boolean> comboBoxLocationCodes = new JComboBox<Boolean>(new DefaultComboBoxModel<Boolean>(trueFalseAnswer));
		GridBagConstraints gbc_comboBoxLocationCodes = new GridBagConstraints();
		gbc_comboBoxLocationCodes.fill = GridBagConstraints.BOTH;
		gbc_comboBoxLocationCodes.insets = new Insets(5, 5, 5, 5);
		gbc_comboBoxLocationCodes.gridx = 1;
		gbc_comboBoxLocationCodes.gridy = 1;
		panel.add(comboBoxLocationCodes, gbc_comboBoxLocationCodes);
		comboBoxLocationCodes.setSelectedItem(ConfigurationOption.getOption("plotLocationCodes", false));


		// useAllLocationsInDensityEstimation
		JLabel lblAllLocations = new JLabel("useAllLocationsInDensityEstimation");
		lblAllLocations.setToolTipText(rb.getString("tooltip_lblAllLocations"));
		GridBagConstraints gbc_lblAllLocations = new GridBagConstraints();
		gbc_lblAllLocations.fill = GridBagConstraints.BOTH;
		gbc_lblAllLocations.insets = new Insets(5, 5, 5, 5);
		gbc_lblAllLocations.gridx = 0;
		gbc_lblAllLocations.gridy = 2;
		panel.add(lblAllLocations, gbc_lblAllLocations);

		final JComboBox<Boolean> comboBoxAllLocations = new JComboBox<Boolean>(new DefaultComboBoxModel<Boolean>(trueFalseAnswer));
		GridBagConstraints gbc_comboBoxAllLocations = new GridBagConstraints();
		gbc_comboBoxAllLocations.fill = GridBagConstraints.BOTH;
		gbc_comboBoxAllLocations.insets = new Insets(5, 5, 5, 5);
		gbc_comboBoxAllLocations.gridx = 1;
		gbc_comboBoxAllLocations.gridy = 2;
		panel.add(comboBoxAllLocations, gbc_comboBoxAllLocations);
		comboBoxAllLocations.setSelectedItem(ConfigurationOption.getOption("useAllLocationsInDensityEstimation", true));


		// useLocationAggregation
		JLabel lblLocationAggregation = new JLabel("useLocationAggregation");
		lblLocationAggregation.setToolTipText(rb.getString("tooltip_lblLocationAggregation"));
		GridBagConstraints gbc_lblLocationAggregation = new GridBagConstraints();
		gbc_lblLocationAggregation.fill = GridBagConstraints.BOTH;
		gbc_lblLocationAggregation.insets = new Insets(5, 5, 5, 5);
		gbc_lblLocationAggregation.gridx = 0;
		gbc_lblLocationAggregation.gridy = 3;
		panel.add(lblLocationAggregation, gbc_lblLocationAggregation);

		final JComboBox<Boolean> comboBoxLocationAggregation = new JComboBox<Boolean>(new DefaultComboBoxModel<Boolean>(trueFalseAnswer));
		GridBagConstraints gbc_comboBoxLocationAggregation = new GridBagConstraints();
		gbc_comboBoxLocationAggregation.fill = GridBagConstraints.BOTH;
		gbc_comboBoxLocationAggregation.insets = new Insets(5, 5, 5, 5);
		gbc_comboBoxLocationAggregation.gridx = 1;
		gbc_comboBoxLocationAggregation.gridy = 3;
		panel.add(comboBoxLocationAggregation, gbc_comboBoxLocationAggregation);
		comboBoxLocationAggregation.setSelectedItem(ConfigurationOption.getOption("useLocationAggregation", false));


		// ignoreFrequenciesInDensityEstimation
		JLabel lblIgnoreFrequencies = new JLabel("ignoreFrequenciesInDensityEstimation");
		lblIgnoreFrequencies.setToolTipText(rb.getString("tooltip_lblIgnoreFrequencies"));
		GridBagConstraints gbc_lblIgnoreFrequencies = new GridBagConstraints();
		gbc_lblIgnoreFrequencies.fill = GridBagConstraints.BOTH;
		gbc_lblIgnoreFrequencies.insets = new Insets(5, 5, 5, 5);
		gbc_lblIgnoreFrequencies.gridx = 0;
		gbc_lblIgnoreFrequencies.gridy = 4;
		panel.add(lblIgnoreFrequencies, gbc_lblIgnoreFrequencies);

		final JComboBox<Boolean> comboBoxIgnoreFrequencies = new JComboBox<Boolean>(new DefaultComboBoxModel<Boolean>(trueFalseAnswer));
		GridBagConstraints gbc_comboBoxIgnoreFrequencies = new GridBagConstraints();
		gbc_comboBoxIgnoreFrequencies.fill = GridBagConstraints.BOTH;
		gbc_comboBoxIgnoreFrequencies.insets = new Insets(5, 5, 5, 5);
		gbc_comboBoxIgnoreFrequencies.gridx = 1;
		gbc_comboBoxIgnoreFrequencies.gridy = 4;
		panel.add(comboBoxIgnoreFrequencies, gbc_comboBoxIgnoreFrequencies);
		comboBoxIgnoreFrequencies.setSelectedItem(ConfigurationOption.getOption("ignoreFrequenciesInDensityEstimation", false));


		// defaultBorderId
		final JLabel lblDefaultBorder = new JLabel("defaultBorder");
		lblDefaultBorder.setToolTipText(rb.getString("tooltip_lblDefaultBorder"));
		GridBagConstraints gbc_lblDefaultBorder = new GridBagConstraints();
		gbc_lblDefaultBorder.fill = GridBagConstraints.BOTH;
		gbc_lblDefaultBorder.insets = new Insets(5, 5, 5, 5);
		gbc_lblDefaultBorder.gridx = 0;
		gbc_lblDefaultBorder.gridy = 5;
		panel.add(lblDefaultBorder, gbc_lblDefaultBorder);

		comboBoxDefaultBorder.setModel((new DefaultComboBoxModel<String>(borderNames)));
		GridBagConstraints gbc_comboBoxDefaultBorder = new GridBagConstraints();
		gbc_comboBoxDefaultBorder.fill = GridBagConstraints.BOTH;
		gbc_comboBoxDefaultBorder.insets = new Insets(5, 5, 5, 5);
		gbc_comboBoxDefaultBorder.gridx = 1;
		gbc_comboBoxDefaultBorder.gridy = 5;
		panel.add(comboBoxDefaultBorder, gbc_comboBoxDefaultBorder);
		if (borders.length>0) {
			comboBoxDefaultBorder.setSelectedIndex(0);
		}


		// button to set configuration options
		JButton buttonSetConfiguration = new JButton(rb.getString("text_buttonSetConfiguration"));
		GridBagConstraints gbc_buttonSetConfiguration = new GridBagConstraints();
		gbc_buttonSetConfiguration.fill = GridBagConstraints.BOTH;
		gbc_buttonSetConfiguration.insets = new Insets(5, 5, 5, 5);
		gbc_buttonSetConfiguration.gridx = 0;
		gbc_buttonSetConfiguration.gridy = 6;
		gbc_buttonSetConfiguration.gridwidth = 2;
		panel.add(buttonSetConfiguration, gbc_buttonSetConfiguration);
		buttonSetConfiguration.addActionListener(new ActionListener() {		
			/** Button is pressed. */
			public void actionPerformed(ActionEvent arg0) {
				ConfigurationOption.setOption("plotLocationCodes", trueFalseAnswer[comboBoxLocationCodes.getSelectedIndex()]);
				ConfigurationOption.setOption("useAllLocationsInDensityEstimation", trueFalseAnswer[comboBoxAllLocations.getSelectedIndex()]);
				ConfigurationOption.setOption("useLocationAggregation", trueFalseAnswer[comboBoxLocationAggregation.getSelectedIndex()]);
				ConfigurationOption.setOption("ignoreFrequenciesInDensityEstimation", trueFalseAnswer[comboBoxIgnoreFrequencies.getSelectedIndex()]);
				if (borders.length>0) {
					ConfigurationOption.setOption("defaultBorderId", borders[comboBoxDefaultBorder.getSelectedIndex()].getInteger("id"));
				}
			}
		});


	}

}
