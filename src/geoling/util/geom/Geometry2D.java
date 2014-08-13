package geoling.util.geom;

import java.util.Vector;
import java.awt.geom.GeneralPath;
import java.awt.Graphics2D;

import geoling.util.DoubleBox;
import geoling.util.sim.grain.ConvexPolytope;
import geoling.util.sim.util.RandomSetElement;

/**
 * This class provides elementary geometrical algorithms
 * for intersections and other related stuff for
 * the 2-dimensional case.
 *
 * @author  Institute of Stochastics, Ulm University
 * @version 1.1, 2002-03-04
 */
public class Geometry2D {
    
    /** There should be no instance of <code>Geometry2D</code>. */
    private Geometry2D() {
    }
    
    /**
     * The value of epsilon used for correction of
     * numerical errors.
     */
    //private static final double eps = 1e-10;
    private static final double eps = 1e-9;
    
    /**
     * Returns the maximum absolute value of three
     * real numbers.
     *
     * @param	x	the first number.
     * @param	y	the second number.
     * @param	z	the third number.
     * @return	the maximum of the absolute values.
     */
    private static double maxabs(double x, double y, double z) {
        x = (x < 0) ? -x : x;
        y = (y < 0) ? -y : y;
        z = (z < 0) ? -z : z;
        return (x > y) ? (x > z) ? x : z
		       : (y > z) ? y : z;
    }
    
    /**
     * Tests two double values for equality and
     * deals with numerical errors.
     *
     * @param	x	one double value to be compared.
     * @param	y	the other double value to be compared.
     * @return	<code>true</code> if and only if <code>x</code>
     *		and <code>y</code> are equally.
     */
    public static boolean isEqual(double x, double y) {
        return (x == y) ||
	       // size of eps is adapted for values greater than 1
	       (Math.abs(x - y) < eps * maxabs(1, x, y));
    }
    
    /** Interface for all geometric objects. */
    public static interface GeometricObject {
    }
    
    /** This class represents a 2-dimensional point. */
    public static class Point implements GeometricObject {
        /** The cartesian coordinates of the point. */
        public double x, y;
        /**
         * Constructs a new point with the given
         * cartesian coordinates.
         *
         * @param	x	the x-coordinate.
         * @param	y	the y-coordinate.
         */
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
        /**
         * Constructs a new point which is just a
         * copy of the given point.
         *
         * @param	p	the point which is to be copied.
         */
        public Point(Point p) {
            x = p.x;
            y = p.y;
        }
        /**
         * !!!WARNING!!! This breaks the usability of this class with Collections,
         * because the hashCode() method is not written in a suitable way!
         *
         * Returns <code>true</code> when the given object
         * is a point which is very close or equal to
         * this point.
         *
         * @param	o	the object which should be tested
         *			for equality.
         * @return	<code>true</code> when the given object
         *		is a point nearby this point.
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof Point) {
                Point p = (Point) o;
                return isEqual(p.x, x) && isEqual(p.y, y);
            }
            else
                return false;
        }
        
        /**
         * !!!WARNING!!! This breaks the usability of this class with Collections,
         * because the equals() method is not written in a suitable way!
         */
        @Override
        public int hashCode() {
            return (int) (x+y);
        }
        /**
         * Translate the point by the given vector.
         *
         * @param	dx	the translation for the
         *			x-coordinate.
         * @param	dy	the translation for the
         *			y-coordinate.
         */
        public void translateBy(double dx, double dy) {
            x += dx;
            y += dy;
        }
        /**
         * Multiplies the coordinates of this point
         * with the given factor.
         *
         * @param	factor	the factor by which the coordinates
         *			are to be multiplied.
         */
        public void scaleWith(double factor) {
            x *= factor;
            y *= factor;
        }
        
        /**
    	 * Rotate the point by the given angle 
    	 *@param	angle the angle in degrees
    	 */
    	public void rotate(double angle){
    		//angle in radians
    		double alpha=angle*2.0*Math.PI/360.0;
    		double x_new=x*Math.cos(alpha)-y*Math.sin(alpha);
    		double y_new=x*Math.sin(alpha)+y*Math.cos(alpha);
    		x=x_new;
    		y=y_new;
    	}

        /**
         * Returns the radius of the polar coordinates of this point.
         *
         * @return	the radius of the polar coordinates of this point.
         */
        public double getRadius() {
            return Math.sqrt(x * x + y * y);
        }
        /**
         * Returns the angle of the polar coordinates of this point.
         *
         * @return	the angle of the polar coordinates of this point.
         */
        public double getAngle() {
            return Math.atan2(y, x);
        }
        /**
         * Returns the distance between this point and <code>p</code>.
         *
         * @param	p	a point.
         * @return	the distance between this point
         *		and <code>p</code>.
         */
        public double distanceTo(Point p) {
            double dx = x - p.x;
            double dy = y - p.y;
            return Math.sqrt(dx * dx + dy * dy);
        }
        /**
         * Returns a string representation of this point.
         *
         * @return	a string representation of this point.
         */
        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
        
        /**
         * Transforms this point to <code>geoling.util.sim.grain.Point</code>
         * @return <code>geoling.util.sim.grain.Point</code>
         */
    	public geoling.util.sim.grain.Point toPoint(){
    		double [] coord = new double[] {this.x, this.y};		
    		return new geoling.util.sim.grain.Point(coord);

    	}
        
    	/**
    	 * shifts this point by <code>point</code>
    	 * @param point <code>Geometry2D.Point</code>
    	 * @return the shifted point
    	 */
        public Geometry2D.Point moveLeft(Geometry2D.Point point) {
        	return new Geometry2D.Point(this.x - point.x, this.y - point.y);
        }
    }
    
    /** This class represents a 2-dimensional line. */
    public static class Line implements GeometricObject {
        /** A point on this line. */
        public Point p;
        /** The x- and y-coordinate of the direction of this line. */
        public double dx, dy;
        /**
         * Constructs a new line which goes through <code>p</code>
         * and has the direction <code>(dx,dy)</code>.
         *
         * @param	p	a point on the line.
         * @param	dx	the x-coordinate of the direction.
         * @param	dy	the y-coordinate of the direction.
         * @throws	IllegalArgumentException
         *			if <code>(dx,dy)</code> is the
         *			vector <code>(0,0)</code>.
         */
        public Line(Point p, double dx, double dy) {
            if (isEqual(dx, 0) && isEqual(dy, 0))
                throw new IllegalArgumentException("(dx,dy) must not be (0,0)");
            
            this.p = new Point(p);
            this.dx = dx;
            this.dy = dy;
        }
        /**
         * Constructs a line which goes through the given points.
         *
         * @param	p1	one point on the line.
         * @param	p2	another point on the line.
         * @throws	IllegalArgumentException
         *			if both points are equal.
         */
        public Line(Point p1, Point p2) {
            if (p1.equals(p2))
                throw new IllegalArgumentException("p1 must be different from p2");
            
            this.p = new Point(p1);
            dx = p2.x - p1.x;
            dy = p2.y - p1.y;
        }
        /**
         * Constructs a new line whose perpendicular point
         * has the given polar coordinates.
         *
         * @param	r	the radius of the polar coordinates
         *			of the perpendicular point.
         * @param	alpha	the angle of the polar coordinates
         *			of the perpendicular point.
         */
        public Line(double r, double alpha) {
            double cos = Math.cos(alpha);
            double sin = Math.sin(alpha);
            
            p = new Point(r * cos, r * sin);
            dx = -sin;
            dy = cos;
        }
        /**
         * Constructs a new line which is a copy of
         * the given line.
         *
         * @param	l	the line which is copied.
         */
        public Line(Line l) {
            p = new Point(l.p);
            dx = l.dx;
            dy = l.dy;
        }
        /**
         * !!!WARNING!!! This breaks the usability of this class with Collections,
         * because the hashCode() method is not written in a suitable way!
         *
         * Returns <code>true</code> when the given object
         * is a line and equal (or nearly equal).
         *
         * @param	o	the object which should be
         *			compared.
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof Line) {
                Line l = (Line) o;
                
                Point pd = new Point(l.dx, l.dy);
                pd.scaleWith(100.0 / pd.getRadius());
                pd.translateBy(l.p.x, l.p.y);
                
                // test whether two points with distance 100
                // on line l have at most distance eps
                // from this line
                return distance(this, l.p) < eps &&
                    distance(this, pd) < eps;
            }
            else
                return false;
        }
        /**
         * Translate this line by the given vector.
         *
         * @param	dx	the x-coordinate of the vector.
         * @param	dy	the y-coordinate of the vector.
         */
        public void translateBy(double dx, double dy) {
            p.x += dx;
            p.y += dy;
        }
        /**
         * Returns the point on this line which is closest
         * to the origin.
         *
         * @return	the perpendicular point.
         */
        public Point getPerpendicularPoint() {
            double lambda = (dx * p.y - dy * p.x) / (dx * dx + dy * dy);
            return new Point(lambda * (-dy), lambda * dx);
        }
        /**
         * Returns a string representation of this line.
         *
         * @return	a string representation of this line.
         */
        @Override
        public String toString() {
            return "(x,y) = " + p + " + lambda * " + (new Point(dx, dy));
        }
    }
    
    /** This class represents a 2-dimensional line segment. */
    public static class LineSegment implements GeometricObject,RandomSetElement {
        /** The (two) end points of the line segment. */
        public Point p1, p2;
        /**
         * Constructs a line segment with the given
         * end points.
         *
         * @param	p1	one end point.
         * @param	p2	the other end point.
         * @throws	IllegalArgumentException
         *			if both points are equal.
         */
        public LineSegment(Point p1, Point p2) {
            if (p1.equals(p2))
                throw new IllegalArgumentException("p1 is equal to p2");
            
            this.p1 = new Point(p1);
            this.p2 = new Point(p2);
        }
        /**
         * Constructs a new line segment as a copy of the
         * given line segment.
         *
         * @param	l	the line segment which is to
         *			be copied.
         */
        public LineSegment(LineSegment l) {
            p1 = new Point(l.p1);
            p2 = new Point(l.p2);
        }
        /**
         * Returns the line which contains this line segment.
         *
         * @return	the line which contains this line segment.
         */
        public Line toLine() {
            return new Line(p1, p2.x - p1.x, p2.y - p1.y);
        }
        
        /**
         * Returns the corresponding geoling.util.sim.grain.LineSegment .
         *
         * @return	the corresponding geoling.util.sim.grain.LineSegment.
         */
        public geoling.util.sim.grain.LineSegment toLineSegment() {
            return new geoling.util.sim.grain.LineSegment(this.p1.x,this.p1.y,this.p2.x,this.p2.y,true);
        }
        
		/**
         * !!!WARNING!!! This breaks the usability of this class with Collections,
         * because the hashCode() method is not written in a suitable way!
         *
         * Returns <code>true</code> when the given object
         * is a point which is very close or equal to
         * this point.
         *
         * @param	o	the object which should be tested
         *			for equality.
         * @return	<code>true</code> when the given object
         *		is a point nearby this point.
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof LineSegment) {
                LineSegment ls = (LineSegment) o;
                return ls.p1.equals(p1) && ls.p2.equals(p2);
            }
            else
                return false;
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
            if (image instanceof Graphics2D) {
                Graphics2D g = (Graphics2D) image;
                
                GeneralPath p = new GeneralPath();
                p.moveTo((float) p1.x, (float) p1.y);
                p.lineTo((float) p2.x, (float) p2.y);
                g.draw(p);
            }
//            else if (image instanceof BinaryImage) {
//                Draw2D draw = new DrawBinaryImage2D((BinaryImage) image);
//                DiscreteLineSegment.draw(draw, p1.x, p1.y, p2.x, p2.y);
//            }
            else
                throw new IllegalArgumentException("draw not yet implemented for this image type");
        }
        /**
         * !!!WARNING!!! This breaks the usability of this class with Collections,
         * because the equals() method is not written in a suitable way!
         */
        @Override
        public int hashCode() {
            return p1.hashCode()+p2.hashCode();
        }
        /**
         * Returns a string representation of this line segment.
         *
         * @return	a string representation of this line segment.
         */
        @Override
        public String toString() {
            return p1 + " - " + p2;
        }
        
        /**
         * Checks if a point is located on the LineSegment
         */
        public boolean contains(Geometry2D.Point p){
        	
        	if(this.p1.equals(p) || this.p2.equals(p))
        		return true;
        	
        	double t1 = p.x - this.p2.x;
        	double t2 = this.p1.x - this.p2.x;
        	double s1 = p.y - this.p2.y;
        	double s2 = this.p1.y - this.p2.y;
        	boolean t_true = false;
        	boolean s_true = false;
        	double t = 0;
        	double s = 0;
        	
        	if((t1==0 && t2!=0) || (t1!=0 && t2==0))
        		return false;
        	if(t1==0 && t2 ==0)
        		t_true = true;
        	if(t1!=0 && t2!=0)
        		t = t1/t2;
        	
        	if((s1==0 && s2!=0) || (s1!=0 && s2==0))
        		return false;
        	if(s1==0 && s2 ==0)
        		s_true = true;
        	if(s1!=0 && s2!=0)
        		s = s1/s2;
        	
        	
        	if(s_true && t_true)
        		return true;
        	
        	if(s_true && 0<=t && t<=1)
        		return true;
        	
        	if(t_true && 0<=s && s<=1)
        		return true;
        	
        	if(Math.abs(s-t)<eps && 0<=s && s<=1)
        		return true;
        	else
        		return false;
        }
        
        public int getDimension() {
            return 2;
        }
        
        public DoubleBox getBoundingBox(){
            return new DoubleBox(new double[]{Math.min(p1.x,p2.x),Math.min(p1.y,p2.y)},new double[]{Math.max(p1.x,p2.x),Math.max(p1.y,p2.y)});
        }
        
        public void translateBy(double[] vector){
            p1.x += vector[0];
            p2.x += vector[0];
            p1.y += vector[1];
            p2.y += vector[1];
        }
        
        /**
         * @return the length of this line segment
         * 
         */
        public double length() {        	
        	return Math.sqrt((p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y));
        }
        
        /**
         * 
         * @return returns the orientation of the line segment as a number between -pi/2 and pi/2
         */    public double getOrientation(){
        	
        	double dx=p2.x-p1.x;
        	double dy=p2.y-p1.y;
        	
        	return Math.atan(dy/dx);
        	
        }
        
         /**
     	 * Rotate the linesegment by the given angle, where p1 is fixed
     	 *@param	angle the angle in degrees
     	 */ 
         public void rotate(double angle){
         	Point end=new Geometry2D.Point(p2.x-p1.x, p2.y-p1.y);
         	
         	end.rotate(angle);
         	p2.x=p1.x+end.x;
         	p2.y=p1.y+end.y;
         
         }
         
         public void scaleBy(double factor){
        	 p1.scaleWith(factor);
        	 p2.scaleWith(factor);
         }
        
        /**
         * Shifts this line segment by <code>point</code>
         * @param point the <code>Geometry2D.Point</code>
         * @return the shifted line segment
         */
        public Geometry2D.LineSegment moveLeft(Geometry2D.Point point) {
        	Geometry2D.Point p1 = new Geometry2D.Point(this.p1.x - point.x,this.p1.y - point.y);
        	Geometry2D.Point p2 = new Geometry2D.Point(this.p2.x - point.x,this.p2.y - point.y);
        	return new Geometry2D.LineSegment(p1, p2);
        }
		/**
		 * @return the length of this line segment
		 * 
		 */
		public double getLength() {        	
			return Math.sqrt((p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y));
		}
    }
    
    
    /** This class represents a 2-dimensional convex polygon. */
    public static class ConvexPolygon implements GeometricObject {
        /** 
         * Contains all vertices of this convex polygon in
         * counter-clockwise order.
         */
        public Point[] p;
        /** The bounding box of the convex polygon. */
        private BoundingBox b;
        /**
         * Constructs a new convex polygon with the given vertices
         * in counter-clockwise order.
         *
         * @param	p	the vertices in counter-clockwise order.
         */
        public ConvexPolygon(Point[] p) {
            /* TO CHECK THE INPUT (DOES NOT REALLY WORK YET ;-)
            // make p duplicate free
            Vector vertices = new Vector();
            
            vertices.add(p[0]);
            for (int i = 1; i < p.length; i++)
            if (! p[i].equals(p[i-1]))
		    vertices.add(p[i]);
            
            if (vertices.firstElement().equals(vertices.lastElement()))
            vertices.remove(0);
            
            int i_next, i_prev;
            
            // test if p[.] determine a convex polygon (in counter-clockwise order)
            for (int i = 0; i < vertices.size(); i++) {
            i_prev = (i == 0) ? p.length-1 : i-1;
            i_next = (i == p.length-1) ? 0 : i+1;
            
            int s = side(new Line(p[i_prev], p[i]), p[i_next]);
            if (s == SIDE_RIGHT)
		    throw new IllegalArgumentException("the vertices are not in counter-clockwise order");
            else if (s == SIDE_ON) {
		    vertices.remove(i);
		    i--;
            }
            }
            
            p = new Point[vertices.size()];
            p = (Point[]) vertices.toArray(p);
            */
            // eliminate all succeeding equal vertices
            Vector<Point> v = new Vector<Point>();
            v.add(p[0]);
            for (int i = 1; i < p.length; i++)
                if (!v.lastElement().equals(p[i]))
                    v.add(p[i]);
            if (v.firstElement().equals(v.lastElement()))
                v.remove(v.size()-1);
            if (v.size() < 3)
                throw new IllegalArgumentException("polygon has less than 3 different vertices");
            this.p = (Point[]) v.toArray(new Point[v.size()]);
            //this.p = (Point[]) p.clone();
            for (int i = 0; i < this.p.length; i++)
                this.p[i] = new Point(this.p[i]);
            b = new BoundingBox(p);
        }
        /**
         * Constructs a convex polygon as a copy of the
         * given convex polygon.
         *
         * @param	poly	the convex polygon which is to
         *			be copied.
         */
        public ConvexPolygon(ConvexPolygon poly) {
            p = poly.p.clone();
            for (int i = 0; i < p.length; i++)
                p[i] = new Point(p[i]);
            b = new BoundingBox(poly.b);
        }
        /**
         * Returns <code>true</code> when the given point
         * is contained within this convex polygon.
         *
         * @return	<code>true</code> when the given point
         * 		is contained within this convex polygon.
         */
        public boolean contains(Point q) {
            if (!b.contains(q))
                return false;
            
            for (int i = 0; i < p.length; i++)
                if (side(new Line(p[i], p[(i+1 >= p.length) ? 0 : i+1]), q) == SIDE_RIGHT)
                    return false;
            
            return true;
        }
        /**
         * Returns <code>true</code> when the given convex polygon
         * is contained within this convex polygon.
         *
         * @return	<code>true</code> when the given convex polygon
         *		is contained within this convex polygon.
         */
        public boolean contains(ConvexPolygon poly) {
            if (! b.contains(poly.b))
                return false;
            
            for (int i = 0; i < poly.p.length; i++)
                if (! contains(poly.p[i]))
                    return false;
            
            return true;
        }
        
        /**
         * Returns the transformed ConvexPolytope.
         *
         * @return	the transformed ConvexPolytope.
         */
        public ConvexPolytope toPolytope(){
        	Geometry2D.Point[] vertices = this.p;
        	geoling.util.sim.grain.Point[] p = new geoling.util.sim.grain.Point[vertices.length];
        	for (int i = 0; i < p.length; i++)
        		p[i] = vertices[i].toPoint();
        	return new ConvexPolytope(p);
        }
        
        /**
         * Returns the transformed (filled/unfilled) convex polygon.
         *
         * @param	filled	specifies whether the convex polytope is to be filled.
         * @return	the transformed convex polygon.
         */

         public ConvexPolytope toPolytope(boolean filled) {

         	geoling.util.sim.grain.Point[] point = new geoling.util.sim.grain.Point[this.p.length];
         	for (int i = 0; i < p.length; i++)
         		point[i] = this.p[i].toPoint();
         	
         	return new ConvexPolytope(point, filled);
         }
        
        /**
         * Translates the polygon by the given vector.
         *
         * @param	dx	the x-coordinate of the
         *			translational vector.
         * @param	dy	the y-coordinate of the
         *			translational vector.
         */
        public void translateBy(double dx, double dy) {
            for (int i = 0; i < p.length; i++) {
                p[i].translateBy(dx, dy); }
            b = new BoundingBox(p);
        }
        /**
         * Scales the polygon with the given <code>factor</code>.
         *
         * @param	factor	the scaling factor.
         */
        public void scaleWith(double factor) {
            for (int i = 0; i < p.length; i++)
                p[i].scaleWith(factor);
        }
        /**
         * Returns the area of the convex polygon.
         *
         * @return	the area of the convex polygon.
         */
        public double getArea() {
            double a = 0.0;
            for (int i = 0; i < p.length; i++) {
                int j = (i+1 == p.length) ? 0 : i+1;
                a += (p[i].x - p[j].x) * (p[i].y + p[j].y);
            }
            return a / 2.0;
        }
        /**
         * Returns the perimeter of the convex polygon.
         *
         * @return	the perimeter of the convex polygon.
         */
        public double getPerimeter() {
            double l = 0.0;
            for (int i = 0; i < p.length; i++) {
                int j = (i+1 == p.length) ? 0 : i+1;
                double dx = p[i].x - p[j].x;
                double dy = p[i].y - p[j].y;
                l += Math.sqrt(dx * dx + dy * dy);
            }
            return l;
        }
        /**
         * Returns a string representation of this convex polygon.
         *
         * @return	a string representation of this convex polygon.
         */
        @Override
        public String toString() {
            String s = "{";
            for (int i = 0; i < p.length; i++)
                s += (i > 0 ? ", " : "") + p[i];
            return s + "}";
        }
		/**
		* Returns <code>true</code> when the given point
		* is contained within this convex polygon.
		*
		* @return	<code>true</code> when the given point
		* 		is contained within this convex polygon.
		*/
		
		public boolean containsOnBoundary(Point q) {
			for (int i = 0; i < p.length; i++)
			if (geoling.util.Utilities.onSegment(new LineSegment(p[i], p[(i+1 >= p.length) ? 0 : i+1]), q))
			    return true;
		
		    return false;
		}
    }
    
    /** This class represents a 2-dimensional circle. */
    public static class Circle implements GeometricObject {
        /** The center point of the circle. */
        public Point center;
        /** The radius of the circle. */
        public double r;
        /**
         * Constructs a new circle with the given center point
         * and radius.
         *
         * @param	center	the center point of the circle.
         * @param	r	the radius of the circle.
         */
        public Circle(Point center, double r) {
            this.center = new Point(center);
            this.r = r;
        }
        /**
         * Constructs a new circle as a copy of the given circle.
         *
         * @param	c	the circle which is to be copied.
         */
        public Circle(Circle c) {
            center = new Point(c.center);
            r = c.r;
        }
        /**
         * Returns <code>true</code> if and only if the given
         * point is contained within this circle.
         *
         * @param	p	the point to be tested.
         * @return	<code>true</code> if and only if the given
         *		point is contained within this circle.
         */
        public boolean contains(Point p) {
            double dx = p.x - center.x;
            double dy = p.y - center.y;
            return Math.sqrt(dx * dx + dy * dy) <= r * (1 + eps);
        }
        /**
         * Returns <code>true</code> if and only if the given
         * circle <code>c</code> is contained within this circle.
         *
         * @param	c	the circle to be tested.
         * @return	<code>true</code> if and only if the given
         *		circle <code>c</code> is contained within this circle.
         */
        public boolean contains(Circle c) {
            return contains(c.center) &&
		   center.distanceTo(c.center) + c.r <= r * (1 + eps);
        }
    }
    
    /** This class represents a bounding box for convex polygons. */
    private static class BoundingBox {
        /** The range for the x-coordinate. */
        private double xmin, xmax;
        /** The range for the y-coordinate. */
        private double ymin, ymax;
//        /**
//         * Constructs a new bounding box with the given ranges
//         * for the x- and y-coordinates.
//         *
//         * @param	xmin	the minimal x-coordinate.
//         * @param	ymin	the minimal y-coordinate.
//         * @param	xmax	the maximal x-coordinate.
//         * @param	ymax	the maximal y-coordinate.
//         * @throws	IllegalArgumentException
//         *			if <code>xmin > xmax</code> or
//         *			<code>ymin > ymax</code>.
//         */
//        public BoundingBox(double xmin, double ymin, double xmax, double ymax) {
//            if (xmin > xmax)
//                throw new IllegalArgumentException("xmin is greater than xmax");
//            if (ymin > ymax)
//                throw new IllegalArgumentException("ymin is greater than ymax");
//            
//            this.xmin = xmin;
//            this.xmax = xmax;
//            this.ymin = ymin;
//            this.ymax = ymax;
//        }
        /**
         * Constructs the minimal bounding box for the given points.
         *
         * @param	p	the points.
         */
        public BoundingBox(Point[] p) {
            xmin = ymin = Double.POSITIVE_INFINITY;
            xmax = ymax = Double.NEGATIVE_INFINITY;
            
            for (int i = 0; i < p.length; i++) {
                xmin = (p[i].x < xmin ? p[i].x : xmin);
                xmax = (p[i].x > xmax ? p[i].x : xmax);
                ymin = (p[i].y < ymin ? p[i].y : ymin);
                ymax = (p[i].y > ymax ? p[i].y : ymax);
            }
        }
        /**
         * Constructs a new bounding box as a copy of the
         * given bounding box.
         *
         * @param	b	the copied bounding box.
         */
        public BoundingBox(BoundingBox b) {
            this.xmin = b.xmin;
            this.xmax = b.xmax;
            this.ymin = b.ymin;
            this.ymax = b.ymax;
        }
        /**
         * Returns <code>true</code> exactly when the
         * point is contained within the bounding box.
         *
         * @param	p	the point.
         * @return	<code>true</code> exactly when the
         * 		point is contained within the bounding box.
         */
        public boolean contains(Point p) {
            return xmin <= p.x && p.x <= xmax &&
		   ymin <= p.y && p.y <= ymax;
	}
        /**
         * Returns <code>true</code> if and only if the
         * given bounding box is contained within this
         * bounding box.
         *
         * @param	b	the bounding box.
         * @return	<code>true</code> when <code>b</code>
         *		is contained within this bounding box.
         */
        public boolean contains(BoundingBox b) {
            return xmin <= b.xmin && b.xmax <= xmax &&
		   ymin <= b.ymin && b.ymax <= ymax;
	}
        /**
         * Returns <code>true</code> exactly when <code>b</code>
         * intersects with this bounding box.
         *
         * @param	b	the bounding box.
         * @return	<code>true</code> exactly when <code>b</code>
         * 		intersects with this bounding box.
         */
        public boolean intersectsWith(BoundingBox b) {
            return ((xmin <= b.xmin && b.xmin <= xmax) ||
                    (b.xmin <= xmin && xmin <= b.xmax)) &&
		   ((ymin <= b.ymin && b.ymin <= ymax) ||
		    (b.ymin <= ymin && ymin <= b.ymax));
        }
    }
    
    /** 
	 * 
	 * This class represents a 2-dimensional convex polygon. 
	 * It is used for the segmentation to 2 partitions.
	 * 
	 * */
	public static class ConvexPolygon2 extends ConvexPolygon {
	    
		
	    /** The classification variable */
	    
	    public boolean d;
	    
	    /** The index variable*/
	    
	    public int index;
	
	    /** The number of data points in this polytope */
	    
	    public int count;
	    
	    /** The generating point for this polytope*/
	    
	    public Point point;
	    
	    
	    /**
	     * Constructs a new convex polygon with the given vertices
	     * in counter-clockwise order.
	     *
	     * @param	p	the vertices in counter-clockwise order.
	     */
	    public ConvexPolygon2(Point[] p) {
	
	    	super(p);
	    	
	    }
	
	    /**
	     * Constructs a convex polygon as a copy of the
	     * given convex polygon.
	     *
	     * @param	poly	the convex polygon which is to
	     *			be copied.
	     */
	    public ConvexPolygon2(ConvexPolygon poly) {
	
	    	super(poly);
	    	
	    }
	}

	/** 
	     * 
	     * This class represents a 2-dimensional ConvexPolygon. 
	     *  This ConvexPolygon is used for segmentation into 3 
	     *  partitions, so it has a variable int d
	     *  
	     **/
	    
	    public static class ConvexPolygon3 extends ConvexPolygon {
	        
	        public int d;
	        
	        /** The index variable*/
	        
	        public int index;
	
	        /** The number of data points in this polytope */
	        
	        public int count;
	        
	        /** The generating point for this polytope*/
	        
	        public Point point;
	        
	        /**
	         * Constructs a new convex polygon with the given vertices
	         * in counter-clockwise order.
	         *
	         * @param	p	the vertices in counter-clockwise order.
	         */
	        public ConvexPolygon3(Point[] p) {
	
	        	super(p);
	        
	        }
	        
	        
	        /**
	         * Constructs a convex polygon as a copy of the
	         * given convex polygon.
	         *
	         * @param	poly	the convex polygon which is to
	         *			be copied.
	         */
	        
	        public ConvexPolygon3(ConvexPolygon poly) {
	            
	        	super(poly);
	        	
	        }
	   }

	/**
     * Returns the distance between the given point and line.
     *
     * @param	l	the line.
     * @param	p	the point.
     * @return	the distance between the given point and line.
     */
    public static double distance(Line l, Point p) {
        l = new Line(l);	// make a copy before modifying
        l.translateBy(-p.x, -p.y);
        return l.getPerpendicularPoint().getRadius();
    }
    
    /**
     * Returns an array of length 2 with the coefficients of the
     * line equations, if both lines intersect. If both lines are
     * equal, an dummy array of length 1 is returned. Otherwise,
     * i.e. the lines do not intersect, <code>null</code>
     * is returned.
     *
     * @param	l1	the first line.
     * @param	l2	the second line.
     * @return	the coefficients of the line equations or an dummy
     *		array of length one or <code>null</code>, depending
     *		on the intersection of the lines.
     */
    private static double[] intersection(Line l1, Line l2) {
        // test for equality first (concerning also numerical errors!)
        if (l1.equals(l2))
            return new double[] {1};
        
        double d = l2.dx * l1.dy - l2.dy * l1.dx;
        double d1 = (l1.p.x - l2.p.x) * l2.dy - (l1.p.y - l2.p.y) * l2.dx;
        double d2 = (l1.p.x - l2.p.x) * l1.dy - (l1.p.y - l2.p.y) * l1.dx;
        
        // intersection is exactly one point
        if (!isEqual(d,0))
            // return the coefficients for the line formulae
            return new double[] {d1 / d, d2 / d};
        // ... or infinitely many points (i.e. the lines are equal)
        else if (isEqual(d,0) && isEqual(d1,0)  && isEqual(d2,0) )
            // this return value is only a "marker"
            return new double[] {1};
        // ... or empty
        else
            return null;
    }
    
    /**
     * Returns a <code>Line</code>, a <code>Point</code> or
     * <code>null</code> depending on the type of the intersection
     * of both lines.
     *
     * @param	l1	the first line.
     * @param	l2	the second line.
     * @return	the intersection of <code>l1</code> and <code>l2</code>.
     */
    public static GeometricObject intersect(Line l1, Line l2) {
        double[] lambda = intersection(l1, l2);
        
        // intersection is empty
        if (lambda == null)
            return null;
        // ... or the lines itself
        else if (lambda.length == 1)
            return new Line(l1);
        // ... or exactly one point
        else
            return new Point(l1.p.x + lambda[0] * l1.dx, l1.p.y + lambda[0] * l1.dy);
    }
    
    /**
     * Returns a <code>LineSegment</code>, a <code>Point</code> or
     * <code>null</code> depending on the type of the intersection
     * of the line and the line segment.
     *
     * @param	l	the line.
     * @param	s	the line segment.
     * @return	the intersection of <code>l</code> and <code>s</code>.
     */
    public static GeometricObject intersect(Line l, LineSegment s) {
        Line ls = s.toLine();
        double[] lambda = intersection(l, ls);
        
        // intersection is empty
        if (lambda == null)
            return null;
        // ... or the line segment
        else if (lambda.length == 1)
            return new LineSegment(s);
        // ... or a point outside the line segment
        // (r is the length of the line segment)
        double r = Math.sqrt(ls.dx * ls.dx + ls.dy * ls.dy);
        if (r * lambda[1] < -eps || r * lambda[1] > r + eps)
            return null;
        // ... or a point on the line segment
        else
            return new Point(l.p.x + lambda[0] * l.dx, l.p.y + lambda[0] * l.dy);
    }
    
    /**
     * Returns the intersection of the line segments <code>s1</code>
     * and <code>s2</code>
     * The returned object is a <code>LineSegment</code>, a <code>Point</code>
     * or <code>null</code> depending on the type of the intersection
     * of the line segments.
     *
     * @param	s1	one line segment.
     * @param	s2	the other line segment.
     * @return	the intersection of the line segments <code>s1</code>
     *		and <code>s2</code>.
     */
    public static GeometricObject intersect(LineSegment s1, LineSegment s2) {
        Line ls1 = s1.toLine();
        Line ls2 = s2.toLine();
        double[] lambda = intersection(ls1, ls2);
        
        // intersection is empty
        if (lambda == null)
            return null;
        // ... or both lines are equal
        else if (lambda.length == 1) {
            double l1 = (ls2.p.x - ls1.p.x) / ls1.dx;
            double l2 = ((ls2.p.x + ls2.dx) - ls1.p.x) / ls1.dx;
            // sort such that l1 <= l2
            if (l1 > l2) {
                double tmp = l1; l1 = l2; l2 = tmp;
            }
            // the segments do not overlap?
            if (l2 < eps || l1 > 1.0 + eps)
                return null;
            
            // correct the values such that the interval is contained in [0,1]
            if (l2 > 1.0) l2 = 1.0;
            if (l1 < 0.0) l1 = 0.0;
            
            double x1 = ls1.p.x + l1 * ls1.dx;
            double y1 = ls1.p.y + l1 * ls1.dy;
            double x2 = ls1.p.x + l2 * ls1.dx;
            double y2 = ls1.p.y + l2 * ls1.dy;
            
            // intersection is only one point?
            if (l1 + eps >= l2)
                return new Point(x1, y1);
            else
                return new LineSegment(new Point(x1, y1), new Point(x2, y2));
        }
        // ... or an intersection point outside of both line segments
        if (lambda[0] < -eps || lambda[0] > 1.0 + eps
            || lambda[1] < -eps || lambda[1] > 1.0 + eps)
            return null;
        // ... or a point on the line segments
        else
            return new Point(ls1.p.x + lambda[0] * ls1.dx, ls1.p.y + lambda[0] * ls1.dy);
    }
    
    /** Indicates that the point is on the line. */
    public static final int SIDE_ON = 0;
    /** Indicates that the point is on the left side of the line. */
    public static final int SIDE_LEFT = 1;
    /** Indicates that the point is on the right side of the line. */
    public static final int SIDE_RIGHT = 2;
    
    /**
     * Determines on which side (relative to the directional
     * vector <code>(dx,dy)</code>) of the given line
     * the point <code>p</code> lies.
     *
     * @param	l	the line.
     * @param	p	the point.
     * @return	<code>SIDE_ON</code>, <code>SIDE_LEFT</code> or
     *		<code>SIDE_RIGHT</code> depending on which
     *		side of the line the point lies.
     */
    public static int side(Line l, Point p) {
        if (isEqual(distance(l, p), 0))
            return SIDE_ON;
        
        // third component of the (3-dimensional) cross product
        double z = l.dx * (p.y - l.p.y) - l.dy * (p.x - l.p.x);
        
        return (z > 0) ? SIDE_LEFT :
	       (z < 0) ? SIDE_RIGHT :
	    		 SIDE_ON;	// this case should not occur
    }
    
    /**
     * Splits a convex polygons into one or to polygons
     * through the intersection with the given line.
     *
     * @param	poly	the convex polygon.
     * @param	l	the line.
     * @return	an array of length one or two depending
     *		on whether the line splits the convex polygon
     *		into two polygons or not.
     */
    public static ConvexPolygon[] split(ConvexPolygon poly, Line l) {
        /*
         * state = 0: before the first intersection (with the line l)
         * state = 1: after the first intersection (with the line l)
         * state = 2: after the second intersection (with the line l)
         */
        int state = 0;
        
        // poly1 is constructed in states 0 and 2
        Vector<Point> poly1 = new Vector<Point>();
        // poly2 is constructed in state 1
        Vector<Point> poly2 = new Vector<Point>();
        
        // intersect all edges of poly with l
        for (int i = 0; i < poly.p.length; i++) {
            int j = (i+1 >= poly.p.length) ? 0 : i+1;
            LineSegment s = new LineSegment(poly.p[i], poly.p[j]);
            GeometricObject o = intersect(l, s);
            
            // if this edge does not intersect with l
            if (o == null) {
                //System.out.println("null");
                // add p[j] to the proper polygon
                if (state == 1)
                    poly2.add(poly.p[j]);
                else
                    poly1.add(poly.p[j]);
            }
            // ... or if the intersection is the whole edge
            else if (o instanceof LineSegment) {
                //System.out.println(o);
                // then we can not reach state 1 and 2
                poly1.add(poly.p[j]);
            }
            // ... or if the intersection is a point
            else if (o instanceof Point) {
                //System.out.println(o);
                Point p = (Point) o;
                // if the point is the second vertex of the edge
                if (p.equals(poly.p[j])) {
                    if (state == 1) {
                        poly2.add(poly.p[j]);
                        poly1.add(poly.p[j]);
                        state++;
                    }
                    else if (state == 0) {
                        poly1.add(poly.p[j]);
                        // test whether the polygon is splitted
                        // or the line only touches the polygon
                        if (i+2 < poly.p.length) {
                            int s1 = side(l, poly.p[i]);
                            int s2 = side(l, poly.p[i+2]);
                            // the line only splits the polygon
                            // when the previous and next vertex lie
                            // on different sides of the line
                            if (s1 != s2 && s1 != SIDE_ON && s2 != SIDE_ON) {
                                poly2.add(poly.p[j]);  // PROBLEM FIXED: previously i+2 instead of j
                                state++;
                            }
                        }
                    }
                    else
                        throw new RuntimeException("illegal state");
                }
                // ... if the point is not the other vertex of the edge
                else if (! p.equals(poly.p[i])) {
                    poly1.add(p);
                    poly2.add(p);
                    if (state == 1)
                        poly1.add(poly.p[j]);
                    else if (state == 0)
                        poly2.add(poly.p[j]);
                    else
                        throw new RuntimeException("illegal state");
                    state++;
                }
                // ... else if the point is the second vertex of the edge
                else {
                    if (state == 1)
                        poly2.add(poly.p[j]);
                    else
                        poly1.add(poly.p[j]);
                }
            }
            else
                throw new RuntimeException("unknown dynamic type of the intersection result");
        }
        
        // after splitting the state must be 0 or 2
        // (depending whether the polygon was splitted or not)
        if (state != 0 && state != 2) {
            System.out.println(l+" "+poly);
            throw new RuntimeException("illegal state: " + state);
	    }
        
        // finally convert the vertex lists into convex polygons
        ConvexPolygon[] result = new ConvexPolygon[(state == 0) ? 1 : 2];
        
        // convert the first convex polygon
        Point[] points = new Point[poly1.size()];
        for (int i = 0; i < points.length; i++)
            points[i] = (Point) poly1.get(i);
        result[0] = new ConvexPolygon(points);
        
        // convert the second convex polygon
        if (state == 2) {
            points = new Point[poly2.size()];
            for (int i = 0; i < points.length; i++)
                points[i] = (Point) poly2.get(i);
            result[1] = new ConvexPolygon(points);
        }
        
        return result;
    }
    
    /**
     * Returns the convex polygon which is the intersection of the
     * given convex polygon with the halfplane on the left side
     * (regarding the directional vector <code>(dx,dy)</code>) of
     * the given line.
     *
     * @param	poly	the convex polygon.
     * @param	l	the line.
     * @return	the result of the intersection which is a convex
     *		polygon or <code>null</code> if the intersection
     *		is empty.
     */
    public static ConvexPolygon intersectWithHalfplane(ConvexPolygon poly, Line l) {
        ConvexPolygon[] polys = split(poly, l);
        
        ConvexPolygon res = null;
        for (int i = 0; i < polys.length; i++) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            
            // compute the minimum and maximum of the
            // third coordinate of the cross product
            for (int j = 0; j < polys[i].p.length; j++) {
                double z = l.dx * (polys[i].p[j].y - l.p.y) - l.dy * (polys[i].p[j].x - l.p.x);
                
                min = (z < min ? z : min);
                max = (z > max ? z : max);
            }
            
            // ... and choose the (absolute) greater of both
            double z = (Math.abs(min) > Math.abs(max) ? min : max);
            
            // and if it is positive, the polygon polys[i]
            // is on the left side of l
            if (z > eps) {
                res = polys[i];
                break;
            }
        }
        
        return res;
    }
    
    /**
     * Returns the intersection of the given convex polygons.
     *
     * @param	poly1	the first convex polygon.
     * @param	poly2	the second convex polygon.
     * @return	the result of the intersection which is a
     *		convex polygon or <code>null</code> if the intersection
     *		is empty.
     */
    public static ConvexPolygon intersect(ConvexPolygon poly1, ConvexPolygon poly2) {
        // if the bounding boxes do not intersect,
        // the intersection must be empty
        if (! poly1.b.intersectsWith(poly2.b))
            return null;
        
        // succesively intersect poly1 with all halfplanes
        // induced by polygon poly2
        for (int i = 0; poly1 != null && i < poly2.p.length; i++) {
            int j = (i+1 == poly2.p.length) ? 0 : i+1;
            poly1 = intersectWithHalfplane(poly1, new Line(poly2.p[i], poly2.p[j]));
        }
        
        return poly1;
    }
    
    /**
     * Returns the bisector of the given two points, i.e.
     * the line which is perpendicular to the line segment
     * connecting both points and has equal distance from both
     * points.
     * <i>It is guaranteed that <code>p1</code> lies on the
     * left side of the bisector (relative to the directional
     * vector <code>(dx,dy)</code> of the bisector) and
     * therefore <code>p2</code> must lie on the right side.</i>
     *
     * @param	p1	one point.
     * @param	p2	the other point.
     * @return	the bisector of <code>p1</code> and <code>p2</code>.
     * @throws	IllegalArgumentException
     *			if both points are equal.
     */
    public static Line bisector(Point p1, Point p2) {
        if (p1.equals(p2))
            throw new IllegalArgumentException("p1 must not equal p2");
        
        double xm = (p1.x + p2.x) / 2.0;
        double ym = (p1.y + p2.y) / 2.0;
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        
        return new Line(new Point(xm, ym), new Point(xm - dy, ym + dx));
    }
    
    /**
     * Returns the bisecting line of the angle formed by the given lines.
     * If both lines are parallel, the line in the middle of both lines
     * is returned. Otherwise, if there are two possibilities, the
     * returned is determines by the directions of the lines.
     *
     * @param	l1	one line.
     * @param	l2	the other line.
     * @return	the bisecting line of the given lines.
     */
    public static Line bisectingLine(Line l1, Line l2) {
        GeometricObject o = intersect(l1, l2);
        
        if (o instanceof Point) {
            Point p = (Point) o;
            
            double dx1 = l1.dx, dy1 = l1.dy;
            double len1 = Math.sqrt(dx1*dx1 + dy1*dy1);
            dx1 /= len1; dy1 /= len1;
            
            double dx2 = l2.dx, dy2 = l2.dy;
            double len2 = Math.sqrt(dx2*dx2 + dy2*dy2);
            dx2 /= len2; dy2 /= len2;
            
            double dx = dx1 + dx2, dy = dy1 + dy2;
            
            return new Line(p, dx, dy);
        }
        else if (o instanceof Line)
            return (Line) o;
        else {
            Line l = new Line(l1.p, -l1.dy, l1.dx);
            
            Point p1 = (Point) intersect(l, l1);
            Point p2 = (Point) intersect(l, l2);
            
            Point p = new Point((p1.x+p2.x)/2.0, (p1.y+p2.y)/2.0);
            
            return new Line(p, l1.dx, l1.dy);
        }
    }
    
    /**
     * Returns the center of the cirumcircle of the triangle defined by the
     * given points p1, p2, and p3.
     *
     * @param	p1	on point of the triangle.
     * @param	p2	another point of the triangle.
     * @param	p3	the third point of the triangle.
     * @return	the center of the cirumcircle of the triangle defined by the
     *		given points p1, p2, and p3.
     */
    public static Point cirumCircleCenter(Point p1, Point p2, Point p3) {
        Line l1 = bisector(p1, p2);
        Line l2 = bisector(p1, p3);
        GeometricObject o = intersect(l1, l2);
        if (o instanceof Point)
            return (Point) o;
        else
            return null;
    }

	/**
	 * Diese Methode schneidet ein ConvexPolytope mit einem LineSegment.
	 * Es liefert entweder null, Geometry2D.Point oder Geometry2D.LineSegment
	 * zurueck.
	 * 
	 * @param con	ConvexPolygon
	 * @param ls	LineSegment
	 * 
	 * @return the intersection
	 */
	
	static public Geometry2D.GeometricObject intersect(ConvexPolygon con, LineSegment ls){
		Point[] points = con.p;
		int len = points.length;
		Point p1 = new Point(ls.p1);
		Point p2 = new Point(ls.p2);
		Geometry2D.LineSegment[] lines = new Geometry2D.LineSegment[len];
		for(int i = 0; i < len-1; i++){
			lines[i]= new Geometry2D.LineSegment(
					new Geometry2D.Point(points[i]),
					new Geometry2D.Point(points[i+1])
					);
		}
		lines[len-1]= new Geometry2D.LineSegment(
				new Geometry2D.Point(points[len-1]),
				new Geometry2D.Point(points[0])
				);
		
		//Jetzt kommen 4 Faelle:
		//1: p1 und p2 in con
		//2: nur p1 ist in con
		//3: nur p2 ist in con
		//4: weder p1 noch p2 ist in con
		if ( con.contains(p1)&& con.contains(p2)){
			return ls;
		}
		if (con.contains(p1)){
			Geometry2D.GeometricObject go;
			boolean onlyOnePoint = false;
			for (int i = 0; i< len; i++){
				go = Geometry2D.intersect(lines[i], ls);
				if (go instanceof Geometry2D.Point){
					Geometry2D.Point p = (Geometry2D.Point) go;
					if (!p.equals(ls.p1)){
						return new Geometry2D.LineSegment(p, ls.p1);
					} else {
						onlyOnePoint = true;
					}
				}
				if (go instanceof Geometry2D.LineSegment){
					Geometry2D.LineSegment p = (Geometry2D.LineSegment) go;
					return new Geometry2D.LineSegment(p);
				}
			}
			if (onlyOnePoint){
				return ls.p1;
			}
		}
		if (con.contains(p2)){
			Geometry2D.GeometricObject go;
			boolean onlyOnePoint = false;
			for (int i = 0; i< len; i++){
				go = Geometry2D.intersect(lines[i], ls);
				if (go instanceof Geometry2D.Point){
					Geometry2D.Point p = (Geometry2D.Point) go;
					if (!p.equals(ls.p2)){
						return new Geometry2D.LineSegment(p, ls.p2);
					} else {
						onlyOnePoint = true;
					}
				}
				if (go instanceof Geometry2D.LineSegment){
					Geometry2D.LineSegment p = (Geometry2D.LineSegment) go;
					return new Geometry2D.LineSegment(p);
				}
			}
			if (onlyOnePoint){
				return ls.p2;
			}
		} 
		else {
			Vector<Geometry2D.Point> go = new Vector<Geometry2D.Point>();
			Geometry2D.GeometricObject go1;
			for (int i = 0; i< len; i++){
				go1 = Geometry2D.intersect(lines[i], ls);
				if (go1 instanceof Geometry2D.LineSegment){
					return go1;
				}
				if (go1 instanceof Geometry2D.Point){
					if(go.size() ==0){
						go.add(new Geometry2D.Point((Geometry2D.Point)go1));
						
					}else if(go.size() ==1){
						if (!go.elementAt(0).equals(go1)){
							go.add(new Geometry2D.Point((Geometry2D.Point)go1));
							return new Geometry2D.LineSegment((Geometry2D.Point)go.elementAt(0), (Geometry2D.Point)go.elementAt(1));
						}
					}
				}
			}
			return null;
		}
		System.out.println("intersect(con, ls):Hier sollte ich gar nie nicht sein!!");
		
		
		return null;
	}
}
