package geoling.gui;

import geoling.maps.AreaClassMap;
import geoling.maps.clustering.data.*;
import geoling.maps.plot.PlotAreaClassMap;
import geoling.maps.plot.PlotHelper;
import geoling.maps.projection.MapProjection;
import geoling.maps.projection.MercatorProjection;
import geoling.maps.util.RectangularGrid;
import geoling.maps.util.RectangularGridCache;
import geoling.models.Border;
import geoling.models.Map;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.util.plot.PlotToGraphics2D;

import javax.swing.JPanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JScrollPane;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.*;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Panel for cluster analysis visualization in dialectometry GUI.
 * 
 * @author Raphael Wimmer (partially based on previous work), Institute of Stochastics, Ulm University
 */
public class ClusterVisualizationPanel{

	/** <code>JPanel</code> for the contents. */
	private JPanel panelClusterAnalysisVisualize;
	private JScrollPane scrollPaneForOutput;

	private JTable table;

	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;

	/**
	 * 
	 * @param tabbedPane
	 *            To this <code>JTabbedPane</code> the new panel is added.
	 * @param result Contains all areaClassMaps, to which cluster every areaclassmap belongs and whether the clusteranalysis has been fuzzy or hard.
	 * @param voronoiMaps show Voronoi area-class-maps maps or continuous prevalence maps
	 */
	public ClusterVisualizationPanel(final JTabbedPane tabbedPane, final MapClusteringResult result, final boolean voronoiMaps) {

		rb = ResourceBundle.getBundle("ClusterVisualizationPanel", GeoLingGUI.LANGUAGE);
		panelClusterAnalysisVisualize = new JPanel();
		tabbedPane.addTab(rb.getString("title_ClusterVisualizationPanel"), null, panelClusterAnalysisVisualize, null);
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

		// create layout
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 50, 100, 250, 0 };
		gridBagLayout.rowHeights = new int[] { 40, 360, 0 };
		gridBagLayout.columnWeights = new double[] { 0.1, 0.1, 0.8, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panelClusterAnalysisVisualize.setLayout(gridBagLayout);


		final Polytope borderPolygon = Border.getDefaultBorder().toPolygon();
		final MapProjection mapProjection = new MercatorProjection();
		final RectangularGrid grid = RectangularGridCache.getGrid(borderPolygon, mapProjection);

		// create label
		JLabel lblCluster = new JLabel(rb.getString("text_lblCluster"));
		lblCluster.setToolTipText(rb.getString("tooltip_lblCluster"));
		GridBagConstraints gbc_lblCluster = new GridBagConstraints();
		gbc_lblCluster.fill = GridBagConstraints.BOTH;
		gbc_lblCluster.insets = new Insets(5, 5, 5, 5);
		gbc_lblCluster.anchor = GridBagConstraints.WEST;
		gbc_lblCluster.gridx = 0;
		gbc_lblCluster.gridy = 0;
		panelClusterAnalysisVisualize.add(lblCluster, gbc_lblCluster);

		ArrayList<MapClusterObject> objects = result.getCluster(0);
		objects.get(0).getAreaClassMap();

		// create names for clusters
		String[] clusterNames = new String[result.getClusterCount()];
		for (int i=0; i<result.getClusterCount(); i++) {
			clusterNames[i] = "Cluster " + (i+1);
		}
		// create combo box to select cluster
		JComboBox<String> comboBoxCluster = new JComboBox<String>(clusterNames);
		comboBoxCluster.addItemListener(new ItemListener() {		
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					// find selected cluster
					String clusterName = (String) e.getItem();
					int clusterNumber = Integer.parseInt(clusterName.substring(8)) - 1;
					ArrayList<MapClusterObject> cluster = result.getCluster(clusterNumber);

					// create new table
					scrollPaneForOutput.remove(table);
					table = new JTable();
					int nRows = ((cluster.size()-1)/3+1)*2;
					table.setModel(new DefaultTableModel(nRows, 3) {
						private static final long serialVersionUID = 1L;			
						public boolean isCellEditable(int rowIndex, int columnIndex) {
							return false;
						}
					});
					table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
						private static final long serialVersionUID = 1L;

						public void setValue(Object value) {
							if (value instanceof Icon) {
								setIcon((Icon) value);
								setText("");
							} else {
								setIcon(null);
								super.setValue(value);
							}
						}
					});
					scrollPaneForOutput.setViewportView(table);


					// draw maps and their names
					for (int j = 0; j < cluster.size(); j++) {
						int rowIndexHeader = (j / 3) * 2;
						int rowIndexIcon = rowIndexHeader + 1;
						int colIndex = j % 3;

						MapClusterObject mapClusterObject = cluster.get(j);
						AreaClassMap areaClassMap = mapClusterObject.getAreaClassMap();
						Map selectedMap = areaClassMap.getMap();
						areaClassMap.buildAreas(borderPolygon, mapProjection);

						int height = 450;
						PlotHelper helper = new PlotHelper(borderPolygon, mapProjection, height, 10);
						PlotAreaClassMap plot = new PlotAreaClassMap(areaClassMap);

						BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);

						try (PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
							if (voronoiMaps) {
								plot.voronoiExport(gre, helper, null, null);
							} else {
								areaClassMap.buildGridDensityCache(grid);
								plot.gridExport(gre, helper, null, null, null);
							}
						}

						ImageIcon icon = new ImageIcon(bi);

						if(result.isFuzzy()==false) {
							table.setValueAt(selectedMap.getString("name").substring(0, Math.min(40,selectedMap.getString("name").length())-1), rowIndexHeader, colIndex);	
						}
						else {
							table.setValueAt(selectedMap.getString("name").substring(0, Math.min(40,selectedMap.getString("name").length())-1)+" ("+Math.round(result.getObjectInClusterProbability(clusterNumber, mapClusterObject)*1000)/1000.0+")", rowIndexHeader, colIndex);	
						}

						table.setRowHeight(rowIndexIcon, 450);
						table.setValueAt(icon, rowIndexIcon, colIndex);
					}

				}
			}
		});
		comboBoxCluster.setSelectedIndex(-1);

		GridBagConstraints gbc_comboBoxCluster = new GridBagConstraints();
		gbc_comboBoxCluster.insets = new Insets(5, 5, 5, 5);
		gbc_lblCluster.anchor = GridBagConstraints.WEST;
		gbc_comboBoxCluster.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxCluster.gridx = 1;
		gbc_comboBoxCluster.gridy = 0;
		panelClusterAnalysisVisualize.add(comboBoxCluster, gbc_comboBoxCluster);


		// create table
		table = new JTable();
		scrollPaneForOutput = new JScrollPane(table);
		GridBagConstraints gbc_scrollPaneForOutput = new GridBagConstraints();
		gbc_scrollPaneForOutput.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPaneForOutput.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneForOutput.gridwidth = 3;
		gbc_scrollPaneForOutput.gridx = 0;
		gbc_scrollPaneForOutput.gridy = 1;
		panelClusterAnalysisVisualize.add(scrollPaneForOutput, gbc_scrollPaneForOutput);
		scrollPaneForOutput.getViewport().setView(table);		

	}


}