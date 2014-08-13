package geoling.util.sim.util.plot;

/**
 * An interface for objects that can be drawn into a 2D image.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public interface DrawableObject2D {
	
	/**
	 * Draws this object to a <code>Graphics2D</code> object using a
	 * <code>PlotToGraphics2D</code> plot class.
	 * 
	 * @param graphics  the <code>PlotToGraphics2D</code> plot object
	 */
	public void draw(PlotToGraphics2D graphics);
	
	/**
	 * Draws this object to an EPS image file using a
	 * <code>PlotToEPS</code> plot class.
	 * 
	 * @param eps  the <code>PlotToEPS</code> plot object
	 */
	public void drawEPS(PlotToEPS eps);
	
}