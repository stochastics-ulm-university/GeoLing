package geoling.util.sim.grain;

import geoling.util.DoubleBox;
import geoling.util.geom.Geometry2D;
import geoling.util.sim.util.RandomSetElement;

import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

/**
 * This class implements a point object which may be an
 * element of a random set.
 *
 * @author  Institute of Stochastics, Ulm University
 * @version 1.1, 2014-02-13
 */
public class Point implements RandomSetElement, Cloneable {
    /** The coordinates of the point. */
    private double[] coord;

    /**
     * Constructs a new point with the given coordinates.
     *
     * @param	coordinates	the coordinates of the point. 
     */
    public Point(double[] coordinates) {
        coord = (double[]) coordinates.clone();
    }

    /**
     * Returns a copy of this point.
     *
     * @return	a copy of this point.
     */
    @Override
    public Object clone() {
        Point p;
        try {
            p = (Point) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
        p.coord = (double[]) coord.clone();
        return p;
    }

    /**
     * Returns the coordinates of the point.
     *
     * @return	the coordinates of the point.
     */
    public double[] getCoordinates() {
        return (double[]) coord.clone();
    }

    /**
     * Sets the coordinates of the point.
     *
     * @param	coordinates	the coordinates of the point.
     */
    public void setCoordinates(double[] coordinates) {
        coord = (double[]) coordinates.clone();
    }

    /**
     * Returns the dimension of the point.
     *
     * @return	the dimension of the point.
     */
    public int getDimension() {
        return coord.length;
    }

    /**
     * Returns the bounding box of the point.
     *
     * @return	the bounding box of the point.
     */
    public DoubleBox getBoundingBox() {
        return new DoubleBox(coord, coord);
    }

    /**
     * Translates the point by the given vector.
     *
     * @param	vector	the vector by which the point is
     *			to be translated.
     * @throws	IllegalArgumentException
     *			if <code>vector</code> has not the
     *			same dimension as the point.
     */
    public void translateBy(double[] vector) {
        if (coord.length != vector.length)
            throw new IllegalArgumentException("vector must have the same dimension as the point");
        for (int i = 0; i < coord.length; i++)
            coord[i] += vector[i];
    }

    /**
     * Draws the point in to the given image.
     *
     * @param	image	the image in which the point is to be drawn.
     * @throws    IllegalArgumentException
     *                   if drawing is not yet implemented for
     *                   this image type or dimension.
     */
    public void draw(Object image) {
        if (image instanceof Graphics2D && getDimension() == 2) {
            Graphics2D g = (Graphics2D) image;
            GeneralPath p = new GeneralPath();
            p.moveTo((float) coord[0], (float) coord[1]);
            p.closePath();
            g.draw(p);
        }
        else
            throw new IllegalArgumentException("draw not yet implemented for this image type");
    }

    /**
     * Returns the Euclidean distance of the given points.
     *
     * @param	p	one point.
     * @param	q	the other point.
     * @return	the Euclidean distance of <code>p</code> and <code>q</code>.
     * @throws	IllegalArgumentException
     *			if <code>p</code> and <code>q</code> do not
     *			have the same dimension.
     */
    public static double distance(Point p, Point q) {
        return distance(p.coord, q.coord);
    }

    /**
     * Returns the Euclidean distance of the given points.
     *
     * @param	p	one point.
     * @param	q	the other point.
     * @return	the Euclidean distance of <code>p</code> and <code>q</code>.
     * @throws	IllegalArgumentException
     *			if <code>p</code> and <code>q</code> do not
     *			have the same dimension.
     */
    public static double distance(double[] p, double[] q) {
        if (p.length != q.length)
            throw new IllegalArgumentException("p and q must have the same length");
        double dist2 = 0.0;
        for (int i = 0; i < p.length; i++)
            dist2 += (p[i] - q[i]) * (p[i] - q[i]);
        return Math.sqrt(dist2);
    }

    /**
     * Returns a string representation of this point for
     * debugging purposes.
     *
     * @return	a string represenation of the point.
     */
    @Override
    public String toString() {
        String s = "Point([";
        for (int i = 0; i < coord.length; i++) {
            if (i > 0)
                s = s + ", ";
            s = s + coord[i];
        }
        return s + "])";
    }
    
    /**
     * !!!WARNING!!! This breaks the usability of this class with Collections,
     * because the equals() method is not written in a suitable way!
     */
    @Override
    public int hashCode() {
    	double x = this.getCoordinates()[0];
        double y = this.getCoordinates()[1];
    	return (int) (x + y);
    }

    /**
     * !!!WARNING!!! This breaks the usability of this class with Collections,
     * because the hashCode() method is not written in a suitable way!
     *
     * Returns <code>true</code> when the given object is a point which is very
     * close or equal to this point.
     *
     * @param	o	the object which should be tested
     *			for equality.
     * @return	<code>true</code> when the given object
     *		is a point nearby this point.
     */
    @Override
    public boolean equals(Object o) {
        if(o instanceof Point) {
            Point p = (Point) o;
            for(int i = 0; i < p.getDimension(); i++){
                if(!geoling.util.Utilities.isEqual(p.coord[i], this.coord[i]))
                    return false;
            }
            return true;
        } else 
            return false;
    }
    
    /**
     * transforms this point to <code>Geometry2D.Point</code>
     * @return <code>Geometry2D.Point</code>
     */
    public Geometry2D.Point toGeo2DPoint(){
        if(this.coord.length==2) return new Geometry2D.Point(this.coord[0], this.coord[1]);
        else throw new RuntimeException("Only 2D points can be converted to Geometry2D.Point!"); // old: return new Geometry2D.Point(0,0);
    }
    
    /**
     * Stretch a point by the stretch factor scale
     */
    public void stretch(double scale){
        this.coord = new double[]{this.coord[0]*scale, this.coord[1]*scale};
    }
}
