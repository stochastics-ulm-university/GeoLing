package geoling.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import geoling.config.Database;
import geoling.config.Settings;
import geoling.maps.*;
import geoling.maps.density.*;
import geoling.maps.density.bandwidth.*;
import geoling.maps.density.kernels.*;
import geoling.maps.distances.*;
import geoling.maps.plot.*;
import geoling.maps.projection.*;
import geoling.maps.util.*;
import geoling.maps.weights.*;
import geoling.models.*;
import geoling.util.Directory;
import geoling.util.ModelHelper;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.util.plot.PlotToEPS;
import geoling.util.sim.util.plot.PlotToGraphics2D;

/**
 * Simple example for usage of GeoLing classes: load weights of a map,
 * perform density estimation, generate maps, plot to EPS and PNG files.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class PlotExampleMap {
	
	/**
	 * Example: Plot a single map of the SBS database.
	 * 
	 * @param args  command line parameters (not required)
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		// directory for the images
		String outputDir = Directory.ensureTrailingSlash(System.getProperty("user.home")) + "Desktop" + File.separator;
		
		// load settings, choose SBS database and connect
		Settings.load();
		Settings.setDatabaseIdentifier("SBS");
		Database.connect(Settings.getDatabaseIdentifier());
		
		// load the map we want to plot and the variants aggregation level
		Map map = ModelHelper.findByTag(Map.class, "Kartennummer", "8080");
		Level level = ModelHelper.findByTag(Level.class, "Level", "3");
		
		System.out.println("Map: "+map.getString("name"));
		System.out.println("Level: "+level.getString("name"));
		
		// load the weights of all variants
		VariantWeights variantWeights = new VariantWeightsWithLevel(map, level);
		
		// choose the distance measure between locations, the bandwidth estimator and the kernel
		DistanceMeasure distance = new GeographicalDistance();
		String estimatorIdStr = LeastSquaresCrossValidation.getStaticIdentificationString();
		Kernel kernel = new GaussianKernel(distance, variantWeights, estimatorIdStr);
		//DistanceMeasure distance = new LinguisticDistance(level, (Group)Group.findFirst("name = ?", "Karten der Wortgeographie (mit Levels)"), true);
		//String estimatorIdStr = MinComplexityMaxFidelity.getStaticIdentificationString();
		//Kernel kernel = new K3Kernel(distance, variantWeights, estimatorIdStr);
		
		// construct object for density estimation
		DensityEstimation densityEstimation = new KernelDensityEstimation(kernel);
		
		// construct area-class-map
		AreaClassMap areaClassMap = new AreaClassMap(variantWeights, densityEstimation);
		
		// select border polygon, choose type of projection from geographical coordinates to the plane
		Polytope borderPolygon = Border.getDefaultBorder().toPolygon();
		MapProjection mapProjection = new MercatorProjection();
		
		// for continuous area-class-maps: build grid with small rectangles
		// (continuous maps/grids only supported for geographical distance)
		RectangularGrid grid = null;
		if (distance instanceof GeographicalDistance) {
			grid = RectangularGridCache.getGrid(borderPolygon, mapProjection);
		}
		
		// compute the areas of the area-class-map
		areaClassMap.buildAreas(borderPolygon, mapProjection);
		
		// output some characteristics
		System.out.println("total border length: "+areaClassMap.computeTotalBorderLength());
		System.out.println("overall area compactness: "+areaClassMap.computeOverallAreaCompactness());
		System.out.println("overall homogeneity: "+areaClassMap.computeOverallHomogeneity());
		
		// helper object for drawing images, allows us to specify the size, line widths etc.
		PlotHelper helper = new PlotHelper(borderPolygon, mapProjection);
		
		// plot area-class-map
		{
			PlotAreaClassMap plot = new PlotAreaClassMap(areaClassMap);
			
			// save as Voronoi map (EPS and PNG)
			{
				try (PlotToEPS eps = new PlotToEPS(helper.getWindow(), new FileOutputStream(outputDir+"sbs_area_class_map.eps"))) {
					plot.voronoiExport(eps, helper, null, null);
				}
				
				BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);
				try (PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
					plot.voronoiExport(gre, helper, null, null);
				}
				ImageIO.write(bi, "png", new File(outputDir+"sbs_area_class_map.png"));
			}
			
			// save as continous map (EPS and PNG)
			if (distance instanceof GeographicalDistance) {
				try (PlotToEPS eps = new PlotToEPS(helper.getWindow(), new FileOutputStream(outputDir+"sbs_area_class_map_grid.eps"))) {
					plot.gridExport(eps, helper, grid, null, null);
				}
				
				BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);
				try (PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics())) {
					plot.gridExport(gre, helper, grid, null, null);
				}
				ImageIO.write(bi, "png", new File(outputDir+"sbs_area_class_map_grid.png"));
			}
		}
		
		// save variant occurrence maps
		/*
		for (java.util.Map.Entry<Variant,VariantMap> entry : areaClassMap.getVariantMaps().entrySet()) {
			Variant variant = entry.getKey();
			VariantMap variantMap = entry.getValue();
			
			// get variant name suitable for file names (the part before ":")
			String variantName = variant.getString("name");
			if (variantName.contains(":")) {
				variantName = variantName.substring(0, variantName.indexOf(":"));
			}
			
			PlotVariantMap plot = new PlotVariantMap(variantMap, borderPolygon, mapProjection);
			try (PlotToEPS eps = new PlotToEPS(helper.getWindow(), new FileOutputStream(outputDir+variantName+".eps"))) {
				plot.voronoiExport(eps, helper, null, null);
			}
			if (distance instanceof GeographicalDistance) {
				try (PlotToEPS eps = new PlotToEPS(helper.getWindow(), new FileOutputStream(outputDir+variantName+"_grid.eps"))) {
					plot.gridExport(eps, helper, grid, null, null);
				}
			}
		}
		//*/
	}
	
}