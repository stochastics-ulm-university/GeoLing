package geoling.gui;

import geoling.config.Database;
import geoling.gui.util.ComboBoxDistanceElement;
import geoling.gui.util.ComboBoxGroupElement;
import geoling.gui.util.ComboBoxLevelElement;
import geoling.gui.util.JFileChooserConfirmOverwrite;
import geoling.maps.AreaClassMap;
import geoling.maps.clustering.data.*;
import geoling.maps.clustering.distances.*;
import geoling.maps.density.DensityEstimation;
import geoling.maps.density.KernelDensityEstimation;
import geoling.maps.density.bandwidth.BandwidthEstimator;
import geoling.maps.density.bandwidth.LeastSquaresCrossValidation;
import geoling.maps.density.bandwidth.LikelihoodCrossValidation;
import geoling.maps.density.bandwidth.MinComplexityMaxFidelity;
import geoling.maps.density.bandwidth.computation.ComputeBandwidths;
import geoling.maps.density.kernels.*;
import geoling.maps.distances.*;
import geoling.maps.projection.KilometresProjection;
import geoling.maps.util.BuilderMethods;
import geoling.maps.util.MapBorder;
import geoling.maps.util.RectangularGrid;
import geoling.maps.weights.VariantWeights;
import geoling.maps.weights.VariantWeightsNoLevel;
import geoling.maps.weights.VariantWeightsWithLevel;
import geoling.models.Distance;
import geoling.models.Group;
import geoling.models.Level;
import geoling.models.Location;
import geoling.models.Map;
import geoling.util.ThreadedTodoWorker;
import geoling.util.XMLExport;
import geoling.util.clusteranalysis.ClusterAnalysis;
import geoling.util.clusteranalysis.ClusteringResult;
import geoling.util.clusteranalysis.distance.EuclideanDistance;
import geoling.util.clusteranalysis.linkage.CentroidMethod;
import geoling.util.clusteranalysis.linkage.LinkageMethod;
import geoling.util.clusteranalysis.linkage.WardsMethod;
import geoling.util.clusteranalysis.methods.AgglomerativeHierarchicalClustering;
import geoling.util.clusteranalysis.methods.FuzzyCMeansClusteringExtended;
import geoling.util.clusteranalysis.methods.FuzzyCMeansClusteringImproved;
import geoling.util.clusteranalysis.methods.KMeansClusteringExtended;
import geoling.util.clusteranalysis.methods.KMeansClusteringImproved;
import geoling.util.clusteranalysis.termination.DistanceVariabilityThreshold;
import geoling.util.clusteranalysis.termination.NumberOfClusters;
import geoling.util.clusteranalysis.termination.TerminationCriterion;
import geoling.util.sim.grain.Polytope;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.ProgressMonitor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.xml.stream.XMLStreamException;

import org.javalite.activejdbc.LazyList;

/**
 * Panel for cluster analysis in dialectometry GUI.
 * 
 * @author Raphael Wimmer (partially based on previous work), Institute of Stochastics, Ulm University
 */
public class ClusterAnalysisPanel {


	/** <code>JPanel</code> for the contents. */
	private JPanel panelClusterAnalysis;
	private JScrollPane scrollPaneForOutput;
	private ProgressMonitor pm;


	// general options
	private JComboBox<String> comboBoxWhichAnalysis;
	private int selectedAnalysisInt;

	private JComboBox<ComboBoxLevelElement> comboBoxLevel;
	private Level selectedLevel;

	private JComboBox<ComboBoxGroupElement> comboBoxGroup;
	private Group selectedGroup;

	private JComboBox<ComboBoxDistanceElement> comboBoxWhichDistanceMeasure;
	private DistanceMeasure selectedDistanceMeasure;

	private JComboBox<String> comboBoxKernel;
	private Kernel selectedKernel;

	private JComboBox<String> comboBoxEstimatorIdentification;
	private String selectedEstimatorIdentification;


	// options for agglomerative clustering
	private JComboBox<String> comboBoxMapDistance;
	private MapDistance selectedMapDistance;
	private String selectedMapDistanceIdentification;
	private boolean useCovarianceFunction = false;

	private JComboBox<String> comboBoxClusterDistance;
	private LinkageMethod selectedClusterDistance;
	private String selectedClusterDistanceIdentification;

	private JTextField textFieldTerminationThresholdK;
	private JTextField textFieldTerminationNumberOfClusters;
	private String selectedTerminationCriterion;
	private double selectedTerminationValue;

	private Component[] componentsAgglomerativeClustering;
	private ButtonGroup buttonGroupAgglomerative;


	// options for Fuzzy CMeans
	private JTextField textFieldNumberOfClustersFuzzy;
	private int selectedNumberOfClustersFuzzy;
	private boolean computeNumberOfClustersAutomaticallyFuzzy;

	private JTextField textFieldExponentM;
	private double selectedExponentM;

	private JTextField textFieldEpsilon;
	private double selectedEpsilon;

	private Component[] componentsFuzzyCMeansClustering;
	private ButtonGroup buttonGroupFuzzy;


	// options for K-Means
	private JTextField textFieldNumberOfClustersKMeans;
	private int selectedNumberOfClustersKMeans;
	private boolean computeNumberOfClustersAutomaticallyKMeans;

	private Component[] componentsKMeansClustering;
	private ButtonGroup buttonGroupKMeans;

	private MapClusteringResult result;

	private Object[][] tableData;
	private JTable table;
	private ArrayList<AreaClassMap> tableAreaClassMaps;

	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;

	/**
	 * 
	 * @param tabbedPane
	 *            To this <code>JTabbedPane</code> the new panel is added.
	 * @param outputfolder
	 *            The folder where files will be saved by default.
	 */
	public ClusterAnalysisPanel(final JTabbedPane tabbedPane, final String outputfolder) {

		rb = ResourceBundle.getBundle("ClusterAnalysisPanel", GeoLingGUI.LANGUAGE);
		panelClusterAnalysis = new JPanel();
		tabbedPane.addTab(rb.getString("title_ClusterAnalysisPanel"), null, panelClusterAnalysis, null);

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
		gridBagLayout.columnWidths = new int[] { 200, 150, 150, 0 };
		gridBagLayout.rowHeights = new int[] { 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 100 };
		gridBagLayout.columnWeights = new double[] { 0.9, 0.1, 0.1, 0.1, Double.MIN_VALUE };

		panelClusterAnalysis.setLayout(gridBagLayout);

		initializeGeneralComponents();

		initializeComponentsOfAgglomerativeClustering();

		// initializes the Button which makes the Cluster Analysis (*) and the button which saves the results
		final JButton buttonSaveResults = new JButton(rb.getString("text_buttonSaveResults"));
		buttonSaveResults.setEnabled(false);

		JButton buttonDoClusterAnalysis = new JButton(rb.getString("text_buttonDoClusterAnalysis"));
		buttonDoClusterAnalysis.addActionListener(new ActionListener() {

			/**
			 * If you Press the Button the cluster analysis will be done for the
			 * defined Parameters.
			 */
			public void actionPerformed(ActionEvent arg0) {

				// check if correct distance measure is selected
				if (selectedAnalysisInt == 0 && useCovarianceFunction) { // agglomerative clustering
					if (!(selectedDistanceMeasure instanceof GeographicalDistance)) {
						JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupGeographicDistanceAgglomerative"), rb.getString("title_popupGeographicDistance"), JOptionPane.WARNING_MESSAGE);
						return;
					}
				}
				if (selectedAnalysisInt == 1) { // fuzzy c-means
					if (!(selectedDistanceMeasure instanceof GeographicalDistance)) {
						JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupGeographicDistanceFuzzy"), rb.getString("title_popupGeographicDistance"), JOptionPane.WARNING_MESSAGE);
						return;
					}
				}
				if (selectedAnalysisInt == 2) { // k-means clustering
					if (!(selectedDistanceMeasure instanceof GeographicalDistance)) {
						JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupGeographicDistanceKMeans"), rb.getString("title_popupGeographicDistance"), JOptionPane.WARNING_MESSAGE);
						return;
					}
				}

				Thread thread = new Thread(new Runnable() {

					public void run() {

						Database.ensureConnection();

						pm = new ProgressMonitor(panelClusterAnalysis, rb.getString("text_clusterAnalysisRunning"), "", 0, 100);
						try {
							pm.setMillisToDecideToPopup(0);
							pm.setMillisToPopup(0);
							pm.setProgress(0);

							String[] columnNames;


							if (selectedAnalysisInt == 0) { // agglomerative clustering

								// create object with TerminationCriterion, parse value from text field
								TerminationCriterion termination = null;
								if (selectedTerminationCriterion.equals(rb.getString("text_radioButtonTerminationDistanceVariability"))) {
									try {
										selectedTerminationValue = Double.parseDouble(textFieldTerminationThresholdK.getText());
										termination = new DistanceVariabilityThreshold(selectedTerminationValue);
									}
									catch (NumberFormatException e) {
										pm.close();
										JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupWrongK")
												, rb.getString("title_popupWrongFormat"), JOptionPane.WARNING_MESSAGE);
										return;
									}

								}
								else {
									try {
										selectedTerminationValue = Integer.parseInt(textFieldTerminationNumberOfClusters.getText());
										termination = new NumberOfClusters((int) selectedTerminationValue);
									}
									catch (NumberFormatException e) {
										pm.close();
										JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupWrongNumber")
												, rb.getString("title_popupWrongFormat"), JOptionPane.WARNING_MESSAGE);
										return;
									}
								}

								ArrayList<AreaClassMap> areaClassMaps = createAreaClassMaps();
								if (areaClassMaps == null || areaClassMaps.isEmpty()) {
									pm.close();
									return;
								}
								ArrayList<MapClusterObject> clusterObjects = parseAreaClassMapsToObjects(areaClassMaps);
								if (pm.isCanceled()) {
									return;
								}
								if (useCovarianceFunction) {
									clusterObjects = computeCovarianceFunctionForClusterObjects(clusterObjects);
									if (pm.isCanceled()) {
										return;
									}
								}
								ClusterAnalysis clustering = new AgglomerativeHierarchicalClustering(selectedClusterDistance, termination);
								result = new MapClusteringResult(performClusterAnalysis(clustering, clusterObjects));

								if (pm.isCanceled()) {
									return;
								}

								columnNames = new String[] { rb.getString("resulting_Column_Name_Cluster"),
										rb.getString("resulting_Column_Name_Name") };
								tableData = new Object[areaClassMaps.size()][2];
								tableAreaClassMaps = new ArrayList<AreaClassMap>();

								int k = 0;
								for (int i = 0; i < result.getClusterCount(); i++) {
									for (MapClusterObject mapClusterObject : result.getCluster(i)) {
										AreaClassMap areaClassMap = mapClusterObject.getAreaClassMap();
										tableData[k][0] = rb.getString("resulting_Column_Name_Cluster") + " " + (i + 1);
										tableData[k][1] = areaClassMap.getMap().getString("name");
										tableAreaClassMaps.add(areaClassMap);
										k++;
									}
								}

							} else if (selectedAnalysisInt == 1) { // fuzzy c-means

								// parse values from text fields
								try {
									selectedExponentM = Double.parseDouble(textFieldExponentM.getText());
									selectedEpsilon = Double.parseDouble(textFieldEpsilon.getText());
								}
								catch (NumberFormatException e) {
									pm.close();
									JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupWrongEpsilonExponent")
											, rb.getString("title_popupWrongFormat"), JOptionPane.WARNING_MESSAGE);
									return;
								}

								if (!computeNumberOfClustersAutomaticallyFuzzy) {
									try {
										selectedNumberOfClustersFuzzy = Integer.parseInt(textFieldNumberOfClustersFuzzy.getText());
									}
									catch (NumberFormatException e) {
										pm.close();
										JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupWrongNumber")
												, rb.getString("title_popupWrongFormat"), JOptionPane.WARNING_MESSAGE);
										return;
									}			
								}
								ArrayList<AreaClassMap> areaClassMaps = createAreaClassMaps();
								if (areaClassMaps == null || areaClassMaps.isEmpty()) {
									pm.close();
									return;
								}
								ArrayList<MapClusterObject> clusterObjects = parseAreaClassMapsToObjects(areaClassMaps);
								if (pm.isCanceled()) {
									return;
								}
								clusterObjects = computeCovarianceFunctionForClusterObjects(clusterObjects);
								if (pm.isCanceled()) {
									return;
								}

								ClusterAnalysis clustering = null;
								if (computeNumberOfClustersAutomaticallyFuzzy) {
									clustering = new FuzzyCMeansClusteringExtended(selectedExponentM, selectedEpsilon, -1, -1, 5, 5, pm);
								}
								else {
									clustering = new FuzzyCMeansClusteringImproved(selectedNumberOfClustersFuzzy, selectedExponentM, selectedEpsilon, 10);
								}
								result = new MapClusteringResult(performClusterAnalysis(clustering, clusterObjects));

								if (pm.isCanceled()) {
									return;
								}

								columnNames = new String[] { rb.getString("resulting_Column_Name_Cluster"),
										rb.getString("resulting_Column_Name_Name"),
										rb.getString("resulting_Column_Name_Prob") };
								tableData = new Object[result.getClusterCount() * areaClassMaps.size()][3];
								tableAreaClassMaps = new ArrayList<AreaClassMap>();

								int k = 0;
								for (int i = 0; i < result.getClusterCount(); i++) {
									for (MapClusterObject mapClusterObject : result.getCluster(i)) {
										AreaClassMap areaClassMap = mapClusterObject.getAreaClassMap();
										tableData[k][0] = rb.getString("resulting_Column_Name_Cluster") + " " + (i + 1);
										tableData[k][1] = areaClassMap.getMap().getString("name");
										tableData[k][2] = result.getObjectInClusterProbability(i, mapClusterObject);
										tableAreaClassMaps.add(areaClassMap);
										k++;
									}
								}
							}

							else { // k-means clustering

								if (!computeNumberOfClustersAutomaticallyKMeans) {
									try {
										selectedNumberOfClustersKMeans = Integer.parseInt(textFieldNumberOfClustersKMeans.getText());
									}
									catch (NumberFormatException e) {
										pm.close();
										JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupWrongNumber")
												, rb.getString("title_popupWrongFormat"), JOptionPane.WARNING_MESSAGE);
										return;
									}			
								}
								ArrayList<AreaClassMap> areaClassMaps = createAreaClassMaps();
								if (areaClassMaps == null || areaClassMaps.isEmpty()) {
									pm.close();
									return;
								}
								ArrayList<MapClusterObject> clusterObjects = parseAreaClassMapsToObjects(areaClassMaps);
								if (pm.isCanceled()) {
									return;
								}
								clusterObjects = computeCovarianceFunctionForClusterObjects(clusterObjects);
								if (pm.isCanceled()) {
									return;
								}

								ClusterAnalysis clustering = null;
								if (computeNumberOfClustersAutomaticallyKMeans) {
									clustering = new KMeansClusteringExtended(-1, -1, 5, 5, 5, pm);
								}
								else {
									clustering = new KMeansClusteringImproved(selectedNumberOfClustersKMeans, 5, 5);
								}
								result = new MapClusteringResult(performClusterAnalysis(clustering, clusterObjects));

								if (pm.isCanceled()) {
									return;
								}

								columnNames = new String[] { rb.getString("resulting_Column_Name_Cluster"),
										rb.getString("resulting_Column_Name_Name")};
								tableData = new Object[result.getClusterCount() * areaClassMaps.size()][2];
								tableAreaClassMaps = new ArrayList<AreaClassMap>();

								int k = 0;
								for (int i = 0; i < result.getClusterCount(); i++) {
									for (MapClusterObject mapClusterObject : result.getCluster(i)) {
										AreaClassMap areaClassMap = mapClusterObject.getAreaClassMap();
										tableData[k][0] = rb.getString("resulting_Column_Name_Cluster") + " " + (i + 1);
										tableData[k][1] = areaClassMap.getMap().getString("name");
										tableAreaClassMaps.add(areaClassMap);
										k++;
									}
								}

							}

							// Make the table's content visible

							table = new JTable();
							table.setModel(new DefaultTableModel(tableData, columnNames) {
								private static final long serialVersionUID = 1L;

								public boolean isCellEditable(int rowIndex, int columnIndex) {
									return false;
								}
							});

							table.addMouseListener(new MouseAdapter() {
								public void mouseClicked(MouseEvent arg0) {
									if (arg0.getClickCount() == 2) {
										int row = table.getSelectedRow();
										if (row >= 0) {
											AreaClassMap areaClassMap = tableAreaClassMaps.get(row);
											if (areaClassMap != null) {
												new MapPanel(tabbedPane, outputfolder, areaClassMap.getMap());
											}
										}
									}
								}
							});

							scrollPaneForOutput.getViewport().setView(table);

							// Now you are able to press the save-button and the pressing will cause an action.
							buttonSaveResults.setEnabled(true);

						} catch (Exception e) {
							pm.close();
							e.printStackTrace();
							JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupErrorClustering")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupErrorClustering"), JOptionPane.ERROR_MESSAGE);
							return;
						}

						//						 Visualize results (in this thread, i.e., in background). 
						try {
							if (result.isFuzzy()) {
								String[] possibilities = {rb.getString("option1_dialogueVisualizeFuzzy"),
										rb.getString("option2_dialogueVisualizeFuzzy"),
										rb.getString("option3_dialogueVisualizeFuzzy"),
										rb.getString("option4_dialogueVisualizeFuzzy")};
								String s = (String)JOptionPane.showInputDialog(
										panelClusterAnalysis, rb.getString("message_dialogueVisualizeFuzzy"),
										rb.getString("title_dialogueVisualizeFuzzy"),
										JOptionPane.PLAIN_MESSAGE,
										null, possibilities, possibilities[0]);
								if(s!=null){
									boolean hard = false;
									boolean voronoiMaps = false;
									if (s.equalsIgnoreCase(possibilities[0])){
										voronoiMaps = true;
									} else if (s.equalsIgnoreCase(possibilities[2])){
										hard = true;
										voronoiMaps = true;
									} else if (s.equalsIgnoreCase(possibilities[3])){
										hard = true;
									}

									MapClusteringResult tmpResult;
									if (hard) {
										tmpResult = new MapClusteringResult(result.clusterResult.getHardResult());
									} else {
										tmpResult = result;
									}

									new ClusterVisualizationPanel(tabbedPane, tmpResult, voronoiMaps);	
								}
							} else {
								String[] possibilities = {rb.getString("option1_dialogueVisualizeHard"),
										rb.getString("option2_dialogueVisualizeHard")};
								String s = (String)JOptionPane.showInputDialog(
										panelClusterAnalysis, rb.getString("message_dialogueVisualizeHard"),
										rb.getString("title_dialogueVisualizeHard"),
										JOptionPane.PLAIN_MESSAGE,
										null, possibilities, possibilities[0]);
								if(s!=null){
									new ClusterVisualizationPanel(tabbedPane, result, possibilities[0].equalsIgnoreCase(s));	
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
							JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupErrorVisualization")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupErrorVisualization"), JOptionPane.ERROR_MESSAGE);
							return;
						}
					}
				});

				thread.start();

			}
		});

		GridBagConstraints gbc_buttonDoClusterAnalysis = new GridBagConstraints();
		gbc_buttonDoClusterAnalysis.fill = GridBagConstraints.BOTH;
		gbc_buttonDoClusterAnalysis.insets = new Insets(0, 0, 5, 5);
		gbc_buttonDoClusterAnalysis.gridx = 1;
		gbc_buttonDoClusterAnalysis.gridy = 12;
		panelClusterAnalysis.add(buttonDoClusterAnalysis, gbc_buttonDoClusterAnalysis);

		// button for saving the data
		buttonSaveResults.addActionListener(new ActionListener() {

			/**
			 * If you Press the Button the results of cluster analysis will be exported.
			 */
			public void actionPerformed(ActionEvent arg0) {

				JFileChooser chooser = new JFileChooserConfirmOverwrite(outputfolder + "/cluster_analysis.xml");
				FileNameExtensionFilter filter = new FileNameExtensionFilter(rb.getString("filter_xml_csv"), "xml", "csv");
				chooser.setFileFilter(filter);

				if (chooser.showSaveDialog(tabbedPane) == JFileChooser.APPROVE_OPTION) {

					String selectedFile = chooser.getSelectedFile().getAbsolutePath();

					// write xml
					if (selectedFile.endsWith(".xml")) {
						try {
							XMLExport writer = new XMLExport(selectedFile);

							writer.XML.writeStartElement("clusteranalysis");

							writer.XML.writeStartElement("data");

							writer.XML.writeStartElement("maps");
							List<Map> maps;
							if (selectedGroup != null) {
								writer.XML.writeAttribute("group_id", selectedGroup.getId().toString());
								maps = selectedGroup.getAll(Map.class);
							} else {
								// no group: fallback to all maps
								maps = Map.findAll();
							}
							for (Map map : maps) {
								writer.XML.writeStartElement("map");
								writer.XML.writeAttribute("id", map.getId().toString());
								writer.XML.writeAttribute("name", map.getString("name"));
								writer.XML.writeEndElement();
							}
							writer.XML.writeEndElement(); // </maps>

							writer.XML.writeStartElement("densities");
							writer.XML.writeAttribute("estimation_type", KernelDensityEstimation.getStaticIdentificationString());
							writer.XML.writeStartElement("option");
							writer.XML.writeAttribute("distances", selectedDistanceMeasure.getIdentificationString());
							writer.XML.writeEndElement();
							writer.XML.writeStartElement("option");
							writer.XML.writeAttribute("kernel", selectedKernel.getIdentificationStringWithoutParameters());
							writer.XML.writeEndElement();
							writer.XML.writeStartElement("option");
							writer.XML.writeAttribute("bandwidth_estimator", selectedEstimatorIdentification);
							writer.XML.writeEndElement();
							writer.XML.writeEndElement(); // </densities>

							writer.XML.writeEndElement(); // </data>

							writer.XML.writeStartElement("options");
							if (selectedAnalysisInt == 0) { // agglomerative clustering
								writer.XML.writeStartElement("option");
								writer.XML.writeAttribute("clustering_type", "hard");
								writer.XML.writeEndElement();
								writer.XML.writeStartElement("option");
								writer.XML.writeAttribute("clustering_method", "agglomerative_hierarchical");
								writer.XML.writeEndElement();
								writer.XML.writeStartElement("option");
								writer.XML.writeAttribute("map_distance", selectedMapDistanceIdentification);
								writer.XML.writeEndElement();
								writer.XML.writeStartElement("option");
								writer.XML.writeAttribute("cluster_distance", selectedClusterDistanceIdentification);
								writer.XML.writeEndElement();
								writer.XML.writeStartElement("option");
								if (selectedTerminationCriterion.equals(rb.getString("text_radioButtonTerminationDistanceVariability"))) {
									writer.XML.writeAttribute("k_for_termination_threshold", Double.toString(selectedTerminationValue));
								}
								else {
									writer.XML.writeAttribute("number_of_clusters_for_termination", Double.toString(selectedTerminationValue));
								}
								writer.XML.writeEndElement();
							} 
							else if (selectedAnalysisInt == 1) { // Fuzzy C-Means
								writer.XML.writeStartElement("option");
								writer.XML.writeAttribute("clustering_type", "fuzzy");
								writer.XML.writeEndElement();
								writer.XML.writeStartElement("option");
								writer.XML.writeAttribute("clustering_method", "fuzzy_c_means");
								writer.XML.writeEndElement();
								writer.XML.writeStartElement("option");
								writer.XML.writeAttribute("exponent_m", Double.toString(selectedExponentM));
								writer.XML.writeEndElement();
								writer.XML.writeStartElement("option");
								writer.XML.writeAttribute("epsilon", Double.toString(selectedEpsilon));
								writer.XML.writeEndElement();
							}
							else { // K-Means
								writer.XML.writeStartElement("option");
								writer.XML.writeAttribute("clustering_type", "k-means");
								writer.XML.writeEndElement();
								writer.XML.writeStartElement("option");
								writer.XML.writeAttribute("clustering_method", "k-means");
								writer.XML.writeEndElement();
							}
							writer.XML.writeEndElement(); // </options>

							writer.XML.writeStartElement("clusters");
							for (int i = 0; i < result.getClusterCount(); i++) {
								writer.XML.writeStartElement("cluster");
								writer.XML.writeAttribute("index", Integer.toString(i));
								for (MapClusterObject mapClusterObject : result.getCluster(i)) {
									AreaClassMap areaClassMap = mapClusterObject.getAreaClassMap();
									writer.XML.writeStartElement("map");
									writer.XML.writeAttribute("id", areaClassMap.getMap().getId().toString());
									writer.XML.writeAttribute("probability", Double.toString(result.getObjectInClusterProbability(i, mapClusterObject)));
									writer.XML.writeEndElement();
								}
								writer.XML.writeEndElement(); // </cluster>
							}
							writer.XML.writeEndElement(); // </clusters>

							writer.XML.writeEndElement(); // </clusteranalysis>

							writer.close();



						} catch (IOException | XMLStreamException e) {
							e.printStackTrace();
							JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupXMLError")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupXMLError"), JOptionPane.ERROR_MESSAGE);
						}
					}
					// write csv file
					else if (selectedFile.endsWith(".csv")) {
						try {						
							BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFile));
							if (tableData[0].length==2) {
								// write header
								bw.write("cluster;map_name");
								bw.write(System.getProperty("line.separator"));
								// write rows of table
								for (int i=0; i<tableData.length; i++) {
									bw.write(tableData[i][0]+";"+tableData[i][1]);
									bw.write(System.getProperty("line.separator"));
								}
							}
							// fuzzy result
							if (tableData[0].length==3) {
								// write header
								bw.write("cluster;map_name;probability");
								bw.write(System.getProperty("line.separator"));
								// write rows of table
								for (int i=0; i<tableData.length; i++) {
									bw.write(tableData[i][0]+";"+tableData[i][1]+";"+tableData[i][2]);
									bw.write(System.getProperty("line.separator"));
								}
							}

							bw.close();
						}
						catch (IOException e) {
							e.printStackTrace();
							JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupCSVError")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupCSVError"), JOptionPane.ERROR_MESSAGE);
						}
					}
					else {
						JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupUnknownExtension"), rb.getString("title_popupUnknownExtension"), JOptionPane.WARNING_MESSAGE);
					}



				}
			}
		});

		GridBagConstraints gbc_buttonSaveResults = new GridBagConstraints();
		gbc_buttonSaveResults.fill = GridBagConstraints.BOTH;
		gbc_buttonSaveResults.insets = new Insets(0, 0, 5, 5);
		gbc_buttonSaveResults.gridx = 2;
		gbc_buttonSaveResults.gridy = 12;
		panelClusterAnalysis.add(buttonSaveResults, gbc_buttonSaveResults);

	}



	/**
	 * Initializes combo boxes which are required for all clustering methods.
	 */
	private void initializeGeneralComponents() {
		// initialize scroll pane for table of results on the left
		scrollPaneForOutput = new JScrollPane(table);
		GridBagConstraints gbc_scrollPaneForOutput = new GridBagConstraints();
		gbc_scrollPaneForOutput.gridheight = 40;
		gbc_scrollPaneForOutput.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPaneForOutput.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneForOutput.gridx = 0;
		gbc_scrollPaneForOutput.gridy = 0;
		panelClusterAnalysis.add(scrollPaneForOutput, gbc_scrollPaneForOutput);

		// initialize combo box to select type of clustering
		JLabel lblWhichAnalysis = new JLabel(rb.getString("text_lblWhichAnalysis"));
		lblWhichAnalysis.setToolTipText(rb.getString("tooltip_lblWhichAnalysis"));
		GridBagConstraints gbc_lblWhichAnalysis = new GridBagConstraints();
		gbc_lblWhichAnalysis.fill = GridBagConstraints.BOTH;
		gbc_lblWhichAnalysis.insets = new Insets(0, 0, 5, 5);
		gbc_lblWhichAnalysis.anchor = GridBagConstraints.EAST;
		gbc_lblWhichAnalysis.gridx = 1;
		gbc_lblWhichAnalysis.gridy = 0;
		panelClusterAnalysis.add(lblWhichAnalysis, gbc_lblWhichAnalysis);

		comboBoxWhichAnalysis = new JComboBox<String>(new String[] { rb.getString("text_Agglomerative"), rb.getString("text_Fuzzy"), rb.getString("text_KMeans") });

		selectedAnalysisInt = 0;
		comboBoxWhichAnalysis.addItemListener(new ItemListener() {		
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (e.getItem().toString().equals(rb.getString("text_Agglomerative"))) {
						if (selectedAnalysisInt==1) {
							removeComponentsOfFuzzyCMeansClustering();
						}
						if (selectedAnalysisInt==2) {
							removeComponentsOfKMeansClustering();
						}
						initializeComponentsOfAgglomerativeClustering();
						selectedAnalysisInt = 0; // 0 for agglomerative
					} else if (e.getItem().toString().equals(rb.getString("text_Fuzzy"))) {
						if (selectedAnalysisInt == 0) {
							removeComponentsOfAgglomerativeClustering();
						}
						if (selectedAnalysisInt == 2) {
							removeComponentsOfKMeansClustering();
						}
						initializeComponentsOfFuzzyCMeansClustering();
						selectedAnalysisInt = 1; // 1 for fuzzy
					}
					else {
						if (selectedAnalysisInt == 0) {
							removeComponentsOfAgglomerativeClustering();
						}
						if (selectedAnalysisInt == 1) {
							removeComponentsOfFuzzyCMeansClustering();
						}
						initializeComponentsOfKMeansClustering();
						selectedAnalysisInt = 2; // 2 for k-means
					}

				}
			}
		});

		GridBagConstraints gbc_comboBoxWhichAnalysis = new GridBagConstraints();
		gbc_comboBoxWhichAnalysis.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxWhichAnalysis.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxWhichAnalysis.gridx = 2;
		gbc_comboBoxWhichAnalysis.gridy = 0;
		gbc_comboBoxWhichAnalysis.gridwidth = 2;
		panelClusterAnalysis.add(comboBoxWhichAnalysis, gbc_comboBoxWhichAnalysis);



		// initialize combo box to select level
		JLabel lblLevel = new JLabel(rb.getString("text_lblLevel"));
		lblLevel.setToolTipText(rb.getString("tooltip_lblLevel"));
		GridBagConstraints gbc_lblLevel = new GridBagConstraints();
		gbc_lblLevel.fill = GridBagConstraints.BOTH;
		gbc_lblLevel.insets = new Insets(0, 0, 5, 5);
		gbc_lblLevel.anchor = GridBagConstraints.EAST;
		gbc_lblLevel.gridx = 1;
		gbc_lblLevel.gridy = 1;
		panelClusterAnalysis.add(lblLevel, gbc_lblLevel);

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
			selectedLevel = ((ComboBoxLevelElement) comboBoxLevel.getItemAt(0)).getLevel();
		}

		comboBoxLevel.addItemListener(new ItemListener() {	
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					ComboBoxLevelElement element = (ComboBoxLevelElement) e.getItem(); // ComboBoxLevelElement
					selectedLevel = element.getLevel();
				}
			}
		});
		GridBagConstraints gbc_comboBoxLevel = new GridBagConstraints();
		gbc_comboBoxLevel.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxLevel.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxLevel.gridx = 2;
		gbc_comboBoxLevel.gridy = 1;
		gbc_comboBoxLevel.gridwidth = 2;
		panelClusterAnalysis.add(comboBoxLevel, gbc_comboBoxLevel);


		// initialize combo box to select group
		JLabel lblGroup = new JLabel(rb.getString("text_lblGroup"));
		lblGroup.setToolTipText(rb.getString("tooltip_lblGroup"));
		GridBagConstraints gbc_lblGroup = new GridBagConstraints();
		gbc_lblGroup.fill = GridBagConstraints.BOTH;
		gbc_lblGroup.insets = new Insets(0, 0, 5, 5);
		gbc_lblGroup.anchor = GridBagConstraints.EAST;
		gbc_lblGroup.gridx = 1;
		gbc_lblGroup.gridy = 2;
		panelClusterAnalysis.add(lblGroup, gbc_lblGroup);

		comboBoxGroup = new JComboBox<ComboBoxGroupElement>();
		LazyList<Group> groups = Group.findAll();
		ComboBoxGroupElement[] groupElements = new ComboBoxGroupElement[groups.size()];
		for (int i = 0; i < groups.size(); i++) {
			groupElements[i] = new ComboBoxGroupElement(groups.get(i));
		}

		//		ComboBoxGroupElement tmp = groupElements[0];
		//		groupElements[0] = groupElements[1];
		//		groupElements[1] = tmp;
		comboBoxGroup.setModel(new DefaultComboBoxModel<ComboBoxGroupElement>(groupElements));
		// first initialization of selectedGroup
		if (comboBoxGroup.getItemCount() > 0) {
			comboBoxGroup.setSelectedIndex(0);
			selectedGroup = ((ComboBoxGroupElement) comboBoxGroup.getItemAt(0)).getGroup();
		}

		comboBoxGroup.addItemListener(new ItemListener() {		
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					ComboBoxGroupElement element = (ComboBoxGroupElement) e.getItem();
					selectedGroup = element.getGroup();
				}
			}
		});
		GridBagConstraints gbc_comboBoxGroup = new GridBagConstraints();
		gbc_comboBoxGroup.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxGroup.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxGroup.gridx = 2;
		gbc_comboBoxGroup.gridy = 2;
		gbc_comboBoxGroup.gridwidth = 2;
		panelClusterAnalysis.add(comboBoxGroup, gbc_comboBoxGroup);


		// initialize combo box to select type of distance measure (e.g. geographical, linguistic)
		JLabel lblWhichDistanceMeasure = new JLabel(rb.getString("text_lblWhichDistanceMeasure"));
		lblWhichDistanceMeasure.setToolTipText(rb.getString("tooltip_lblWhichDistanceMeasure"));
		GridBagConstraints gbc_lblWhichDistanceMeasure = new GridBagConstraints();
		gbc_lblWhichDistanceMeasure.fill = GridBagConstraints.BOTH;
		gbc_lblWhichDistanceMeasure.insets = new Insets(0, 0, 5, 5);
		gbc_lblWhichDistanceMeasure.anchor = GridBagConstraints.EAST;
		gbc_lblWhichDistanceMeasure.gridx = 1;
		gbc_lblWhichDistanceMeasure.gridy = 3;
		panelClusterAnalysis.add(lblWhichDistanceMeasure, gbc_lblWhichDistanceMeasure);

		comboBoxWhichDistanceMeasure = new JComboBox<ComboBoxDistanceElement>();
		LazyList<Distance> distances = Distance.findAll();
		ComboBoxDistanceElement[] distanceElements = new ComboBoxDistanceElement[distances.size()];
		for (int i = 0; i < distances.size(); i++) {
			distanceElements[i] = new ComboBoxDistanceElement(distances.get(i));
		}
		comboBoxWhichDistanceMeasure.setModel(new DefaultComboBoxModel<ComboBoxDistanceElement>(distanceElements));
		comboBoxWhichDistanceMeasure.setSelectedIndex(0);		
		selectedDistanceMeasure = new GeographicalDistance();
		comboBoxWhichDistanceMeasure.addItemListener(new ItemListener() {		
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					ComboBoxDistanceElement element = (ComboBoxDistanceElement) e.getItem();
					selectedDistanceMeasure = element.getDistance().getDistanceMeasure(true);
				}
			}
		});

		GridBagConstraints gbc_comboBoxWhichDistanceMeasure = new GridBagConstraints();
		gbc_comboBoxWhichDistanceMeasure.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxWhichDistanceMeasure.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxWhichDistanceMeasure.gridx = 2;
		gbc_comboBoxWhichDistanceMeasure.gridy = 3;
		gbc_comboBoxWhichDistanceMeasure.gridwidth = 2;
		panelClusterAnalysis.add(comboBoxWhichDistanceMeasure, gbc_comboBoxWhichDistanceMeasure);


		// initialize combo box to select kernel type
		JLabel lblKernel = new JLabel(rb.getString("text_lblKernel"));
		lblKernel.setToolTipText(rb.getString("tooltip_lblKernel"));
		GridBagConstraints gbc_lblKernel = new GridBagConstraints();
		gbc_lblKernel.fill = GridBagConstraints.BOTH;
		gbc_lblKernel.insets = new Insets(0, 0, 5, 5);
		gbc_lblKernel.anchor = GridBagConstraints.EAST;
		gbc_lblKernel.gridx = 1;
		gbc_lblKernel.gridy = 4;
		panelClusterAnalysis.add(lblKernel, gbc_lblKernel);

		comboBoxKernel = new JComboBox<String>(new String[] { rb.getString("Gauss"), rb.getString("Epanechnikov"), rb.getString("K3") });

		selectedKernel = new GaussianKernel(selectedDistanceMeasure, null);
		comboBoxKernel.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (e.getItem().toString().equals(rb.getString("K3"))) {
						selectedKernel = new K3Kernel(selectedDistanceMeasure, null);
					}
					if (e.getItem().toString().equals(rb.getString("Epanechnikov"))) {
						selectedKernel = new EpanechnikovKernel(selectedDistanceMeasure, null);
					}
					if (e.getItem().toString().equals(rb.getString("Gauss"))) {
						selectedKernel = new GaussianKernel(selectedDistanceMeasure, null);
					}

				}

			}

		});

		GridBagConstraints gbc_comboBoxKernel = new GridBagConstraints();
		gbc_comboBoxKernel.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxKernel.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxKernel.gridx = 2;
		gbc_comboBoxKernel.gridy = 4;
		gbc_comboBoxKernel.gridwidth = 2;
		panelClusterAnalysis.add(comboBoxKernel, gbc_comboBoxKernel);


		// initialize combo box to select estimator
		JLabel lblEstimatorIdentification = new JLabel(rb.getString("text_lblEstimatorIdentification"));
		lblEstimatorIdentification.setToolTipText(rb.getString("tooltip_lblEstimatorIdentification"));
		GridBagConstraints gbc_lblEstimatorIdentification = new GridBagConstraints();
		gbc_lblEstimatorIdentification.fill = GridBagConstraints.BOTH;
		gbc_lblEstimatorIdentification.insets = new Insets(0, 0, 5, 5);
		gbc_lblEstimatorIdentification.anchor = GridBagConstraints.EAST;
		gbc_lblEstimatorIdentification.gridx = 1;
		gbc_lblEstimatorIdentification.gridy = 5;
		panelClusterAnalysis.add(lblEstimatorIdentification, gbc_lblEstimatorIdentification);

		comboBoxEstimatorIdentification = new JComboBox<String>(new String[] { rb.getString("LCV"), rb.getString("LSCV"), rb.getString("MinCMaxL") });

		selectedEstimatorIdentification = LikelihoodCrossValidation.getStaticIdentificationString();
		comboBoxEstimatorIdentification.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (e.getItem().toString().equals(rb.getString("LCV"))) {
						selectedEstimatorIdentification = LikelihoodCrossValidation.getStaticIdentificationString();
					} else if (e.getItem().toString().equals(rb.getString("LSCV"))) {
						selectedEstimatorIdentification = LeastSquaresCrossValidation.getStaticIdentificationString();
					} else if (e.getItem().toString().equals(rb.getString("MinCMaxL"))) {
						selectedEstimatorIdentification = MinComplexityMaxFidelity.getStaticIdentificationString();
					}
				}
			}
		});

		GridBagConstraints gbc_comboBoxEstimatorIdentification = new GridBagConstraints();
		gbc_comboBoxEstimatorIdentification.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxEstimatorIdentification.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxEstimatorIdentification.gridx = 2;
		gbc_comboBoxEstimatorIdentification.gridy = 5;
		gbc_comboBoxEstimatorIdentification.gridwidth = 2;
		panelClusterAnalysis.add(comboBoxEstimatorIdentification, gbc_comboBoxEstimatorIdentification);
	}


	/**
	 * Initializes components required for options of agglomerative clustering.
	 */
	private void initializeComponentsOfAgglomerativeClustering() {
		componentsAgglomerativeClustering = new Component[10];
		// label
		JLabel lblClusteringOptions = new JLabel(rb.getString("text_lblClusteringOptionsAgglomerative"));
		GridBagConstraints gbc_lblClusteringOptions = new GridBagConstraints();
		gbc_lblClusteringOptions.fill = GridBagConstraints.BOTH;
		gbc_lblClusteringOptions.insets = new Insets(0, 0, 5, 5);
		gbc_lblClusteringOptions.anchor = GridBagConstraints.EAST;
		gbc_lblClusteringOptions.gridx = 1;
		gbc_lblClusteringOptions.gridy = 6;
		gbc_lblClusteringOptions.gridwidth = 3;
		panelClusterAnalysis.add(lblClusteringOptions, gbc_lblClusteringOptions);
		componentsAgglomerativeClustering[0] = lblClusteringOptions;

		// initialize combo box to select distance between maps
		JLabel lblMapDistance = new JLabel(rb.getString("text_lblMapDistance"));
		lblMapDistance.setToolTipText(rb.getString("tooltip_lblMapDistance"));
		GridBagConstraints gbc_lblMapDistance = new GridBagConstraints();
		gbc_lblMapDistance.fill = GridBagConstraints.BOTH;
		gbc_lblMapDistance.insets = new Insets(0, 0, 5, 5);
		gbc_lblMapDistance.anchor = GridBagConstraints.EAST;
		gbc_lblMapDistance.gridx = 1;
		gbc_lblMapDistance.gridy = 7;
		panelClusterAnalysis.add(lblMapDistance, gbc_lblMapDistance);
		componentsAgglomerativeClustering[1] = lblMapDistance;

		comboBoxMapDistance = new JComboBox<String>(
				new String[] { rb.getString("Map_Distance_By_Relative_Intensities"), rb.getString("Map_Distance_By_Sector_Method"), rb.getString("Map_Distance_By_Covariance_Function") });
		selectedMapDistance = new MapDistanceByRelativeIntensityMethod();
		selectedMapDistanceIdentification = "relative_intensities";
		useCovarianceFunction = false;
		comboBoxMapDistance.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (e.getItem().toString().equals(rb.getString("Map_Distance_By_Relative_Intensities"))) {
						selectedMapDistance = new MapDistanceByRelativeIntensityMethod();
						selectedMapDistanceIdentification = "relative_intensities";
						useCovarianceFunction = false;
					} 
					else if (e.getItem().toString().equals(rb.getString("Map_Distance_By_Sector_Method"))) {
						int numberOfSections = 1;
						try {
							numberOfSections = Integer.parseInt(JOptionPane.showInputDialog(rb.getString("popUpSectorMethod") + ": ",
									rb.getString("numberOfSectors")));
						} catch (NumberFormatException e1) {
							JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("ErrorMessageSectorMethod")
									,rb.getString("title_popupWrongFormat"), JOptionPane.WARNING_MESSAGE);

						}
						selectedMapDistance = new MapDistanceBySectorMethod(numberOfSections);
						selectedMapDistanceIdentification = "sector_method";
						useCovarianceFunction = false;
					}
					else {
						selectedMapDistanceIdentification = "covariance_method";
						useCovarianceFunction = true;
					}

					// force re-evaluation of cluster distance
					Object tmp = comboBoxClusterDistance.getSelectedItem();
					comboBoxClusterDistance.setSelectedItem(null);
					comboBoxClusterDistance.setSelectedItem(tmp);
				}
			}
		});

		GridBagConstraints gbc_comboBoxMapDistance = new GridBagConstraints();
		gbc_comboBoxMapDistance.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxMapDistance.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxMapDistance.gridx = 2;
		gbc_comboBoxMapDistance.gridy = 7;
		gbc_comboBoxMapDistance.gridwidth = 2;
		panelClusterAnalysis.add(comboBoxMapDistance, gbc_comboBoxMapDistance);
		componentsAgglomerativeClustering[2] = comboBoxMapDistance;



		// initialize combo box to select distance between clusters
		JLabel lblClusterDistance = new JLabel(rb.getString("text_lblClusterDistance"));
		lblClusterDistance.setToolTipText(rb.getString("tooltip_lblClusterDistance"));
		GridBagConstraints gbc_lblClusterDistance = new GridBagConstraints();
		gbc_lblClusterDistance.fill = GridBagConstraints.BOTH;
		gbc_lblClusterDistance.insets = new Insets(0, 0, 5, 5);
		gbc_lblClusterDistance.anchor = GridBagConstraints.EAST;
		gbc_lblClusterDistance.gridx = 1;
		gbc_lblClusterDistance.gridy = 8;
		panelClusterAnalysis.add(lblClusterDistance, gbc_lblClusterDistance);
		componentsAgglomerativeClustering[3] = lblClusterDistance;

		comboBoxClusterDistance = new JComboBox<String>(new String[] { rb.getString("Cluster_Distance_By_Complete_Linkage"),
				rb.getString("Cluster_Distance_By_Average_Linkage"), rb.getString("Cluster_Distance_By_Single_Linkage"), rb.getString("Cluster_Distance_By_Centroid_Method"), rb.getString("Cluster_Distance_By_Wards_Method") });
		selectedClusterDistance = new ClusterDistanceByCompleteLinkage(selectedMapDistance);
		selectedClusterDistanceIdentification = "complete_linkage";

		comboBoxClusterDistance.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (e.getItem().toString().equals(rb.getString("Cluster_Distance_By_Average_Linkage"))) {
						selectedClusterDistance = new ClusterDistanceByAverageLinkage(selectedMapDistance);
						selectedClusterDistanceIdentification = "average_linkage";
					}
					if (e.getItem().toString().equals(rb.getString("Cluster_Distance_By_Complete_Linkage"))) {
						selectedClusterDistance = new ClusterDistanceByCompleteLinkage(selectedMapDistance);
						selectedClusterDistanceIdentification = "complete_linkage";
					}
					if (e.getItem().toString().equals(rb.getString("Cluster_Distance_By_Single_Linkage"))) {
						selectedClusterDistance = new ClusterDistanceBySingleLinkage(selectedMapDistance);
						selectedClusterDistanceIdentification = "single_linkage";
					}
					if (e.getItem().toString().equals(rb.getString("Cluster_Distance_By_Centroid_Method"))) {
						if (useCovarianceFunction) {
							selectedClusterDistance = new CentroidMethod(new EuclideanDistance());
							selectedClusterDistanceIdentification = "centroid_method";
						}
						else {
							JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupCentroidMethod")
									,rb.getString("title_popupWrongClusterDistance"), JOptionPane.WARNING_MESSAGE);
						}
					}
					if (e.getItem().toString().equals(rb.getString("Cluster_Distance_By_Wards_Method"))) {
						if (useCovarianceFunction) {
							selectedClusterDistance = new WardsMethod(new EuclideanDistance());
							selectedClusterDistanceIdentification = "wards_method";
						}
						else {
							JOptionPane.showMessageDialog(panelClusterAnalysis, rb.getString("text_popupWardsMethod")
									, rb.getString("title_popupWrongClusterDistance"), JOptionPane.WARNING_MESSAGE);
						}
					}
				}
			}
		});

		GridBagConstraints gbc_comboBoxClusterDistance = new GridBagConstraints();
		gbc_comboBoxClusterDistance.insets = new Insets(0, 0, 5, 5);
		gbc_comboBoxClusterDistance.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxClusterDistance.gridx = 2;
		gbc_comboBoxClusterDistance.gridy = 8;
		gbc_comboBoxClusterDistance.gridwidth = 2;
		panelClusterAnalysis.add(comboBoxClusterDistance, gbc_comboBoxClusterDistance);
		componentsAgglomerativeClustering[4] = comboBoxClusterDistance;


		// initialize fields for termination criterion
		JLabel lblTerminationCriterion = new JLabel(rb.getString("text_lblTerminationCriterion"));
		lblTerminationCriterion.setToolTipText(rb.getString("tooltip_lblTerminationCriterion"));
		GridBagConstraints gbc_lblTerminationCriterion = new GridBagConstraints();
		gbc_lblTerminationCriterion.fill = GridBagConstraints.BOTH;
		gbc_lblTerminationCriterion.insets = new Insets(0, 0, 5, 5);
		gbc_lblTerminationCriterion.anchor = GridBagConstraints.EAST;
		gbc_lblTerminationCriterion.gridx = 1;
		gbc_lblTerminationCriterion.gridy = 9;
		panelClusterAnalysis.add(lblTerminationCriterion, gbc_lblTerminationCriterion);
		componentsAgglomerativeClustering[5] = lblTerminationCriterion;

		JRadioButton radioButtonTerminationDistanceVariability = new JRadioButton(rb.getString("text_radioButtonTerminationDistanceVariability"));
		radioButtonTerminationDistanceVariability.setToolTipText(rb.getString("tooltip_radioButtonTerminationDistanceVariability"));
		radioButtonTerminationDistanceVariability.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedTerminationCriterion = rb.getString("text_radioButtonTerminationDistanceVariability");			
			}
		});
		GridBagConstraints gbc_radioButtonTerminationDistanceVariability = new GridBagConstraints();
		gbc_radioButtonTerminationDistanceVariability.fill = GridBagConstraints.BOTH;
		gbc_radioButtonTerminationDistanceVariability.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonTerminationDistanceVariability.anchor = GridBagConstraints.EAST;
		gbc_radioButtonTerminationDistanceVariability.gridx = 2;
		gbc_radioButtonTerminationDistanceVariability.gridy = 9;
		panelClusterAnalysis.add(radioButtonTerminationDistanceVariability, gbc_radioButtonTerminationDistanceVariability);
		componentsAgglomerativeClustering[6] = radioButtonTerminationDistanceVariability;

		textFieldTerminationThresholdK = new JTextField();
		textFieldTerminationThresholdK.setText("1.7");	
		GridBagConstraints gbc_textFieldTerminationThresholdK = new GridBagConstraints();
		gbc_textFieldTerminationThresholdK.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldTerminationThresholdK.fill = GridBagConstraints.BOTH;
		gbc_textFieldTerminationThresholdK.gridx = 3;
		gbc_textFieldTerminationThresholdK.gridy = 9;
		panelClusterAnalysis.add(textFieldTerminationThresholdK, gbc_textFieldTerminationThresholdK);
		componentsAgglomerativeClustering[7] = textFieldTerminationThresholdK;
		textFieldTerminationThresholdK.setColumns(10);



		JRadioButton radioButtonTerminationNumberOfClusters = new JRadioButton(rb.getString("text_radioButtonTerminationNumberOfClusters"));
		radioButtonTerminationNumberOfClusters.setToolTipText(rb.getString("tooltip_radioButtonTerminationNumberOfClusters"));
		radioButtonTerminationNumberOfClusters.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedTerminationCriterion = rb.getString("text_radioButtonTerminationNumberOfClusters");
			}
		});
		GridBagConstraints gbc_radioButtonTerminationNumberOfClusters = new GridBagConstraints();
		gbc_radioButtonTerminationNumberOfClusters.fill = GridBagConstraints.BOTH;
		gbc_radioButtonTerminationNumberOfClusters.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonTerminationNumberOfClusters.anchor = GridBagConstraints.EAST;
		gbc_radioButtonTerminationNumberOfClusters.gridx = 2;
		gbc_radioButtonTerminationNumberOfClusters.gridy = 10;
		panelClusterAnalysis.add(radioButtonTerminationNumberOfClusters, gbc_radioButtonTerminationNumberOfClusters);
		componentsAgglomerativeClustering[8] = radioButtonTerminationNumberOfClusters;

		textFieldTerminationNumberOfClusters = new JTextField();
		GridBagConstraints gbc_textFieldTerminationNumberOfClusters = new GridBagConstraints();
		gbc_textFieldTerminationNumberOfClusters.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldTerminationNumberOfClusters.fill = GridBagConstraints.BOTH;
		gbc_textFieldTerminationNumberOfClusters.gridx = 3;
		gbc_textFieldTerminationNumberOfClusters.gridy = 10;
		panelClusterAnalysis.add(textFieldTerminationNumberOfClusters, gbc_textFieldTerminationNumberOfClusters);
		componentsAgglomerativeClustering[9] = textFieldTerminationNumberOfClusters;
		textFieldTerminationNumberOfClusters.setColumns(10);

		buttonGroupAgglomerative = new ButtonGroup();
		buttonGroupAgglomerative.add(radioButtonTerminationDistanceVariability);
		buttonGroupAgglomerative.add(radioButtonTerminationNumberOfClusters);
		radioButtonTerminationDistanceVariability.setSelected(true);
		selectedTerminationCriterion = rb.getString("text_radioButtonTerminationDistanceVariability");

		for (Component component : componentsAgglomerativeClustering) {
			panelClusterAnalysis.setComponentZOrder(component, 0);
		}
		panelClusterAnalysis.revalidate();
	}



	/**
	 * Initializes components required for options of Fuzzy CMeans clustering.
	 */
	private void initializeComponentsOfFuzzyCMeansClustering() {
		componentsFuzzyCMeansClustering = new Component[9]; 
		// label
		JLabel lblClusteringOptions = new JLabel(rb.getString("text_lblClusteringOptionsFuzzy"));
		GridBagConstraints gbc_lblClusteringOptions = new GridBagConstraints();
		gbc_lblClusteringOptions.fill = GridBagConstraints.BOTH;
		gbc_lblClusteringOptions.insets = new Insets(0, 0, 5, 5);
		gbc_lblClusteringOptions.anchor = GridBagConstraints.EAST;
		gbc_lblClusteringOptions.gridx = 1;
		gbc_lblClusteringOptions.gridy = 6;
		gbc_lblClusteringOptions.gridwidth = 3;
		panelClusterAnalysis.add(lblClusteringOptions, gbc_lblClusteringOptions);
		componentsFuzzyCMeansClustering[0] = lblClusteringOptions;

		// initialize text field for exponent m
		JLabel lblExponentM = new JLabel(rb.getString("text_lblExponentM"));
		lblExponentM.setToolTipText(rb.getString("tooltip_lblExponentM"));
		GridBagConstraints gbc_lblExponentM = new GridBagConstraints();
		gbc_lblExponentM.fill = GridBagConstraints.BOTH;
		gbc_lblExponentM.insets = new Insets(0, 0, 5, 5);
		gbc_lblExponentM.anchor = GridBagConstraints.EAST;
		gbc_lblExponentM.gridx = 1;
		gbc_lblExponentM.gridy = 7;
		panelClusterAnalysis.add(lblExponentM, gbc_lblExponentM);
		componentsFuzzyCMeansClustering[1] = lblExponentM;

		textFieldExponentM = new JTextField();
		textFieldExponentM.setText("2.0");

		GridBagConstraints gbc_textFieldExponentM = new GridBagConstraints();
		gbc_textFieldExponentM.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldExponentM.fill = GridBagConstraints.BOTH;
		gbc_textFieldExponentM.gridx = 2;
		gbc_textFieldExponentM.gridy = 7;
		gbc_textFieldExponentM.gridwidth = 2;
		panelClusterAnalysis.add(textFieldExponentM, gbc_textFieldExponentM);
		componentsFuzzyCMeansClustering[2] = textFieldExponentM;
		textFieldExponentM.setColumns(10);


		// initialize text field for epsilon
		JLabel lblEpsilon = new JLabel(rb.getString("text_lblEpsilon"));
		lblEpsilon.setToolTipText(rb.getString("tooltip_lblEpsilon"));
		GridBagConstraints gbc_lblEpsilon = new GridBagConstraints();
		gbc_lblEpsilon.fill = GridBagConstraints.BOTH;
		gbc_lblEpsilon.insets = new Insets(0, 0, 5, 5);
		gbc_lblEpsilon.anchor = GridBagConstraints.EAST;
		gbc_lblEpsilon.gridx = 1;
		gbc_lblEpsilon.gridy = 8;
		panelClusterAnalysis.add(lblEpsilon, gbc_lblEpsilon);
		componentsFuzzyCMeansClustering[3] = lblEpsilon;

		textFieldEpsilon = new JTextField();
		textFieldEpsilon.setText("0.0001");
		GridBagConstraints gbc_textFieldEpsilon = new GridBagConstraints();
		gbc_textFieldEpsilon.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldEpsilon.fill = GridBagConstraints.BOTH;
		gbc_textFieldEpsilon.gridx = 2;
		gbc_textFieldEpsilon.gridy = 8;
		gbc_textFieldEpsilon.gridwidth = 2;
		panelClusterAnalysis.add(textFieldEpsilon, gbc_textFieldEpsilon);
		componentsFuzzyCMeansClustering[4] = textFieldEpsilon;
		textFieldEpsilon.setColumns(10);


		// initialize text field for number of clusters
		JLabel lblNumberOfClusters = new JLabel(rb.getString("text_lblNumberOfClusters"));
		lblNumberOfClusters.setToolTipText(rb.getString("tooltip_lblNumberOfClusters"));
		GridBagConstraints gbc_lblNumberOfClusters = new GridBagConstraints();
		gbc_lblNumberOfClusters.fill = GridBagConstraints.BOTH;
		gbc_lblNumberOfClusters.insets = new Insets(0, 0, 5, 5);
		gbc_lblNumberOfClusters.anchor = GridBagConstraints.EAST;
		gbc_lblNumberOfClusters.gridx = 1;
		gbc_lblNumberOfClusters.gridy = 9;
		panelClusterAnalysis.add(lblNumberOfClusters, gbc_lblNumberOfClusters);
		componentsFuzzyCMeansClustering[5] = lblNumberOfClusters;

		JRadioButton radioButtonNumberManually = new JRadioButton(rb.getString("text_radioButtonNumberManually"));
		radioButtonNumberManually.setToolTipText(rb.getString("tooltip_radioButtonNumberManually"));
		radioButtonNumberManually.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				computeNumberOfClustersAutomaticallyFuzzy = false;
			}
		});
		GridBagConstraints gbc_radioButtonNumberManually = new GridBagConstraints();
		gbc_radioButtonNumberManually.fill = GridBagConstraints.BOTH;
		gbc_radioButtonNumberManually.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonNumberManually.anchor = GridBagConstraints.EAST;
		gbc_radioButtonNumberManually.gridx = 2;
		gbc_radioButtonNumberManually.gridy = 9;
		panelClusterAnalysis.add(radioButtonNumberManually, gbc_radioButtonNumberManually);
		componentsFuzzyCMeansClustering[6] = radioButtonNumberManually;

		textFieldNumberOfClustersFuzzy = new JTextField();
		GridBagConstraints gbc_textFieldNumberOfClustersFuzzy = new GridBagConstraints();
		gbc_textFieldNumberOfClustersFuzzy.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldNumberOfClustersFuzzy.fill = GridBagConstraints.BOTH;
		gbc_textFieldNumberOfClustersFuzzy.gridx = 3;
		gbc_textFieldNumberOfClustersFuzzy.gridy = 9;
		panelClusterAnalysis.add(textFieldNumberOfClustersFuzzy, gbc_textFieldNumberOfClustersFuzzy);
		componentsFuzzyCMeansClustering[7] = textFieldNumberOfClustersFuzzy;
		textFieldNumberOfClustersFuzzy.setColumns(10);

		JRadioButton radioButtonNumberAutomatically = new JRadioButton(rb.getString("text_radioButtonNumberAutomatically"));
		radioButtonNumberAutomatically.setToolTipText(rb.getString("tooltip_radioButtonNumberAutomatically"));
		radioButtonNumberAutomatically.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				computeNumberOfClustersAutomaticallyFuzzy = true;
			}
		});
		GridBagConstraints gbc_radioButtonNumberAutomatically = new GridBagConstraints();
		gbc_radioButtonNumberAutomatically.fill = GridBagConstraints.BOTH;
		gbc_radioButtonNumberAutomatically.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonNumberAutomatically.anchor = GridBagConstraints.EAST;
		gbc_radioButtonNumberAutomatically.gridx = 2;
		gbc_radioButtonNumberAutomatically.gridy = 10;
		panelClusterAnalysis.add(radioButtonNumberAutomatically, gbc_radioButtonNumberAutomatically);
		componentsFuzzyCMeansClustering[8] = radioButtonNumberAutomatically;

		buttonGroupFuzzy = new ButtonGroup();
		buttonGroupFuzzy.add(radioButtonNumberManually);
		buttonGroupFuzzy.add(radioButtonNumberAutomatically);
		radioButtonNumberManually.setSelected(true);
		computeNumberOfClustersAutomaticallyFuzzy = false;

		for (Component component : componentsFuzzyCMeansClustering) {
			panelClusterAnalysis.setComponentZOrder(component, 0);
		}
		panelClusterAnalysis.revalidate();
	}


	/**
	 * Initializes components required for options of KMeans clustering.
	 */
	private void initializeComponentsOfKMeansClustering() {
		componentsKMeansClustering = new Component[5];
		// label
		JLabel lblClusteringOptions = new JLabel(rb.getString("text_lblClusteringOptionsKMeans"));
		GridBagConstraints gbc_lblClusteringOptions = new GridBagConstraints();
		gbc_lblClusteringOptions.fill = GridBagConstraints.BOTH;
		gbc_lblClusteringOptions.insets = new Insets(0, 0, 5, 5);
		gbc_lblClusteringOptions.anchor = GridBagConstraints.EAST;
		gbc_lblClusteringOptions.gridx = 1;
		gbc_lblClusteringOptions.gridy = 6;
		gbc_lblClusteringOptions.gridwidth = 3;
		panelClusterAnalysis.add(lblClusteringOptions, gbc_lblClusteringOptions);
		componentsKMeansClustering[0] = lblClusteringOptions;

		// initialize text field for number of clusters
		JLabel lblNumberOfClusters = new JLabel(rb.getString("text_lblNumberOfClusters"));
		lblNumberOfClusters.setToolTipText(rb.getString("tooltip_lblNumberOfClusters"));
		GridBagConstraints gbc_lblNumberOfClusters = new GridBagConstraints();
		gbc_lblNumberOfClusters.fill = GridBagConstraints.BOTH;
		gbc_lblNumberOfClusters.insets = new Insets(0, 0, 5, 5);
		gbc_lblNumberOfClusters.anchor = GridBagConstraints.EAST;
		gbc_lblNumberOfClusters.gridx = 1;
		gbc_lblNumberOfClusters.gridy = 7;
		panelClusterAnalysis.add(lblNumberOfClusters, gbc_lblNumberOfClusters);
		componentsKMeansClustering[1] = lblNumberOfClusters;

		JRadioButton radioButtonNumberManually = new JRadioButton(rb.getString("text_radioButtonNumberManually"));
		radioButtonNumberManually.setToolTipText(rb.getString("tooltip_radioButtonNumberManually"));
		radioButtonNumberManually.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				computeNumberOfClustersAutomaticallyKMeans = false;
			}
		});
		GridBagConstraints gbc_radioButtonNumberManually = new GridBagConstraints();
		gbc_radioButtonNumberManually.fill = GridBagConstraints.BOTH;
		gbc_radioButtonNumberManually.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonNumberManually.anchor = GridBagConstraints.EAST;
		gbc_radioButtonNumberManually.gridx = 2;
		gbc_radioButtonNumberManually.gridy = 7;
		panelClusterAnalysis.add(radioButtonNumberManually, gbc_radioButtonNumberManually);
		componentsKMeansClustering[2] = radioButtonNumberManually;

		textFieldNumberOfClustersKMeans = new JTextField();
		GridBagConstraints gbc_textFieldNumberOfClustersKMeans = new GridBagConstraints();
		gbc_textFieldNumberOfClustersKMeans.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldNumberOfClustersKMeans.fill = GridBagConstraints.BOTH;
		gbc_textFieldNumberOfClustersKMeans.gridx = 3;
		gbc_textFieldNumberOfClustersKMeans.gridy = 7;
		panelClusterAnalysis.add(textFieldNumberOfClustersKMeans, gbc_textFieldNumberOfClustersKMeans);
		componentsKMeansClustering[3] = textFieldNumberOfClustersKMeans;
		textFieldNumberOfClustersKMeans.setColumns(10);

		JRadioButton radioButtonNumberAutomatically = new JRadioButton(rb.getString("text_radioButtonNumberAutomatically"));
		radioButtonNumberAutomatically.setToolTipText(rb.getString("tooltip_radioButtonNumberAutomatically"));
		radioButtonNumberAutomatically.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				computeNumberOfClustersAutomaticallyKMeans = true;
			}
		});
		GridBagConstraints gbc_radioButtonNumberAutomatically = new GridBagConstraints();
		gbc_radioButtonNumberAutomatically.fill = GridBagConstraints.BOTH;
		gbc_radioButtonNumberAutomatically.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonNumberAutomatically.anchor = GridBagConstraints.EAST;
		gbc_radioButtonNumberAutomatically.gridx = 2;
		gbc_radioButtonNumberAutomatically.gridy = 8;
		gbc_radioButtonNumberAutomatically.gridwidth = 2;
		panelClusterAnalysis.add(radioButtonNumberAutomatically, gbc_radioButtonNumberAutomatically);
		componentsKMeansClustering[4] = radioButtonNumberAutomatically;

		buttonGroupKMeans = new ButtonGroup();
		buttonGroupKMeans.add(radioButtonNumberManually);
		buttonGroupKMeans.add(radioButtonNumberAutomatically);
		radioButtonNumberManually.setSelected(true);
		computeNumberOfClustersAutomaticallyKMeans = false;

		for (Component component : componentsKMeansClustering) {
			panelClusterAnalysis.setComponentZOrder(component, 0);
		}
		panelClusterAnalysis.revalidate();

	}

	/** Removes components required for agglomerative clustering from <code>JPanel</code>. */
	private void removeComponentsOfAgglomerativeClustering() {
		for (Component component : componentsAgglomerativeClustering) {
			panelClusterAnalysis.remove(component);
		}
		buttonGroupAgglomerative.clearSelection();
		panelClusterAnalysis.revalidate();
	}


	/** Removes components required for Fuzzy CMeans clustering from <code>JPanel</code>. */
	private void removeComponentsOfFuzzyCMeansClustering() {
		for (Component component : componentsFuzzyCMeansClustering) {
			panelClusterAnalysis.remove(component);
		}
		buttonGroupFuzzy.clearSelection();
		panelClusterAnalysis.revalidate();
	}


	/** Removes components required for KMeans clustering from <code>JPanel</code>. */
	private void removeComponentsOfKMeansClustering() {
		for (Component component : componentsKMeansClustering) {
			panelClusterAnalysis.remove(component);
		}
		buttonGroupKMeans.clearSelection();
		panelClusterAnalysis.revalidate();
	}


	/**
	 * This method constructs a list of area Class Maps which contains all maps
	 * given by <code>maps</code>. The bandwidth, which is necessary to create
	 * an area Class Map, is calculated by the given <code>distance</code>,
	 * <code>level</code> <code>kernel</code>,
	 * <code>estimatorIdentification</code>.
	 * 
	 * @return The described Array List of area Class Maps
	 */
	private ArrayList<AreaClassMap> createAreaClassMaps() {
		List<Map> maps = (selectedGroup != null) ? selectedGroup.getAll(Map.class) : null;
		if (maps == null) {
			// no group: fallback to all maps
			maps = Map.findAll();
		}
		final ArrayList<AreaClassMap> result = new ArrayList<AreaClassMap>();

		pm.setProgress(0);
		pm.setMaximum(maps.size()+1);

		BandwidthEstimator estimator = BuilderMethods.getBandwidthEstimatorObj(selectedKernel, selectedEstimatorIdentification);

		for (int i = 0; i < maps.size(); i++) {
			pm.setNote(String.format(rb.getString("format_noteLoadingMap"), (i+1), maps.size()));
			pm.setProgress(i+1);
			if (pm.isCanceled()) {
				return null;
			}
			VariantWeights variantWeights = (selectedLevel != null) ? new VariantWeightsWithLevel(maps.get(i), selectedLevel) : new VariantWeightsNoLevel(maps.get(i));

			BigDecimal bandwidth = ComputeBandwidths.findOrComputeAndSaveBandwidth(variantWeights, estimator, false, null);
			DensityEstimation densityEstimation = new KernelDensityEstimation(selectedKernel.copyOfKernelWithBandwidth(bandwidth));
			result.add(new AreaClassMap(variantWeights, densityEstimation));
		}

		// build location density cache here to be able to use the progress monitor
		pm.setProgress(pm.getMaximum());
		pm.setProgress(0);
		pm.setMaximum(result.size()+1);

		ThreadedTodoWorker.workOnTodoList(result, new ThreadedTodoWorker.SimpleTodoWorker<AreaClassMap>() {

			private int progress = 0;

			public void processTodoItem(AreaClassMap todo) {
				synchronized (this) {
					progress++;
					pm.setNote(String.format(rb.getString("format_noteDensityEstimation"), progress, result.size()));
					pm.setProgress(progress);
				}
				if (pm.isCanceled()) {
					return;
				}
				todo.buildLocationDensityCache();
			}
		});
		if (pm.isCanceled()) {
			return null;
		}
		pm.setProgress(pm.getMaximum());

		return result;
	}


	/**
	 * This method parses a list of area Class Maps to a list of Cluster objects.
	 * 
	 * @param areaClassMaps
	 *            The area Class Map which shall be parsed.
	 * @return The ArrayList of the cluster objects.
	 */
	private ArrayList<MapClusterObject> parseAreaClassMapsToObjects(ArrayList<AreaClassMap> areaClassMaps) {
		if (pm.isCanceled()) {
			return null;
		}
		ArrayList<MapClusterObject> result = new ArrayList<MapClusterObject>();
		for (int i = 0; i < areaClassMaps.size(); i++) {
			result.add(new MapClusterObject(areaClassMaps.get(i)));
		}
		return result;
	}


	private ArrayList<MapClusterObject> computeCovarianceFunctionForClusterObjects(final ArrayList<MapClusterObject> clusterObjects) {

		final int n = clusterObjects.size();

		if (pm.isCanceled()) {
			return null;
		}
		pm.setNote(rb.getString("text_noteInitialization"));

		LazyList<Location> allLocations = Location.findAll();
		Polytope border = new Polytope(MapBorder.getConvexHull(allLocations).getVertices(), true);
		final RectangularGrid grid = new RectangularGrid(border, new KilometresProjection(border), 1.0);


		// project border polygon to coordinates in kilometres, 
		// use average of the width and height of its bounding box as p
		double[] size = new KilometresProjection(border).projectLatLong(border).getBoundingBox().getWidth();
		final int p = (int) ((size[0] + size[1]) / 2.0);


		// precompute distances between grid points
		if (pm.isCanceled()) {
			return null;
		}
		pm.setNote(rb.getString("text_noteComputeGridDistances"));
		pm.setProgress(0);
		pm.setMaximum(grid.getGridPoints().size() * grid.getGridPoints().size() / 2 + 1);

		final double[][] cachedDistances = new double[grid.getGridPoints().size()][];
		ThreadedTodoWorker.workOnIndices(0, grid.getGridPoints().size() - 1, 1, new ThreadedTodoWorker.SimpleTodoWorker<Integer>() {
			int counter = 0;

			public void processTodoItem(Integer todo) {
				int j = todo.intValue();
				cachedDistances[j] = new double[j];
				for (int k = 0; k < j; k++) {
					RectangularGrid.GridPoint currentLocation1 = grid.getGridPoints().get(j);
					RectangularGrid.GridPoint currentLocation2 = grid.getGridPoints().get(k);
					cachedDistances[j][k] = selectedDistanceMeasure.getDistance(currentLocation1.getLatLong(), currentLocation2.getLatLong());

					if (pm.isCanceled()) {
						return;
					}
					synchronized (this) {
						counter++;
						pm.setProgress(counter);
					}

				}
			}
		});

		if (pm.isCanceled()) {
			return null;
		}
		pm.setProgress(pm.getMaximum());


		// compute all covariance functions
		pm.setProgress(0);
		pm.setMaximum(n*2);

		ThreadedTodoWorker.workOnIndices(0, n - 1, 1, new ThreadedTodoWorker.SimpleTodoWorker<Integer>() {
			int counter = 0, counter2 = 0;

			public void processTodoItem(Integer todo) {
				int i = todo.intValue();


				if (pm.isCanceled()) {
					return;
				}
				synchronized (this) {
					counter++;
					counter2++;
					pm.setProgress(counter2);
					pm.setNote(String.format(rb.getString("format_noteEstimateCovariance"), counter, n));
				}


				clusterObjects.get(i).computeCovarianceFunction(grid, cachedDistances, p);


				synchronized (this) {
					counter2++;
					pm.setProgress(counter2);
				}

			}
		});

		return clusterObjects;
	}


	/**
	 * Performs the cluster analysis for the given cluster objects with the given clustering method.
	 * 
	 * @return returns an object of type ClusteringResult which contains an
	 *         ArrayList of type Cluster which is containing all resulting
	 *         cluster, a boolean which is false when the cluster Analysis has
	 *         not been fuzzy, truth otherwise, a.s.o.
	 * 
	 */
	private ClusteringResult performClusterAnalysis(ClusterAnalysis clustering, final ArrayList<MapClusterObject> clusterObjects) {

		pm.setNote(rb.getString("text_noteInitializationMapDistances"));
		pm.setProgress(0);
		pm.setMaximum(clusterObjects.size() * clusterObjects.size() / 2);

		ThreadedTodoWorker.workOnIndices(0, clusterObjects.size() - 1, 1, new ThreadedTodoWorker.SimpleTodoWorker<Integer>() {
			private int k = 0;

			public void processTodoItem(Integer todo) {
				int i = todo.intValue();
				for (int j = 0; j < i; j++) {
					// computed value will be cached automatically
					selectedMapDistance.distance(clusterObjects.get(i), clusterObjects.get(j));

					synchronized (this) {
						if (pm.isCanceled()) {
							return;
						}
						k++;
						pm.setProgress(k);
					}
				}
			}
		});

		pm.setNote(rb.getString("text_clusteringRunning"));
		pm.setProgress(0);

		ClusteringResult result = clustering.clusterAnalysis(clusterObjects);

		pm.setProgress(pm.getMaximum());

		return result;
	}
}
