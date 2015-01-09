package geoling.gui;

import geoling.config.Database;
import geoling.gui.util.ComboBoxDistanceElement;
import geoling.gui.util.ComboBoxGroupElement;
import geoling.gui.util.ComboBoxLevelElement;
import geoling.gui.util.JDirectoryChooserConfirmNonempty;
import geoling.maps.AreaClassMap;
import geoling.maps.density.KernelDensityEstimation;
import geoling.maps.density.bandwidth.BandwidthEstimator;
import geoling.maps.density.bandwidth.LeastSquaresCrossValidation;
import geoling.maps.density.bandwidth.LikelihoodCrossValidation;
import geoling.maps.density.bandwidth.MinComplexityMaxFidelity;
import geoling.maps.density.bandwidth.computation.ComputeBandwidths;
import geoling.maps.density.kernels.EpanechnikovKernel;
import geoling.maps.density.kernels.GaussianKernel;
import geoling.maps.density.kernels.K3Kernel;
import geoling.maps.density.kernels.Kernel;
import geoling.maps.distances.DistanceMeasure;
import geoling.maps.distances.PrecomputedDistance;
import geoling.maps.plot.PlotAreaClassMap;
import geoling.maps.plot.PlotHelper;
import geoling.maps.projection.MapProjection;
import geoling.maps.projection.MercatorProjection;
import geoling.maps.util.BuilderMethods;
import geoling.maps.weights.VariantWeights;
import geoling.maps.weights.VariantWeightsNoLevel;
import geoling.maps.weights.VariantWeightsWithLevel;
import geoling.models.Bandwidth;
import geoling.models.Border;
import geoling.models.Distance;
import geoling.models.Group;
import geoling.models.Level;
import geoling.models.Map;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.util.plot.PlotToEPS;
import geoling.util.sim.util.plot.PlotToGraphics2D;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.ProgressMonitor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.javalite.activejdbc.LazyList;

/**
 * Panel for map export in dialectometry GUI.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class ExportPanel {

	private JPanel panelExport;

	private JComboBox<ComboBoxGroupElement> comboBoxGroup;
	private JComboBox<ComboBoxLevelElement> comboBoxLevel;
	private JComboBox<ComboBoxDistanceElement> comboBoxDistance;

	private ButtonGroup buttonGroupMapTypes = new ButtonGroup();
	private ButtonGroup buttonGroupKernels = new ButtonGroup();
	private ButtonGroup buttonGroupEstimators = new ButtonGroup();

	private JTextPane textPaneMessages;
	private JProgressBar progressBar;
	private JScrollPane scrollPaneForTextPane;

	private String exportFolder;

	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;

	/**
	 * Create a panel for saving area class maps for a whole groups.
	 * 
	 * @param tabbedPane
	 *            To this <code>JTabbedPane</code> the new panel is added.
	 * @param outputfolder
	 *            Basis folder for output, defined in a property file.
	 */
	public ExportPanel(final JTabbedPane tabbedPane, final String outputfolder) {

		rb = ResourceBundle.getBundle("ExportPanel", GeoLingGUI.LANGUAGE);
		exportFolder = outputfolder;

		tabbedPane.addChangeListener(new ChangeListener() {

			// This method is called whenever the selected tab changes
			public void stateChanged(ChangeEvent evt) {
				JTabbedPane pane = (JTabbedPane) evt.getSource();
				if (pane.getSelectedIndex() == GeoLingGUI.TAB_INDEX_EXPORT) { 
					// Refreshes groups on third tab
					LazyList<Group> allGroups = Group.findAll();
					ComboBoxGroupElement[] groupElements = new ComboBoxGroupElement[allGroups.size()];
					for (int i = 0; i < groupElements.length; i++) {
						Group group = allGroups.get(i);
						groupElements[i] = new ComboBoxGroupElement(group);
					}
					comboBoxGroup.setModel(new DefaultComboBoxModel<ComboBoxGroupElement>(groupElements));
					if (groupElements.length > 0) {
						comboBoxGroup.setSelectedIndex(0);
					}

				}

			}
		});

		panelExport = new JPanel();
		tabbedPane.insertTab(rb.getString("title_ExportPanel"), null, panelExport, null, GeoLingGUI.TAB_INDEX_EXPORT);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 100, 100, 100, 100, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.25, 0.25, 0.25, 0.25, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		panelExport.setLayout(gridBagLayout);


		// label and combo box for group
		JLabel lblGroup = new JLabel(rb.getString("text_lblGroup"));
		GridBagConstraints gbc_lblGroup = new GridBagConstraints();
		gbc_lblGroup.fill = GridBagConstraints.BOTH;
		gbc_lblGroup.insets = new Insets(0, 0, 5, 5);
		gbc_lblGroup.gridx = 0;
		gbc_lblGroup.gridy = 0;
		panelExport.add(lblGroup, gbc_lblGroup);

		comboBoxGroup = new JComboBox<ComboBoxGroupElement>();
		GridBagConstraints gbc_comboBoxGroup = new GridBagConstraints();
		gbc_comboBoxGroup.gridwidth = 2;
		gbc_comboBoxGroup.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxGroup.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxGroup.gridx = 1;
		gbc_comboBoxGroup.gridy = 0;
		panelExport.add(comboBoxGroup, gbc_comboBoxGroup);


		// label and combo box for level
		JLabel lblLevel = new JLabel(rb.getString("text_lblLevel"));
		GridBagConstraints gbc_lblLevel = new GridBagConstraints();
		gbc_lblLevel.insets = new Insets(0, 0, 5, 5);
		gbc_lblLevel.fill = GridBagConstraints.BOTH;
		gbc_lblLevel.gridx = 0;
		gbc_lblLevel.gridy = 1;
		panelExport.add(lblLevel, gbc_lblLevel);

		comboBoxLevel = new JComboBox<ComboBoxLevelElement>();
		LazyList<Level> levels = Level.findAll();
		ComboBoxLevelElement[] levelElements = new ComboBoxLevelElement[levels.size()];
		for (int i = 0; i < levels.size(); i++) {
			levelElements[i] = new ComboBoxLevelElement(levels.get(i));
		}
		comboBoxLevel.setModel(new DefaultComboBoxModel<ComboBoxLevelElement>(levelElements));
		if (levelElements.length > 0) {
			comboBoxLevel.setSelectedIndex(0);
		}
		GridBagConstraints gbc_comboBoxLevel = new GridBagConstraints();
		gbc_comboBoxLevel.gridwidth = 2;
		gbc_comboBoxLevel.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxLevel.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxLevel.gridx = 1;
		gbc_comboBoxLevel.gridy = 1;
		panelExport.add(comboBoxLevel, gbc_comboBoxLevel);


		// label and combo box for distance
		JLabel lblDistance = new JLabel(rb.getString("text_lblDistance"));
		GridBagConstraints gbc_lblDistance = new GridBagConstraints();
		gbc_lblDistance.insets = new Insets(0, 0, 5, 5);
		gbc_lblDistance.fill = GridBagConstraints.BOTH;
		gbc_lblDistance.gridx = 0;
		gbc_lblDistance.gridy = 2;
		panelExport.add(lblDistance, gbc_lblDistance);

		comboBoxDistance = new JComboBox<ComboBoxDistanceElement>();
		LazyList<Distance> distances = Distance.findAll();
		ComboBoxDistanceElement[] distanceElements = new ComboBoxDistanceElement[distances.size()];
		for (int i = 0; i < distances.size(); i++) {
			distanceElements[i] = new ComboBoxDistanceElement(distances.get(i));
		}
		comboBoxDistance.setModel(new DefaultComboBoxModel<ComboBoxDistanceElement>(distanceElements));
		if (distanceElements.length > 0) {
			comboBoxDistance.setSelectedIndex(0);
		}
		GridBagConstraints gbc_comboBoxDistance = new GridBagConstraints();
		gbc_comboBoxDistance.gridwidth = 2;
		gbc_comboBoxDistance.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxDistance.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxDistance.gridx = 1;
		gbc_comboBoxDistance.gridy = 2;
		panelExport.add(comboBoxDistance, gbc_comboBoxDistance);


		// radio buttons for plotting type
		JLabel lblMapType = new JLabel(rb.getString("text_lblMapType"));
		GridBagConstraints gbc_lblMapType = new GridBagConstraints();
		gbc_lblMapType.fill = GridBagConstraints.BOTH;
		gbc_lblMapType.insets = new Insets(0, 0, 5, 5);
		gbc_lblMapType.gridx = 0;
		gbc_lblMapType.gridy = 3;
		panelExport.add(lblMapType, gbc_lblMapType);

		JRadioButton radioButtonVoronoi = new JRadioButton(rb.getString("text_radioButtonVoronoi"));
		radioButtonVoronoi.setActionCommand("Voronoi");
		buttonGroupMapTypes.add(radioButtonVoronoi);
		radioButtonVoronoi.setSelected(true);
		GridBagConstraints gbc_radioButtonVoronoi = new GridBagConstraints();
		gbc_radioButtonVoronoi.fill = GridBagConstraints.BOTH;
		gbc_radioButtonVoronoi.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonVoronoi.gridx = 1;
		gbc_radioButtonVoronoi.gridy = 3;
		panelExport.add(radioButtonVoronoi, gbc_radioButtonVoronoi);

		JRadioButton radioButtonGrid = new JRadioButton(rb.getString("text_radioButtonGrid"));
		radioButtonGrid.setActionCommand("Grid");
		buttonGroupMapTypes.add(radioButtonGrid);
		GridBagConstraints gbc_radioButtonGrid = new GridBagConstraints();
		gbc_radioButtonGrid.fill = GridBagConstraints.BOTH;
		gbc_radioButtonGrid.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonGrid.gridx = 2;
		gbc_radioButtonGrid.gridy = 3;
		panelExport.add(radioButtonGrid, gbc_radioButtonGrid);


		// radio buttons for kernels
		JLabel lblKernel = new JLabel(rb.getString("text_lblKernel"));
		GridBagConstraints gbc_lblKernel = new GridBagConstraints();
		gbc_lblKernel.fill = GridBagConstraints.BOTH;
		gbc_lblKernel.insets = new Insets(0, 0, 5, 5);
		gbc_lblKernel.gridx = 0;
		gbc_lblKernel.gridy = 4;
		panelExport.add(lblKernel, gbc_lblKernel);

		JRadioButton radioButtonGauss = new JRadioButton(rb.getString("Gauss"));
		radioButtonGauss.setActionCommand(GaussianKernel.getStaticIdentificationString());
		buttonGroupKernels.add(radioButtonGauss);
		radioButtonGauss.setSelected(true);
		GridBagConstraints gbc_radioButtonGauss = new GridBagConstraints();
		gbc_radioButtonGauss.fill = GridBagConstraints.BOTH;
		gbc_radioButtonGauss.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonGauss.gridx = 1;
		gbc_radioButtonGauss.gridy = 4;
		panelExport.add(radioButtonGauss, gbc_radioButtonGauss);

		JRadioButton radioButtonK3 = new JRadioButton(rb.getString("K3"));
		radioButtonK3.setActionCommand(K3Kernel.getStaticIdentificationString());
		buttonGroupKernels.add(radioButtonK3);
		GridBagConstraints gbc_radioButtonK3 = new GridBagConstraints();
		gbc_radioButtonK3.fill = GridBagConstraints.BOTH;
		gbc_radioButtonK3.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonK3.gridx = 2;
		gbc_radioButtonK3.gridy = 4;
		panelExport.add(radioButtonK3, gbc_radioButtonK3);

		JRadioButton radioButtonEpanechnikov = new JRadioButton(rb.getString("Epanechnikov"));
		radioButtonEpanechnikov.setActionCommand(EpanechnikovKernel.getStaticIdentificationString());
		buttonGroupKernels.add(radioButtonEpanechnikov);
		GridBagConstraints gbc_radioButtonEpanechnikov = new GridBagConstraints();
		gbc_radioButtonEpanechnikov.fill = GridBagConstraints.BOTH;
		gbc_radioButtonEpanechnikov.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonEpanechnikov.gridx = 3;
		gbc_radioButtonEpanechnikov.gridy = 4;
		panelExport.add(radioButtonEpanechnikov, gbc_radioButtonEpanechnikov);


		// radio buttons for estimators
		JLabel lblBandwidth = new JLabel(rb.getString("text_lblBandwidth"));
		GridBagConstraints gbc_lblBandwidth = new GridBagConstraints();
		gbc_lblBandwidth.fill = GridBagConstraints.BOTH;
		gbc_lblBandwidth.insets = new Insets(0, 0, 5, 5);
		gbc_lblBandwidth.gridx = 0;
		gbc_lblBandwidth.gridy = 5;
		panelExport.add(lblBandwidth, gbc_lblBandwidth);

		JRadioButton radioButtonLcv = new JRadioButton(rb.getString("LCV"));
		radioButtonLcv.setActionCommand(LikelihoodCrossValidation.getStaticIdentificationString());
		buttonGroupEstimators.add(radioButtonLcv);
		radioButtonLcv.setSelected(true);
		GridBagConstraints gbc_radioButtonLcv = new GridBagConstraints();
		gbc_radioButtonLcv.fill = GridBagConstraints.BOTH;
		gbc_radioButtonLcv.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonLcv.gridx = 1;
		gbc_radioButtonLcv.gridy = 5;
		panelExport.add(radioButtonLcv, gbc_radioButtonLcv);

		JRadioButton radioButtonLscv = new JRadioButton(rb.getString("LSCV"));
		radioButtonLscv.setActionCommand(LeastSquaresCrossValidation.getStaticIdentificationString());
		buttonGroupEstimators.add(radioButtonLscv);
		GridBagConstraints gbc_radioButtonLscv = new GridBagConstraints();
		gbc_radioButtonLscv.fill = GridBagConstraints.BOTH;
		gbc_radioButtonLscv.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonLscv.gridx = 2;
		gbc_radioButtonLscv.gridy = 5;
		panelExport.add(radioButtonLscv, gbc_radioButtonLscv);

		JRadioButton radioButtonCl = new JRadioButton(rb.getString("MinCMaxL"));
		radioButtonCl.setActionCommand(MinComplexityMaxFidelity.getStaticIdentificationString());
		buttonGroupEstimators.add(radioButtonCl);
		GridBagConstraints gbc_radioButtonCl = new GridBagConstraints();
		gbc_radioButtonCl.fill = GridBagConstraints.BOTH;
		gbc_radioButtonCl.insets = new Insets(0, 0, 5, 0);
		gbc_radioButtonCl.gridx = 3;
		gbc_radioButtonCl.gridy = 5;
		panelExport.add(radioButtonCl, gbc_radioButtonCl);

		JButton buttonStartExport = new JButton(rb.getString("text_buttonStartExport"));
		buttonStartExport.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				if (progressBar.isIndeterminate()) {
					// old export thread is still running, break
					return;
				}
				textPaneMessages.setText("");

				JFileChooser jfc = new JDirectoryChooserConfirmNonempty(exportFolder);
				jfc.setDialogTitle(rb.getString("title_outputFolderChooser"));
				if (jfc.showOpenDialog(tabbedPane) == JFileChooser.APPROVE_OPTION) {
					exportFolder = jfc.getSelectedFile().getAbsolutePath();
				} else {
					return;
				}

				final Group group = (comboBoxGroup.getSelectedIndex() >= 0) ? ((ComboBoxGroupElement) comboBoxGroup.getSelectedItem()).getGroup() : null;
				LazyList<Map> mapsLazy = (group != null) ? group.getAll(Map.class) : null;
				if (mapsLazy == null) {
					// no group: fallback to all maps
					mapsLazy = Map.findAll();
				}
				final ArrayList<Map> maps = new ArrayList<Map>(mapsLazy);

				try {
					final DistanceMeasure distance  = ((ComboBoxDistanceElement) comboBoxDistance.getSelectedItem()).getDistance().getDistanceMeasure(true);
					String kernel_identification    = buttonGroupKernels.getSelection().getActionCommand();
					final Kernel kernel             = BuilderMethods.getKernelObj(distance, null, kernel_identification);
					String estimator_identification = buttonGroupEstimators.getSelection().getActionCommand();
					final BandwidthEstimator estimator = BuilderMethods.getBandwidthEstimatorObj(kernel, estimator_identification);

					// cache global values from this panel, user may change them in
					// the GUI while map generation is still running
					final String kernel_identificationCached = kernel_identification;
					final Level level = (comboBoxLevel.getSelectedIndex() >= 0) ? ((ComboBoxLevelElement) comboBoxLevel.getSelectedItem()).getLevel() : null;
					final boolean gridMapTypeCached = buttonGroupMapTypes.getSelection().getActionCommand().equals("Voronoi") ? false : true;
					final String exportFolderCached = exportFolder;

					if (gridMapTypeCached && (kernel.getDistanceMeasure() instanceof PrecomputedDistance)) {
						JOptionPane.showMessageDialog(panelExport, rb.getString("text_popupWrongDistance"), rb.getString("title_popupWrongDistance"), JOptionPane.ERROR_MESSAGE);
						return;
					}

					// start background thread, this way the GUI will be updated
					// automatically and is usable
					Thread thread = new Thread(new Runnable() {

						public void run() {
							progressBar.setIndeterminate(true);

							// database connection is missing in this worker
							// thread...
							Database.ensureConnection();

							try {
								// generate a ProgressMonitor object (window with
								// progress bar and "cancel"-button)
								ProgressMonitor pm = new ProgressMonitor(panelExport, rb.getString("text_exportRunning"), "", 0, maps.size());

								// writer for csv export
								String groupName = (group != null) ? group.getString("name") : "all_maps";
								groupName = groupName.substring(0, Math.min(groupName.length(), 20));
								groupName = groupName.replaceAll(" ", "_").replaceAll("[\\\\/:*?\"<>|]", "");
								BufferedWriter writer = null;
								try {
									writer = new BufferedWriter(new FileWriter(exportFolderCached+"/characteristics_"+groupName+".csv"));
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

										VariantWeights variantWeights = (level != null) ? new VariantWeightsWithLevel(map, level) : new VariantWeightsNoLevel(map);
										BigDecimal selectedBandwidth = Bandwidth.findNumberByIdentificationObj(variantWeights, estimator);

										if (pm.isCanceled()) {
											writer.close();
											return;
										}

										if (selectedBandwidth == null) {
											textPaneMessages.setText(textPaneMessages.getText()
													+ String.format(rb.getString("message_estimateBandwidth"), map.getString("name")) + "\n");
											selectedBandwidth = ComputeBandwidths.findOrComputeAndSaveBandwidth(variantWeights, estimator, false, null);
										}

										if (pm.isCanceled()) {
											writer.close();
											return;
										}

										textPaneMessages.setText(textPaneMessages.getText()
												+ String.format(rb.getString("message_createMap"), map.getString("name"), selectedBandwidth.stripTrailingZeros())
												+ "\n");

										Kernel kernel = BuilderMethods.getKernelObj(distance, selectedBandwidth, kernel_identificationCached);

										AreaClassMap areaClassMap = new AreaClassMap(variantWeights, new KernelDensityEstimation(kernel));

										String mapName = areaClassMap.getMap().getString("name").replaceAll(" ", "_");
										mapName = mapName.replaceAll("[\\\\/:*?\"<>|]", "");
										mapName = mapName.substring(0, Math.min(mapName.length(), 31));

										try {
											Polytope borderPolygon = Border.getDefaultBorder().toPolygon();
											MapProjection mapProjection = new MercatorProjection();

											areaClassMap.buildLocationDensityCache();
											areaClassMap.buildAreas(borderPolygon, mapProjection);
											PlotHelper helper = new PlotHelper(borderPolygon, mapProjection);
											PlotAreaClassMap plot = new PlotAreaClassMap(areaClassMap);

											if (pm.isCanceled()) {
												writer.close();
												return;
											}

											try (PlotToEPS eps = new PlotToEPS(helper.getWindow(), new FileOutputStream(exportFolderCached + "/" + mapName + ".eps"))) {
												if (gridMapTypeCached) {
													plot.gridExport(eps, helper, null, null, null);
												} else {
													plot.voronoiExport(eps, helper, null, null);
												}
											}

											BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);
											try (PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
												if (gridMapTypeCached) {
													plot.gridExport(gre, helper, null, null, null);
												} else {
													plot.voronoiExport(gre, helper, null, null);
												}
											}
											ImageIO.write(bi, "png", new File(exportFolderCached + "/" + mapName + ".png"));

											areaClassMap.toXML(exportFolderCached + "/" + mapName + ".xml", gridMapTypeCached);

											writer.write(map.getString("name")+";");
											writer.write(areaClassMap.computeTotalBorderLength()+";");
											writer.write(areaClassMap.computeOverallHomogeneity()+";");
											writer.write(areaClassMap.computeOverallAreaCompactness()+";");
											writer.write(areaClassMap.getNumberOfAreas()+"");
											writer.write(System.getProperty("line.separator"));
										} catch (IOException e) {
											e.printStackTrace();
											textPaneMessages.setText(textPaneMessages.getText()
													+ String.format(rb.getString("message_createMapError"), map.getString("name"), (e.getMessage() != null ? e.getMessage() : e)) + "\n");
										}
									}

									writer.close();
								}
								catch (IOException e) {
									e.printStackTrace();
									textPaneMessages.setText(textPaneMessages.getText()	+ rb.getString("message_writeCSVError") + (e.getMessage() != null ? e.getMessage() : e) + "\n");
								}	
							}
							finally {
								progressBar.setIndeterminate(false);
							}
						}
					});
					thread.start();

				} catch (LeastSquaresCrossValidation.LeastSquaresCrossValidationNotSupportedException e) {
					JOptionPane.showMessageDialog(panelExport, rb.getString("text_popupWrongKernel"), rb.getString("title_popupWrongKernel"), JOptionPane.WARNING_MESSAGE);
				}

			}
		});
		buttonStartExport.setToolTipText(rb.getString("tooltip_buttonStartExport"));
		GridBagConstraints gbc_buttonStartExport = new GridBagConstraints();
		gbc_buttonStartExport.insets = new Insets(0, 0, 5, 5);
		gbc_buttonStartExport.fill = GridBagConstraints.BOTH;
		gbc_buttonStartExport.gridx = 1;
		gbc_buttonStartExport.gridy = 6;
		panelExport.add(buttonStartExport, gbc_buttonStartExport);

		progressBar = new JProgressBar();
		GridBagConstraints gbc_progressBar = new GridBagConstraints();
		gbc_progressBar.insets = new Insets(5, 0, 5, 0);
		gbc_progressBar.fill = GridBagConstraints.BOTH;
		gbc_progressBar.gridx = 0;
		gbc_progressBar.gridy = 7;
		gbc_progressBar.gridwidth = 4;
		panelExport.add(progressBar, gbc_progressBar);

		scrollPaneForTextPane = new JScrollPane();
		GridBagConstraints gbc_scrollPaneForTextPane = new GridBagConstraints();
		gbc_scrollPaneForTextPane.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPaneForTextPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneForTextPane.gridx = 0;
		gbc_scrollPaneForTextPane.gridy = 8;
		gbc_scrollPaneForTextPane.gridwidth = 4;
		panelExport.add(scrollPaneForTextPane, gbc_scrollPaneForTextPane);

		textPaneMessages = new JTextPane();
		scrollPaneForTextPane.setViewportView(textPaneMessages);

	}

}
