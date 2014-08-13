package geoling.util;

import java.io.File;
import java.io.IOException;

/**
 * Class providing convenience methods for working with directories.
 *
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @version 1.1, 2014-07-24
 */
public class Directory {
	
	/**
	 * Checks whether the given path exists and is a directory.
	 *
	 * @param path  the path to check
	 * @return <code>true</code> if the directory exists, <code>false</code>
	 *         if there exists no directory (or file) with the given name
	 * @throws IOException if the given path points to a file, i.e., not a directory
	 */
	public static boolean exists(String path) throws IOException {
		File file = new File(path);
		if (file.exists()) {
			if (file.isDirectory()) {
				return true;
			} else {
				throw new IOException("Given path points to an existing file, not a directory: "+path);
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Creates a directory with the specified path recursively.
	 *
	 * @param path  the path to create if it doesn't exist already
	 * @throws IOException if creating the directory failed, i.e., it does not exist
	 */
	public static void mkdir(String path) throws IOException {
		File file = new File(path);
		if (!file.exists()) {
			if (!file.mkdirs()) {
				throw new IOException("Could not create directory: "+path);
			}
		}
	}
	
	/**
	 * Ensures that the given path to a directory ends with a path
	 * separator (either "/" or "\") - adds the separator if necessary.
	 * 
	 * @param path  the path
	 * @return the path, ends with a path separator
	 */
	public static String ensureTrailingSlash(String path) {
		if (path.endsWith("/") || path.endsWith("\\")) {
			return path;
		} else {
			return path + File.separator;
		}
	}
	
	/**
	 * Creates a directory with the specified path recursively.
	 *
	 * @param path  the path to create if it doesn't exist already
	 * @throws IOException if the given path does not exist already or if the path
	 *                     points to a file (i.e., not a directory)
	 */
	public static boolean isEmpty(String path) throws IOException {
		File file = new File(path);
		if (file.isDirectory()) {
			return (file.list().length == 0);
		} else {
			throw new IOException("The given path does not exist or it is not a directory: "+path);
		}
	}
	
}