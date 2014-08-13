package geoling.sql;

/*
 * Slightly modified version of the com.ibatis.common.jdbc.ScriptRunner class
 * from the iBATIS Apache project. Only removed dependency on Resource class
 * and a constructor
 */
/*
 *  Copyright 2004 Clinton Begin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Class to run database scripts (which may be contained in a zip file).
 * 
 * @author Clinton Begin
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class SQLReader {

	/** The default delimiter after statements. */
	public static final String DEFAULT_DELIMITER = ";";
	/** The program terminates on the first error, if <code>true</code>. */
	public static boolean STOP_ON_ERROR = true;
	/** The auto-commit mode. */
	public static boolean AUTO_COMMIT = false;

	/** The database connection. */
	private Connection connection;
	/** A <code>Reader</code> for the SQL script. */
	private Reader reader;


	private PrintWriter logWriter = new PrintWriter(System.out);
	private PrintWriter errorLogWriter = new PrintWriter(System.err);

	private String delimiter = DEFAULT_DELIMITER;
	private boolean fullLineDelimiter = false;

	/**
	 * Constructs object.
	 * 
	 * @param connection the connection to a SQL database
	 * @param reader the <code>Reader</code> to the SQL script
	 */
	public SQLReader(Connection connection, Reader reader) {
		this.connection = connection;
		this.reader = reader;
	}


	/**
	 * Constructs object for a <code>ZipFile</code>.
	 * 
	 * @param connection the connection to a SQL database
	 * @param zipInput the input <code>ZipFile</code> containing the SQL script
	 * @param charset the <code>Charset</code> which gives the encoding of the sql dump
	 * @throws IOException if <code>getInputStream(ZipEntry)</code> fails.
	 */
	public SQLReader(Connection connection, ZipFile zipInput, Charset charset) throws IOException {
		this.connection = connection;

		Enumeration<? extends ZipEntry> enumeration = zipInput.entries();
		int countEntry = 0;
		while (enumeration.hasMoreElements()) {
			countEntry++;
			if (countEntry>1) {
				throw new IllegalArgumentException("ZipFile must contain only one file: " + zipInput.getName());
			}
			ZipEntry entry = enumeration.nextElement();
			InputStream inputStream = zipInput.getInputStream(entry);
			this.reader = new InputStreamReader(inputStream, charset);
		}
		if (countEntry==0) {
			throw new IllegalArgumentException("ZipFile must contain one file: " + zipInput.getName());
		}
	}

	/**
	 * Runs an SQL script that is read from the <code>Reader</code>
	 *
	 * @throws SQLException if any SQL error occurs.
	 * @throws IOException  if any error during reading occurs.
	 */
	public void runScript() throws IOException, SQLException {
		boolean originalAutoCommit = connection.getAutoCommit();
		if (originalAutoCommit != AUTO_COMMIT) {
			connection.setAutoCommit(AUTO_COMMIT);
		}
		runScript(connection, reader);
		connection.setAutoCommit(originalAutoCommit);
	}

	/**
	 * Runs an SQL script (read in using the Reader parameter) using the
	 * connection passed in
	 *
	 * @param conn
	 *            - the connection to use for the script
	 * @param reader
	 *            - the source of the script
	 * @throws SQLException
	 *             if any SQL errors occur
	 * @throws IOException
	 *             if there is an error reading from the Reader
	 */
	private void runScript(Connection conn, Reader reader) throws IOException, SQLException {
		StringBuffer command = null;
		try {
			LineNumberReader lineReader = new LineNumberReader(reader);
			String line = null;
			while ((line = lineReader.readLine()) != null) {
				if (command == null) {
					command = new StringBuffer();
				}
				String trimmedLine = line.trim();
				if (trimmedLine.startsWith("--")) {
				} 
				else if (trimmedLine.length() < 1 || trimmedLine.startsWith("//")) {
					// Do nothing
				} 
				else if (trimmedLine.length() < 1 || trimmedLine.startsWith("--")) {
					// Do nothing
				} 
				else if (!fullLineDelimiter	&& trimmedLine.endsWith(getDelimiter())	|| fullLineDelimiter && trimmedLine.equals(getDelimiter())) {
					command.append(line.substring(0, line.lastIndexOf(getDelimiter())));
					command.append(" ");
					Statement statement = conn.createStatement();

					if (STOP_ON_ERROR) {
						statement.execute(command.toString());
					} else {
						try {
							statement.execute(command.toString());
						} catch (SQLException e) {
							System.err.println("Error executing: " + command);
							e.printStackTrace();
						}
					}

					if (AUTO_COMMIT && !conn.getAutoCommit()) {
						conn.commit();
					}


					command = null;
					try {
						statement.close();
					} catch (Exception e) {
						// Ignore to workaround a bug in Jakarta DBCP
					}
					Thread.yield();
				} 
				else {
					command.append(line);
					command.append(" ");
				}
			}
			if (!AUTO_COMMIT) {
				conn.commit();
			}
		} catch (SQLException e) {
			System.err.println("Error executing: " + command);
			e.printStackTrace();
			throw e;
		} catch (IOException e) {
			System.err.println("Error executing: " + command);
			throw e;
		} finally {
			conn.rollback();
			flush();
		}
	}


	public void setDelimiter(String delimiter, boolean fullLineDelimiter) {
		this.delimiter = delimiter;
		this.fullLineDelimiter = fullLineDelimiter;
	}

	/**
	 * Setter for logWriter property
	 *
	 * @param logWriter
	 *            - the new value of the logWriter property
	 */
	public void setLogWriter(PrintWriter logWriter) {
		this.logWriter = logWriter;
	}

	/**
	 * Setter for errorLogWriter property
	 *
	 * @param errorLogWriter
	 *            - the new value of the errorLogWriter property
	 */
	public void setErrorLogWriter(PrintWriter errorLogWriter) {
		this.errorLogWriter = errorLogWriter;
	}



	private String getDelimiter() {
		return delimiter;
	}



	private void flush() {
		if (logWriter != null) {
			logWriter.flush();
		}
		if (errorLogWriter != null) {
			errorLogWriter.flush();
		}
	}
}


