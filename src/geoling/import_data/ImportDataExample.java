package geoling.import_data;

import geoling.config.Database;
import geoling.config.Settings;
import geoling.maps.density.bandwidth.computation.ComputeBandwidths;
import geoling.maps.distances.computation.LinguisticDistanceComputation;
import geoling.models.*;
import geoling.util.Directory;
import geoling.util.ProgressOutput;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;

/**
 * Provides several static methods to import data from text files (.txt and .csv format).
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 * @version 1.0, 16.12.2013
 */
public class ImportDataExample {
	
	/** The used <code>Charset</code> of the input files (default: <code>UTF-8</code>). */
	public static Charset USED_CHARSET = StandardCharsets.UTF_8;
	
	/** The delimiter that separates columns in the .csv files.	 */
	public static String DELIMITER_COLUMNS = ";";
	
	/** The delimiter that separates multiple answers of an informant for a given map. */ 
	public static String DELIMITER_REGEX_MULTIPLE_ANSWERS = "\\|";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		long begin = System.currentTimeMillis();

		String importFolder = "./import/";
		USED_CHARSET = StandardCharsets.ISO_8859_1;

		Settings.load();
		Settings.setDatabaseIdentifier("ICECREAM");
		Database.connect(Settings.getDatabaseIdentifier());

		deleteAllTables();

		System.out.println("Set configuration options...");
		ConfigurationOption.setOption("useAllLocationsInDensityEstimation", true);
		ConfigurationOption.setOption("ignoreFrequenciesInDensityEstimation", false);
		ConfigurationOption.setOption("useLocationAggregation", false);
		ConfigurationOption.setOption("plotLocationCodes", true);
		
		Distance distance = new Distance();
		distance.set("name", "Geographical distance");
		distance.set("type", "geographic");
		distance.saveIt();

		System.out.println("Read locations...");
		readLocations(importFolder+"locations.csv");

		System.out.println("Read maps...");
		readMaps(importFolder+"maps.csv");

		System.out.println("Read informant answers...");
		readInformantAnswers(importFolder+"informant_answers.csv", 4);

		System.out.println("Read border...");
		readBorderCoordinates(importFolder+"border_coordinates.csv", "BorderGermanyAustriaSwitzerland");

		System.out.println("Read distances...");
		readDistances(importFolder+"distances.csv", "Random distance", "random_distance");

		System.out.println("Read categories...");
		readCategories(importFolder+"categories.txt");

		System.out.println("Read categories_maps...");
		readCategoriesMaps(importFolder+"categories_maps.txt");

		System.out.println("Read groups_maps...");
		readGroupsMaps(importFolder+"groups_maps.txt");

		System.out.println("Write files with all variants of each map...");
		prepareVariantsMappings(importFolder+"variants_mappings/");

		System.out.println("Read variants_mappings...");
		readVariantsMappings(importFolder+"variants_mappings_with_levels/");

		System.out.println("Compute linguistic distances...");
		LinguisticDistanceComputation.computeDistances(new ProgressOutput(System.out));

		System.out.println("Compute bandwidths..."); 
		{
			LazyList<Group> groups = Group.findAll();
			ArrayList<Group> relevantGroups = new ArrayList<Group>();
			for (Group group : groups) {
				relevantGroups.add(group);
			}
			ComputeBandwidths.computeBandwidths(null, null, ComputeBandwidths.getDefaultEstimators(relevantGroups), true, false);
		}

		System.out.println("Required time for import: " + (System.currentTimeMillis()-begin)/1000 + " s.");

	}





	/**
	 * Deletes all contents from the connected database.
	 * Uses <code>truncate</code> operation.
	 */
	public static void deleteAllTables() {
		String[] tableNames = {"bandwidths", "border_coordinates", "borders", "categories",  
				"categories_maps", "configuration_options",	"distances", "groups", "groups_maps", 
				"informants", "interview_answers", "interviewers", "levels", "location_distances", "locations", 
				"maps", "tags", "variants", "variants_mappings"};
		for (String tableName : tableNames) {
			String query = "TRUNCATE TABLE " + tableName;
			Base.exec(query);
		}

	}


	/**
	 * Reads .csv file and saves information in Table <code>locations</code>.
	 * @param filename the path of the .csv file
	 */
	public static void readLocations(String filename) {
		int requiredColumns = 4;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;

			// read header, check if enough input columns
			line = br.readLine();
			String[] header = line.split(DELIMITER_COLUMNS);
			// check if enough input columns
			int inputColumns = header.length;
			if (inputColumns<requiredColumns) {
				throw new IllegalArgumentException("Not enough input columns for file: " + filename);
			}
			// look for optional tags and get their names
			int tagColumns = inputColumns-requiredColumns;
			String[] tagNames;
			if (tagColumns>0) {
				tagNames = new String[tagColumns];
				for (int t=0; t<tagColumns; t++) {
					tagNames[t] = header[requiredColumns+t].substring(1, header[requiredColumns+t].length()-1);
				}
			}
			else {
				tagNames = null;
			}

			// read all further lines and create one entry for each line
			int countLine = 0;
			while ((line=br.readLine())!=null) {
				countLine++;
				String[] split = line.split(DELIMITER_COLUMNS);
				if (split.length!=inputColumns) {
					throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
				}
				Location location = new Location();
				location.set("name", split[0]);
				location.set("code", split[1]);
				location.set("latitude", split[2]);
				location.set("longitude", split[3]);
				location.saveIt();
				// add optional tags
				for (int t=0; t<tagColumns; t++) {
					if (split[requiredColumns+t].length()>0) {
						Tag tag = new Tag();
						tag.set("name", tagNames[t]);
						tag.set("value", split[requiredColumns+t]);
						location.add(tag);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("IOException in readLocations.");
			e.printStackTrace();
		}
	}

	/**
	 * Reads .csv file and saves information in Table <code>maps</code>.
	 * @param filename the path of the .csv file
	 */
	public static void readMaps(String filename) {
		int requiredColumns = 1;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;

			// read header, check if enough input columns
			line = br.readLine();
			String[] header = line.split(DELIMITER_COLUMNS);
			// check if enough input columns
			int inputColumns = header.length;
			if (inputColumns<requiredColumns) {
				throw new IllegalArgumentException("Not enough input columns for file: " + filename);
			}
			// look for optional tags and get their names
			int tagColumns = inputColumns-requiredColumns;
			String[] tagNames;
			if (tagColumns>0) {
				tagNames = new String[tagColumns];
				for (int t=0; t<tagColumns; t++) {
					tagNames[t] = header[requiredColumns+t].substring(1, header[requiredColumns+t].length()-1);
				}
			}
			else {
				tagNames = null;
			}

			// read all further lines and create one entry for each line
			int countLine = 0;
			while ((line=br.readLine())!=null) {
				countLine++;
				String[] split = line.split(DELIMITER_COLUMNS);
				if (split.length!=inputColumns) {
					throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
				}
				Map map = new Map();
				map.set("name", split[0]);
				map.saveIt();
				// add optional tags
				for (int t=0; t<tagColumns; t++) {
					if (split[requiredColumns+t].length()>0) {
						Tag tag = new Tag();
						tag.set("name", tagNames[t]);
						tag.set("value", split[requiredColumns+t]);
						map.add(tag);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("IOException in readMaps.");
			e.printStackTrace();
		}
	}


	/**
	 * Reads .csv file and saves information in the following tables:<br>
	 * - <code>informants</code><br>
	 * - <code>interviewers</code><br>
	 * - <code>interview_answers</code><br>
	 * - <code>variants</code>
	 * @param filename the path of the .csv file
	 * @param nrMaps the number of maps for which answers are given in this file
	 */
	public static void readInformantAnswers(String filename, int nrMaps) {
		// check if all necessary tables are non-empty
		if (Location.count()==0) {
			throw new IllegalArgumentException("Table Locations must not be empty!");
		}
		if (Map.count()==0) {
			throw new IllegalArgumentException("Table Maps must not be empty!");
		}
		int requiredColumns = 3 + nrMaps;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;

			// read header, check if enough input columns
			line = br.readLine();
			String[] header = line.split(DELIMITER_COLUMNS);
			// check if enough input columns
			int inputColumns = header.length;
			if (inputColumns<requiredColumns) {
				throw new IllegalArgumentException("Not enough input columns for file: " + filename);
			}

			// check if the names of the maps in the header are contained in table Maps and get the id of each map
			Object[] mapIds = new Object[nrMaps];
			for (int m=0; m<nrMaps; m++) {
				LazyList<Map> maps = Map.find("name = ?", header[3+m]);
				if (maps.size()==1) {
					mapIds[m] = maps.get(0).getId();
				}
				else {
					if (maps.size()==0) {
						throw new IllegalArgumentException("Name of map is not contained in table Maps: " + header[3+m]);
					}
					if (maps.size()>1) {
						throw new IllegalArgumentException("Map name is not unique: " + header[3+m]);
					}
				}

			}

			// look for optional tags and get their names
			int tagColumns = inputColumns-requiredColumns;
			String[] tagNames;
			if (tagColumns>0) {
				tagNames = new String[tagColumns];
				for (int t=0; t<tagColumns; t++) {
					tagNames[t] = header[requiredColumns+t].substring(1, header[requiredColumns+t].length()-1);
				}
			}
			else {
				tagNames = null;
			}


			// put all given answers (=variants) in a cache to avoid too many database queries
			// a HashMap saves Key-Value pairs for unique keys
			// here: Key: the name of the variant, Value: the id of the variant
			List<HashMap<String, Object>> listOfVariants = new ArrayList<HashMap<String, Object>>();
			for (int m=0; m<nrMaps; m++) {
				listOfVariants.add(new HashMap<String, Object>());
			}

			// read all further lines and create entries in various tables for each line
			int countLine = 0;
			while ((line=br.readLine())!=null) {
				countLine++;
				String[] split = line.split(DELIMITER_COLUMNS);
				if (split.length!=inputColumns) {
					throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
				}
				// create entry in table Informants
				Informant informant = new Informant();
				informant.set("name", split[0]);
				// get id of location and check if location is consistent
				LazyList<Location> locations = Location.find("name = ?", split[1]);
				if (locations.size()==0) {
					throw new IllegalArgumentException("Name of location is not contained in table Locations: " + split[1]);
				}
				if (locations.size()>1) {
					throw new IllegalArgumentException("Location name is not unique: " + split[1]);
				}
				informant.set("location_id", locations.get(0).getId());
				informant.saveIt();
				Object informantId = informant.getId();

				// add optional tags to informant
				for (int t=0; t<tagColumns; t++) {
					if (split[requiredColumns+t].length()>0) {
						Tag tag = new Tag();
						tag.set("name", tagNames[t]);
						tag.set("value", split[requiredColumns+t]);
						informant.add(tag);
					}
				}

				// get id of interviewer
				Object interviewerId;
				// if interviewer of current line is not already in table Interviewers
				if (Interviewer.first("name = ?", split[2])==null) {
					// create entry in table Interviewers
					Interviewer interviewer = new Interviewer();
					interviewer.set("name", split[2]);
					interviewer.saveIt();
					interviewerId = interviewer.getId();
				}
				else {
					interviewerId = Interviewer.first("name = ?", split[2]).getId();
				}

				// iterate over the maps given in the file
				for (int m=0; m<nrMaps; m++) {
					String[] answers = split[3+m].split(DELIMITER_REGEX_MULTIPLE_ANSWERS);
					if (answers.length>0) {
						for (int a=0; a<answers.length; a++) {
							String answer = answers[a];
							if (answer.length()==0) {
								continue;
							}
							Object variantId;
							// if variant is already in cache
							if (listOfVariants.get(m).containsKey(answer)) {
								variantId = listOfVariants.get(m).get(answer);
							}
							else {
								// create entry in table Variants
								Variant variant = new Variant();
								variant.set("map_id", mapIds[m]);
								variant.set("name", answer);
								variant.saveIt();
								variantId = variant.getId();
								listOfVariants.get(m).put(answer, variantId);
							}

							// create entry in table Interview_Answers
							InterviewAnswer interviewAnswer = new InterviewAnswer();
							interviewAnswer.set("interviewer_id", interviewerId);
							interviewAnswer.set("informant_id", informantId);
							interviewAnswer.set("variant_id", variantId);
							interviewAnswer.saveIt();
						}
					}
				}

			}
			
		} catch (IOException e) {
			System.err.println("IOException in readInformantAnswers.");
			e.printStackTrace();
		}
	}


	/**
	 * Reads .csv file and saves information in Table <code>borders</code> and <code>border_coordinates</code>.
	 * @param filename the path of the .csv file
	 * @param borderName the name of the given border
	 */
	public static void readBorderCoordinates(String filename, String borderName) {
		int requiredColumns = 3;
		Border border = new Border();
		border.set("name", borderName);
		border.saveIt();
		Object borderId = border.getId();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;

			// read header, check if enough input columns
			line = br.readLine();
			String[] header = line.split(DELIMITER_COLUMNS);
			// check if enough input columns
			int inputColumns = header.length;
			if (inputColumns<requiredColumns) {
				throw new IllegalArgumentException("Not enough input columns for file: " + filename);
			}
			// look for optional tags and get their names
			int tagColumns = inputColumns-requiredColumns;
			String[] tagNames;
			if (tagColumns>0) {
				tagNames = new String[tagColumns];
				for (int t=0; t<tagColumns; t++) {
					tagNames[t] = header[requiredColumns+t].substring(1, header[requiredColumns+t].length()-1);
				}
			}
			else {
				tagNames = null;
			}

			// read all further lines and create one entry for each line
			int countLine = 0;
			while ((line=br.readLine())!=null) {
				countLine++;
				String[] split = line.split(DELIMITER_COLUMNS);
				if (split.length!=inputColumns) {
					throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
				}
				BorderCoordinate borderCoordinate = new BorderCoordinate();
				borderCoordinate.set("border_id", borderId);
				borderCoordinate.set("order_index", split[0]);
				borderCoordinate.set("latitude", split[1]);
				borderCoordinate.set("longitude", split[2]);
				borderCoordinate.saveIt();
				// add optional tags
				for (int t=0; t<tagColumns; t++) {
					if (split[requiredColumns+t].length()>0) {
						Tag tag = new Tag();
						tag.set("name", tagNames[t]);
						tag.set("value", split[requiredColumns+t]);
						borderCoordinate.add(tag);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("IOException in readBorderCoordinates.");
			e.printStackTrace();
		}
	}


	/**
	 * Reads .csv file and saves information in Table <code>distances</code>.
	 * @param filename the path of the .csv file
	 * @param distanceName the name of the distance which is read
	 * @param distanceIdentification a <code>String</code> identifier for the distance
	 */
	public static void readDistances(String filename, String distanceName, String distanceIdentification) {
		int requiredColumns = 3;
		
		// create new entry in table distances
		Distance distance = new Distance();
		distance.set("name", distanceName);
		distance.set("type", "precomputed");
		distance.set("identification", distanceIdentification);
		distance.saveIt();
		Object distanceId = distance.getId();

		// fetch all locations to get their ids and put them in a catch to reduce the number of database queries
		LazyList<Location> locations = Location.findAll();
		// a HashMap saves Key-Value pairs for unique keys
		// here: Key: the name of the location, Value: the id of the location
		HashMap<String, Object> locationsMap = new HashMap<String, Object>();
		for (Location location : locations) {
			locationsMap.put(location.getString("name"), location.getId());
		}

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;

			// read header, check if enough input columns
			line = br.readLine();
			String[] header = line.split(DELIMITER_COLUMNS);
			// check if enough input columns
			int inputColumns = header.length;
			if (inputColumns<requiredColumns) {
				throw new IllegalArgumentException("Not enough input columns for file: " + filename);
			}
			// look for optional tags and get their names
			int tagColumns = inputColumns-requiredColumns;
			String[] tagNames;
			if (tagColumns>0) {
				tagNames = new String[tagColumns];
				for (int t=0; t<tagColumns; t++) {
					tagNames[t] = header[requiredColumns+t].substring(1, header[requiredColumns+t].length()-1);
				}
			}
			else {
				tagNames = null;
			}

			// read all further lines and create one entry for each line
			int countLine = 0;
			while ((line=br.readLine())!=null) {
				countLine++;
				String[] split = line.split(DELIMITER_COLUMNS);
				if (split.length!=inputColumns) {
					throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
				}
				LocationDistance locationDistance = new LocationDistance();
				locationDistance.set("distance_id", distanceId);
				Object locationId1;
				Object locationId2;
				if (locationsMap.containsKey(split[0])) {
					locationId1 = locationsMap.get(split[0]);
				}
				else {
					throw new IllegalArgumentException("Name of location is not contained in table Locations: " + split[0]);

				}
				if (locationsMap.containsKey(split[1])) {
					locationId2 = locationsMap.get(split[1]);
				}
				else {
					throw new IllegalArgumentException("Name of location is not contained in table Locations: " + split[1]);

				}
				locationDistance.set("location_id1", locationId1);
				locationDistance.set("location_id2", locationId2);
				locationDistance.set("distance", split[2]);
				locationDistance.saveIt();

				// add optional tags
				for (int t=0; t<tagColumns; t++) {
					if (split[requiredColumns+t].length()>0) {
						Tag tag = new Tag();
						tag.set("name", tagNames[t]);
						tag.set("value", split[requiredColumns+t]);
						locationDistance.add(tag);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("IOException in readDistances.");
			e.printStackTrace();
		}
	}


	/**
	 * Reads .txt file and saves information in Table <code>categories</code>.
	 * @param filename the path of the .txt file
	 */
	public static void readCategories(String filename) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;

			// read first line with header
			line = br.readLine();

			// read second line with the category that has no parent (=top node)
			line = br.readLine();
			String[] parentSplit = line.split(":");
			if (parentSplit.length!=2) {
				throw new IllegalArgumentException("Wrong format in file " + filename);
			}
			// create entry for the top node
			int lft = 1;
			Category topNode = new Category();
			topNode.set("name", parentSplit[0]);
			topNode.set("lft", lft);
			topNode.set("rgt", lft);
			topNode.saveIt();
			lft++;
			// create entries for the children of the top node
			String[] childrenTopNode = parentSplit[1].split(DELIMITER_COLUMNS);
			for (int c=0; c<childrenTopNode.length; c++) {
				Category category = new Category();
				category.set("name", childrenTopNode[c]);
				category.set("parent_id", topNode.getId());
				category.set("lft", lft);
				category.set("rgt", lft);
				category.saveIt();
				lft++;
			}

			// read all further categories, their parent category has to be already read
			while ((line=br.readLine())!=null) {
				String[] split = line.split(":");
				if (split.length!=2) {
					throw new IllegalArgumentException("Wrong format in file " + filename);
				}
				// get id of parent (=entry in front of ":")
				LazyList<Category> parents = Category.find("name = ?", split[0]);
				Object parentId = null;
				if (parents.size()==1) {
					parentId = parents.get(0).getId();
				}
				else {
					if (parents.size()==0) {
						throw new IllegalArgumentException("Name of category is not contained in table Categories: " + split[0]);
					}
					if (parents.size()>0) {
						throw new IllegalArgumentException("Category name is not unique: " + split[0]);
					}
				}
				String[] children = split[1].split(DELIMITER_COLUMNS);
				for (int c=0; c<children.length; c++) {
					Category category = new Category();
					category.set("name", children[c]);
					category.set("parent_id", parentId);
					category.set("lft", lft);
					category.set("rgt", lft);
					category.saveIt();
					lft++;
				}
			}
			
			// rebuild the lft and rgt attributes in the table Categories
			Category.rebuildLftRgtAttributes();
		}
		catch (IOException e) {
			System.err.println("IOException in readCategories.");
			e.printStackTrace();
		}

	}



	/**
	 * Reads .txt file and saves information in Table <code>categories_maps</code>.
	 * @param filename the path of the .txt file
	 */
	public static void readCategoriesMaps(String filename) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;

			// read header
			line = br.readLine();

			// read all further lines
			// each line contains the name of a category and a list of the maps that belong to this category
			while ((line=br.readLine())!=null) {
				String[] split = line.split(":");
				if (split.length!=2) {
					throw new IllegalArgumentException("Wrong format in file " + filename);
				}

				// get id of the category in the current line
				LazyList<Category> categories = Category.find("name = ?", split[0]);
				Object categoryId = null;
				if (categories.size()==1) {
					categoryId = categories.get(0).getId();
				}
				else {
					if (categories.size()==0) {
						throw new IllegalArgumentException("Name of category is not contained in table Categories: " + split[0]);
					}
					if (categories.size()>0) {
						throw new IllegalArgumentException("Category name is not unique: " + split[0]);
					}
				}

				// create entries in categories_maps
				String[] mapNames = split[1].split(DELIMITER_COLUMNS);
				for (int m=0; m<mapNames.length; m++) {
					// get id of map
					LazyList<Map> maps = Map.find("name = ?", mapNames[m]);
					Object mapId = null;
					if (maps.size()==1) {
						mapId = maps.get(0).getId();
					}
					else {
						if (maps.size()==0) {
							throw new IllegalArgumentException("Name of map is not contained in table Maps: " + mapNames[m]);
						}
						if (maps.size()>0) {
							throw new IllegalArgumentException("Map name is not unique: " + mapNames[m]);
						}
					}


					CategoriesMaps categoriesMaps = new CategoriesMaps();
					categoriesMaps.set("category_id", categoryId);
					categoriesMaps.set("map_id", mapId);
					categoriesMaps.saveIt();
				}
			}
		}
		catch (IOException e) {
			System.err.println("IOException in readCategoriesMaps.");
			e.printStackTrace();
		}

	}


	/**
	 * Reads .txt file and saves information in Table <code>groups_maps</code> and <code>groups</code>.
	 * @param filename the path of the .txt file
	 */
	public static void readGroupsMaps(String filename) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
			String line;

			// read header
			line = br.readLine();

			// read all further lines
			// each line contains the name of a group and a list of the maps that belong to this group
			while ((line=br.readLine())!=null) {
				String[] split = line.split(":");
				if (split.length!=2) {
					throw new IllegalArgumentException("Wrong format in file " + filename);
				}

				// create entry in groups
				Group group = new Group();
				group.set("name", split[0]);
				group.saveIt();

				// create entries in groups_maps
				String[] mapNames = split[1].split(DELIMITER_COLUMNS);
				for (int m=0; m<mapNames.length; m++) {
					// get id of map
					LazyList<Map> maps = Map.find("name = ?", mapNames[m]);
					Object mapId = null;
					if (maps.size()==1) {
						mapId = maps.get(0).getId();
					}
					else {
						if (maps.size()==0) {
							throw new IllegalArgumentException("Name of map is not contained in table Maps: " + mapNames[m]);
						}
						if (maps.size()>0) {
							throw new IllegalArgumentException("Map name is not unique: " + mapNames[m]);
						}
					}


					GroupsMaps groupsMaps = new GroupsMaps();
					groupsMaps.set("group_id", group.getId());
					groupsMaps.set("map_id", mapId);
					groupsMaps.saveIt();
				}
			}
		}
		catch (IOException e) {
			System.err.println("IOException in readGroupsMaps.");
			e.printStackTrace();
		}

	}


	/**
	 * Writes one .csv file for each map. The name of the file is given by the id of the map.
	 * Each line contains the name of one variant with the count that it was answered.
	 * @param outputFolder the folder to which the .csv files shall be written to
	 */
	public static void prepareVariantsMappings(String outputFolder) {
		try {
			Directory.mkdir(outputFolder);

			LazyList<Map> maps = Map.findAll();
			for (Map map : maps) {
				try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFolder+"map_"+map.getId()+".csv"), USED_CHARSET))) {
					bw.write("variant_name;count");
					bw.newLine();
					// get all variants of the current map
					LazyList<Variant> variants = Variant.find("map_id = ?", map.getId());
					for (Variant variant : variants) {
						int count = variant.getAll(InterviewAnswer.class).size();
						bw.write(variant.getString("name") + ";" + count);
						bw.newLine();
					}
				}
			}
		}
		catch (IOException e) {
			System.err.println("IOException in prepareVariantsMappings.");
			e.printStackTrace();
		}
	}


	/**
	 * Read one file for each map. The name of the file is given by the id of the map.
	 * Saves information in table <code>levels</code> and <code>variants_mappings</code>.
	 * @param inputFolder the folder which contains the .csv file of variants and their level mapping
	 */
	public static void readVariantsMappings(String inputFolder) {


		try {

			LazyList<Map> maps = Map.findAll();
			for (Map map : maps) {
				Object mapId = map.getId();

				// put all given answers (=variants) from the database into a cache to avoid too many database queries
				LazyList<Variant> variantsFromDatabase = Variant.find("map_id = ?", mapId);
				// a HashMap saves Key-Value pairs for unique keys
				// here: Key: the name of the variant, Value: the id of the variant
				HashMap<String, Object> variants = new HashMap<String, Object>();
				for (Variant variant : variantsFromDatabase) {
					variants.put(variant.getString("name"), variant.getId());
				}


				String filename = inputFolder +"map_"+mapId+".csv";
				try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), USED_CHARSET))) {
					String line;
	
					// read header, check if enough input columns
					line = br.readLine();
					String[] header = line.split(DELIMITER_COLUMNS);
					int inputColumns = header.length;
					if (inputColumns<3) {
						throw new IllegalArgumentException("Not enough input columns for file: " + filename);
					}
	
					int nrLevels = inputColumns-2;
					Object[] levelIds = new Object[nrLevels];
					// add levels of header if they are not already in table level
					for (int l=0; l<nrLevels; l++) {
						if (Level.findFirst("name = ?", header[2+l])==null) {
							Level level = new Level();
							level.set("name", header[2+l]);
							level.saveIt();
							levelIds[l] = level.getId();
						}
						else {
							levelIds[l] = Level.findFirst("name = ?", header[2+l]).getId();
						}
					}
	
					// read all further lines and create entries in table variants_mappings for each line
					int countLine = 0;
					while ((line=br.readLine())!=null) {
						countLine++;
						String[] split = line.split(DELIMITER_COLUMNS);
						if (split.length!=inputColumns) {
							throw new IllegalArgumentException("Wrong number of columns in line " + (countLine+1) + " for file: " + filename);
						}
	
						Object variantId;
						if (variants.containsKey(split[0])) {
							variantId = variants.get(split[0]);
						}
						else {
							throw new IllegalArgumentException("Mapping for unknown variant " + split[0] + " in map with id " + mapId + ".");
						}
						for (int l=0; l<nrLevels; l++) {
							String toVariantName = split[2+l];
							Object toVariantId;
							if (variants.containsKey(toVariantName)) {
								toVariantId = variants.get(toVariantName);
							}
							else {
								// create new variant
								Variant variant = new Variant();
								variant.set("name", toVariantName);
								variant.set("map_id", mapId);
								variant.saveIt();
								toVariantId = variant.getId();
								// put it to the hash map of all variants of the current map
								variants.put(toVariantName, toVariantId);
							}
	
							// create entry in table variants_mappings
							VariantsMapping variantsMapping = new VariantsMapping();
							variantsMapping.set("variant_id", variantId);
							variantsMapping.set("level_id", levelIds[l]);
							variantsMapping.set("to_variant_id", toVariantId);
							variantsMapping.saveIt();
						}
	
					}

				}
			}
		} catch (IOException e) {
			System.err.println("IOException in readVariantsMappings.");
			e.printStackTrace();
		}
	}

	
}
