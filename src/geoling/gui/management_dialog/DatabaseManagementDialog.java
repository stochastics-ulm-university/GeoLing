package geoling.gui.management_dialog;

import geoling.config.Database;
import geoling.gui.GeoLingGUI;

import java.awt.Toolkit;
import java.awt.Window;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

/**
 * Database management dialog.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class DatabaseManagementDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	
	/** The index in the <code>JTabbedPane</code> for <code>DatabaseImportPanel</code>. */
	final public static int TAB_IMPORT = 0;
	/** The index in the <code>JTabbedPane</code> for <code>DatabaseExportPanel</code>. */
	final public static int TAB_EXPORT = 1;
	/** The index in the <code>JTabbedPane</code> for <code>DatabaseConfigurationPanel</code>. */
	final public static int TAB_CONFIGURATION = 2;
	/** The index in the <code>JTabbedPane</code> for <code>DatabaseDistancePanel</code>. */
	final public static int TAB_DISTANCE = 3;
	/** The index in the <code>JTabbedPane</code> for <code>DatabaseBandwidthPanel</code>. */
	final public static int TAB_BANDWIDTH = 4;


	/** To this <code>JTabbedPane</code> new panels can be added or removed */
	private JTabbedPane tabbedPane;

	/** The current connection to a MySQL or SQLite database. */
	private Connection connection;
	
	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;

	public DatabaseManagementDialog() throws IOException {
		super((Window)null);
		
		rb = ResourceBundle.getBundle("DatabaseManagementDialog", GeoLingGUI.LANGUAGE);
		
		this.setModal(true);
		this.setTitle(rb.getString("title_DatabaseManagementDialog"));
		if (Toolkit.getDefaultToolkit().getScreenSize().getWidth() < 1024 ||
		    Toolkit.getDefaultToolkit().getScreenSize().getHeight() < 768) {
			this.setBounds(0, 0, 800, 576);
		} else {
			this.setBounds(0, 0, 1024, 600);
		}

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		this.add(tabbedPane);

		// read database settings
		InputStream databaseSettingsInputStream = new FileInputStream(Database.getFileName(GeoLingGUI.DATABASE_IDENTIFIER));
		Properties databaseSettings = new Properties();
		databaseSettings.load(databaseSettingsInputStream);

		// connect to database (ActiveJDBC)
		Database.connect(databaseSettings);

		// create MySQL connection for execution of SQL queries
		if (databaseSettings.getProperty("DBTYPE").equals("MySQL")) {
			String dbName = databaseSettings.getProperty("DBNAME");
			String dbHost = databaseSettings.getProperty("DBHOST");
			String dbUser = databaseSettings.getProperty("DBUSER");
			String dbPass = databaseSettings.getProperty("DBPASSWORD");

			try {
				// load driver 
				Class.forName("com.mysql.jdbc.Driver");

				try {
					// enable connection to database
					String urlDB = "jdbc:mysql://"+dbHost+":3306/"+dbName;
					this.connection = DriverManager.getConnection(urlDB, dbUser, dbPass);

				}
				catch (SQLException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(this, rb.getString("text_connectionFailedMySQL")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_connectionFailedMySQL"), JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}

			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, rb.getString("text_driverFailedMySQL")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_driverFailedMySQL"), JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		}
		// create SQLite connection for execution of SQL queries
		else {
			try {
				// load driver 
				Class.forName("org.sqlite.JDBC");

				try {
					// enable connection to database
					String urlDB = "jdbc:sqlite:"+databaseSettings.getProperty("FILENAME");
					this.connection = DriverManager.getConnection(urlDB);

				}
				catch (SQLException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(this, rb.getString("text_connectionFailedSQLite")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_connectionFailedSQLite"), JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}

			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, rb.getString("text_driverFailedSQLite")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_driverFailedSQLite"), JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		}


		// construct the tabs
		new DatabaseImportPanel(tabbedPane, connection, databaseSettings.getProperty("DBTYPE"));
		new DatabaseExportPanel(tabbedPane, connection);
		new DatabaseConfigurationPanel(tabbedPane);
		new DatabaseDistancePanel(tabbedPane);
		new DatabaseBandwidthPanel(tabbedPane);

		this.setVisible(true);

	}


}
