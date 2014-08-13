package geoling.models;

import org.javalite.activejdbc.DBException;

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;

/**
 * A single option for configuration of the software or global values/parameters.
 * 
 * All available options:
 * <ul>
 * <li>useAllLocationsInDensityEstimation: boolean value that determines whether
 *     density values should be estimated for all locations in the database
 *     (by default, densities are only estimated for locations having data
 *     for the currently processed map)
 * <li>ignoreFrequenciesInDensityEstimation: boolean value that determines whether
 *     we only use the relative frequencies (weights) of answers at locations in the
 *     density estimation instead of absolute frequencies; this is useful to reduce
 *     the effect of a large number of answers in a city to spread to the surrounding
 *     area
 * <li>useLocationAggregation: boolean value that determines whether locations
 *     have to be aggregated (necessary if two or more locations have the same
 *     geographical coordinates)
 * <li>plotLocationCodes: boolean value that determines whether the short
 *     locations codes should be plotted instead of points
 * <li>defaultBorderId: ID of the default border
 * </ul>
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class ConfigurationOption extends ExtendedModel {
	
	/** The string value used for boolean <code>true</code>. */
	public final static String TRUE_VALUE = "1";
	
	/** The string value used for boolean <code>false</code>. */
	public final static String FALSE_VALUE = "0";
	
	static {
		validatePresenceOf("name");
	}
	
	/**
	 * Reads the value for the given option with fallback to
	 * the given default value.
	 * 
	 * @param name          the identifier of the option
	 * @param defaultValue  the default value
	 * @return the value
	 */
	public static String getOption(String name, String defaultValue) {
		try {
			ConfigurationOption option = ConfigurationOption.findFirst("name = ?", name);
			
			if (option == null) {
				return defaultValue;
			} else {
				String result = option.getString("value");
				if (result == null) {
					return defaultValue;
				} else {
					return result;
				}
			}
		} catch (DBException e) {
			if (e.getCause() instanceof MySQLSyntaxErrorException) {
				// just return default value if e.g. table does not exist yet
				return defaultValue;
			} else {
				// re-throw exception on other problems, e.g., no database connection
				throw e;
			}
		}
	}
	
	/**
	 * Reads the integer value for the given option with fallback to
	 * the given default value.
	 * 
	 * @param name          the identifier of the option
	 * @param defaultValue  the default value
	 * @return the value
	 */
	public static int getOption(String name, int defaultValue) {
		return Integer.parseInt(getOption(name, Integer.toString(defaultValue)));
	}
	
	/**
	 * Reads the boolean value for the given option with fallback to
	 * the given default value.
	 * 
	 * @param name          the identifier of the option
	 * @param defaultValue  the default value
	 * @return the value
	 */
	public static boolean getOption(String name, boolean defaultValue) {
		String value = getOption(name, defaultValue ? TRUE_VALUE : FALSE_VALUE);
		if (value.equals(TRUE_VALUE)) {
			return true;
		} else if (value.equals(FALSE_VALUE)) {
			return false;
		} else {
			throw new RuntimeException("Option \""+name+"\": Value \""+value+"\" is not \""+FALSE_VALUE+"\" or \""+TRUE_VALUE+"\"!");
		}
	}
	
	/**
	 * Writes the value for the given option.
	 * 
	 * @param name   the identifier of the option
	 * @param value  the value to write
	 */
	public static void setOption(String name, String value) {
		ConfigurationOption option = ConfigurationOption.findFirst("name = ?", name);
		if (option == null) {
			option = new ConfigurationOption();
			option.setString("name", name);
		}
		option.setString("value", value);
		option.saveIt();
	}
	
	/**
	 * Writes the integer value for the given option.
	 * 
	 * @param name   the identifier of the option
	 * @param value  the value to write
	 */
	public static void setOption(String name, int value) {
		setOption(name, Integer.toString(value));
	}
	
	/**
	 * Writes the boolean value for the given option.
	 * 
	 * @param name   the identifier of the option
	 * @param value  the value to write
	 */
	public static void setOption(String name, boolean value) {
		setOption(name, value ? TRUE_VALUE : FALSE_VALUE);
	}

}