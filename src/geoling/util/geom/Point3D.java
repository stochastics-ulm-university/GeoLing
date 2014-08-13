package geoling.util.geom;

import geoling.util.Utilities;

/**
 * This class represents a 3D point. This class is immutable, that means that it
 * can't be changed after its construction.
 * 
 * @author Institute of Stochastics, Ulm University
 * @version 0.95, 13. 11. 2012
 */
public class Point3D implements Geometry3D {
	
	/** A constant for the point located at the origin. */
	public static final Point3D ORIGIN = new Point3D(0.0, 0.0, 0.0);
	
	/** the coordinates defining this point */
	private final double[] coordinates;
	
	/**
	 * Constructs a new point with the coordinates given by
	 * <code>coordinates</code>.
	 * @param coordinates the coordinates defining the point
	 */
	public Point3D(double[] coordinates) {
		this.coordinates = coordinates.clone();
	}
	
	/**
	 * Constructs a new point with the coordinates given by
	 * <code>(coord1,coord2,coord3)</code>.
	 * @param coord1 the first coordinate of the point
	 * @param coord2 the second coordinate of the point
	 * @param coord3 the third coordinate of the point
	 */
	public Point3D(double coord1, double coord2, double coord3) {
		this.coordinates = new double[] { coord1, coord2, coord3 };
	}
	
	/**
	 * Constructs a new point with the same coordinates as the given point.
	 * @param point the point which will be copied to define this point
	 */
	public Point3D(Point3D point) {
		this.coordinates = point.getCoordinates();
	}
	
	/**
	 * Checks if the point is very close or equal to <code>geom</code>.
	 * @param geom the Geometry3D which shall be compared to the point
	 * @return <code>true</code> if the Geometry3D <code>geom</code> is
	 *         very close or equal to the point
	 */
	@Override
	public boolean isSimilar(Geometry3D geom) {
		if (geom instanceof Point3D) {
			double[] coordsO = ((Point3D) geom).getCoordinates();
			for (int i = 0; i < 3; i++) {
				if ((coordsO[i] != coordinates[i]) && !Utilities.isEqual(coordsO[i], coordinates[i])) 
					return false;
			}
			return true;
		} else
			return false;
	}
	
	/**
	 * Reflects the point at the origin without changing the original point.
	 * @return the resulting reflected point
	 */
	public Point3D reflectOrigin() {
		return new Point3D(new double[]{-coordinates[0], -coordinates[1],
				-coordinates[2]});
	}
	
	/**
	 * Applies a real scale to the point without changing the original point.
	 * @param scale the scale for this operation
	 * @return the scaled point
	 */
	public Point3D scaleBy(double scale) {
		return new Point3D(new double[]{scale * coordinates[0],
				scale * coordinates[1],
				scale * coordinates[2]});
	}
	
	/**
	 * Scales the point a three-dimensional scale in each direction without
	 * changing the original point.
	 * @param scale the 3D scale for this operation
	 * @return the resulting scaled point
	 */
	public Point3D scaleBy(double[] scale) {
		return new Point3D(new double[]{coordinates[0] * scale[0],
				coordinates[1] * scale[1],
				coordinates[2] * scale[2]});
	}
	
	/**
	 * Translates the point by a point <code>translateVector</code> without
	 * changing the original point.
	 * @param translateVector the point or vector by which the object shall be
	 * translated
	 * @return the resulting translated point
	 */
	@Override
	public Point3D translateBy(Point3D translateVector) {
		double[] newCoords = translateVector.getCoordinates().clone();
		for (int i = 0; i < 3; i++) {
			newCoords[i] += coordinates[i];
		}
		return new Point3D(newCoords);
	}
	
	/**
	 * Returns the coordinates of the point
	 * @return the coordinates of the point
	 */
	public double[] getCoordinates() {
		return coordinates.clone();
	}
	
	/**
	 * Gets the vector from a point <code>fromPoint</code> to this point.
	 * @param fromPoint the other point
	 * @return the resulting vector as a point
	 */
	public Point3D getVectorTo(Point3D fromPoint){
		return this.translateBy(fromPoint.reflectOrigin());
	}
	
	/**
	 * Calculates the vector product of a given vector and this point
	 * @param vector the given vector represented by a point
	 * @return the vector product represented by a point
	 */
	public Point3D getVectorProduct(Point3D vector){
		double[]coordinates2=vector.getCoordinates();
		double[]coordinates3=new double[3];
		for (int i=0;i<3;i++){
			coordinates3[i]= coordinates[(i+1)%3]*coordinates2[(i+2)%3]
					-coordinates[(i+2)%3]*coordinates2[(i+1)%3];
		}
		return new Point3D(coordinates3);
	}
	
	/**
	 * Calculates the scalar product between a given vector and this point
	 * @param vector the given vector represented by a point
	 * @return the scalar product as a double
	 */
	public double getScalarProduct(Point3D vector){
		double[]coordinates2=vector.getCoordinates();
		double result=0;
		for (int i=0;i<3;i++){
			result+=coordinates[i]*coordinates2[i];
		}
		return result;
	}
	/**
	 * Get the length of the vector.
	 * @return the length as double
	 */
	public double getLength() {
		return Math.sqrt(coordinates[0] * coordinates[0]
		                 + coordinates[1] * coordinates[1]
		                 + coordinates[2] * coordinates[2]);
	}
	/**
	 * Norms the point without changing the original point.
	 * @return the normed point
	 */
	public Point3D norm() {
		double norm = this.getLength();
		return new Point3D(new double[]{ coordinates[0] / norm,
		                                 coordinates[1] / norm,
		                                 coordinates[2] / norm }); 
	}
	
	/**
	 * Returns a string representation of this point for debugging purposes.
	 * @return a string representation of this point
	 */
	@Override
	public String toString() {
		return coordinates[0] + ", " + coordinates[1] + ", " + coordinates[2];
	}
	
}
