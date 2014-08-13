package geoling.maps.clustering.data;

import geoling.maps.AreaClassMap;
import geoling.maps.AreaClassMap.VariantDensityResult;
import geoling.maps.util.RectangularGrid;
import geoling.util.clusteranalysis.ClusterObject;

/**
 * A wrapper class for maps to implement the <code>ClusterObject</code> interface.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class MapClusterObject implements ClusterObject {
	
	private final AreaClassMap areaClassMap;
	private double[] covarianceFunction;
	
	public MapClusterObject(AreaClassMap areaClassMap) {
		this.areaClassMap = areaClassMap;
		this.covarianceFunction = null;
	}
	
	public void computeCovarianceFunction(RectangularGrid grid, double[][] gridDistances, int maxDistance) {
		areaClassMap.buildGridDensityCache(grid);
		
		double meanPrevalence = areaClassMap.computeMeanPrevalence(true);
		
		double[] covarianceFunction = new double[maxDistance + 1];
		int[] numberOfLocations = new int[maxDistance + 1];
		
		for (int j = 0; j < grid.getGridPoints().size(); j++) {
			for (int k = 0; k < j; k++) {
				RectangularGrid.GridPoint currentLocation1 = grid.getGridPoints().get(j);
				RectangularGrid.GridPoint currentLocation2 = grid.getGridPoints().get(k);
				
				double distance = gridDistances[j][k];
				int h = (int)Math.round(distance);
				if (h > maxDistance) {
					continue;
				}
				
				VariantDensityResult dominantVariantAndDensity1 = areaClassMap.getDominantVariantAndDensity(currentLocation1);
				VariantDensityResult dominantVariantAndDensity2 = areaClassMap.getDominantVariantAndDensity(currentLocation2);
				
				if (dominantVariantAndDensity1 != null && dominantVariantAndDensity2 != null) {
					covarianceFunction[h] = covarianceFunction[h] + (dominantVariantAndDensity1.density - meanPrevalence) * (dominantVariantAndDensity2.density - meanPrevalence);
					numberOfLocations[h]++;
				}
			}
		}
		
		for (int k = 0; k < covarianceFunction.length; k++) {
			if (numberOfLocations[k] > 0) {
				covarianceFunction[k] = covarianceFunction[k] / numberOfLocations[k];
			}
		}
		
		areaClassMap.clearGridDensityCache();
		
		this.covarianceFunction = covarianceFunction;
	}
	
	public AreaClassMap getAreaClassMap() {
		return areaClassMap;
	}
	
	public double[] getCovarianceFunction() {
		return covarianceFunction;
	}
	
	public double[] getCoordinates() {
		return covarianceFunction;
	}
	
	public boolean hasCoordinates() {
		return (covarianceFunction != null);
	}
	
	public double getWeight() {
		return 1.0;
	}
	
}
