package geoling.util.sim.grain;



import geoling.util.DoubleBox;






import geoling.util.sim.util.RandomSetElement;

import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;



/**

* This class implements a real object which represents a

* (possibly filled) rectangle.

*

* @author  Institute of Stochastics, Ulm University

* @version 1.1, 2003-12-28

*/

public final class Rectangle implements RandomSetElement {



    /** The coordinates of the center of the rectangle. */

    private double x, y;

    /** The width of the rectangle. */

    private double width;

    /** The height of the rectangle. */

    private double height;

    /** The rotational angle of the rectangle. */

    private double angle;

    /** Indicates whether the rectangle is filled or not. */

    private boolean filled = false;

    /** The coordinates of two vertices of the rectangle. */

    private double x1, y1, x2, y2;

	

	

    /**

    * Constructs a new rectangle with the given

    * <code>width</code>, <code>height</code> and rotational

    * angle <code>angle</code>.

    * This rectangle is not filled per default.

    *

    * @param	x	the x-coordinate of the center of the rectangle.

    * @param	y	the y-coordinate of the center of the rectangle.

    * @param	width	the width of the rectangle.

    * @param	height	the height of the rectangle.

    * @param	angle	the rotational angle of the rectangle.

    */

    public Rectangle(double x, double y, double width, double height, double angle) {

	this(x, y, width, height, angle, false);

    }



    /**

    * Constructs a new rectangle with the given

    * <code>width</code>, <code>height</code> and rotational

    * angle <code>angle</code>.

    * This rectangle is filled iff <code>filled</code>

    * is <code>true</code>.

    *

    * @param	x	the x-coordinate of the center of the rectangle.

    * @param	y	the y-coordinate of the center of the rectangle.

    * @param	width	the width of the rectangle.

    * @param	height	the height of the rectangle.

    * @param	angle	the rotational angle of the rectangle.

    * @param	filled	indicates whether the rectangle is filled or not.

    */

    public Rectangle(double x, double y, double width, double height, double angle, boolean filled) {

		this.x = x;

		this.y = y;

		this.width = width;

		this.height = height;

		this.angle = angle;

		this.filled = filled;

	

		double p = 0.9;	// experimentally good value ;-)

		double diag = StrictMath.sqrt((width-p) * (width-p) + (height-p) * (height-p)) / 2.0;

		double alpha = StrictMath.atan2(height-p, width-p);

	

		x1 = diag * StrictMath.cos(angle + alpha);

		y1 = diag * StrictMath.sin(angle + alpha);

		x2 = diag * StrictMath.cos(angle - alpha);

		y2 = diag * StrictMath.sin(angle - alpha);

    }



    public Point getCenter() {

		return new Point(new double[] {x, y});

    }



    public double getWidth() {

		return width;

    }



    public double getHeight() {

		return height;

    }



    public double getAngle() {

		return angle;

    }



    public boolean isFilled() {

		return filled;

    }



    /**

    * Returns the dimension of the rectangle, i.e. the value 2.

    *

    * @return	the value 2.

    */

    public int getDimension() {

		return 2;

    }



    /**

    * Returns the bounding box of the rectangle.

    *

    * @return	the bounding box of the rectangle.

    */

    public DoubleBox getBoundingBox() {

		double xmin = min(new double[] {x-x1, x-x2, x+x1, x+x2});

		double xmax = max(new double[] {x-x1, x-x2, x+x1, x+x2});

		double ymin = min(new double[] {y-y1, y-y2, y+y1, y+y2});

		double ymax = max(new double[] {y-y1, y-y2, y+y1, y+y2});

	

		return new DoubleBox(new double[] {xmin, ymin}, new double[] {xmax, ymax});

    }



    /**

    * Translates the rectangle by the given vector.

    *

    * @param	vector	the vector by which the rectangle is to be translated.

    * @throws	IllegalArgumentException

    *			if the length of <code>vector</code>

    *			is not equal to 2.

    */

    public void translateBy(double[] vector) {

		if (vector.length != 2)

		    throw new IllegalArgumentException("vector must have length 2");

	

		x += vector[0];

		y += vector[1];

    }



    /**

    * Draws the rectangle in the given image.

    *

    * @param	image	the image in which the rectangle is to be drawn.

    * @throws	IllegalArgumentException

    *			if drawing is not yet implemented for

    *			this image type or dimension.

    */

    public void draw(Object image) {

        if (image instanceof Graphics2D && getDimension() == 2) {

	    Graphics2D g = (Graphics2D) image;



	    GeneralPath p = new GeneralPath();

	    p.moveTo((float) (x + x1), (float) (y + y1));

	    p.lineTo((float) (x - x2), (float) (y - y2));

	    p.lineTo((float) (x - x1), (float) (y - y1));

	    p.lineTo((float) (x + x2), (float) (y + y2));

	    p.closePath();

																			 

	    if (filled)

		g.fill(p);

	    else

		g.draw(p);

	}

	else

	    throw new IllegalArgumentException("draw not yet implemented for this image type");

    }

	

	public double[][] getVertices(){

		double[][] vertices = new double[4][];



		vertices[0] = new double[] {x + x1, y + y1};

		vertices[1] = new double[] {x - x2, y - y2};

		vertices[2] = new double[] {x - x1, y - y1};

		vertices[3] = new double[] {x + x2, y + y2};

		

		return vertices;



	}
	
	/**

    * Returns the maximal element of the array <code>a</code>.

    *

    * @param	a	the array whose maximum is to be computed.

    * @return	the maximal element of the array <code>a</code>.

    */

    private static double max(double[] a) {

	double curMax = Double.NEGATIVE_INFINITY;



	for (int i = 0; i < a.length; i++)

	    curMax = (a[i] > curMax ? a[i] : curMax);



	return curMax;

    }



    /**

    * Returns the minimal element of the array <code>a</code>.

    *

    * @param	a	the array whose minimum is to be computed.

    * @return	the minimal element of the array <code>a</code>.

    */

    private static double min(double[] a) {

	double curMin = Double.POSITIVE_INFINITY;



	for (int i = 0; i < a.length; i++)

	    curMin = (a[i] < curMin ? a[i] : curMin);



	return curMin;

    }

}

