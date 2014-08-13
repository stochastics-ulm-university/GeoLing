package geoling.util.sim.grain;

import geoling.util.DoubleBox;
import geoling.util.Utilities;
import geoling.util.geom.Geometry2D;
import geoling.util.sim.util.RandomSet;
import geoling.util.sim.util.RandomSetElement;

import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.util.*;

/**
* This class implements a polytope which may be an 
* element of a random set or a sampling window for a random set.
* 
* @author  	Institute of Stochastics, Ulm University
* @version 	1.0.2,  2014-06-24
*/
public class Polytope implements RandomSetElement, Cloneable {

	/** The vertices of the polytope. */
	private Point[] vertices;
	/** The dimension of the polytope. */
	private int dimension;
    	/** The bounding box of the polytope. */
    	private DoubleBox boundingBox;
    	/** Indicates whether the polytope should be filled or not. */
    	private boolean filled = false;
	/** The edges of the polytope. */
	//private RandomSet edges;

	final static double EPS = 0.0000000001f;
        
        

	/**
    	* Constructs a new polytope with the given vertices
    	* (in counter-clockwise order in the 2-dimensional case).
    	*
    	* @param	vertices	the vertices of the polytope.
    	*/
	public Polytope(Point[] vertices) {
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
   	* Constructs a possibly filled polytope with the given vertices.
    	*
    	* @param	vertices	the vertices of the polytope.
    	* @param	filled		is <code>true</code> when the polytope
    	*				should be filled.
    	*/
	public Polytope(Point[] vertices, boolean filled) {
		this(vertices);
		this.filled = filled;
    	}


	/**
    	* Returns a copy of this polytope.
    	*
    	* @return	a copy of this polytope.
    	*/
 	public Object clone() {
        	Polytope p;

		try {
	    	p = (Polytope) super.clone();
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
    	* Returns the vertices of the polytope. If this
    	* polytope has dimension 2, the vertices must be in counterclockwise
    	* order.
    	*
    	* @return	the vertices of the polytope.
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
    	* Returns the dimension of the polytope.
    	*
    	* @return	the dimension of the polytope.
    	*/
    	public int getDimension() {
        	return dimension;
    	}


    	public boolean isFilled() {
		return filled;
    	}


	/**
    	* Returns the bounding box of the polytope.
    	*
    	* @return	the bounding box of the polytope.
    	*/
    	public DoubleBox getBoundingBox() {
		return (DoubleBox) boundingBox.clone();
    	}
	
 	/**
    	* Translates the polytope by the given vector.
    	*
    	* @param	vector	the vector by which the polytope is
    	*			to be translated.
    	* @throws	IllegalArgumentException
    	*			if <code>vector</code> has not the
    	*			same dimension as the polytope.
    	*/
    	public void translateBy(double[] vector) {
		if (dimension != vector.length)
	    	throw new IllegalArgumentException("vector must have the same dimension as the polytope");

		for (int i = 0; i < vertices.length; i++)
	    	vertices[i].translateBy(vector);

		boundingBox.translateBy(vector);
    	}


    	/**
    	* Draws the polytope in to the given image.
    	*
    	* @param	image	the image in which the polytope is to be drawn.
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
    	* Returns a string representation of this polytope
    	* for debugging purposes.
    	*
    	* @return	a string representation of this polytope.
    	*/
    	public String toString() {
		String s = "Polytope(\n";

		for (int i = 0; i < vertices.length; i++)
	    		s = s + (i > 0 ? ",\n" : "") + "  " + vertices[i];

		return s + "\n)";
    	}
	

	/**
    	* Returns the area of this polytope. Therefore, the dimension of 
	* this polytope must be 2.
    	*
    	* @return	the area of this polygon.
    	* @throws	IllegalArgumentException
    	*			if the dimension of this polytope is not 2.
    	*/
	public double getArea() {
		if (getDimension() != 2)
			throw new IllegalArgumentException("this polytope must have dimension 2");
		// number of vertices
		int n = vertices.length;
		double area = 0.0f;
		for (int p = n - 1, q = 0; q < n; p = q++) {
			double[] coord_p = this.vertices[p].getCoordinates();
			double[] coord_q = this.vertices[q].getCoordinates();
			area += coord_p[0] * coord_q[1] - coord_q[0] * coord_p[1];
		}
		return area * 0.5f;
	}

	
	/**
	* Returns the circumference of this polytope. Therefore, the dimension of
	* this polytope must be 2.
	*
	* @return the circumference of this polytope
	* @throws IllegalArgumentException
	*		if the dimension of this polytope is not 2.
	*/
	public double getCircumference() {
		if (getDimension() != 2)
			throw new IllegalArgumentException("this polytope must have dimension 2");
		else {
			double cir = 0;
			double a_x, a_y, b_x, b_y;
			for (int i = 0; i < vertices.length; i++) {
				a_x = vertices[i].getCoordinates()[0];
				a_y = vertices[i].getCoordinates()[1];
				// neighboring vertex, in counterclockwise order
				b_x = vertices[(i + 1) % vertices.length].getCoordinates()[0];
				b_y = vertices[(i + 1) % vertices.length].getCoordinates()[1];
				cir += Math.sqrt(Math.pow(a_x - b_x,2) + Math.pow(a_y - b_y, 2));
			}
			return cir;
		}
	}

	/**
	* Method which is needed within the procedure of triangulation,
	* checks whether a diagonal can be constructed between the three vertices u, v, and w.
	*
	* @param 	vert		array of all vertices of the polygon.
	*		u, v, w		indices of the three neighboring vertices within the array 
	*				<code>vert</code>.
	*		vek		array of the indices of all vertices, ordered counterclockwise.				
	* @return 	<code>true</code> when there exists a diagonal between u, v, and w.
	*		<code>false</code> when there exists no diagonal.
	*/
	private static boolean snip(Point[] vert, int u, int v, int w, int n, int[] vek) {
		double a_x, a_y, b_x, b_y, c_x, c_y, p_x, p_y;
		// get x- and y-coordinates of the neighboring points of indices u, v, w in vert
		a_x = vert[vek[u]].getCoordinates()[0];
		a_y = vert[vek[u]].getCoordinates()[1];

		b_x = vert[vek[v]].getCoordinates()[0];
		b_y = vert[vek[v]].getCoordinates()[1];

		c_x = vert[vek[w]].getCoordinates()[0];
		c_y = vert[vek[w]].getCoordinates()[1];
		
		// if v is a concave vertex, then there exists no diagonal
		if ( EPS > ((b_x - a_x) * (c_y - a_y)) - ((b_y - a_y) * (c_x - a_x)) ) return false;

		for (int p = 0; p < n; p++) {
			if ( (p == u) || (p == v) || (p == w) ) continue;
			p_x = vert[vek[p]].getCoordinates()[0];
			p_y = vert[vek[p]].getCoordinates()[1];
			Point[] points = new Point[3];
			points[0] = new Point(new double[] {a_x, a_y});
			points[1] = new Point(new double[] {b_x, b_y});
			points[2] = new Point(new double[] {c_x, c_y});
			ConvexPolytope triangle = new ConvexPolytope(points);
			
			// if any other vertex lies within the triangle of u, v, and w, then there
			// exists no diagonal
			if ( triangle.contains(new Point(new double[] {p_x, p_y})) ) return false;
		}
		return true;

	}
	
	
	/**
	* Triangulation of (non-convex) simple polytopes in the plane
	* decomposes the polytope into triangles
	*
	* @return RandomSet containing the triangles
	* @throws IllegalArgumentException
	*		if the polygon has only 2 vertices
	*		if the polygon is not simple.
	*/
	public RandomSet triangulate() {
		RandomSet rs = new RandomSet(this.boundingBox);
		int n = this.vertices.length;

		// if less than three vertices => error, not a polygon!  
		if (n < 3) throw new IllegalArgumentException ("ERROR - only 2 vertices, nothing to do!");
		// array for the indices of the vertices of the polygon ordered counterclockwise 
		int[] vec = new int[n];

		// the vertices should be given in counterclockwise order; 
		// otherwise: change the order
		if (0.0f < this.getArea())
			for (int i = 0; i < n; i++) vec[i] = i;
		else
			for (int i = 0; i < n; i++) vec[i] = (n-1) - i;
		
		int nv = n;
		int m = 0;

		for (int v = nv - 1; nv > 2; ) {
			// three consecutive vertices in current polygon, <u,v,w>
			int u = v;
			if (nv <= u) u = 0;	// previous
			v = u + 1;
			if (nv <= v) v = 0;	// new v
			int w = v + 1;
			if (nv <= w) w = 0;	// next

			if (snip(this.vertices, u, v, w, nv, vec)) {
				int a, b, c, s, t;
			
				// true names of the vertices
				a = vec[u]; b = vec[v]; c = vec[w];
	
				// triangle consisting of the points vertices[a], vertices[b], vertices[c]
				Point[] points = new Point[3];
				points[0] = vertices[a];
				points[1] = vertices[b];
				points[2] = vertices[c];
				ConvexPolytope triangle = new ConvexPolytope(points);
				rs.add(triangle);

				// remove v from remaining polygon
				for (s = v, t = v + 1; t < nv; s++, t++) vec[s] = vec[t];
				nv --;
				m = 0;
			} else {
				m++;
				if (m >= nv) {
					throw new IllegalArgumentException ("ERROR - intersecting constraints violated?!");
				}
			}
		}
		return rs;
	}


    	/** Returns the edges of this polygon as a RandomSet
    	*
    	* @return RandomSet containing the edges
    	*/
	public RandomSet getEdges() {
           RandomSet edges = new RandomSet(this.boundingBox);
            Geometry2D.Point x, y;
            for (int i = 0; i < vertices.length; i++) {
                    x = Utilities.transform(vertices[i]);
                    // neighboring vertice, counterclockwise
                    y = Utilities.transform(vertices[(i + 1) % vertices.length]);
                    //if(!x.equals(y))
                    //{
                        Geometry2D.LineSegment g2dls = new Geometry2D.LineSegment(x, y); 

                        LineSegment ls = Utilities.transform(g2dls);
                        edges.add(ls);
                        //}  
            }            
            return edges;
	}

    /**Tests whether the given point lies within the polytope.
     *
     *@param p the point, that is to be tested
     *@return true if the point lies within the polytope, false if it does not
     */    
      public boolean contains(Point p)
      {
          double[] c = p.getCoordinates(); 
          if (!(this.boundingBox.contains(c)))
              return false;
          
          if(this.boundingBox.getMax(0)==c[0] || this.boundingBox.getMin(0) == c[0])
            return false;
          if(this.boundingBox.getMax(1)==c[1] || this.boundingBox.getMin(1) == c[1])
            return false;
          
          
          int n=0; //Anzahl Schnittpunkte
          //int m=0;
          double length = Math.max(this.boundingBox.getMax(0)-this.boundingBox.getMin(0), this.boundingBox.getMax(1)-this.boundingBox.getMin(1));  

          //anhand Konstruktion eines Liniensegments mit bel. Winkel durch den Punkt und zaehlen der Schnitte mit Kanten des Polytopes (ungerade = in, gerade = out)
          //"Strahlmethode"
          geoling.util.sim.random.Uniform u = new geoling.util.sim.random.Uniform(0, 2*Math.PI);
          Geometry2D.LineSegment ls = geoling.util.Utilities.transform(new LineSegment(c[0],c[1],2*length,u.realise())); //Anfangspunkt, Endpunkt
          
          //Geometry2D.LineSegment ls = new Geometry2D.LineSegment(new Geometry2D.Point(c[0],c[1]),new Geometry2D.Point(c[0]+2*length,c[1])); //Anfangspunkt, Endpunkt
          //Geometry2D.LineSegment ls_ =  new Geometry2D.LineSegment(new Geometry2D.Point(c[0],c[1]),new Geometry2D.Point(c[0],c[1]+2*length)); //Anfangspunkt, Endpunkt
          RandomSet edges = getEdges();        
          Vector<Geometry2D.Point> v = new Vector<Geometry2D.Point>(); //beinhaltet die Menge an Schnittpunkten
          //Vector w = new Vector();
          for(Iterator<?> it = edges.iterator();it.hasNext();)
          {
              LineSegment l = (LineSegment) it.next();
              Geometry2D.LineSegment ls2D = new Geometry2D.LineSegment(new Geometry2D.Point(((double[])((Point)l.getStartPoint()).getCoordinates())[0],((double[])((Point)l.getStartPoint()).getCoordinates())[1]),new Geometry2D.Point(((double[])((Point)l.getEndPoint()).getCoordinates())[0],((double[])((Point)l.getEndPoint()).getCoordinates())[1]));
              Geometry2D.GeometricObject o = Geometry2D.intersect(ls,ls2D);
              if(o instanceof Geometry2D.Point)
              {         
                  Geometry2D.Point q = (Geometry2D.Point)o;
                  
                  if(!v.contains(q))  
                  {
                      n = n+1;
                      v.add(q);
                  }
              }  
              /*
              Geometry2D.GeometricObject o_ = Geometry2D.intersect(ls_,ls2D);//falls Gerade durch Eckpunkt geht, wird Ergebnis falsch, daher Verwendung zweiter Gerade
              if(o_ instanceof Geometry2D.Point)
              {         
                  Geometry2D.Point q = (Geometry2D.Point)o_;
                  if(!w.contains(q))  
                  {
                      m = m+1;
                      w.add(q);
                  }
              }*/
          }
          if(n%2==0)// || m%2==0)//gerade
            return false;
          else 
              return true;
      }
    
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
