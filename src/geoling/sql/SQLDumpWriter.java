package geoling.sql;

import java.io.IOException;
import java.io.Writer;
import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;

/**
 * This class connects to a database and dumps all the tables and contents out to stdout in the form of
 * a set of SQL executable statements.<br>
 * 
 * You can use, modify and freely distribute this file as long as you credit Isocra Ltd.
 * There is no explicit or implied guarantee of functionality associated with this file, use it at your own risk.
 *
 * @author Copyright Isocra Ltd 2004
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class SQLDumpWriter {
	
	/** Quote character in front of and behind the column name. */
	public static String COL_QUOTE = "`";
	/** Quote character in front of and behind the table name. */
	public static String TABLE_QUOTE = "`";
	
	/** The number of table rows after which <code>StringBuffer</code> is written to file. */
	public static int ROW_BUFFER_SIZE = 300;
	
	/** The database connection. */
	private Connection connection;
	/** The <code>Writer</code> to the SQL script. */
	private Writer writer;
	/** The meta data of the database. */
	private DatabaseMetaData dbMetaData;
	
	/**
	 * Constructs object using a <code>Writer</code>
	 * 
	 * @param connection the connection to a SQL database
	 * @param writer the <code>Writer</code> to the SQL script (may contain a <code>ZipOutputStream</code>)
	 * @throws SQLException if error occurs for fetching meta data
	 */
	public SQLDumpWriter(Connection connection, Writer writer) throws SQLException {
		this.connection = connection;
		this.writer = writer;
		this.dbMetaData = connection.getMetaData();
	}
	
	/**
	 * Writes SQL-dump only containing <code>INSERT</code> statements.
	 * 
	 * @throws SQLException if any SQL errors occur
	 * @throws IOException if there is an error during writing
	 */
	public void writeDump() throws SQLException, IOException {
		String catalog = null;
		String schema = null;
		String tables = null;
		ResultSet rs = dbMetaData.getTables(catalog, schema, tables, null);
		if (! rs.next()) {
			System.err.println("Unable to find any tables matching: catalog="+catalog+" schema="+schema+" tables="+tables);
			rs.close();
		} 
		else {
			// Right, we have some tables, so we can go to work.
			// the details we have are
			// TABLE_NAME String => table name
			
			do {
				String tableName = rs.getString("TABLE_NAME");	
				// Right, we have a table, so we can go and dump it
				dumpTable(tableName);
				
				
			} while (rs.next());
			rs.close();
		}
		
		writer.close();
	}
	
	/** Dump this particular table and writes it using the <code>Writer</code>. 
	 * @throws IOException */
	private void dumpTable(String tableName) throws IOException {
		try {
			// First we output the create table stuff
			PreparedStatement stmt = connection.prepareStatement("SELECT * FROM "+tableName);
			ResultSet rs = stmt.executeQuery();
			ResultSetMetaData metaData = rs.getMetaData();
			int columnCount = metaData.getColumnCount();
			
			StringBuffer result = new StringBuffer();
			// Now we can output the actual data
			result.append("\n\n-- Dumping data for table "+TABLE_QUOTE+tableName+TABLE_QUOTE+"\n");
			
			int rowCount = 0;
			while (rs.next()) {
				// writes insert statement only every ROW_BUFFER_SIZE rows and writes the StringBuffer to file
				if ((rowCount%ROW_BUFFER_SIZE) == 0) {
					writer.write(result.toString());
					writer.flush();
					result = new StringBuffer();
					result.append("INSERT INTO "+TABLE_QUOTE+tableName+TABLE_QUOTE+" VALUES\n");
				}
				
				// writes values
				result.append("(");
				for (int i=0; i<columnCount; i++) {
					if (i > 0) {
						result.append(", ");
					}
					Object value = rs.getObject(i+1);
					if (value == null) {
						result.append("NULL");
					} else {
						// note: don't use "\" to escape special characters, this is not supported in SQLite
						// (for MySQL, we will use NO_BACKSLASH_ESCAPES SQL mode to avoid interpretation of
						//  "\" characters during importing; NO_BACKSLASH_ESCAPES is set in schema/MySQL.sql)
						result.append("'"+value.toString().replaceAll("'", "''")+"'");
					}
				}
				result.append(")");
				if ((rowCount%ROW_BUFFER_SIZE) == ROW_BUFFER_SIZE-1) {
					result.append(";\n");
				}
				else {
					result.append(",\n");
				}
				rowCount++;
			}
			// finally: remove last comma by semicolon and write the remaining lines to file
			result.replace(result.length()-2, result.length(), ";\n");
			writer.write(result.toString());
			writer.flush();
			
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			System.err.println("Unable to dump table "+tableName+" because: "+e);
		}
	}
	
}