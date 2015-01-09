package geoling.maps.util;

import java.util.ArrayList;

import geoling.maps.projection.MapProjection;
import geoling.util.DoubleBox;
import geoling.util.LatLong;
import geoling.util.ThreadedTodoWorker;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.util.RandomSet;

/**
 * A rectangular grid for the maps, which is used to compute/plot
 * continuous maps (grid points instead of locations / their Voronoi cell).
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class RectangularGrid {
	
	/** The default number of grid points for the smallest axis of the border polygon. */
	public static int DEFAULT_GRID_WIDTH = 150;
	
	/** A single point on the rectangular grid. */
	public class GridPoint {
		private LatLong latLong;
		private Polytope latLongRect;
		
		private GridPoint(LatLong latLong, Polytope latLongRect) {
			this.latLong     = latLong;
			this.latLongRect = latLongRect;
		}
		
		public LatLong getLatLong() {
			return latLong;
		}
		
		public Polytope getLatLongRect() {
			return latLongRect;
		}
		
		public RandomSet getLatLongRectIntersected() {
			return borderIntersection.intersect(latLongRect);
		}
	}
	
	/** The border polygon of the map. */
	private Polytope border;
	
	/** Helper for intersection of cells with the border polygon. */ 
	private MapBorderIntersection borderIntersection;
	
	/** The map projection method. */
	private MapProjection mapProjection;
	
	/** The resolution (minimal distance between grid points) w.r.t. the map projection. */
	private double resolution;
	
	/** The list of grid points. */
	private ArrayList<GridPoint> gridPoints;
	
	/**
	 * Constructs a grid for the given border polygon with the specified
	 * map projection method and resolution.
	 * 
	 * @param border        the border polygon of the map
	 * @param mapProjection the map projection method to use
	 * @param resolution    the resolution (minimal distance between
	 *                      grid points) w.r.t. positions given by the
	 *                      map projection method, if <code>0.0</code> or
	 *                      smaller, then a default value is used
	 */
	public RectangularGrid(Polytope border, MapProjection mapProjection, double resolution) {
		this.border             = border;
		this.borderIntersection = MapBorderIntersectionCache.getHelperObject(border);
		
		if (resolution <= 0.0) {
			DoubleBox box = mapProjection.projectLatLong(border).getBoundingBox();
			resolution = Math.min(box.getWidth(0), box.getWidth(1)) / DEFAULT_GRID_WIDTH;
		}
		this.mapProjection = mapProjection;
		this.resolution    = resolution;
		
		final MapBorderIntersection borderIntersection = new MapBorderIntersection(border);
		
		final double[] min = mapProjection.projectLatLong(border.getBoundingBox().getMin());
		final double[] max = mapProjection.projectLatLong(border.getBoundingBox().getMax());
		final int iMax = (int)Math.ceil((max[0]-min[0])/resolution);
		final int jMax = (int)Math.ceil((max[1]-min[1])/resolution);
		
		// objects only used for parallelization below
		final MapProjection finalMapProjection = mapProjection;
		final double finalResolution = resolution;
		
		// build grid points with their rectangles, save them into 2D-array
		final GridPoint[][] gridPointsArray = new GridPoint[iMax+1][jMax+1];
		ThreadedTodoWorker.workOnIndices(0, iMax, 1, new ThreadedTodoWorker.SimpleTodoWorker<Integer>() {
			public void processTodoItem(Integer iObj) {
				int i = iObj.intValue();
				double x = min[0] + i*finalResolution;
				
				for (int j = 0; j <= jMax; j++) {
					double y = min[1] + j*finalResolution;
					
					double[] latLong = finalMapProjection.revertProjection(new double[] { x, y });
					
					// generate rectangle for grid element
					Point p1 = new Point(new double[] { x-finalResolution/2.0, y-finalResolution/2.0 });
					Point p2 = new Point(new double[] { x+finalResolution/2.0, y-finalResolution/2.0 });
					Point p3 = new Point(new double[] { x+finalResolution/2.0, y+finalResolution/2.0 });
					Point p4 = new Point(new double[] { x-finalResolution/2.0, y+finalResolution/2.0 });
					Polytope gridRectXY = new Polytope(new Point[] { p1, p2, p3, p4 });
					Polytope gridRect = finalMapProjection.revertProjection(gridRectXY);
					
					// add only if border polygon and grid element intersect
					boolean partiallyContained = false;
					try {
						partiallyContained = (borderIntersection.intersect(gridRect, null) != null);
					} catch (MapBorderIntersection.IntersectionConsistsOfSeveralPolygons e) {
						partiallyContained = true;
					} catch (RuntimeException e) {
						// numerical issues? ignore
					}
					if (partiallyContained) {
						GridPoint gridPoint = new GridPoint(new LatLong(latLong[0], latLong[1]), gridRect);
						gridPointsArray[i][j] = gridPoint;
					}
				}
			}
		});
		
		// collect grid points that are not null
		// (note that this implementation guarantees a certain order of the grid points,
		//  but this behavior may change in the future)
		this.gridPoints = new ArrayList<GridPoint>((iMax+1)*(jMax+1));
		for (int i = 0; i < gridPointsArray.length; i++) {
			for (int j = 0; j < gridPointsArray[i].length; j++) {
				if (gridPointsArray[i][j] != null) {
					this.gridPoints.add(gridPointsArray[i][j]);
				}
			}
		}
	}
	
	/**
	 * Constructs a grid for the given border polygon with the specified
	 * map projection method and a default resolution.
	 * 
	 * @param border        the border polygon of the map
	 * @param mapProjection the map projection method to use
	 */
	public RectangularGrid(Polytope border, MapProjection mapProjection) {
		this(border, mapProjection, 0.0);
	}
	
	/**
	 * Returns the border polygon of the map.
	 * 
	 * @return the border polygon
	 */
	public Polytope getBorder() {
		return this.border;
	}
	
	/**
	 * Returns the used map projection method.
	 * 
	 * @return the map projection method
	 */
	public MapProjection getMapProjection() {
		return this.mapProjection;
	}
	
	/**
	 * Returns the resolution w.r.t. the map projection.
	 * 
	 * @return the resolution w.r.t. the map projection
	 */
	public double getResolution() {
		return this.resolution;
	}
	
	/**
	 * Returns all grid points.
	 * 
	 * @return all grid points
	 */
	public ArrayList<GridPoint> getGridPoints() {
		return this.gridPoints;
	}
	
}