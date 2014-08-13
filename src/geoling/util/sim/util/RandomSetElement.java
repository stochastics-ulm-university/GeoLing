package geoling.util.sim.util;
import geoling.util.DoubleBox;
/**
* General interface for all elements of a random set.
*
* @author  Institute of Stochastics, Ulm University
* @version 1.0, 2001-09-26
*/
public interface RandomSetElement {
    /**
    * Returns the dimension of the element.
    *
    * @return	the dimension of the element.
    */
    public int getDimension();
    /**
    * Returns the bounding box of the element.
    *
    * @return	the bounding box of the element.
    */
    public DoubleBox getBoundingBox();
    /**
    * Translates the element by the given vector.
    *
    * @param	vector	the vector by which the element is to be translated.
    * @throws	IllegalArgumentException
    *			if the dimension and the dimension of the
    *			vector are not equal.
    */
    public void translateBy(double[] vector);
    /**
    * Draws the element in the given image.
    *
    * @param	image	the image in which the element
    *			is to be drawn.
    * @throws    IllegalArgumentException
    *                   if drawing is not yet implemented for
    *                   this image type or dimension.
    */
    public void draw(Object image);
}
