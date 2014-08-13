package geoling.util;



import java.util.*;



/**

* This class can represent (rectangular) boxes with arbitrary dimension.

* The box is defined through two points being contained in the box.

* Namely the point whose coordinates are all minimal (the so called

* "minimal point") and the one whose coordinates are all maximal

* (the so called "maximal point").

* The coordinates of both points must be integer

* values. This class is mainly used for bounding boxes of images.

*

* @author  Institute of Stochastics, Ulm University

* @version 1.0, 2001-06-27

*/

public final class Box implements Cloneable {



    /**

    * The point within the box which has minimal coordinates.

    * We say that <code>min</code> is the minimal point within the box.

    */

    private int[] min;

    /**

    * The point within the box which has maximal coordinates.

    * We say that <code>max</code> is the maximal point within the box.

    */

    private int[] max;

    /**

    * Is <code>true</code> when the box is empty and

    * otherwise <code>false</code>.

    */

    private boolean empty;



    /**

    * Constructs an empty box.

    */

    public Box() {

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

    public Box(int[] min, int[] max) {

	if (min.length != max.length)

	    throw new IllegalArgumentException("min and max must have the same length");



	if (! (min.length > 0))

	    throw new IllegalArgumentException("min and max must have positive length");



	for (int i = 0; i < min.length; i++)

	    if (min[i] > max[i])

		throw new IllegalArgumentException("min[" + i + "] is greater than max[" + i + "]");



	this.min = (int[]) min.clone();

	this.max = (int[]) max.clone();

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

	Box b;



	try {

	    b = (Box) super.clone();

	}

	catch (CloneNotSupportedException e) {

	    throw new InternalError(e.toString());

	}



	if (! empty) {

	    b.min = (int[]) min.clone();

	    b.max = (int[]) max.clone();

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

    * Returns the volume of the box. The volume of a box is the number

    * of points with integer coordinates which are within the box.

    *

    * @return	the volume of the box.

    */

    public long getVolume() {

	if (empty)

	    return 0;



	long volume = 1;

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

    public int getMin(int i) {

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

    public int[] getMin() {

	if (empty)

	    throw new IllegalArgumentException("this is empty");



	return (int[]) min.clone();

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

    public int getMax(int i) {

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

    public int[] getMax() {

	if (empty)

	    throw new IllegalArgumentException("this is empty");



	return (int[]) max.clone();

    }



    /**

    * Returns the width of the box in the <code>i</code>-th dimension.

    *

    * @param	i	the number of dimension.

    * @return	the width of the box in the <code>i</code>-th dimension.

    */

    public int getWidth(int i) {

	return getMax(i) - getMin(i) + 1;

    }



    /**

    * Returns the width of the box in all dimensions.

    *

    * @return	the width of the box in all dimension within an array.

    */

    public int[] getWidth() {

	if (empty)

	    throw new IllegalArgumentException("this is empty");



	int[] width = new int[getDimension()];

	for (int i = 0; i < getDimension(); i++)

	    width[i] = max[i] - min[i] + 1;



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

    public void setRange(int i, int min, int max) {

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

    public void changeRange(int i, int dmin, int dmax) {

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

    public void translateToOrigin(int[] point) {

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

    public boolean contains(int[] point) {

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

    public boolean contains(Box b) {

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

    public boolean intersectsWith(Box b) {

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

    public void translateBy(int[] vector) {

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

    public Box union(Box b) {

	if (empty)

	    return (Box) b.clone();

	else if (b.empty)

	    return (Box) clone();

	else {

	    if (getDimension() != b.getDimension())

		throw new IllegalArgumentException("b must have the same dimension as this");



	    Box result = (Box) clone();



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

    public Box intersection(Box b) {

	if (empty)

	    return (Box) clone();

	else if (b.empty)

	    return (Box) b.clone();

	else {

	    if (getDimension() != b.getDimension())

		throw new IllegalArgumentException("b must have the same dimension as this");



	    Box result = (Box) clone();



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

    public Box plusSampling(Box se) {

	if (empty)

	    return (Box) clone();

	else if (se.empty)

	    return (Box) se.clone();

	else {

	    if (getDimension() != se.getDimension())

		throw new IllegalArgumentException("se must have the same dimension as this");



	    Box result = (Box) clone();



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

    public Box minusSampling(Box se) {

	if (empty)

	    return (Box) clone();

	else if (se.empty)

	    return (Box) se.clone();

	else {

	    if (getDimension() != se.getDimension())

		throw new IllegalArgumentException("se must have the same dimension as this");



	    Box result = (Box) clone();



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

    * Returns an Iterator object which iterates all points which

    * are contained in the box. The dynamic type of all objects

    * returned by the Iterator object is <code>int[]</code>.

    * The <code>remove()</code> method of this Iterator object throws an

    * <code>UnsupportedOperationException</code>, because you can not

    * remove anything. For optimisation reasons, the point returned

    * by the iterator <i>must not be changed</i>!

    *

    * @return	the Iterator object which iterates all points that

    *		are contained in the box.

    */

    public Iterator<int[]> containedPoints() {

	if (empty)

	    return new Iterator<int[]>() {

		public boolean hasNext() {

		    return false;

		}



		public int[] next() {

		    throw new NoSuchElementException();

		}



		public void remove() {

		    throw new UnsupportedOperationException();

		}

	    };

	else

	    return new Iterator<int[]>() {

		// private copies of variables (optimisation)

		private int[] pmin = (int[]) min.clone();

		private int[] pmax = (int[]) max.clone();

		private int i; // instead of a local variable in next() (optimisation)



		private long remaining = getVolume();

		private int[] index = (int[]) min.clone();



		{

		    index[0]--; // because incremented before returned by next()

		}



		public boolean hasNext() {

		    return remaining > 0;

		}



		// !!! returned object must not be changed !!! (optimisation)

		public int[] next() {

		    if (remaining > 0) {

    			index[0]++;



			/*

			* The test "i < pmin.length - 1" can be left out because

			* we only come here, when there is still one

			* point in the box (remaining > 0)!

			* (optimisation)

			*/

			for (i = 0; index[i] > pmax[i]; i++) {

			    index[i] = pmin[i];

		    	    index[i+1]++;

			}



			remaining--;



			return index;

		    }

		    else

			throw new NoSuchElementException();

		}



		public void remove() {

		    throw new UnsupportedOperationException();

		}

	    };

    }



    /**

    * Returns a continuous version of this box.

    *

    * @return   a continuous version of this box.

    */

    public DoubleBox toDoubleBox() {

        if (isEmpty())

	    return new DoubleBox();



	double[] dmin = new double[min.length];

	double[] dmax = new double[max.length];



	for (int i = 0; i < min.length; i++) {

	    dmin[i] = min[i] - 0.5;

	    dmax[i] = max[i] + 0.5;

	}



	return new DoubleBox(dmin, dmax);

    }



    public String toString() {

	String s = "Box: (";

	for (int i = 0; i < min.length; i++)

	    s += (i > 0 ? "," : "") + min[i];

	s += ") - (";

	for (int i = 0; i < max.length; i++)

	    s += (i > 0 ? "," : "") + max[i];

	return s + ")";

    }



}

