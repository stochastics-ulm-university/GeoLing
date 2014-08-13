package geoling.gui;

import geoling.gui.util.ColorHueTableCellRenderer;
import geoling.gui.util.ComboBoxDistanceElement;
import geoling.gui.util.ComboBoxLevelElement;
import geoling.gui.util.AreaClassMapLabel;
import geoling.gui.util.JFileChooserConfirmOverwrite;
import geoling.gui.util.TableVariantElement;
import geoling.locations.util.AggregatedLocation;
import geoling.maps.AreaClassMap;
import geoling.maps.VariantMap;
import geoling.maps.density.KernelDensityEstimation;
import geoling.maps.density.bandwidth.*;
import geoling.maps.density.bandwidth.computation.ComputeBandwidths;
import geoling.maps.density.kernels.*;
import geoling.maps.distances.*;
import geoling.maps.plot.*;
import geoling.maps.projection.*;
import geoling.maps.util.BuilderMethods;
import geoling.maps.weights.*;
import geoling.models.*;
import geoling.util.SetComparison;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.util.plot.PlotToEPS;
import geoling.util.sim.util.plot.PlotToGraphics2D;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.javalite.activejdbc.LazyList;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Insets;
import java.awt.event.*;

/**
 * Panel for map plots in dialectometry GUI.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class MapPanel {

	/**
	 * For this map is this <code>MapPanel</code> created.
	 * 
	 * @see GeoLingGUI#selectedMap
	 */
	private Map selectedMap;

	/** <code>JPanel</code> for the contents. */
	private JPanel panelMap;
	/**
	 * The <code>JLabel</code> <i>labelMap</i> is set to the viewport view of
	 * this <code>JScrollPane</code>.
	 * 
	 * @see #labelMap
	 */
	private JScrollPane scrollPaneForLabelMap;
	/**
	 * This <code>ImageIcon</code> is used for the construction of
	 * <code>JLabel</code> with the drawn map.
	 */
	private ImageIcon iconMap;
	/**
	 * The <code>MapLabel</code> shows the drawn <code>AreaClassMap</code> or
	 * <code>VariantMap</code> and has a special
	 * <code>getToolTipText(MouseEvent)</code> method for displaying locations.
	 */
	private AreaClassMapLabel labelMap;
	/** The <code>JComboBox</code> shows all levels of variant mappings. */
	private JComboBox<ComboBoxLevelElement> comboBoxLevel;
	/**
	 * This <code>VariantWeights</code> object without levels is stored for
	 * higher performance of a later generated
	 * <code>VariantWeightsWithLevel</code>.
	 * 
	 * @see VariantWeightsNoLevel
	 */
	private VariantWeights variantWeightsInitialization;
	/**
	 * This <code>VariantWeights</code> are calculated with the chosen
	 * <code>Level</code>.
	 * 
	 * @see #selectedLevel
	 */
	private VariantWeights variantWeights = variantWeightsInitialization;
	/**
	 * The <code>Level</code> for variant mapping, which can be chosen in the
	 * <code>JComboBox</code>.
	 * 
	 * @see #comboBoxLevel
	 */
	private Level selectedLevel;

	private JComboBox<ComboBoxDistanceElement> comboBoxDistance;
	private Distance selectedDistance;

	/**
	 * This <code>JButton</code> opens a new <code>JPanel</code>, in which a
	 * table with the mapping of variants is shown.
	 */
	private JButton buttonShowMapping;
	/**
	 * This <code>ButtonGroup</code> offers two <code>RadioButton</code> for
	 * choosing the map type.
	 */
	private ButtonGroup buttonGroupMapTypes;
	/**
	 * True, if grid map type is selected; false, if Voronoi map type is
	 * selected.
	 */
	private boolean gridMapType = false;

	private JComboBox<String> comboBoxKernel;

	// bandwidths
	/** This <code>ButtonGroup</code> offers for <code>RadioButton</code> for choosing the bandwidth. */
	private ButtonGroup buttonGroupBandwidths;

	/** This <code>JLabel</code> shows the bandwidth estimated by LCV. */
	private JLabel lblLcvValue;
	/** This <code>JLabel</code> shows the bandwidth estimated by LSCV. */
	private JLabel lblLscvValue;
	/** This <code>JLabel</code> shows the bandwidth estimated by Min-Complexity-Max-Fidelity. */
	private JLabel lblClValue;

	/** This <code>JRadioButton</code> represents the selection of LCV bandwidth. */
	private JRadioButton buttonLcv;
	/** This <code>JRadioButton</code> represents the selection of LSCV bandwidth. */
	private JRadioButton buttonLscv;
	/** This <code>JRadioButton</code> represents the selection of Min-Complexity-Max-Fidelity bandwidth. */
	private JRadioButton buttonCl;
	/** This <code>JRadioButton</code> represents the selection of a manual inserted bandwidth. */
	private JRadioButton buttonManual;

	/** This bandwidth has been estimated by LCV and is saved in the database. */
	private BigDecimal bandwidthLcv;
	/** This bandwidth has been estimated by LSCV and is saved in the database. */
	private BigDecimal bandwidthLscv;
	/** This bandwidth has been estimated by Min-Complexity-Max-Fidelity and is saved in the database. */
	private BigDecimal bandwidthCl;
	/** In this <code>JTextField</code> can be inserted an optional bandwidth. */
	private JTextField textFieldManualBandwidth;
	/** This <code>BigDecimal</code> saves the current selected bandwidth. */
	private BigDecimal selectedBandwidth;


	/**
	 * This <code>JScrollPane</code> is the environment for the
	 * <code>JTable</code> <i>tableVariantenFarbe</i>.
	 * 
	 * @see #tableVariantColor
	 */
	private JScrollPane scrollPaneForTableVariantColor;
	/**
	 * In the first column of this <code>JTable</code> the dominant variants of
	 * this <code>AreaClassMap</code> are listed and in the second column the
	 * corresponding RGB-color value of the dominant variants are shown.
	 * Requires <code>TableVariantElement</code> and
	 * <code>ColorTableCellRenderer</code>.
	 * 
	 * @see TableVariantElement
	 * @see ColorHueTableCellRenderer
	 */
	private JTable tableVariantColor;
	/**
	 * Pressing this <code>JButton</code> draws a area class map on the
	 * <code>MapLabel</code> <i>labelMap</i>.
	 */
	private JButton buttonDrawMap;
	/**
	 * Pressing this <code>JButton</code> saves the currently drawn map as .eps
	 * file.
	 */
	private JButton buttonSaveMap;
	/**
	 * Pressing this <code>JButton</code> removes this <code>JPanel</code> from
	 * <code>JTabbedPane</code>.
	 */
	private JButton buttonCloseMapPanel;
	private JLabel labelCharacteristics;

	private String weights_identification;
	private String kernel_identification;
	private String distance_identification;

	private AreaClassMap areaClassMap;
	private HashMap<Variant, Color> variantColors;
	private Object[][] tableContentsColor;
	private Variant selectedVariant;
	private int tableColumn;
	private PlotHelper helper;
	private boolean classmap = false;
	private HashMap<Polytope, AggregatedLocation> hints = new HashMap<Polytope, AggregatedLocation>();

	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;

	/**
	 * Create a panel for drawing area class maps and variant maps with
	 * different bandwidths.
	 * 
	 * @param tabbedPane
	 *            To this <code>JTabbedPane</code> the new panel is added.
	 * @param outputfolder
	 *            Basis folder for output, defined in a property file.
	 * @param selectedMap
	 *            This <code>Map</code> can be drawn in this panel.
	 */
	public MapPanel(final JTabbedPane tabbedPane, final String outputfolder, final Map selectedMap) {
		this.selectedMap = selectedMap;

		rb = ResourceBundle.getBundle("MapPanel", GeoLingGUI.LANGUAGE);

		panelMap = new JPanel();
		String title = selectedMap.getString("name");
		if (title.length() > 22) {
			title = title.substring(0, Math.min(20, title.length()))+"...";
		}
		tabbedPane.addTab(title, null, panelMap, selectedMap.getString("name"));
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

		GridBagLayout gbl_panelMap = new GridBagLayout();
		gbl_panelMap.columnWidths = new int[] {
				250,  // left: map
				3,
				50,   // right: 3-column-layout
				50,
				50,
				0
		};
		gbl_panelMap.rowHeights = new int[] {
				30,   // level
				0,
				30,   // distance
				0,
				30,   // map type
				0,
				30,   // kernel
				0,
				30,   // bandwidth
				0,
				20,   // LCV
				20,   // LSCV
				20,   // CL
				20,   // manual bandwidth
				0,
				20,   // empty space / map characteristics
				40,   // draw map
				40,   // save map
				40,   // draw variant map
				40,   // levels
				40,   // close tab
				0
		};
		gbl_panelMap.columnWeights = new double[] {
				0.7,              // left: map
				Double.MIN_VALUE,
				0.1,              // right: 3-column-layout
				0.1,
				0.1,
				Double.MIN_VALUE
		};
		gbl_panelMap.rowWeights = new double[] {
				0.1,              // level
				Double.MIN_VALUE,
				0.1,              // distance
				Double.MIN_VALUE,
				0.1,              // map type
				Double.MIN_VALUE,
				0.1,              // kernel
				Double.MIN_VALUE,
				0.1,              // bandwidth
				Double.MIN_VALUE,
				0.05,             // LCV
				0.05,             // LSCV
				0.05,             // CL
				0.05,             // manual bandwidth
				Double.MIN_VALUE,
				0.05,             // empty space / map characteristics
				0.05,             // draw map
				0.05,             // save map
				0.05,             // draw variant map
				0.05,             // levels
				0.05,             // close tab
				Double.MIN_VALUE
		};
		panelMap.setLayout(gbl_panelMap);

		variantWeightsInitialization = new VariantWeightsNoLevel(selectedMap);

		final Polytope borderPolygon = Border.getDefaultBorder().toPolygon();
		final MapProjection mapProjection = new MercatorProjection();

		JLabel labelMapName = new JLabel(selectedMap.getString("name"));
		GridBagConstraints gbc_labelMapName = new GridBagConstraints();
		gbc_labelMapName.insets = new Insets(0, 0, 0, 5);
		gbc_labelMapName.fill = GridBagConstraints.BOTH;
		gbc_labelMapName.gridx = 0;
		gbc_labelMapName.gridy = 0;
		panelMap.add(labelMapName, gbc_labelMapName);

		scrollPaneForLabelMap = new JScrollPane();
		GridBagConstraints gbc_scrollPaneForLabelMap = new GridBagConstraints();
		gbc_scrollPaneForLabelMap.gridheight = 18;
		gbc_scrollPaneForLabelMap.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPaneForLabelMap.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneForLabelMap.gridx = 0;
		gbc_scrollPaneForLabelMap.gridy = 2;

		labelMap = new AreaClassMapLabel();
		scrollPaneForLabelMap.setViewportView(labelMap);
		panelMap.add(scrollPaneForLabelMap, gbc_scrollPaneForLabelMap);

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
		gbc_sliderZoom.insets = new Insets(0, 0, 5, 5);
		gbc_sliderZoom.fill = GridBagConstraints.BOTH;
		gbc_sliderZoom.gridx = 0;
		gbc_sliderZoom.gridy = 20;
		panelMap.add(sliderZoom, gbc_sliderZoom);

		labelCharacteristics = new JLabel();
		labelCharacteristics.setFont(labelCharacteristics.getFont().deriveFont(labelCharacteristics.getFont().getSize2D()-2.0f));
		GridBagConstraints gbc_labelCharacteristics = new GridBagConstraints();
		gbc_labelCharacteristics.insets = new Insets(5, 5, 5, 5);
		gbc_labelCharacteristics.fill = GridBagConstraints.BOTH;
		gbc_labelCharacteristics.gridx = 2;
		gbc_labelCharacteristics.gridy = 15;
		gbc_labelCharacteristics.gridwidth = 3;
		panelMap.add(labelCharacteristics, gbc_labelCharacteristics);

		// combo box to select level
		JLabel lblLevel = new JLabel(rb.getString("text_lblLevel"));
		GridBagConstraints gbc_lblLevel = new GridBagConstraints();
		gbc_lblLevel.insets = new Insets(0, 0, 5, 5);
		gbc_lblLevel.fill = GridBagConstraints.BOTH;
		gbc_lblLevel.gridx = 2;
		gbc_lblLevel.gridy = 0;
		panelMap.add(lblLevel, gbc_lblLevel);

		LazyList<Level> levels = Level.findAll();
		ComboBoxLevelElement[] levelElements = new ComboBoxLevelElement[levels.size()];
		for (int i = 0; i < levels.size(); i++) {
			levelElements[i] = new ComboBoxLevelElement(levels.get(i));
		}
		comboBoxLevel = new JComboBox<ComboBoxLevelElement>();
		comboBoxLevel.setModel(new DefaultComboBoxModel<ComboBoxLevelElement>(levelElements));
		// first initialization of identification strings
		if (comboBoxLevel.getItemCount() > 0) {
			comboBoxLevel.setSelectedIndex(0);
			selectedLevel = ((ComboBoxLevelElement) comboBoxLevel.getItemAt(0)).getLevel();
			variantWeights = new VariantWeightsWithLevel(variantWeightsInitialization, selectedLevel);
		} else {
			selectedLevel = null;
			variantWeights = variantWeightsInitialization;
		}
		weights_identification = variantWeights.getIdentificationString();

		comboBoxLevel.setPrototypeDisplayValue(new ComboBoxLevelElement(null));
		comboBoxLevel.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					ComboBoxLevelElement element = (ComboBoxLevelElement) e.getItem(); // ComboBoxLevelElement
					selectedLevel = element.getLevel();

					variantWeights = new VariantWeightsWithLevel(variantWeightsInitialization, selectedLevel);
					weights_identification = variantWeights.getIdentificationString();
					setBandwidthLabels();
				}
			}
		});

		GridBagConstraints gbc_comboBoxLevel = new GridBagConstraints();
		gbc_comboBoxLevel.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxLevel.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxLevel.gridx = 3;
		gbc_comboBoxLevel.gridy = 0;
		gbc_comboBoxLevel.gridwidth = 2;
		panelMap.add(comboBoxLevel, gbc_comboBoxLevel);

		buttonShowMapping = new JButton(rb.getString("text_buttonShowMapping"));
		buttonShowMapping.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				new VariantsMappingPanel(tabbedPane, selectedMap);
			}
		});
		buttonShowMapping.setToolTipText(rb.getString("tooltip_buttonShowMapping"));
		GridBagConstraints gbc_buttonShowMapping = new GridBagConstraints();
		gbc_buttonShowMapping.insets = new Insets(0, 0, 5, 5);
		gbc_buttonShowMapping.fill = GridBagConstraints.BOTH;
		gbc_buttonShowMapping.gridx = 2;
		gbc_buttonShowMapping.gridy = 19;
		panelMap.add(buttonShowMapping, gbc_buttonShowMapping);

		JSeparator separatorLevel = new JSeparator();
		separatorLevel.setForeground(Color.BLACK);
		GridBagConstraints gbc_separatorLevel = new GridBagConstraints();
		gbc_separatorLevel.gridwidth = 3;
		gbc_separatorLevel.fill = GridBagConstraints.BOTH;
		gbc_separatorLevel.insets = new Insets(0, 0, 5, 0);
		gbc_separatorLevel.gridx = 2;
		gbc_separatorLevel.gridy = 1;
		panelMap.add(separatorLevel, gbc_separatorLevel);

		// combo box to select distance
		JLabel lblDistance = new JLabel(rb.getString("text_lblDistance"));
		GridBagConstraints gbc_lblAbstand = new GridBagConstraints();
		gbc_lblAbstand.fill = GridBagConstraints.BOTH;
		gbc_lblAbstand.insets = new Insets(0, 0, 5, 5);
		gbc_lblAbstand.gridx = 2;
		gbc_lblAbstand.gridy = 2;
		panelMap.add(lblDistance, gbc_lblAbstand);

		comboBoxDistance = new JComboBox<ComboBoxDistanceElement>();
		LazyList<Distance> distances = Distance.findAll();
		ComboBoxDistanceElement[] distanceElements = new ComboBoxDistanceElement[distances.size()];
		for (int i = 0; i < distances.size(); i++) {
			distanceElements[i] = new ComboBoxDistanceElement(distances.get(i));
		}
		comboBoxDistance.setModel(new DefaultComboBoxModel<ComboBoxDistanceElement>(distanceElements));
		if (distanceElements.length > 0) {
			comboBoxDistance.setSelectedIndex(0);
			selectedDistance = distanceElements[0].getDistance();
			distance_identification = selectedDistance.getString("identification");
			if (distance_identification==null) { // geographic distance's identification is null
				distance_identification = selectedDistance.getString("type");
			}
		}
		
		comboBoxDistance.setPrototypeDisplayValue(new ComboBoxDistanceElement(null));
		comboBoxDistance.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					ComboBoxDistanceElement element = (ComboBoxDistanceElement) e.getItem();
					selectedDistance = element.getDistance();
					distance_identification = selectedDistance.getString("identification");
					if (distance_identification==null) { // geographic distance's identification is null
						distance_identification = selectedDistance.getString("type");
					}
					setBandwidthLabels();
				}
			}
		});
		GridBagConstraints gbc_comboBoxDistance = new GridBagConstraints();
		gbc_comboBoxDistance.gridwidth = 2;
		gbc_comboBoxDistance.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxDistance.fill = GridBagConstraints.BOTH;
		gbc_comboBoxDistance.gridx = 3;
		gbc_comboBoxDistance.gridy = 2;
		panelMap.add(comboBoxDistance, gbc_comboBoxDistance);

		JSeparator separatorDistance = new JSeparator();
		separatorDistance.setForeground(Color.BLACK);
		GridBagConstraints gbc_separatorDistance = new GridBagConstraints();
		gbc_separatorDistance.gridwidth = 3;
		gbc_separatorDistance.fill = GridBagConstraints.BOTH;
		gbc_separatorDistance.insets = new Insets(0, 0, 5, 0);
		gbc_separatorDistance.gridx = 2;
		gbc_separatorDistance.gridy = 3;
		panelMap.add(separatorDistance, gbc_separatorDistance);

		// radio buttons to select map type
		JLabel lblMapType = new JLabel(rb.getString("text_lblMapType"));
		GridBagConstraints gbc_lblMapType = new GridBagConstraints();
		gbc_lblMapType.fill = GridBagConstraints.BOTH;
		gbc_lblMapType.insets = new Insets(0, 0, 5, 5);
		gbc_lblMapType.gridx = 2;
		gbc_lblMapType.gridy = 4;
		panelMap.add(lblMapType, gbc_lblMapType);

		buttonGroupMapTypes = new ButtonGroup();

		JRadioButton buttonVoronoi = new JRadioButton(rb.getString("text_buttonVoronoi"));
		buttonVoronoi.setMnemonic(KeyEvent.VK_V);
		buttonVoronoi.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				gridMapType = false;
			}
		});
		buttonVoronoi.setSelected(true);
		GridBagConstraints gbc_buttonVoronoi = new GridBagConstraints();
		gbc_buttonVoronoi.fill = GridBagConstraints.BOTH;
		gbc_buttonVoronoi.insets = new Insets(0, 0, 5, 5);
		gbc_buttonVoronoi.gridx = 3;
		gbc_buttonVoronoi.gridy = 4;
		panelMap.add(buttonVoronoi, gbc_buttonVoronoi);
		buttonGroupMapTypes.add(buttonVoronoi);

		JRadioButton buttonGrid = new JRadioButton(rb.getString("text_buttonGrid"));
		buttonGrid.setMnemonic(KeyEvent.VK_K);
		buttonGrid.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				gridMapType = true;
			}
		});
		GridBagConstraints gbc_buttonGrid = new GridBagConstraints();
		gbc_buttonGrid.fill = GridBagConstraints.BOTH;
		gbc_buttonGrid.insets = new Insets(0, 0, 5, 0);
		gbc_buttonGrid.gridx = 4;
		gbc_buttonGrid.gridy = 4;
		panelMap.add(buttonGrid, gbc_buttonGrid);
		buttonGroupMapTypes.add(buttonGrid);

		JSeparator separatorMapType = new JSeparator();
		separatorMapType.setForeground(Color.BLACK);
		GridBagConstraints gbc_separatorMapType = new GridBagConstraints();
		gbc_separatorMapType.gridwidth = 3;
		gbc_separatorMapType.fill = GridBagConstraints.BOTH;
		gbc_separatorMapType.insets = new Insets(0, 0, 5, 0);
		gbc_separatorMapType.gridx = 2;
		gbc_separatorMapType.gridy = 5;
		panelMap.add(separatorMapType, gbc_separatorMapType);


		// radio buttons to select kernel type
		JLabel lblKernel = new JLabel(rb.getString("text_lblKernel"));
		GridBagConstraints gbc_lblKernel = new GridBagConstraints();
		gbc_lblKernel.fill = GridBagConstraints.BOTH;
		gbc_lblKernel.insets = new Insets(0, 0, 5, 5);
		gbc_lblKernel.gridx = 2;
		gbc_lblKernel.gridy = 6;
		panelMap.add(lblKernel, gbc_lblKernel);


		comboBoxKernel = new JComboBox<String>(new String[] { rb.getString("Gauss"), rb.getString("K3"), rb.getString("Epanechnikov") });
		comboBoxKernel.setSelectedIndex(0);
		kernel_identification = GaussianKernel.getStaticIdentificationString();
		
		comboBoxKernel.setPrototypeDisplayValue("Some kernel");
		comboBoxKernel.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (e.getItem().toString().equals(rb.getString("Gauss"))) {
						kernel_identification = GaussianKernel.getStaticIdentificationString();
					}
					if (e.getItem().toString().equals(rb.getString("K3"))) {
						kernel_identification = K3Kernel.getStaticIdentificationString();
					}
					if (e.getItem().toString().equals(rb.getString("Epanechnikov"))) {
						kernel_identification = EpanechnikovKernel.getStaticIdentificationString();
					}
					setBandwidthLabels();
				}

			}

		});

		GridBagConstraints gbc_comboBoxKernel = new GridBagConstraints();
		gbc_comboBoxKernel.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxKernel.fill = GridBagConstraints.BOTH;
		gbc_comboBoxKernel.gridx = 3;
		gbc_comboBoxKernel.gridy = 6;
		gbc_comboBoxKernel.gridwidth = 2;
		panelMap.add(comboBoxKernel, gbc_comboBoxKernel);


		JSeparator separatorKernel = new JSeparator();
		separatorKernel.setForeground(Color.BLACK);
		GridBagConstraints gbc_separatorKernel = new GridBagConstraints();
		gbc_separatorKernel.fill = GridBagConstraints.BOTH;
		gbc_separatorKernel.gridwidth = 3;
		gbc_separatorKernel.insets = new Insets(0, 0, 5, 0);
		gbc_separatorKernel.gridx = 2;
		gbc_separatorKernel.gridy = 7;
		panelMap.add(separatorKernel, gbc_separatorKernel);


		// labels to show bandwidth
		JLabel lblBandwidth1 = new JLabel(rb.getString("text_lblBandwidth1"));
		GridBagConstraints gbc_lblBandwidth1 = new GridBagConstraints();
		gbc_lblBandwidth1.fill = GridBagConstraints.BOTH;
		gbc_lblBandwidth1.insets = new Insets(0, 0, 5, 5);
		gbc_lblBandwidth1.gridx = 2;
		gbc_lblBandwidth1.gridy = 8;
		panelMap.add(lblBandwidth1, gbc_lblBandwidth1);

		JLabel lblLcv = new JLabel(rb.getString("LCV"));
		GridBagConstraints gbc_lblLcv = new GridBagConstraints();
		gbc_lblLcv.fill = GridBagConstraints.BOTH;
		gbc_lblLcv.insets = new Insets(0, 0, 5, 5);
		gbc_lblLcv.gridx = 2;
		gbc_lblLcv.gridy = 9;
		panelMap.add(lblLcv, gbc_lblLcv);

		JLabel lblLscv = new JLabel(rb.getString("LSCV"));
		GridBagConstraints gbc_lblLscv = new GridBagConstraints();
		gbc_lblLscv.fill = GridBagConstraints.BOTH;
		gbc_lblLscv.insets = new Insets(0, 0, 5, 5);
		gbc_lblLscv.gridx = 2;
		gbc_lblLscv.gridy = 10;
		panelMap.add(lblLscv, gbc_lblLscv);

		JLabel lblCl = new JLabel(rb.getString("MinCMaxL"));
		GridBagConstraints gbc_lblCl = new GridBagConstraints();
		gbc_lblCl.fill = GridBagConstraints.BOTH;
		gbc_lblCl.insets = new Insets(0, 0, 5, 5);
		gbc_lblCl.gridx = 2;
		gbc_lblCl.gridy = 11;
		panelMap.add(lblCl, gbc_lblCl);

		lblLcvValue = new JLabel();
		GridBagConstraints gbc_lblLcvValue = new GridBagConstraints();
		gbc_lblLcvValue.insets = new Insets(0, 0, 5, 5);
		gbc_lblLcvValue.gridx = 3;
		gbc_lblLcvValue.gridy = 9;
		panelMap.add(lblLcvValue, gbc_lblLcvValue);

		lblLscvValue = new JLabel();
		GridBagConstraints gbc_lblLscvValue = new GridBagConstraints();
		gbc_lblLscvValue.insets = new Insets(0, 0, 5, 5);
		gbc_lblLscvValue.gridx = 3;
		gbc_lblLscvValue.gridy = 10;
		panelMap.add(lblLscvValue, gbc_lblLscvValue);

		lblClValue = new JLabel();
		GridBagConstraints gbc_lblClValue = new GridBagConstraints();
		gbc_lblClValue.insets = new Insets(0, 0, 5, 5);
		gbc_lblClValue.gridx = 3;
		gbc_lblClValue.gridy = 11;
		panelMap.add(lblClValue, gbc_lblClValue);

		buttonGroupBandwidths = new ButtonGroup();

		buttonLcv = new JRadioButton(rb.getString("text_buttonLCV"));
		buttonLcv.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				if (bandwidthLcv == null) {
					bandwidthLcv = ComputeBandwidths.findOrComputeAndSaveBandwidth(variantWeights, new LikelihoodCrossValidation(getKernel()), false, null);
					lblLcvValue.setText(bandwidthLcv.stripTrailingZeros().toPlainString());
				}
				selectedBandwidth = bandwidthLcv;
			}
		});
		GridBagConstraints gbc_buttonLcv = new GridBagConstraints();
		gbc_buttonLcv.fill = GridBagConstraints.BOTH;
		gbc_buttonLcv.insets = new Insets(0, 0, 5, 0);
		gbc_buttonLcv.gridx = 4;
		gbc_buttonLcv.gridy = 9;
		panelMap.add(buttonLcv, gbc_buttonLcv);
		buttonGroupBandwidths.add(buttonLcv);

		buttonLscv = new JRadioButton(rb.getString("text_buttonLSCV"));
		buttonLscv.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				if (bandwidthLscv == null) {
					Kernel kernel = getKernel();
					DistanceMeasure distanceMeasure = kernel.getDistanceMeasure();
					if ((kernel instanceof GaussianKernel) && (distanceMeasure instanceof GeographicalDistance)) {
						bandwidthLscv = ComputeBandwidths.findOrComputeAndSaveBandwidth(variantWeights, new LeastSquaresCrossValidation(getKernel()), false, null);
						lblLscvValue.setText(bandwidthLscv.stripTrailingZeros().toPlainString());
					}
					else {
						JOptionPane.showMessageDialog(panelMap, rb.getString("text_popupWrongKernel"), rb.getString("title_popupWrongKernel"), JOptionPane.WARNING_MESSAGE);
					}
				}
				selectedBandwidth = bandwidthLscv;
			}
		});
		GridBagConstraints gbc_buttonLscv = new GridBagConstraints();
		gbc_buttonLscv.fill = GridBagConstraints.BOTH;
		gbc_buttonLscv.insets = new Insets(0, 0, 5, 0);
		gbc_buttonLscv.gridx = 4;
		gbc_buttonLscv.gridy = 10;
		panelMap.add(buttonLscv, gbc_buttonLscv);
		buttonGroupBandwidths.add(buttonLscv);

		buttonCl = new JRadioButton(rb.getString("text_buttonMinCMaxL"));
		buttonCl.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				if (bandwidthCl == null) {
					bandwidthCl = ComputeBandwidths.findOrComputeAndSaveBandwidth(variantWeights, new MinComplexityMaxFidelity(getKernel()), false, null);
					lblClValue.setText(bandwidthCl.stripTrailingZeros().toPlainString());
				}
				selectedBandwidth = bandwidthCl;
			}
		});
		GridBagConstraints gbc_buttonCl = new GridBagConstraints();
		gbc_buttonCl.fill = GridBagConstraints.BOTH;
		gbc_buttonCl.insets = new Insets(0, 0, 5, 0);
		gbc_buttonCl.gridx = 4;
		gbc_buttonCl.gridy = 11;
		panelMap.add(buttonCl, gbc_buttonCl);
		buttonGroupBandwidths.add(buttonCl);

		setBandwidthLabels();

		buttonManual = new JRadioButton(rb.getString("text_buttonManual"));
		buttonManual.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				try {
					selectedBandwidth = new BigDecimal(textFieldManualBandwidth.getText());
					buttonManual.setSelected(true);
				} catch (NumberFormatException ex) {
					selectedBandwidth = null;
				}
			}
		});

		JSeparator separatorBandbreite1 = new JSeparator();
		separatorBandbreite1.setForeground(Color.BLACK);
		GridBagConstraints gbc_separatorBandbreite1 = new GridBagConstraints();
		gbc_separatorBandbreite1.fill = GridBagConstraints.BOTH;
		gbc_separatorBandbreite1.gridwidth = 3;
		gbc_separatorBandbreite1.insets = new Insets(0, 0, 5, 0);
		gbc_separatorBandbreite1.gridx = 2;
		gbc_separatorBandbreite1.gridy = 12;
		panelMap.add(separatorBandbreite1, gbc_separatorBandbreite1);

		GridBagConstraints gbc_buttonManual = new GridBagConstraints();
		gbc_buttonManual.fill = GridBagConstraints.BOTH;
		gbc_buttonManual.insets = new Insets(0, 0, 5, 0);
		gbc_buttonManual.gridx = 4;
		gbc_buttonManual.gridy = 13;
		panelMap.add(buttonManual, gbc_buttonManual);
		buttonGroupBandwidths.add(buttonManual);


		JLabel lblBandwidth2 = new JLabel(rb.getString("text_lblBandwidth2"));
		GridBagConstraints gbc_lblBandwidth2 = new GridBagConstraints();
		gbc_lblBandwidth2.fill = GridBagConstraints.BOTH;
		gbc_lblBandwidth2.insets = new Insets(0, 0, 5, 5);
		gbc_lblBandwidth2.gridx = 2;
		gbc_lblBandwidth2.gridy = 13;
		panelMap.add(lblBandwidth2, gbc_lblBandwidth2);

		textFieldManualBandwidth = new JTextField();
		textFieldManualBandwidth.setToolTipText(rb.getString("tooltip_textFieldManualBandwidth"));
		textFieldManualBandwidth.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent e) {
				setManualBandwidth();
			}

			public void removeUpdate(DocumentEvent e) {
				setManualBandwidth();
			}

			public void insertUpdate(DocumentEvent e) {
				setManualBandwidth();
			}

			public void setManualBandwidth() {
				try {
					selectedBandwidth = new BigDecimal(textFieldManualBandwidth.getText());
					buttonManual.setSelected(true);
				} catch (NumberFormatException ex) {
					selectedBandwidth = null;
				}
			}
		});

		GridBagConstraints gbc_textFieldManuellBandwidth = new GridBagConstraints();
		gbc_textFieldManuellBandwidth.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldManuellBandwidth.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldManuellBandwidth.gridx = 3;
		gbc_textFieldManuellBandwidth.gridy = 13;
		panelMap.add(textFieldManualBandwidth, gbc_textFieldManuellBandwidth);

		JSeparator separatorBandbreite2 = new JSeparator();
		separatorBandbreite2.setForeground(Color.BLACK);
		GridBagConstraints gbc_separatorBandbreite2 = new GridBagConstraints();
		gbc_separatorBandbreite2.fill = GridBagConstraints.BOTH;
		gbc_separatorBandbreite2.gridwidth = 3;
		gbc_separatorBandbreite2.insets = new Insets(0, 0, 5, 0);
		gbc_separatorBandbreite2.gridx = 2;
		gbc_separatorBandbreite2.gridy = 14;
		panelMap.add(separatorBandbreite2, gbc_separatorBandbreite2);


		buttonDrawMap = new JButton(rb.getString("text_buttonDrawMap"));
		buttonDrawMap.addActionListener(new ActionListener() {

			/**
			 * Draws map.
			 * @param arg0   Press Button
			 */
			public void actionPerformed(ActionEvent arg0) {
				if (selectedBandwidth == null) {
					JOptionPane.showMessageDialog(panelMap, rb.getString("text_popupNoBandwidth"), rb.getString("title_popupNoBandwidth"), JOptionPane.WARNING_MESSAGE);
				} else {
					Kernel kernel = getKernel();

					if (gridMapType && (kernel.getDistanceMeasure() instanceof PrecomputedDistance)) {
						JOptionPane.showMessageDialog(panelMap, rb.getString("text_popupWrongDistance"), rb.getString("title_popupWrongDistance"), JOptionPane.ERROR_MESSAGE);
						return;
					}

					KernelDensityEstimation kde = new KernelDensityEstimation(kernel);
					if (areaClassMap == null ||
					    !areaClassMap.getVariantWeights().getIdentificationString().equals(variantWeights.getIdentificationString()) ||
					    !areaClassMap.getDensityEstimation().getIdentificationString().equals(kde.getIdentificationString())) {
						areaClassMap = new AreaClassMap(variantWeights, kde);
						areaClassMap.buildLocationDensityCache();
						areaClassMap.buildAreas(borderPolygon, mapProjection);
					}

					DecimalFormat df = new DecimalFormat("0.000");
					labelCharacteristics.setText(rb.getString("totalBorderLength") + ": " + df.format(areaClassMap.computeTotalBorderLength())
							+ " km, " + rb.getString("totalCompactness") + ": " + df.format(areaClassMap.computeOverallAreaCompactness()) + ", " + rb.getString("totalHomogeneity") + ": "
							+ df.format(areaClassMap.computeOverallHomogeneity()));

					int height = scrollPaneForLabelMap.getSize().height - 25;
					helper = new PlotHelper(borderPolygon, mapProjection, height, 10);
					PlotAreaClassMap plot = new PlotAreaClassMap(areaClassMap);
					classmap = true;

					variantColors = getUpdatedVariantColors(variantColors, plot.getDefaultAreaColors(false), areaClassMap);

					BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);
					try (PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
						if (gridMapType) {
							plot.gridExport(gre, helper, null, variantColors, hints);
						} else {
							plot.voronoiExport(gre, helper, variantColors, hints);
						}
					}

					iconMap = new ImageIcon(bi);
					labelMap = new AreaClassMapLabel(iconMap, hints, areaClassMap, 1.0f);
					ToolTipManager.sharedInstance().registerComponent(labelMap);
					scrollPaneForLabelMap.getViewport().setView(labelMap);

					tableContentsColor = new Object[variantColors.size()][2];
					int variantIndex = 0;
					ArrayList<Variant> sortedVariants = new ArrayList<Variant>(variantColors.keySet());
					Collections.sort(sortedVariants);
					for (Variant variant : sortedVariants) {
						tableContentsColor[variantIndex][0] = new TableVariantElement(variant);
						tableContentsColor[variantIndex][1] = variantColors.get(variant);
						variantIndex++;
					}
					tableVariantColor.setModel(new DefaultTableModel(tableContentsColor, new String[] { rb.getString("columnName1_tableVariantColor"),
							rb.getString("columnName2_tableVariantColor") }) {

						private static final long serialVersionUID = 1L;

						public boolean isCellEditable(int rowIndex, int columnIndex) {
							return false;
						}
					});
					ColorHueTableCellRenderer ctr = new ColorHueTableCellRenderer();
					tableVariantColor.getColumnModel().getColumn(1).setCellRenderer(ctr);
					// set slider for zoom on 100 percent
					sliderZoom.setValue(100);
				}
			}
		});
		buttonDrawMap.setToolTipText(rb.getString("tooltip_buttonDrawMap"));
		GridBagConstraints gbc_buttonDrawMap = new GridBagConstraints();
		gbc_buttonDrawMap.fill = GridBagConstraints.BOTH;
		gbc_buttonDrawMap.insets = new Insets(0, 0, 5, 5);
		gbc_buttonDrawMap.gridx = 2;
		gbc_buttonDrawMap.gridy = 16;
		panelMap.add(buttonDrawMap, gbc_buttonDrawMap);


		scrollPaneForTableVariantColor = new JScrollPane();
		GridBagConstraints gbc_scrollPaneForTableVariantColor = new GridBagConstraints();
		gbc_scrollPaneForTableVariantColor.gridwidth = 2;
		gbc_scrollPaneForTableVariantColor.gridheight = 5;
		gbc_scrollPaneForTableVariantColor.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneForTableVariantColor.gridx = 3;
		gbc_scrollPaneForTableVariantColor.gridy = 16;
		panelMap.add(scrollPaneForTableVariantColor, gbc_scrollPaneForTableVariantColor);


		tableVariantColor = new JTable();
		tableVariantColor.setToolTipText(rb.getString("tooltip_tableVariantColor"));
		tableVariantColor.setModel(new DefaultTableModel(new Object[][] { { null, null }, }, new String[] { rb.getString("columnName1_tableVariantColor"),
				rb.getString("columnName2_tableVariantColor") }));
		scrollPaneForTableVariantColor.setViewportView(tableVariantColor);
		tableVariantColor.addMouseListener(new MouseAdapter() {

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
				int row = tableVariantColor.getSelectedRow();
				tableColumn = tableVariantColor.getSelectedColumn();
				if (tableContentsColor[row][0] instanceof TableVariantElement) {
					Variant variant = ((TableVariantElement) tableContentsColor[row][0]).getVariant();
					selectedVariant = variant;
					if (arg0.getClickCount() == 2) {
						Color oldColor = variantColors.get(selectedVariant);
						Color newColor = JColorChooser.showDialog(panelMap, rb.getString("message_colorChooser"), oldColor);
						if (newColor != null) {
							newColor = Color.getHSBColor(Color.RGBtoHSB(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), null)[0], PlotAreaClassMap.DEFAULT_SATURATION, PlotAreaClassMap.DEFAULT_BRIGHTNESS);
							variantColors.put(variant, newColor);
							tableContentsColor[row][1] = newColor;
							tableVariantColor.setModel(new DefaultTableModel(tableContentsColor, new String[] { rb.getString("columnName1_tableVariantColor"),
									rb.getString("columnName2_tableVariantColor") }) {

								private static final long serialVersionUID = 1L;

								public boolean isCellEditable(int rowIndex, int columnIndex) {
									return false;
								}
							});
							ColorHueTableCellRenderer ctr = new ColorHueTableCellRenderer();
							tableVariantColor.getColumnModel().getColumn(1).setCellRenderer(ctr);
							// draws area class map with new color of variant
							PlotAreaClassMap plot = new PlotAreaClassMap(areaClassMap);
							classmap = true;
							BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);

							try (PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
								if (gridMapType) {
									plot.gridExport(gre, helper, null, variantColors, hints);
								} else {
									plot.voronoiExport(gre, helper, variantColors, hints);
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
				} else {
					System.err.println("tableContentsColor has no TableVariantElement");
				}

			}
		});


		JButton buttonDrawVariantMap = new JButton(rb.getString("text_buttonDrawVariantMap"));
		buttonDrawVariantMap.addActionListener(new ActionListener() {

			/**
			 * ActionListener for pressing button to draw variant map.
			 * 
			 * @param arg0
			 *            Press Button
			 * @return The selected variant in the table is drawn left-hand. If
			 *         a cell in right column is selected, the dominance of
			 *         variant will be drawn in the color, which is used in area
			 *         class map. If a cell in left column is selected, the
			 *         dominance of variant will be drawn in blue.
			 */
			public void actionPerformed(ActionEvent arg0) {
				if (selectedVariant != null) {
					classmap = false;
					VariantMap variantMap = areaClassMap.getVariantMaps().get(selectedVariant);

					if (areaClassMap.getAreas().containsKey(selectedVariant)) {
						DecimalFormat df = new DecimalFormat("0.000");
						labelCharacteristics.setText(rb.getString("compactness") + ": " + df.format(areaClassMap.computeAreaCompactness(selectedVariant)) + ", " + rb.getString("homogeneity") + ": "
								+ df.format(areaClassMap.computeAreaHomogeneity(selectedVariant)));
					} else {
						labelCharacteristics.setText("");
					}

					PlotVariantMap plot = new PlotVariantMap(variantMap, borderPolygon, mapProjection);
					int height = scrollPaneForLabelMap.getSize().height - 25;
					helper = new PlotHelper(borderPolygon, mapProjection, height, 10);
					BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);

					Color variantColor;
					if (tableColumn == 0) {
						variantColor = null;
					} else {
						variantColor = variantColors.get(selectedVariant);
					}

					try (PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
						if (gridMapType) {
							plot.gridExport(gre, helper, null, variantColor, hints);
						} else {
							plot.voronoiExport(gre, helper, variantColor, hints);
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
		buttonDrawVariantMap.setToolTipText(rb.getString("tooltip_buttonDrawVariantMap"));
		GridBagConstraints gbc_buttonDrawVariantMap = new GridBagConstraints();
		gbc_buttonDrawVariantMap.fill = GridBagConstraints.BOTH;
		gbc_buttonDrawVariantMap.insets = new Insets(0, 0, 5, 5);
		gbc_buttonDrawVariantMap.gridx = 2;
		gbc_buttonDrawVariantMap.gridy = 17;
		panelMap.add(buttonDrawVariantMap, gbc_buttonDrawVariantMap);


		buttonSaveMap = new JButton(rb.getString("text_buttonSaveMap"));
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
					String mapName = selectedMap.getString("name");
					mapName = mapName.substring(0, Math.min(mapName.length(), 20));
					mapName = mapName.replaceAll(" ", "_").replaceAll("[\\\\/:*?\"<>|]", "");

					JFileChooser chooser = new JFileChooserConfirmOverwrite(outputfolder + "/" + mapName + ".eps");
					chooser.setFileFilter(new FileNameExtensionFilter(rb.getString("filter_xml_eps_png"), "xml", "eps", "png"));

					if (chooser.showSaveDialog(tabbedPane) == JFileChooser.APPROVE_OPTION) {
						try {
							if (chooser.getSelectedFile().getAbsolutePath().toLowerCase().endsWith(".xml")) {
								areaClassMap.toXML(chooser.getSelectedFile().getAbsolutePath(), gridMapType);
							} else {
								PlotHelper localHelper = new PlotHelper(borderPolygon, mapProjection);
								PlotAreaClassMap plot = new PlotAreaClassMap(areaClassMap);

								if (chooser.getSelectedFile().getAbsolutePath().toLowerCase().endsWith(".png")) {
									BufferedImage bi = new BufferedImage(localHelper.getWidth(), localHelper.getHeight(), BufferedImage.TYPE_INT_RGB);
									try (PlotToGraphics2D gre = new PlotToGraphics2D(localHelper.getWindow(), bi.createGraphics())) {
										if (gridMapType) {
											plot.gridExport(gre, localHelper, null, variantColors, null);
										} else {
											plot.voronoiExport(gre, localHelper, variantColors, null);
										}
									}
									ImageIO.write(bi, "png", new File(chooser.getSelectedFile().getAbsolutePath()));
								} else {
									try (PlotToEPS eps = new PlotToEPS(localHelper.getWindow(), new FileOutputStream(chooser.getSelectedFile().getAbsolutePath()))) {
										if (gridMapType) {
											plot.gridExport(eps, localHelper, null, variantColors, null);
										} else {
											plot.voronoiExport(eps, localHelper, variantColors, null);
										}
									}
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
							JOptionPane.showMessageDialog(panelMap, rb.getString("text_popupSavingError")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupSavingError"), JOptionPane.ERROR_MESSAGE);
						}
					}
				} else {
					String mapName = selectedMap.getString("name");
					mapName = mapName.substring(0, Math.min(mapName.length(), 20));
					mapName = mapName + "_" + selectedVariant.getString("name");
					mapName = mapName.replaceAll(" ", "_").replaceAll("[\\\\/:*?\"<>|]", "");

					JFileChooser chooser = new JFileChooserConfirmOverwrite(outputfolder + "/" + mapName + ".eps");
					FileNameExtensionFilter filter = new FileNameExtensionFilter(rb.getString("filter_eps_png"), "eps", "png");
					chooser.setFileFilter(filter);

					if (chooser.showSaveDialog(tabbedPane) == JFileChooser.APPROVE_OPTION) {
						try {
							PlotHelper localHelper = new PlotHelper(borderPolygon, mapProjection);
							Color variantColor;
							if (tableColumn == 0) {
								variantColor = null;
							} else {
								variantColor = variantColors.get(selectedVariant);
							}
							PlotVariantMap plot = new PlotVariantMap(areaClassMap.getVariantMaps().get(selectedVariant), borderPolygon, mapProjection);

							if (chooser.getSelectedFile().getAbsolutePath().toLowerCase().endsWith(".png")) {
								BufferedImage bi = new BufferedImage(localHelper.getWidth(), localHelper.getHeight(), BufferedImage.TYPE_INT_RGB);
								try (PlotToGraphics2D gre = new PlotToGraphics2D(localHelper.getWindow(), bi.createGraphics())) {
									if (gridMapType) {
										plot.gridExport(gre, localHelper, null, variantColor, null);
									} else {
										plot.voronoiExport(gre, localHelper, variantColor, null);
									}
								}
								ImageIO.write(bi, "png", new File(chooser.getSelectedFile().getAbsolutePath()));
							} else {
								try (PlotToEPS eps = new PlotToEPS(localHelper.getWindow(), new FileOutputStream(chooser.getSelectedFile().getAbsolutePath()))) {
									if (gridMapType) {
										plot.gridExport(eps, localHelper, null, variantColor, null);
									} else {
										plot.voronoiExport(eps, localHelper, variantColor, null);
									}
								}
							}

						} catch (IOException e) {
							e.printStackTrace();
							JOptionPane.showMessageDialog(panelMap, rb.getString("text_popupSavingError")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupSavingError"), JOptionPane.ERROR_MESSAGE);
						}
					}
				}

			}
		});
		buttonSaveMap.setToolTipText(rb.getString("tooltip_buttonSaveMap"));
		GridBagConstraints gbc_buttonSaveMap = new GridBagConstraints();
		gbc_buttonSaveMap.fill = GridBagConstraints.BOTH;
		gbc_buttonSaveMap.insets = new Insets(0, 0, 5, 5);
		gbc_buttonSaveMap.gridx = 2;
		gbc_buttonSaveMap.gridy = 18;
		panelMap.add(buttonSaveMap, gbc_buttonSaveMap);


		buttonCloseMapPanel = new JButton(rb.getString("text_buttonCloseMapPanel"));
		buttonCloseMapPanel.addActionListener(new ActionListener() {

			/**
			 * @param arg0
			 *            Press Button
			 * @return Removes <code>JPanel</code> panel_Map from
			 *         <code>JTabbedPane</code> tabbedPane.
			 */
			public void actionPerformed(ActionEvent arg0) {
				tabbedPane.remove(panelMap);
				tabbedPane.setSelectedIndex(GeoLingGUI.TAB_INDEX_MAPTREE);
			}
		});
		buttonCloseMapPanel.setToolTipText(rb.getString("tooltip_buttonCloseMapPanel"));
		GridBagConstraints gbc_buttonCloseMapPanel = new GridBagConstraints();
		gbc_buttonCloseMapPanel.fill = GridBagConstraints.BOTH;
		gbc_buttonCloseMapPanel.insets = new Insets(0, 0, 0, 5);
		gbc_buttonCloseMapPanel.gridx = 2;
		gbc_buttonCloseMapPanel.gridy = 20;
		panelMap.add(buttonCloseMapPanel, gbc_buttonCloseMapPanel);

	}

	/**
	 * 
	 * @param estimator_identification
	 *            of one of the following estimators:
	 *            LeastSquaresCrossValidation, LikelihoodCrossValidation or
	 *            MinComplexityMaxFidelity
	 * @return ---, if there is no bandwidth in the database for selected
	 *         weight, kernel, distance and estimator, otherwise the BigDecimal
	 *         bandwidth is returned as a string
	 */
	private BigDecimal getBandwidth(String estimator_identification) {
		return Bandwidth.findNumberByIdentificationStr(selectedMap, weights_identification, kernel_identification, distance_identification, estimator_identification);
	}

	/**
	 * Returns the currently selected kernel including distance measure and bandwidth.
	 * 
	 * @return the currently selected kernel
	 */
	private Kernel getKernel() {
		return BuilderMethods.getKernelObj(selectedDistance.getDistanceMeasure(true), selectedBandwidth, kernel_identification);
	}

	/**
	 * Sets text of the three <code>JLabel</code> lblLcvValue, lblLscvValue and
	 * lblClValue and assigns the bandwidth values to the the
	 * <code>BigDecimal</code> variables bandwidthLcv, bandwidthLscv and
	 * bandwidthCl. This method has to be called after every change of one of
	 * the following identification strings: weights_identification,
	 * kernel_identification or distance_identification.
	 */
	private void setBandwidthLabels() {

		BigDecimal bigDecimal = getBandwidth(LikelihoodCrossValidation.getStaticIdentificationString());
		if (bigDecimal == null) {
			lblLcvValue.setText("---");
			if (buttonLcv.isSelected()) {
				buttonGroupBandwidths.clearSelection();
			}
			bandwidthLcv = null;
		} else {
			lblLcvValue.setText(bigDecimal.stripTrailingZeros().toPlainString());
			bandwidthLcv = bigDecimal;
		}

		bigDecimal = getBandwidth(LeastSquaresCrossValidation.getStaticIdentificationString());
		if (bigDecimal == null) {
			lblLscvValue.setText("---");
			if (buttonLscv.isSelected()) {
				buttonGroupBandwidths.clearSelection();
			}
			bandwidthLscv = null;
		} else {
			lblLscvValue.setText(bigDecimal.stripTrailingZeros().toPlainString());
			bandwidthLscv = bigDecimal;
		}

		bigDecimal = getBandwidth(MinComplexityMaxFidelity.getStaticIdentificationString());
		if (bigDecimal == null) {
			lblClValue.setText("---");
			if (buttonCl.isSelected()) {
				buttonGroupBandwidths.clearSelection();
			}
			bandwidthCl = null;
		} else {
			lblClValue.setText(bigDecimal.stripTrailingZeros().toPlainString());
			bandwidthCl = bigDecimal;
		}

		if (!buttonLcv.isSelected() && !buttonLscv.isSelected() && !buttonCl.isSelected()) {
			if (bandwidthLscv != null) {
				buttonLscv.setSelected(true);
			} else if (bandwidthCl != null) {
				buttonCl.setSelected(true);
			} else if (bandwidthLcv != null) {
				buttonLcv.setSelected(true);
			}
		}

		if (buttonLcv.isSelected()) {
			selectedBandwidth = bandwidthLcv;
		} else if (buttonLscv.isSelected()) {
			selectedBandwidth = bandwidthLscv;
		} else if (buttonCl.isSelected()) {
			selectedBandwidth = bandwidthCl;
		}
	}

	/**
	 * Tries to adjust the existing variant colors also for the new/current area-class-map.
	 * Uses the default colors if there would be dominant variants without color otherwise,
	 * and non-dominant variants with colors are converted to a very light/bright color having the
	 * same hue.
	 * 
	 * @param oldVariantColors     the existing colors of variants
	 * @param defaultVariantColors the default colors of dominant-variants in the current map
	 * @param areaClassMap         the current map
	 * @return the variant colors which are applicable to the current map
	 */
	public static HashMap<Variant,Color> getUpdatedVariantColors(HashMap<Variant,Color> oldVariantColors, HashMap<Variant,Color> defaultVariantColors, AreaClassMap areaClassMap) {
		if ((oldVariantColors == null) || !SetComparison.equalSets(defaultVariantColors.keySet(), oldVariantColors.keySet())) {
			// different sets of variants: use default colors
			return defaultVariantColors;
		} else {
			HashMap<Variant,Color> result = oldVariantColors;
			
			// update variant colors with default values only if some variant is now dominant that does not have a color yet
			for (Entry<Variant,Color> entry : defaultVariantColors.entrySet()) {
				if (entry.getValue() != Color.WHITE) {
					Color existingColor = oldVariantColors.get(entry.getKey());
					if (existingColor == null || existingColor == Color.WHITE) {
						result = defaultVariantColors;
						break;
					}
				}
			}
			
			// do not forget the color/hue of the non-dominant variants, but use a very light version of the color in the table
			if (result == oldVariantColors) {
				for (Entry<Variant,Color> entry : result.entrySet()) {
					if (entry.getValue() != Color.WHITE) {
						float saturation;
						if (areaClassMap.getAreas().containsKey(entry.getKey())) {
							saturation = PlotAreaClassMap.DEFAULT_SATURATION;
						} else {
							saturation = 0.2f;
						}
						entry.setValue(Color.getHSBColor(Color.RGBtoHSB(entry.getValue().getRed(), entry.getValue().getGreen(), entry.getValue().getBlue(), null)[0], saturation, PlotAreaClassMap.DEFAULT_BRIGHTNESS));
					}
				}
			}
			
			return result;
		}
	}

}
