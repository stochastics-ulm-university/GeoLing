package geoling.tools;

import geoling.config.Database;
import geoling.config.Settings;

import java.io.IOException;

/**
 * This program tests the database connection.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class DatabaseConnectionTest {

	/**
	 * Tests the database connection.
	 * 
	 * @param args  command line parameters (not required)
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Settings.load();
		Database.connect(Settings.getDatabaseIdentifier());
		System.out.println("OK.");
	}

}