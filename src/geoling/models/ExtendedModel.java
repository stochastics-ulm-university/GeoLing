package geoling.models;

import org.javalite.activejdbc.Model;

/**
 * Base model class (used in all other model classes), provides a hash code and
 * <code>equals</code< method and caches the record ID object for faster access.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @see <a href="http://code.google.com/p/activejdbc/wiki/Inheritance">Inheritance support in activejdbc</a>
 */
public abstract class ExtendedModel extends Model {
	
	/** Cached ID object. */
	protected Object cachedId = null;
	
	/**
	 * Returns the ID object of this record.
	 * 
	 * @return the ID object or <code>null</code> if this record is not yet saved
	 */
	@Override
	public Object getId() {
		if (this.cachedId == null) {
			this.cachedId = super.getId();
		}
		return this.cachedId;
	}
	
	/**
	 * Returns a hash code.
	 * Uses the class name and record ID for computing the hash value, therefore
	 * this should be mainly used after the record has been saved!
	 * 
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return this.getClass().hashCode()+this.getId().hashCode();
	}
	
	/**
	 * Implements a check for equality of two <code>Model</code> objects.
	 * Note that this method compares the class and the record ID, therefore
	 * it works only for saved objects. If one of the objects is unsaved,
	 * this method returns only <code>true</code> if the two object references
	 * themselves are identical.
	 * 
	 * @param other  the second model object
	 * @return <code>true</code> if the two model objects are equal
	 */
	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		return (this == other) || (((this.getClass() == other.getClass()) && (this.getId() != null) && (this.getId().equals(((Model)other).getId()))));
	}
	
}