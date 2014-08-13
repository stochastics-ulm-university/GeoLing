package geoling.util;

import java.lang.Math;

/**
 * This Class is used to represent and calculate locations, bearings and 
 * distances on the surface of the earth. Input and Return Values are in decimal degrees resp. km.
 * Conversion between Nautical Miles, (Statute) Miles and Kilometers is available.
 * 
 * @author  Jonas Rumpf, Institute of Stochastics, Ulm University
 * @version 1.0
 */
public class LatLong  {
    
    /** 
     * The latitude of <code>this</code> in radian 
     */
    private double latitu=0.0;
    
    /** 
     * The longitude of <code>this</code> in radian 
     */
    private double longit=0.0;
   
    /** The Mean Earth Radius in km*/
    private static final double R=6371.0;;  
    /** 1 Nautical Mile in km*/
    private static final double nautical_mile=1.852;
    /** 1 (Statute) Mile in km */
    private static final double statute_mile=1.6093;  
    
    /** Tolerance in km below which 2 locations are considered equal */
    private static final double TOLERANCE = 0.001;
    
    /** Creates a new instance of LatLong, representing a location anywhere on earth.
     *
     *  @param degLatitu Latitude in decimal degrees; between -90.0 and 90.0. Positive values are considered North, negative values South.
     *  @param degLongit Longitude in decimal degrees; between -180.0 and 180.0. Positive values are considered East, negative values West.
     * 
     *  @throws IllegalArgumentException if <code>degLatitu</code> is outside the interval [-90,90].
     *  @throws IllegalArgumentException if <code>degLongit</code> is outside the interval [-180,180].
     */
     public LatLong(double degLatitu, double degLongit) throws IllegalArgumentException
     {
        if (!((degLatitu<=90) && (degLatitu>=-90)))   
        {throw new IllegalArgumentException("Invalid Location Latitude "+degLatitu+"; must be between -90 and 90.");}
        
        if (!((degLongit<=180) && (degLongit>=-180))) 
        {throw new IllegalArgumentException("Invalid Location Longitude "+degLongit+"; must be between -180 and 180.");}
              
        this.latitu = degToRad(degLatitu);
        this.longit = degToRad(degLongit);
     }
    
    
    /** Calculates the starting direction when travelling in a straight line 
     * from <code>this</code> location to the location represented by <code>dest</code>.
     * 0 is to be considered North, 90 East, 180 South and 270 West.
     *
     * @param dest Destination Location, represented by a LatLong Object
     *
     * @return direction from <code>this</code> to <code>dest</code> in decimal degrees.
     *          Is 0.0 , if <code>this.equals(dest)</code> is <code>true</code>.
     *
     * @see <a href="http://www.movable-type.co.uk/scripts/LatLong.html">http://www.movable-type.co.uk/scripts/LatLong.html</a> 
     * @see <a href="http://www.census.gov/cgi-bin/geo/gisfaq?Q5.1">http://www.census.gov/cgi-bin/geo/gisfaq?Q5.1</a> 
     * @see "R.W. Sinnott, "Virtues of the Haversine", Sky and Telescope, vol. 68, no. 2, 1984" 
     *
     */
    public double calculateBearingTo(LatLong dest)
    {
        double y = Math.sin(this.longit - dest.longit) * Math.cos(dest.latitu);
        double x = Math.cos(this.latitu)*Math.sin(dest.latitu)-Math.sin(this.latitu)*Math.cos(dest.latitu)*Math.cos(this.longit-dest.longit);
        return radToDeg(Math.atan2(-y,x));
    }
    
    /** Calculates the distance in km to travel when travelling in a straight line from <code>this</code>
     * location to the location represented by <code>dest</code>.
     *
     * @param dest Destination Location, represented by a LatLong Object
     *
     * @return distance from <code>this</code> to <code>dest</code> in km
     * @see <a href="http://www.movable-type.co.uk/scripts/LatLong.html">http://www.movable-type.co.uk/scripts/LatLong.html</a>
     * @see <a href="http://www.census.gov/cgi-bin/geo/gisfaq?Q5.1">http://www.census.gov/cgi-bin/geo/gisfaq?Q5.1</a>
     * @see "R.W. Sinnott, "Virtues of the Haversine", Sky and Telescope, vol. 68, no. 2, 1984"
     *
     */
    public double calculateDistanceTo(LatLong dest)
    {
        double dlongit = dest.longit - this.longit;
        double dlatitu = dest.latitu - this.latitu;
        
        double a = Math.sin(dlatitu/2) * Math.sin(dlatitu/2) + Math.cos(this.latitu) * Math.cos(dest.latitu) * Math.sin(dlongit/2) * Math.sin(dlongit/2);
        
        // Math.atan2(x,y): Konvertiert(x,y) in Polarkoordinaten r,theta; berechnet Theta als Arcustangens von y/x; liefert Theta zurueck.
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        
        return d;  // in km! 
    }
    
    /** Calculates the destination that will be reached if travelling 
     *  from <code>this</code> location in a straight line into a given direction for a given distance.     
     *
     * @param bearing The direction in which to start from this location
     * @param distance The distance in km to head into the given direction
     *
     * @return A LatLong Object representing the destination location
     *
     * @see <a href="http://williams.best.vwh.net/avform.htm#LL">Aviation Formulary V1.42</a>
     *
     * @throws IllegalArgumentException if something went wrong in constructing the returned object
     */
    public LatLong getDestination(double bearing, double distance) throws IllegalArgumentException
    {
      
       bearing  = degToRad(bearing);
       distance = distance/R;
        
       double y = Math.asin(Math.sin(this.latitu)*Math.cos(distance)+Math.cos(this.latitu)*Math.sin(distance)*Math.cos(bearing));
       double d_x = Math.atan2(Math.sin(bearing)*Math.sin(distance)*Math.cos(this.latitu),Math.cos(distance)-Math.sin(this.latitu)*Math.sin(y));
       double x = ((this.longit + d_x + Math.PI) %(2*Math.PI))-Math.PI;
       
       x = radToDeg(x);
       y = radToDeg(y);  
       
       
       if(x>180) x-=360;
      // if(x<-180) x+=360;
       
       if(y>90) y-=360;
       //if(y<-90) y+=360;
       
       //System.out.println("--->x="+x+"--->y="+y);
       
               
       return new LatLong(y, x);
    }
    
    
    /** Converts Kilometers to Nautical Miles
     *   
     *  @param km Value to be converted
     *
     *  @return km converted to Nautical Miles
     *
     */
    public static double kmTOnm(double km)
    {
        return (km/nautical_mile); 
    }
    
    /** Converts Kilometers to (Statute) Miles
     *   
     *  @param km Value to be converted
     *
     *  @return km converted to (Statute) Miles
     */
    public static double kmTOmile(double km)
    {
        return (km/statute_mile); 
    }
   
    /** Converts Nautical Miles to Kilometers
     *   
     *  @param nm Value to be converted
     *
     *  @return nm converted to Kilometers
     */
    public static double nmTOkm(double nm)
    {
        return (nm*nautical_mile);
    }
    
    /** Converts (Statute) Miles to Kilometers
     *   
     *  @param miles Value to be converted
     *
     *  @return miles converted to Kilometers
     *
     */
    public static double mileTOkm(double miles)
    {
        return (miles*statute_mile);
    }
    
    /** Converts angles from degrees to radian.
     *
     *  @param deg angle in degrees
     *
     *  @return angle in radian
    */    
    public static double degToRad(double deg)
    {
        return (deg * java.lang.Math.PI / 180.0);
    }
    
    /** Converts angles from radian to degrees.
     *
     *  @param rad angle in radian 
     *
     *  @return  angle in degrees; between 0 and 360 degrees.
    */    
    public static double radToDeg(double rad)
    {
        return (rad * 180.0 / Math.PI + 360 ) % 360; 
    }
    
    /** @return the Latitude of <code>this</code> Location in decimal degrees
    */ 
    public double getLatitude()
    {
    	double r = radToDeg(this.latitu);
    	if(r>90) r = r - 360;
    	
    	return r;
    }
    
    /** @return the Longitude of <code>this</code> Location in decimal degrees
    */ 
    public double getLongitude()
    {
        double r = radToDeg(this.longit);
        if(r>180) r = r - 360;
        
        return r;
    }
    
    
    /** Compares <code>Object</code> to <code>this</code>.
     *
     *  @param obj <code>Object</code> to be compared with <code>this</code>.
     *
     * @return <code>true</code> if <code>this</code> represents the
     *  same location as <code>obj</code>, and <code>false</code> otherwise.
     */   
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LatLong) {
            LatLong ll = (LatLong) obj;
            double distance = this.calculateDistanceTo(ll);
            
            return (distance < TOLERANCE);
        } else {
    	    return false;
        }
    }
     
    /**
     * Dummy method to support the equals method. Always returns 0.
     * @return 0
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /** An output method
     *
     * @return a String representation of <code>this</code> LatLong object
     */
    @Override
    public String toString()
    {
        return "Latitude="+this.getLatitude()+", Longitude="+this.getLongitude();       
    }
    
}
