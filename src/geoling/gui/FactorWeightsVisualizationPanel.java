package geoling.gui;

import geoling.config.Database;
import geoling.factor_analysis.util.ReconstructedVariantWeights;
import geoling.gui.util.ColorHueTableCellRenderer;
import geoling.gui.util.ComboBoxMapElement;
import geoling.gui.util.AreaClassMapLabel;
import geoling.gui.util.JDirectoryChooserConfirmNonempty;
import geoling.gui.util.JFileChooserConfirmOverwrite;
import geoling.gui.util.TableVariantElement;
import geoling.locations.util.AggregatedLocation;
import geoling.maps.AreaClassMap;
import geoling.maps.VariantMap;
import geoling.maps.density.WeightPassthrough;
import geoling.maps.plot.PlotAreaClassMap;
import geoling.maps.plot.PlotHelper;
import geoling.maps.plot.PlotVariantMap;
import geoling.maps.projection.MapProjection;
import geoling.maps.projection.MercatorProjection;
import geoling.maps.weights.VariantWeights;
import geoling.models.Border;
import geoling.models.Group;
import geoling.models.Map;
import geoling.models.Variant;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.util.plot.PlotToEPS;
import geoling.util.sim.util.plot.PlotToGraphics2D;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.ResourceBundle;










import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ProgressMonitor;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.javalite.activejdbc.LazyList;

/**
 * Panel to draw maps for the passed group, where reconstructed variant weights from factor analysis are used.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class FactorWeightsVisualizationPanel {

	/** <code>JPanel</code> for the contents. */
	private JPanel panelFactorWeightsVisualization;

	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;

	private Map selectedMap;

	private JScrollPane scrollPaneForLabelMap;
	private ImageIcon iconMap;
	private AreaClassMapLabel labelMap;


	private JScrollPane scrollPaneForTableVariantColor;
	private JTable tableVariantColor;

	private JButton buttonDrawMap;
	private JButton buttonSaveMap;
	private JButton buttonExportGroup;

	private AreaClassMap areaClassMap;
	private HashMap<Variant, Color> variantColors;
	private Object[][] tableContentsColor;
	private Variant selectedVariant;
	private int tableColumn;
	private PlotHelper helper;
	private boolean classmap = false;
	private HashMap<Polytope, AggregatedLocation> hints = new HashMap<Polytope, AggregatedLocation>();

	/**
	 * Create a panel for drawing a map with variant weights reconstructed from factor analysis.
	 * 
	 * @param tabbedPane  To this <code>JTabbedPane</code> the new panel is added.
	 * @param outputfolder  Basis folder for output, defined in a property file.
	 * @param selectedGroup the group for which factor analysis was done
	 * @param weights a <code>HashMap</code> which saves the reconstructed weights for each map
	 */
	public FactorWeightsVisualizationPanel(final JTabbedPane tabbedPane, final String outputfolder, final Group selectedGroup, final HashMap<Map, ReconstructedVariantWeights> weights) {

		rb = ResourceBundle.getBundle("FactorWeightsVisualizationPanel", GeoLingGUI.LANGUAGE);

		panelFactorWeightsVisualization = new JPanel();
		tabbedPane.addTab(rb.getString("title_FactorWeightsVisualizationPanel"), null, panelFactorWeightsVisualization, selectedGroup.getString("name"));
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

		// define layout
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 300, 50, 50, 0 };
		gridBagLayout.rowHeights = new int[] { 40, 40, 40, 100, 40, 0 };
		gridBagLayout.columnWeights = new double[] { 0.8, 0.1, 0.1, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.1, 0.1, 0.1, 0.6, 0.1, Double.MIN_VALUE };
		panelFactorWeightsVisualization.setLayout(gridBagLayout);

		final Polytope borderPolygon = Border.getDefaultBorder().toPolygon();
		final MapProjection mapProjection = new MercatorProjection();

		// combo box to choose map
		JLabel lblMap = new JLabel(rb.getString("text_lblMap"));
		GridBagConstraints gbc_lblMap = new GridBagConstraints();
		gbc_lblMap.fill = GridBagConstraints.BOTH;
		gbc_lblMap.anchor = GridBagConstraints.EAST;
		gbc_lblMap.insets = new Insets(0, 0, 5, 5);
		gbc_lblMap.gridx = 1;
		gbc_lblMap.gridy = 0;
		panelFactorWeightsVisualization.add(lblMap, gbc_lblMap);

		JComboBox<ComboBoxMapElement> comboBoxMap = new JComboBox<ComboBoxMapElement>();
		LazyList<Map> maps = selectedGroup.getAll(Map.class);
		ComboBoxMapElement[] mapElements = new ComboBoxMapElement[maps.size()];
		for (int i = 0; i < maps.size(); i++) {
			mapElements[i] = new ComboBoxMapElement(maps.get(i));
		}
		comboBoxMap.setModel(new DefaultComboBoxModel<ComboBoxMapElement>(mapElements));
		// first initialization of selectedMap
		if (comboBoxMap.getItemCount() > 0) {
			comboBoxMap.setSelectedIndex(0);
			selectedMap = ((ComboBoxMapElement) comboBoxMap.getItemAt(0)).getMap();
		}
		comboBoxMap.setPrototypeDisplayValue(new ComboBoxMapElement(null));

		comboBoxMap.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					ComboBoxMapElement element = (ComboBoxMapElement) e.getItem();
					selectedMap = element.getMap();
				}
			}
		});
		GridBagConstraints gbc_comboBoxMap = new GridBagConstraints();
		gbc_comboBoxMap.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxMap.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxMap.gridx = 2;
		gbc_comboBoxMap.gridy = 0;
		panelFactorWeightsVisualization.add(comboBoxMap, gbc_comboBoxMap);



		scrollPaneForLabelMap = new JScrollPane();
		GridBagConstraints gbc_scrollPaneForLabelMap = new GridBagConstraints();
		gbc_scrollPaneForLabelMap.gridheight = 4;
		gbc_scrollPaneForLabelMap.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPaneForLabelMap.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneForLabelMap.gridx = 0;
		gbc_scrollPaneForLabelMap.gridy = 0;

		labelMap = new AreaClassMapLabel();
		scrollPaneForLabelMap.setViewportView(labelMap);
		panelFactorWeightsVisualization.add(scrollPaneForLabelMap, gbc_scrollPaneForLabelMap);

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
		gbc_sliderZoom.gridy = 4;
		panelFactorWeightsVisualization.add(sliderZoom, gbc_sliderZoom);



		buttonDrawMap = new JButton(rb.getString("text_buttonDrawMap"));
		buttonDrawMap.setToolTipText(rb.getString("tooltip_buttonDrawMap"));
		buttonDrawMap.addActionListener(new ActionListener() {

			/**
			 * Draws map in <code>JPanel</code> labelMap
			 * @param arg0   Press Button <code>buttonDrawMap</code>
			 */
			public void actionPerformed(ActionEvent arg0) {

				areaClassMap = new AreaClassMap(weights.get(selectedMap), new WeightPassthrough());
				areaClassMap.buildLocationDensityCache();
				areaClassMap.buildAreas(borderPolygon, mapProjection);

				int height = scrollPaneForLabelMap.getSize().height - 25;
				helper = new PlotHelper(borderPolygon, mapProjection, height, 10);
				PlotAreaClassMap plot = new PlotAreaClassMap(areaClassMap);
				classmap = true;

				variantColors = MapPanel.getUpdatedVariantColors(variantColors, plot.getDefaultAreaColors(false), areaClassMap);

				BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);
				try (PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
					plot.voronoiExport(gre, helper, variantColors, hints);
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
		});
		GridBagConstraints gbc_buttonDrawMap = new GridBagConstraints();
		gbc_buttonDrawMap.fill = GridBagConstraints.BOTH;
		gbc_buttonDrawMap.insets = new Insets(0, 0, 5, 5);
		gbc_buttonDrawMap.gridx = 1;
		gbc_buttonDrawMap.gridy = 1;
		panelFactorWeightsVisualization.add(buttonDrawMap, gbc_buttonDrawMap);

		scrollPaneForTableVariantColor = new JScrollPane();
		GridBagConstraints gbc_scrollPaneForTableVariantColor = new GridBagConstraints();
		gbc_scrollPaneForTableVariantColor.gridwidth = 2;
		gbc_scrollPaneForTableVariantColor.gridheight = 2;
		gbc_scrollPaneForTableVariantColor.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneForTableVariantColor.gridx = 1;
		gbc_scrollPaneForTableVariantColor.gridy = 3;
		panelFactorWeightsVisualization.add(scrollPaneForTableVariantColor, gbc_scrollPaneForTableVariantColor);

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
						Color newColor = JColorChooser.showDialog(null, rb.getString("message_colorChooser"), oldColor);
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
							JOptionPane.showMessageDialog(panelFactorWeightsVisualization, rb.getString("text_popupSavingError")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupSavingError"), JOptionPane.ERROR_MESSAGE);
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
									plot.voronoiExport(gre, localHelper, variantColor, null);
								}
								ImageIO.write(bi, "png", new File(chooser.getSelectedFile().getAbsolutePath()));
							} else {
								try (PlotToEPS eps = new PlotToEPS(localHelper.getWindow(), new FileOutputStream(chooser.getSelectedFile().getAbsolutePath()))) {
									plot.voronoiExport(eps, localHelper, variantColor, null);
								}
							}

						} catch (IOException e) {
							e.printStackTrace();
							JOptionPane.showMessageDialog(panelFactorWeightsVisualization, rb.getString("text_popupSavingError")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupSavingError"), JOptionPane.ERROR_MESSAGE);
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
		gbc_buttonSaveMap.gridy = 1;
		panelFactorWeightsVisualization.add(buttonSaveMap, gbc_buttonSaveMap);

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
						plot.voronoiExport(gre, helper, variantColor, hints);
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
		gbc_buttonDrawVariantMap.gridx = 1;
		gbc_buttonDrawVariantMap.gridy = 2;
		panelFactorWeightsVisualization.add(buttonDrawVariantMap, gbc_buttonDrawVariantMap);

		buttonExportGroup = new JButton(rb.getString("text_buttonExportGroup"));
		buttonExportGroup.addActionListener(new ActionListener() {

			/**
			 * @param arg0  Press Button to start export
			 */
			public void actionPerformed(ActionEvent arg0) {

				JFileChooser jfc = new JDirectoryChooserConfirmNonempty(outputfolder);
				jfc.setDialogTitle(rb.getString("title_outputFolderChooser"));
				if (jfc.showOpenDialog(tabbedPane) == JFileChooser.APPROVE_OPTION) {
					final String exportFolder = jfc.getSelectedFile().getAbsolutePath();

					Thread thread = new Thread(new Runnable() {

						public void run() {

							Database.ensureConnection();

							try {
								LazyList<Map> maps  = selectedGroup.getAll(Map.class);

								// generate a ProgressMonitor object (window with
								// progress bar and "cancel"-button)
								ProgressMonitor pm = new ProgressMonitor(panelFactorWeightsVisualization, rb.getString("text_exportRunning"), "", 0, maps.size());


								// writer for csv export
								String groupName = selectedGroup.getString("name");
								groupName = groupName.substring(0, Math.min(groupName.length(), 20));
								groupName = groupName.replaceAll(" ", "_").replaceAll("[\\\\/:*?\"<>|]", "");
								BufferedWriter writer = null;

								writer = new BufferedWriter(new FileWriter(exportFolder+"/reconstructed_weights_characteristics_"+groupName+".csv"));
								writer.write("map_name;total_border_length;overall_homogeneity;overall_area_compactness;number_dominant_variants");
								writer.write(System.getProperty("line.separator"));

								for (int i = 0; i < maps.size(); i++) {
									Map map = maps.get(i);
									pm.setNote(map.getString("name"));
									pm.setProgress(i + 1);

									if (pm.isCanceled()) {
										writer.close();
										return;
									}

									VariantWeights variantWeights = weights.get(maps.get(i));


									AreaClassMap areaClassMap = new AreaClassMap(variantWeights, new WeightPassthrough());

									String mapName = areaClassMap.getMap().getString("name").replaceAll(" ", "_");
									mapName = mapName.replaceAll("[\\\\/:*?\"<>|]", "");
									mapName = mapName.substring(0, Math.min(mapName.length(), 31));

									Polytope borderPolygon = Border.getDefaultBorder().toPolygon();
									MapProjection mapProjection = new MercatorProjection();

									areaClassMap.buildLocationDensityCache();
									areaClassMap.buildAreas(borderPolygon, mapProjection);
									PlotHelper helper = new PlotHelper(borderPolygon, mapProjection);
									PlotAreaClassMap plot = new PlotAreaClassMap(areaClassMap);

									try (PlotToEPS eps = new PlotToEPS(helper.getWindow(), new FileOutputStream(exportFolder + "/reconstructed_weights_" + mapName + ".eps"))) {
										plot.voronoiExport(eps, helper, null, null);
									}

									BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);
									try (PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
										plot.voronoiExport(gre, helper, null, null);
									}
									ImageIO.write(bi, "png", new File(exportFolder + "/reconstructed_weights_" + mapName + ".png"));

									areaClassMap.toXML(exportFolder + "/reconstructed_weights_" + mapName + ".xml", false);

									writer.write(map.getString("name")+";");
									writer.write(areaClassMap.computeTotalBorderLength()+";");
									writer.write(areaClassMap.computeOverallHomogeneity()+";");
									writer.write(areaClassMap.computeOverallAreaCompactness()+";");
									writer.write(areaClassMap.getNumberOfAreas()+"");
									writer.write(System.getProperty("line.separator"));


								}
								writer.close();

							}
							catch (IOException e) {
								e.printStackTrace();
								JOptionPane.showMessageDialog(panelFactorWeightsVisualization, rb.getString("text_popupErrorExport")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupErrorExport"), JOptionPane.ERROR_MESSAGE);
							}
						}
					});

					thread.start();
				}
			}
		});
		buttonExportGroup.setToolTipText(rb.getString("tooltip_buttonExportGroup"));
		GridBagConstraints gbc_buttonExportGroup = new GridBagConstraints();
		gbc_buttonExportGroup.fill = GridBagConstraints.BOTH;
		gbc_buttonExportGroup.insets = new Insets(0, 0, 5, 5);
		gbc_buttonExportGroup.gridx = 2;
		gbc_buttonExportGroup.gridy = 2;
		panelFactorWeightsVisualization.add(buttonExportGroup, gbc_buttonExportGroup);
	}
}


