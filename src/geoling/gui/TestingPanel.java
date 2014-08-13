package geoling.gui;

import geoling.gui.util.JFileChooserConfirmOverwrite;
import geoling.gui.util.VoronoiMapLabel;
import geoling.locations.LocationAggregator;
import geoling.locations.LocationPassthrough;
import geoling.locations.SimilarCoordinatesAggregation;
import geoling.locations.util.AggregatedLocation;
import geoling.maps.plot.PlotHelper;
import geoling.maps.plot.PlotVoronoiMap;
import geoling.maps.projection.MapProjection;
import geoling.maps.projection.MercatorProjection;
import geoling.maps.util.VoronoiMap;
import geoling.maps.util.VoronoiMapCache;
import geoling.maps.util.VoronoiMap.LocationCell;
import geoling.models.Border;
import geoling.models.ConfigurationOption;
import geoling.models.Location;
import geoling.util.sim.grain.LineSegment;
import geoling.util.sim.grain.Polytope;
import geoling.util.sim.util.plot.DrawableObject2D;
import geoling.util.sim.util.plot.DrawableRandomSetElement2D;
import geoling.util.sim.util.plot.PlotToEPS;
import geoling.util.sim.util.plot.PlotToGraphics2D;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.javalite.activejdbc.LazyList;

/**
 * Panel showing a Voronoi map where areas or borders can be defined by clicks on the map.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class TestingPanel {

	public static int DEFAULT_MAP_HEIGHT = 500;

	// options for export
	/** The width of the selected edges. */
	private double edgeLineWidth = 2.0;
	/** The line width between two cells. */
	private double cellLineWidth = 1.0;
	/** The width of the surrounding border. */
	private double borderLineWidth = 2.0;
	/** The color of the selected line segment between two cells. */
	private Color cellLineColor = Color.BLUE;
	/** The color of the selected area. */
	private Color areaColor = Color.GREEN;
	/** The color of the marked cell in border modus. */
	private Color cellColor = Color.ORANGE;
	/** <code>true</code> if edge shall be plotted in foreground,<br>
	 *  <code>false</code> if cell line shall be plotted in foreground. */
	private boolean edgeInForeground = true;

	/** The <code>JPanel</code> to which contents are added. */
	private JPanel panel = new JPanel();

	/** The <code>JScrollPane</code> to which the Voronoi map is added. */
	private JScrollPane scrollPaneMap;

	/**
	 * The <code>MapLabel</code> shows the drawn <code>VoroniMap</code> and has a special
	 * <code>getToolTipText(MouseEvent)</code> method for displaying locations.
	 */
	private VoronoiMapLabel labelMap;

	/** A <code>HashMap</code> that gives the corresponding location for a polytope. */
	private HashMap<Polytope, AggregatedLocation> hints = new HashMap<Polytope, AggregatedLocation>();

	/** The list of all (aggregated) locations for the empty map. */
	private ArrayList<AggregatedLocation> locations;
	/** The last selected location (may be <code>null</code>). */
	private AggregatedLocation lastLocation = null;

	/** <code>true</code> if a border shall be defined, <code>false</code> if an area shall be defined. */
	private boolean edgeModus = true;

	/** The default border polygon. */
	private Polytope borderPolygon = Border.getDefaultBorder().toPolygon();
	/** The map projection from geographical coordinates to map coordinates. */
	private MapProjection mapProjection = new MercatorProjection();

	/** The empty Voronoi map. */
	private VoronoiMap voronoiMap;
	/** This <code>HashMap</code> gives the <code>Polytope</code> in the Voronoi map for each <code>AggregatedLocation</code>. */
	private HashMap<AggregatedLocation, Polytope> polytopesToLocations;

	/** The <code>PlotHelper</code> for determining shift vector and scaling factor. */
	private PlotHelper helper;

	/** The list of all line segments (defined by the adjacent locations, saved with the cellLineColor) , relevant if <code>defineBorder==true</code>. */
	private HashMap<LocationPair, Color> locationPairs = new HashMap<LocationPair, Color>();
	/** The list of all areas (saved with the areaColor), relevant if <code>defineBorder==false</code>. */
	private HashMap<AggregatedLocation, Color> areaLocations = new HashMap<AggregatedLocation, Color>();

	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;


	/** Adds a <code>JPanel</code> to the <code>JTabbedPane</code>. An empty Voronoi map is
	 * drawn and borders or areas can be selected and subsequently analysed by statistical tests.
	 * @param tabbedPane the <code>JTabbedPane</code>
	 */
	public TestingPanel(final JTabbedPane tabbedPane, final String outputfolder) {




		rb = ResourceBundle.getBundle("TestingPanel", GeoLingGUI.LANGUAGE);

		tabbedPane.addTab(rb.getString("title_TestingPanel"), null, panel, null);
		tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);

		// set layout
		GridBagLayout gbl_panelMap = new GridBagLayout();
		gbl_panelMap.columnWidths = new int[] { 250, 75, 75, 0 };
		gbl_panelMap.rowHeights = new int[] { 40, 40, 40, 40, 40, 40, 100, 40, 0  };
		gbl_panelMap.columnWeights = new double[] { 0.8, 0.1, 0.1, Double.MIN_VALUE };
		gbl_panelMap.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		panel.setLayout(gbl_panelMap);



		// create list of all (aggregated) locations
		{
			// fetch all locations
			LazyList<Location> tmp = Location.findAll();
			ArrayList<Location> locationObjects = new ArrayList<Location>(tmp);

			// do not aggregate locations by default, but aggregate locations which have essentially the same coordinates
			// if this is configured
			LocationAggregator locationAggregator;
			if (ConfigurationOption.getOption("useLocationAggregation", false)) {
				locationAggregator = new SimilarCoordinatesAggregation();
			} else {
				locationAggregator = new LocationPassthrough();
			}

			this.locations = new ArrayList<AggregatedLocation>(locationAggregator.getAggregatedLocations(locationObjects));
		}

		// create voronoi map
		helper = getHelper(1.0f);
		voronoiMap = VoronoiMapCache.getVoronoiMap(locations, borderPolygon, mapProjection);

		polytopesToLocations = new HashMap<AggregatedLocation, Polytope>();
		for (LocationCell cell : voronoiMap.getLocationCells()) {
			polytopesToLocations.put(cell.getLocation(), cell.getVoronoiCell());
		}

		// create empty Voronoi map
		scrollPaneMap = new JScrollPane();
		GridBagConstraints gbc_scrollPaneMap = new GridBagConstraints();
		gbc_scrollPaneMap.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPaneMap.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneMap.gridx = 0;
		gbc_scrollPaneMap.gridy = 0;
		gbc_scrollPaneMap.gridheight = 7;
		panel.add(scrollPaneMap, gbc_scrollPaneMap);


		// draw empty voronoi map
		plotVoronoiWithDrawableObjects(new LinkedList<DrawableObject2D>());


		// define JSlider to zoom image
		final JSlider sliderZoom = new JSlider(0, 500, 100);
		sliderZoom.addChangeListener(new ChangeListener() {
			/** Zoom factor has been changed, redraw Voronoi map and drawable objects. */
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					if (source.getValue()<5) {
						sliderZoom.setValue(5);
					}
					else {
						float scaleFactor = (float) (source.getValue() / 100.0);
						helper = getHelper(scaleFactor);
						if (edgeModus) {
							plotVoronoiWithDrawableObjects(getDrawableLineSegments());
						}
						else {
							plotVoronoiWithDrawableObjects(getDrawablePolytopes());
						}
					}
				}
			}
		});
		sliderZoom.setMajorTickSpacing(50);
		sliderZoom.setPaintTicks(true);
		sliderZoom.setPaintLabels(true);
		GridBagConstraints gbc_sliderZoom = new GridBagConstraints();
		gbc_sliderZoom.insets = new Insets(0, 0, 5, 5);
		gbc_sliderZoom.fill = GridBagConstraints.BOTH;
		gbc_sliderZoom.gridx = 0;
		gbc_sliderZoom.gridy = 7;
		panel.add(sliderZoom, gbc_sliderZoom);


		JLabel lblDrawMap = new JLabel(rb.getString("text_lblDrawMap"));
		GridBagConstraints gbc_lblDrawMap = new GridBagConstraints();
		gbc_lblDrawMap.insets = new Insets(0, 0, 5, 5);
		gbc_lblDrawMap.fill = GridBagConstraints.BOTH;
		gbc_lblDrawMap.gridx = 1;
		gbc_lblDrawMap.gridy = 0;
		gbc_lblDrawMap.gridwidth = 2;
		panel.add(lblDrawMap, gbc_lblDrawMap);

		// radio buttons to choose between border or area definition
		JRadioButton radioButtonBorder = new JRadioButton(rb.getString("text_radioButtonBorder"));
		radioButtonBorder.setToolTipText(rb.getString("tooltip_radioButtonBorder"));
		GridBagConstraints gbc_radioButtonBorder = new GridBagConstraints();
		gbc_radioButtonBorder.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonBorder.fill = GridBagConstraints.BOTH;
		gbc_radioButtonBorder.gridx = 1;
		gbc_radioButtonBorder.gridy = 1;
		panel.add(radioButtonBorder, gbc_radioButtonBorder);
		radioButtonBorder.addActionListener(new ActionListener() {
			/** Radio button has been selected, switch to border modus. */
			public void actionPerformed(ActionEvent arg0) {
				edgeModus = true;
				locationPairs = new HashMap<LocationPair, Color>();
				// draw empty Voronoi map
				plotVoronoiWithDrawableObjects(new LinkedList<DrawableObject2D>());
			}
		});

		JRadioButton radioButtonArea = new JRadioButton(rb.getString("text_radioButtonArea"));
		radioButtonArea.setToolTipText(rb.getString("tooltip_radioButtonArea"));
		GridBagConstraints gbc_radioButtonArea = new GridBagConstraints();
		gbc_radioButtonArea.insets = new Insets(0, 0, 5, 5);
		gbc_radioButtonArea.fill = GridBagConstraints.BOTH;
		gbc_radioButtonArea.gridx = 2;
		gbc_radioButtonArea.gridy = 1;
		panel.add(radioButtonArea, gbc_radioButtonArea);
		radioButtonArea.addActionListener(new ActionListener() {
			/** Radio button has been selected, switch to border modus. */
			public void actionPerformed(ActionEvent arg0) {
				edgeModus = false;
				areaLocations = new HashMap<AggregatedLocation, Color>();
				// draw empty voronoi map
				plotVoronoiWithDrawableObjects(new LinkedList<DrawableObject2D>());
			}
		});

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(radioButtonBorder);
		buttonGroup.add(radioButtonArea);
		radioButtonBorder.setSelected(true);


		// button to choose edge color
		JButton buttonEdgeColor = new JButton(rb.getString("text_buttonEdgeColor"));
		GridBagConstraints gbc_buttonEdgeColor = new GridBagConstraints();
		gbc_buttonEdgeColor.insets = new Insets(0, 0, 5, 5);
		gbc_buttonEdgeColor.fill = GridBagConstraints.BOTH;
		gbc_buttonEdgeColor.gridx = 1;
		gbc_buttonEdgeColor.gridy = 2;
		panel.add(buttonEdgeColor, gbc_buttonEdgeColor);
		buttonEdgeColor.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Color newColor = JColorChooser.showDialog(panel, rb.getString("message_colorChooser"), cellLineColor);
				if (newColor!=null) {
					cellLineColor = newColor;
				}
			}
		});

		// button to choose area color
		JButton buttonAreaColor = new JButton(rb.getString("text_buttonAreaColor"));
		GridBagConstraints gbc_buttonAreaColor = new GridBagConstraints();
		gbc_buttonAreaColor.insets = new Insets(0, 0, 5, 5);
		gbc_buttonAreaColor.fill = GridBagConstraints.BOTH;
		gbc_buttonAreaColor.gridx = 2;
		gbc_buttonAreaColor.gridy = 2;
		panel.add(buttonAreaColor, gbc_buttonAreaColor);
		buttonAreaColor.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Color newColor = JColorChooser.showDialog(panel, rb.getString("message_colorChooser"), areaColor);
				if (newColor!=null) {
					areaColor = newColor;
				}
			}
		});


		// button to define options of exported EPS graphic
		JButton buttonDefineOptions = new JButton(rb.getString("text_buttonDefineOptions"));
		buttonDefineOptions.setToolTipText(rb.getString("tooltip_buttonDefineOptions"));
		GridBagConstraints gbc_buttonDefineOptions = new GridBagConstraints();
		gbc_buttonDefineOptions.insets = new Insets(0, 0, 5, 5);
		gbc_buttonDefineOptions.fill = GridBagConstraints.BOTH;
		gbc_buttonDefineOptions.gridx = 1;
		gbc_buttonDefineOptions.gridy = 3;
		panel.add(buttonDefineOptions, gbc_buttonDefineOptions);
		buttonDefineOptions.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JTextField jTFedgeLineWidth = new JTextField(edgeLineWidth+"");
				JTextField jTFcellLineWidth = new JTextField(cellLineWidth+"");
				JTextField jTFborderLineWidth = new JTextField(borderLineWidth+"");
				JCheckBox checkBoxForeground = new JCheckBox(rb.getString("text_checkBoxForeground"));
				checkBoxForeground.setSelected(edgeInForeground);
				Object[] message = {rb.getString("message_edgeLineWidth"), jTFedgeLineWidth, rb.getString("message_cellLineWidth"), jTFcellLineWidth, 
						rb.getString("message_borderLineWidth"), jTFborderLineWidth, checkBoxForeground};
				JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				pane.createDialog(panel, rb.getString("title_plotOptions")).setVisible(true);
				try {
					edgeLineWidth = Double.parseDouble(jTFedgeLineWidth.getText());
					cellLineWidth = Double.parseDouble(jTFcellLineWidth.getText());
					borderLineWidth = Double.parseDouble(jTFborderLineWidth.getText());
					edgeInForeground = checkBoxForeground.isSelected();
				}
				catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(panel, rb.getString("text_popupWrongFormat"), rb.getString("title_popupWrongFormat"), JOptionPane.WARNING_MESSAGE);
				}
			}
		});


		// button to save map
		JButton buttonSaveMap = new JButton(rb.getString("text_buttonSaveMap"));
		buttonSaveMap.setToolTipText(rb.getString("tooltip_buttonSaveMap"));
		GridBagConstraints gbc_buttonSaveMap = new GridBagConstraints();
		gbc_buttonSaveMap.insets = new Insets(0, 0, 5, 5);
		gbc_buttonSaveMap.fill = GridBagConstraints.BOTH;
		gbc_buttonSaveMap.gridx = 2;
		gbc_buttonSaveMap.gridy = 3;
		panel.add(buttonSaveMap, gbc_buttonSaveMap);
		buttonSaveMap.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooserConfirmOverwrite(outputfolder + "/" + "custom_map.eps");
				chooser.setFileFilter(new FileNameExtensionFilter(rb.getString("filter_eps"), "eps"));

				if (chooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
					String file = chooser.getSelectedFile().getAbsolutePath();
					try {

						FileOutputStream fos = new FileOutputStream(file);
						// keep current helper in memory, to reuse it, after eps graphic has been exported
						PlotHelper currentHelper = helper;
						// create helper with default height
						helper = new PlotHelper(borderPolygon, mapProjection);
						helper.setCellLineWidth(cellLineWidth);
						helper.setBorderLineWidth(borderLineWidth);
						PlotVoronoiMap plot = new PlotVoronoiMap(voronoiMap);

						try (PlotToEPS eps = new PlotToEPS(helper.getWindow(), fos)) {
							// getDrawableLineSegments() uses object variable helper
							if (edgeModus) {
								if (edgeInForeground) {
									plot.voronoiExport(eps, helper, null);
									ArrayList<DrawableObject2D> objects = getDrawableLineSegments();
									eps.plot(objects);
								}
								else {
									ArrayList<DrawableObject2D> objects = getDrawableLineSegments();
									eps.plot(objects);
									plot.voronoiExport(eps, helper, null);
								}
							}
							else {
								ArrayList<DrawableObject2D> objects = getDrawablePolytopes();
								eps.plot(objects);
								plot.voronoiExport(eps, helper, null);
							}

						}


						helper = currentHelper;
					}
					catch (IOException e) {
						e.printStackTrace();
						JOptionPane.showMessageDialog(panel, rb.getString("text_popupSavingError")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupSavingError"), JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});


		JLabel lblTests = new JLabel(rb.getString("text_lblTests"));
		GridBagConstraints gbc_lblTests = new GridBagConstraints();
		gbc_lblTests.insets = new Insets(0, 0, 5, 5);
		gbc_lblTests.fill = GridBagConstraints.BOTH;
		gbc_lblTests.gridx = 1;
		gbc_lblTests.gridy = 4;
		gbc_lblTests.gridwidth = 2;
		panel.add(lblTests, gbc_lblTests);


		// buttons to start statistical tests
		JButton buttonStartBorderTest = new JButton(rb.getString("text_buttonStartBorderTest"));
		GridBagConstraints gbc_buttonStartBorderTest = new GridBagConstraints();
		gbc_buttonStartBorderTest.insets = new Insets(0, 0, 5, 5);
		gbc_buttonStartBorderTest.fill = GridBagConstraints.BOTH;
		gbc_buttonStartBorderTest.gridx = 1;
		gbc_buttonStartBorderTest.gridy = 5;
		panel.add(buttonStartBorderTest, gbc_buttonStartBorderTest);
		buttonStartBorderTest.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (edgeModus) {
					try {
						@SuppressWarnings("unused")
						ArrayList<LineSegment> connectedSegments = getConnectedBorder();
					}
					catch (IllegalArgumentException e) {
						e.printStackTrace();
						JOptionPane.showMessageDialog(panel, rb.getString("text_popupErrorConnectedBorder") + (e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupErrorConnectedBorder"), JOptionPane.WARNING_MESSAGE);
					}
					// TODO: Aaron, Lisa: test for border
				}

			}
		});

		JButton buttonStartAreaTest = new JButton(rb.getString("text_buttonStartAreaTest"));
		GridBagConstraints gbc_buttonStartAreaTest = new GridBagConstraints();
		gbc_buttonStartAreaTest.insets = new Insets(0, 0, 5, 5);
		gbc_buttonStartAreaTest.fill = GridBagConstraints.BOTH;
		gbc_buttonStartAreaTest.gridx = 2;
		gbc_buttonStartAreaTest.gridy = 5;
		panel.add(buttonStartAreaTest, gbc_buttonStartAreaTest);
		buttonStartAreaTest.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!edgeModus) {
					// TODO: Aaron, Lisa: test for area
				}
			}
		});










	}

	/** Returns a <code>PlotHelper</code> for the given scale factor. 
	 * 
	 * @param scaleFactor the scale factor
	 * @return the <code>PlotHelper</code> with adjusted size and appropriate options
	 */
	private PlotHelper getHelper(float scaleFactor) {
		PlotHelper helper = new PlotHelper(borderPolygon, mapProjection, Math.round(DEFAULT_MAP_HEIGHT*scaleFactor), 10);
		helper.setCellLineWidth(1.0);
		return helper;
	}


	/**
	 * Plot a list of drawable objects, overlay a Voronoi map and attach the created map
	 * to the <code>JScrollPane</code>.
	 * @param objects a list of drawable objects
	 */
	private void plotVoronoiWithDrawableObjects(List<DrawableObject2D> objects) {

		// save values of the scroll bars to set scroll bars to the same position after drawing the new map
		int horizontalValue = scrollPaneMap.getHorizontalScrollBar().getValue();
		int verticalValue = scrollPaneMap.getVerticalScrollBar().getValue();

		BufferedImage bi = new BufferedImage(helper.getWidth(), helper.getHeight(), BufferedImage.TYPE_INT_RGB);
		PlotToGraphics2D gre = new PlotToGraphics2D(helper.getWindow(), bi.createGraphics());
		PlotVoronoiMap plot = new PlotVoronoiMap(voronoiMap);

		gre.plot(objects);
		plot.voronoiExport(gre, helper, hints);
		ImageIcon iconMap = new ImageIcon(bi);

		labelMap = new VoronoiMapLabel(iconMap, hints);
		MapMouseListener listener = new MapMouseListener();
		labelMap.addMouseListener(listener);
		labelMap.addMouseMotionListener(listener);
		// add new map to scroll pane
		ToolTipManager.sharedInstance().registerComponent(labelMap);
		scrollPaneMap.getViewport().setView(labelMap);
		// set old values
		scrollPaneMap.getHorizontalScrollBar().setValue(horizontalValue);
		scrollPaneMap.getVerticalScrollBar().setValue(verticalValue);
	}



	/** Looks at the selected location pairs and returns a list with drawable line segments.
	 * 
	 * @return a list of line segments that can be drawn
	 */
	private ArrayList<DrawableObject2D> getDrawableLineSegments() {
		ArrayList<DrawableObject2D> drawableObjects = new ArrayList<DrawableObject2D>();
		for (Map.Entry<LocationPair, Color> entry : locationPairs.entrySet()) {
			LocationPair pair = entry.getKey();
			LineSegment ls = pair.getProjectedLineSegment();
			ls.translateBy(helper.getShift());
			ls.stretch(helper.getScale());
			drawableObjects.add(new DrawableRandomSetElement2D(ls, edgeLineWidth, entry.getValue()));
		}

		return drawableObjects;
	}

	/** Looks at the selected locations and returns a list with drawable polytopes.
	 * 
	 * @return a list of polytopes that can be drawn
	 */
	private ArrayList<DrawableObject2D> getDrawablePolytopes() {
		ArrayList<DrawableObject2D> drawableObjects = new ArrayList<DrawableObject2D>();
		for (Map.Entry<AggregatedLocation, Color> entry : areaLocations.entrySet()) {
			AggregatedLocation location = entry.getKey();
			Polytope polytope = mapProjection.projectLatLong(polytopesToLocations.get(location));
			polytope.translateBy(helper.getShift());
			polytope.stretch(helper.getScale());
			drawableObjects.add(new DrawableRandomSetElement2D(new Polytope(polytope.getVertices(), true), 0.0, entry.getValue()));
		}

		return drawableObjects;
	}

	/**
	 * Validates the defined border, i.e. graph must be connected and may not have multiple branches.
	 * @return a list of all line segments in the correct order
	 * 
	 * @throws IllegalArgumentException if graph is not connected or if graph has multiple branches
	 */
	private ArrayList<LineSegment> getConnectedBorder() {
		if (locationPairs.size()==0) {
			throw new IllegalArgumentException(rb.getString("text_illArgExNoSegments"));
		}
		ArrayList<LineSegment> segments = new ArrayList<LineSegment>();
		for (LocationPair pair : locationPairs.keySet()) {
			segments.add(pair.getLineSegment());
		}

		ArrayList<LineSegment> result = new ArrayList<LineSegment>();
		result.add(segments.remove(0));
		if (locationPairs.size()==1) {
			return result;
		}
		else {
			int countEnd = 0;
			int countStart = 0;
			LineSegment firstSegment = result.get(0);
			// find a second segment
			LineSegment secondSegment = null;
			LineSegment segmentToBeRemoved = null;
			for (int i=0; i<segments.size(); i++) {
				LineSegment segment = segments.get(i);
				if (countStart==0) { // no line segment has alread been found for the start point of firstSegment
					// case 1
					if (firstSegment.getEndPoint().equals(segment.getStartPoint())) {
						segmentToBeRemoved = segment;
						secondSegment = segment;
						countEnd++;
					}
					// case 2
					if (firstSegment.getEndPoint().equals(segment.getEndPoint())) {
						segmentToBeRemoved = segment;
						secondSegment = getFlippedLineSegment(segment);
						countEnd++;
					}
				}
				if (countEnd==0) { // no line segment has alread been found for the end point of firstSegment
					// case 3
					if (firstSegment.getStartPoint().equals(segment.getEndPoint())) {
						segmentToBeRemoved = segment;
						secondSegment = segment;
						countStart++;
					}
					// case 4
					if (firstSegment.getStartPoint().equals(segment.getStartPoint())) {
						segmentToBeRemoved = segment;
						secondSegment = getFlippedLineSegment(segment);
						countStart++;
					}
				}
			}
			if ((countEnd+countStart)==0) {
				throw new IllegalArgumentException(rb.getString("text_illArgExNotConnected"));
			}
			else if ((countEnd+countStart)==1) {
				segments.remove(segmentToBeRemoved);
				if (countEnd==1) {
					result.add(secondSegment);
				}
				else {
					result.add(0, secondSegment);
				}
			}
			else {
				throw new IllegalArgumentException(rb.getString("text_illArgExMultipleBranches"));
			}

		}

		// now result exactly contains two segments
		boolean foundFirstSegment = false;
		boolean foundLastSegment = false;

		int sizeOld = segments.size();
		while (segments.size()>0) {
			// FIRST SEGMENT
			if (!foundFirstSegment) {
				LineSegment firstSegment = result.get(0);

				LineSegment nextSegment = null;
				LineSegment segmentToBeRemoved = null;
				// look for the segment that is linked to the start point of firstSegment
				for (int index=0; index<segments.size(); index++) {
					LineSegment segment = segments.get(index);
					if (firstSegment.getStartPoint().equals(segment.getEndPoint())) {
						if (nextSegment!=null) {
							throw new IllegalArgumentException(rb.getString("text_illArgExMultipleBranches"));
						}
						segmentToBeRemoved = segment;
						nextSegment = segment;
					}
					if (firstSegment.getStartPoint().equals(segment.getStartPoint())) {
						if (nextSegment!=null) {
							throw new IllegalArgumentException(rb.getString("text_illArgExMultipleBranches"));
						}
						// flip start point and end point of segment
						segmentToBeRemoved = segment;
						nextSegment = getFlippedLineSegment(segment);
					}
				}
				// exactly one segment connected to start point of firstSegment was found, start next iteration of while loop
				if (nextSegment!=null) {
					result.add(0, nextSegment); // add in front of first segment
					segments.remove(segmentToBeRemoved);
				}
				// no segment was found connected to firstSegment -> first segment was found
				else {
					foundFirstSegment = true;
				}
			}

			// LAST SEGMENT
			if (!foundLastSegment) {
				LineSegment lastSegment = result.get(result.size()-1);

				LineSegment nextSegment = null;
				LineSegment segmentToBeRemoved = null;
				// look for the segment that is linked to the start point of firstSegment
				for (int index=0; index<segments.size(); index++) {
					LineSegment segment = segments.get(index);
					if (lastSegment.getEndPoint().equals(segment.getStartPoint())) {
						if (nextSegment!=null) {
							throw new IllegalArgumentException(rb.getString("text_illArgExMultipleBranches"));
						}
						segmentToBeRemoved = segment;
						nextSegment = segment;
					}
					if (lastSegment.getEndPoint().equals(segment.getEndPoint())) {
						if (nextSegment!=null) {
							throw new IllegalArgumentException(rb.getString("text_illArgExMultipleBranches"));
						}
						// flip start point and end point of segment
						segmentToBeRemoved = segment;
						nextSegment = getFlippedLineSegment(segment);
					}
				}
				// exactly one segment connected to end point of lastSegment was found, start next iteration of while loop
				if (nextSegment!=null) {
					result.add(nextSegment); // add behind of last segment
					segments.remove(segmentToBeRemoved);
				}
				// no segment was found connected to lastSegment -> last segment was found
				else {
					foundLastSegment = true;
				}
			}

			if (segments.size()==sizeOld) {
				throw new IllegalArgumentException(rb.getString("text_illArgExNotConnected"));
			}
			else {
				sizeOld = segments.size();
			}
		}

		return result;
	}

	private static LineSegment getFlippedLineSegment(LineSegment segment) {
		return new LineSegment(segment.getEndPoint().toGeo2DPoint().x, segment.getEndPoint().toGeo2DPoint().y, segment.getStartPoint().toGeo2DPoint().x, segment.getStartPoint().toGeo2DPoint().y, true);
	}




	/**
	 * This class implements <code>MouseListener</code> and offers the <code>mouseClicked</code>
	 * method. This method reacts on the clicks of the user and draws line segments or
	 * polygons into the Voronoi map, depending on the selected modus.
	 * 
	 * @author Raphael Wimmer
	 */
	public class MapMouseListener implements MouseListener, MouseMotionListener {

		/** Used to notice whether several areas shall be painted at the same time, relevant for <code>edgeModus=false</code>. */
		private boolean multiselect = false;
		/** Used to reconstruct <code>mouseClicked</code> event for <code>edgeModus=false</code>. */
		private boolean click = false;
		/** This <code>HashMap</code> saves all locations that were crossed during the mouse was pressed and the mouse was released, 
		 * relevant for <code>edgeModus=false</code>. 
		 */
		private HashMap<AggregatedLocation, Color> tempLocations = new HashMap<AggregatedLocation, Color>();

		@Override
		public void mouseReleased(MouseEvent event) {
			if (!edgeModus) {
				// variable click is necessary, because mousdeDragged event interferes mouseClicked event
				if (click) {
					AggregatedLocation currentLocation = labelMap.getLocation(event);
					// the user has clicked on a location and not outside of the border
					if (currentLocation!=null) {
						processClickForAreaModus(currentLocation);
					}
					click = false;
				}
				else {
					multiselect = false;
					areaLocations.putAll(tempLocations);
					tempLocations.clear();
					plotVoronoiWithDrawableObjects(getDrawablePolytopes());
				}
			}
		}			
		@Override
		public void mousePressed(MouseEvent event) {
			if (!edgeModus) {
				click = true;
				multiselect = true;
				tempLocations.clear();
			}
		}
		@Override
		public void mouseExited(MouseEvent event) {
			if (!edgeModus) {
				multiselect = false;
				areaLocations.putAll(tempLocations);
				tempLocations.clear();
				plotVoronoiWithDrawableObjects(getDrawablePolytopes());
			}
		}	
		@Override
		public void mouseEntered(MouseEvent event) {}

		/**
		 * Click on the Voronoi map in <code>edgeModus=true</code>. For <code>edgeModus=false</code>,
		 * the click is event is manually constructed by mousePressed, mouseReleased and the
		 * variable click.
		 * @param event the <code>MouseEvent</code>, i.e. a left or right click on the
		 * <code>VoronoiMapLabel</code>
		 */
		public void mouseClicked(MouseEvent event) {
			if (SwingUtilities.isLeftMouseButton(event)) {
				AggregatedLocation currentLocation = labelMap.getLocation(event);
				// the user has clicked on a location and not outside of the border
				if (currentLocation!=null) {
					// modus: define border
					if (edgeModus) {
						// click on the same location -> deselect current cell
						if (currentLocation.equals(lastLocation)) {
							plotVoronoiWithDrawableObjects(getDrawableLineSegments());
							lastLocation = null;
						}
						else {
							processClickForBorderModus(currentLocation);	
						}
					}
				}
			}
			// right mouse click 
			if (SwingUtilities.isRightMouseButton(event)) {	
				// modus: define border
				if (edgeModus) { // -> deselect current cell
					plotVoronoiWithDrawableObjects(getDrawableLineSegments());
					lastLocation = null;
				}
			}

		}

		@Override
		public void mouseDragged(MouseEvent event) {
			if (!edgeModus) {
				click = false;
				if (multiselect) {
					AggregatedLocation currentLocation = labelMap.getLocation(event);
					if (currentLocation!=null) {
						tempLocations.put(currentLocation, areaColor);
					}
				}
			}
		}
		@Override
		public void mouseMoved(MouseEvent event) {}


		/**
		 * Perform click on the given location in the border modus.
		 * @param currentLocation the selected <code>AggregatedLocation</code>
		 */
		private void processClickForBorderModus(AggregatedLocation currentLocation) {
			// no location has been chosen before
			if (lastLocation==null) {

				ArrayList<DrawableObject2D> objects = getDrawableLineSegments();
				// add Voronoi cell
				Polytope polytope = mapProjection.projectLatLong(polytopesToLocations.get(currentLocation));
				polytope.translateBy(helper.getShift());
				polytope.stretch(helper.getScale());
				objects.add(new DrawableRandomSetElement2D(new Polytope(polytope.getVertices(), true), 0.0, cellColor));
				plotVoronoiWithDrawableObjects(objects);

				lastLocation = currentLocation;

			}
			else { // the second location is chosen
				LineSegment lineSegment = voronoiMap.getSeparatingEdge(currentLocation, lastLocation);
				// if the two selected locations are neighbours and have a shared line segment
				if (lineSegment!=null) {
					LocationPair pair = new LocationPair(currentLocation, lastLocation, lineSegment);
					if (locationPairs.containsKey(pair)) {
						locationPairs.remove(pair);
					}
					else {
						locationPairs.put(pair, cellLineColor);
					}
					plotVoronoiWithDrawableObjects(getDrawableLineSegments());

					lastLocation = null;

				}


			}
		}

		private void processClickForAreaModus(AggregatedLocation currentLocation) {
			// click on a location that has already been in the list -> remove the location from the list
			if (areaLocations.containsKey(currentLocation)) {
				areaLocations.remove(currentLocation);
			}
			else {
				areaLocations.put(currentLocation, areaColor);
			}
			plotVoronoiWithDrawableObjects(getDrawablePolytopes());
		}

	}


	/**
	 * This class contains two adjacent cells (represented by their <code>AggregatedLocation</code>)
	 * and their common <code>LineSegment</code>.
	 * 
	 * @author Raphael Wimmer
	 */
	public class LocationPair {
		private AggregatedLocation location1;
		private AggregatedLocation location2;
		private LineSegment lineSegment;
		private LineSegment projectedLineSegment;

		public LocationPair(AggregatedLocation location1, AggregatedLocation location2, LineSegment lineSegment) {
			this.location1 = location1;
			this.location2 = location2;
			this.lineSegment = lineSegment;
			this.projectedLineSegment = mapProjection.projectLatLong(lineSegment);
		}

		public LineSegment getLineSegment() {
			return this.lineSegment.clone();
		}

		public LineSegment getProjectedLineSegment() {
			return this.projectedLineSegment.clone();
		}

		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof LocationPair) {
				LocationPair p = (LocationPair) o;
				if ((this.location1.equals(p.location1) && this.location2.equals(p.location2))
						|| (this.location1.equals(p.location2) && this.location2.equals(p.location1))) {
					return true;
				}
			}
			return false;

		}

		public int hashCode() {
			return (int) (location1.getId()*location2.getId()); // TODO: evtl. besseren hashCode implementieren
		}

		public String toString() {
			return "location1: " + location1 + "\t location2: " + location2;
		}


	}

}





