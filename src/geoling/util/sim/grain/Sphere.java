package geoling.util.sim.grain;



import geoling.util.DoubleBox;





import geoling.util.sim.util.RandomSetElement;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;



/**

* This class implements a real object which represents a

* (possibly filled) sphere.

*

* @author  Institute of Stochastics, Ulm University

* @version 1.0, 2001-09-28

*/

public final class Sphere implements RandomSetElement {



    /** The coordinates of the center point of the sphere. */

    private double[] center;

    /** The radius of the sphere. */

    private double r;

    /** Indicates whether the sphere is filled or not. */

    private boolean filled = false;



    /**

    * Constructs a new sphere with the given center

    * point <code>center</code> and radius <code>r</code>.

    * This sphere is not filled per default.

    *

    * @param	center	the center point.

    * @param	r	the radius.

    * @throws	IllegalArgumentException

    *			if the dimension of <code>center</code>

    *			is not positive.

    */

    public Sphere(double[] center, double r) {

	if (center.length < 1)

	    throw new IllegalArgumentException("the dimension of the sphere must be positive");



	this.center = (double[]) center.clone();

	this.r = r;

    }



    /**

    * Constructs a new sphere with the given center

    * point <code>center</code> and radius <code>r</code>.

    * This sphere is filled iff <code>filled</code>

    * is <code>true</code>.

    *

    * @param	center	the center point.

    * @param	r	the radius.

    * @param	filled	indicates whether the sphere is filled or not.

    * @throws	IllegalArgumentException

    *			if the dimension of <code>center</code>

    *			is not positive.

    */

    public Sphere(double[] center, double r, boolean filled) {

	this(center, r);

	this.filled = filled;

    }



    /**

    * Returns the dimension of the sphere.

    *

    * @return	the dimension of the sphere.

    */

    public int getDimension() {

	return center.length;

    }



    /**

    * Returns the bounding box of the sphere.

    *

    * @return	the bounding box of the sphere.

    */

    public DoubleBox getBoundingBox() {

	double[] min = (double[]) center.clone();

	double[] max = (double[]) center.clone();



	for (int i = 0; i < min.length; i++) {

	    min[i] -= r;

	    max[i] += r;

	}



	return new DoubleBox(min, max);

    }



    public boolean isFilled() {

	return filled;

    }



    /**

    * Returns a copy of the center of the sphere.

    *

    * @return	a copy of the center of the sphere.

    */

    public double[] getCenter() {

	return (double[]) center.clone();

    }



    /**

    * Returns the radius of the sphere.

    *

    * @return	the radius of the sphere.

    */

    public double getRadius() {

	return r;

    }



    /**

    * Translates the sphere by the given vector.

    *

    * @param	vector	the vector by which the sphere is to be translated.

    */

    public void translateBy(double[] vector) {

	if (vector.length != center.length)

	    throw new IllegalArgumentException("vector and sphere do not have the same dimension");



	for (int i = 0; i < center.length; i++)

	    center[i] += vector[i];

    }



    /**

    * Draws the sphere in the given image.

    *

    * @param	image	the image in which the sphere is to be drawn.

    * @throws	IllegalArgumentException

    *			if drawing is not yet implemented for

    *			this image type or dimension.

    */

    public void draw(Object image) {

	if (image instanceof Graphics2D && getDimension() == 2) {

	    Graphics2D g = (Graphics2D) image;



	    if (filled)

		g.fill(new Ellipse2D.Double(center[0]-r, center[1]-r, 2*r, 2*r));

	    else

		g.draw(new Ellipse2D.Double(center[0]-r, center[1]-r, 2*r, 2*r));

	}

	else

	    throw new IllegalArgumentException("draw not yet implemented for this image type");

    }



}

