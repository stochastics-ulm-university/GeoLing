package geoling.util.sim.util;

import geoling.util.DoubleBox;
//import geoling.util.sim.grain.Point;


import geoling.util.sim.grain.ConvexPolytope;
import geoling.util.sim.grain.LineSegment;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
/**
 * This class defines a random set which can hold elements of
 * type <code>RandomSetElement</code>.
 *
 * @author  Institute of Stochastics, Ulm University
 * @version 1.01,  08. 07. 2010
 */
public class RandomSet implements RandomSetElement {
    /** The bounding box of the random set. */
    private DoubleBox box;
    /** The set which contains all elements of the random set. */
    private Set<RandomSetElement> s = new HashSet<RandomSetElement>();
    /** The used sampling window. */
    private ConvexPolytope samplingWindow = null;
    /**
     * Constructs a new random set with the given bounding box.
     *
     * @param	boundingBox	the bounding box for the new random set.
     */
    public RandomSet(DoubleBox boundingBox) {
        box = (DoubleBox) boundingBox.clone();
    }
    /**
     * Constructs a new random set with the given sampling window.
     *
     * @param	samplingWindow	the convex polytope used as sampling window.
     */
    public RandomSet(ConvexPolytope samplingWindow) {
        this.samplingWindow = (ConvexPolytope) samplingWindow.clone();
        box = samplingWindow.getBoundingBox();
    }
    /**
     * Returns the dimension of the random set.
     *
     * @return	the dimension of the random set.
     */
    @Override
	public int getDimension() {
        return box.getDimension();
    }
    /**
     * Returns the bounding box of the random set.
     *
     * @return	the bounding box of the random set.
     */
    @Override
	public DoubleBox getBoundingBox() {
        return (DoubleBox) box.clone();
    }
    /**
     * Returns the sampling window of the random set.
     *
     * @return	the sampling window of the random set.
     */
    public ConvexPolytope getSamplingWindow() {
        if (samplingWindow != null)
            return (ConvexPolytope) samplingWindow.clone();
        else
            return null;
    }
    /**
     * Translates the random set by the given vector,
     * i.e. all elements of the random set are translated by
     * the given vector.
     *
     * @param	vector	the vector by which the random
     *			set is to be translated.
     */
    @Override
	public void translateBy(double[] vector) {
        // translate all elements
        for (RandomSetElement elem: s) {
            elem.translateBy(vector);
        }
        // ... and translate the bounding box
        box.translateBy(vector);
        // ... and also the sampling window
        if (samplingWindow != null)
            samplingWindow.translateBy(vector);
    }
    
    /**
     * Translates the random set by the vector such that
     * a vertex of the random set is the origin.
     *
     */    
    public void translateToOrigin(){
    	double[] min = this.getBoundingBox().getMin();    	
        this.translateBy(new double[]{-min[0],-min[1]});
    }
    
    /**
     * Draws the random set in <code>image</code> by drawing all
     * its elements in <code>image</code>.
     *
     * @param	image	the image in which the random set
     *			is to be drawn.
     */
    @Override
	public void draw(Object image) {
        for (RandomSetElement elem : s) {
            try {
                elem.draw(image);
            }
            catch (IllegalArgumentException e) {
            	/* continue with next element */
            }
        }
    }
    /**
     * Returns the size (cardinality) of the random set.
     *
     * @return	the cardinality of the random set.
     */
    public int size() {
        return s.size();
    }
    /**
     * Returns <code>true</code> iff the random set is empty.
     *
     * @return	<code>true</code> iff the random set is empty.
     */
    public boolean isEmpty() {
        return s.isEmpty();
    }
    /**
     * Removes all elements from the random set.
     */
    public void clear() {
        s.clear();
    }
    /**
     * Adds the specified element to the random set,
     * if it is not already contained.
     *
     * @param	element	the element which has to be added
     *			to the random set.
     */
    public void add(RandomSetElement element) {
        s.add(element);
    }
    /**
     * Removes the specified element from the random set,
     * if it is contained.
     *
     * @param	element	the element which has to be
     *			removed from the random set.
     */
    public void remove(RandomSetElement element) {
        s.remove(element);
    }
    /**
     * Returns <code>true</code> iff the given element
     * is contained in the random set.
     *
     * @param	element	the element which has to be tested.
     * @return	<code>true</code> iff the element is contained
     *		in the random set.
     */
    public boolean contains(RandomSetElement element) {
        return s.contains(element);
    }
    /**
     * Returns an iterator for the random set.
     *
     * @return	an iterator for the random set.
     */
    public Iterator<RandomSetElement> iterator() {
        return s.iterator();
    }
    
    @Override
    public RandomSet clone(){
    	
    	RandomSet rs_new;
    	
    	if(this.samplingWindow == null){
    		rs_new = new RandomSet(this.box);
    	}
    	else{
    		rs_new = new RandomSet(this.samplingWindow);
    	}
    	
    	for(RandomSetElement o: s){
    		if(o instanceof Point){
    			Point p = (Point) o;
    			rs_new.add((Point) p.clone());
    		}
    		else if(o instanceof LineSegment){
    			LineSegment ls = (LineSegment) o;
    			rs_new.add(ls.clone());
    		}
    		else if(o instanceof Polytope){
    			Polytope poly = (Polytope) o;
    			rs_new.add((Polytope) poly.clone());
    		}
    		else if(o instanceof ConvexPolytope){
    			ConvexPolytope poly = (ConvexPolytope) o;
    			rs_new.add((ConvexPolytope) poly.clone());
    		}
    		else if(o instanceof RandomSet){
    			RandomSet rs = (RandomSet) o;
    			rs_new.add(rs.clone());
    		}
			else{
				try {throw new Exception("Cloning of the Element not implemented: "+o.getClass());} 
    			catch (Exception e) {e.printStackTrace();}
			}
    	}
    	return rs_new;
    }

    
    /**
	 * Stretch the RandomSet with the factor scale.
	 * 
	 * @param scale		The stretch factor
	 */
	public void stretch(double scale){
		double[] min = box.getMin();
		double[] max = box.getMax();
    	double[] min_new = new double[]{min[0]*scale, min[1]*scale};
    	double[] max_new = new double[]{max[0]*scale, max[1]*scale};
    	
    	this.box = new DoubleBox(min_new, max_new);
    	
    	if(samplingWindow != null)
			samplingWindow.stretch(scale);
		
		
		for(RandomSetElement x : s){
			if( x instanceof LineSegment){
				LineSegment ls = (LineSegment) x;
				ls.stretch(scale);
			}
			if( x instanceof Point){
				Point p = (Point) x;
				p.stretch(scale);
			}
			if( x instanceof ConvexPolytope){
				ConvexPolytope convpoly = (ConvexPolytope) x;
				convpoly.stretch(scale);
			}
			if(x instanceof Polytope){
				Polytope poly = (Polytope) x;
				poly.stretch(scale);
			}
			if(x instanceof RandomSet){
				RandomSet rs = (RandomSet) x;
				rs.stretch(scale);
			}
		}
	}
}
