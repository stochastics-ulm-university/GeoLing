package geoling.util.sim.util.plot;

import geoling.util.sim.grain.ConvexPolytope;
import geoling.util.sim.grain.Point;
import geoling.util.DoubleBox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * A class for drawing objects into <code>Graphics2D</code> images.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class PlotToGraphics2D extends PlotObjects2D implements AutoCloseable {
	
	/** The image object. */
	protected Graphics2D out;
	
	/** Determines whether we have already initialized the image. */
	protected boolean initDone = false;
    
	/**
	 * Generates a new <code>Graphics2D</code> plot object.
	 * 
	 * @param box  the bounding box
	 * @param out  the <code>Graphics2D</code> image
	 */
    public PlotToGraphics2D(DoubleBox box, Graphics2D out) {
		super(box);
		this.out = out;
	}
	
	/**
	 * Returns the image object.
	 * 
	 * @return the image object
	 */
	public Graphics2D getGraphics2D() {
		return this.out;
	}
	
	/**
	 * Plots all the specified objects in the given order to the
	 * <code>Graphics2D</code> image.
	 * Note that this method may be called several times to add more objects
	 * to an existing image.
	 * 
	 * @param objects  the objects that should be plotted
	 */
	public synchronized void plot(List<DrawableObject2D> objects) {
		if (!initDone) {
			initDone = true;
			initImage();
		}
		
		for (DrawableObject2D object : objects) {
			object.draw(this);
		}
	}
	
	/**
	 * Finalizes the image, i.e., adds the border rectangle.
	 */
	public synchronized void close() {
		outputBorder();
	}
	
	/**
	 * Initializes the image, i.e., sets the transformation required for
	 * having the same behavior as in EPS and sets the clipping area.
	 */
	private synchronized void initImage() {
		double[] min = box.getMin();
		double[] max = box.getMax();
		double width = max[0] - min[0];
		double height = max[1] - min[1];
		
		// transformation: mirror y-coordinates to have the same behavior as in EPS export
		
		out.transform(new AffineTransform(1, 0, 0, -1, 0, height));
		
		// establish clipping area (bounding box)
		
		out.setBackground(Color.WHITE);
		out.setClip((int)Math.floor(min[0]), (int)Math.floor(min[1]), (int)Math.ceil(width), (int)Math.ceil(height));
		out.fillRect((int)Math.floor(min[0]), (int)Math.floor(min[1]), (int)Math.ceil(width), (int)Math.ceil(height));
	}
		
	/**
	 * Adds the border rectangle.
	 */
	private synchronized void outputBorder() {
		double[] min = box.getMin();
		double[] max = box.getMax();
		
		out.setStroke(new BasicStroke((float)this.getBorderLineWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
		out.setColor(this.getBorderColor());
		
		(new ConvexPolytope(new Point[] { new Point(new double[] { min[0], min[1] }),
		                                  new Point(new double[] { max[0], min[1] }),
		                                  new Point(new double[] { max[0], max[1] }),
		                                  new Point(new double[] { min[0], max[1] })})).draw(out);
	}
	
}
