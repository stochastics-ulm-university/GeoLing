package geoling.util.sim.grain;

import geoling.util.DoubleBox;
import geoling.util.sim.util.RandomSetElement;

import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

/**
* This class implements a line segment which may be an
* element of a random set.
*
* @author  Institute of Stochastics, Ulm University
* @version 1.0, 2001-09-28
*/
public final class LineSegment implements RandomSetElement {

    /** The coordinates of the starting point of the line segment. */
    private double x, y;
    /** The length of the line segment. */
    private double length;
    /** The angle of the line segment. */
    private double angle;
    /** The coordinates of the end point of the line segment. */
    private double xe, ye;

    /**
    * Constructs a new line segment with parameters.
    *
    * @param	x	the x-coordinate of the starting point.
    * @param	y	the y-coordinate of the starting point.
    * @param	length	the length.
    * @param	angle	the angle.
    */
    public LineSegment(double x, double y, double length, double angle) {
	this.x = x;
	this.y = y;
	this.length = length;
	this.angle = angle;
	computeEndPoint();
    }
    
    public LineSegment(double x, double y, double xe, double ye, boolean b) {
   		this.x = x;
   		this.y = y;
   		this.xe = xe;
   		this.ye = ye;
   		this.length=Math.sqrt((x-xe)*(x-xe)+(y-ye)*(y-ye));
    }

    /**
    * Returns the dimension of the line segment, i.e. the value 2.
    *
    * @return	the dimension of the line segment.
    */
    public int getDimension() {
        return 2;
    }

    /**
    * Returns the bounding box of the line segment.
    *
    * @return	the bounding box of the line segment.
    */
    public DoubleBox getBoundingBox() {
	double xmin = Math.min(x, xe);
	double ymin = Math.min(y, ye);
	double xmax = Math.max(x, xe);
	double ymax = Math.max(y, ye);
	return new DoubleBox(new double[] {xmin, ymin}, new double[] {xmax, ymax});
    }

    public Point getStartPoint() {
	    return new Point(new double[] {x, y});
    }
    
    public double getLength(){
        return this.length;
    }

    public Point getEndPoint() {
        return new Point(new double[] {xe, ye});
    }
    
    @Override
    public String toString(){
    	
    	String str = "LineSegment[("+this.x+","+this.y+"),("+this.xe+","+this.ye+")]";
    	return str;
    }
    
    /**
     * !!!WARNING!!! This breaks the usability of this class with Collections,
     * because the hashCode() method is not written in a suitable way!
     *
     * Returns true when the given object is a LineSegment which is very close
 	 * or equal to this LineSegmeent.
 	 *
 	 * @param	o	the LineSegment which should be tested for equality.
 	 * @return	true when the given object is a LineSegment nearby this 
 	 * 			LineSegment.
 	 */
    @Override
 	public boolean equals(Object o) {
 		
 	    if (o instanceof LineSegment) {
 	    	LineSegment ls = (LineSegment) o;
 	    	return ls.getStartPoint().equals(getStartPoint()) && ls.getEndPoint().equals(getEndPoint());
 	    }
 	    else
 	    	return false;
 	}

    /**
    * Translates the line segment by the given vector.
    *
    * @param	vector	the vector by which the line segment is
    *			to be translated.
    * @throws	IllegalArgumentException
    *			if <code>vector</code> has not dimension 2.
    */
    public void translateBy(double[] vector) {
	if (vector.length != 2)
	    throw new IllegalArgumentException("vector must have dimension 2");
	x += vector[0];
	y += vector[1];
	xe += vector[0];
	ye += vector[1];
    }

    /**
    * Draws the line segment in to the given image.
    *
    * @param	image	the image in which the line segment is to be drawn.
    * @throws    IllegalArgumentException
    *                   if drawing is not yet implemented for
    *                   this image type or dimension.
    */
    public void draw(Object image) {
	if (image instanceof Graphics2D && getDimension() == 2) {
	    Graphics2D g = (Graphics2D) image;

	    GeneralPath p = new GeneralPath();
	    p.moveTo((float) x, (float) y);
	    p.lineTo((float) xe, (float) ye);
	    g.draw(p);
	}
	else
	    throw new IllegalArgumentException("draw not yet implemented for this image type");
    }

    
    @Override
    public LineSegment clone(){
    	
    	Point p1 = this.getStartPoint();
    	Point p2 = this.getEndPoint();
    	
    	Point p1_clone = (Point) p1.clone();
    	Point p2_clone = (Point) p2.clone();
    	
    	return new LineSegment(p1_clone.getCoordinates()[0],p1_clone.getCoordinates()[1],p2_clone.getCoordinates()[0], p2_clone.getCoordinates()[1],true);
    }

    /**
     * Stretch a LineSegment by the stretch factor scale
     */
    public void stretch(double scale){
    	
    	this.x = this.x*scale;
    	this.y = this.y*scale;
    	this.xe = this.xe*scale;
    	this.ye = this.ye*scale;
    	this.length = this.length*scale;
    }
    
    /**
    * Computes the end point of the line segment.
    */
    private void computeEndPoint() {
	xe = x + length * Math.cos(angle);
	ye = y + length * Math.sin(angle);
    }
}

