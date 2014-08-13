package geoling.maps.plot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.AreaClassMap;
import geoling.maps.util.RectangularGrid;
import geoling.models.Variant;
import geoling.util.geom.Geometry2D;
import geoling.util.sim.grain.LineSegment;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.grain.Rectangle;
import geoling.util.sim.util.RandomSet;
import geoling.util.sim.util.plot.DrawableObject2D;
import geoling.util.sim.util.plot.DrawableRandomSetElement2D;
import geoling.util.sim.util.plot.DrawableText;
import geoling.util.sim.util.plot.PlotObjects2D;

/**
 * Class for drawing an area-class-map.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class PlotAreaClassMap extends PlotVoronoiMap {
	
	/**
	 * Golden ratio used to increment the hue (mod 1) when generating colors.
	 * 
	 * @see <a href="http://en.wikipedia.org/wiki/Golden_ratio">Golden ratio</a>
	 */
	public static final double GOLDEN_RATIO = 0.618033988749895;
	
	/** The default value for the saturation of generated colors. */
	public static final float DEFAULT_SATURATION = 0.75f;
	
	/** The default value for the brightness of generated colors. */
	public static final float DEFAULT_BRIGHTNESS = 1.0f;
	
	/** The area-class-map. */
	private AreaClassMap areaClassMap;
	
	/** The minimum dominance value at locations on this map, <code>NaN</code> as long as not yet evaluated. */
	private double dominanceMinCache = Double.NaN;
	
	/** The maximum dominance value at locations on this map, <code>NaN</code> as long as not yet evaluated. */
	private double dominanceMaxCache = Double.NaN;
	
	/**
	 * Constructs the plot object for the given area-class-map.
	 * 
	 * @param areaClassMap  the area-class-map, with initialized areas and
	 *                      thus indirectly assigned border polygon
	 */
	public PlotAreaClassMap(AreaClassMap areaClassMap) {
		super(areaClassMap.getVoronoiMap());
		this.areaClassMap  = areaClassMap;
		
		if (!this.areaClassMap.hasAreas()) {
			throw new IllegalArgumentException("Areas not initialized, remember to use buildAreas!");
		}
	}
	
	/**
	 * Returns the area-class-map.
	 * 
	 * @return the area-class-map
	 */
	public AreaClassMap getAreaClassMap() {
		return this.areaClassMap;
	}
	
	/**
	 * Draws the Voronoi area-class-map using a <code>PlotObjects2D</code> object.
	 * 
	 * @param export      the <code>PlotObjects2D</code> object
	 * @param helper      helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param areaColors  the base colors for the Voronoi cells, depending on the dominant variant,
	 *                    may be <code>null</code>
	 * @param hints       a map object to be filled with the polygons and the corresponding locations, useful
	 *                    to show hints when hovering the mouse cursor, may be <code>null</code>
	 */
	public void voronoiExport(PlotObjects2D export, PlotHelper helper, HashMap<Variant,Color> areaColors, HashMap<Polytope,AggregatedLocation> hints) {
		if (areaColors == null) {
			areaColors = getDefaultAreaColors(false);
		}
		
		voronoiCellsExport(export, helper, areaColors);
		voronoiCellsHints(export, helper, hints);
		voronoiCellBordersExport(export, helper);
		areaBordersExport(export, helper);
		mapBorderExport(export, helper);
		locationPointsExport(export, helper);
		locationLabelsExport(export, helper);
		legendExport(export, helper, areaColors);
	}
	
	/**
	 * Draws the continuous area-class-map using a <code>PlotObjects2D</code> object.
	 * 
	 * @param export     the <code>PlotObjects2D</code> object
	 * @param helper     helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param grid       the rectangular grid defining the points for estimation, may be <code>null</code>
	 * @param areaColors the base colors for the Voronoi cells, depending on the dominant variant,
	 *                   may be <code>null</code>
	 * @param hints      a map object to be filled with the polygons and the corresponding locations, useful
	 *                   to show hints when hovering the mouse cursor, may be <code>null</code>
	 */
	public void gridExport(PlotObjects2D export, PlotHelper helper, RectangularGrid grid, HashMap<Variant,Color> areaColors, HashMap<Polytope,AggregatedLocation> hints) {
		if (areaColors == null) {
			areaColors = getDefaultAreaColors(false);
		}
		
		gridRectanglesExport(export, helper, grid, areaColors);
		voronoiExport(export, helper, hints);
		legendExport(export, helper, areaColors);
	}
	
	/**
	 * Draws the colored Voronoi cells.
	 * 
	 * @param export  the <code>PlotObjects2D</code> object
	 * @param helper  helper object which provides shift, scaling factor, clipping window and drawing parameters
	 */
	public void areaBordersExport(PlotObjects2D export, PlotHelper helper) {
		if (helper.getAreaLineWidth() < 0.0) {
			return;
		}
		
		// get shift, scaling and clipping window
		double[] shift = helper.getShift();
		double scale   = helper.getScale();
		
		// line segments to plot
		LinkedList<DrawableObject2D> areaBorders = new LinkedList<DrawableObject2D>();
		
		// construct the area border line segments
		for (Iterator<?> it = this.areaClassMap.getBorderBetweenAllAreas().iterator(); it.hasNext(); ) {
			LineSegment ls = this.getVoronoiMap().getMapProjection().projectLatLong(((Geometry2D.LineSegment)it.next()).toLineSegment());
			ls.translateBy(shift);
			ls.stretch(scale);
			areaBorders.add(new DrawableRandomSetElement2D(ls, helper.getAreaLineWidth(), helper.getAreaBorderColor()));
		}
		
		// and, finally, draw the objects
		export.plot(areaBorders);
	}
	
	/**
	 * Draws the colored Voronoi cells.
	 * 
	 * @param export      the <code>PlotObjects2D</code> object
	 * @param helper      helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param areaColors  the base colors for the Voronoi cells, depending on the dominant variant,
	 *                    may be <code>null</code>
	 */
	public void voronoiCellsExport(PlotObjects2D export, PlotHelper helper, HashMap<Variant,Color> areaColors) {
		if (areaColors == null) {
			throw new IllegalArgumentException("Base colors for variants required!");
		}
		
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
			
			// fetch dominant variant for Voronoi cell
			AreaClassMap.VariantDensityResult result = this.areaClassMap.getDominantVariantAndDensity(this.getVoronoiMap().getLocationCells().get(j).getLocation());
			
			// get dominance as percentage
			double relDominance = this.getRelativeDominance(this.getVoronoiMap().getLocationCells().get(j).getLocation());
			
			// color for Voronoi cell
			Color color;
			if (result == null) {
				color = Color.WHITE;
			} else {
				color = this.getBackgroundColor(areaColors.get(result.variant), relDominance);
			}
			
			cells.add(new DrawableRandomSetElement2D(new Polytope(polytope.getVertices(), true), 0.0, color));
		}
		
		// and, finally, draw the objects
		export.plot(cells);
	}
	
	/**
	 * Draws the rectangles of the grid.
	 * 
	 * @param export     the <code>PlotObjects2D</code> object
	 * @param helper     helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param grid       the rectangular grid defining the points for estimation, may be <code>null</code>
	 * @param areaColors the base colors for the Voronoi cells, depending on the dominant variant,
	 *                   may be <code>null</code>
	 */
	public void gridRectanglesExport(PlotObjects2D export, PlotHelper helper, RectangularGrid grid, HashMap<Variant,Color> areaColors) {
		if (grid == null) {
			grid = this.areaClassMap.getGrid(true);
		}
		if (areaColors == null) {
			throw new IllegalArgumentException("Base colors for variants required!");
		}
		
		// build cache
		this.areaClassMap.buildGridDensityCache(grid);
		
		// get shift, scaling and clipping window
		double[] shift   = helper.getShift();
		double scale     = helper.getScale();
		
		// grid elements to plot
		LinkedList<DrawableObject2D> gridRectanglesInbetween = new LinkedList<DrawableObject2D>();
		LinkedList<DrawableObject2D> gridRectangles = new LinkedList<DrawableObject2D>();

		// construct the objects for the grid points
		for (RectangularGrid.GridPoint gridPoint : grid.getGridPoints()) {
			RandomSet polytopesLatLong = gridPoint.getLatLongRectIntersected();
			
			// fetch dominant variant for grid point
			AreaClassMap.VariantDensityResult result = this.areaClassMap.getDominantVariantAndDensity(gridPoint);
			
			// get dominance as percentage
			double relDominance = this.getRelativeDominance(gridPoint);
			
			// color for grid element
			Color color;
			if (result == null) {
				color = Color.WHITE;
			} else {
				color = this.getBackgroundColor(areaColors.get(result.variant), relDominance);
			}
			
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
	 * @param export      the <code>PlotObjects2D</code> object
	 * @param helper      helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param areaColors  the base colors for the Voronoi cells, depending on the dominant variant,
	 *                    may be <code>null</code>
	 */
	public void legendExport(PlotObjects2D export, PlotHelper helper, HashMap<Variant,Color> areaColors) {
		// default values for parameters
		if (areaColors == null) {
			throw new IllegalArgumentException("Base colors for variants required!");
		}
		
		// get shift, scaling and clipping window
		boolean legend = !Double.isNaN(helper.getLegendLeft()) && (helper.getLegendFontSize() > 0);
		
		if (!legend) {
			return;
		}
		
		// objects to plot
		LinkedList<DrawableObject2D> legendObjects = new LinkedList<DrawableObject2D>();
		
		// construct the objects for the legend, if necessary
		ArrayList<Variant> sortedVariants = new ArrayList<Variant>(areaColors.keySet());
		Collections.sort(sortedVariants);
		for (int j = 0; j < sortedVariants.size(); j++) {
			double left = helper.getLegendLeft() + helper.getLegendFontSize();
			double top  = helper.getHeight() - (helper.getHeight() - sortedVariants.size()*helper.getLegendFontSize()*1.5)/2.0 - helper.getLegendFontSize()*1.5*j;
			double size = helper.getLegendFontSize();
			Rectangle rect = new Rectangle(left, top, size, size, 0.0, true);
			DrawableText text = new DrawableText(new Point(new double[] { left+size, top-helper.getLegendFontSize()/3 }),
			                                     sortedVariants.get(j).getString("name"),
			                                     helper.getLegendFontSize(), Color.BLACK, DrawableText.Alignment.LEFT);
			Color baseColor = areaColors.get(sortedVariants.get(j));
			
			legendObjects.add(new DrawableRandomSetElement2D(rect, helper.getCellLineWidth(), (baseColor == null) ? Color.WHITE : baseColor));
			legendObjects.add(text);
		}
		
		// and, finally, draw the objects
		export.plot(legendObjects);
	}
	
	/**
	 * Returns the default colors used for the Voronoi cells of the
	 * different areas.
	 * 
	 * @param singleColor determines whether all dominant variants should have
	 *                    the same color (used for continuous map based on grid)
	 * @return the colors for all areas, adds also variants which aren't
	 *         dominant with the color white
	 */
	public HashMap<Variant,Color> getDefaultAreaColors(boolean singleColor) {
		ArrayList<Variant> variants = new ArrayList<Variant>(this.areaClassMap.getVariantWeights().getVariants());
		
		// sort variants w.r.t. their area size (number of locations), so we will get
		// a better optical distinction
		Collections.sort(variants, new Comparator<Variant>() {
			public int compare(Variant o1, Variant o2) {
				HashSet<AggregatedLocation> area1 = areaClassMap.getAreas().get(o1);
				HashSet<AggregatedLocation> area2 = areaClassMap.getAreas().get(o2);
				return Integer.compare((area2 != null) ? area2.size() : 0, (area1 != null) ? area1.size() : 0);
			}
		});
		
		// assign colors to dominant variants, but use white as color for
		// non-dominant ones
		HashMap<Variant,Color> variantColors = new HashMap<Variant,Color>();
		int i = 0;
		for (Variant variant : variants) {
			Color color;
			if (this.areaClassMap.getAreas().containsKey(variant)) {
				if (singleColor) {
					color = Color.getHSBColor(0.0f, DEFAULT_SATURATION, DEFAULT_BRIGHTNESS);
				} else {
					double h = GOLDEN_RATIO*i;
					color = Color.getHSBColor((float)h-(int)h, DEFAULT_SATURATION, DEFAULT_BRIGHTNESS);
					i++;
				}
			} else {
				color = Color.WHITE;
			}
			variantColors.put(variant, color);
		}
		return variantColors;
	}
	
	/**
	 * Returns the minimal dominance on the area-class-map.
	 * The result is cached for future accesses.
	 * 
	 * @return the minimal dominance value
	 */
	protected double getMinDominance() {
		if (Double.isNaN(dominanceMinCache)) {
			dominanceMinCache = Double.POSITIVE_INFINITY;
			for (int j = 0; j < this.getVoronoiMap().getLocationCells().size(); j++) {
				double dominance = this.areaClassMap.computeDominanceAtLocation(this.getVoronoiMap().getLocationCells().get(j).getLocation());
				if (dominance < dominanceMinCache) dominanceMinCache = dominance;
			}
		}
		return dominanceMinCache;
	}
	
	/**
	 * Returns the maximal dominance on the area-class-map.
	 * The result is cached for future accesses.
	 * 
	 * @return the maximal dominance value
	 */
	protected double getMaxDominance() {
		if (Double.isNaN(dominanceMaxCache)) {
			dominanceMaxCache = Double.NEGATIVE_INFINITY;
			for (int j = 0; j < this.getVoronoiMap().getLocationCells().size(); j++) {
				double dominance = this.areaClassMap.computeDominanceAtLocation(this.getVoronoiMap().getLocationCells().get(j).getLocation());
				if (dominance > dominanceMaxCache) dominanceMaxCache = dominance;
			}
		}
		return dominanceMaxCache;
	}
	
	/**
	 * Returns the relative dominance value of the dominant variant at the given location.
	 * 
	 * @param location  the location
	 * @return the relative dominance value, i.e., 0 corresponds to the
	 *         minimal value and 1 corresponds to the maximal value
	 *         on the area-class-map
	 */
	protected double getRelativeDominance(AggregatedLocation location) {
		double dominanceMin = this.getMinDominance();
		double dominanceMax = this.getMaxDominance();
		
		double dominance    = this.areaClassMap.computeDominanceAtLocation(location);
		double relDominance = (dominance - dominanceMin) / (dominanceMax - dominanceMin);
		
		if (Double.isNaN(relDominance)) relDominance = 1.0;
		
		return relDominance;
	}
	
	/**
	 * Returns the relative dominance value of the dominant variant at the given location.
	 * 
	 * @param gridPoint  the location on the grid
	 * @return the relative dominance value, i.e., 0 corresponds to the
	 *         minimal value and 1 corresponds to the maximal value
	 *         on the area-class-map
	 */
	protected double getRelativeDominance(RectangularGrid.GridPoint gridPoint) {
		double dominanceMin = this.getMinDominance();
		double dominanceMax = this.getMaxDominance();
		
		double dominance    = this.areaClassMap.computeDominanceAtGridPoint(gridPoint);
		double relDominance = (dominance - dominanceMin) / (dominanceMax - dominanceMin);

		if (relDominance < 0.0) relDominance = 0.0;
		if (relDominance > 1.0) relDominance = 1.0;
		if (Double.isNaN(relDominance)) relDominance = 1.0;
		
		return relDominance;
	}
	
	/**
	 * Generates the background color that should be used for cells or grid rectangles.
	 * 
	 * @param baseColor    the base color whose hue is used
	 * @param relDominance the relative dominance of the variant
	 * @return the color
	 */
	protected Color getBackgroundColor(Color baseColor, double relDominance) {
		if (baseColor == null) {
			throw new IllegalArgumentException("No base color given!");
		}
		
		float hue = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null)[0];
		float saturation = (float)(0.75 * relDominance + 0.25);
		float brightness = 1.0f - (float)(0.75 * relDominance);
		
		return Color.getHSBColor(hue, saturation, brightness);
	}
	
}