package geoling.util.sim.util.plot;

import geoling.util.DoubleBox;
import geoling.util.Utilities;

import java.awt.Color;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * A class for drawing objects into EPS image files.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class PlotToEPS extends PlotObjects2D implements AutoCloseable {
	
	/** The output stream. */
	protected PrintStream p;
	
	/** The title of the image. */
	private String title = "(unknown)";
	
	/** Determines whether we have already written the EPS header to the stream. */
	protected boolean headerWritten = false;
	
	/** Stores the previously written line width. */
	protected double prevLineWidth = Double.NaN;
	
	/** Stores the previously written color. */
	protected Color prevColor = null;
    
	/**
	 * Generates a new EPS plot object for the given output stream.
	 * 
	 * @param box  the bounding box
	 * @param out  the output stream
	 */
    public PlotToEPS(DoubleBox box, OutputStream out) {
		super(box);
		p = new PrintStream(out);
	}
	
    /**
     * Returns the output stream of the EPS file.
     * 
     * @return the EPS stream
     */
	public PrintStream getStream() {
		return this.p;
	}
	
	/**
	 * Returns the title of the EPS file.
	 *
	 * @return the title of the EPS file
	 */
	public synchronized String getTitle() {
		return this.title;
	}
	
	/**
	 * Sets the title of the EPS file.
	 *
	 * @param title  the title of the EPS file
	 */
	public synchronized void setTitle(String title) {
		this.title = title;
	}
	
	/**
	 * Plots all the specified objects in the given order to the EPS image.
	 * Note that this method may be called several times to add more objects
	 * to an existing image.
	 * 
	 * @param objects  the objects that should be plotted
	 */
	public synchronized void plot(List<DrawableObject2D> objects) {
		if (!headerWritten) {
			headerWritten = true;
			writeHeader();
		}
		
		for (DrawableObject2D object : objects) {
			object.drawEPS(this);
		}
	}
	
	/**
	 * Writes the border rectangle and closes the EPS stream.
	 */
	public synchronized void close() {
		writeFooter();
		p.close();
	}
	
	/**
	 * Writes all necessary header information to the EPS stream
	 * and sets the clipping area.
	 */
	private synchronized void writeHeader() {
		double[] min = box.getMin();
		double[] max = box.getMax();
		
		// EPS header
		
		p.println("%!PS-Adobe-2.0 EPSF-2.0");
		p.println("%%Title: "+title);
		p.println("%%Creator: GeoLing EpsExport");
		p.println("%%CreationDate: "+new java.util.Date(System.currentTimeMillis()));
		p.println("%%BoundingBox: "+(min[0]-this.getBorderLineWidth()/2.0)+" "+(min[1]-this.getBorderLineWidth()/2.0)+" "+
		                            (max[0]+this.getBorderLineWidth()/2.0)+" "+(max[1]+this.getBorderLineWidth()/2.0));
		p.println("%%Magnification: 1.0000");
		p.println("%%EndComments");
		
		p.println();
		
		p.println("%%BeginProlog");
		p.println("/ep  { showpage gr gr } def");
		p.println("/t   { 6 -2 roll moveto gsave rotate");
		p.println("       ps mul neg 0 2 1 roll rmoveto");
		p.println("       1 index stringwidth pop");
		p.println("       mul neg 0 rmoveto show grestore } def");
		p.println("/rgb { setrgbcolor } def");
		p.println("/s   { scalefont setfont } def");
		p.println();
		p.println("%%IncludeResource: font "+DrawableText.TEXT_FONT_NAME);
		p.println("/"+DrawableText.TEXT_FONT_NAME+" findfont");
		p.println("dup length dict begin");
		p.println("  {1 index /FID ne {def} {pop pop} ifelse} forall");
		p.println("  /Encoding ISOLatin1Encoding def");
		p.println("  currentdict");
		p.println("  end");
		p.println("%%EndProlog");
		
		p.println();
		
		// save old clipping area
		
		p.println("gsave");
		p.println();
		
		// establish clipping area (= bounding box)
		
		p.println("newpath");
		p.println(" "+min[0]+" "+min[1]+" moveto");
		p.println(" "+max[0]+" "+min[1]+" lineto");
		p.println(" "+max[0]+" "+max[1]+" lineto");
		p.println(" "+min[0]+" "+max[1]+" lineto");
		p.println("closepath");
		
		p.println("clip");
		
		p.println();
		
		// general settings (line width and line join style)
		
		printLineWidth(this.getBorderLineWidth());
		p.println("1 setlinejoin");
		
		printColor(Color.BLACK);
		
		p.println();
	}
	
	/**
	 * Writes the border rectangle.
	 */
	private synchronized void writeFooter() {
		double[] min = box.getMin();
		double[] max = box.getMax();
		
		// restore old clipping area
		
		p.println();
		p.println("grestore");
		p.println();
		
		// general settings (line width and line join style)
		
		printLineWidth(this.getBorderLineWidth());
		p.println("1 setlinejoin");
		
		// color for the enclosing frame
		
		printColor(this.getBorderColor());
		
		// draw the enclosing frame (i.e. clipping path)
		
		p.println("newpath");
		p.println(" "+min[0]+" "+min[1]+" moveto");
		p.println(" "+max[0]+" "+min[1]+" lineto");
		p.println(" "+max[0]+" "+max[1]+" lineto");
		p.println(" "+min[0]+" "+max[1]+" lineto");
		p.println("closepath");
		
		p.println("stroke");
		
		p.flush();
	}
	
	/**
	 * Outputs the given line width to the EPS stream.
	 * This method should always be used, so this class can keep
	 * track of the previously written value (in case it does not
	 * change we do not have to set the same line width again).
	 * 
	 * @param lineWidth  the line width
	 */
	protected synchronized void printLineWidth(double lineWidth) {
		if (!Utilities.isEqual(prevLineWidth, lineWidth)) {
			p.println(lineWidth+" setlinewidth");
			prevLineWidth = lineWidth;
		}
	}
	
	/**
	 * Outputs the given color to the EPS stream.
	 * This method should always be used, so this class can keep
	 * track of the previously written value (in case it does not
	 * change we do not have to set the same color again).
	 * 
	 * @param color  the color
	 */
	protected synchronized void printColor(Color color) {
		if (!color.equals(this.prevColor)) {
			double cr = (double)color.getRed() / 255.0;
			double cg = (double)color.getGreen() / 255.0;
			double cb = (double)color.getBlue() / 255.0;
			
			p.println();
			p.println(cr+" "+cg+" "+cb+" setrgbcolor");
			p.println();
			
			this.prevColor = color;
		}
	}
	
}