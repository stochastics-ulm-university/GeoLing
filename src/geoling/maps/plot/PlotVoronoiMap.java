package geoling.maps.plot;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.util.VoronoiMap;
import geoling.util.geom.Geometry2D;
import geoling.util.sim.grain.LineSegment;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.grain.Sphere;
import geoling.util.sim.util.plot.DrawableObject2D;
import geoling.util.sim.util.plot.DrawableRandomSetElement2D;
import geoling.util.sim.util.plot.DrawableText;
import geoling.util.sim.util.plot.PlotObjects2D;

/**
 * Class for drawing a Voronoi map.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class PlotVoronoiMap {
	
	/** The Voronoi map. */
	private VoronoiMap voronoiMap;
	
	/**
	 * Constructs the plot object for the given Voronoi map.
	 * 
	 * @param voronoiMap  the Voronoi map
	 */
	public PlotVoronoiMap(VoronoiMap voronoiMap) {
		this.voronoiMap = voronoiMap;
	}
	
	/**
	 * Returns the variant-occurrence map.
	 * 
	 * @return the variant-occurrence map
	 */
	public VoronoiMap getVoronoiMap() {
		return this.voronoiMap;
	}
	
	/**
	 * Draws the Voronoi variant-occurrence map using a <code>PlotObjects2D</code> object.
	 * 
	 * @param export        the <code>PlotObjects2D</code> object
	 * @param helper        helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param hints         a map object to be filled with the polygons and the corresponding locations, useful
	 *                      to show hints when hovering the mouse cursor, may be <code>null</code>
	 */
	public void voronoiExport(PlotObjects2D export, PlotHelper helper, HashMap<Polytope,AggregatedLocation> hints) {
		voronoiCellsHints(export, helper, hints);
		voronoiCellBordersExport(export, helper);
		mapBorderExport(export, helper);
		locationPointsExport(export, helper);
		locationLabelsExport(export, helper);
	}
	
	/**
	 * Draws the map border.
	 * 
	 * @param export  the <code>PlotObjects2D</code> object
	 * @param helper  helper object which provides shift, scaling factor, clipping window and drawing parameters
	 */
	public void mapBorderExport(PlotObjects2D export, PlotHelper helper) {
		if (helper.getBorderLineWidth() <= 0.0) {
			return;
		}
		
		// get shift, scaling and clipping window
		double[] shift = helper.getShift();
		double scale   = helper.getScale();
		
		// map border line segments to plot
		LinkedList<DrawableObject2D> mapBorderSegments = new LinkedList<DrawableObject2D>();
		
		// construct the map border line segments, if necessary
		if (helper.getBorderLineWidth() > 0.0) {
			Polytope border = this.getVoronoiMap().getMapProjection().projectLatLong(voronoiMap.getBorder());
			for (Iterator<?> it = border.getEdges().iterator(); it.hasNext(); ) {
				LineSegment ls = ((LineSegment)it.next());
				ls.translateBy(shift);
				ls.stretch(scale);
				mapBorderSegments.add(new DrawableRandomSetElement2D(ls, helper.getBorderLineWidth(), helper.getBorderColor()));
			}
		}
		
		export.plot(mapBorderSegments);
	}
	
	/**
	 * Generates the mapping from location cell polytopes to hints.
	 * 
	 * @param export  the <code>PlotObjects2D</code> object
	 * @param helper  helper object which provides shift, scaling factor, clipping window and drawing parameters
	 * @param hints   a map object to be filled with the polygons and the corresponding locations, useful
	 *                to show hints when hovering the mouse cursor, may be <code>null</code>
	 */
	public void voronoiCellsHints(PlotObjects2D export, PlotHelper helper, HashMap<Polytope,AggregatedLocation> hints) {
		if (hints == null) {
			return;
		}
		
		// always remove all old entries, if present
		hints.clear();
		
		// get shift, scaling and clipping window
		double[] shift = helper.getShift();
		double scale   = helper.getScale();
		
		// construct the objects for the locations/cells
		for (int j = 0; j < voronoiMap.getLocationCells().size(); j++) {
			// Voronoi cell
			Polytope polytope = this.getVoronoiMap().getMapProjection().projectLatLong(voronoiMap.getLocationCells().get(j).getVoronoiCell());
			polytope.translateBy(shift);
			polytope.stretch(scale);
			
			hints.put(polytope, voronoiMap.getLocationCells().get(j).getLocation());
		}
	}
	
	/**
	 * Draws the borders of the Voronoi cells.
	 * 
	 * @param export  the <code>PlotObjects2D</code> object
	 * @param helper  helper object which provides shift, scaling factor, clipping window and drawing parameters
	 */
	public void voronoiCellBordersExport(PlotObjects2D export, PlotHelper helper) {
		if (helper.getCellLineWidth() <= 0.0) {
			return;
		}
		
		// get shift, scaling and clipping window
		double[] shift = helper.getShift();
		double scale   = helper.getScale();
		
		// cell borders to plot
		LinkedList<DrawableObject2D> cellBorders = new LinkedList<DrawableObject2D>();
		
		// construct the objects for the locations/cells
		for (int j = 0; j < voronoiMap.getLocationCells().size(); j++) {
			// Voronoi cell
			Polytope polytope = this.getVoronoiMap().getMapProjection().projectLatLong(voronoiMap.getLocationCells().get(j).getVoronoiCell());
			polytope.translateBy(shift);
			polytope.stretch(scale);
			
			// border of Voronoi cell
			cellBorders.add(new DrawableRandomSetElement2D(new Polytope(polytope.getVertices(), false), helper.getCellLineWidth(), helper.getCellBorderColor()));
		}
		
		// and, finally, draw the objects
		export.plot(cellBorders);
	}
	
	/**
	 * Draws the location points.
	 * 
	 * @param export  the <code>PlotObjects2D</code> object
	 * @param helper  helper object which provides shift, scaling factor, clipping window and drawing parameters
	 */
	public void locationPointsExport(PlotObjects2D export, PlotHelper helper) {
		if (helper.getLocationPointRadius() <= 0.0) {
			return;
		}
		
		// get shift, scaling and clipping window
		double[] shift = helper.getShift();
		double scale   = helper.getScale();
		
		// points of locations to plot
		LinkedList<DrawableObject2D> locationPoints = new LinkedList<DrawableObject2D>();
		
		// construct the objects for the locations/cells
		for (int j = 0; j < voronoiMap.getLocationCells().size(); j++) {
			Point point = this.getVoronoiMap().getMapProjection().projectLatLong(voronoiMap.getLocationCells().get(j).getPoint().toPoint());
			point.translateBy(shift);
			point.stretch(scale);
			Sphere sphere = new Sphere(point.getCoordinates(), helper.getLocationPointRadius(), true);
			locationPoints.add(new DrawableRandomSetElement2D(sphere, 0.0, helper.getLocationPointColor()));
		}
		
		// and, finally, draw the objects
		export.plot(locationPoints);
	}
	
	/**
	 * Draws the location codes.
	 * 
	 * @param export  the <code>PlotObjects2D</code> object
	 * @param helper  helper object which provides shift, scaling factor, clipping window and drawing parameters
	 */
	public void locationLabelsExport(PlotObjects2D export, PlotHelper helper) {
		if (helper.getLocationFontSize() <= 0) {
			return;
		}
		
		// get shift, scaling and clipping window
		double[] shift = helper.getShift();
		double scale   = helper.getScale();
		
		// location codes to plot
		LinkedList<DrawableObject2D> locationLabels = new LinkedList<DrawableObject2D>();
		
		// construct the objects for the locations/cells
		for (int j = 0; j < voronoiMap.getLocationCells().size(); j++) {
			// Voronoi cell
			Polytope polytope = this.getVoronoiMap().getMapProjection().projectLatLong(voronoiMap.getLocationCells().get(j).getVoronoiCell());
			polytope.translateBy(shift);
			polytope.stretch(scale);
			
			// location code, if necessary
			String locationCode = voronoiMap.getLocationCells().get(j).getLocation().getCode();
			if ((locationCode != null) && !locationCode.isEmpty()) {
				Geometry2D.Point barycentre = polytope.getCenterofGravity();
				Point textCoord = new Point(new double[] { barycentre.x, barycentre.y-helper.getLocationFontSize()/3 });
				locationLabels.add(new DrawableText(textCoord, locationCode, helper.getLocationFontSize(), this.getLocationLabelTextColor(voronoiMap.getLocationCells().get(j).getLocation())));
			}
		}
		
		// and, finally, draw the objects
		export.plot(locationLabels);
	}
	
	/**
	 * Returns the color that should be used to plot the location code into
	 * the map. (This method can be overridden in subclasses to use e.g. white
	 * font in dark areas.)
	 * 
	 * @param location  the location
	 * @return the color, e.g., black
	 */
	protected Color getLocationLabelTextColor(AggregatedLocation location) {
		return Color.BLACK;
	}
	
}
