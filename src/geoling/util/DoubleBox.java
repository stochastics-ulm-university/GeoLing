package geoling.util;



//import java.util.*;



/**

* This class can represent (rectangular) boxes with arbitrary dimension.

* The box is defined through two points being contained in the box.

* Namely the point whose coordinates are all minimal (the so called

* "minimal point") and the one whose coordinates are all maximal

* (the so called "maximal point").

* The coordinates of both points must be double

* values. This class is mainly used for bounding boxes in simulation context.

*

* @author  Institute of Stochastics, Ulm University

* @version 1.0, 2001-09-26

*/

public final class DoubleBox implements Cloneable {



    /**

    * The point within the box which has minimal coordinates.

    * We say that <code>min</code> is the minimal point within the box.

    */

    private double[] min;

    /**

    * The point within the box which has maximal coordinates.

    * We say that <code>max</code> is the maximal point within the box.

    */

    private double[] max;

    /**

    * Is <code>true</code> when the box is empty and

    * otherwise <code>false</code>.

    */

    private boolean empty;



    /**

    * Constructs an empty box.

    */

    public DoubleBox() {

	min = null;

	max = null;

	empty = true;

    }



    /**

    * Constructs a box with minimal point <code>min</code> and maximal

    * point <code>max</code>. The resulting box contains at least one

    * point.

    *

    * @param	min	the so called minimal point within the box which

    *			is the point with minimal coordinates.

    * @param	max	the so called maximal point within the box which

    *			is the point with maximal coordinates.

    * @throws	IllegalArgumentException

    *			if <code>min.length != max.length</code> or not

    *			<code>min.length > 0</code> or

    *			<code>min[i] > max[i]</code> for an

    *			<code>0 <= i < min.length</code>.

    */

    public DoubleBox(double[] min, double[] max) {

	if (min.length != max.length)

	    throw new IllegalArgumentException("min and max must have the same length");



	if (! (min.length > 0))

	    throw new IllegalArgumentException("min and max must have positive length");



	for (int i = 0; i < min.length; i++)

	    if (min[i] > max[i])

		throw new IllegalArgumentException("min[" + i + "] is greater than max[" + i + "]");



	this.min = (double[]) min.clone();

	this.max = (double[]) max.clone();

	empty = false;

    }



    /**

    * Returns a copy of the box.

    *

    * @return	the copy of the box.

    * @throws	InternalError

    *			if a <code>CloneNotSupportedException</code> occurs

    *			when calling the <code>clone()</code> method of the

    *			super class.

    */

    public Object clone() {

	DoubleBox b;



	try {

	    b = (DoubleBox) super.clone();

	}

	catch (CloneNotSupportedException e) {

	    throw new InternalError(e.toString());

	}



	if (! empty) {

	    b.min = (double[]) min.clone();

	    b.max = (double[]) max.clone();

	}



	return b;

    }



    /**

    * Returns <code>true</code> when the box is empty and <code>false</code>

    * otherwise.

    *

    * @return	is <code>true</code> when the box is empty and

    *		<code>false</code> otherwise.

    */

    public boolean isEmpty() {

	return empty;

    }



    /**

    * Returns the dimension of the box and <code>0</code> if the box

    * is empty.

    *

    * @return	the dimension of the box.

    */

    public int getDimension() {

	if (empty)

	    return 0;

	else

	    return min.length;

    }



    /**

    * Returns the volume of the box.

    *

    * @return	the volume of the box.

    */

    public double getVolume() {

	if (empty)

	    return 0.0;



	double volume = 1.0;

	for (int i = 0; i < getDimension(); i++)

	    volume *= getWidth(i);



	return volume;

    }



    /**

    * Returns the <code>i</code>-th coordinate of the minimal point.

    *

    * @param	i	the number of the requested coordinate.

    * @return	the <code>i</code>-th coordinate of the minimal point.

    * @throws	IllegalArgumentException

    *			if the box is empty or not

    *			<code>0 <= i < getDimension()</code>.

    */

    public double getMin(int i) {

	if (empty)

	    throw new IllegalArgumentException("this is empty");



	if (i < 0 || i >= getDimension())

	    throw new IllegalArgumentException("i must be between 0 and dimension - 1");



	return min[i];

    }



    /**

    * Returns a copy of the minimal point of the box.

    *

    * @return	a copy of the minimal point of the box.

    * @throws	IllegalArgumentException

    *			if the box is empty.

    */

    public double[] getMin() {

	if (empty)

	    throw new IllegalArgumentException("this is empty");



	return (double[]) min.clone();

    }



    /**

    * Returns the <code>i</code>-th coordinate of the maximal point.

    *

    * @param	i	the number of the requested coordinate.

    * @return	the <code>i</code>-th coordinate of the maximal point.

    * @throws	IllegalArgumentException

    *			if the box is empty or not

    *			<code>0 <= i < getDimension()</code>.

    */

    public double getMax(int i) {

	if (empty)

	    throw new IllegalArgumentException("this is empty");



	if (i < 0 || i >= getDimension())

	    throw new IllegalArgumentException("i must be between 0 and dimension - 1");



	return max[i];

    }



    /**

    * Returns a copy of the maximal point of the box.

    *

    * @return	a copy of the maximal point of the box.

    * @throws	IllegalArgumentException

    *			if the box is empty.

    */

    public double[] getMax() {

	if (empty)

	    throw new IllegalArgumentException("this is empty");



	return (double[]) max.clone();

    }



    /**

    * Returns the width of the box in the <code>i</code>-th dimension.

    *

    * @param	i	the number of dimension.

    * @return	the width of the box in the <code>i</code>-th dimension.

    */

    public double getWidth(int i) {

	return getMax(i) - getMin(i);

    }



    /**

    * Returns the width of the box in all dimensions.

    *

    * @return	the width of the box in all dimension within an array.

    */

    public double[] getWidth() {

	if (empty)

	    throw new IllegalArgumentException("this is empty");



	double[] width = new double[getDimension()];

	for (int i = 0; i < getDimension(); i++)

	    width[i] = max[i] - min[i];



	return width;

    }



    /**

    * Sets the <code>i</code>-th coordinate of the minimal point of

    * the box to <code>min</code> and the <code>i</code>-th coordinate of the

    * maximal point of the box to <code>max</code>.

    *

    * @param	i	the number of the coordinate to be changed.

    * @param	min	the new value of the <code>i</code>-th coordinate

    *			of the minimal point.

    * @param	max	the new value of the <code>i</code>-th coordinate

    *			of the maximal point.

    * @throws	IllegalArgumentException

    *			if the box is empty or not

    *			<code>0 <= i < getDimension()</code> or

    *			<code>min > max</code>.

    */

    public void setRange(int i, double min, double max) {

	if (empty)

	    throw new IllegalArgumentException("this is empty");



	if (i < 0 || i >= getDimension())

	    throw new IllegalArgumentException("i must be between 0 and dimension - 1");



	if (min > max)

	    throw new IllegalArgumentException("min must not be greater than max");



	this.min[i] = min;

	this.max[i] = max;

    }



    /**

    * Increases the <code>i</code>-th coordinate of the minimal point of

    * the box by <code>dmin</code> and the <code>i</code>-th coordinate of the

    * maximal point of the box by <code>dmax</code>.

    *

    * @param	i	the number of the coordinate to be changed.

    * @param	dmin	the value by which the <code>i</code>-th coordinate

    *			of the minimal point is increased.

    * @param	dmax	the value by which the <code>i</code>-th coordinate

    *			of the maximal point is increased.

    * @throws	IllegalArgumentException

    *			if the box is empty or not

    *			<code>0 <= i < getDimension()</code> or

    *			<code>getMin(i) + dmin > getMax(i) + dmax</code>.

    */

    public void changeRange(int i, double dmin, double dmax) {

	setRange(i, getMin(i) + dmin, getMax(i) + dmax);

    }



    /**

    * Translates <code>point</code> whose origin is the minimal point

    * such that its origin is the origin of the coordinate systems.

    * Therefore, <code>point</code> is translated by the vector

    * which is the inverse of the minimal point. The translation takes

    * only place if the dimension of the box is the same as the dimension

    * of <code>point</code>.

    *

    * @param	point	the point which is to be translated.

    */

    public void translateToOrigin(double[] point) {

	if (point.length == getDimension())

	    for (int i = 0; i < point.length; i++)

		point[i] -= min[i];

    }



    /**

    * Returns <code>true</code> when <code>point</code> is contained

    * within the box and <code>false</code> otherwise. An empty box

    * contains no point.

    *

    * @param	point	the point which should be tested.

    * @return	<code>true</code> when <code>point</code> is contained

    *		within the box and <code>false</code> otherwise.

    * @throws	IllegalArgumentException

    *			if the box is not empty and the point does not

    *			have the same dimension as the box.

    */

    public boolean contains(double[] point) {

	if (empty)

	    return false;



	if (point.length != getDimension())

	    throw new IllegalArgumentException("point must have same dimension as this");



	for (int i = 0; i < point.length; i++)

	    if (point[i] < min[i] || point[i] > max[i])

		return false;



	return true;

    }



    /**

    * Returns <code>true</code> when <code>b</code> is contained in the

    * box and <code>false</code> otherwise. An empty box is contained within

    * every non empty box. An empty box can contain no box.

    *

    * @param	b	the box which should be tested whether it is

    *			contained within the (<code>this</code>) box.

    * @return	<code>true</code> when <code>b</code> is contained in the

    *		box and <code>false</code> otherwise.

    * @throws	IllegalArgumentException

    *			if both boxes are not empty and do not have

    *			the same dimensions.

    */

    public boolean contains(DoubleBox b) {

	if (b.empty)

	    return true;

	else if (empty)

	    return false;



	if (getDimension() != b.getDimension())

	    throw new IllegalArgumentException("b must have the same dimension as this");



	for (int i = 0; i < getDimension(); i++)

	    if (b.min[i] < min[i] || b.max[i] > max[i])

		return false;



	return true;

    }



    /**

    * Tests whether the intersection of <code>b</code> and the box is

    * not empty.

    *

    * @param	b	the box which should be tested whether it intersects

    *			with the (<code>this</code>) box.

    * @return	<code>true</code> when <code>b</code> intersects with the

    *		box and <code>false</code> otherwise.

    * @throws	IllegalArgumentException

    *			if both boxes are not empty and do not have

    *			the same dimensions.

    */

    public boolean intersectsWith(DoubleBox b) {

	return ! intersection(b).isEmpty();

    }



    /**

    * Translates the box by <code>vector</code>.

    *

    * @param	vector	the vector by which the box is to be translated.

    * @throws	IllegalArgumentException

    *			if the box is empty or <code>vector</code> does not

    *			have the same dimension as the box.

    */

    public void translateBy(double[] vector) {

	if (empty)

	    throw new IllegalArgumentException("this is empty");



	if (vector.length != getDimension())

	    throw new IllegalArgumentException("vector must have the same dimension as this");



	for (int i = 0; i < vector.length; i++) {

	    min[i] += vector[i];

	    max[i] += vector[i];

	}

    }



    /**

    * Returns the minimal box which contains <code>b</code> and the

    * <code>this</code> box. The empty box is the neutral element of

    * this operation.

    *

    * @param	b	the second box for the union.

    * @return	the minimal box which contains <code>b</code> and the

    *		<code>this</code> box.

    * @throws	IllegalArgumentException

    *			if both boxes are not empty and do not have

    *			the same dimensions.

    */

    public DoubleBox union(DoubleBox b) {

	if (empty)

	    return (DoubleBox) b.clone();

	else if (b.empty)

	    return (DoubleBox) clone();

	else {

	    if (getDimension() != b.getDimension())

		throw new IllegalArgumentException("b must have the same dimension as this");



	    DoubleBox result = (DoubleBox) clone();



	    for (int i = 0; i < min.length; i++) {

		result.min[i] = Math.min(min[i], b.min[i]);

		result.max[i] = Math.max(max[i], b.max[i]);

	    }



	    return result;

	}

    }



    /**

    * Returns the maximal box which is contained in <code>b</code> and the

    * <code>this</code> box.

    *

    * @param	b	the second box for the intersection.

    * @return	the maximal box which is contained in <code>b</code> and

    *		the <code>this</code> box.

    * @throws	IllegalArgumentException

    *			if both boxes are not empty and do not have

    *			the same dimensions.

    */

    public DoubleBox intersection(DoubleBox b) {

	if (empty)

	    return (DoubleBox) clone();

	else if (b.empty)

	    return (DoubleBox) b.clone();

	else {

	    if (getDimension() != b.getDimension())

		throw new IllegalArgumentException("b must have the same dimension as this");



	    DoubleBox result = (DoubleBox) clone();



	    for (int i = 0; i < min.length; i++) {

		result.min[i] = Math.max(min[i], b.min[i]);

		result.max[i] = Math.min(max[i], b.max[i]);

		if (result.min[i] > result.max[i]) {

		    result.min = null;

		    result.max = null;

		    result.empty = true;

		    break;

		}

	    }



	    return result;

	}

    }



    /**

    * Returns the maximal box which contains all points such that when

    * <code>se</code> is translated by one of these points it intersects

    * with <code>this</code> box. This operation is called <i>plus sampling</i>

    * where <code>se</code> is the structuring element. (It is the same as

    * a dilatation by the same structuring element.) If <code>this</code>

    * box or <code>se</code> is empty, then the resulting box is empty, too.

    *

    * @param	se	the structuring element for the plus sampling.

    * @return	the box which results from the plus sampling.

    * @throws	IllegalArgumentException

    *			if both boxes are not empty and do not have

    *			the same dimensions.

    */

    public DoubleBox plusSampling(DoubleBox se) {

	if (empty)

	    return (DoubleBox) clone();

	else if (se.empty)

	    return (DoubleBox) se.clone();

	else {

	    if (getDimension() != se.getDimension())

		throw new IllegalArgumentException("se must have the same dimension as this");



	    DoubleBox result = (DoubleBox) clone();



	    for (int i = 0; i < min.length; i++) {

		result.min[i] = min[i] - se.max[i];

		result.max[i] = max[i] - se.min[i];

	    }



	    return result;

	}

    }



    /**

    * Returns the maximal box which contains all points of <code>this</code>

    * box such that <code>se</code> being translated by one of these points

    * is contained in <code>this</code> box. This operation is called

    * <i>minus sampling</i> where <code>se</code> is the structuring element.

    * (It is the same as an erosion by the structuring element, or equivalently a

    * Minkowski subtraction by the structuring element reflected at the origin.)

    * If <code>this</code> box or <code>se</code> is empty, then the resulting

    * box is empty, too.

    *

    * @param	se	the structuring element for the minus sampling.

    * @return	the box which results from the minus sampling.

    * @throws	IllegalArgumentException

    *			if both boxes are not empty and do not have

    *			the same dimensions.

    */

    public DoubleBox minusSampling(DoubleBox se) {

	if (empty)

	    return (DoubleBox) clone();

	else if (se.empty)

	    return (DoubleBox) se.clone();

	else {

	    if (getDimension() != se.getDimension())

		throw new IllegalArgumentException("se must have the same dimension as this");



	    DoubleBox result = (DoubleBox) clone();



	    for (int i = 0; i < min.length; i++) {

		result.min[i] = min[i] - se.min[i];

		result.max[i] = max[i] - se.max[i];

		if (result.min[i] > result.max[i]) {

		    result.min = null;

		    result.max = null;

		    result.empty = true;

		    break;

		}

	    }



	    return result;

	}

    }



    /**

    * Returns a discrete version of this box.

    *

    * @return   a discrete version of this box.

    */

    public Box toBox() {

        if (isEmpty())

	    return new Box();



	int[] imin = new int[min.length];

	int[] imax = new int[max.length];



	for (int i = 0; i < min.length; i++) {

	    imin[i] = (int) Math.ceil(min[i] + 0.5);

	    imax[i] = (int) Math.floor(max[i] - 0.5);

	}



	return new Box(imin, imax);

    }



    /**

    * Returns a string representation of this box for

    * debugging purposes.

    *

    * @return	a string representation of this box.

    */

    public String toString() {

	String s = "DoubleBox(";



	for (int i = 0; i < min.length; i++) {

	    if (i > 0)

		s = s + " x ";

	    s = s + "[" + min[i] + ";" + max[i] + "]";

	}



	return s + ")";

    }



}

