package geoling.gui.management_dialog;

import geoling.gui.GeoLingGUI;
import geoling.gui.util.JFileChooserConfirmOverwrite;
import geoling.gui.util.StatusLabel;
import geoling.sql.SQLDumpWriter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Panel offering two export functions.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class DatabaseExportPanel {


	/** The panel to which contents are added. */
	private JPanel panel = new JPanel();

	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;

	public DatabaseExportPanel(final JTabbedPane tabbedPane, final Connection connection) {

		rb = ResourceBundle.getBundle("DatabaseExportPanel", GeoLingGUI.LANGUAGE);

		tabbedPane.insertTab(rb.getString("title_DatabaseExportPanel"), null, panel, null, DatabaseManagementDialog.TAB_EXPORT);

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 200, 150, 10, 0 };
		gridBagLayout.rowHeights = new int[] { 50, 50, 10, 0 };
		gridBagLayout.columnWeights = new double[] { 0.1, 0.1, 0.8,  Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0, 0, 1, Double.MIN_VALUE };
		panel.setLayout(gridBagLayout);

		final StatusLabel statusLabelExportSQL = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelExportSQL = new GridBagConstraints();
		gbc_statusLabelExportSQL.fill = GridBagConstraints.BOTH;
		gbc_statusLabelExportSQL.insets = new Insets(5, 5, 5, 5);
		gbc_statusLabelExportSQL.gridx = 1;
		gbc_statusLabelExportSQL.gridy = 0;
		panel.add(statusLabelExportSQL, gbc_statusLabelExportSQL);

		final JButton buttonExportSQL = new JButton(rb.getString("text_buttonExportSQL"));
		GridBagConstraints gbc_buttonExportSQL = new GridBagConstraints();
		gbc_buttonExportSQL.fill = GridBagConstraints.BOTH;
		gbc_buttonExportSQL.insets = new Insets(5, 5, 5, 5);
		gbc_buttonExportSQL.gridx = 0;
		gbc_buttonExportSQL.gridy = 0;
		panel.add(buttonExportSQL, gbc_buttonExportSQL);
		buttonExportSQL.addActionListener(new ActionListener() {
			/** opens window to choose file to which dump shall be written */
			public void actionPerformed(ActionEvent arg0) {
				String outputPath = null;
				JFileChooser chooser = new JFileChooserConfirmOverwrite(null);
				chooser.setFileFilter(new FileNameExtensionFilter(rb.getString("filter_sql"), "sql"));
				int returnVal = chooser.showSaveDialog(tabbedPane);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					outputPath = chooser.getSelectedFile().toPath().toAbsolutePath().toString();
				}
				if (outputPath==null) {
					return;
				}
				else {
					statusLabelExportSQL.changeStatus(2);
					buttonExportSQL.setEnabled(false);
					final String outputPathFinal = outputPath;
					Thread thread = new Thread(new Runnable() {
						public void run() {
							try {
								BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPathFinal), StandardCharsets.UTF_8));
								SQLDumpWriter dumpWriter = new SQLDumpWriter(connection, writer);
								dumpWriter.writeDump();
								statusLabelExportSQL.changeStatus(3);
								buttonExportSQL.setEnabled(true);
							} catch (IOException | SQLException e) {
								statusLabelExportSQL.changeStatus(4);
								buttonExportSQL.setEnabled(true);
								JOptionPane.showMessageDialog(panel, rb.getString("text_popupExportError"), rb.getString("title_popupExportError"), JOptionPane.ERROR_MESSAGE);
							}
						}
					});
					thread.start();

				}
			}
		});



		final StatusLabel statusLabelExportZIP = new StatusLabel(0);
		GridBagConstraints gbc_statusLabelExportZIP = new GridBagConstraints();
		gbc_statusLabelExportZIP.fill = GridBagConstraints.BOTH;
		gbc_statusLabelExportZIP.insets = new Insets(5, 5, 5, 5);
		gbc_statusLabelExportZIP.gridx = 1;
		gbc_statusLabelExportZIP.gridy = 1;
		panel.add(statusLabelExportZIP, gbc_statusLabelExportZIP);

		final JButton buttonExportZIP = new JButton(rb.getString("text_buttonExportZIP"));
		GridBagConstraints gbc_buttonExportZIP = new GridBagConstraints();
		gbc_buttonExportZIP.fill = GridBagConstraints.BOTH;
		gbc_buttonExportZIP.insets = new Insets(5, 5, 5, 5);
		gbc_buttonExportZIP.gridx = 0;
		gbc_buttonExportZIP.gridy = 1;
		panel.add(buttonExportZIP, gbc_buttonExportZIP);
		buttonExportZIP.addActionListener(new ActionListener() {
			/** opens window to choose zip file to which dump shall be written */
			public void actionPerformed(ActionEvent arg0) {
				String outputPath = null;
				JFileChooser chooser = new JFileChooserConfirmOverwrite(null);
				chooser.setFileFilter(new FileNameExtensionFilter(rb.getString("filter_zip"), "zip"));
				int returnVal = chooser.showSaveDialog(tabbedPane);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					outputPath = chooser.getSelectedFile().toPath().toAbsolutePath().toString();
				}
				if (outputPath==null) {
					return;
				}
				else {
					statusLabelExportZIP.changeStatus(2);
					buttonExportZIP.setEnabled(false);
					final String outputPathFinal = outputPath;
					Thread thread = new Thread(new Runnable() {
						public void run() {
							try {
								ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(outputPathFinal));
								ZipEntry zipEntry = new ZipEntry("dump.sql");
								stream.putNextEntry(zipEntry);
								Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
								SQLDumpWriter dumpWriter = new SQLDumpWriter(connection, writer);
								dumpWriter.writeDump();
								statusLabelExportZIP.changeStatus(3);
								buttonExportZIP.setEnabled(true);
							} catch (IOException | SQLException e) {
								statusLabelExportZIP.changeStatus(4);
								buttonExportZIP.setEnabled(true);
								e.printStackTrace();
								JOptionPane.showMessageDialog(panel, rb.getString("text_popupExportError")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupExportError"), JOptionPane.ERROR_MESSAGE);
							}
						}
					});
					thread.start();
				}
			}
		});


	}

}
