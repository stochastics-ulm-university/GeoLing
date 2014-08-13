package geoling.util.sim.util.plot;

import geoling.util.sim.grain.ConvexPolytope;
import geoling.util.sim.grain.LineSegment;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.grain.Rectangle;
import geoling.util.sim.grain.Sphere;
import geoling.util.sim.util.RandomSet;
import geoling.util.sim.util.RandomSetElement;
import geoling.util.DoubleBox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.PrintStream;

/**
 * A class for wrapping geometric objects such that they can be drawn
 * into a 2D image.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class DrawableRandomSetElement2D implements DrawableObject2D {
	
	/** The geometric object that is to be drawn. */
	private RandomSetElement obj;
	
	/** The line width to use, if applicable. */
	private double lineWidth;
	
	/** The color to use, if applicable. */
	private Color color;
	
	/**
	 * Constructs a new drawable object for a given geometric object.
	 * 
	 * @param obj       the geometric object that is to be drawn
	 * @param lineWidth the line width to use
	 * @param color     the color to use
	 */
	public DrawableRandomSetElement2D(RandomSetElement obj, double lineWidth, Color color) {
		this.obj       = obj;
		this.lineWidth = lineWidth;
		this.color     = color;
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
			
			out.setStroke(new BasicStroke((float)lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
			out.setColor(color);
			
			draw(out, obj, graphics.getBox());
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
			eps.printLineWidth(lineWidth);
			eps.printColor(color);
			
			PrintStream p = eps.getStream();
			drawEPS(p, obj, eps.getBox());
		}
	}
	
	/**
	 * Draws the given geometric object to a <code>Graphics2D</code> object.
	 * 
	 * @param out  the <code>Graphics2D</code> object
	 * @param obj  the object to draw
	 * @param box  the bounding box, which may be used to draw unbounded objects
	 */
	private void draw(Graphics2D out, RandomSetElement obj, DoubleBox box) {
		if (obj instanceof RandomSet) {
			draw(out, obj, box);
//		} else if (obj instanceof Line) {
//			Geometry2D.GeometricObject intersection = Geometry2D.intersect(Utilities.transform((Line)obj), box);
//			if (intersection instanceof Geometry2D.Point) {
//				draw(out, Utilities.transform((Geometry2D.Point)intersection), box);
//			} else if (intersection instanceof Geometry2D.LineSegment) {
//				draw(out, Utilities.transform((Geometry2D.LineSegment)intersection), box);
//			}
		} else {
			obj.draw(out);
		}
	}
	
	/**
	 * Draws the given geometric object to an EPS file.
	 * 
	 * @param p    the EPS output stream
	 * @param obj  the object to draw
	 * @param box  the bounding box, which may be used to draw unbounded objects
	 */
	private void drawEPS(PrintStream p, RandomSetElement obj, DoubleBox box) {
		if (obj instanceof RandomSet) {
			drawEPS(p, obj, box);
		} else if (obj instanceof ConvexPolytope) {
			ConvexPolytope c = (ConvexPolytope)obj;
			drawPolygonEPS(p, c);
			if (c.isFilled())
				p.println("fill");
			else
				p.println("stroke");
		} else if (obj instanceof Polytope) {
			Polytope c = (Polytope)obj;
			drawPolytopeEPS(p, c);
			if (c.isFilled())
				p.println("fill");
			else
				p.println("stroke");
//		} else if (obj instanceof PolygonalChain2D) {
//			PolygonalChain2D c = (PolygonalChain2D)obj;
//			drawPolygonalChain2DEPS(p, c);
//			p.println("stroke");
		} else if (obj instanceof LineSegment) {
			LineSegment s = (LineSegment)obj;
			drawLineSegmentEPS(p, s);
			p.println("stroke");
		} else if (obj instanceof Point) {
			Point pt = (Point)obj;
			drawPointEPS(p, pt, this.lineWidth);
			p.println("fill");
		} else if (obj instanceof Sphere) {
			Sphere s = (Sphere)obj;
			drawDiscEPS(p, s);
			if (s.isFilled())
				p.println("fill");
			else
				p.println("stroke");
		} else if (obj instanceof Rectangle) {
			Rectangle rect = (Rectangle)obj;
			p.println("gsave");
			drawRectangleEPS(p, rect);
			if (rect.isFilled())
				p.println("fill");
			else
				p.println("stroke");
			p.println("grestore");
//		} else if (obj instanceof Line) {
//			Geometry2D.GeometricObject intersection = Geometry2D.intersect(Utilities.transform((Line)obj), box);
//			if (intersection instanceof Geometry2D.Point) {
//				drawEPS(p, Utilities.transform((Geometry2D.Point)intersection), box);
//			} else if (intersection instanceof Geometry2D.LineSegment) {
//				drawEPS(p, Utilities.transform((Geometry2D.LineSegment)intersection), box);
//			}
		} else {
			throw new RuntimeException("Geometric object of type "+obj.getClass().getName()+": EPS plot not implemented!");
		}
	}
	
	/**
	 * Draws a convex polygon c into p.
	 *
	 * @param c  the convex polygon to be drawn
	 */
	private static void drawPolygonEPS(PrintStream p, ConvexPolytope c) {
		Point[] pt = c.getVertices();
		
		p.println("newpath");
		
		double[] coord;
		
		coord = pt[0].getCoordinates();
		p.println(" "+coord[0]+" "+coord[1]+" moveto");
		
		for (int i = 1; i < pt.length; i++) {
			coord = pt[i].getCoordinates();
			p.println(" "+coord[0]+" "+coord[1]+" lineto");
		}
		
		p.println("closepath");
	}
	
//	/**
//	 * Draws a 2-dimensional polygonal chain into p.
//	 * 
//	 * @param c  the polygonal chain to be drawn
//	 */
//	private static void drawPolygonalChain2DEPS(PrintStream p, PolygonalChain2D c) {
//		Geometry2D.Point[] vertices = c.getVertices();
//		p.println("newpath");
//		p.println(" " + vertices[0].x + " " + vertices[0].y + " moveto");
//		for (int i = 1; i < vertices.length; i++) {
//			p.println(" " + vertices[i].x + " " + vertices[i].y + " lineto");
//		}
//	}
	
	/**
	 * Draws a polatope c into p.
	 *
	 * @param c  the polytope to be drawn
	 */
	private static void drawPolytopeEPS(PrintStream p, Polytope c) {
		Point[] pt = c.getVertices();
		
		p.println("newpath");
		
		double[] coord;
		
		coord = pt[0].getCoordinates();
		p.println(" "+coord[0]+" "+coord[1]+" moveto");
		
		for (int i = 1; i < pt.length; i++) {
			coord = pt[i].getCoordinates();
			p.println(" "+coord[0]+" "+coord[1]+" lineto");
		}
		
		p.println("closepath");
	}
	
	/**
	 * Draws the line segment s into p.
	 *
	 * @param s  the line segment to be drawn into p
	 */
	private static void drawLineSegmentEPS(PrintStream p, LineSegment s) {
		p.println("newpath");
		
		double[] coord;
		
		coord = s.getStartPoint().getCoordinates();
		p.println(" "+coord[0]+" "+coord[1]+" moveto");
		
		coord = s.getEndPoint().getCoordinates();
		p.println(" "+coord[0]+" "+coord[1]+" lineto");
	}
	
	/**
	 * Draws the point pt into p.
	 *
	 * @param pt  the point to be drawn
	 */
	private static void drawPointEPS(PrintStream p, Point pt, double pointRadius) {
		double[] coord = pt.getCoordinates();
		
		p.println("newpath");
		p.println(coord[0]+" "+coord[1]+" "+pointRadius+" 0 360 arc");
	}
	
	/**
	 * Draws the disc s into p.
	 *
	 * @param s  the disc to be drawn
	 */
	private static void drawDiscEPS(PrintStream p, Sphere s) {
		double[] coord = s.getCenter();
		double r = s.getRadius();
		
		p.println("newpath");
		p.println(coord[0]+" "+coord[1]+" "+r+" 0 360 arc");
	}
	
	/**
	 * Draws the rectangle r into p.
	 *
	 * @param r  the rectangle to be drawn
	 */
	private static void drawRectangleEPS(PrintStream p, Rectangle r) {
		double[] coord = r.getCenter().getCoordinates();
		double width = r.getWidth();
		double height = r.getHeight();
		double angle = r.getAngle() / Math.PI * 180.0;
		
		p.println(coord[0]+" "+coord[1]+" translate");
		p.println(angle+" rotate");
		p.println("newpath");
		p.println(" "+(-width/2.0)+" "+(-height/2.0)+" moveto");
		p.println(" "+width+" 0 rlineto");
		p.println(" 0 "+height+" rlineto");
		p.println(" "+(-width)+" 0 rlineto");
		p.println("closepath");
	}
	
}
