package geoling.locations.util;

import geoling.locations.LocationAggregator;
import geoling.models.Location;
import geoling.util.LatLong;
import geoling.util.vendor.HumaneStringComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * An aggregated location replaces a set of locations. Locations should be
 * aggregated via the <code>LocationAggregator</code> interface, which also
 * defines the conditions which have to be followed. In particular, you should
 * not directly create <code>AggregatedLocation</code> objects, but use (or
 * implement) a class implementing <code>LocationAggregator</code>. 
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @see geoling.locations.LocationAggregator
 */
public class AggregatedLocation implements Comparable<AggregatedLocation> {
	
	/** The aggregator which created this object. */
	private LocationAggregator aggregator;
	
	/** The set of locations that are aggregated. */
	private ArrayList<Location> locations;
	
	/** The ID of this aggregation object, it is always the smallest ID of the locations it stands for. */
	private long id;
	
	/** A name of the aggregation. */
	private String name;
	
	/** A short name (or number) of the aggregation. */
	private String code;
	
	/** The geographical coordinates of the aggregation, e.g. the geographical centre. */
	private LatLong latLong;
	
	/** The precomputed hash-code of this object, for fast access. */
	private int hashCode;
	
	/**
	 * Constructs a new aggregation of locations.
	 * 
	 * @param locations  the set of locations
	 */
	public AggregatedLocation(LocationAggregator aggregator, Collection<Location> locations) {
		this(aggregator, locations, null, null, null);
	}
	
	/**
	 * Constructs a new aggregation of locations.
	 * 
	 * @param locations the set of locations
	 * @param name      the name of the aggregation, may be <code>null</code> (then the
	 *                  locations' names will be concatenated)
	 * @param code      the short name (or number) of the aggregation, may be <code>null</code>
	 *                  (then the locations' codes will be concatenated)
	 * @param latLong   the geographical coordinates of the aggregation, e.g. the geographical
	 *                  centre, may be <code>null</code> (then the locations' coordinates
	 *                  centre will be computed)
	 */
	public AggregatedLocation(LocationAggregator aggregator, Collection<Location> locations, String name, String code, LatLong latLong) {
		if (locations.isEmpty()) {
			throw new IllegalArgumentException("The set of locations may not be empty!");
		}
		this.aggregator = aggregator;
		this.locations  = new ArrayList<Location>(locations);
		this.name       = name;
		this.code       = code;
		this.latLong    = latLong;
		
		// ensure that the locations have always the same order, but this isn't really
		// important (and should not be relied upon)
		Collections.sort(this.locations);
		
		// use the smallest ID of the locations as ID for this aggregation
		this.id       = Long.MAX_VALUE;
		this.hashCode = 0;
		for (Location location : this.locations) {
			if (this.id > location.getLongId()) {
				this.id = location.getLongId();
			}
			this.hashCode += location.hashCode();
		}
		
		// use default values for empty fields (name, code or latLong)
		
		if ((this.name == null) || (this.code == null)) {
			if (this.name == null) {
				for (Location location : this.locations) {
					if (this.name == null) {
						this.name = location.getString("name");
					} else {
						this.name += ", "+location.getString("name");
					}
				}
			}
			
			if (this.code == null) {
				for (Location location : this.locations) {
					// note: code of the location object may be empty
					if ((location.getString("code") != null) && !location.getString("code").isEmpty()) {
						if (this.code == null) {
							this.code = location.getString("code");
						} else {
							this.code += ", "+location.getString("code");
						}
					}
				}
			}
		}
		
		if (this.name == null) {
			throw new IllegalArgumentException("Could not auto-construct a name for the location aggregation, the locations have no names!");
		}
		
		if (this.code == null) {
			// behave the same way as a Location object, which may have an empty code
			this.code = "";
		}
		
		if (this.latLong == null) {
			// just use the arithmetic mean of the coordinates as centre
			double[] latLongSum = new double[] { 0.0, 0.0 };
			int n = 0;
			for (Location location : this.locations) {
				latLongSum[0] += location.getLatLong().getLatitude();
				latLongSum[1] += location.getLatLong().getLongitude();
				n++;
			}
			this.latLong = new LatLong(latLongSum[0]/n, latLongSum[1]/n);
		}
	}
	
	/**
	 * Returns the aggregator, which created this object.
	 * 
	 * @return the aggregator, which created this object
	 */
	public LocationAggregator getLocationAggregator() {
		return this.aggregator;
	}
	
	/**
	 * Returns the set of locations that are aggregated.
	 * 
	 * @return the set of locations that are aggregated
	 */
	public List<Location> getLocations() {
		return Collections.unmodifiableList(this.locations);
	}
	
	/**
	 * Returns the ID of this aggregation object, it is always the smallest ID
	 * of the locations it stands for.
	 * 
	 * @return the ID of this aggregation object, it is unique with respect to
	 *         the class implementing <code>LocationAggregator</code> that created
	 *         this object
	 */
	public long getId() {
		return this.id;
	}
	
	/**
	 * Returns the name of this aggregation.
	 * 
	 * @return the name of this aggregation
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Returns the short name (or number) of this aggregation.
	 * 
	 * @return the short name (or number) of this aggregation
	 */
	public String getCode() {
		return this.code;
	}
	
	/**
	 * Returns the geographical coordinates of this aggregation.
	 * 
	 * @return the geographical coordinates of this aggregation
	 */
	public LatLong getLatLong() {
		return this.latLong;
	}
	
	/**
	 * Returns a hash code value for this object.
	 * 
	 * @return a hash code value for this object
	 */
	@Override
	public int hashCode() {
		return this.hashCode;
	}
	
	/**
	 * Indicates whether some other object is "equal to" this one.
	 * 
	 * @return <code>true</code> if the other object is also an aggregated
	 *         location and it stands for the same locations
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AggregatedLocation) {
			if (this.hashCode() != obj.hashCode()) {
				return false;
			}
			
			List<Location> otherLocations = ((AggregatedLocation)obj).getLocations();
			if (this.locations.size() != otherLocations.size()) {
				return false;
			}
			return (this.locations.containsAll(otherLocations) && otherLocations.containsAll(this.locations));
		} else {
			return false;
		}
	}
	
	/**
	 * Compares this object with the specified object for order. Returns a negative
	 * integer, zero, or a positive integer as this object is less than, equal to,
	 * or greater than the specified object.
	 * 
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 */
	public int compareTo(AggregatedLocation other) {
		return HumaneStringComparator.DEFAULT.compare(this.getName(), other.getName());
	}
	
	/**
	 * Returns a string representation of this object.
	 * 
	 * @return a string representation of the object
	 */
	@Override
	public String toString() {
		return "AggregatedLocation, attributes: {id="+this.getId()+", code="+this.getCode()+", name="+this.getName()+", latitude="+this.getLatLong().getLatitude()+", longitude="+this.getLatLong().getLongitude()+"}";
	}
	
}
