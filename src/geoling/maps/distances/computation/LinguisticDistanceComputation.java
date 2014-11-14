package geoling.maps.distances.computation;

import geoling.config.Database;
import geoling.config.Settings;
import geoling.maps.distances.LinguisticDistance;
import geoling.maps.weights.*;
import geoling.models.*;
import geoling.util.ProgressOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;

/**
 * This class provides a method to compute the linguistic distances for all
 * pairs of locations.
 * 
 * @author student assistant, Institute of Stochastics, Ulm University
 * @see "S. Pickl, A. Spettl, S. Pröll, S. Elspaß, W. König and V. Schmidt,
 *       Linguistic distances in dialectometric intensity estimation.
 *       Journal of Linguistic Geography 2 (2014), 25-40."
 * @see "Julius Vogelbacher, Statistische Analyse von Wortvarianten des Sprachatlas von
 *       Bayerisch-Schwaben, Universität Ulm, Diplomarbeit, Kapitel 2.5"
 */
public class LinguisticDistanceComputation {
	
	/** Helper object for computation of linguistic distance between two locations. */
	public static class Dling {
		public final Location location1, location2;
		private int n = 0;
		private double d = 0.0;
		
		private Dling(Location location1, Location location2) {
			this.location1 = location1;
			this.location2 = location2;
		}
		
		private void increment_n() {
			n++;
		}
		
		private void addLingDistValueTo_d(double value1, double value2) {
			d += 0.5*Math.abs(value1-value2);
		}
		
		public double getLinguisticDistance() {
			return d / ((double)n);
		}
	}
	
	/**
	 * Computes the linguistic distance (for all levels and all groups), and
	 * writes the distances to the database.
	 * 
	 * @param progress object for progress messages
	 */
	public static void computeDistances(ProgressOutput progress) {
		LazyList<Group> groups = Group.findAll();
		for (Group group : groups) {
			computeDistances(group, progress);
		}
	}
	
	/**
	 * Computes the linguistic distance (for all levels) using the maps in the given group,
	 * and writes the distances to the database.
	 * 
	 * @param group    the group with the maps to consider, may be <code>null</code>, then all maps are used
	 * @param progress object for progress messages
	 */
	public static void computeDistances(Group group, ProgressOutput progress) {
		LazyList<Level> levels = Level.findAll();
		for (Level level : levels) {
			progress.customMessage("Group and level: " + ((group == null) ? "(all maps)" : group.getString("name")) + " - " + level.getString("name"));
			computeDistances(group, level, progress);
			progress.customMessage("");
		}
	}
	
	/**
	 * Computes the linguistic distance using the maps in the given group and
	 * the given level, and writes the distances to the database.
	 * 
	 * @param group    the group with the maps to consider, may be <code>null</code>, then all maps are used
	 * @param level    the level used for variant aggregation, may be <code>null</code>
	 * @param progress object for progress messages
	 */
	public static void computeDistances(Group group, Level level, ProgressOutput progress) {
		LazyList<Map> maps;
		if (group == null) {
			maps = Map.findAll();
		} else {
			maps = group.getAll(Map.class);
		}
		
		ArrayList<VariantWeights> variantWeightsList = new ArrayList<VariantWeights>(maps.size());
		progress.customMessage("Loading maps with variant weights...");
		progress.reset(maps.size());
		progress.initCurrent();
		for (Map map : maps) {
			// compute weights for current map with the given level
			VariantWeights variantWeights;
			if (level == null) {
				variantWeights = new VariantWeightsNoLevel(map);
			} else {
				variantWeights = new VariantWeightsWithLevel(map, level);
			}
			variantWeightsList.add(variantWeights);
			progress.incrementCurrent();
		}
		
		// compute distances
		HashMap<String,Dling> dlings = computeDistances(variantWeightsList, progress);
		
		// save distances
		progress.customMessage("Writing distances to database...");
		progress.reset(dlings.size());
		progress.initCurrent();
		String identification = LinguisticDistance.getStaticIdentificationString(level, group);
		
		Distance distance = Distance.findFirst("identification = ?", identification);
		if (distance == null) {
			String name = "Linguistic distance";
			if (group == null) {
				name += " (all groups";
			} else {
				name += " ("+group.getString("name");
			}
			if (level != null) {
				name += ", ";
				if (level.getString("name").contains(":")) {
					name += level.getString("name").substring(0, level.getString("name").indexOf(":"));
				} else {
					name += level.getString("name");
				}
			}
			name += ")";
			
			distance = new Distance();
			distance.set("name", name);
			distance.set("type", "precomputed");
			distance.set("identification", identification);
			distance.saveIt();
		}
		
		// remove all existing distances for pairs of locations
		Base.exec("DELETE FROM location_distances WHERE distance_id = ?", distance.getId());
		
		// save distances for pairs of locations, build large SQL queries directly for faster storage
		StringBuffer sql = new StringBuffer();
		for (Entry<String,Dling> entry : dlings.entrySet()){
			Dling dling = entry.getValue();
			
			if (sql.length() == 0) {
				sql.append("INSERT INTO location_distances (distance_id, location_id1, location_id2, distance) VALUES");
			} else {
				sql.append(",");
			}
			sql.append(" ("+distance.getId()+", "+
			                dling.location1.getId()+", "+
			                dling.location2.getId()+", "+
			                dling.getLinguisticDistance()+")");
			
			progress.incrementCurrent();
			
			if (sql.length() > 65000) {
				Base.exec(sql.toString());
				sql = new StringBuffer();
			}
		}
		if (sql.length() > 0) {
			Base.exec(sql.toString());
		}
	}
	
	/**
	 * Computes the linguistic distance using the maps in the given group and
	 * the given level.
	 * 
	 * @param variantWeightsList the variant weights objects of the maps to consider
	 * @param progress           object for progress messages
	 * @return the distances as a map, where the key is a string that contains the two location
	 *         ids (smallest number first, separated by an underscore), and the value is a
	 *         <code>Dling</code> object
	 */
	public static HashMap<String,Dling> computeDistances(List<VariantWeights> variantWeightsList, ProgressOutput progress) {
		// hash map that holds for every pair of locations the Dling object,
		// the key is given by the two location ids (separated by underscore,
		// smaller id at the beginning)
		HashMap<String,Dling> dlings = new HashMap<String,Dling>();
		
		// calculate distance for every map
		progress.customMessage("Computing linguistic distances...");
		progress.reset(variantWeightsList.size());
		progress.initCurrent();
		for (VariantWeights variantWeights : variantWeightsList) {
			ArrayList<Location> locations = new ArrayList<Location>(variantWeights.getLocations());
			
			// iterate over all pairs of locations (where first id smaller than second,
			// i.e., every pair only exactly once)
			for (Location location1 : locations) {
				for (Location location2 : locations) {
					if (location1.getLongId().longValue() < location2.getLongId().longValue()) {
						HashSet<Variant> v1 = variantWeights.getVariantsAtLocation(location1);
						HashSet<Variant> v2 = variantWeights.getVariantsAtLocation(location2);
						if ((v1.isEmpty()) || (v2.isEmpty())) {
							// no variants at one of the two locations, then nothing to do
							// (no variants = no information at this location, so we just can't
							//  compute a distance for the current map)
							continue;
						}
						
						// fetch existing Dling object or create a new one
						String key = location1.getId()+"_"+location2.getId();
						Dling d = dlings.get(key);
						if (d == null) {
							d = new Dling(location1, location2);
							dlings.put(key, d);
						}
						
						HashSet<Variant> v1_v2 = new HashSet<Variant>();
						v1_v2.addAll(v1);
						v1_v2.addAll(v2);
						// now update the Dling object
						d.increment_n();
						for (Variant variant : v1_v2) {
							d.addLingDistValueTo_d(variantWeights.getWeight(variant, location1),
							                       variantWeights.getWeight(variant, location2));
						}
					}
				}
			}
			
			progress.incrementCurrent();
		}
		
		return dlings;
	}
	
	/**
	 * Main method, allows to execute this class to compute the distances for the
	 * groups found in the database (and all levels).
	 * 
	 * @param args  command line parameters (not used)
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Settings.load();
		Database.connect(Settings.getDatabaseIdentifier());
		
		computeDistances(new ProgressOutput(System.out));
	}
	
}