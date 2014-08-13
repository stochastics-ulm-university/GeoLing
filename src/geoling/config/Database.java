package geoling.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.DB;
import org.sqlite.SQLiteConnectionPoolDataSource;

import javax.sql.DataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

/**
 * Class for establishing the database connection.
 * Note that activejdbc establishes a connection only for the current thread,
 * i.e., you have to call <code>ensureConnection</code> for every new thread
 * that requires the database.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Database {
	
	/** Default database connection identifier used in <code>Base</code> class of <code>activejdbc</code>. */
	private static final String DEFAULT_DB_NAME = "default";
	
	/** The default file name of the database settings file (in the properties directory), "%s" is replaced by the database identifier. */
	public static String DEFAULT_DATABASE_SETTINGS_FILENAME = "database%s.properties";
	
	/** The global database information object. */
	private static Database dbObj = null;
	
	/** MySQL/SQLite connection pool. */
	private DataSource dataSource;
	
	/**
	 * Constructs the database information object.
	 * 
	 * @param hostname  the host name
	 * @param database  the database name
	 * @param user      the user name
	 * @param password  the password
	 * @param options   the options string which is appended, should be <code>null</code>
	 */
	private Database(String hostname, String database, String user, String password, String options) {
		hostname = (hostname == null) ? "localhost" : hostname;
		int port = 3306;
		database = (database == null) ? ""          : database;
		user     = (user == null)     ? "root"      : user;
		password = (password == null) ? ""          : password;
		
		if (hostname.isEmpty() || database.isEmpty() || user.isEmpty()) {
			throw new IllegalArgumentException("Host name, database name and user name required!");
		}
		
		MysqlConnectionPoolDataSource ds = new MysqlConnectionPoolDataSource();
		ds.setServerName(hostname);
		ds.setPort(port);
		ds.setUser(user);
		ds.setPassword(password);
		ds.setDatabaseName(database);
		this.dataSource = ds;
	}
	
	private Database(String sqliteFileName) {
		SQLiteConnectionPoolDataSource ds = new SQLiteConnectionPoolDataSource();
		ds.setUrl("jdbc:sqlite:"+sqliteFileName);
		this.dataSource = ds;
	}
	
	/**
	 * Connects to the database (for the current thread), if there is no connection already.
	 */
	public static void ensureConnection() {
		if (!Base.hasConnection()) {
			connect();
		}
	}
	
	/**
	 * Connects to the database (for the current thread), using the database information already present.
	 */
	private static void connect() {
		if (Base.hasConnection()) {
			Base.close();
		}
		if (dbObj == null) {
			throw new RuntimeException("No database connection information!");
		}
		new DB(DEFAULT_DB_NAME).open(dbObj.dataSource);
	}

	/**
	 * Establishes the database connection (creates a connection pool and connects for the
	 * current thread), uses the given database settings file.
	 * 
	 * @param databaseSettings  the database settings file
	 */
	public static void connect(Properties databaseSettings) {
		if (dbObj != null) {
			throw new RuntimeException("Database connection may only be initialized once!");
		}
		
		if (databaseSettings.getProperty("DBTYPE").equals("SQLite")) {
			dbObj = new Database(databaseSettings.getProperty("FILENAME"));
		} else if (databaseSettings.getProperty("DBTYPE").equals("MySQL")) {
			dbObj = new Database(databaseSettings.getProperty("DBHOST"),
			                     databaseSettings.getProperty("DBNAME"),
			                     databaseSettings.getProperty("DBUSER"),
			                     databaseSettings.getProperty("DBPASSWORD"),
			                     databaseSettings.getProperty("DBOPTIONS"));
		} else {
			throw new IllegalArgumentException("DBTYPE = SQLite or DBTYPE = MySQL required!");
		}
		
		try {
			connect();
		} catch (RuntimeException e) {
			// reset database connection information and re-throw the exception
			dbObj = null;
			throw e;
		}
	}
	
	/**
	 * Establishes the database connection (creates a connection pool and connects for the
	 * current thread), uses the given database  identifier to load the corresponding
	 * database settings file.
	 * 
	 * @param databaseIdentifier  the identifier of the database
	 * @throws IOException if an I/O error occurs, e.g., the settings file does not exist
	 */
	public static void connect(String databaseIdentifier) throws IOException {
		Properties databaseSettings = new Properties();
		databaseSettings.load(new FileInputStream(getFileName(databaseIdentifier)));
		connect(databaseSettings);
	}
	
	/**
	 * Constructs the database settings file name.
	 * 
	 * @param databaseIdentifier  the identifier of the database
	 * @return the file name
	 */
	public static String getFileName(String databaseIdentifier) {
		if (databaseIdentifier == null || databaseIdentifier.isEmpty()) {
			throw new IllegalArgumentException("Database identifier required!");
		}
		return Settings.PROPERTIES_PATH + String.format(DEFAULT_DATABASE_SETTINGS_FILENAME, databaseIdentifier);
	}
	
}