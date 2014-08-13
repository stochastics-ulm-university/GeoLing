package geoling.util.sim.grain;



/**

* This class implements a markedpoint object which may be an

* element of a random set.

*

* @author  Institute of Stochastics, Ulm University

* @version 1.0, 02-01-17

*/

public class MarkedPoint extends Point {



    /** The value of the markedpoint. */

	private double val;



    /**

    * Constructs a new markedpoint with the given coordinates

	* and value

    *

    * @param	coordinates	the coordinates of the markedpoint.

	* @param	value	the value of the markedpoint

    */

    public MarkedPoint(double[] coordinates, double value) {

		super(coordinates);

		val = value;

    }



    /**

    * Sets the value of the markedpoint.

    *

    * @param	value	the value of the markedpoint.

    */

	public void setValue(double value) {

		val = value;

	}



	/**

    * Returns the value of the markedpoint.

    *

    * @return	the value of the markedpoint.

    */

	public double getValue() {

		return val;

	}

}

