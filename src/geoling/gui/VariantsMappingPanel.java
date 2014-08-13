package geoling.gui;

import geoling.gui.vendor.MultiLineCellRenderer;
import geoling.gui.vendor.MultiLineTable;
import geoling.models.Level;
import geoling.models.Map;
import geoling.models.Variant;
import geoling.models.VariantsMapping;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.javalite.activejdbc.LazyList;

/**
 * Panel for listing of variants and their level mappings in dialectometry GUI.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class VariantsMappingPanel {
		
	private JPanel panelVariantsMapping;
	private JScrollPane scrollPaneVariantsMapping;
	
	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;
	
	public VariantsMappingPanel(final JTabbedPane tabbedPane, Map selectedMap) {
		
		rb = ResourceBundle.getBundle("VariantsMappingPanel", GeoLingGUI.LANGUAGE);
		
		panelVariantsMapping = new JPanel();
		tabbedPane.addTab(rb.getString("title_VariantsMappingPanel"), null, panelVariantsMapping, null);
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
		
		GridBagLayout gbl_panel_VariantsMapping = new GridBagLayout();
		gbl_panel_VariantsMapping.columnWidths = new int[] { 400, 0 };
		gbl_panel_VariantsMapping.rowHeights = new int[] { 30, 370, 0 };
		gbl_panel_VariantsMapping.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_VariantsMapping.rowWeights = new double[] { 0.05, 1.0, Double.MIN_VALUE };
		panelVariantsMapping.setLayout(gbl_panel_VariantsMapping);
		
		JButton buttonCloseVMTab = new JButton(rb.getString("text_buttonCloseVMTab"));
		buttonCloseVMTab.addActionListener(new ActionListener() {
			
			/**
			 * @param arg0
			 *            Press Button
			 * @return Removes <code>JPanel</code> panel_VariantsMapping from
			 *         <code>JTabbedPane</code> tabbedPane.
			 */
			public void actionPerformed(ActionEvent arg0) {
				tabbedPane.remove(panelVariantsMapping);
			}
		});
		GridBagConstraints gbc_buttonCloseVMTab = new GridBagConstraints();
		gbc_buttonCloseVMTab.fill = GridBagConstraints.NONE;
		gbc_buttonCloseVMTab.anchor = GridBagConstraints.EAST;
		gbc_buttonCloseVMTab.insets = new Insets(0, 0, 5, 0);
		gbc_buttonCloseVMTab.gridx = 0;
		gbc_buttonCloseVMTab.gridy = 0;
		panelVariantsMapping.add(buttonCloseVMTab, gbc_buttonCloseVMTab);
		
		scrollPaneVariantsMapping = new JScrollPane();
		GridBagConstraints gbc_scrollPaneVariantsMapping = new GridBagConstraints();
		gbc_scrollPaneVariantsMapping.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPaneVariantsMapping.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneVariantsMapping.gridx = 0;
		gbc_scrollPaneVariantsMapping.gridy = 1;
		
		LazyList<Level> levels = Level.findAll();
		String[] columnNames = new String[levels.size() + 1];
		columnNames[0] = rb.getString("columnName1_tableVariantsMapping");
		for (int j = 0; j < levels.size(); j++) {
			columnNames[j + 1] = levels.get(j).getString("name");
		}
		LazyList<Variant> variants = selectedMap.getAll(Variant.class).orderBy("name");
		String[][] tableContentsVM = new String[variants.size()][levels.size() + 1];
		for (int i = 0; i < variants.size(); i++) {
			Variant variant = variants.get(i);
			tableContentsVM[i][0] = variant.getString("name");
			LazyList<VariantsMapping> variantsMappings = VariantsMapping.find("variant_id = ?", variant.getId()).orderBy("level_id");
			if (variantsMappings.size() > 0) {
				int vm_index = 0;
				for (int j = 0; j < levels.size(); j++) {
					String cellContent = "";
					while (levels.get(j).getLongId().equals(variantsMappings.get(vm_index).getLong("level_id"))) {
						Object toVariantId = variantsMappings.get(vm_index).get("to_variant_id");
						String variantString;
						if (toVariantId == null) {
							variantString = rb.getString("noVariant_tableVariantsMapping");
						} else {
							variantString = Variant.findById(toVariantId).getString("name");
						}
						cellContent = cellContent + variantString + "\n";
						vm_index++;
						if (vm_index == variantsMappings.size()) {
							break;
						}
					}
					if (cellContent.length() > 0) {
						cellContent = cellContent.substring(0, cellContent.length() - 1); // removes
																							// last
																							// \n
					}
					tableContentsVM[i][j + 1] = cellContent;
				}
			} else { // virtual variant not given by an informant or no mapping
				for (int j = 0; j < levels.size(); j++) {
					tableContentsVM[i][j + 1] = rb.getString("virtualVariant_tableVariantsMapping");
				}
			}
		}
		
		MultiLineTable tableVariantsMapping = new MultiLineTable(tableContentsVM, columnNames);
		MultiLineCellRenderer multiLineCR = new MultiLineCellRenderer();
		tableVariantsMapping.getColumnModel().getColumn(0).setCellRenderer(multiLineCR);
		tableVariantsMapping.getColumnModel().getColumn(1).setCellRenderer(multiLineCR);
		tableVariantsMapping.getColumnModel().getColumn(2).setCellRenderer(multiLineCR);
		
		scrollPaneVariantsMapping.setViewportView(tableVariantsMapping);
		panelVariantsMapping.add(scrollPaneVariantsMapping, gbc_scrollPaneVariantsMapping);
	}
	
}
