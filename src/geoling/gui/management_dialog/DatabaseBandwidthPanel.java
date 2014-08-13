package geoling.gui.management_dialog;

import geoling.config.Database;
import geoling.gui.GeoLingGUI;
import geoling.gui.util.StatusLabel;
import geoling.gui.util.TableDistanceElement;
import geoling.gui.util.TableGroupElement;
import geoling.maps.density.bandwidth.*;
import geoling.maps.density.bandwidth.computation.ComputeBandwidths;
import geoling.maps.density.kernels.*;
import geoling.maps.distances.*;
import geoling.maps.weights.*;
import geoling.models.Distance;
import geoling.models.Group;
import geoling.models.Level;
import geoling.models.Map;
import geoling.util.ThreadedTodoWorker;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ProgressMonitor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.javalite.activejdbc.LazyList;

/**
 * A panel to compute bandwidths for valid combinations of groups, estimators, kernels and distance measures.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class DatabaseBandwidthPanel {

	/** The panel to which contents are added. */
	private JPanel panel = new JPanel();

	/** A <code>ProgressMonitor</code> to show process. */
	private ProgressMonitor pm;

	/** A <code>JTable</code> with check boxes showing all available groups. */
	private JTable tableGroup;
	/** The <code>JScrollPane</code> for the <code>JTable</code> tableGroup. */
	private JScrollPane scrollPaneGroup = new JScrollPane();

	/** A <code>JTable</code> with check boxes showing all available estimators. */
	private JTable tableEstimator;
	/** The <code>JScrollPane</code> for the <code>JTable</code> tableEstimator. */
	private JScrollPane scrollPaneEstimator = new JScrollPane();

	/** A <code>JTable</code> with check boxes showing all available kernels. */
	private JTable tableKernel;
	/** The <code>JScrollPane</code> for the <code>JTable</code> tableKernel. */
	private JScrollPane scrollPaneKernel = new JScrollPane();

	/** A <code>JTable</code> with check boxes showing all available distance measures. */
	private JTable tableDistance;
	/** The <code>JScrollPane</code> for the <code>JTable</code> tableGroup. */
	private JScrollPane scrollPaneDistance = new JScrollPane();

	/** A <code>JCheckBox</code> where the user can decided whether bandwidths shall be recomputed. */
	private JCheckBox checkBoxRecompute;
	
	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;


	public DatabaseBandwidthPanel(final JTabbedPane tabbedPane) {

		rb = ResourceBundle.getBundle("DatabaseBandwidthPanel", GeoLingGUI.LANGUAGE);
		
		tabbedPane.insertTab(rb.getString("title_DatabaseBandwidthPanel"), null, panel, null, DatabaseManagementDialog.TAB_BANDWIDTH);
		tabbedPane.addChangeListener(new ChangeListener() {

			// This method is called whenever the selected tab changes
			public void stateChanged(ChangeEvent evt) {
				JTabbedPane pane = (JTabbedPane) evt.getSource();
				// if database distance tab is selected
				if (pane.getSelectedIndex() == DatabaseManagementDialog.TAB_BANDWIDTH) { 
					tableGroup = createGroupTable();
					scrollPaneGroup.getViewport().setView(tableGroup);
					tableDistance = createDistanceTable();
					scrollPaneDistance.getViewport().setView(tableDistance);
				}

			}
		});

		// set layout
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 120, 60, 60, 120, 0 };
		gridBagLayout.rowHeights = new int[] { 200, 50, 10, 0 };
		gridBagLayout.columnWeights = new double[] { 0.35, 0.15, 0.15, 0.35,  Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.9, 0, 0.1, Double.MIN_VALUE };
		panel.setLayout(gridBagLayout);

		// groups
		GridBagConstraints gbc_scrollPaneGroup = new GridBagConstraints();
		gbc_scrollPaneGroup.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneGroup.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPaneGroup.gridx = 0;
		gbc_scrollPaneGroup.gridy = 0;
		panel.add(scrollPaneGroup, gbc_scrollPaneGroup);

		tableGroup = createGroupTable();
		scrollPaneGroup.setViewportView(tableGroup);
		scrollPaneGroup.setToolTipText(rb.getString("tooltip_scrollPaneGroup"));


		// estimators
		GridBagConstraints gbc_scrollPaneEstimator = new GridBagConstraints();
		gbc_scrollPaneEstimator.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneEstimator.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPaneEstimator.gridx = 1;
		gbc_scrollPaneEstimator.gridy = 0;
		panel.add(scrollPaneEstimator, gbc_scrollPaneEstimator);

		tableEstimator = createEstimatorTable();
		scrollPaneEstimator.setViewportView(tableEstimator);
		scrollPaneEstimator.setToolTipText(rb.getString("tooltip_scrollPaneEstimator"));


		// kernels
		GridBagConstraints gbc_scrollPaneKernel = new GridBagConstraints();
		gbc_scrollPaneKernel.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneKernel.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPaneKernel.gridx = 2;
		gbc_scrollPaneKernel.gridy = 0;
		panel.add(scrollPaneKernel, gbc_scrollPaneKernel);

		tableKernel = createKernelTable();
		scrollPaneKernel.setViewportView(tableKernel);
		scrollPaneKernel.setToolTipText(rb.getString("tooltip_scrollPaneKernel"));


		// distance measures
		GridBagConstraints gbc_scrollPaneDistance = new GridBagConstraints();
		gbc_scrollPaneDistance.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneDistance.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPaneDistance.gridx = 3;
		gbc_scrollPaneDistance.gridy = 0;
		panel.add(scrollPaneDistance, gbc_scrollPaneDistance);

		tableDistance = createKernelTable();
		scrollPaneDistance.setViewportView(tableDistance);
		scrollPaneDistance.setToolTipText(rb.getString("tooltip_scrollPaneDistance"));


		// status label
		final StatusLabel statusLabelComputation = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelComputation = new GridBagConstraints();
		gbc_statusLabelComputation.fill = GridBagConstraints.BOTH;
		gbc_statusLabelComputation.insets = new Insets(5, 5, 5, 5);
		gbc_statusLabelComputation.gridx = 3;
		gbc_statusLabelComputation.gridy = 1;
		panel.add(statusLabelComputation, gbc_statusLabelComputation);

		checkBoxRecompute = new JCheckBox(rb.getString("text_checkBoxRecompute"));
		checkBoxRecompute.setToolTipText(rb.getString("tooltip_checkBoxRecompute"));
		GridBagConstraints gbc_checkBoxRecompute = new GridBagConstraints();
		gbc_checkBoxRecompute.fill = GridBagConstraints.BOTH;
		gbc_checkBoxRecompute.insets = new Insets(5, 5, 5, 5);
		gbc_checkBoxRecompute.gridx = 2;
		gbc_checkBoxRecompute.gridy = 1;
		panel.add(checkBoxRecompute, gbc_checkBoxRecompute);

		// button to start computation
		JButton buttonStartComputation = new JButton(rb.getString("text_buttonStartComputation"));
		GridBagConstraints gbc_buttonStartComputation = new GridBagConstraints();
		gbc_buttonStartComputation.fill = GridBagConstraints.BOTH;
		gbc_buttonStartComputation.insets = new Insets(5, 5, 5, 5);
		gbc_buttonStartComputation.gridx = 0;
		gbc_buttonStartComputation.gridy = 1;
		gbc_buttonStartComputation.gridwidth = 2;
		panel.add(buttonStartComputation, gbc_buttonStartComputation);
		buttonStartComputation.addActionListener(new ActionListener() {
			/** Button start computation is pressed. */
			public void actionPerformed(ActionEvent arg0) {
				// get selected groups, estimators, kernels and distances
				TableModel modelGroup = tableGroup.getModel();
				final ArrayList<Group> selectedGroups = new ArrayList<Group>();
				for (int row=0; row<modelGroup.getRowCount(); row++) {
					boolean isSelected = (boolean) modelGroup.getValueAt(row, 1);
					if (isSelected) {
						selectedGroups.add(((TableGroupElement) modelGroup.getValueAt(row, 0)).getGroup());
					}
				}
				TableModel modelEstimator = tableEstimator.getModel();
				ArrayList<String> selectedEstimators = new ArrayList<String>();
				for (int row=0; row<modelEstimator.getRowCount(); row++) {
					boolean isSelected = (boolean) modelEstimator.getValueAt(row, 1);
					if (isSelected) {
						selectedEstimators.add((String) modelEstimator.getValueAt(row, 0));
					}
				}
				TableModel modelKernel = tableKernel.getModel();
				ArrayList<String> selectedKernels = new ArrayList<String>();
				for (int row=0; row<modelKernel.getRowCount(); row++) {
					boolean isSelected = (boolean) modelKernel.getValueAt(row, 1);
					if (isSelected) {
						selectedKernels.add((String) modelKernel.getValueAt(row, 0));
					}
				}
				TableModel modelDistance = tableDistance.getModel();
				ArrayList<Distance> selectedDistances = new ArrayList<Distance>();
				for (int row=0; row<modelDistance.getRowCount(); row++) {
					boolean isSelected = (boolean) modelDistance.getValueAt(row, 1);
					if (isSelected) {
						selectedDistances.add(((TableDistanceElement) modelDistance.getValueAt(row, 0)).getDistance());
					}
				}
				// look for at least one entry in each list
				if (selectedGroups.size()==0 || selectedEstimators.size()==0 || selectedKernels.size()==0 || selectedDistances.size()==0) {
					JOptionPane.showMessageDialog(panel, rb.getString("text_popupMissingEntries"), rb.getString("title_popupMissingEntries"), JOptionPane.WARNING_MESSAGE);
					return;
				}

				// create list with all bandwidth estimators (i.e. create all valid combinations of distances, kernels and estimators)
				final ArrayList<BandwidthEstimator> bandwidthEstimators = new ArrayList<BandwidthEstimator>();

				// iterate over distances
				for (Distance distance : selectedDistances) {
					DistanceMeasure distanceMeasure = distance.getDistanceMeasure(true);

					// iterate over kernels
					for (String kernelString : selectedKernels) {
						Kernel kernel;
						if (kernelString.equals(rb.getString("Gauss"))) {
							kernel = new GaussianKernel(distanceMeasure, null);
						}
						else if (kernelString.equals(rb.getString("K3"))) {
							kernel = new K3Kernel(distanceMeasure, null);
						}
						else { // Epanechnikov
							kernel = new EpanechnikovKernel(distanceMeasure, null);
						}

						// iterate over estimators and them to the list using the current kernel
						for (String estimatorString : selectedEstimators) {
							if (estimatorString.equals(rb.getString("LSCV"))) {
								if ((distanceMeasure instanceof GeographicalDistance) && (kernel instanceof GaussianKernel)) {
									bandwidthEstimators.add(new LeastSquaresCrossValidation(kernel));
								}
							}
							else if (estimatorString.equals(rb.getString("LCV"))) {
								bandwidthEstimators.add(new LikelihoodCrossValidation(kernel));
							}
							else { // min-C-max-L
								bandwidthEstimators.add(new MinComplexityMaxFidelity(kernel));
							}
						}
					}
				}

				if (bandwidthEstimators.size()==0) {
					JOptionPane.showMessageDialog(panel, rb.getString("text_popupNoEstimator"), rb.getString("title_popupNoEstimator"), JOptionPane.WARNING_MESSAGE);
					return;
				}

				final boolean recompute = checkBoxRecompute.isSelected();
				statusLabelComputation.changeStatus(2);

				Thread thread = new Thread(new Runnable() {
					public void run() {		

						Database.ensureConnection();
						final LazyList<Level> levels = Level.findAll();
						pm = new ProgressMonitor(panel, rb.getString("text_progressComputing"), "", 0, 100);
						pm.setMillisToDecideToPopup(0);
						pm.setMillisToPopup(0);

						for (Group group : selectedGroups) {
							LazyList<Map> maps = group.getAll(Map.class);
							// customize progress for the current group
							if (pm.isCanceled()) {
								return;
							}
							pm.setProgress(0);
							pm.setMaximum(maps.size());
							pm.setNote(group.getString("name"));

							ThreadedTodoWorker.workOnTodoList(maps, new ThreadedTodoWorker.SimpleTodoWorker<Map>() {
								private int progress = 0;

								public void processTodoItem(Map map) {
									Database.ensureConnection();

									VariantWeights variantWeightsBase = new VariantWeightsNoLevel(map);

									for (Level level : levels) {
										VariantWeights variantWeights = new VariantWeightsWithLevel(variantWeightsBase, level);

										for (BandwidthEstimator estimator : bandwidthEstimators) {
											if (estimator.getDistanceMeasure() instanceof LinguisticDistance) {
												// if linguistic distance, then perform the estimation only if the level
												// matches and this map is contained in the group
												LinguisticDistance d = (LinguisticDistance)estimator.getDistanceMeasure();
												if ((d.getLevel() != null) && !d.getLevel().equals(level)) {
													continue;
												}
											}

											if (pm.isCanceled()) {
												return;
											}
											ComputeBandwidths.findOrComputeAndSaveBandwidth(variantWeights, estimator, recompute, null);

										}
									}

									synchronized (this) {
										progress++;
										pm.setProgress(progress);
									}
								}
							});
						}

						statusLabelComputation.changeStatus(3);
					}
				});
				thread.start();
				





			}
		});

	}


	/** Find all groups in database and show them in a <code>JTable</code>. */
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
			Object[] columnNames = {rb.getString("columnName1_tableGroup"), rb.getString("columnName2_tableGroup")};
			// create table contents
			Object[][] data = new Object[groups.size()][columnNames.length];
			for (int i=0; i<groups.size(); i++) {
				data[i][0] = new TableGroupElement(groups.get(i));
				data[i][1] = false;
			}

			DefaultTableModel model = new DefaultTableModel(data, columnNames);
			JTable table = new JTable(model) {
				private static final long serialVersionUID = 1L;

				@Override
				public Class<?> getColumnClass(int column) {
					switch (column) {
					case 0: return TableGroupElement.class;
					case 1:	return Boolean.class;
					default: return Object.class;                     
					}
				}
			};
			return table;
		}
	}


	/** Show all estimators in a <code>JTable</code>. */
	private JTable createEstimatorTable() {

		Object[] columnNames = {rb.getString("columnName1_tableEstimator"), rb.getString("columnName2_tableEstimator")};
		// create table contents
		Object[][] data = {{rb.getString("LCV"), false}, {rb.getString("LSCV"), false}, {rb.getString("Min-C-Max-L"), false}};


		DefaultTableModel model = new DefaultTableModel(data, columnNames);
		JTable table = new JTable(model) {
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int column) {
				switch (column) {
				case 0: return String.class;
				case 1:	return Boolean.class;
				default: return Object.class;                     
				}
			}
		};

		return table;		
	}


	/** Show all kernels in a <code>JTable</code>. */
	private JTable createKernelTable() {

		Object[] columnNames = {rb.getString("columnName1_tableKernel"), rb.getString("columnName2_tableKernel")};
		// create table contents
		Object[][] data = {{rb.getString("Gauss"), false}, {rb.getString("K3"), false}, {rb.getString("Epanechnikov"), false}};


		DefaultTableModel model = new DefaultTableModel(data, columnNames);
		JTable table = new JTable(model) {
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int column) {
				switch (column) {
				case 0: return String.class;
				case 1:	return Boolean.class;
				default: return Object.class;                     
				}
			}
		};

		return table;		
	}


	/** Find all distances in database, creates distance measures and show them in a <code>JTable</code>. */
	private JTable createDistanceTable() {
		List<Distance> distances = null;
		try {
			distances = Distance.findAll();
			distances = new ArrayList<Distance>(distances);
		} catch (Exception e) {
			// ignore missing database table etc.
			distances = new ArrayList<Distance>();
		}
		
		if (distances.size()==0) {
			return new JTable();
		}
		else {
			Object[] columnNames = {rb.getString("columnName1_tableDistance"), rb.getString("columnName2_tableDistance")};
			// create table contents
			Object[][] data = new Object[distances.size()][columnNames.length];
			for (int i=0; i<distances.size(); i++) {
				data[i][0] = new TableDistanceElement(distances.get(i));
				data[i][1] = false;
			}

			DefaultTableModel model = new DefaultTableModel(data, columnNames);
			JTable table = new JTable(model) {
				private static final long serialVersionUID = 1L;

				@Override
				public Class<?> getColumnClass(int column) {
					switch (column) {
					case 0: return TableDistanceElement.class;
					case 1:	return Boolean.class;
					default: return Object.class;                     
					}
				}
			};
			return table;
		}
	}

}

