package geoling.maps.plot;

import java.awt.Color;

import geoling.maps.projection.MapProjection;
import geoling.maps.util.MapBorder;
import geoling.models.ConfigurationOption;
import geoling.util.DoubleBox;
import geoling.util.sim.grain.Polytope;

/**
 * Plot helper object for determining shift vector and scaling factor of a map,
 * provides also further parameters for drawing the map.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class PlotHelper {
	
	/** The default height used for new <code>PlotHelper</code> objects. */
	public static int DEFAULT_HEIGHT = 800;
	
	/** The default width of the border used for new <code>PlotHelper</code> objects. */
	public static int DEFAULT_BORDER = 10;
	
	/** The shift vector to be applied before scaling. */
	private double[] shift;
	
	/** The scaling factor to be applied after shifting. */
	private double scale;
	
	/** The resulting clipping window. */
	private DoubleBox window;
	
	/** The line width for the map border. */
	private double borderLineWidth;
	
	/** The radius for the points of the locations. */
	private double locationPointRadius;
	
	/** The font size for the short location codes. */
	private int locationFontSize;
	
	/** y-position of the legend, <code>Double.NaN</code> if no legend should be inserted. */
	private double legendLeft;
	
	/** The font size for legend. */
	private int legendFontSize;
	
	/** The line width for the border of every cell. */
	private double cellLineWidth;
	
	/** The line width for the border of the areas. */
	private double areaLineWidth;
	
	/** The color for the map border. */
	private Color borderColor;
	
	/** The color for the points of the locations. */
	private Color locationPointColor;
	
	/** The color for the border of every cell. */
	private Color areaBorderColor;
	
	/** The color for the border of the areas. */
	private Color cellBorderColor;
	
	/**
	 * Constructs a new plot helper object for a given border polygon with
	 * a default size of the map.
	 * 
	 * @param border          the border polygon
	 * @param mapProjection   the projection method for the coordinates
	 */
	public PlotHelper(Polytope border, MapProjection mapProjection) {
		this(border, mapProjection, -1, -1);
	}
	
	/**
	 * Constructs a new plot helper object for a given border polygon with
	 * desired height of the map itself, plus a fixed number of pixels for
	 * a white border.
	 * 
	 * @param border          the border polygon
	 * @param mapProjection   the projection method for the coordinates
	 * @param height          the height in pixels of the map itself, used to compute a scale factor,
	 *                        uses a default value for negative numbers
	 * @param addBorderPixels add blank pixels at the border of the image,
	 *                        uses a default value for negative numbers
	 */
	public PlotHelper(Polytope border, MapProjection mapProjection, int height, int addBorderPixels) {
		this(border, mapProjection, height, addBorderPixels, false);
	}
	
	/**
	 * Constructs a new plot helper object for a given border polygon with
	 * desired height of the map itself, plus a fixed number of pixels for
	 * a white border.
	 * 
	 * @param border          the border polygon
	 * @param mapProjection   the projection method for the coordinates
	 * @param height          the height in pixels of the map itself, used to compute a scale factor,
	 *                        uses a default value for negative numbers
	 * @param addBorderPixels add blank pixels at the border of the image,
	 *                        uses a default value for negative numbers
	 * @param legend          determines whether a legend should be inserted at the right-hand side of the plot
	 */
	public PlotHelper(Polytope border, MapProjection mapProjection, int height, int addBorderPixels, boolean legend) {
		if (height < 0) {
			height = DEFAULT_HEIGHT;
		}
		if (addBorderPixels < 0) {
			addBorderPixels = DEFAULT_BORDER;
		}
		
		// window with x-y coordinates: create window based on the polygon transformed to kilometres
		// (transformation of the box given by latitude/longitude is problematic due to the
		//  latitude having influence on the longitude kilometres)
		DoubleBox windowXY = MapBorder.getWindow(mapProjection.projectLatLong(border));
		
		// construct scale factor required to achieve the given height
		scale = (double)height / windowXY.getWidth(1);
		// construct shift vector to shift the min-point of the window to (0,0)
		shift = new double[2];
		shift[0] = -windowXY.getMin(0)+addBorderPixels/scale;
		shift[1] = -windowXY.getMin(1)+addBorderPixels/scale;
		
		// shifted window
		double mapWidth  = windowXY.getWidth(0)*scale;
		double mapHeight = windowXY.getWidth(1)*scale;
		double legendWidth = (legend ? mapWidth/3.0 + addBorderPixels : 0.0);
		window = new DoubleBox(new double[] { 0.0, 0.0 }, new double[] { mapWidth + legendWidth + 2*addBorderPixels,
		                                                                 mapHeight + 2*addBorderPixels });
		legendLeft = (legend ? mapWidth + 2*addBorderPixels : Double.NaN );
		
		// default values for drawing parameters
		// TODO: set line width / radius variables depending on the image height
		borderLineWidth     = 1.0;
		locationPointRadius = 2.0;
		locationFontSize    = 0;
		legendFontSize      = legend ? 10 : 0;
		cellLineWidth       = 0.0;
		areaLineWidth       = 2.0;
		borderColor         = Color.black;
		locationPointColor  = Color.black;
		areaBorderColor     = Color.orange;
		cellBorderColor     = Color.black;
		
		if (ConfigurationOption.getOption("plotLocationCodes", false)) {
			locationPointRadius = 0.0;
			locationFontSize    = 10;
		}
	}
	
	/**
	 * Returns the shift vector to be applied before scaling.
	 * 
	 * @return the shift vector
	 */
	public double[] getShift() {
		return shift;
	}
	
	/**
	 * Returns the scaling factor to be applied after shifting.
	 * 
	 * @return the scaling factor
	 */
	public double getScale() {
		return scale;
	}
	
	/**
	 * Returns the resulting clipping window.
	 * 
	 * @return the window
	 */
	public DoubleBox getWindow() {
		return window;
	}
	
	/**
	 * Returns the width of the window as an integer value.
	 * 
	 * @return the width
	 */
	public int getWidth() {
		return (int)Math.ceil(window.getWidth(0));
	}
	
	/**
	 * Returns the height of the window as an integer value.
	 * 
	 * @return the height
	 */
	public int getHeight() {
		return (int)Math.ceil(window.getWidth(1));
	}
	
	/**
	 * Returns the line width for the map border.
	 * 
	 * @return the line width
	 */
	public double getBorderLineWidth() {
		return borderLineWidth;
	}
	
	/**
	 * Sets the line width for the map border.
	 * 
	 * @param borderLineWidth the line width to set
	 */
	public void setBorderLineWidth(double borderLineWidth) {
		this.borderLineWidth = borderLineWidth;
	}
	
	/**
	 * Returns the radius for the points of the locations.
	 * 
	 * @return the radius
	 */
	public double getLocationPointRadius() {
		return locationPointRadius;
	}
	
	/**
	 * Sets the radius for the points of the locations.
	 * 
	 * @param locationPointRadius the radius to set
	 */
	public void setLocationPointRadius(double locationPointRadius) {
		this.locationPointRadius = locationPointRadius;
	}
	
	/**
	 * Returns the font size for the short location codes.
	 * 
	 * @return the font size
	 */
	public int getLocationFontSize() {
		return locationFontSize;
	}
	
	/**
	 * Sets the font size for the short location codes.
	 * 
	 * @param locationFontSize the font size to set
	 */
	public void setLocationFontSize(int locationFontSize) {
		this.locationFontSize = locationFontSize;
	}
	
	/**
	 * Returns the y-position of the legend.
	 * 
	 * @return y-position of the legend, <code>Double.NaN</code> if no legend should be inserted
	 */
	public double getLegendLeft() {
		return legendLeft;
	}
	
	/**
	 * Returns the font size for the legend.
	 * 
	 * @return the font size
	 */
	public int getLegendFontSize() {
		return legendFontSize;
	}
	
	/**
	 * Sets the font size for the legend.
	 * 
	 * @param legendFontSize the font size to set
	 */
	public void setLegendFontSize(int legendFontSize) {
		this.legendFontSize = legendFontSize;
	}
	
	/**
	 * Returns the line width for the border of every cell.
	 * 
	 * @return the line width
	 */
	public double getCellLineWidth() {
		return cellLineWidth;
	}
	
	/**
	 * Sets the line width for the border of every cell.
	 * 
	 * @param cellLineWidth the line width to set
	 */
	public void setCellLineWidth(double cellLineWidth) {
		this.cellLineWidth = cellLineWidth;
	}
	
	/**
	 * Returns the line width for the border of the areas.
	 * 
	 * @return the line width
	 */
	public double getAreaLineWidth() {
		return areaLineWidth;
	}
	
	/**
	 * Sets the line width for the border of the areas.
	 * 
	 * @param areaLineWidth the line width to set
	 */
	public void setAreaLineWidth(double areaLineWidth) {
		this.areaLineWidth = areaLineWidth;
	}
	
	/**
	 * Returns the color for the map border.
	 * 
	 * @return the color
	 */
	public Color getBorderColor() {
		return borderColor;
	}
	
	/**
	 * Sets the color for the map border.
	 * 
	 * @param borderColor the color to set
	 */
	public void setBorderColor(Color borderColor) {
		this.borderColor = borderColor;
	}
	
	/**
	 * Returns the color for the points of the locations.
	 * 
	 * @return the color
	 */
	public Color getLocationPointColor() {
		return locationPointColor;
	}
	
	/**
	 * Sets the color for the points of the locations.
	 * 
	 * @param locationPointColor the color to set
	 */
	public void setLocationPointColor(Color locationPointColor) {
		this.locationPointColor = locationPointColor;
	}
	
	/**
	 * Returns the color for the border of every cell.
	 * 
	 * @return the color
	 */
	public Color getAreaBorderColor() {
		return areaBorderColor;
	}
	
	/**
	 * Sets the color for the border of every cell.
	 * 
	 * @param areaBorderColor the color to set
	 */
	public void setAreaBorderColor(Color areaBorderColor) {
		this.areaBorderColor = areaBorderColor;
	}
	
	/**
	 * Returns the color for the border of the areas.
	 * 
	 * @return the color
	 */
	public Color getCellBorderColor() {
		return cellBorderColor;
	}
	
	/**
	 * Sets the color for the border of the areas.
	 * 
	 * @param cellBorderColor the color to set
	 */
	public void setCellBorderColor(Color cellBorderColor) {
		this.cellBorderColor = cellBorderColor;
	}
	
}
