package geoling.gui.util;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.AreaClassMap;
import geoling.models.Variant;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;

import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * This class is a <code>JLabel</code> with a different
 * <code>getToolTipText(MouseEvent event)</code> method. This special method
 * shows the name of a location and the most probable variants, if the mouse pointer is kept over a voronoi
 * cell.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class AreaClassMapLabel extends JLabel {

	private static final long serialVersionUID = 5002068197186762188L;
	private ImageIcon imageIcon;
	private HashMap<Polytope, AggregatedLocation> hints;
	private AreaClassMap areaClassMap;
	private float scaleFactor;
	private Polytope lastPolytope;

	/** Default constructor using constructor of super class. */
	public AreaClassMapLabel() {
		super();
	}

	/**
	 * Constructs the object which is used for the tooltip showing the current location and the most probable variants.
	 * @param imageIcon the <code>ImageIcon</code> representing the drawn map
	 * @param hints the object which gives the <code>AggregatedLocation</code> for each <code>Polytope</code>
	 * @param areaClassMap the <code>AreaClassMap</code> to get the most probable variants for each location
	 * @param scaleFactor the scale factor of the map (since map can be zoomed)
	 */
	public AreaClassMapLabel(ImageIcon imageIcon, HashMap<Polytope, AggregatedLocation> hints, AreaClassMap areaClassMap, float scaleFactor) {
		super(imageIcon);
		this.imageIcon = imageIcon;
		this.hints = hints;
		this.areaClassMap = areaClassMap;
		this.scaleFactor = scaleFactor;
	}

	@Override
	public String getToolTipText(MouseEvent event) {
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
				return getToolTipText(hints.get(lastPolytope), areaClassMap);
			} 
			// the cursor has moved to another voronoi cell
			else {
				lastPolytope = null;
				Iterator<Entry<Polytope, AggregatedLocation>> it = this.hints.entrySet().iterator();
				while (it.hasNext()) {
					Entry<Polytope, AggregatedLocation> entry = it.next();
					if (entry.getKey().contains(new Point(new double[] { x, y }))) {
						lastPolytope = entry.getKey();
						return getToolTipText(hints.get(lastPolytope), areaClassMap);
					}
				}
			}
		}
		return null;

	}

	/**
	 * Gives a useful <code>String</code> for the selected location.
	 * @param location the location of the cursor
	 * @param areaClassMap the area class map
	 * @return a <code>String</code> with html code giving the name of the location and the five most probable variants
	 */
	private String getToolTipText(final AggregatedLocation location, final AreaClassMap areaClassMap) {
		ArrayList<Variant> sortedVariants = new ArrayList<Variant>(areaClassMap.getVariantWeights().getVariants());
		Collections.sort(sortedVariants, new Comparator<Variant>() {

			@Override
			public int compare(Variant o1, Variant o2) {
				Double d1 = areaClassMap.getVariantDensity(o1, location);
				Double d2 = areaClassMap.getVariantDensity(o2, location);
				return -d1.compareTo(d2);
			}
		});

		String result = "<html><b>" + location.getName() + "</b>";
		DecimalFormat df = new DecimalFormat("0.000");
		int counter = 1;
		for (Variant variant : sortedVariants) {
			if (counter <= 5) {
				double value = areaClassMap.getVariantDensity(variant, location);
				if (value > 0.0) {
					result += "<br>" + variant.getString("name") + ": " + df.format(value);
					counter++;
				}
			} else {
				break;
			}
		}
		return result + "</html>";
	}

}