package geoling.gui.util;

import geoling.locations.util.AggregatedLocation;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * This class is a <code>JLabel</code> with a different
 * <code>getToolTipText(MouseEvent event)</code> method. This special method
 * shows the name of a location, if the mouse pointer is kept over a voronoi cell.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class VoronoiMapLabel extends JLabel {

	private static final long serialVersionUID = 5002068197186762188L;
	private ImageIcon imageIcon;
	private HashMap<Polytope, AggregatedLocation> hints;
	private float scaleFactor;
	private Polytope lastPolytope;

	/** Default constructor using constructor of super class. */
	public VoronoiMapLabel() {
		super();
	}
	
	/**
	 * Constructs the object which is used for the tool tip showing the current location.
	 * @param imageIcon the <code>ImageIcon</code> representing the drawn map
	 * @param hints the object which gives the <code>AggregatedLocation</code> for each <code>Polytope</code>
	 */
	public VoronoiMapLabel(ImageIcon imageIcon, HashMap<Polytope, AggregatedLocation> hints) {
		super(imageIcon);
		this.imageIcon = imageIcon;
		this.hints = hints;
		this.scaleFactor = 1.0f;
	}

	/**
	 * Constructs the object which is used for the tool tip showing the current location.
	 * @param imageIcon the <code>ImageIcon</code> representing the drawn map
	 * @param hints the object which gives the <code>AggregatedLocation</code> for each <code>Polytope</code>
	 * @param scaleFactor the scale factor of the map (since map can be zoomed)
	 */
	public VoronoiMapLabel(ImageIcon imageIcon, HashMap<Polytope, AggregatedLocation> hints, float scaleFactor) {
		super(imageIcon);
		this.imageIcon = imageIcon;
		this.hints = hints;
		this.scaleFactor = scaleFactor;
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		AggregatedLocation location = getLocation(event);
		if (location!=null) {
			return location.getName();
		}
		else {
			return null;
		}

	}


	/** Computes the location for the current cursor position.
	 * 
	 * @param event the <code>MouseEvent</code>
	 * @return the <code>AggregatedLocation</code>
	 */
	public AggregatedLocation getLocation(MouseEvent event) {
		int x = event.getPoint().x;
		int y = event.getPoint().y;
		if (this.getWidth() > imageIcon.getIconWidth()) {
			int diff = this.getWidth() - imageIcon.getIconWidth();
			x = x - diff / 2;
		}
		if (this.getHeight() > imageIcon.getIconHeight()) {
			int diff = this.getHeight() - imageIcon.getIconHeight();
			y = y - diff / 2;
		}
		y = imageIcon.getIconHeight() - y;
		if (x >= 0 && x <= imageIcon.getIconWidth() && y >= 0 & y <= imageIcon.getIconHeight()) {
			x = (int) (x / scaleFactor);
			y = (int) (y / scaleFactor);
			// the cursor is still in the previous voronoi cell
			if (lastPolytope != null && lastPolytope.contains(new Point(new double[] { x, y }))) {
				return hints.get(lastPolytope);
			} 
			// the cursor has moved to another voronoi cell
			else {
				Iterator<Entry<Polytope, AggregatedLocation>> it = this.hints.entrySet().iterator();
				while (it.hasNext()) {
					Entry<Polytope, AggregatedLocation> entry = it.next();
					if (entry.getKey().contains(new Point(new double[] { x, y }))) {
						lastPolytope = entry.getKey();
						return hints.get(lastPolytope);
					}
				}
			}
		}
		return null;
	}


}