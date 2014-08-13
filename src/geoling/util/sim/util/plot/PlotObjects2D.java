package geoling.util.sim.util.plot;

import geoling.util.DoubleBox;

import java.awt.Color;
import java.util.List;

/**
 * An abstract class for drawing objects into 2D images.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public abstract class PlotObjects2D {
	
	/** The frame enclosing the image. */
	protected DoubleBox box;
	
	/** The line width of the frame enclosing the image. */
	private double borderLineWidth;
	
	/** The color of the frame enclosing the image. */
	private Color borderColor;
	
	/**
	 * Generates the basic plot object.
	 * 
	 * @param box  the bounding box
	 */
	public PlotObjects2D(DoubleBox box) {
		if (box.getDimension() != 2) {
			throw new IllegalArgumentException("Box must have dimension 2.");
		}
		
		this.box             = box;
		this.borderLineWidth = Math.max(box.getWidth(0), box.getWidth(1)) * 0.001;
		this.borderColor     = Color.BLACK;
	}
	
	/**
	 * Returns the bounding box.
	 * 
	 * @return the bounding box
	 */
	public DoubleBox getBox() {
		return box;
	}
	
	/**
	 * Returns the line width of the frame enclosing the image.
	 * 
	 * @return the line width of the frame enclosing the image
	 */
	public double getBorderLineWidth() {
		return borderLineWidth;
	}
	
	/**
	 * Sets the line width of the frame enclosing the image.
	 *
	 * @param borderLineWidth  the line width of the frame enclosing the image
	 * @throws IllegalArgumentException if borderLineWidth is not positive
	 */
	public void setBorderLineWidth(double borderLineWidth) {
		if (borderLineWidth <= 0.0) {
			throw new IllegalArgumentException("borderLineWidth must be positive");
		}
		
		this.borderLineWidth = borderLineWidth;
	}
	
	/**
	 * Returns the color of the frame enclosing the image.
	 *
	 * @return the color of the frame enclosing the image
	 */
	public Color getBorderColor() {
		return borderColor;
	}
	
	/**
	 * Sets the color of the frame enclosing the image.
	 *
	 * @param borderColor the color of the frame enclosing the image
	 * @throws IllegalArgumentException if borderColor is null
	 */
	public void setBorderColor(Color borderColor) {
		if (borderColor == null) {
			throw new IllegalArgumentException("borderColor must not be null");
		}
		this.borderColor = borderColor;
	}
	
	/**
	 * Plots all the specified objects in the given order to the image.
	 * Note that this method may be called several times to add more objects
	 * to an existing image.
	 * 
	 * @param objects  the objects that should be plotted
	 */
	public abstract void plot(List<DrawableObject2D> objects);
	
}