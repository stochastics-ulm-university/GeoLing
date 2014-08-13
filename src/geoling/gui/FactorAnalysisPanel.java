package geoling.gui;

import geoling.config.Database;
import geoling.factor_analysis.FactorAnalysis;
import geoling.factor_analysis.util.FactorLoadings;
import geoling.gui.util.ColorHueTableCellRenderer;
import geoling.gui.util.ComboBoxGroupElement;
import geoling.gui.util.ComboBoxLevelElement;
import geoling.gui.util.AreaClassMapLabel;
import geoling.gui.util.JFileChooserConfirmOverwrite;
import geoling.gui.util.TableVariantElement;
import geoling.locations.util.AggregatedLocation;
import geoling.maps.AreaClassMap;
import geoling.maps.VariantMap;
import geoling.maps.density.DensityEstimation;
import geoling.maps.density.WeightPassthrough;
import geoling.maps.plot.PlotAreaClassMap;
import geoling.maps.plot.PlotHelper;
import geoling.maps.plot.PlotVariantMap;
import geoling.maps.projection.MapProjection;
import geoling.maps.projection.MercatorProjection;
import geoling.maps.weights.VariantWeights;
import geoling.maps.weights.VariantWeightsWithLevel;
import geoling.models.Border;
import geoling.models.Group;
import geoling.models.Level;
import geoling.models.Map;
import geoling.models.Variant;
import geoling.util.ProgressOutput;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.util.plot.PlotToEPS;
import geoling.util.sim.util.plot.PlotToGraphics2D;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Image;

import javax.swing.JScrollPane;

import java.awt.GridBagConstraints;

import javax.swing.JLabel;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ProgressMonitor;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.javalite.activejdbc.LazyList;

import javax.swing.JSlider;

/**
 * Panel for factor analysis in dialectometry GUI.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class FactorAnalysisPanel {


	/** <code>JPanel</code> for the contents. */
	private JPanel panelFactorAnalysis;
	private JScrollPane scrollPaneForLabelMap;
	private AreaClassMapLabel labelMap;
	private ImageIcon iconMap;

	/** <code>JComboBox</code> for choosing <code>Level</code>. */
	private JComboBox<ComboBoxLevelElement> comboBoxLevel;
	/** <code>JComboBox</code> for choosing <code>Group</code>. */
	private JComboBox<ComboBoxGroupElement> comboBoxGroup;
	/** This <code>Group</code> will be researched <code>FactorAnalysis</code>. */
	private Group selectedGroup;

	/** If this <code>JRadioButton</code> is selected, the number of factors will be automatically computed. */
	private JRadioButton radioButtonKaiser;
	/** This <code>JTextField</code> is used for inserting own number of factors. */
	private JTextField textField;

	/**
	 * The <code>FactorAnalysis</code> object with results that has been generated
	 * last.
	 */
	private FactorAnalysis factorAnalysis = null;
	/**
	 * <code>AreaClassMap</code> filled with <code>FactorLoadings</code> instead
	 * of <code>VariantWeights</code>.
	 */
	private AreaClassMap areaClassMap;
	/**
	 * This class is necessary to define width, height and other properties of
	 * the plot of area class map.
	 */
	private PlotHelper helper;
	/**
	 * This <code>HashMap</code> is used for showing locations on the drawn map.
	 */
	private HashMap<Polytope, AggregatedLocation> hints = new HashMap<Polytope, AggregatedLocation>();
	/**
	 * In the first column of this <code>JTable</code> the dominant factors of
	 * this <code>AreaClassMap</code> are listed and in the second column the
	 * corresponding RGB-color value of the dominant factors are shown. Requires
	 * <code>TableVariantElement</code> and <code>ColorTableCellRenderer</code>.
	 * 
	 * @see TableVariantElement
	 * @see ColorHueTableCellRenderer
	 */
	private JTable tableFactorColor;
	/** The currently selected row in <i>tableFactor</color> */
	private int tableColumn;
	/**
	 * Variants are saved in the first column of this two-dimensional array and
	 * their corresponding RGB-colors are saved in the second column.
	 */
	private Object[][] tableContentsColor;
	/**
	 * This <code>HashMap</code> provides the RGB-color for one of the dominant
	 * variants/factors.
	 */
	private HashMap<Variant, Color> variantColors;
	/**
	 * This <code>Variant</code> represents a factor and is used to draw a
	 * <code>VariantMap</code>.
	 */
	private Variant selectedVariant;
	/**
	 * <code>true</code>, if all factors are drawn, <code>false</code> if only
	 * one factor is drawn. Necessary for saving map in a file.
	 */
	private boolean classmap;

	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;

	/** Object to remember the group previously used to build the contents of variable <code>factorAnalysis</code>. */
	private Group prevSelectedGroup = null;
	/** Object to remember the level previously used to build the contents of variable <code>factorAnalysis</code>. */
	private Level prevSelectedLevel = null;
	/** Object to remember the number of factors previously used to build the contents of variable <code>factorAnalysis</code>. */
	private Integer prevNumberOfFactors = null;

	/**
	 * Create a panel for drawing a map with factor loadings.
	 * 
	 * @param tabbedPane
	 *            To this <code>JTabbedPane</code> the new panel is added.
	 * @param outputfolder
	 *            Basis folder for output, defined in a property file.
	 */
	public FactorAnalysisPanel(final JTabbedPane tabbedPane, final String outputfolder) {

		rb = ResourceBundle.getBundle("FactorAnalysisPanel", GeoLingGUI.LANGUAGE);

		panelFactorAnalysis = new JPanel();
		tabbedPane.addTab(rb.getString("title_FactorAnalysisPanel"), null, panelFactorAnalysis, null);

		tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);

		tabbedPane.addChangeListener(new ChangeListener() {

			// This method is called whenever the selected tab changes (groups may have changed)
			public void stateChanged(ChangeEvent evt) {
				LazyList<Group> groups = Group.findAll();
				ComboBoxGroupElement[] groupElements = new ComboBoxGroupElement[groups.size()];
				int oldIndex = -1;
				for (int i = 0; i < groupElements.length; i++) {
					Group group = groups.get(i);
					groupElements[i] = new ComboBoxGroupElement(group);
					if (selectedGroup!=null && selectedGroup.equals(group)) {
						oldIndex = i;
					}
				}
				comboBoxGroup.setModel(new DefaultComboBoxModel<ComboBoxGroupElement>(groupElements));

				if (groupElements.length > 0) {
					// if selected group does not exist anymore
					if (oldIndex==-1) {
						selectedGroup = groupElements[0].getGroup();
						comboBoxGroup.setSelectedIndex(0);
					}
					else {
						comboBoxGroup.setSelectedIndex(oldIndex);
					}
				}

			}
		});

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 350, 50, 50, 50, 0 };
		gridBagLayout.rowHeights = new int[] { 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 0 };
		gridBagLayout.columnWeights = new double[] { 0.75, 0.05, 0.1, 0.1, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.05, 0.05, 0.05, 0.05, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12, Double.MIN_VALUE };
		panelFactorAnalysis.setLayout(gridBagLayout);

		final Polytope borderPolygon = Border.getDefaultBorder().toPolygon();
		final MapProjection mapProjection = new MercatorProjection();

		scrollPaneForLabelMap = new JScrollPane();
		GridBagConstraints gbc_scrollPaneForLabelMap = new GridBagConstraints();
		gbc_scrollPaneForLabelMap.gridheight = 9;
		gbc_scrollPaneForLabelMap.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPaneForLabelMap.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneForLabelMap.gridx = 0;
		gbc_scrollPaneForLabelMap.gridy = 0;
		panelFactorAnalysis.add(scrollPaneForLabelMap, gbc_scrollPaneForLabelMap);

		final JSlider sliderZoom = new JSlider(0, 500, 100);
		sliderZoom.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					if (source.getValue()<5) {
						sliderZoom.setValue(5);
					}
					else {
						scrollPaneForLabelMap.remove(labelMap);
						final float scaleFactor = (float) (source.getValue() / 100.0);
						Image original = iconMap.getImage();
						Image scaled = original.getScaledInstance((int) (iconMap.getIconWidth() * scaleFactor), (int) (iconMap.getIconHeight() * scaleFactor),
								Image.SCALE_SMOOTH);
						final ImageIcon iconMapScaled = new ImageIcon(scaled);
						labelMap = new AreaClassMapLabel(iconMapScaled, hints, areaClassMap, scaleFactor);
						ToolTipManager.sharedInstance().registerComponent(labelMap);
						scrollPaneForLabelMap.setViewportView(labelMap);
					}
				}
			}
		});
		sliderZoom.setMajorTickSpacing(50);
		sliderZoom.setPaintTicks(true);
		sliderZoom.setPaintLabels(true);
		GridBagConstraints gbc_sliderZoom = new GridBagConstraints();
		gbc_sliderZoom.fill = GridBagConstraints.BOTH;
		gbc_sliderZoom.insets = new Insets(0, 0, 0, 5);
		gbc_sliderZoom.gridx = 0;
		gbc_sliderZoom.gridy = 9;
		panelFactorAnalysis.add(sliderZoom, gbc_sliderZoom);

		// label and combo box to select level
		JLabel lblLevel = new JLabel(rb.getString("text_lblLevel"));
		GridBagConstraints gbc_lblLevel = new GridBagConstraints();
		gbc_lblLevel.fill = GridBagConstraints.BOTH;
		gbc_lblLevel.insets = new Insets(0, 0, 5, 5);
		gbc_lblLevel.anchor = GridBagConstraints.EAST;
		gbc_lblLevel.gridx = 1;
		gbc_lblLevel.gridy = 0;
		panelFactorAnalysis.add(lblLevel, gbc_lblLevel);

		comboBoxLevel = new JComboBox<ComboBoxLevelElement>();
		LazyList<Level> levels = Level.findAll();
		ComboBoxLevelElement[] levelElements = new ComboBoxLevelElement[levels.size()];
		for (int i = 0; i < levels.size(); i++) {
			levelElements[i] = new ComboBoxLevelElement(levels.get(i));
		}
		comboBoxLevel.setModel(new DefaultComboBoxModel<ComboBoxLevelElement>(levelElements));
		// first initialization of selectedLevel
		if (comboBoxLevel.getItemCount() > 0) {
			comboBoxLevel.setSelectedIndex(0);
		}
		comboBoxLevel.setPrototypeDisplayValue(new ComboBoxLevelElement(null));
		GridBagConstraints gbc_comboBoxLevel = new GridBagConstraints();
		gbc_comboBoxLevel.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxLevel.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxLevel.gridx = 2;
		gbc_comboBoxLevel.gridy = 0;
		gbc_comboBoxLevel.gridwidth = 2;
		panelFactorAnalysis.add(comboBoxLevel, gbc_comboBoxLevel);


		// label and combo box to select group
		JLabel lblGroup = new JLabel(rb.getString("text_lblGroup"));
		GridBagConstraints gbc_lblGroup = new GridBagConstraints();
		gbc_lblGroup.fill = GridBagConstraints.BOTH;
		gbc_lblGroup.anchor = GridBagConstraints.EAST;
		gbc_lblGroup.insets = new Insets(0, 0, 5, 5);
		gbc_lblGroup.gridx = 1;
		gbc_lblGroup.gridy = 1;
		panelFactorAnalysis.add(lblGroup, gbc_lblGroup);

		comboBoxGroup = new JComboBox<ComboBoxGroupElement>();
		LazyList<Group> groups = Group.findAll();
		ComboBoxGroupElement[] groupElements = new ComboBoxGroupElement[groups.size()];
		for (int i = 0; i < groups.size(); i++) {
			groupElements[i] = new ComboBoxGroupElement(groups.get(i));
		}
		comboBoxGroup.setModel(new DefaultComboBoxModel<ComboBoxGroupElement>(groupElements));
		// first initialization of selectedGroup
		if (comboBoxGroup.getItemCount() > 0) {
			comboBoxGroup.setSelectedIndex(0);
		}
		comboBoxGroup.setPrototypeDisplayValue(new ComboBoxGroupElement(null));
		GridBagConstraints gbc_comboBoxGroup = new GridBagConstraints();
		gbc_comboBoxGroup.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxGroup.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxGroup.gridx = 2;
		gbc_comboBoxGroup.gridy = 1;
		gbc_comboBoxGroup.gridwidth = 2;
		panelFactorAnalysis.add(comboBoxGroup, gbc_comboBoxGroup);


		// label, text fields and radio buttons to choose number of factors
		JLabel lblFactornumber = new JLabel(rb.getString("text_lblFactornumber"));
		GridBagConstraints gbc_lblFactornumber = new GridBagConstraints();
		gbc_lblFactornumber.fill = GridBagConstraints.BOTH;
		gbc_lblFactornumber.insets = new Insets(0, 0, 5, 5);
		gbc_lblFactornumber.gridx = 1;
		gbc_lblFactornumber.gridy = 2;
		panelFactorAnalysis.add(lblFactornumber, gbc_lblFactornumber);

		ButtonGroup buttonGroupNumber = new ButtonGroup();

		radioButtonKaiser = new JRadioButton(rb.getString("text_radioButtonKaiser"));
		radioButtonKaiser.setSelected(true);
		GridBagConstraints gbc_radioButtonKaiser = new GridBagConstraints();
		gbc_radioButtonKaiser.fill = GridBagConstraints.BOTH;
		gbc_radioButtonKaiser.insets = new Insets(0, 0, 5, 0);
		gbc_radioButtonKaiser.gridx = 2;
		gbc_radioButtonKaiser.gridy = 2;
		panelFactorAnalysis.add(radioButtonKaiser, gbc_radioButtonKaiser);
		buttonGroupNumber.add(radioButtonKaiser);

		textField = new JTextField();
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 3;
		gbc_textField.gridy = 3;
		panelFactorAnalysis.add(textField, gbc_textField);
		textField.setColumns(8);

		JRadioButton radioButtonManual = new JRadioButton(rb.getString("text_radioButtonManual"));
		GridBagConstraints gbc_radioButtonManual = new GridBagConstraints();
		gbc_radioButtonManual.insets = new Insets(0, 0, 5, 0);
		gbc_radioButtonManual.fill = GridBagConstraints.BOTH;
		gbc_radioButtonManual.gridx = 2;
		gbc_radioButtonManual.gridy = 3;
		panelFactorAnalysis.add(radioButtonManual, gbc_radioButtonManual);
		buttonGroupNumber.add(radioButtonManual);



		JButton buttonDrawMap = new JButton(rb.getString("text_buttonDrawMap"));
		buttonDrawMap.addActionListener(new ActionListener() {

			/**
			 * @param arg0
			 *            Press Button
			 * @return Draws map in <code>LabelMap</code>.
			 */
			public void actionPerformed(ActionEvent arg0) {

				final Level selectedLevel = ((ComboBoxLevelElement) comboBoxLevel.getSelectedItem()).getLevel();			
				selectedGroup = ((ComboBoxGroupElement) comboBoxGroup.getSelectedItem()).getGroup();
				final Integer numberOfFactors;
				if (radioButtonKaiser.isSelected()) {
					numberOfFactors = null;
				}
				else {
					try {
						numberOfFactors = Integer.parseInt(textField.getText());
					}
					catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(panelFactorAnalysis, rb.getString("text_popupWrongFormat")
								, rb.getString("title_popupWrongFormat"), JOptionPane.WARNING_MESSAGE);
						return;
					}
				}

				if (selectedLevel != null && selectedGroup != null) {
					Thread thread = new Thread(new Runnable() {

						public void run() {
							// database connection is missing in this worker
							// thread...
							Database.ensureConnection();
							
							ProgressMonitor pm = null;

							if (selectedLevel.equals(prevSelectedLevel) && selectedGroup.equals(prevSelectedGroup) &&
							    ((numberOfFactors == null && prevNumberOfFactors == null) ||
							     (numberOfFactors != null && numberOfFactors.equals(prevNumberOfFactors)))) {
								// old objects are up-to-date
							} else {
								LazyList<Map> maps = selectedGroup.getAll(Map.class);

								pm = new ProgressMonitor(panelFactorAnalysis, rb.getString("text_factorAnalysisRunning"), "", 0, maps.size()*11/10);
								pm.setNote(String.format(rb.getString("format_text_loadingData"), maps.size()));

								ArrayList<VariantWeights> variantWeightsList = new ArrayList<VariantWeights>(maps.size());
								int i = 0;
								for (Map map : maps) {
									pm.setProgress(i++);

									variantWeightsList.add(new VariantWeightsWithLevel(map, selectedLevel));

									if (pm.isCanceled()) {
										return;
									}
								}

								FactorAnalysis factorAnalysisLocal = new FactorAnalysis(variantWeightsList, numberOfFactors);
								pm.setProgress(pm.getMaximum());

								if (pm.isCanceled()) {
									return;
								}

								pm.setProgress(pm.getMinimum());
								pm.setNote(String.format(rb.getString("format_text_calculate"), factorAnalysisLocal.getDataSize(), factorAnalysisLocal.getVarSize()));

								factorAnalysisLocal.calculateFactorLoadings(new ProgressOutput(null, pm, 1, false, 100));

								if (pm.isCanceled()) {
									return;
								}

								pm.setProgress(pm.getMinimum());
								pm.setNote(String.format(rb.getString("format_text_visualization"), factorAnalysisLocal.getNumberOfFactors()));

								factorAnalysis = factorAnalysisLocal;
								prevSelectedLevel = selectedLevel;
								prevSelectedGroup = selectedGroup;
								prevNumberOfFactors = numberOfFactors;
							}
							
							FactorLoadings factorLoadings = factorAnalysis.getFactorLoadingsObj();

							DensityEstimation densityEstimation = new WeightPassthrough();
							areaClassMap = new AreaClassMap(factorLoadings, densityEstimation);
							Polytope borderPolygon = Border.getDefaultBorder().toPolygon();
							MapProjection mapProjection = new MercatorProjection();
							areaClassMap.buildLocationDensityCache();
							areaClassMap.buildAreas(borderPolygon, mapProjection);
							
							if (pm != null) pm.setProgress(pm.getMaximum()/2);

							int height = scrollPaneForLabelMap.getSize().height - 25;
							helper = new PlotHelper(borderPolygon, mapProjection, height, 10);
							PlotAreaClassMap plot = new PlotAreaClassMap(areaClassMap);
							classmap = true;

							variantColors = MapPanel.getUpdatedVariantColors(variantColors, plot.getDefaultAreaColors(false), areaClassMap);

							BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);
							try(PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
								plot.voronoiExport(gre, helper, variantColors, hints);
							}
							if (pm != null) pm.setProgress(pm.getMaximum());

							iconMap = new ImageIcon(bi);
							labelMap = new AreaClassMapLabel(iconMap, hints, areaClassMap, 1.0f);
							ToolTipManager.sharedInstance().registerComponent(labelMap);
							scrollPaneForLabelMap.getViewport().setView(labelMap);

							tableContentsColor = new Object[variantColors.size()][2];
							int variantIndex = 0;
							for (Variant variant : variantColors.keySet()) {
								tableContentsColor[variantIndex][0] = new TableVariantElement(variant, 60);
								tableContentsColor[variantIndex][1] = variantColors.get(variant);
								variantIndex++;
							}
							tableFactorColor.setModel(new DefaultTableModel(tableContentsColor, new String[] { rb.getString("columnName1_tableFactorColor"),
									rb.getString("columnName2_tableFactorColor") }) {

								private static final long serialVersionUID = 1L;

								public boolean isCellEditable(int rowIndex, int columnIndex) {
									return false;
								}
							});
							ColorHueTableCellRenderer ctr = new ColorHueTableCellRenderer();
							tableFactorColor.getColumnModel().getColumn(1).setCellRenderer(ctr);
							// set slider for zoom on 100 percent
							sliderZoom.setValue(100);
						}
					});

					thread.start();
				}
			}
		});
		buttonDrawMap.setToolTipText(rb.getString("tooltip_buttonDrawMap"));
		GridBagConstraints gbc_buttonDrawMap = new GridBagConstraints();
		gbc_buttonDrawMap.fill = GridBagConstraints.BOTH;
		gbc_buttonDrawMap.insets = new Insets(0, 0, 5, 5);
		gbc_buttonDrawMap.gridx = 1;
		gbc_buttonDrawMap.gridy = 4;
		panelFactorAnalysis.add(buttonDrawMap, gbc_buttonDrawMap);


		JScrollPane scrollPaneForTableFactorColor = new JScrollPane();
		GridBagConstraints gbc_scrollPaneForTableFactorColor = new GridBagConstraints();
		gbc_scrollPaneForTableFactorColor.gridwidth = 2;
		gbc_scrollPaneForTableFactorColor.gridheight = 6;
		gbc_scrollPaneForTableFactorColor.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneForTableFactorColor.gridx = 2;
		gbc_scrollPaneForTableFactorColor.gridy = 4;
		panelFactorAnalysis.add(scrollPaneForTableFactorColor, gbc_scrollPaneForTableFactorColor);


		tableFactorColor = new JTable();
		tableFactorColor.setModel(new DefaultTableModel(new Object[][] { { null, null }, }, new String[] { rb.getString("columnName1_tableFactorColor"),
				rb.getString("columnName2_tableFactorColor") }));
		scrollPaneForTableFactorColor.setViewportView(tableFactorColor);

		tableFactorColor.addMouseListener(new MouseAdapter() {

			/**
			 * ActionListener for mouse click on the table with dominant
			 * variants and their corresponding colors
			 * 
			 * @param arg0
			 *            double mouse click
			 * @return user can changes the color of a variant and after that
			 *         the map is repainted
			 */
			public void mouseClicked(MouseEvent arg0) {
				int row = tableFactorColor.getSelectedRow();
				tableColumn = tableFactorColor.getSelectedColumn();
				if (tableContentsColor[row][0] instanceof TableVariantElement) {
					Variant variant = ((TableVariantElement) tableContentsColor[row][0]).getVariant();
					selectedVariant = variant;
					if (arg0.getClickCount() == 2) {
						Color newColor = JColorChooser.showDialog(null, rb.getString("message_colorChooser"), null);
						if (newColor != null) {
							newColor = Color.getHSBColor(Color.RGBtoHSB(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), null)[0], PlotAreaClassMap.DEFAULT_SATURATION, PlotAreaClassMap.DEFAULT_BRIGHTNESS);
							variantColors.put(variant, newColor);
							tableContentsColor[row][1] = newColor;
							tableFactorColor.setModel(new DefaultTableModel(tableContentsColor, new String[] { rb.getString("columnName1_tableFactorColor"),
									rb.getString("columnName2_tableFactorColor") }) {

								private static final long serialVersionUID = 1L;

								public boolean isCellEditable(int rowIndex, int columnIndex) {
									return false;
								}
							});
							ColorHueTableCellRenderer ctr = new ColorHueTableCellRenderer();
							tableFactorColor.getColumnModel().getColumn(1).setCellRenderer(ctr);
							// draws area class map with new color of variant
							PlotAreaClassMap plot = new PlotAreaClassMap(areaClassMap);
							classmap = true;
							BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);
							try(PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
								plot.voronoiExport(gre, helper, variantColors, hints);
							}
							iconMap = new ImageIcon(bi);
							labelMap = new AreaClassMapLabel(iconMap, hints, areaClassMap, 1.0f);
							ToolTipManager.sharedInstance().registerComponent(labelMap);
							scrollPaneForLabelMap.getViewport().setView(labelMap);
							// set slider for zoom on 100 percent
							sliderZoom.setValue(100);
						}
					}
				} else {
					System.err.println("tableContentsColor has no TableVariantElement");
				}

			}
		});


		JButton buttonShowFactor = new JButton(rb.getString("text_buttonShowFactor"));
		buttonShowFactor.addActionListener(new ActionListener() {

			/**
			 * ActionListener for pressing button to draw map for only one
			 * factor.
			 * 
			 * @param arg0
			 *            Press Button
			 * @return The selected variant/factor in the table is drawn
			 *         left-hand. If a cell in right column is selected, the
			 *         dominance of variant will be drawn in the color, which is
			 *         used in area class map. If a cell in left column is
			 *         selected, the dominance of variant will be drawn in blue.
			 */
			public void actionPerformed(ActionEvent arg0) {
				if (selectedVariant != null) {
					classmap = false;
					VariantMap variantMap = areaClassMap.getVariantMaps().get(selectedVariant);
					PlotVariantMap plot = new PlotVariantMap(variantMap, borderPolygon, mapProjection);
					int height = scrollPaneForLabelMap.getSize().height - 25;
					helper = new PlotHelper(borderPolygon, mapProjection, height, 10);
					BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);
					try (PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
						if (tableColumn == 0) { // use default color for variantMap
							plot.voronoiExport(gre, helper, null, hints);
						} else {
							Color color = variantColors.get(selectedVariant);
							plot.voronoiExport(gre, helper, color, hints);
						}
					}
					iconMap = new ImageIcon(bi);
					labelMap = new AreaClassMapLabel(iconMap, hints, areaClassMap, 1.0f);
					ToolTipManager.sharedInstance().registerComponent(labelMap);
					scrollPaneForLabelMap.getViewport().setView(labelMap);
					// set slider for zoom on 100 percent
					sliderZoom.setValue(100);
				}
			}
		});
		buttonShowFactor.setToolTipText(rb.getString("tooltip_buttonShowFactor"));
		GridBagConstraints gbc_buttonShowFactor = new GridBagConstraints();
		gbc_buttonShowFactor.fill = GridBagConstraints.BOTH;
		gbc_buttonShowFactor.insets = new Insets(0, 0, 5, 5);
		gbc_buttonShowFactor.gridx = 1;
		gbc_buttonShowFactor.gridy = 5;
		panelFactorAnalysis.add(buttonShowFactor, gbc_buttonShowFactor);


		JButton buttonSaveMap = new JButton(rb.getString("text_buttonSaveMap"));
		buttonSaveMap.addActionListener(new ActionListener() {

			/**
			 * ActionListener for pressing button to save a areaclassmap or a
			 * variantmap in file.
			 * 
			 * @param arg0
			 *            Press Button
			 * @return Map is saved in file with default name
			 */
			public void actionPerformed(ActionEvent arg0) {
				if (areaClassMap == null) {
					return;
				}
				if (classmap) {
					String groupName = selectedGroup.getString("name");
					groupName = groupName.substring(0, Math.min(groupName.length(), 20));
					groupName = groupName.replaceAll(" ", "_").replaceAll("[\\\\/:*?\"<>|]", "");

					JFileChooser chooser = new JFileChooserConfirmOverwrite(outputfolder + "/dominant_factors_" + groupName + ".eps");
					chooser.setFileFilter(new FileNameExtensionFilter(rb.getString("filter_xml_eps_png"), "xml", "eps", "png"));

					if (chooser.showSaveDialog(tabbedPane) == JFileChooser.APPROVE_OPTION) {
						try {
							if (chooser.getSelectedFile().getAbsolutePath().toLowerCase().endsWith(".xml")) {
								areaClassMap.toXML(chooser.getSelectedFile().getAbsolutePath(), false);
							} else {
								PlotHelper localHelper = new PlotHelper(borderPolygon, mapProjection);
								PlotAreaClassMap plot = new PlotAreaClassMap(areaClassMap);

								if (chooser.getSelectedFile().getAbsolutePath().toLowerCase().endsWith(".png")) {
									BufferedImage bi = new BufferedImage(localHelper.getWidth(), localHelper.getHeight(), BufferedImage.TYPE_INT_RGB);
									try (PlotToGraphics2D gre = new PlotToGraphics2D(localHelper.getWindow(), bi.createGraphics())) {
										plot.voronoiExport(gre, localHelper, variantColors, null);
									}
									ImageIO.write(bi, "png", new File(chooser.getSelectedFile().getAbsolutePath()));
								} else {
									try (PlotToEPS eps = new PlotToEPS(localHelper.getWindow(), new FileOutputStream(chooser.getSelectedFile().getAbsolutePath()))) {
										plot.voronoiExport(eps, localHelper, variantColors, null);
									}
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
							JOptionPane.showMessageDialog(panelFactorAnalysis, rb.getString("text_popupSavingError")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupSavingError"), JOptionPane.ERROR_MESSAGE);
						}
					}
				} else {
					String groupName = selectedGroup.getString("name");
					groupName = groupName.substring(0, Math.min(groupName.length(), 20));
					groupName = groupName + "_" + selectedVariant.getString("name");
					groupName = groupName.replaceAll(" ", "_").replaceAll("[\\\\/:*?\"<>|]", "");

					JFileChooser chooser = new JFileChooserConfirmOverwrite(outputfolder + "/factor_loadings_" + groupName + ".eps");
					FileNameExtensionFilter filter = new FileNameExtensionFilter(rb.getString("filter_eps_png"), "eps", "png");
					chooser.setFileFilter(filter);

					if (chooser.showSaveDialog(tabbedPane) == JFileChooser.APPROVE_OPTION) {
						try {
							PlotHelper localHelper = new PlotHelper(borderPolygon, mapProjection);
							PlotVariantMap plot = new PlotVariantMap(areaClassMap.getVariantMaps().get(selectedVariant), borderPolygon, mapProjection);
							
							if (chooser.getSelectedFile().getAbsolutePath().toLowerCase().endsWith(".png")) {
								BufferedImage bi = new BufferedImage(localHelper.getWidth(), localHelper.getHeight(), BufferedImage.TYPE_INT_RGB);
								try (PlotToGraphics2D gre = new PlotToGraphics2D(localHelper.getWindow(), bi.createGraphics())) {
									if (tableColumn == 0) {
										plot.voronoiExport(gre, localHelper, null, null);
									} else {
										plot.voronoiExport(gre, localHelper, variantColors.get(selectedVariant), null);
									}
								}
								ImageIO.write(bi, "png", new File(chooser.getSelectedFile().getAbsolutePath()));
							} else {
								try (PlotToEPS eps = new PlotToEPS(localHelper.getWindow(), new FileOutputStream(chooser.getSelectedFile().getAbsolutePath()))) {
									if (tableColumn == 0) {
										plot.voronoiExport(eps, localHelper, null, null);
									} else {
										plot.voronoiExport(eps, localHelper, variantColors.get(selectedVariant), null);
									}
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
							JOptionPane.showMessageDialog(panelFactorAnalysis, rb.getString("text_popupSavingError")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupSavingError"), JOptionPane.ERROR_MESSAGE);
						}	
					}
				}

			}
		});
		buttonSaveMap.setToolTipText(rb.getString("tooltip_buttonSaveMap"));
		GridBagConstraints gbc_buttonSaveMap = new GridBagConstraints();
		gbc_buttonSaveMap.fill = GridBagConstraints.BOTH;
		gbc_buttonSaveMap.insets = new Insets(0, 0, 5, 5);
		gbc_buttonSaveMap.gridx = 1;
		gbc_buttonSaveMap.gridy = 6;
		panelFactorAnalysis.add(buttonSaveMap, gbc_buttonSaveMap);


		JButton buttonSaveFA = new JButton(rb.getString("text_buttonSaveFA"));
		buttonSaveFA.setToolTipText(rb.getString("tooltip_buttonSaveFA"));
		buttonSaveFA.addActionListener(new ActionListener() {

			/**
			 * ActionListener for pressing button to save a factor analysis.
			 * 
			 * @param arg0
			 *            Press Button
			 * @return factor analysis is saved in file with default name
			 */
			public void actionPerformed(ActionEvent arg0) {
				if (factorAnalysis == null) {
					return;
				}
				String groupName = selectedGroup.getString("name");
				groupName = groupName.substring(0, Math.min(groupName.length(), 20));
				groupName = groupName.replaceAll(" ", "_").replaceAll("[\\\\/:*?\"<>|]", "");

				JFileChooser chooser = new JFileChooserConfirmOverwrite(outputfolder + "/factor_analysis_" + groupName + ".xml");
				chooser.setFileFilter(new FileNameExtensionFilter(rb.getString("filter_xml"), "xml"));

				if (chooser.showSaveDialog(tabbedPane) == JFileChooser.APPROVE_OPTION) {
					try {
						factorAnalysis.toXML(chooser.getSelectedFile().getAbsolutePath());
					} catch (IOException e) {
						e.printStackTrace();
						JOptionPane.showMessageDialog(panelFactorAnalysis, rb.getString("text_popupXMLError")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupXMLError"), JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		GridBagConstraints gbc_buttonSaveFA = new GridBagConstraints();
		gbc_buttonSaveFA.fill = GridBagConstraints.BOTH;
		gbc_buttonSaveFA.insets = new Insets(0, 0, 5, 5);
		gbc_buttonSaveFA.gridx = 1;
		gbc_buttonSaveFA.gridy = 7;
		panelFactorAnalysis.add(buttonSaveFA, gbc_buttonSaveFA);


		JButton buttonReconstruction = new JButton(rb.getString("text_buttonReconstruction"));
		buttonReconstruction.setToolTipText(rb.getString("tooltip_buttonReconstruction"));
		buttonReconstruction.addActionListener(new ActionListener() {		
			/** ActionListener for pressing button to reconstruct area class map. */
			public void actionPerformed(ActionEvent arg0) {
				if (factorAnalysis == null) {
					return;
				}
				new FactorWeightsVisualizationPanel(tabbedPane, outputfolder, selectedGroup, factorAnalysis.getReconstructedWeights());

			}
		});
		GridBagConstraints gbc_buttonReconstruction = new GridBagConstraints();
		gbc_buttonReconstruction.fill = GridBagConstraints.BOTH;
		gbc_buttonReconstruction.insets = new Insets(0, 0, 5, 5);
		gbc_buttonReconstruction.gridx = 1;
		gbc_buttonReconstruction.gridy = 8;
		panelFactorAnalysis.add(buttonReconstruction, gbc_buttonReconstruction);


		JButton buttonClosePanel = new JButton(rb.getString("text_buttonClosePanel"));
		buttonClosePanel.addActionListener(new ActionListener() {

			/**
			 * @param arg0
			 *            Press Button
			 * @return Removes <code>JPanel</code> panel from
			 *         <code>JTabbedPane</code> tabbedPane.
			 */
			public void actionPerformed(ActionEvent arg0) {
				tabbedPane.remove(panelFactorAnalysis);
				tabbedPane.setSelectedIndex(GeoLingGUI.TAB_INDEX_MAPTREE);
			}
		});
		buttonClosePanel.setToolTipText(rb.getString("tooltip_buttonClosePanel"));
		GridBagConstraints gbc_buttonClosePanel = new GridBagConstraints();
		gbc_buttonClosePanel.fill = GridBagConstraints.BOTH;
		gbc_buttonClosePanel.insets = new Insets(0, 0, 0, 5);
		gbc_buttonClosePanel.gridx = 1;
		gbc_buttonClosePanel.gridy = 9;
		panelFactorAnalysis.add(buttonClosePanel, gbc_buttonClosePanel);

	}

}
