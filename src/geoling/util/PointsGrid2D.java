package geoling.util;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import com.google.common.collect.MinMaxPriorityQueue;

import geoling.util.geom.PointsHelper;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Rectangle;

/**
 * Builds a grid for a set of points in 2D and provides methods
 * for finding the nearest neighbour, k nearest neighbours or all
 * neighbours in a given distance.
 * This is a generic class, <code>E</code> should be a point class
 * which is known by the <code>PointsHelper.getPointCoordinates</code>
 * method.
 * <p>
 * Note that this class is based on <code>PointsGrid3D</code> and
 * works best if the points disperse roughly uniformly on the grid.
 * <p>
 * Important: The points are assumed to be immutable, that is, you should
 * never change their coordinates after adding them to the grid!
 *
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @version 0.2, 15.11.2013
 */
public class PointsGrid2D<E> {
	
	/**
	 * A grid element, currently just a <code>LinkedList</code> with its points.
	 */
	private class GridElement extends ArrayList<E> {
		private static final long serialVersionUID = 2L;
		
		private Rectangle rect;
		
		public GridElement(int capacity, Rectangle rect) {
			super(capacity);
			this.rect = rect;
		}
	}
	
	/**
	 * Comparator for points, sorts them by their distance to a given
	 * third point.
	 * A local class that implements the <code>Comparator</code> interface
	 * and works on a collection of points, it compares them by their
	 * distance to a third point and uses an ascending order.
	 */
	private class PointDistanceComparator implements Comparator<E> {
		
		/**
		 * The point to which the distances will be calculated.
		 */
		private E fromPoint;
		
		/**
		 * Constructor using the point which is the base
		 * for the distance calculation.
		 *
		 * @param fromPoint  the point to which the distances
		 *                   will be calculated
		 */
		public PointDistanceComparator(E fromPoint) {
			this.fromPoint = fromPoint;
		}
		
		/**
		 * Compares the two points by their distance to the
		 * point given to the constructor.
		 *
		 * @param point1  the first point
		 * @param point2  the second point
		 * @return <code>-1</code>, <code>0</code> or <code>1</code> if the
		 *         distance of the first point is lower than, equal to or
		 *         higher than the distance of the second point
		 */
		public int compare(E point1, E point2) {
			return (int)Math.round(Math.signum(PointsHelper.getDistance(this.fromPoint, point1) - PointsHelper.getDistance(this.fromPoint, point2)));
		}
	}
	
	/**
	 * The target number of points per grid element.
	 * Affects the size of a single grid element.
	 */
	private static final int DEF_MEAN_NUMBER_OF_POINTS_PER_ELEMENT = 27;
	
	/**
	 * The set of all points.
	 */
	private LinkedList<E> points;
	
	/**
	 * The observation window / bounding box for all points.
	 */
	private DoubleBox box;
	
	/**
	 * The lower coordinates of the box, used to shift the point
	 * coordinates to [0, sizeX] x [0, sizeY].
	 */
	private double[] shift;
	
	/**
	 * The size of the bounding box.
	 */
	private double[] boxSize;
	
	/**
	 * Defines the scaling factors to use for point coordinates,
	 * depends on the grid resolution.
	 */
	private double[] scale;
	
	/**
	 * The size of the grid.
	 */
	private int[] size;
	
	/**
	 * The grid elements.
	 * See the <code>getGridIndex</code> methods to see how
	 * the indices are calculated.
	 */
	private ArrayList<GridElement> grid;
	
	/**
	 * Constructor using the points, the observation window (as <code>DoubleBox</code>)
	 * and the targeted mean number of points per grid element.
	 *
	 * @param points           the points
	 * @param box              the observation window (bounding box for the points)
	 * @param pointsPerElement the desired number of points per grid element
	 */
	public PointsGrid2D(Collection<E> points, DoubleBox box, int pointsPerElement) {
		this.points = null;
		this.box    = box;
		
		if (box.getDimension() != 2) {
			throw new IllegalArgumentException("PointsGrid2D expects a bounding box with dimension 2.");
		}
		
		// variables required for the grid
		this.shift    = box.getMin();
		this.boxSize  = box.getWidth();
		
		// variables required for the grid
		double gridElementTargetSideLength = Math.sqrt((double)pointsPerElement * box.getVolume() / points.size());
		this.size = new int[] { (int)Math.ceil(this.boxSize[0] / gridElementTargetSideLength),
		                        (int)Math.ceil(this.boxSize[1] / gridElementTargetSideLength) };
		this.scale = new double[] { this.size[0]/this.boxSize[0],
		                            this.size[1]/this.boxSize[1] };
		
		// initialize the grid itself
		int gridLength = this.size[0] * this.size[1];
		this.grid = new ArrayList<GridElement>(gridLength);
		for (int i = 0; i < gridLength; i++) {
			this.grid.add(null);
		}
		for (int i1 = 0; i1 < this.size[0]; i1++) {
			for (int i2 = 0; i2 < this.size[1]; i2++) {
				double[] low  = new double[] { this.shift[0]+i1/this.scale[0],
				                               this.shift[1]+i2/this.scale[1] };
				double[] high = new double[] { this.shift[0]+(i1+1)/this.scale[0],
				                               this.shift[1]+(i2+1)/this.scale[1] };
				Rectangle rect = new Rectangle((low[0]+high[0])/2.0, (low[1]+high[1])/2.0,
				                               high[0]-low[0], high[1]-low[1], 0.0);
				this.grid.set(this.getGridIndex(i1, i2), new GridElement(pointsPerElement, rect));
			}
		}
		
		// add the points to the grid
		this.points = new LinkedList<E>();
		this.add(points);
	}
	
	/**
	 * Constructor using the points and the observation window (as <code>DoubleBox</code>).
	 *
	 * @param points  the points
	 * @param box     the observation window (bounding box for the points)
	 */
	public PointsGrid2D(Collection<E> points, DoubleBox box) {
		this(points, box, DEF_MEAN_NUMBER_OF_POINTS_PER_ELEMENT);
	}
	
	/**
	 * Returns (a copy of) the list of all points.
	 *
	 * @return the list of the points
	 */
	public List<E> getPointsList() {
		return new ArrayList<E>(this.points);
	}
	
	/**
	 * Adds the given points to this grid.
	 *
	 * @param points  the points to add
	 */
	public void add(Collection<E> points) {
		for (E point : points) {
			this.add(point);
		}
	}

	/**
	 * Adds the given point to this grid.
	 *
	 * @param point  the point to add
	 */
	public void add(E point) {
		if (!this.box.contains(PointsHelper.getPointCoordinates(point))) {
			throw new RuntimeException("Bounding box doesn't contain this point!");
		}
		this.grid.get(this.getGridIndex(point)).add(point);
		this.points.add(point);
	}
	
	/**
	 * Removes the given points from this grid.
	 *
	 * @param points  the points to remove
	 */
	public void remove(Collection<E> points) {
		for (E point : points) {
			this.remove(point);
		}
	}
	
	/**
	 * Removes the given point from this grid.
	 *
	 * @param point  the point to remove
	 */
	public void remove(E point) {
		this.grid.get(this.getGridIndex(point)).remove(point);
		this.points.remove(point);
	}
	
	/**
	 * Searches the <code>k</code> nearest neighbours of the given point, the
	 * search can be restricted to a radius <code>r</code>.
	 * 
	 * @param point  the point
	 * @param k      the number of the nearest neighbours to find
	 *               (any non-positive value means no limit)
	 * @param r      the maximum distance to search in
	 *               (any non-positive value means no limit)
	 * @param sort   determines whether the result list should be
	 *               sorted by distance (ascendingly)
	 * @return the nearest neighbours or an empty list if none could be found
	 */
	public List<E> findKNearestNeighbours(E point, int k, double r, boolean sort) {
		// adjust k if not that many neighbours possible
		if ((k > 0) && (this.points.size() <= k)) {
			// set to the total number of points
			k = this.points.size();
			
			// but decrement if the point is contained in the list
			if (this.points.contains(point)) {
				k--;
			}
		}
		
		// return now if no neighbour is possible
		// (note: we don't require the point to be contained in the grid)
		if (!((this.points.size() > 1) || ((this.points.size() == 1) && !this.points.contains(point)))) {
			return new ArrayList<E>();
		}
		
		// make sure the loop below breaks
		if ((k <= 0) && (r <= 0.0)) {
			throw new IllegalArgumentException("findKNearestNeighbours: k and r may not be non-positive together!");
		}
		
		// Point3D object for the given point coordinates is required below
		Point simGrainPoint = new Point(PointsHelper.getPointCoordinates(point));
		
		// the position in the grid of this point, coordinates as a floating-point number
		int[] pointGridCoord = this.getGridCoordinates(point);
		
		// remember the already checked grid elements because we'll get a single grid
		// element several times
		boolean[][] checkedGridElements = new boolean[this.size[0]][this.size[1]];
		
		// remember the total number of already checked grid elements:
		// we have to know when we have searched all of them (and therefore, have to abort)
		int checkedGridElementsCount = 0;
		
		// construct the result object, neighbours are automatically sorted by their distance:
		// for all neighbours (in a given distance) we use the standard priority queue provided
		// by Java, for a limitation to k objects we use an implementation of the Guava library
		// (which is slower, but provides more features)
		Comparator<E> comparator = new PointDistanceComparator(point);
		AbstractQueue<E> nearestNeighbours;
		if (k > 0) {
			nearestNeighbours = MinMaxPriorityQueue.orderedBy(comparator).maximumSize(k).create();
		} else {
			nearestNeighbours = new PriorityQueue<E>(10, comparator);
		}
		
		// the relevant search radius, may be adjusted in the loop below
		double maxSearchRadius = r;
		
		// start the loop at the grid element where the original point is contained, then
		// increase the level of grid element neighbours to search
		int level = 0;
		// if the original point is outside the grid, then choose the initial level appropriately
		if (!this.validGridIndex(pointGridCoord[0], pointGridCoord[1])) {
			for (int i = 0; i < 2; i++) {
				if (pointGridCoord[i] < 0) {
					level = Math.max(level, -pointGridCoord[i]);
				}
				if (pointGridCoord[i] >= this.size[i]) {
					level = Math.max(level, pointGridCoord[i]-this.size[i]+1);
				}
			}
		}
		while (true) {
			boolean somethingDone = false;
			
			for (int i1 = pointGridCoord[0]-level; i1 <= pointGridCoord[0]+level; i1++) {
				for (int i2 = pointGridCoord[1]-level; i2 <= pointGridCoord[1]+level; i2++) {
					Rectangle gridElementCuboid = null;
					
					if (this.validGridIndex(i1, i2) && !checkedGridElements[i1][i2]) {
						// we are in a grid element not already checked
						GridElement ge = this.grid.get(this.getGridIndex(i1, i2));
						if (gridElementCuboid == null) {
							gridElementCuboid = ge.rect;
						}
						
						// is it in a relevant distance?
						if ((maxSearchRadius <= 0.0) || (exteriorDistanceToBoundary(gridElementCuboid, simGrainPoint) < maxSearchRadius+Utilities.EPS)) {
							// yes, so check its points
							for (E e : ge) {
								if (e == point) {
									// the point whose neighbours are searched shouldn't be returned
									continue;
								}
								
								// relevant points: add to sorted nearest neighbour list
								if ((maxSearchRadius <= 0.0) || (PointsHelper.getDistance(point, e) < maxSearchRadius)) {
									nearestNeighbours.add(e);
									// if we want only a given number of nearest neighbours, we try
									// to use this information to restrict the search radius
									// (because a nearer neighbour has to be closer, of course)
									if ((k > 0) && (nearestNeighbours.size() == k)) {
										double neighbourMaxDist = PointsHelper.getDistance(point, ((MinMaxPriorityQueue<?>)nearestNeighbours).peekLast());
										if ((maxSearchRadius <= 0.0) || (maxSearchRadius > neighbourMaxDist)) {
											maxSearchRadius = neighbourMaxDist;
										}
									}
								}
							}
							
							checkedGridElements[i1][i2] = true;
							checkedGridElementsCount++;
							somethingDone = true;
						}
					}
				}
			}
			
			if (!somethingDone) {
				// we didn't check any of the grid elements, because they are all too far away,
				// therefore we are done
				break;
			}
			
			if (checkedGridElementsCount >= this.grid.size()) {
				// all grid elements processed, there cannot exist more points...
				break;
			}
			
			level++;
		}
		
		// take all detected neighbours and return them after sorting, because
		// the priority queue doesn't guarantee an order
		ArrayList<E> result = new ArrayList<E>(nearestNeighbours);
		if (sort) {
			Collections.sort(result, comparator);
		}
		return result;
	}
	
	/**
	 * Searches the <code>k</code> nearest neighbours of the given point.
	 *
	 * @param point  the point
	 * @param k      the number of the nearest neighbours to find
	 * @param sort   determines whether the result list should be
	 *               sorted by distance (ascendingly)
	 * @return the nearest neighbours or an empty list if none could be found
	 */
	public List<E> findKNearestNeighbours(E point, int k, boolean sort) {
		return this.findKNearestNeighbours(point, k, 0.0, sort);
	}
	
	/**
	 * Searches the neighbours of the given point up to a specified
	 * distance <code>r</code>.
	 *
	 * @param point  the point
	 * @param r      the maximum of the radius (and thus the maximum
	 *               distance of the neighbours)
	 * @param sort   determines whether the result list should be
	 *               sorted by distance (ascendingly)
	 * @return the nearest neighbours or an empty list if none could be found
	 */
	public List<E> findNeighboursInDistance(E point, double r, boolean sort) {
		return this.findKNearestNeighbours(point, 0, r, sort);
	}
	
	/**
	 * Searches the nearest neighbour of the given point.
	 *
	 * @param point  the point
	 * @return the nearest neighbour or <code>null</code> if none was found
	 */
	public E findNearestNeighbour(E point) {
		List<E> neighbours = this.findKNearestNeighbours(point, 1, false);
		if (neighbours.isEmpty()) {
			return null;
		} else {
			return neighbours.get(0);
		}
	}
	
	/**
	 * Searches the nearest neighbour of the given point and returns
	 * the distance.
	 *
	 * @param point  the point
	 * @return the distance to the nearest neighbour or (positive) infinity
	 *         if no neighbour exists
	 */
	public double findNearestNeighbourDistance(E point) {
		E nearestNeighbour = this.findNearestNeighbour(point);
		if (nearestNeighbour == null) {
			return Double.POSITIVE_INFINITY;
		} else {
			return PointsHelper.getDistance(point, nearestNeighbour);
		}
	}
	
	/**
	 * Calculates the coordinates in the grid for the given point.
	 *
	 * @param point  the point
	 * @return the coordinates as an array of length 2
	 */
	private int[] getGridCoordinates(E point) {
		return this.getGridCoordinates(PointsHelper.getPointCoordinates(point));
	}
	
	/**
	 * Calculates the coordinates in the grid for the given point,
	 * specified by its coordinates.
	 *
	 * @param coord  the coordinates of the point
	 * @return the coordinates as an array of length 2
	 */
	private int[] getGridCoordinates(double[] coord) {
		return this.getGridCoordinates(coord[0], coord[1]);
	}
	
	/**
	 * Calculates the coordinates in the grid for the given point,
	 * specified by its coordinates.
	 *
	 * @param x  the x-coordinate of the point
	 * @param y  the y-coordinate of the point
	 * @return the coordinates as an array of length 2
	 */
	private int[] getGridCoordinates(double x, double y) {
		int gridX = (int)((x - this.shift[0]) * this.scale[0]);
		int gridY = (int)((y - this.shift[1]) * this.scale[1]);
		return new int[] { gridX, gridY };
	}
	
	/**
	 * Calculates the index of the grid element for the given point.
	 *
	 * @param point  the point
	 * @return the index of the grid element
	 */
	private int getGridIndex(E point) {
		return this.getGridIndex(this.getGridCoordinates(point));
	}
	
	/**
	 * Calculates the index of the grid element for the given grid
	 * coordinates.
	 *
	 * @param gridCoord  the coordinates in the grid
	 * @return the index of the grid element
	 */
	private int getGridIndex(int[] gridCoord) {
		return this.getGridIndex(gridCoord[0], gridCoord[1]);
	}
	
	/**
	 * Calculates the index of the grid element for the given grid
	 * coordinates.
	 *
	 * @param gridX  the x-coordinate in the grid
	 * @param gridY  the y-coordinate in the grid
	 * @return the index of the grid element
	 */
	private int getGridIndex(int gridX, int gridY) {
		if (!this.validGridIndex(gridX, gridY)) {
			throw new IndexOutOfBoundsException();
		}
		return (gridX + gridY * this.size[0]);
	}
	
	/**
	 * Checks that the given grid coordinates are valid.
	 *
	 * @param gridX  the x-coordinate in the grid
	 * @param gridY  the y-coordinate in the grid
	 * @return <code>true</code> if the coordinates are valid
	 */
	private boolean validGridIndex(int gridX, int gridY) {
		return ((gridX >= 0) && (gridY >= 0) && (gridX < this.size[0]) && (gridY < this.size[1]));
	}
	
	/**
	 * Computes the minimum distance from an arbitrary point to the
	 * given rectangle.
	 * 
	 * @param rect   the rectangle
	 * @param point  the point
	 * @return the distance to the nearest point on the surface or zero
	 *         if the point is contained in the cuboid
	 */
	private static double exteriorDistanceToBoundary(Rectangle rect, Point point) {
		double[] low  = new double[] { rect.getCenter().getCoordinates()[0]-rect.getWidth()/2.0, rect.getCenter().getCoordinates()[1]-rect.getHeight()/2.0 };
		double[] high = new double[] { rect.getCenter().getCoordinates()[0]+rect.getWidth()/2.0, rect.getCenter().getCoordinates()[1]+rect.getHeight()/2.0 };
		double[] coord = point.getCoordinates();
		
		double[] nearest = new double[2];
		for (int i = 0; i < 2; i++) {
			nearest[i] = coord[i];
			if (nearest[i] < low[i]) nearest[i] = low[i];
			if (nearest[i] > high[i]) nearest[i] = high[i];
		}
		
		return Point.distance(coord, nearest);
	}
	
}