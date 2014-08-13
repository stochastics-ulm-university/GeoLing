package geoling.util.geom;

/**
 * Interface for all 3D geometric objects, provides some basic functions.
 * 
 * @author Institute of Stochastics, Ulm University
 * @version 1.2, 13. 02. 2013
 */
public interface Geometry3D {
	
	/**
	 * Translates the 3D object by a point <code>p</code> without changing the
	 * original object.
	 * @param p the point or vector by which the object shall be translated
	 * @return the resulting translated object
	 */
	Geometry3D translateBy(Point3D p);
	
	/**
	 * Scales the 3D object by a real scale without changing the original
	 * object.
	 * @param scale the scale for this operation
	 * @return the resulting scaled object
	 */
	Geometry3D scaleBy(double scale);
	
	/**
	 * Checks if the 3D object is very close or equal to <code>geom</code>. This
	 * property is checked near the origin (if necessary), i.e. two "almost
	 * parallel" lines are considered to be similar if they intersect near the
	 * origin and not similar if they intersect "far away" from the origin.
	 *
	 * @param geom the Geometry3D which shall be compared to the 3D object
	 * @return <code>true</code> if the Geometry3D <code>geom</code> is
	 *         of the same class and very close or equal to the 3D object
	 */
	boolean isSimilar(Geometry3D geom);
	
}
