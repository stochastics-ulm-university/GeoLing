package geoling.util.sim.grain;

import java.util.Vector;

import geoling.util.DoubleBox;
import geoling.util.Utilities;
import geoling.util.geom.Geometry2D;
import geoling.util.sim.util.RandomSet;
import geoling.util.sim.util.RandomSetElement;

import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

/**
* This class implements a convex polytope which may be an
* element of a random set or a sampling window for a random set.
*
* @author  Institute of Stochastics, Ulm University
* @version 1.0.1, 2013-02-22
*/
public final class ConvexPolytope implements RandomSetElement, Cloneable {

    /** The vertices of the convex polytope. */
    private Point[] vertices;
    /** The dimension of the convex polytope. */
    private int dimension;
    /** The bounding box of the convex polytope. */
    private DoubleBox boundingBox;
    /** Indicates whether the convex polytope should be filled or not. */
    private boolean filled = false;

    /**
    * Constructs a new convex polytopes with the given vertices
    * (in counter-clockwise order in the 2-dimensional case).
    *
    * @param	vertices	the vertices of the convex polytope.
    */
    public ConvexPolytope(Point[] vertices) {
	dimension = vertices[0].getDimension();
	for (int i = 1; i < vertices.length; i++)
	    if (dimension != vertices[i].getDimension())
		throw new IllegalArgumentException("all vertices must have the same dimension");

	this.vertices = (Point[]) vertices.clone();
	for (int i = 0; i < vertices.length; i++)
	    this.vertices[i] = (Point) vertices[i].clone();

	double[] min = new double[dimension];
	double[] max = new double[dimension];

	for (int i = 0; i < min.length; i++) {
	    min[i] = Double.POSITIVE_INFINITY;
	    max[i] = Double.NEGATIVE_INFINITY;
	}

	double[] vertex;
	for (int i = 0; i < vertices.length; i++) {
	    vertex = vertices[i].getCoordinates();
	    for (int j = 0; j < dimension; j++) {
		min[j] = Math.min(min[j], vertex[j]);
		max[j] = Math.max(max[j], vertex[j]);
	    }
	}

	boundingBox = new DoubleBox(min, max);
    }

    /**
    * Constructs a possibly filled convex polytope with the given
    * vertices.
    *
    * @param	vertices	the vertices of the convex polytope.
    * @param	filled		is <code>true</code> when the convex polytope
    *				should be filled.
    */
    public ConvexPolytope(Point[] vertices, boolean filled) {
	this(vertices);
	this.filled = filled;
    }

    /**
    * Returns a copy of this convex polytope.
    *
    * @return	a copy of this convex polytope.
    */
    public Object clone() {
        ConvexPolytope p;

	try {
	    p = (ConvexPolytope) super.clone();
	}
	catch (CloneNotSupportedException e) {
	    throw new InternalError(e.toString());
	}

	p.boundingBox = (DoubleBox) boundingBox.clone();
	p.vertices = (Point[]) vertices.clone();
	for (int i = 0; i < vertices.length; i++)
	    p.vertices[i] = (Point) vertices[i].clone();

	return p;
    }

    /**
    * Returns the vertices of the convex polytope. If this
    * polytope has dimension 2, the vertices must be in counterclockwise
    * order.
    *
    * @return	the vertices of the convex polytope.
    */
    public Point[] getVertices() {
	Point[] tmpVertices = (Point[]) vertices.clone();

	for (int i = 0; i < tmpVertices.length; i++)
	    tmpVertices[i] = (Point) vertices[i].clone();

	return tmpVertices;
    }

    /**
    * Returns the number of vertices.
    *
    * @return	the number of vertices.
    */
    public int getNumberOfVertices() {
	return vertices.length;
    }

    /**
    * Returns the dimension of the convex polytope.
    *
    * @return	the dimension of the convex polytope.
    */
    public int getDimension() {
        return dimension;
    }

    public boolean isFilled() {
	return filled;
    }

    /**
    * Returns the bounding box of the convex polytope.
    *
    * @return	the bounding box of the convex polytope.
    */
    public DoubleBox getBoundingBox() {
	return (DoubleBox) boundingBox.clone();
    }

    /**
    * Translates the convex polytope by the given vector.
    *
    * @param	vector	the vector by which the convex polytope is
    *			to be translated.
    * @throws	IllegalArgumentException
    *			if <code>vector</code> has not the
    *			same dimension as the convex polytope.
    */
    public void translateBy(double[] vector) {
	if (dimension != vector.length)
	    throw new IllegalArgumentException("vector must have the same dimension as the convex polytope");

	for (int i = 0; i < vertices.length; i++)
	    vertices[i].translateBy(vector);

	boundingBox.translateBy(vector);
    }

    /**
    * Draws the convex polytope in to the given image.
    *
    * @param	image	the image in which the convex polytope is to be drawn.
    * @throws    IllegalArgumentException
    *                   if drawing is not yet implemented for
    *                   this image type or dimension.
    */
    public void draw(Object image) {
	if (image instanceof Graphics2D && getDimension() == 2) {
	    Graphics2D g = (Graphics2D) image;

	    GeneralPath p = new GeneralPath();

	    double[] coord;
	    for (int i = 0; i < vertices.length; i++) {
		coord = vertices[i].getCoordinates();
		if (i == 0)
		    p.moveTo((float) coord[0], (float) coord[1]);
		else
		    p.lineTo((float) coord[0], (float) coord[1]);
	    }

	    p.closePath();

	    if (filled)
		g.fill(p);
	    else
		g.draw(p);
	}
	else
	    throw new IllegalArgumentException("draw not yet implemented for this image type");
    }

    /**
    * Returns a string representation of this convex polytope
    * for debugging purposes.
    *
    * @return	a string representation of this convex polytope.
    */
    public String toString() {
	String s = "ConvexPolytope(\n";

	for (int i = 0; i < vertices.length; i++)
	    s = s + (i > 0 ? ",\n" : "") + "  " + vertices[i];

	return s + "\n)";
    }
    /**
     * Returns a string representation of this convex polytope
     * without any breaks.
     *
     * @return	a string representation of this convex polytope without any breaks.
     */
     public String toStringnoBreaks() {
    	 String s = "ConvexPolytope(";
    	 for (int i = 0; i < vertices.length; i++)
    		 s = s + (i > 0 ? "," : "") + " " + vertices[i];
    	 return s + ")";
     }
    /**
    * Converts this convex polytope into a convex polygon.
    *
    * @return	the convex polygon.
    * @throws	IllegalArgumentException
    *			if the dimension of this convex
    *			polytope is not 2.
    */
    public Geometry2D.ConvexPolygon toPolygon() {
	if (getDimension() != 2)
	    throw new IllegalArgumentException("this convex polytope must have dimension 2");

	Geometry2D.Point[] p = new Geometry2D.Point[vertices.length];
	for (int i = 0; i < vertices.length; i++) {
	    double[] c = vertices[i].getCoordinates();
	    p[i] = new Geometry2D.Point(c[0], c[1]);
	}

	return new Geometry2D.ConvexPolygon(p);
    }

    /**
    * Returns the area of this convex polygon. Therefore,
    * the dimension of this convex polytope must be 2.
    *
    * @return	the area of this convex polygon.
    * @throws	IllegalArgumentException
    *			if the dimension of this convex
    *			polytope is not 2.
    */
    public double getArea() {
	if (getDimension() != 2)
	    throw new IllegalArgumentException("this convex polytope must have dimension 2");

	return toPolygon().getArea();
    }                                   

    /**
    * Returns the circumference of this convex polygon. Therefore,
    * the dimension of this convex polytope must be 2.
    *
    * @return	the circumference of this convex polygon.
    * @throws	IllegalArgumentException
    *			if the dimension of this convex
    *			polytope is not 2.
    */
    public double getCircumference() {
	if (getDimension() != 2)
	    throw new IllegalArgumentException("this convex polytope must have dimension 2");

	return toPolygon().getPerimeter();
    }
    /** Returns the edges of this polygon as a RandomSet
     *
     *@return RandomSet containing the edges
     */
    public RandomSet getEdges()
    {
        RandomSet rs = new RandomSet(this.boundingBox);
        Geometry2D.Point x,y;
        
        for(int i=0;i<vertices.length;i++)
        {
            x = Utilities.transform(vertices[i]);
            y = Utilities.transform(vertices[(i+1)%vertices.length]);
            Geometry2D.LineSegment g2dls = new Geometry2D.LineSegment(x,y);
            LineSegment ls = Utilities.transform(g2dls);
            rs.add(ls);
        }
        return rs;
    }
    
    /**
    * Tests wheter the given point is within this convex polytope.
    * This algorithm is currently only implemented for the 2-dimensional
    * case.
    *
    * @param	p	the point which should be tested for containment
    *			in the convex polytope.
    * @return	<code>true</code> when <code>p</code> is contained within
    *		the convex polytope.
    * @throws	IllegalArgumentException
    *			if the dimension of the point differs from that
    *			of the convex polytope or the dimension of the
    *			point is not equal to 2.
    */
    public boolean contains(Point p) {
	if (p.getDimension() != getDimension())
	    throw new IllegalArgumentException("p must have the same dimension as this polytope");
	if (p.getDimension() != 2)
	    throw new IllegalArgumentException("method currently implemented only for dimension 2");

	double[] c = p.getCoordinates();

	return toPolygon().contains(new Geometry2D.Point(c[0], c[1]));
    }
    
   
    
//    /**
//     * Tests whether the given polytope "poly" is contained in this rectangle.
//     * Not applicable for arbitrary convex sampling windows.
//     * 
//     */
//    public boolean containsPol(ConvexPolytope poly) {
//    	Geometry2D.Point pointU = this.getUpRight().toGeo2DPoint();
//    	Geometry2D.Point pointD = this.getDownLeft().toGeo2DPoint();
//    	for(int i=0;i< poly.vertices.length;i++){
//    		Geometry2D.Point p = poly.vertices[i].toGeo2DPoint();
//    		if(!(p.x<=pointU.x && p.y<=pointU.y ))
//    			return false;
//    	}
//    	return true;
//        }
//    
//    
//    /**
//     * Tests whether the given polytope "poly" is at least partially contained in this rectangle.
//     * Not applicable for arbitrary convex sampling windows.
//     * 
//     */
//    public boolean containsPolPart(ConvexPolytope poly) {
//    	Geometry2D.Point pointU = this.getUpRight().toGeo2DPoint();
//    	for(int i=0;i< poly.vertices.length;i++){
//    		Geometry2D.Point p = poly.vertices[i].toGeo2DPoint();
//    		if((p.x<=pointU.x && p.y<=pointU.y ))
//    			return true;
//    	}
//    	return false;
//        }
//    
//    
//    /**
//     * Tests whether the given point is contained in this rectangle.
//     * Not applicable for arbitrary convex sampling windows.
//     * 
//     */
//    public boolean containsPoint(Point point) {
//    	Geometry2D.Point pointU = this.getUpRight().toGeo2DPoint();
//    	Geometry2D.Point p = point.toGeo2DPoint();
//    		
//    	if(p.x<=pointU.x && p.y<=pointU.y )
//    			return true;
//    	
//    	return false;
//        }
    
    /** Returns the center of gravity of this 2D-polygon
     *
     *@return the coordinates of the center of gravity
     */    
    public double[] getCenterOfGravity()
    {
    	Geometry2D.Point p = getCenterofGravity();
    	return new double[] { p.x, p.y };
    }
    
   /** Returns the edges of this 2D-polygon
    *
    *@return the edges of this polygon as Geometry2D.LineSegment in a vector
    */
    public Vector<Geometry2D.LineSegment> getEdgesGeo2D()
    {
        Vector<Geometry2D.LineSegment> vec = new Vector<Geometry2D.LineSegment>();
        Geometry2D.Point x,y;
        
        for(int i=0;i<vertices.length;i++)
        {
            x = Utilities.transform(vertices[i]);
            y = Utilities.transform(vertices[(i+1)%vertices.length]);
            Geometry2D.LineSegment ls = new Geometry2D.LineSegment(x,y);
            
            vec.add(ls);
        }
        return vec;
    }
    
    
    /** Returns the center of gravity of this 2D-polygon
    *
    *@return the center of gravity as Geometry2D.Point
    */
    public Geometry2D.Point getCenterofGravity() {
        double A = getArea();
        
        double sum_x = 0, sum_y = 0;
        for(int i=0; i<vertices.length;i++) {
        	int j = (i+1 == vertices.length) ? 0 : i+1;
        	double[] p = vertices[i].getCoordinates();
        	double[] q = vertices[j].getCoordinates();
        	sum_x += (p[0] + q[0]) * (p[0]*q[1] - q[0]*p[1]);
        	sum_y += (p[1] + q[1]) * (p[0]*q[1] - q[0]*p[1]);
        }
        
        return new Geometry2D.Point(sum_x/(6*A), sum_y/(6*A));
    }
    
    
    
    /** Returns the vertices of this 2D-polygon
    *
    *@return the vertices of this polygon as Geometry2D.Point in a vector
    */
    public Vector<Geometry2D.Point> getVerticesGeo2D()
    {
        Vector<Geometry2D.Point> edges = new Vector<Geometry2D.Point>();
        Geometry2D.Point p;
        
        for(int i=0;i<vertices.length;i++)
        {
            p = Utilities.transform(vertices[i]);
            
            
            edges.add(p);
        }
        return edges;
    }
    
    
    /**
     * 
     * @return the down left vertex of the bounding box of this polygon
     */
    public Geometry2D.Point getMin() {
    	double[] min= boundingBox.getMin();
    	if(min.length!=2) throw new IllegalArgumentException("length must be 2");
    	
    	return new Geometry2D.Point(min[0],min[1]);
    }
    
    /**
     * 
     * @return the right upper vertex of the bounding box of this polygon
     */
    public Geometry2D.Point getMax() {
    	double[] max= boundingBox.getMax();
    	if(max.length!=2) throw new IllegalArgumentException("length must be 2");
    	
    	return new Geometry2D.Point(max[0],max[1]);
    }
    
    
    /**
     * Checks if two ConvexPolytopes are the identical.
     * 
     * @param convpoly		the Convpoly to compare with
     * @return 				true if the ConvexPolytopes are identical, else false
     */
    
    public boolean equals(ConvexPolytope convpoly) {
	    Vector<Geometry2D.LineSegment> edges1 = convpoly.getEdgesGeo2D();
    	Vector<Geometry2D.LineSegment> edges2 = this.getEdgesGeo2D();
	    
    	for(int i=0;i<edges1.size();i++){
    		Geometry2D.LineSegment ls = (Geometry2D.LineSegment) edges1.elementAt(i);
    		if(!edges2.contains(ls))
    			return false; 
    	}
    	
    	for(int i=0;i<edges1.size();i++){
    		Geometry2D.LineSegment ls = (Geometry2D.LineSegment) edges1.elementAt(i);
    		if(!edges2.contains(ls))
    			return false; 
    	}
    	for(int i=0;i<edges2.size();i++){
    		Geometry2D.LineSegment ls = (Geometry2D.LineSegment) edges2.elementAt(i);
    		if(!edges1.contains(ls))
    			return false; 
    	}
    	
    	return true;
    }
    
    
    public void stretch(double scale){
		
    	for(int s=0;s<this.vertices.length;s++)
			this.vertices[s].stretch(scale);
    	
    	double[] min = this.getBoundingBox().getMin();
    	double[] max = this.getBoundingBox().getMax();
    	
    	double[] min_new = new double[]{min[0]*scale, min[1]*scale};
    	double[] max_new = new double[]{max[0]*scale, max[1]*scale};
    	
    	this.boundingBox = new DoubleBox(min_new, max_new);
    }
    
}
