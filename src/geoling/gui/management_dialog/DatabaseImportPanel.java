package geoling.gui.management_dialog;

import geoling.config.Database;
import geoling.gui.GeoLingGUI;
import geoling.gui.util.StatusLabel;
import geoling.models.*;
import geoling.sql.SQLReader;
import geoling.util.Directory;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Map.Entry;
import java.util.zip.ZipFile;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.javalite.activejdbc.LazyList;

/**
 * Panel offering various import functions.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class DatabaseImportPanel {
	
	/** The used <code>Charset</code> of the input files (default: <code>UTF-8</code>). */
	public static Charset USED_CHARSET = StandardCharsets.UTF_8;

	/** The delimiter that separates columns in the .csv files.	 */
	public static String DELIMITER_COLUMNS = ";";
	/** The delimiter that separates multiple answers of an informant for a given map. */ 
	public static String DELIMITER_REGEX_MULTIPLE_ANSWERS = "\\|";

	/** The panel to which contents are added. */
	private JPanel panel = new JPanel();

	/** The folder with the files for import. */
	private String importFolder = null;
	/** The folder where to write the <code>map_*.csv</code> files for variant mapping. */
	private String mappingOutputFolder = null;
	/** The folder with the <code>map_*.csv</code> files containing the variant mapping. */
	private String mappingInputFolder = null;
	/** The path to the sql dump. */
	private String dumpPath = null;

	/** The name for the new border that shall be read. */
	private String borderName = null;;
	/** The name for the new distance that shall be read. */
	private String distanceName = null;
	/** The identification string for the new distance that shall be read. */
	private String distanceIdentification = null;

	/** This variable determines whether a running <code>Thread</code> shall terminate as soon as possible. */
	private boolean killThread = false;
	/** This <code>HashMap</code> links one <code>StatusLabel</code> to each <code>JCheckBox</code>. */
	private HashMap<JCheckBox, StatusLabel> hashMapStatus;
	
	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;

	/**
	 * Creates a new panel and adds it to the <code>JTabbedPane</code>.
	 * @param tabbedPane the <code>JTabbedPane</code> to which panel is added
	 * @param connection the connection to the SQL database
	 * @param dbType the type of the SQL database (<code>MySQL</code> or <code>SQLite</code>)
	 */
	public DatabaseImportPanel(final JTabbedPane tabbedPane, final Connection connection, final String dbType) {

		rb = ResourceBundle.getBundle("DatabaseImportPanel", GeoLingGUI.LANGUAGE);

		tabbedPane.insertTab(rb.getString("title_DatabaseImportPanel"), null, panel, null, DatabaseManagementDialog.TAB_IMPORT);

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 20, 200, 50, 150, 0 };
		gridBagLayout.rowHeights = new int[] { 30, 1, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 10, 30, 10, 50, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.3, 0.3, 0.0, Double.MIN_VALUE };
		panel.setLayout(gridBagLayout);


		// create components (two radio buttons, many check boxes and some buttons to choose files or folders)

		JRadioButton radioButtonImportFiles = new JRadioButton(rb.getString("text_radioButtonImportFiles"));
		radioButtonImportFiles.setActionCommand(rb.getString("text_radioButtonImportFiles"));
		GridBagConstraints gbc_radioButtonImportFiles = new GridBagConstraints();
		gbc_radioButtonImportFiles.fill = GridBagConstraints.BOTH;
		gbc_radioButtonImportFiles.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonImportFiles.gridx = 0;
		gbc_radioButtonImportFiles.gridy = 0;
		gbc_radioButtonImportFiles.gridwidth = 2;
		panel.add(radioButtonImportFiles, gbc_radioButtonImportFiles);

		JButton buttonFolderImportFiles = new JButton(rb.getString("text_buttonFolderImportFiles"));
		GridBagConstraints gbc_buttonFolderImportFiles = new GridBagConstraints();
		gbc_buttonFolderImportFiles.fill = GridBagConstraints.BOTH;
		gbc_buttonFolderImportFiles.insets = new Insets(0, 0, 5, 5);
		gbc_buttonFolderImportFiles.gridx = 2;
		gbc_buttonFolderImportFiles.gridy = 0;
		panel.add(buttonFolderImportFiles, gbc_buttonFolderImportFiles);
		buttonFolderImportFiles.addActionListener(new ActionListener() {	
			/** opens window to choose folder with input files */
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = chooser.showOpenDialog(tabbedPane);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					importFolder = chooser.getSelectedFile().toPath().toAbsolutePath().toString();
				}
				if (importFolder==null) {
					for (JCheckBox checkBox : hashMapStatus.keySet()) {
						checkBox.setEnabled(false);
					}
				}
				else {
					importFolder = Directory.ensureTrailingSlash(importFolder);
					for (JCheckBox checkBox : hashMapStatus.keySet()) {
						checkBox.setEnabled(true);
					}
				}
			}
		});

		JButton buttonChooseEncoding = new JButton(rb.getString("text_buttonChooseEncoding"));
		buttonChooseEncoding.setToolTipText(rb.getString("tooltip_buttonChooseEncoding"));
		GridBagConstraints gbc_buttonChooseEncoding = new GridBagConstraints();
		gbc_buttonChooseEncoding.fill = GridBagConstraints.BOTH;
		gbc_buttonChooseEncoding.insets = new Insets(0, 0, 5, 5);
		gbc_buttonChooseEncoding.gridx = 3;
		gbc_buttonChooseEncoding.gridy = 0;
		panel.add(buttonChooseEncoding, gbc_buttonChooseEncoding);
		buttonChooseEncoding.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				JLabel labelEncoding = new JLabel(rb.getString("text_labelEncoding"));
				JRadioButton radioButtonUTF = new JRadioButton(StandardCharsets.UTF_8.displayName());
				radioButtonUTF.setActionCommand("UTF8");
				JRadioButton radioButtonISO = new JRadioButton(StandardCharsets.ISO_8859_1.displayName());
				radioButtonISO.setActionCommand("ISO");
				ButtonGroup buttonGroupEncoding = new ButtonGroup();
				buttonGroupEncoding.add(radioButtonUTF);
				buttonGroupEncoding.add(radioButtonISO);
				radioButtonUTF.setSelected(true);
				Object[] messageEncoding = {labelEncoding, radioButtonUTF, radioButtonISO};
				JOptionPane paneEncoding = new JOptionPane(messageEncoding, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				paneEncoding.createDialog(panel, rb.getString("title_paneEncoding")).setVisible(true);
				if(((Integer)paneEncoding.getValue()).intValue() == JOptionPane.OK_OPTION) {
				  if (buttonGroupEncoding.getSelection().getActionCommand().equals("UTF8")) {
					  USED_CHARSET = StandardCharsets.UTF_8;
				  }
				  else {
					  USED_CHARSET = StandardCharsets.ISO_8859_1;
				  }
				}

				
			}
		});



		// read locations
		final JCheckBox checkBoxLocations = new JCheckBox(rb.getString("text_checkBoxLocations"));
		GridBagConstraints gbc_checkBoxLocations = new GridBagConstraints();
		gbc_checkBoxLocations.fill = GridBagConstraints.BOTH;
		gbc_checkBoxLocations.insets = new Insets(0, 0, 5, 5);
		gbc_checkBoxLocations.gridx = 1;
		gbc_checkBoxLocations.gridy = 2;
		panel.add(checkBoxLocations, gbc_checkBoxLocations);

		final StatusLabel statusLabelLocations = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelLocations = new GridBagConstraints();
		gbc_statusLabelLocations.fill = GridBagConstraints.BOTH;
		gbc_statusLabelLocations.insets = new Insets(0, 0, 5, 5);
		gbc_statusLabelLocations.gridx = 3;
		gbc_statusLabelLocations.gridy = 2;
		panel.add(statusLabelLocations, gbc_statusLabelLocations);


		// read maps
		final JCheckBox checkBoxMaps = new JCheckBox(rb.getString("text_checkBoxMaps"));
		GridBagConstraints gbc_checkBoxMaps = new GridBagConstraints();
		gbc_checkBoxMaps.fill = GridBagConstraints.BOTH;
		gbc_checkBoxMaps.insets = new Insets(0, 0, 5, 5);
		gbc_checkBoxMaps.gridx = 1;
		gbc_checkBoxMaps.gridy = 3;
		panel.add(checkBoxMaps, gbc_checkBoxMaps);

		final StatusLabel statusLabelMaps = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelMaps = new GridBagConstraints();
		gbc_statusLabelMaps.fill = GridBagConstraints.BOTH;
		gbc_statusLabelMaps.insets = new Insets(0, 0, 5, 5);
		gbc_statusLabelMaps.gridx = 3;
		gbc_statusLabelMaps.gridy = 3;
		panel.add(statusLabelMaps, gbc_statusLabelMaps);


		// read informant answers
		final JCheckBox checkBoxInformantAnswers = new JCheckBox(rb.getString("text_checkBoxInformantAnswers"));
		GridBagConstraints gbc_checkBoxInformantAnswers = new GridBagConstraints();
		gbc_checkBoxInformantAnswers.fill = GridBagConstraints.BOTH;
		gbc_checkBoxInformantAnswers.insets = new Insets(0, 0, 5, 5);
		gbc_checkBoxInformantAnswers.gridx = 1;
		gbc_checkBoxInformantAnswers.gridy = 4;
		panel.add(checkBoxInformantAnswers, gbc_checkBoxInformantAnswers);

		final StatusLabel statusLabelInformantAnswers = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelInformantAnswers = new GridBagConstraints();
		gbc_statusLabelInformantAnswers.fill = GridBagConstraints.BOTH;
		gbc_statusLabelInformantAnswers.insets = new Insets(0, 0, 5, 5);
		gbc_statusLabelInformantAnswers.gridx = 3;
		gbc_statusLabelInformantAnswers.gridy = 4;
		panel.add(statusLabelInformantAnswers, gbc_statusLabelInformantAnswers);


		// read border coordinates
		final JCheckBox checkBoxBorderCoordinates = new JCheckBox(rb.getString("text_checkBoxBorderCoordinates"));
		GridBagConstraints gbc_checkBoxBorderCoordinates = new GridBagConstraints();
		gbc_checkBoxBorderCoordinates.fill = GridBagConstraints.BOTH;
		gbc_checkBoxBorderCoordinates.insets = new Insets(0, 0, 5, 5);
		gbc_checkBoxBorderCoordinates.gridx = 1;
		gbc_checkBoxBorderCoordinates.gridy = 5;
		panel.add(checkBoxBorderCoordinates, gbc_checkBoxBorderCoordinates);
		checkBoxBorderCoordinates.addActionListener(new ActionListener() {
			/** Asks for border name if check box is selected. */
			public void actionPerformed(ActionEvent e) {
				if (checkBoxBorderCoordinates.isSelected()) {
					JTextField fieldBorderName = new JTextField();
					Object[] message = {rb.getString("text_dialogBorderCoordinates"), fieldBorderName};
					int option = JOptionPane.showConfirmDialog(panel, message, rb.getString("title_dialogBorderCoordinates"), JOptionPane.OK_CANCEL_OPTION);
					if (option == JOptionPane.OK_OPTION) {
						borderName = fieldBorderName.getText();
					}
					else {
						checkBoxBorderCoordinates.setSelected(false);
					}
				}

			}
		});


		final StatusLabel statusLabelBorderCoordinates = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelBorderCoordinates = new GridBagConstraints();
		gbc_statusLabelBorderCoordinates.fill = GridBagConstraints.BOTH;
		gbc_statusLabelBorderCoordinates.insets = new Insets(0, 0, 5, 5);
		gbc_statusLabelBorderCoordinates.gridx = 3;
		gbc_statusLabelBorderCoordinates.gridy = 5;
		panel.add(statusLabelBorderCoordinates, gbc_statusLabelBorderCoordinates);


		// read groups
		final JCheckBox checkBoxGroups = new JCheckBox(rb.getString("text_checkBoxGroups"));
		GridBagConstraints gbc_checkBoxGroups = new GridBagConstraints();
		gbc_checkBoxGroups.fill = GridBagConstraints.BOTH;
		gbc_checkBoxGroups.insets = new Insets(0, 0, 5, 5);
		gbc_checkBoxGroups.gridx = 1;
		gbc_checkBoxGroups.gridy = 6;
		panel.add(checkBoxGroups, gbc_checkBoxGroups);

		final StatusLabel statusLabelGroups = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelGroups = new GridBagConstraints();
		gbc_statusLabelGroups.fill = GridBagConstraints.BOTH;
		gbc_statusLabelGroups.insets = new Insets(0, 0, 5, 5);
		gbc_statusLabelGroups.gridx = 3;
		gbc_statusLabelGroups.gridy = 6;
		panel.add(statusLabelGroups, gbc_statusLabelGroups);


		// read categories
		final JCheckBox checkBoxCategories = new JCheckBox(rb.getString("text_checkBoxCategories"));
		GridBagConstraints gbc_checkBoxCategories = new GridBagConstraints();
		gbc_checkBoxCategories.fill = GridBagConstraints.BOTH;
		gbc_checkBoxCategories.insets = new Insets(0, 0, 5, 5);
		gbc_checkBoxCategories.gridx = 1;
		gbc_checkBoxCategories.gridy = 7;
		panel.add(checkBoxCategories, gbc_checkBoxCategories);

		final StatusLabel statusLabelCategories = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelCategories = new GridBagConstraints();
		gbc_statusLabelCategories.fill = GridBagConstraints.BOTH;
		gbc_statusLabelCategories.insets = new Insets(0, 0, 5, 5);
		gbc_statusLabelCategories.gridx = 3;
		gbc_statusLabelCategories.gridy = 7;
		panel.add(statusLabelCategories, gbc_statusLabelCategories);


		// prepare variant mapping
		final JCheckBox checkBoxPrepareMapping = new JCheckBox(rb.getString("text_checkBoxPrepareMapping"));
		GridBagConstraints gbc_checkBoxPrepareMapping = new GridBagConstraints();
		gbc_checkBoxPrepareMapping.fill = GridBagConstraints.BOTH;
		gbc_checkBoxPrepareMapping.insets = new Insets(0, 0, 5, 5);
		gbc_checkBoxPrepareMapping.gridx = 1;
		gbc_checkBoxPrepareMapping.gridy = 8;
		panel.add(checkBoxPrepareMapping, gbc_checkBoxPrepareMapping);

		JButton buttonFolderPrepareMapping = new JButton(rb.getString("text_buttonFolderPrepareMapping"));
		GridBagConstraints gbc_buttonFolderPrepareMapping = new GridBagConstraints();
		gbc_buttonFolderPrepareMapping.fill = GridBagConstraints.BOTH;
		gbc_buttonFolderPrepareMapping.insets = new Insets(0, 0, 5, 5);
		gbc_buttonFolderPrepareMapping.gridx = 2;
		gbc_buttonFolderPrepareMapping.gridy = 8;
		panel.add(buttonFolderPrepareMapping, gbc_buttonFolderPrepareMapping);
		buttonFolderPrepareMapping.addActionListener(new ActionListener() {	
			/** opens window to choose output folder */
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (importFolder!=null) {
					chooser.setCurrentDirectory(new File(importFolder));
				}
				int returnVal = chooser.showOpenDialog(tabbedPane);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					mappingOutputFolder = chooser.getSelectedFile().toPath().toAbsolutePath().toString();
				}
				if (mappingOutputFolder==null) {
					checkBoxPrepareMapping.setEnabled(false);
					checkBoxPrepareMapping.setSelected(false);
				}
				else {
					mappingOutputFolder = Directory.ensureTrailingSlash(mappingOutputFolder);
					checkBoxPrepareMapping.setEnabled(true);
					checkBoxPrepareMapping.setSelected(true);
				}
			}
		});

		final StatusLabel statusLabelPrepareMapping = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelPrepareMapping = new GridBagConstraints();
		gbc_statusLabelPrepareMapping.fill = GridBagConstraints.BOTH;
		gbc_statusLabelPrepareMapping.insets = new Insets(0, 0, 5, 5);
		gbc_statusLabelPrepareMapping.gridx = 3;
		gbc_statusLabelPrepareMapping.gridy = 8;
		panel.add(statusLabelPrepareMapping, gbc_statusLabelPrepareMapping);


		// read variant mapping
		final JCheckBox checkBoxReadMapping = new JCheckBox(rb.getString("text_checkBoxReadMapping"));
		GridBagConstraints gbc_checkBoxReadMapping = new GridBagConstraints();
		gbc_checkBoxReadMapping.fill = GridBagConstraints.BOTH;
		gbc_checkBoxReadMapping.insets = new Insets(0, 0, 5, 5);
		gbc_checkBoxReadMapping.gridx = 1;
		gbc_checkBoxReadMapping.gridy = 9;
		panel.add(checkBoxReadMapping, gbc_checkBoxReadMapping);

		JButton buttonFolderReadMapping = new JButton(rb.getString("text_buttonFolderReadMapping"));
		GridBagConstraints gbc_buttonFolderReadMapping = new GridBagConstraints();
		gbc_buttonFolderReadMapping.fill = GridBagConstraints.BOTH;
		gbc_buttonFolderReadMapping.insets = new Insets(0, 0, 5, 5);
		gbc_buttonFolderReadMapping.gridx = 2;
		gbc_buttonFolderReadMapping.gridy = 9;
		panel.add(buttonFolderReadMapping, gbc_buttonFolderReadMapping);
		buttonFolderReadMapping.addActionListener(new ActionListener() {	
			/** opens window to choose input folder */
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (importFolder!=null) {
					chooser.setCurrentDirectory(new File(importFolder));
				}
				int returnVal = chooser.showOpenDialog(tabbedPane);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					mappingInputFolder = chooser.getSelectedFile().toPath().toAbsolutePath().toString();
				}
				if (mappingInputFolder==null) {
					checkBoxReadMapping.setEnabled(false);
					checkBoxReadMapping.setSelected(false);
				}
				else {
					mappingInputFolder = Directory.ensureTrailingSlash(mappingInputFolder);
					checkBoxReadMapping.setEnabled(true);
					checkBoxReadMapping.setSelected(true);
				}
			}
		});

		final StatusLabel statusLabelReadMapping = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelReadMapping = new GridBagConstraints();
		gbc_statusLabelReadMapping.fill = GridBagConstraints.BOTH;
		gbc_statusLabelReadMapping.insets = new Insets(0, 0, 5, 5);
		gbc_statusLabelReadMapping.gridx = 3;
		gbc_statusLabelReadMapping.gridy = 9;
		panel.add(statusLabelReadMapping, gbc_statusLabelReadMapping);


		// optional
		JLabel lblOptional = new JLabel(rb.getString("text_lblOptional"));
		GridBagConstraints gbc_lblOptional = new GridBagConstraints();
		gbc_lblOptional.fill = GridBagConstraints.BOTH;
		gbc_lblOptional.insets = new Insets(0, 0, 5, 5);
		gbc_lblOptional.gridx = 1;
		gbc_lblOptional.gridy = 10;
		panel.add(lblOptional, gbc_lblOptional);


		// read distances
		final JCheckBox checkBoxDistances = new JCheckBox(rb.getString("text_checkBoxDistances"));
		GridBagConstraints gbc_checkBoxDistances = new GridBagConstraints();
		gbc_checkBoxDistances.fill = GridBagConstraints.BOTH;
		gbc_checkBoxDistances.insets = new Insets(0, 0, 5, 5);
		gbc_checkBoxDistances.gridx = 1;
		gbc_checkBoxDistances.gridy = 11;
		panel.add(checkBoxDistances, gbc_checkBoxDistances);
		checkBoxDistances.addActionListener(new ActionListener() {
			/** Asks for distance name and distance identification if check box is selected. */
			public void actionPerformed(ActionEvent e) {
				if (checkBoxDistances.isSelected()) {
					JTextField fieldDistanceName = new JTextField();
					JTextField fieldDistanceIdentification = new JTextField();
					Object[] message = {
							rb.getString("message1_dialogDistances"), fieldDistanceName,
							rb.getString("message2_dialogDistances"), fieldDistanceIdentification
					};

					int option = JOptionPane.showConfirmDialog(panel, message, rb.getString("title_dialogDistances"), JOptionPane.OK_CANCEL_OPTION);
					if (option == JOptionPane.OK_OPTION) {
						distanceName = fieldDistanceName.getText();
						distanceIdentification = fieldDistanceIdentification.getText();
					}
					else {
						checkBoxDistances.setSelected(false);
					}
				}
			}
		});


		final StatusLabel statusLabelDistances = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelDistances = new GridBagConstraints();
		gbc_statusLabelDistances.fill = GridBagConstraints.BOTH;
		gbc_statusLabelDistances.insets = new Insets(0, 0, 5, 5);
		gbc_statusLabelDistances.gridx = 3;
		gbc_statusLabelDistances.gridy = 11;
		panel.add(statusLabelDistances, gbc_statusLabelDistances);


		// hash map were status labels are assigned to check boxes
		hashMapStatus = new HashMap<JCheckBox, StatusLabel>();
		hashMapStatus.put(checkBoxLocations, statusLabelLocations);
		hashMapStatus.put(checkBoxMaps, statusLabelMaps);
		hashMapStatus.put(checkBoxInformantAnswers, statusLabelInformantAnswers);
		hashMapStatus.put(checkBoxBorderCoordinates, statusLabelBorderCoordinates);
		hashMapStatus.put(checkBoxGroups, statusLabelGroups);
		hashMapStatus.put(checkBoxCategories, statusLabelCategories);
		hashMapStatus.put(checkBoxPrepareMapping, statusLabelPrepareMapping);
		hashMapStatus.put(checkBoxReadMapping, statusLabelReadMapping);
		hashMapStatus.put(checkBoxDistances, statusLabelDistances);
		// import folder must be chosen such that check boxes are enabled
		for (JCheckBox checkBox : hashMapStatus.keySet()) {
			checkBox.setEnabled(false);
		}

		// import dump file
		JSeparator separatorImportDump = new JSeparator();
		separatorImportDump.setForeground(Color.BLACK);
		GridBagConstraints gbc_separatorImportDump = new GridBagConstraints();
		gbc_separatorImportDump.fill = GridBagConstraints.BOTH;
		gbc_separatorImportDump.insets = new Insets(5, 0, 5, 0);
		gbc_separatorImportDump.gridx = 0;
		gbc_separatorImportDump.gridy = 12;
		gbc_separatorImportDump.gridwidth = 4;
		panel.add(separatorImportDump, gbc_separatorImportDump);

		JRadioButton radioButtonImportDump = new JRadioButton(rb.getString("text_radioButtonImportDump"));
		radioButtonImportDump.setActionCommand(rb.getString("text_radioButtonImportDump"));
		GridBagConstraints gbc_radioButtonImportDump = new GridBagConstraints();
		gbc_radioButtonImportDump.fill = GridBagConstraints.BOTH;
		gbc_radioButtonImportDump.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonImportDump.gridx = 0;
		gbc_radioButtonImportDump.gridy = 13;
		gbc_radioButtonImportDump.gridwidth = 2;
		panel.add(radioButtonImportDump, gbc_radioButtonImportDump);

		final ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(radioButtonImportFiles);
		buttonGroup.add(radioButtonImportDump);
		radioButtonImportFiles.setSelected(true);

		JButton buttonFileImportDump = new JButton(rb.getString("text_buttonFileImportDump"));
		GridBagConstraints gbc_buttonFileImportDump = new GridBagConstraints();
		gbc_buttonFileImportDump.fill = GridBagConstraints.BOTH;
		gbc_buttonFileImportDump.insets = new Insets(0, 0, 5, 5);
		gbc_buttonFileImportDump.gridx = 2;
		gbc_buttonFileImportDump.gridy = 13;
		panel.add(buttonFileImportDump, gbc_buttonFileImportDump);
		buttonFileImportDump.addActionListener(new ActionListener() {	
			/** opens window to file with sql dump */
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter(rb.getString("filter_sql_zip"), "sql", "zip");
				chooser.setFileFilter(filter);
				if (importFolder!=null) {
					chooser.setCurrentDirectory(new File(importFolder));
				}
				int returnVal = chooser.showOpenDialog(tabbedPane);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					dumpPath = chooser.getSelectedFile().toPath().toAbsolutePath().toString();
				}
			}
		});

		final StatusLabel statusLabelImportDump = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelImportDump = new GridBagConstraints();
		gbc_statusLabelImportDump.fill = GridBagConstraints.BOTH;
		gbc_statusLabelImportDump.insets = new Insets(0, 0, 5, 5);
		gbc_statusLabelImportDump.gridx = 3;
		gbc_statusLabelImportDump.gridy = 13;
		panel.add(statusLabelImportDump, gbc_statusLabelImportDump);


		// buttons for start import and cancel import
		JSeparator separatorStartImport = new JSeparator();
		separatorStartImport.setForeground(Color.BLACK);
		GridBagConstraints gbc_separatorStartImport = new GridBagConstraints();
		gbc_separatorStartImport.fill = GridBagConstraints.BOTH;
		gbc_separatorStartImport.insets = new Insets(5, 0, 5, 0);
		gbc_separatorStartImport.gridx = 0;
		gbc_separatorStartImport.gridy = 14;
		gbc_separatorStartImport.gridwidth = 4;
		panel.add(separatorStartImport, gbc_separatorStartImport);

		JButton buttonStartImport = new JButton(rb.getString("text_buttonStartImport"));
		GridBagConstraints gbc_buttonStartImport = new GridBagConstraints();
		gbc_buttonStartImport.fill = GridBagConstraints.BOTH;
		gbc_buttonStartImport.insets = new Insets(5, 5, 5, 5);
		gbc_buttonStartImport.gridx = 0;
		gbc_buttonStartImport.gridy = 15;
		gbc_buttonStartImport.gridwidth = 2;
		panel.add(buttonStartImport, gbc_buttonStartImport);
		buttonStartImport.addActionListener(new ActionListener() {
			/** Start import process. */
			public void actionPerformed(ActionEvent arg0) {
				killThread = false;
				Thread thread = new Thread(new Runnable() {

					public void run() {

						Database.ensureConnection();
						// import from files
						if (buttonGroup.getSelection().getActionCommand().equals(rb.getString("text_radioButtonImportFiles"))) {
							if (importFolder==null) {
								JOptionPane.showMessageDialog(panel, rb.getString("text_popupMissingImportFolder"), rb.getString("title_popupMissingImportFolder"), JOptionPane.WARNING_MESSAGE);
								return;
							}
							if ((checkBoxPrepareMapping.isSelected()) && (mappingOutputFolder==null)) {
								JOptionPane.showMessageDialog(panel, rb.getString("text_popupMissingPrepareMappingFolder"), rb.getString("title_popupMissingPrepareMappingFolder"), JOptionPane.WARNING_MESSAGE);
								return;
							}
							if ((checkBoxReadMapping.isSelected()) && (importFolder==null)) {
								JOptionPane.showMessageDialog(panel, rb.getString("text_popupMissingReadMappingFolder"), rb.getString("title_popupMissingReadMappingFolder"), JOptionPane.WARNING_MESSAGE);
								return;
							}

							// initialize status labels
							for (Entry<JCheckBox, StatusLabel> entry :hashMapStatus.entrySet()) {
								JCheckBox checkBox = entry.getKey();
								StatusLabel statusLabel = entry.getValue();
								if (checkBox.isSelected()) {
									statusLabel.changeStatus(1);
								}
								else {
									statusLabel.changeStatus(0);
								}
							}


							// creates geographic distance if it does not exists
							validatePresenceOfGeographicDistance();

							// read locations
							if (checkBoxLocations.isSelected()) {
								try {
									statusLabelLocations.changeStatus(2);
									readLocations(importFolder+"locations.csv");
									statusLabelLocations.changeStatus(3);
								}
								catch (IllegalArgumentException | IOException e) {
									statusLabelLocations.changeStatus(4);
									e.printStackTrace();
									JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorImportFiles"), rb.getString("title_popupErrorImportFiles"), JOptionPane.ERROR_MESSAGE);
								}
							}
							if (killThread==true) {
								return;
							}
							// read maps
							if (checkBoxMaps.isSelected()) {
								try {
									statusLabelMaps.changeStatus(2);
									readMaps(importFolder+"maps.csv");
									statusLabelMaps.changeStatus(3);
								}
								catch (IllegalArgumentException | IOException e) {
									statusLabelMaps.changeStatus(4);
									e.printStackTrace();
									JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorImportFiles"), rb.getString("title_popupErrorImportFiles"), JOptionPane.ERROR_MESSAGE);
								}
							}
							if (killThread==true) {
								return;
							}
							// read informant answers
							if (checkBoxInformantAnswers.isSelected()) {
								try {
									statusLabelInformantAnswers.changeStatus(2);
									readInformantAnswers(importFolder+"informant_answers.csv");
									statusLabelInformantAnswers.changeStatus(3);
								}
								catch (IllegalArgumentException | IOException e) {
									statusLabelInformantAnswers.changeStatus(4);
									e.printStackTrace();
									JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorImportFiles"), rb.getString("title_popupErrorImportFiles"), JOptionPane.ERROR_MESSAGE);
								}
							}
							if (killThread==true) {
								return;
							}
							// read border coordinates
							if (checkBoxBorderCoordinates.isSelected()) {
								try {
									statusLabelBorderCoordinates.changeStatus(2);
									readBorderCoordinates(importFolder+"border_coordinates.csv", borderName);
									statusLabelBorderCoordinates.changeStatus(3);
								}
								catch (IllegalArgumentException | IOException e) {
									statusLabelBorderCoordinates.changeStatus(4);
									e.printStackTrace();
									JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorImportFiles"), rb.getString("title_popupErrorImportFiles"), JOptionPane.ERROR_MESSAGE);
								}
							}
							if (killThread==true) {
								return;
							}
							// read groups
							if (checkBoxGroups.isSelected()) {
								try {
									statusLabelGroups.changeStatus(2);
									readGroupsMaps(importFolder+"groups_maps.txt");
									statusLabelGroups.changeStatus(3);
								}
								catch (IllegalArgumentException | IOException e) {
									statusLabelGroups.changeStatus(4);
									e.printStackTrace();
									JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorImportFiles"), rb.getString("title_popupErrorImportFiles"), JOptionPane.ERROR_MESSAGE);
								}
							}
							if (killThread==true) {
								return;
							}
							// read categories
							if (checkBoxCategories.isSelected()) {
								try {
									statusLabelCategories.changeStatus(2);
									readCategories(importFolder+"categories.txt");
									readCategoriesMaps(importFolder+"categories_maps.txt");
									statusLabelCategories.changeStatus(3);
								}
								catch (IllegalArgumentException | IOException e) {
									statusLabelCategories.changeStatus(4);
									e.printStackTrace();
									JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorImportFiles"), rb.getString("title_popupErrorImportFiles"), JOptionPane.ERROR_MESSAGE);
								}
							}
							if (killThread==true) {
								return;
							}
							// prepare variants mappings
							if (checkBoxPrepareMapping.isSelected()) {
								try {
									statusLabelPrepareMapping.changeStatus(2);
									prepareVariantsMappings(mappingOutputFolder);
									statusLabelPrepareMapping.changeStatus(3);
								}
								catch (IllegalArgumentException | IOException e) {
									statusLabelPrepareMapping.changeStatus(4);
									e.printStackTrace();
									JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorImportFiles"), rb.getString("title_popupErrorImportFiles"), JOptionPane.ERROR_MESSAGE);
								}
							}
							if (killThread==true) {
								return;
							}
							// read variants mappings
							if (checkBoxReadMapping.isSelected()) {
								try {
									statusLabelReadMapping.changeStatus(2);
									readVariantsMappings(mappingInputFolder);
									statusLabelReadMapping.changeStatus(3);
								}
								catch (IllegalArgumentException | IOException e) {
									statusLabelReadMapping.changeStatus(4);
									e.printStackTrace();
									JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorImportFiles"), rb.getString("title_popupErrorImportFiles"), JOptionPane.ERROR_MESSAGE);
								}
							}
							if (killThread==true) {
								return;
							}
							// read distances
							if (checkBoxDistances.isSelected()) {
								try {
									statusLabelDistances.changeStatus(2);
									readDistances(importFolder+"distances.csv", distanceName, distanceIdentification);
									statusLabelDistances.changeStatus(3);
								}
								catch (IllegalArgumentException | IOException e) {
									statusLabelDistances.changeStatus(4);
									e.printStackTrace();
									JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorImportFiles"), rb.getString("title_popupErrorImportFiles"), JOptionPane.ERROR_MESSAGE);
								}
							}

						}

						// import from dump
						else {
							if (dumpPath==null) {
								JOptionPane.showMessageDialog(panel, rb.getString("text_popupMissingSQLDump"), rb.getString("title_popupMissingSQLDump"), JOptionPane.WARNING_MESSAGE);
								return;
							}
							// read SQL dump
							try {
								statusLabelImportDump.changeStatus(2);
								
								// always recreate database tables from schema file
								try (Reader reader = new FileReader(String.format(GeoLingGUI.SCHEMA_PATH, dbType))) {
									SQLReader sqlReader = new SQLReader(connection, reader);
									sqlReader.runScript();
								} catch (Exception e) {
									// failed: maybe we do not have the permissions, just try to import anyway
								}
								
								if (dumpPath.endsWith(".zip")) {
									try (ZipFile zipInput = new ZipFile(dumpPath)) {
										SQLReader sqlReader = new SQLReader(connection, zipInput, USED_CHARSET);
										sqlReader.runScript();
									}
								}
								else {
									try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dumpPath), USED_CHARSET))) {
										SQLReader sqlReader = new SQLReader(connection, reader);
										sqlReader.runScript();
									}
								}
								statusLabelImportDump.changeStatus(3);
							} catch (IOException | SQLException e) {
								statusLabelImportDump.changeStatus(4);
								e.printStackTrace();
								JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorImportDump"), rb.getString("title_popupErrorImportDump"), JOptionPane.ERROR_MESSAGE);
							}



						}


					}
				});

				thread.start();

			}
		});


		JButton buttonRebuildTables = new JButton(rb.getString("text_buttonRebuildTables"));
		GridBagConstraints gbc_buttonRebuildTables = new GridBagConstraints();
		gbc_buttonRebuildTables.fill = GridBagConstraints.BOTH;
		gbc_buttonRebuildTables.insets = new Insets(5, 5, 5, 5);
		gbc_buttonRebuildTables.gridx = 2;
		gbc_buttonRebuildTables.gridy = 15;
		panel.add(buttonRebuildTables, gbc_buttonRebuildTables);
		buttonRebuildTables.addActionListener(new ActionListener() {
			/** Drop all tables and create them subsequently. */
			public void actionPerformed(ActionEvent arg0) {
				try (Reader reader = new FileReader(String.format(GeoLingGUI.SCHEMA_PATH, dbType))) {
					SQLReader sqlReader = new SQLReader(connection, reader);
					sqlReader.runScript();
					JOptionPane.showMessageDialog(panel, rb.getString("text_popupSuccessRecreateTables"), rb.getString("title_popupSuccessRecreateTables"), JOptionPane.INFORMATION_MESSAGE);
				}
				catch (SQLException | IOException e) {
					JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorRecreateTables"), rb.getString("title_popupErrorRecreateTables"), JOptionPane.ERROR_MESSAGE);
				}
			}
		});


		JButton buttonCancelImport = new JButton(rb.getString("text_buttonCancelImport"));
		GridBagConstraints gbc_buttonCancelImport = new GridBagConstraints();
		gbc_buttonCancelImport.fill = GridBagConstraints.BOTH;
		gbc_buttonCancelImport.insets = new Insets(5, 5, 5, 5);
		gbc_buttonCancelImport.gridx = 3;
		gbc_buttonCancelImport.gridy = 15;
		panel.add(buttonCancelImport, gbc_buttonCancelImport);
		buttonCancelImport.addActionListener(new ActionListener() {
			/** Cancel import process. */
			public void actionPerformed(ActionEvent arg0) {
				killThread = true;
			}
		});




	}




	/** Validates presence of geographic distance in table <code>distances</code> and creates entry if
	 * geographic distance does not exist.
	 * Prints error message if more than one distance with type <code>geographic</code> exists.
	 */
	private static void validatePresenceOfGeographicDistance() {

		LazyList<Distance> geographicDistances = Distance.find("type = ?", "geographic");
		if (geographicDistances.size()==0) {
			Distance geographicDistance = new Distance();
			geographicDistance.set("name", "Geographical distance");
			geographicDistance.set("type", "geographic");
			geographicDistance.saveIt();
		}
		if (geographicDistances.size()>1) {
			throw new IllegalArgumentException("More than one geographical distance in table distances was found!");
		}
	}


	/**
	 * Reads .csv file and saves information in table <code>locations</code>.
	 * @param filename the path of the .csv file
	 */
	private static void readLocations(String filename) throws IllegalArgumentException, IOException {
		int requiredColumns = 4;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;
	
			// read header, check if enough input columns
			line = br.readLine();
			String[] header = line.split(DELIMITER_COLUMNS);
			// check if enough input columns
			int inputColumns = header.length;
			if (inputColumns<requiredColumns) {
				throw new IllegalArgumentException("Not enough input columns for file: " + filename);
			}
			// look for optional tags and get their names
			int tagColumns = inputColumns-requiredColumns;
			String[] tagNames;
			if (tagColumns>0) {
				tagNames = new String[tagColumns];
				for (int t=0; t<tagColumns; t++) {
					tagNames[t] = header[requiredColumns+t].substring(1, header[requiredColumns+t].length()-1);
				}
			}
			else {
				tagNames = null;
			}
	
			// read all further lines and create one entry for each line
			int countLine = 0;
			while ((line=br.readLine())!=null) {
				countLine++;
				if (line.isEmpty()) continue;
				
				String[] split = line.split(DELIMITER_COLUMNS);
				if (split.length!=inputColumns) {
					throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
				}
				Location location = new Location();
				location.set("name", split[0]);
				location.set("code", split[1]);
				location.set("latitude", split[2]);
				location.set("longitude", split[3]);
				location.saveIt();
				// add optional tags
				for (int t=0; t<tagColumns; t++) {
					if (split[requiredColumns+t].length()>0) {
						Tag tag = new Tag();
						tag.set("name", tagNames[t]);
						tag.set("value", split[requiredColumns+t]);
						location.add(tag);
					}
				}
			}

		}

	}

	/**
	 * Reads .csv file and saves information in table <code>maps</code>.
	 * @param filename the path of the .csv file
	 */
	private static void readMaps(String filename) throws IllegalArgumentException, IOException {
		int requiredColumns = 1;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;
	
			// read header, check if enough input columns
			line = br.readLine();
			String[] header = line.split(DELIMITER_COLUMNS);
			// check if enough input columns
			int inputColumns = header.length;
			if (inputColumns<requiredColumns) {
				throw new IllegalArgumentException("Not enough input columns for file: " + filename);
			}
			// look for optional tags and get their names
			int tagColumns = inputColumns-requiredColumns;
			String[] tagNames;
			if (tagColumns>0) {
				tagNames = new String[tagColumns];
				for (int t=0; t<tagColumns; t++) {
					tagNames[t] = header[requiredColumns+t].substring(1, header[requiredColumns+t].length()-1);
				}
			}
			else {
				tagNames = null;
			}
	
			// read all further lines and create one entry for each line
			int countLine = 0;
			while ((line=br.readLine())!=null) {
				countLine++;
				if (line.isEmpty()) continue;
				
				String[] split = line.split(DELIMITER_COLUMNS);
				if (split.length!=inputColumns) {
					throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
				}
				Map map = new Map();
				map.set("name", split[0]);
				map.saveIt();
				// add optional tags
				for (int t=0; t<tagColumns; t++) {
					if (split[requiredColumns+t].length()>0) {
						Tag tag = new Tag();
						tag.set("name", tagNames[t]);
						tag.set("value", split[requiredColumns+t]);
						map.add(tag);
					}
				}
			}

		}

	}


	/**
	 * Reads .csv file and saves information in the following tables:<br>
	 * - <code>informants</code><br>
	 * - <code>interviewers</code><br>
	 * - <code>interview_answers</code><br>
	 * - <code>variants</code>
	 * @param filename the path of the .csv file
	 */
	private static void readInformantAnswers(String filename) throws IllegalArgumentException, IOException {
		// check if all necessary tables are non-empty
		if (Location.count()==0) {
			throw new IllegalArgumentException("Table Locations must not be empty!");
		}
		if (Map.count()==0) {
			throw new IllegalArgumentException("Table Maps must not be empty!");
		}
		int requiredColumns = 3;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;
	
			// read header, check if enough input columns
			line = br.readLine();
			String[] header = line.split(DELIMITER_COLUMNS);
			// check if enough input columns
			int inputColumns = header.length;
			if (inputColumns<requiredColumns) {
				throw new IllegalArgumentException("Not enough input columns for file: " + filename);
			}
	
			// analyse header
			// look for how many maps answers are given and if tag colums are available
			int nrMaps = 0;
			ArrayList<Object> mapIds = new ArrayList<Object>();
			for (int m=0; m<inputColumns-requiredColumns; m++) {
				// if tag column, quit for loop
				if (header[requiredColumns+m].startsWith("(") && header[requiredColumns+m].endsWith(")")) {
					break;
				}
	
				nrMaps++;
				// check if the names of the maps in the header are contained in table maps and get the id of each map
				LazyList<Map> maps = Map.find("name = ?", header[3+m]);
				if (maps.size()==1) {
					mapIds.add(maps.get(0).getId());
				}
				else {
					if (maps.size()==0) {
						throw new IllegalArgumentException("Name of map is not contained in table maps: " + header[3+m]);
					}
					if (maps.size()>1) {
						throw new IllegalArgumentException("Map name is not unique: " + header[3+m]);
					}
				}
	
			}
			if (nrMaps==0) {
				throw new IllegalArgumentException("At least answers for one map must be given.");
			}
	
			// look for optional tags and get their names
			int tagColumns = inputColumns-requiredColumns-nrMaps;
			String[] tagNames;
			if (tagColumns>0) {
				tagNames = new String[tagColumns];
				for (int t=0; t<tagColumns; t++) {
					tagNames[t] = header[requiredColumns+nrMaps+t].substring(1, header[requiredColumns+nrMaps+t].length()-1);
				}
			}
			else {
				tagNames = null;
			}
	
	
			// put all given answers (=variants) in a cache to avoid too many database queries
			// a HashMap saves Key-Value pairs for unique keys
			// here: Key: the name of the variant, Value: the id of the variant
			List<HashMap<String, Object>> listOfVariants = new ArrayList<HashMap<String, Object>>();
			for (int m=0; m<nrMaps; m++) {
				listOfVariants.add(new HashMap<String, Object>());
			}
	
			// read all further lines and create entries in various tables for each line
			int countLine = 0;
			while ((line=br.readLine())!=null) {
				countLine++;
				if (line.isEmpty()) continue;
				
				String[] split = line.split(DELIMITER_COLUMNS);
				if (split.length!=inputColumns) {
					throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
				}
				// create entry in table informants
				Informant informant = new Informant();
				informant.set("name", split[0]);
				// get id of location and check if location is consistent
				LazyList<Location> locations = Location.find("name = ?", split[1]);
				if (locations.size()==0) {
					throw new IllegalArgumentException("Name of location is not contained in table locations: " + split[1]);
				}
				if (locations.size()>1) {
					throw new IllegalArgumentException("Location name is not unique: " + split[1]);
				}
				informant.set("location_id", locations.get(0).getId());
				informant.saveIt();
				Object informantId = informant.getId();
	
				// add optional tags to informant
				for (int t=0; t<tagColumns; t++) {
					if (split[requiredColumns+nrMaps+t].length()>0) {
						Tag tag = new Tag();
						tag.set("name", tagNames[t]);
						tag.set("value", split[requiredColumns+nrMaps+t]);
						informant.add(tag);
					}
				}
	
				// get id of interviewer
				Object interviewerId;
				// if interviewer of current line is not already in table interviewers
				if (Interviewer.first("name = ?", split[2])==null) {
					// create entry in table interviewers
					Interviewer interviewer = new Interviewer();
					interviewer.set("name", split[2]);
					interviewer.saveIt();
					interviewerId = interviewer.getId();
				}
				else {
					interviewerId = Interviewer.first("name = ?", split[2]).getId();
				}
	
				// iterate over the maps given in the file
				for (int m=0; m<nrMaps; m++) {
					String[] answers = split[3+m].split(DELIMITER_REGEX_MULTIPLE_ANSWERS);
					if (answers.length>0) {
						for (int a=0; a<answers.length; a++) {
							String answer = answers[a];
							if (answer.length()==0) {
								continue;
							}
							Object variantId;
							// if variant is already in cache
							if (listOfVariants.get(m).containsKey(answer)) {
								variantId = listOfVariants.get(m).get(answer);
							}
							else {
								// create entry in table variants
								Variant variant = new Variant();
								variant.set("map_id", mapIds.get(m));
								variant.set("name", answer);
								variant.saveIt();
								variantId = variant.getId();
								listOfVariants.get(m).put(answer, variantId);
							}
	
							// create entry in table interview_answers
							InterviewAnswer interviewAnswer = new InterviewAnswer();
							interviewAnswer.set("interviewer_id", interviewerId);
							interviewAnswer.set("informant_id", informantId);
							interviewAnswer.set("variant_id", variantId);
							interviewAnswer.saveIt();
						}
					}
				}
	
	
	
			}

		}

	}


	/**
	 * Reads .csv file and saves information in table <code>borders</code> and <code>border_coordinates</code>.
	 * @param filename the path of the .csv file
	 * @param borderName the name of the given border
	 */
	private static void readBorderCoordinates(String filename, String borderName) throws IllegalArgumentException, IOException {
		int requiredColumns = 3;
		Border border = new Border();
		border.set("name", borderName);
		border.saveIt();
		Object borderId = border.getId();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;
	
			// read header, check if enough input columns
			line = br.readLine();
			String[] header = line.split(DELIMITER_COLUMNS);
			// check if enough input columns
			int inputColumns = header.length;
			if (inputColumns<requiredColumns) {
				throw new IllegalArgumentException("Not enough input columns for file: " + filename);
			}
			// look for optional tags and get their names
			int tagColumns = inputColumns-requiredColumns;
			String[] tagNames;
			if (tagColumns>0) {
				tagNames = new String[tagColumns];
				for (int t=0; t<tagColumns; t++) {
					tagNames[t] = header[requiredColumns+t].substring(1, header[requiredColumns+t].length()-1);
				}
			}
			else {
				tagNames = null;
			}
	
			// read all further lines and create one entry for each line
			int countLine = 0;
			while ((line=br.readLine())!=null) {
				countLine++;
				if (line.isEmpty()) continue;
				
				String[] split = line.split(DELIMITER_COLUMNS);
				if (split.length!=inputColumns) {
					throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
				}
				BorderCoordinate borderCoordinate = new BorderCoordinate();
				borderCoordinate.set("border_id", borderId);
				borderCoordinate.set("order_index", split[0]);
				borderCoordinate.set("latitude", split[1]);
				borderCoordinate.set("longitude", split[2]);
				borderCoordinate.saveIt();
				// add optional tags
				for (int t=0; t<tagColumns; t++) {
					if (split[requiredColumns+t].length()>0) {
						Tag tag = new Tag();
						tag.set("name", tagNames[t]);
						tag.set("value", split[requiredColumns+t]);
						borderCoordinate.add(tag);
					}
				}
			}

		}

	}


	/**
	 * Reads .txt file and saves information in table <code>groups_maps</code> and <code>groups</code>.
	 * @param filename the path of the .txt file
	 */
	private static void readGroupsMaps(String filename) throws IllegalArgumentException, IOException {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;
	
			// read header
			line = br.readLine();
	
			// read all further lines
			// each line contains the name of a group and a list of the maps that belong to this group
			while ((line=br.readLine())!=null) {
				if (line.isEmpty()) continue;
				
				String[] split = line.split(":");
				if (split.length!=2) {
					throw new IllegalArgumentException("Wrong format in file " + filename);
				}
	
				// create entry in groups
				Group group = new Group();
				group.set("name", split[0]);
				group.saveIt();
	
				// create entries in groups_maps
				String[] mapNames = split[1].split(DELIMITER_COLUMNS);
				for (int m=0; m<mapNames.length; m++) {
					// get id of map
					LazyList<Map> maps = Map.find("name = ?", mapNames[m]);
					Object mapId = null;
					if (maps.size()==1) {
						mapId = maps.get(0).getId();
					}
					else {
						if (maps.size()==0) {
							throw new IllegalArgumentException("Name of map is not contained in table maps: " + mapNames[m]);
						}
						if (maps.size()>0) {
							throw new IllegalArgumentException("Map name is not unique: " + mapNames[m]);
						}
					}
	
	
					GroupsMaps groupsMaps = new GroupsMaps();
					groupsMaps.set("group_id", group.getId());
					groupsMaps.set("map_id", mapId);
					groupsMaps.saveIt();
				}
			}
		}

	}


	/**
	 * Reads .txt file and saves information in table <code>categories</code>.
	 * @param filename the path of the .txt file
	 */
	private static void readCategories(String filename) throws IllegalArgumentException, IOException {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;
	
			// read first line with header
			line = br.readLine();
	
			// read second line with the category that has no parent (=top node)
			int lft = 1;
			if ((line = br.readLine()) != null) {
				String[] parentSplit = line.split(":");
				if (parentSplit.length!=2) {
					throw new IllegalArgumentException("Wrong format in file " + filename);
				}
				// create entry for the top node
				Category topNode = new Category();
				topNode.set("name", parentSplit[0]);
				topNode.set("lft", lft);
				topNode.set("rgt", lft);
				topNode.saveIt();
				lft++;
				// create entries for the children of the top node
				String[] childrenTopNode = parentSplit[1].split(DELIMITER_COLUMNS);
				for (int c=0; c<childrenTopNode.length; c++) {
					Category category = new Category();
					category.set("name", childrenTopNode[c]);
					category.set("parent_id", topNode.getId());
					category.set("lft", lft);
					category.set("rgt", lft);
					category.saveIt();
					lft++;
				}
			}
	
			// read all further categories, their parent category has to be already read
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) continue;
				
				String[] split = line.split(":");
				if (split.length!=2) {
					throw new IllegalArgumentException("Wrong format in file " + filename);
				}
				// get id of parent (=entry in front of ":")
				LazyList<Category> parents = Category.find("name = ?", split[0]);
				Object parentId = null;
				if (parents.size()==1) {
					parentId = parents.get(0).getId();
				}
				else {
					if (parents.size()==0) {
						throw new IllegalArgumentException("Name of category is not contained in table categories: " + split[0]);
					}
					if (parents.size()>0) {
						throw new IllegalArgumentException("Category name is not unique: " + split[0]);
					}
				}
				String[] children = split[1].split(DELIMITER_COLUMNS);
				for (int c=0; c<children.length; c++) {
					Category category = new Category();
					category.set("name", children[c]);
					category.set("parent_id", parentId);
					category.set("lft", lft);
					category.set("rgt", lft);
					category.saveIt();
					lft++;
				}
			}
		}
		
		// rebuild the lft and rgt attributes in the table categories
		Category.rebuildLftRgtAttributes();

	}



	/**
	 * Reads .txt file and saves information in table <code>categories_maps</code>.
	 * @param filename the path of the .txt file
	 */
	private static void readCategoriesMaps(String filename) throws IllegalArgumentException, IOException {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;
	
			// read header
			line = br.readLine();
			
			// read all further lines
			// each line contains the name of a category and a list of the maps that belong to this category
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) continue;
				
				String[] split = line.split(":");
				if (split.length!=2) {
					throw new IllegalArgumentException("Wrong format in file " + filename);
				}
	
				// get id of the category in the current line
				LazyList<Category> categories = Category.find("name = ?", split[0]);
				Object categoryId = null;
				if (categories.size()==1) {
					categoryId = categories.get(0).getId();
				}
				else {
					if (categories.size()==0) {
						throw new IllegalArgumentException("Name of category is not contained in table categories: " + split[0]);
					}
					if (categories.size()>0) {
						throw new IllegalArgumentException("Category name is not unique: " + split[0]);
					}
				}
	
				// create entries in categories_maps
				String[] mapNames = split[1].split(DELIMITER_COLUMNS);
				for (int m=0; m<mapNames.length; m++) {
					// get id of map
					LazyList<Map> maps = Map.find("name = ?", mapNames[m]);
					Object mapId = null;
					if (maps.size()==1) {
						mapId = maps.get(0).getId();
					}
					else {
						if (maps.size()==0) {
							throw new IllegalArgumentException("Name of map is not contained in table maps: " + mapNames[m]);
						}
						if (maps.size()>0) {
							throw new IllegalArgumentException("Map name is not unique: " + mapNames[m]);
						}
					}
	
	
					CategoriesMaps categoriesMaps = new CategoriesMaps();
					categoriesMaps.set("category_id", categoryId);
					categoriesMaps.set("map_id", mapId);
					categoriesMaps.saveIt();
				}
			}
		}

	}





	/**
	 * Writes one .csv file for each map. The name of the file is given by the id of the map.
	 * Each line contains the name of one variant with the count that it was answered.
	 * @param outputFolder the folder to which the .csv files shall be written to
	 */
	private static void prepareVariantsMappings(String outputFolder) throws IllegalArgumentException, IOException {

		Directory.mkdir(outputFolder);

		LazyList<Map> maps = Map.findAll();
		for (Map map : maps) {
			try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFolder+"map_"+map.getId()+".csv"), USED_CHARSET))) {
				bw.write("variant_name;count");
				bw.newLine();
				// get all variants of the current map
				LazyList<Variant> variants = Variant.find("map_id = ?", map.getId());
				for (Variant variant : variants) {
					int count = variant.getAll(InterviewAnswer.class).size();
					bw.write(variant.getString("name") + ";" + count);
					bw.newLine();
				}
			}
		}

	}


	/**
	 * Read one file for each map. The name of the file is given by the id of the map.
	 * Saves information in table <code>levels</code> and <code>variants_mappings</code>.
	 * @param inputFolder the folder which contains the .csv file of variants and their level mapping
	 */
	private static void readVariantsMappings(String inputFolder) throws IllegalArgumentException, IOException {

		LazyList<Map> maps = Map.findAll();
		for (Map map : maps) {
			Object mapId = map.getId();

			// put all given answers (=variants) from the database into a cache to avoid too many database queries
			LazyList<Variant> variantsFromDatabase = Variant.find("map_id = ?", mapId);
			// a HashMap saves Key-Value pairs for unique keys
			// here: Key: the name of the variant, Value: the id of the variant
			HashMap<String, Object> variants = new HashMap<String, Object>();
			for (Variant variant : variantsFromDatabase) {
				variants.put(variant.getString("name"), variant.getId());
			}


			String filename = inputFolder +"map_"+mapId+".csv";
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
				String line;
	
				// read header, check if enough input columns
				line = br.readLine();
				String[] header = line.split(DELIMITER_COLUMNS);
				int inputColumns = header.length;
				if (inputColumns<3) {
					throw new IllegalArgumentException("Not enough input columns for file: " + filename);
				}
	
				int nrLevels = inputColumns-2;
				Object[] levelIds = new Object[nrLevels];
				// add levels of header if they are not already in table level
				for (int l=0; l<nrLevels; l++) {
					if (Level.findFirst("name = ?", header[2+l])==null) {
						Level level = new Level();
						level.set("name", header[2+l]);
						level.saveIt();
						levelIds[l] = level.getId();
					}
					else {
						levelIds[l] = Level.findFirst("name = ?", header[2+l]).getId();
					}
				}
	
				// read all further lines and create entries in table variants_mappings for each line
				int countLine = 0;
				while ((line=br.readLine())!=null) {
					countLine++;
					if (line.isEmpty()) continue;
					
					String[] split = line.split(DELIMITER_COLUMNS);
					if (split.length!=inputColumns) {
						throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
					}
	
					Object variantId;
					if (variants.containsKey(split[0])) {
						variantId = variants.get(split[0]);
					}
					else {
						throw new IllegalArgumentException("Mapping for unknown variant " + split[0] + " in map with id " + mapId + ".");
					}
					for (int l=0; l<nrLevels; l++) {
						String toVariantName = split[2+l];
						Object toVariantId;
						if (variants.containsKey(toVariantName)) {
							toVariantId = variants.get(toVariantName);
						}
						else {
							// create new variant
							Variant variant = new Variant();
							variant.set("name", toVariantName);
							variant.set("map_id", mapId);
							variant.saveIt();
							toVariantId = variant.getId();
							// put it to the hash map of all variants of the current map
							variants.put(toVariantName, toVariantId);
						}
	
						// create entry in table variants_mappings
						VariantsMapping variantsMapping = new VariantsMapping();
						variantsMapping.set("variant_id", variantId);
						variantsMapping.set("level_id", levelIds[l]);
						variantsMapping.set("to_variant_id", toVariantId);
						variantsMapping.saveIt();
					}
	
				}

			}
		}
	}


	/**
	 * Reads .csv file and saves information in table <code>distances</code>.
	 * @param filename the path of the .csv file
	 * @param distanceName the name of the distance which is read
	 * @param distanceIdentification a <code>String</code> identifier for the distance
	 */
	private static void readDistances(String filename, String distanceName, String distanceIdentification) throws IllegalArgumentException, IOException {
		int requiredColumns = 3;

		// create new entry in table distances
		Distance distance = new Distance();
		distance.set("name", distanceName);
		distance.set("type", "precomputed");
		distance.set("identification", distanceIdentification);
		distance.saveIt();
		Object distanceId = distance.getId();

		// fetch all locations to get their ids and put them in a catch to reduce the number of database queries
		LazyList<Location> locations = Location.findAll();
		// a HashMap saves Key-Value pairs for unique keys
		// here: Key: the name of the location, Value: the id of the location
		HashMap<String, Object> locationsMap = new HashMap<String, Object>();
		for (Location location : locations) {
			locationsMap.put(location.getString("name"), location.getId());
		}

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;
	
			// read header, check if enough input columns
			line = br.readLine();
			String[] header = line.split(DELIMITER_COLUMNS);
			// check if enough input columns
			int inputColumns = header.length;
			if (inputColumns<requiredColumns) {
				throw new IllegalArgumentException("Not enough input columns for file: " + filename);
			}
			// look for optional tags and get their names
			int tagColumns = inputColumns-requiredColumns;
			String[] tagNames;
			if (tagColumns>0) {
				tagNames = new String[tagColumns];
				for (int t=0; t<tagColumns; t++) {
					tagNames[t] = header[requiredColumns+t].substring(1, header[requiredColumns+t].length()-1);
				}
			}
			else {
				tagNames = null;
			}
	
			// read all further lines and create one entry for each line
			int countLine = 0;
			while ((line=br.readLine())!=null) {
				countLine++;
				if (line.isEmpty()) continue;
				
				String[] split = line.split(DELIMITER_COLUMNS);
				if (split.length!=inputColumns) {
					throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
				}
				LocationDistance locationDistance = new LocationDistance();
				locationDistance.set("distance_id", distanceId);
				Object locationId1;
				Object locationId2;
				if (locationsMap.containsKey(split[0])) {
					locationId1 = locationsMap.get(split[0]);
				}
				else {
					throw new IllegalArgumentException("Name of location is not contained in table locations: " + split[0]);
	
				}
				if (locationsMap.containsKey(split[1])) {
					locationId2 = locationsMap.get(split[1]);
				}
				else {
					throw new IllegalArgumentException("Name of location is not contained in table locations: " + split[1]);
	
				}
				locationDistance.set("location_id1", locationId1);
				locationDistance.set("location_id2", locationId2);
				locationDistance.set("distance", split[2]);
				locationDistance.saveIt();
	
				// add optional tags
				for (int t=0; t<tagColumns; t++) {
					if (split[requiredColumns+t].length()>0) {
						Tag tag = new Tag();
						tag.set("name", tagNames[t]);
						tag.set("value", split[requiredColumns+t]);
						locationDistance.add(tag);
					}
				}
			}

		}

	}




}
