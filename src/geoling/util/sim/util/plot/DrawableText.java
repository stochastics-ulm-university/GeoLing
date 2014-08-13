package geoling.util.sim.util.plot;

import geoling.util.sim.grain.Point;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.io.PrintStream;

/**
 * A class for text that can be drawn into a 2D image.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class DrawableText implements DrawableObject2D {
	
	/** The possible alignment types of the text. */
	public static enum Alignment { CENTER, LEFT, RIGHT };
	
	/** The font name for <code>TextObject</code> objects. */
	public static String TEXT_FONT_NAME = "Arial";
	
	/** The position of the text (bottom of the line, center or left of the text). */
	private Point coord;
	
	/** The text that should be written. */
	private String text;
	
	/** The font size that should be used. */
	private int fontSize;
	
	/** The text color that should be used. */
	private Color color;
	
	/** Determines whether the text should be left- or right-aligned instead of centered (default). */
	private Alignment align;
	
	/**
	 * Constructs a text object that can be plotted.
	 * 
	 * @param coord     the position of the text (bottom of the line, center of the text)
	 * @param text      the text string
	 * @param fontSize  the font size, e.g. <code>10</code>
	 * @param color     the color
	 */
	public DrawableText(Point coord, String text, int fontSize, Color color) {
		this(coord, text, fontSize, color, Alignment.CENTER);
	}
	
	/**
	 * Constructs a text object used to insert text into a random set
	 * which will be plotted.
	 * 
	 * @param coord     the position of the text (bottom of the line, center or left of the text)
	 * @param text      the text string
	 * @param fontSize  the font size, e.g. <code>10</code>
	 * @param color     the color
	 * @param align     determines the alignment of the text
	 */
	public DrawableText(Point coord, String text, int fontSize, Color color, Alignment align) {
		if (coord.getDimension() != 2) {
			throw new IllegalArgumentException("TextObject requires 2D coordinates!");
		}
		this.coord     = coord;
		this.text      = text;
		this.fontSize  = fontSize;
		this.color     = color;
		this.align     = align;
	}
	
	/**
	 * Returns the position of the text.
	 * 
	 * @return the position of the text
	 */
	public double[] getCoordinates() {
		return this.coord.getCoordinates();
	}
	
	/**
	 * Returns the text.
	 * 
	 * @return the text
	 */
	public String getText() {
		return this.text;
	}
	
	/**
	 * Returns the font size.
	 * 
	 * @return the font size
	 */
	public int getFontSize() {
		return this.fontSize;
	}
	
	/**
	 * Returns the alignment of the text.
	 * 
	 * @return the alignment of the text
	 */
	public Alignment getAlign() {
		return this.align;
	}
	
	/**
	 * Draws this object to a <code>Graphics2D</code> object using a
	 * <code>PlotToGraphics2D</code> plot class.
	 * 
	 * @param graphics  the <code>PlotToGraphics2D</code> plot object
	 */
	public void draw(PlotToGraphics2D graphics) {
		synchronized (graphics) {
			Graphics2D out = graphics.getGraphics2D();
			out.setColor(color);
			
			Font oldFont = out.getFont();
			AffineTransform oldTransform = out.getTransform();
			out.setFont(new Font(TEXT_FONT_NAME, Font.PLAIN, this.getFontSize()));
			out.setTransform(new AffineTransform());
			
			float shiftLeft;
			switch (this.getAlign()) {
			case CENTER:
				shiftLeft = out.getFontMetrics().stringWidth(this.getText())/2.0f;
				break;
			case LEFT:
				shiftLeft = 0.0f;
				break;
			case RIGHT:
				shiftLeft = out.getFontMetrics().stringWidth(this.getText());
				break;
			default:
				throw new RuntimeException("Unknown alignment option!");
			}
			
			out.drawString(this.getText(), (float)this.getCoordinates()[0] - shiftLeft,
			               out.getClipBounds().height-(float)this.getCoordinates()[1]);
			out.setFont(oldFont);
			out.setTransform(oldTransform);
		}
	}
	
	/**
	 * Draws this object to an EPS image file using a
	 * <code>PlotToEPS</code> plot class.
	 * 
	 * @param eps  the <code>PlotToEPS</code> plot object
	 */
	public void drawEPS(PlotToEPS eps) {
		synchronized (eps) {
			eps.printColor(color);
			
			String align;
			switch (this.getAlign()) {
			case CENTER:
				align = ".5";
				break;
			case LEFT:
				align = "0";
				break;
			case RIGHT:
				align = "1";
				break;
			default:
				throw new RuntimeException("Unknown alignment option!");
			}
			
			PrintStream p = eps.getStream();
			p.println();
			p.println("/ps "+this.getFontSize()+" def /Font1 findfont "+this.getFontSize()+" s");
			p.println(this.getCoordinates()[0]+" "+this.getCoordinates()[1]+" ("+this.getText()+") "+align+" 0 0 t");
		}
	}
	
}
