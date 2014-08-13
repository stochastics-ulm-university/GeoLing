package geoling.util.geom;

import geoling.util.Utilities;

/**
 * This class represents a 3D Plane.
 * The points of the plane are given by <code>n*x = d</code>, where <code>n</code>
 * is the unit normal vector of the plane and <code>d</code> the (positive) distance to
 * the origin.
 * 
 * @author Institute of Stochastics, Ulm University
 * @version 1.2.3, 09. 09. 2013
 */
public class Plane3D implements Geometry3D {
	
	/** The distance from the origin, this is always positive. */
	private double distance;
	
	/** The (direction of the) unit normal vector of the plane. */
	private Point3D normal;
	
	/** Two vectors which span a plane parallel to this one, calculated only on demand. */
	private Point3D vector1 = null;
	private Point3D vector2 = null;
	
	/** The point on the plane with the minimum distance to the origin, calculated only on demand. */
	private Point3D pointMinDist = null;
	
	/** 
	 * Constructs a 3D Plane. The location of the plane is determined by <code>distance</code>,
	 * the minimal distance of the plane from the origin.
	 * 
	 * @param distance	the distance of the plane from the origin
	 * @param normal	The (direction of the) unit normal vector	
	 */
	public Plane3D(double distance, Point3D normal) {
		this.distance = distance;
		this.normal	= normal;
		
		//Check if the unit vector is normalized; if not, calculate the normal vector
		if(this.normal.getLength() != 1) {
			this.normal = this.normal.norm();
		}
		
		//Check if the distance is positive
		if(distance<0) {
			throw new IllegalArgumentException("Cant work with negative distance!");
		}
	}
	
	/** 
	 * Constructs a 3D Plane. The location of the plane is determined by three points
	 * which may not be collinear.
	 * 
	 * @param p1  a point on the plane
	 * @param p2  another point on the plane
	 * @param p3  another point on the plane such that the three points 
	 *            are not collinear
	 */
	public Plane3D(Point3D p1, Point3D p2, Point3D p3) {
		this.normal   = p2.getVectorTo(p1).getVectorProduct(p3.getVectorTo(p1)).norm();
		distance = normal.getScalarProduct(p1);
		
		if (Double.isNaN(distance)) {
			throw new IllegalArgumentException("Points may not be collinear!");
		}
		if (distance < 0) {
			normal = normal.reflectOrigin();
			distance = -distance;
		}
	}
	
	/** 
	 * Checks if a Plane is close or equal to another one.
	 *
	 * @param geom 	the Geometry3D which the object should be compared to
	 * @return <code>true</code> if the other plane is close or equal
	 */
	@Override
	public boolean isSimilar(Geometry3D geom) {
		if (geom instanceof Plane3D) {
			Plane3D s = (Plane3D) geom;
			if (Utilities.isEqual(distance, s.getDistance())) {
				if (normal.isSimilar(s.getNormalVec())) {
					return true;
				} else if (Utilities.isEqual(distance, 0.0) && normal.isSimilar(s.getNormalVec().reflectOrigin())) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Reflects the plane at the origin without changing the original plane.
	 * @return the resulting reflected plane
	 */    
	public Plane3D reflectOrigin() {		
		return new Plane3D(distance, normal.reflectOrigin());
	}
	
	/**
	 * Translates the plane by a vector <code>translateVector</code> without changing the
	 * original plane.
	 * @param translateVector the point or vector by which the object shall be translated
	 * @return the resulting translated plane
	 */
	@Override
	public Plane3D translateBy(Point3D translateVector) {
		double additionalDist = normal.getScalarProduct(translateVector);
		double newDist = distance + additionalDist;
		Point3D newDir;
		if (newDist < 0) {
			newDir = normal.reflectOrigin();
			newDist = -newDist;
		} else {
			newDir = normal;
		}
		return new Plane3D(newDist, newDir);
	}
	
	/**
	 * Scales the plane by a factor.
	 * 
	 * @param scale  the scale for this operation
	 * @return the scaled object
	 */
	@Override
	public Plane3D scaleBy(double scale) {
		double newDist = distance * scale;
		Point3D newDir;
		if (newDist < 0) {
			newDir = normal.reflectOrigin();
			newDist = -newDist;
		} else {
			newDir = normal;
		}
		return new Plane3D(newDist, newDir);
	}
	
	/**
	 * Checks if the plane is parallel or nearly parallel to another plane.
	 * @param plane the plane for which the parallelity to this plane shall be
	 * checked
	 * @return <code>true</code> if the plane is parallel or nearly parallel to
	 * this plane
	 */
	public boolean isParallel(Plane3D plane) {
		return normal.isSimilar(plane.getNormalVec());
	}
	
	/**
	 * Returns the distance of the plane from the origin.      
	 * @return <code>Distance</code>, the distance of the plane from the origin. 
	 */
	public double getDistance(){
		return distance;
	}
	
	/**
	 * Returns the normal unit vector of the plane.      
	 * @return <code>normal</code>, the normal unit vector of the plane.
	 */
	public Point3D getNormalVec(){
		return normal;
	}
	
	/**
	 * Returns the first span vector of the plane.      
	 * @return the first span vector of the plane.
	 */
	public Point3D getVector1(){
		if(vector1 == null)
			calcParallelVectors();
		return vector1;
	}
	
	/**
	 * Returns the second span vector of the plane.      
	 * @return the second span vector of the plane.
	 */
	public Point3D getVector2(){
		if(vector2 == null)
			calcParallelVectors();
		return vector2;
	}   
	
	/**
	 * Returns the point on the plane with the minimum distance to the origin.      
	 * @return <code>PointMinDist</code> the point on the plane with the
	 *  minimum distance to the origin.
	 */
	public Point3D getMinDistPoint(){
		if(pointMinDist == null)
			calcParallelVectors();
		return pointMinDist;
	}
	
	/**
	 * Returns a string representation of this plane for debugging purposes.
	 * @return a string representation of this plane
	 */
	@Override
	public String toString() {
		return " 3D Plane with distance from the origin : " + distance + " and unit normal vector : " + normal;
	}
	
	/**
	 * From the <code>normal</code>, which is the unit normal vector of the plane, one 		 
	 * calculates two vectors, which are orthogonal to <code>normal</code> as well as
	 *  to each other. These two vectors will span the plane.
	 *  
	 *  Additionally <code>pointMinDist</code> is calculated, the point with the minimal
	 *  distance to the origin.
	 *
	 */
	private void calcParallelVectors() {
		double[] normalCoordinates	= normal.getCoordinates();
		
		//The point on the plane which has minimal distance from the origin
		pointMinDist = new Point3D(new double[]{distance*normalCoordinates[0], distance*normalCoordinates[1],distance*normalCoordinates[2]});
		
		//A vector helping to calculate the two span-vectors of the plane
		Point3D Helper = new Point3D(new double[]{normalCoordinates[0]*(1/3)+1,
		                                          normalCoordinates[1]*(1/2)+1,
		                                          normalCoordinates[2]*(1/4)+1});
		double[] HelperCoordinates	= Helper.getCoordinates();
		
		//Getting direction of the first span-vector using the crossproduct
		vector1 = new Point3D(new double[]{HelperCoordinates[1]*normalCoordinates[2]-HelperCoordinates[2]*normalCoordinates[1],
		                                   HelperCoordinates[2]*normalCoordinates[0]-HelperCoordinates[0]*normalCoordinates[2],
		                                   HelperCoordinates[0]*normalCoordinates[1]-HelperCoordinates[1]*normalCoordinates[0]});
		
		double[] DirCoordinates1	= vector1.getCoordinates();
		
		//Getting direction of the second span-vector using the crossproduct
		vector2 = new Point3D(new double[]{DirCoordinates1[1]*normalCoordinates[2]-DirCoordinates1[2]*normalCoordinates[1],
		                                   DirCoordinates1[2]*normalCoordinates[0]-DirCoordinates1[0]*normalCoordinates[2],
		                                   DirCoordinates1[0]*normalCoordinates[1]-DirCoordinates1[1]*normalCoordinates[0]});
	}
}
