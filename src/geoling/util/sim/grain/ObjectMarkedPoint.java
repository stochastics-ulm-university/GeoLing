package geoling.util.sim.grain;

/**
 * 
 * @author Institute of Stochastics, Ulm University
 * 
 * extends geoling.util.sim.grain.Point by a mark that can be an arbitratry object
 *
 */

	public class ObjectMarkedPoint extends Point {



	    /** The value of the markedpoint. */

		private Object val;



	    /**

	    * Constructs a new markedpoint with the given coordinates

		* and value

	    *

	    * @param	coordinates	the coordinates of the markedpoint.

		* @param	o	the value of the markedpoint

	    */

	    public ObjectMarkedPoint(double[] coordinates, Object o) {

			super(coordinates);

			val = o;

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

		public Object getValue() {

			return val;

		}

	
	
	
	
}
