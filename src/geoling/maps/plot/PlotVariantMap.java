package geoling.maps.plot;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.VariantMap;
import geoling.maps.projection.MapProjection;
import geoling.maps.util.RectangularGrid;
import geoling.maps.util.VoronoiMapCache;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.grain.Rectangle;
import geoling.util.sim.util.RandomSet;
import geoling.util.sim.util.plot.DrawableObject2D;
import geoling.util.sim.util.plot.DrawableRandomSetElement2D;
import geoling.util.sim.util.plot.DrawableText;
import geoling.util.sim.util.plot.PlotObjects2D;

/**
 * Class for drawing a variant-occurrence map.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class PlotVariantMap extends PlotVoronoiMap {
	
	/** The variant-occurrence map. */
	private VariantMap variantMap;
	
	/** The minimum density value at locations on this variant map, <code>NaN</code> as long as not yet evaluated. */
	private double densityMinCache = Double.NaN;
	
	/** The maximum density value at locations on this variant map, <code>NaN</code> as long as not yet evaluated. */
	private double densityMaxCache = Double.NaN;
	
	/**
	 * Constructs the plot object for the given variant-occurrence map.
	 * 
	 * @param variantMap    the variant-occurrence map
	 * @param border        the border polygon
	 * @param mapProjection the projection method for the coordinates
	 */
	public PlotVariantMap(VariantMap variantMap, Polytope border, MapProjection mapProjection) {
		super(VoronoiMapCache.getVoronoiMap(variantMap.getLocations(), border, mapProjection));
		this.variantMap = variantMap;
	}
	
	/**
	 * Returns the variant-occurrence map.
	 * 
	 * @return the variant-occurrence map
	 */
	public VariantMap getVariantMap() {
		return this.variantMap;
	}
	
	/**
	 * Draws the Voronoi variant-occurrence map using a <code>PlotObjects2D</code> object.
	 * 
	 * @param export        the <code>PlotObjects2D</code> object
	 * @param helper        helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param variantColor  the base color for the Voronoi cells, may be <code>null</code>
	 * @param hints         a map object to be filled with the polygons and the corresponding locations, useful
	 *                      to show hints when hovering the mouse cursor, may be <code>null</code>
	 */
	public void voronoiExport(PlotObjects2D export, PlotHelper helper, Color variantColor, HashMap<Polytope,AggregatedLocation> hints) {
		if (variantColor == null) {
			variantColor = getDefaultVariantColor();
		}
		
		voronoiCellsExport(export, helper, variantColor);
		voronoiExport(export, helper, hints);
		legendExport(export, helper, variantColor);
	}
	
	/**
	 * Draws the continuous variant-occurrence map using a <code>PlotObjects2D</code> object.
	 * 
	 * @param export        the <code>PlotObjects2D</code> object
	 * @param helper        helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param grid          the rectangular grid defining the points for estimation, may be <code>null</code>
	 * @param variantColor  the base color, may be <code>null</code>
	 * @param hints         a map object to be filled with the polygons and the corresponding locations, useful
	 *                      to show hints when hovering the mouse cursor, may be <code>null</code>
	 */
	public void gridExport(PlotObjects2D export, PlotHelper helper, RectangularGrid grid, Color variantColor, HashMap<Polytope,AggregatedLocation> hints) {
		if (variantColor == null) {
			variantColor = getDefaultVariantColor();
		}
		
		gridRectanglesExport(export, helper, grid, variantColor);
		voronoiExport(export, helper, hints);
		legendExport(export, helper, variantColor);
	}
	
	/**
	 * Draws the colored Voronoi cells.
	 * 
	 * @param export        the <code>PlotObjects2D</code> object
	 * @param helper        helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param variantColor  the base color for the Voronoi cells
	 */
	public void voronoiCellsExport(PlotObjects2D export, PlotHelper helper, Color variantColor) {
		if (variantColor == null) {
			throw new IllegalArgumentException("Base color for Voronoi cells required!");
		}
		
		// build cache
		this.variantMap.getAreaClassMap().buildLocationDensityCache();
		
		// get shift, scaling and clipping window
		double[] shift = helper.getShift();
		double scale   = helper.getScale();
		
		// cells to plot
		LinkedList<DrawableObject2D> cells = new LinkedList<DrawableObject2D>();
		
		// construct the objects for the locations/cells
		for (int j = 0; j < this.getVoronoiMap().getLocationCells().size(); j++) {
			// Voronoi cell
			Polytope polytope = this.getVoronoiMap().getMapProjection().projectLatLong(this.getVoronoiMap().getLocationCells().get(j).getVoronoiCell());
			polytope.translateBy(shift);
			polytope.stretch(scale);
			
			// get density (relative to min-/max-values)
			double relDensity = this.getRelativeDensity(this.getVoronoiMap().getLocationCells().get(j).getLocation());
			
			// color for Voronoi cell
			Color color = this.getBackgroundColor(variantColor, relDensity);
			
			cells.add(new DrawableRandomSetElement2D(new Polytope(polytope.getVertices(), true), 0.0, color));
		}
		
		// and, finally, draw the objects
		export.plot(cells);
	}
	
	/**
	 * Draws the rectangles of the grid.
	 * 
	 * @param export        the <code>PlotObjects2D</code> object
	 * @param helper        helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param grid          the rectangular grid defining the points for estimation, may be <code>null</code>
	 * @param variantColor  the base color, may be <code>null</code>
	 */
	public void gridRectanglesExport(PlotObjects2D export, PlotHelper helper, RectangularGrid grid, Color variantColor) {
		if (grid == null) {
			grid = this.variantMap.getAreaClassMap().getGrid(true);
		}
		if (variantColor == null) {
			throw new IllegalArgumentException("Base color for Voronoi cells required!");
		}
		
		// build cache
		this.variantMap.getAreaClassMap().buildLocationDensityCache();
		this.variantMap.buildGridDensityCache(grid, false);
		
		// get shift, scaling and clipping window
		double[] shift   = helper.getShift();
		double scale     = helper.getScale();

		// grid elements to plot
		LinkedList<DrawableObject2D> gridRectanglesInbetween = new LinkedList<DrawableObject2D>();
		LinkedList<DrawableObject2D> gridRectangles = new LinkedList<DrawableObject2D>();
		
		// construct the objects for the grid points
		for (int j = 0; j < grid.getGridPoints().size(); j++) {
			RandomSet polytopesLatLong = grid.getGridPoints().get(j).getLatLongRectIntersected();
			
			// get density (relative to min-/max-values)
			double relDensity = this.getRelativeDensity(grid.getGridPoints().get(j));
			
			// color for grid element
			Color color = this.getBackgroundColor(variantColor, relDensity);
			
			// add polytope(s) for grid rectangles
			for (Iterator<?> it = polytopesLatLong.iterator(); it.hasNext(); ) {
				Polytope p = this.getVoronoiMap().getMapProjection().projectLatLong((Polytope)it.next());
				p.translateBy(shift);
				p.stretch(scale);
				
				gridRectanglesInbetween.add(new DrawableRandomSetElement2D(new Polytope(p.getVertices(), false), 0.5, color));
				gridRectangles.add(new DrawableRandomSetElement2D(new Polytope(p.getVertices(), true), 0.5, color));
			}
		}
		
		// and, finally, draw the objects
		export.plot(gridRectanglesInbetween);
		export.plot(gridRectangles);
	}
	
	/**
	 * Draws the legend.
	 * 
	 * @param export        the <code>PlotObjects2D</code> object
	 * @param helper        helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param variantColor  the base color for the Voronoi cells, may be <code>null</code>
	 */
	public void legendExport(PlotObjects2D export, PlotHelper helper, Color variantColor) {
		if (variantColor == null) {
			throw new IllegalArgumentException("Base color for Voronoi cells required!");
		}
		
		// get shift, scaling and clipping window
		boolean legend = !Double.isNaN(helper.getLegendLeft()) && (helper.getLegendFontSize() > 0);
		if (!legend) {
			return;
		}
		
		// objects to plot
		LinkedList<DrawableObject2D> legendObjects = new LinkedList<DrawableObject2D>();
		
		// construct the objects for the legend, if necessary
		double left = helper.getLegendLeft() + helper.getLegendFontSize();
		double top  = helper.getHeight() - (helper.getHeight() - helper.getLegendFontSize()*1.5)/2.0;
		double size = helper.getLegendFontSize();
		Rectangle rect = new Rectangle(left, top, size, size, 0.0, true);
		DrawableText text = new DrawableText(new Point(new double[] { left+size, top-helper.getLegendFontSize()/3 }),
		                                     this.variantMap.getVariant().getString("name"),
		                                     helper.getLegendFontSize(), Color.BLACK, DrawableText.Alignment.LEFT);
		
		legendObjects.add(new DrawableRandomSetElement2D(rect, helper.getCellLineWidth(), variantColor));
		legendObjects.add(text);
		
		// and, finally, draw the objects
		export.plot(legendObjects);
	}
	
	/**
	 * Returns the default color used for the Voronoi cells.
	 * 
	 * @return the color
	 */
	public Color getDefaultVariantColor() {
		return Color.getHSBColor(0.6f, PlotAreaClassMap.DEFAULT_SATURATION, PlotAreaClassMap.DEFAULT_BRIGHTNESS);
	}
	
	/**
	 * Returns the minimal density value on the variant maps belonging
	 * to the same area-class-map.
	 * The result is cached for future accesses.
	 * 
	 * @return the minimal density value
	 */
	protected double getMinDensity() {
		if (Double.isNaN(densityMinCache)) {
			densityMinCache = this.variantMap.getAreaClassMap().getMinDensity();
		}
		return densityMinCache;
	}
	
	/**
	 * Returns the maximal density value on the variant maps belonging
	 * to the same area-class-map.
	 * The result is cached for future accesses.
	 * 
	 * @return the maximal density value
	 */
	protected double getMaxDensity() {
		if (Double.isNaN(densityMaxCache)) {
			densityMaxCache = this.variantMap.getAreaClassMap().getMaxDensity();
		}
		return densityMaxCache;
	}
	
	/**
	 * Returns the relative density value of the given location.
	 * 
	 * @param location  the location
	 * @return the relative density value, i.e., 0 corresponds to the
	 *         minimal value and 1 corresponds to the maximal value
	 *         on the variant maps belonging to the same area-class-map
	 */
	protected double getRelativeDensity(AggregatedLocation location) {
		double densityMin = this.getMinDensity();
		double densityMax = this.getMaxDensity();
		
		double density    = this.variantMap.getDensity(location);
		double relDensity = (density - densityMin) / (densityMax - densityMin);
		
		if (Double.isNaN(relDensity)) relDensity = 1.0;
		
		return relDensity;
	}
	
	/**
	 * Returns the relative density value of the given location.
	 * 
	 * @param gridPoint  the location on the grid
	 * @return the relative density value, i.e., 0 corresponds to the
	 *         minimal value and 1 corresponds to the maximal value
	 *         on the variant maps belonging to the same area-class-map
	 */
	protected double getRelativeDensity(RectangularGrid.GridPoint gridPoint) {
		double densityMin = this.getMinDensity();
		double densityMax = this.getMaxDensity();
		
		double density    = this.variantMap.getDensity(gridPoint);
		double relDensity = (density - densityMin) / (densityMax - densityMin);
		
		if (relDensity < 0.0) relDensity = 0.0;
		if (relDensity > 1.0) relDensity = 1.0;
		if (Double.isNaN(relDensity)) relDensity = 1.0;
		
		return relDensity;
	}
	
	/**
	 * Generates the background color that should be used for cells or grid rectangles.
	 * 
	 * @param baseColor   the base color whose hue is used
	 * @param relDensity  the relative density value
	 * @return the color
	 */
	protected Color getBackgroundColor(Color baseColor, double relDensity) {
		if (baseColor == null) {
			throw new IllegalArgumentException("No base color given!");
		}
		
		float hue        = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null)[0];
		float saturation = (float)relDensity;
		float brightness = 1.0f - (float)(0.75 * relDensity);
		
		return Color.getHSBColor(hue, saturation, brightness);
	}
	
}
