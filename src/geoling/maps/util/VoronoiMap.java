package geoling.maps.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import geoling.locations.util.AggregatedLocation;
import geoling.maps.projection.MapProjection;
import geoling.util.Conversion;
import geoling.util.DoubleBox;
import geoling.util.LatLong;
import geoling.util.ThreadedTodoWorker;
import geoling.util.Utilities;
import geoling.util.geom.Geometry2D;
import geoling.util.sim.grain.ConvexPolytope;
import geoling.util.sim.grain.LineSegment;
import geoling.util.sim.grain.MarkedPoint;
import geoling.util.sim.grain.Point;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.util.RandomSet;
import geoling.util.tessellation.LaguerreDelaunayTessellation2D;
import geoling.util.tessellation.LaguerreTessellation2D;

/**
 * Class that creates the Voronoi cells according to the locations and allows
 * to check if two cells are neighbours (they have a common edge).
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @author Lisa Handl, Institute of Stochastics, Ulm University
 */
public class VoronoiMap {
	
	/** A single cell for a location. */
	public class LocationCell implements Comparable<LocationCell> {
		private AggregatedLocation location;
		private Geometry2D.Point point;
		private Polytope voronoiCell;
		
		public LocationCell(AggregatedLocation location, Geometry2D.Point point, int pointsArrayIndex) {
			this.location = location;
			this.point = point;
			this.voronoiCell = null;
		}
		public AggregatedLocation getLocation() {
			return location;
		}
		public Geometry2D.Point getPoint() {
			return point;
		}
		public Polytope getVoronoiCell() {
			return voronoiCell;
		}
		public int compareTo(LocationCell obj) {
			return (int)(this.location.getId()-obj.location.getId());
		}
	}
	
	/** The border polygon of this Voronoi map. */
	private Polytope border;
	
	/** Helper for intersection of cells with the border polygon. */ 
	private MapBorderIntersection borderIntersection;
	
	/** The window (min/max-coordinates) of this Voronoi map. */
	private DoubleBox window;
	
	/** The locations for which this Voronoi map was constructed. */
	private ArrayList<AggregatedLocation> locations;
	
	/** The map projection method used to project the coordinates to a plane. */
	private MapProjection mapProjection;
	
	/** The cells of this Voronoi map. */
	private ArrayList<LocationCell> locationCells;
	
	/** Mapping of location to internal array index. */
	private HashMap<AggregatedLocation,Integer> locationToArrayIndex;
	
	/** Array containing the separating edge for each pair of cells or null if the cells aren't neighbors*/
	private LineSegment[][] edges;
	
	/**
	 * Constructs a Voronoi map for the given locations, uses the given border.
	 * Note that locations outside the polygon defined by the border are ignored.
	 * 
	 * @param locations     the set of (aggregated) locations
	 * @param border        the border polygon
	 * @param mapProjection the projection method for the coordinates
	 */
	public VoronoiMap(Collection<AggregatedLocation> locations, Polytope border, final MapProjection mapProjection) {
		this.locations          = new ArrayList<AggregatedLocation>(locations);
		this.border             = border;
		this.borderIntersection = null;
		this.window             = MapBorder.getWindow(border);
		this.mapProjection      = mapProjection;
		
		// note that we use all locations for the points array, but the cells
		// are only created for those within the border polytope
		LinkedList<Geometry2D.Point> xyPointsList = new LinkedList<Geometry2D.Point>();
		
		// initialize cell array with locations that are inside the border polytope
		// and save the locations with x-y-coordinates into an array (locations not
		// inside the polytope at the end)
		this.locationCells = new ArrayList<LocationCell>(locations.size());
		int m = 0;
		for (AggregatedLocation location : locations) {
			LatLong latLong = location.getLatLong();
			Geometry2D.Point point = Conversion.toGeom2DPoint(latLong);
			Point simGrainPoint = Conversion.toSimGrainPoint(latLong);
			if (border.contains(simGrainPoint) || MapBorderIntersection.isPointOnEdge(border, simGrainPoint)) {
				this.locationCells.add(new LocationCell(location, point, m));
				xyPointsList.add(mapProjection.projectLatLong(point));
				m++;
			} else {
				System.out.println("Warning in VoronoiMap: location with ID "+location.getId()+" ("+location.getName()+") ignored, not contained in border polygon.");
			}
		}
		
		// sorting the cells w.r.t. their location id is not necessary, but maybe more intuitive...
		Collections.sort(this.locationCells);
		// save the indices in locationToArrayIndex
		// and generate list of "marked points" in same order for DelaunayTesselation (LaguerreDelauney, marks = 1)
		this.locationToArrayIndex = new HashMap<AggregatedLocation,Integer>();
		ArrayList<MarkedPoint> xyPointsMarked = new ArrayList<MarkedPoint>(xyPointsList.size());
		for (int i = 0; i < this.locationCells.size(); i++) {
			this.locationToArrayIndex.put(this.locationCells.get(i).getLocation(), new Integer(i));
			double[] tempCoord = mapProjection.projectLatLong(this.locationCells.get(i).getLocation().getLatLong());
			xyPointsMarked.add(new MarkedPoint(tempCoord, 1.0));
		}
		
		// namespace for computing (approximate) neighbourhood of cells;
		final boolean[][] roughNeighbourhood = new boolean[locationCells.size()][locationCells.size()];
		try {
			RandomSet ldt = LaguerreDelaunayTessellation2D.computeLaguerreDelaunayTessellation(xyPointsMarked, true, 1E-9);
			for (Iterator<?> it = ldt.iterator(); it.hasNext(); ) {
				Point[] vertices = ((ConvexPolytope) it.next()).getVertices();
				for (int i = 0; i < vertices.length; i++) {
					int index1 = xyPointsMarked.indexOf(new MarkedPoint(vertices[i].getCoordinates(), 1.0));
					int index2 = xyPointsMarked.indexOf(new MarkedPoint(vertices[(i+1)%vertices.length].getCoordinates(), 1.0));
					roughNeighbourhood[index1][index2] = true;
					roughNeighbourhood[index2][index1] = true;
				}
			}
		} catch (RuntimeException e) {
			// Delaunay tessellation runtime exception
			// => fallback: just set everything in "roughNeighbourhood" to true, code below will be slow!
			for (int i = 0; i < roughNeighbourhood.length; i++) {
				for (int j = 0; j < roughNeighbourhood[i].length; j++) {
					roughNeighbourhood[i][j] = (i != j);
				}
			}
		}
		
		// note: "roughNeighbourhood" may still contain "false" neighbors due to edge effects
		//       (but, for for all real neighbours (i,j) it holds that roughNeighbourhood[i][j]==true)
		
		// check that every location has at least one neighbour
		// (otherwise, there is some problem in the Laguerre-Delaunay construction, or, more precisely, in the set of points)
		for (int i = 0; i < roughNeighbourhood.length; i++) {
			boolean found = false;
			for (int j = 0; j < roughNeighbourhood[i].length; j++) {
				if (roughNeighbourhood[i][j]) {
					found = true;
					break;
				}
			}
			if (!found) {
				throw new RuntimeException("Problem detected: every location must have at least one neighbour in the Voronoi diagram. Maybe there are two locations with the same geographical coordinates?");
			}
		}
		
		// namespace for computing the Voronoi cells
		{
			final ArrayList<MarkedPoint> xyPointsMarkedFinal = new ArrayList<MarkedPoint>(xyPointsMarked);
			ConvexPolytope borderConvexHull = MapBorder.getConvexHull(this.border);
			final Geometry2D.ConvexPolygon xyBorder = mapProjection.projectLatLong(borderConvexHull).toPolygon();
			final boolean intersectRequired = !Utilities.isEqual(borderConvexHull.getArea(), this.border.getArea());
			if (intersectRequired) {
				extendPolygonByFactor15(xyBorder);
				this.borderIntersection = MapBorderIntersectionCache.getHelperObject(border);
			}
			
			ThreadedTodoWorker.workOnTodoList(this.locationCells, new ThreadedTodoWorker.SimpleTodoWorker<LocationCell>() {
				public void processTodoItem(LocationCell locationCell) {
					// get Index and determine (potential) neighbors
					int i = locationToArrayIndex.get(locationCell.getLocation());
					ArrayList<MarkedPoint> neighbours = new ArrayList<MarkedPoint>();
					for (int j = 0; j < roughNeighbourhood[0].length; j++) {
						if (roughNeighbourhood[i][j]) {
							neighbours.add(xyPointsMarkedFinal.get(j));
						}
					}
					// build Voronoi cell
					Geometry2D.ConvexPolygon xyVoronoiCell = LaguerreTessellation2D.constructLaguerreCell(xyPointsMarkedFinal.get(i),
					                                                                                      xyBorder, neighbours);
					ConvexPolytope voronoiCell = mapProjection.revertProjection(xyVoronoiCell.toPolytope(true));
					
					if (intersectRequired) {
						// check if the bounding box of the cell is completely contained in the border polygon, then
						// no intersection is necessary (uses the (scaled) java.awt.Polygon object)
						locationCell.voronoiCell = borderIntersection.intersect(voronoiCell, locationCell.getLocation().getLatLong());
						if (locationCell.voronoiCell == null) {
							throw new RuntimeException("Intersection of Voronoi cell and border polygon is empty!");
						}
					} else {
						locationCell.voronoiCell = new Polytope(voronoiCell.getVertices(), true);
					}
				}
			});
		}
		
		// namespace for collecting (inner) edges of cells and determining exact neighbourhoods
		{
			edges = new LineSegment[roughNeighbourhood.length][roughNeighbourhood.length];
			for (int i = 0; i < roughNeighbourhood.length; i++) {
				for (int j = 0; j < i; j++) {
					if (roughNeighbourhood[i][j]) {
						Polytope cell1 = locationCells.get(i).voronoiCell;
						Polytope cell2 = locationCells.get(j).voronoiCell;
						ArrayList<Geometry2D.LineSegment> commonSegments = new ArrayList<Geometry2D.LineSegment>();
						for (Iterator<?> it1 = cell1.getEdges().iterator(); it1.hasNext(); ) {
							LineSegment a = (LineSegment) it1.next();
							Point aStart = a.getStartPoint();
							Point aEnd = a.getEndPoint();
							Geometry2D.LineSegment a_geo2D = new Geometry2D.LineSegment(aStart.toGeo2DPoint(), 
																						aEnd.toGeo2DPoint());
							for (Iterator<?> it2 = cell2.getEdges().iterator(); it2.hasNext(); ) {
								LineSegment b = (LineSegment) it2.next();
								Point bStart = b.getStartPoint();
								Point bEnd = b.getEndPoint();
								Geometry2D.LineSegment b_geo2D = new Geometry2D.LineSegment(bStart.toGeo2DPoint(), 
										  													bEnd.toGeo2DPoint());
								try {
									Geometry2D.GeometricObject intersection = Utilities.intersectSegments(a_geo2D, b_geo2D);
									// cells are adjacent only if the intersection is a line segment
									
									if (intersection instanceof Geometry2D.LineSegment) {
										// we want to be able to assume that two adjacent cells have the same line
										// segment, i.e., the same start/end points, this has to be true for
										// Voronoi cells!
										commonSegments.add((Geometry2D.LineSegment) intersection);
									}
								} catch (IllegalArgumentException exception) {
									// exception occurs e.g. if Utilities.intersectSegments tries to construct a line segment
									// with identical start and end point
								}
							}
						}
						if (commonSegments.size() == 1) {
							LineSegment segment = commonSegments.get(0).toLineSegment(); 
							edges[i][j] = segment;
							edges[j][i] = segment;
						} else if (commonSegments.size() > 1) {
							// due to numerical problems (e.g., intersection with non-convex border polygon) there may
							// be two or more line segments between a single pair of cells - we just use one large
							// segment instead of them
							
							// collect start and endpoints
							ArrayList<Geometry2D.Point> endPoints = new ArrayList<Geometry2D.Point>();
							for (Geometry2D.LineSegment segment : commonSegments) {
								endPoints.add(segment.p1);
								endPoints.add(segment.p2);
							}
							// compute points with max distance
							double maxDist = Double.NEGATIVE_INFINITY;
							int maxIndex1 = 0;
							int maxIndex2 = 0;
							for (int k = 0; k < endPoints.size(); k++) {
								for (int l = 0; l < k; l++) {
									double dist = endPoints.get(k).distanceTo(endPoints.get(l));
									if (dist > maxDist) {
										maxDist = dist;
										maxIndex1 = k;
										maxIndex2 = l;
									}
								}
							}
							Geometry2D.LineSegment temp = new Geometry2D.LineSegment(endPoints.get(maxIndex1), endPoints.get(maxIndex2));
							LineSegment maxSegment = temp.toLineSegment();
							edges[i][j] = maxSegment;
							edges[j][i] = maxSegment;
							
							//System.err.println("Warning: Found " + commonSegments.size() + " common segments for cells " + i + " and " + j + ":");
							//System.err.println(" -> Added segment connecting endpoints with maximum distance!");
							//System.err.println("Common segments:");
							//for (Geometry2D.LineSegment segment : commonSegments) {
							//	System.err.println(segment);
							//}
							//System.err.println("Added segment: " + maxSegment);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Constructs a Voronoi map for the given locations, uses the convex hull
	 * of the locations as border.
	 * 
	 * @param locations     the set of locations
	 * @param mapProjection the projection method for the coordinates
	 */
	public VoronoiMap(Collection<AggregatedLocation> locations, MapProjection mapProjection) {
		this(locations, new Polytope(MapBorder.getAggregatedLocationsConvexHull(locations).getVertices()), mapProjection);
	}
	
	/**
	 * Scales the given convex polygon by 100% in every direction (seen from the centre
	 * of gravity).
	 * 
	 * @param borderPolygon  the border polygon
	 */
	private static void extendPolygonByFactor15(Geometry2D.ConvexPolygon borderPolygon) {
		double[] centre = borderPolygon.toPolytope(true).getCenterOfGravity();
		for (int i = 0; i < borderPolygon.p.length; i++) {
			borderPolygon.p[i].translateBy(-centre[0], -centre[1]);
			borderPolygon.p[i].scaleWith(1.5);
			borderPolygon.p[i].translateBy(centre[0], centre[1]);
		}
	}
	
	/**
	 * Returns the locations for which this Voronoi map was constructed.
	 * Note that this list may contain locations that generate no Voronoi cell
	 * because they lie outside the border polygon.
	 * 
	 * @return the locations
	 */
	public List<AggregatedLocation> getLocations() {
		return Collections.unmodifiableList(this.locations);
	}
	
	/**
	 * Returns the locations that have a Voronoi cell.
	 * 
	 * @return the locations in the same order as given by <code>getLocationCells</code>
	 */
	public List<AggregatedLocation> getLocationsWithCells() {
		ArrayList<AggregatedLocation> result = new ArrayList<AggregatedLocation>(this.locationCells.size());
		for (LocationCell cell : this.locationCells) {
			result.add(cell.getLocation());
		}
		return result;
	}
	
	/**
	 * Returns the border polygon of this Voronoi map.
	 * 
	 * @return the border polygon
	 */
	public Polytope getBorder() {
		return this.border;
	}
	
	/**
	 * Returns the window of this Voronoi map.
	 * 
	 * @return the window
	 */
	public DoubleBox getWindow() {
		return this.window;
	}
	
	/**
	 * Returns the map projection method of this Voronoi map.
	 * 
	 * @return the map projection method
	 */
	public MapProjection getMapProjection() {
		return this.mapProjection;
	}
	
	/**
	 * Returns the cells of this Voronoi map.
	 * 
	 * @return the cells, every location corresponds to one cell
	 */
	public List<LocationCell> getLocationCells() {
		return Collections.unmodifiableList(this.locationCells);
	}
	
	/**
	 * Returns the array index used for the Voronoi cell of the given location.
	 * 
	 * @param location  the location object
	 * @return the corresponding array index or <code>-1</code> if no index is assigned
	 */
	public int getArrayIndex(AggregatedLocation location) {
		if (this.locationToArrayIndex == null) {
			throw new RuntimeException("Mapping location -> array index not yet initialized!");
		}
		Integer val = this.locationToArrayIndex.get(location);
		if (val == null) {
			return -1;
		} else {
			return val.intValue();
		}
	}
	
	/**
	 * Checks if the given two cells are neighbours.
	 * 
	 * @param cell1  the first cell
	 * @param cell2  the second cell
	 * @return <code>true</code> if the two cells are neighbours
	 */
	public boolean cellsAreNeighbours(LocationCell cell1, LocationCell cell2) {
		return cellsAreNeighbours(cell1.getLocation(), cell2.getLocation());
	}
	
	/**
	 * Checks if the given two cells are neighbours.
	 * 
	 * @param location1  the location of the first cell
	 * @param location2  the location of the second cell
	 * @return <code>true</code> if the two cells are neighbours
	 */
	public boolean cellsAreNeighbours(AggregatedLocation location1, AggregatedLocation location2) {
		Integer index1 = locationToArrayIndex.get(location1);
		Integer index2 = locationToArrayIndex.get(location2);
		if (index1 == null) {
			throw new IllegalArgumentException("Location 1 \""+location1+"\" does not exist in the Voronoi map!");
		}
		if (index2 == null) {
			throw new IllegalArgumentException("Location 2 \""+location2+"\" does not exist in the Voronoi map!");
		}
		return (this.edges[index1][index2] != null);
	}
	
	/**
	 * Returns the edge separating the given two cells or <code>null</code> if the cells
	 * aren't neighbours.
	 * 
	 * @param location1		the location of the first cell
	 * @param location2		the location of the second cell
	 * @return the edge separating the two cells or <code>null</code> if they aren't neighbours;
	 *         note that exactly the same object is returned regardless of the order of the two
	 *         locations
	 */
	public LineSegment getSeparatingEdge(AggregatedLocation location1, AggregatedLocation location2) {
		Integer index1 = locationToArrayIndex.get(location1);
		Integer index2 = locationToArrayIndex.get(location2);
		if (index1 == null) {
			throw new IllegalArgumentException("Location 1 \""+location1+"\" does not exist in the Voronoi map!");
		}
		if (index2 == null) {
			throw new IllegalArgumentException("Location 2 \""+location2+"\" does not exist in the Voronoi map!");
		}
		if (this.edges[index1][index2] == null) {
			return null;
		} else {
			return (this.edges[index1][index2]).clone();
		}
	}
	
	/**
	 * Returns the edge separating the given two cells or <code>null</code> if the cells
	 * aren't neighbours.
	 * 
	 * @param cell1		the first cell
	 * @param cell2		the second cell
	 * @return the edge separating the two cells or <code>null</code> if they aren't neighbours;
	 *         note that exactly the same object is returned regardless of the order of the two
	 *         locations
	 */
	public LineSegment getSeparatingEdge(LocationCell cell1, LocationCell cell2) {
		return getSeparatingEdge(cell1.getLocation(), cell2.getLocation());
	}
	
	/**
	 * Returns the number of inner edges in the VoronoiMap. Edges which are part of the map border
	 * are not considered.
	 * 
	 * @return number of inner edges
	 */
	public int getNumberOfEdges() {
		int n = 0;
		for (int i = 0; i < this.edges.length; i++) {
			for (int j = 0; j < i; j++) {
				if (this.edges[i][j] != null) {
					n++;
				}
			}
		}
		return n;
	}
	
}
