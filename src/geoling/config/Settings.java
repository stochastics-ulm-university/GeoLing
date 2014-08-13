package geoling.config;

import geoling.util.Directory;
import geoling.util.ThreadedTodoWorker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/**
 * Class for computer-dependent settings, which are read from a file.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class Settings {
	
	/** Default path to the directory containing configuration files. */
	public static String PROPERTIES_PATH = "properties/";
	
	/** Default file name of the settings file (in the properties directory). */
	public static String DEFAULT_SETTINGS_FILENAME = "settings.properties";
	
	/** The object representing the desired locale. */
	private static Locale language = null;
	
	/** The identifier of the database which is to be used. */
	private static String databaseIdentifier = null;
	
	/**
	 * Loads the computer-dependent settings, uses the given settings file.
	 * 
	 * @param settings  the settings file
	 */
	public static void load(Properties settings) {
		// read number of threads to use from settings file (if given, otherwise use all)
		ThreadedTodoWorker.NUMBER_OF_THREADS = Integer.parseInt(settings.getProperty("THREADS", Integer.toString(ThreadedTodoWorker.NUMBER_OF_THREADS)));
		ThreadedTodoWorker.ENABLED = (ThreadedTodoWorker.NUMBER_OF_THREADS > 1);
		
		// read locale from settings file (if given, otherwise use default)
		String languageTag = settings.getProperty("LANGUAGE");
		if (languageTag == null || languageTag.isEmpty()) {
			language = Locale.getDefault();
		} else {
			language = Locale.forLanguageTag(languageTag);
		}
		
		// load database identifier from settings file (if given)
		databaseIdentifier = settings.getProperty("DATABASE");
	}
	
	/**
	 * Loads the computer-dependent settings, uses the given settings file.
	 * 
	 * @param settingsFileName  the filename of the settings file
	 * @throws IOException if an I/O error occurs, e.g., the settings file does not exist
	 */
	public static void load(String settingsFileName) throws IOException {
		Properties databaseSettings = new Properties();
		if (new File(settingsFileName).exists()) {
			databaseSettings.load(new FileInputStream(settingsFileName));
		}
		load(databaseSettings);
	}
	
	/**
	 * Loads the computer-dependent settings.
	 * 
	 * @throws IOException if an I/O error occurs, e.g., the settings file does not exist
	 */
	public static void load() throws IOException {
		load(PROPERTIES_PATH + DEFAULT_SETTINGS_FILENAME);
	}
	
	/**
	 * Returns the locale for the chosen language.
	 * 
	 * @return the language
	 */
	public static Locale getLanguage() {
		return language;
	}
	
	/**
	 * Returns the selected database identifier.
	 * 
	 * @return the database identifier
	 */
	public static String getDatabaseIdentifier() {
		return databaseIdentifier;
	}
	
	/**
	 * Sets the database identifier to a custom value.
	 * 
	 * @param databaseIdentifier  the database identifier
	 */
	public static void setDatabaseIdentifier(String databaseIdentifier) {
		Settings.databaseIdentifier = databaseIdentifier;
	}
	
	/**
	 * Ensures that the settings directory exists.
	 * 
	 * @throws IOException if an I/O error occurs
	 */
	public static void createDirectory() throws IOException {
		Directory.mkdir(Settings.PROPERTIES_PATH);
	}
	
}