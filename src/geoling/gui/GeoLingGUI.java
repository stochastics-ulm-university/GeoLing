package geoling.gui;

import geoling.config.Database;
import geoling.config.Settings;
import geoling.gui.management_dialog.DatabaseManagementDialog;
import geoling.gui.util.ExceptionHandler;
import geoling.gui.util.JNameTree;
import geoling.gui.vendor.MultiLineCellRenderer;
import geoling.gui.vendor.MultiLineTable;
import geoling.models.*;
import geoling.sql.SQLReader;
import geoling.util.Directory;
import geoling.util.ModelHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Properties;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.EventQueue;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JTextField;

/**
 * This class contains the main method of the application <code>GeoLing</code>.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
public class GeoLingGUI extends JFrame {

	/** The lower threshold for the maximum heap size in megabytes, where a warning is shown. */
	public static final int WARN_HEAP = 900;

	/** The selected <code>Locale</code>. If <code>null</code>, it can be selected in a dialog. */
	public static Locale LANGUAGE = null;

	/** The identifier of the selected database. If <code>null</code>, it can be selected in a dialog. */ 
	public static String DATABASE_IDENTIFIER = null;

	/** Path to the MySQL/SQLite schema files of the database. */
	public static String SCHEMA_PATH = "./schema/%s.sql";

	private static final long serialVersionUID = 1L;

	/** The index of the tab that shows all maps in a tree order. */
	public static final int TAB_INDEX_MAPTREE = 0;
	/** The index of the tab that shows all groups. */
	public static final int TAB_INDEX_GROUP = 1;
	/** The index of the tab where exports of whole groups can be done. */
	public static final int TAB_INDEX_EXPORT = 2;

	/** Basic <code>JPanel</code> */
	private JPanel contentPane;
	/** To this <code>JTabbedPane</code> new panels can be added or removed */
	private JTabbedPane tabbedPane;

	/**
	 * <code>JPanel</code> for <code>JNameTree</code> and
	 * <code>MultiLineTable</code>. Both are embedded in a
	 * <code>JScrollPane</code>
	 */
	private JPanel panelCategory;
	/** <code>JScrollPane</code> for the <code>JNameTree</code> treeCategory */
	private JScrollPane scrollPaneJNameTree = new JScrollPane();
	/** <code>JScrollPane</code> for the <code>MultiLineTable</code> tableTags */
	private JScrollPane scrollPaneTagTable = new JScrollPane();
	/**
	 * This <code>JNameTree</code> offers an overview for all maps, which are
	 * assigned to a category.
	 */
	private JNameTree treeCategory;
	/**
	 * This <code>Map</code> is currently selected in the <code>JNameTree</code>
	 * treeCategory.
	 */
	private Map selectedMap;
	/**
	 * This <code>MultiLineTable</code> shows the tags of the selected node in
	 * the <code>JNameTree</code> treeCategory. This could be a category or a
	 * map.
	 */
	private MultiLineTable tableTags;
	
	private JLabel infoImageLabel;

	/** <code>ResourceBundle</code> for localization. */
	private ResourceBundle rb;
	/** The default output folder where files are written to. */
	private String outputfolder;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) throws IOException {

		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
		
		// Gnome: set the name of the application
		Toolkit xToolkit = Toolkit.getDefaultToolkit();
		try {
			java.lang.reflect.Field awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
			awtAppClassNameField.setAccessible(true);
			awtAppClassNameField.set(xToolkit, "GeoLing");
		} catch (Exception e) {
			// ignore all problems, probably we are not running on Gnome
		}
		
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				try {
					Settings.createDirectory();
					Settings.load();

					// fetch database identifier from settings file
					String identifierFromSettings = Settings.getDatabaseIdentifier();
					if (identifierFromSettings != null) {
						DATABASE_IDENTIFIER = identifierFromSettings;
					}

					ArrayList<Locale> foundLocales = findAllAvailableLocales();
					// detect language suitable for this system, save as "defaultLocale"
					Locale defaultLocale = null;
					Locale englishLocale = null;
					for (Locale locale : foundLocales) {
						if (locale.getLanguage().equals(Locale.getDefault().getLanguage())) {
							defaultLocale = locale;
						}
						if (locale.getLanguage().equals(new Locale("en").getLanguage())) {
							englishLocale = locale;
						}
					}
					
					// fetch language selected in settings file
					Locale localeFromSettings = Settings.getLanguage();
					if (localeFromSettings == Locale.getDefault()) {
						// Settings.getLanguage() returns Locale.getDefault() if no information is present
						localeFromSettings = null;
					}

					// ask the user only to select a language if his system language is not supported (and he didn't define
					// a language in the settings file)
					if ((localeFromSettings != null) && foundLocales.contains(localeFromSettings)) {
						LANGUAGE = localeFromSettings;
					} else if (defaultLocale != null) {
						LANGUAGE = defaultLocale;
					} else {
						Locale answer = (Locale)JOptionPane.showInputDialog(
								new JFrame(), "Select your language:", "Language",
								JOptionPane.QUESTION_MESSAGE, null,
								foundLocales.toArray(), (englishLocale != null) ? englishLocale : foundLocales.get(0));
						if (answer == null) {
							System.exit(1);
						} else {
							LANGUAGE = answer;
						}
					}

					ResourceBundle rb = ResourceBundle.getBundle("GeoLingGUI", GeoLingGUI.LANGUAGE);

					// set the Java locale for localization of UI strings e.g. JFileChooser
					Locale.setDefault(LANGUAGE);
					JComponent.setDefaultLocale(LANGUAGE);
					try {
						// make sure new locale is used for all components
						UIManager.setLookAndFeel(UIManager.getLookAndFeel());
					} catch (UnsupportedLookAndFeelException e) {
						e.printStackTrace();
					}

					// check max heap size and warn if too small
					long maxHeap = Runtime.getRuntime().maxMemory() / 1024 / 1024;
					if (maxHeap < WARN_HEAP) {
						JOptionPane.showMessageDialog(null, String.format(rb.getString("text_popupJavaHeapProblem"), maxHeap, WARN_HEAP),
						                              rb.getString("title_popupJavaHeapProblem"), JOptionPane.WARNING_MESSAGE);
					}

					// if no database identifier is specified for the static variable
					if (DATABASE_IDENTIFIER==null) {
						// find all databases for which property files are available
						ArrayList<String> databaseList = new ArrayList<String>();
						String[] fileList = (new File(Settings.PROPERTIES_PATH)).list();
						if ((fileList != null) && (fileList.length > 0)) {
							for (String file : fileList) {
								if (file.startsWith("database") && file.endsWith(".properties")) {
									if (validateDatabasePropertyFile(Directory.ensureTrailingSlash(Settings.PROPERTIES_PATH)+file)==true) {
										databaseList.add(file.substring(8, file.length()-11));
									}
									else {
										throw new IllegalArgumentException("The following property file has wrong format: " + file);
									}
								}
							}
						}
						ArrayList<String> databaseAndOperationsList = new ArrayList<String>(databaseList);
						databaseAndOperationsList.add(rb.getString("newDatabase_databaseAndOperationsList"));
						databaseAndOperationsList.add(rb.getString("editDatabase_databaseAndOperationsList"));

						String answer = (String)JOptionPane.showInputDialog(
								new JFrame(), rb.getString("text_dialogChooseDB"), rb.getString("title_dialogChooseDB"),
								JOptionPane.QUESTION_MESSAGE, null,
								databaseAndOperationsList.toArray(), databaseAndOperationsList.get(0));
						if (answer==null) {
							System.exit(1);
						}
						else {
							// ask for parameters for the new database
							if (answer.equals(rb.getString("newDatabase_databaseAndOperationsList"))) {
								JLabel labelDBType = new JLabel(rb.getString("text_labelDBType"));
								JRadioButton radioButtonMySQL = new JRadioButton(rb.getString("text_radioButtonMySQL"));
								radioButtonMySQL.setActionCommand("MySQL");
								JRadioButton radioButtonSQLite = new JRadioButton(rb.getString("text_radioButtonSQLite"));
								radioButtonSQLite.setActionCommand("SQLite");
								ButtonGroup buttonGroupDBType = new ButtonGroup();
								buttonGroupDBType.add(radioButtonMySQL);
								buttonGroupDBType.add(radioButtonSQLite);
								radioButtonMySQL.setSelected(true);
								Object[] messageDBType = {labelDBType, radioButtonMySQL, radioButtonSQLite};
								JOptionPane paneDBType = new JOptionPane(messageDBType, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
								paneDBType.createDialog(null, rb.getString("title_dialogDBType")).setVisible(true);

								// MySQL
								if (buttonGroupDBType.getSelection().getActionCommand().equals("MySQL")) {
									JTextField jTFdatabase = new JTextField();
									JTextField jTFhostname = new JTextField();
									JTextField jTFuser = new JTextField();
									JTextField jTFpassword = new JTextField();
									Object[] message = {rb.getString("message1_parametersMySQL"), jTFdatabase, rb.getString("message2_parametersMySQL"), jTFhostname, rb.getString("message3_parametersMySQL"), jTFuser, rb.getString("message4_parametersMySQL"), jTFpassword };

									JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
									pane.createDialog(null, rb.getString("title_parametersMySQL")).setVisible(true);
									String dbName = jTFdatabase.getText();
									String dbHost = jTFhostname.getText();
									String dbUser = jTFuser.getText();
									String dbPass = jTFpassword.getText();

									if ((dbName.length()==0) || (dbHost.length()==0) || (dbUser.length()==0)) {
										JOptionPane.showMessageDialog(new JFrame(), rb.getString("text_popupMissingParametersMySQL"), rb.getString("title_popupMissingParametersMySQL"), JOptionPane.ERROR_MESSAGE);
										System.exit(1);
									}
									else {
										String databaseIdentifier = getDatabaseIdentifier(dbName);
										while (databaseList.contains(databaseIdentifier)) {
											JOptionPane.showMessageDialog(new JFrame(), String.format(rb.getString("format_popupDuplicateIdentifier"), databaseIdentifier), rb.getString("title_popupDuplicateIdentifier"), JOptionPane.WARNING_MESSAGE);
											databaseIdentifier = databaseIdentifier + "_NEW";
										}
										
										DATABASE_IDENTIFIER = databaseIdentifier;
										// create database and create empty tables
										try {
											// load driver 
											Class.forName("com.mysql.jdbc.Driver");

											try {
												// enable connection to SQL server
												String url = "jdbc:mysql://"+dbHost+":3306/";
												Connection connection = DriverManager.getConnection(url, dbUser, dbPass);

												// create database on server
												Statement stmt = connection.createStatement();
												String sql = "CREATE DATABASE " + dbName;
												stmt.executeUpdate(sql);
												connection.close();
											}
											catch (SQLException e) {
												e.printStackTrace();
												JOptionPane.showMessageDialog(new JFrame(), rb.getString("text_popupCreationFailedMySQL")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupCreationFailedMySQL"), JOptionPane.ERROR_MESSAGE);
												System.exit(1);
											}

											try {
												// enable connection to new database
												String urlDB = "jdbc:mysql://"+dbHost+":3306/"+dbName;
												Connection connectionDB = DriverManager.getConnection(urlDB, dbUser, dbPass);
												// create empty tables (load schema)
												Reader reader = new FileReader(String.format(SCHEMA_PATH, "MySQL"));
												SQLReader sqlReader = new SQLReader(connectionDB, reader);
												sqlReader.runScript();
												connectionDB.close();
											}
											catch (SQLException | IOException e) {
												e.printStackTrace();
												JOptionPane.showMessageDialog(new JFrame(), rb.getString("text_popupSchemaFailedMySQL")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupSchemaFailedMySQL"), JOptionPane.ERROR_MESSAGE);
												System.exit(1);
											}

										}
										catch (ClassNotFoundException e) {
											JOptionPane.showMessageDialog(new JFrame(), rb.getString("text_driverFailedMySQL"), rb.getString("title_driverFailedMySQL"), JOptionPane.ERROR_MESSAGE);
											System.exit(1);
										}
										
										// database was successfully created -> write parameter file
										String databaseSettingsFileName = Database.getFileName(databaseIdentifier);
										FileWriter writer = new FileWriter(databaseSettingsFileName);
										writer.write("DBTYPE = " + "MySQL");
										writer.write(System.getProperty("line.separator"));
										writer.write("DBNAME = " + dbName);
										writer.write(System.getProperty("line.separator"));
										writer.write("DBHOST = " + dbHost);
										writer.write(System.getProperty("line.separator"));
										writer.write("DBUSER = " + dbUser);
										writer.write(System.getProperty("line.separator"));
										writer.write("DBPASSWORD = " + dbPass);
										writer.write(System.getProperty("line.separator"));

										JFileChooser jfc = new JFileChooser();
										jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
										jfc.setDialogTitle(rb.getString("title_jfcOutputfolder"));
										jfc.showOpenDialog(new JFrame());
										writer.write("OUTPUTFOLDER = " + jfc.getSelectedFile().getAbsolutePath().replace("\\", "/"));
										writer.flush();
										writer.close();

										// open DataBaseManagementDialog
										new DatabaseManagementDialog();

									}
								}

								// SQLite
								else {
									// get path of file containing the (new) SQLite database
									JFileChooser fileChooserDB = new JFileChooser();
									fileChooserDB.setDialogTitle(rb.getString("title_jfcFileSQLite"));
									fileChooserDB.showSaveDialog(new JFrame());
									File selectedFile = fileChooserDB.getSelectedFile();
									if (selectedFile==null) {
										System.exit(1);
									}
									boolean sqliteExists;
									if (selectedFile.exists()) {
										sqliteExists = true;
									}
									else {
										sqliteExists = false;
										selectedFile.createNewFile();
									}
									String filename = selectedFile.getAbsolutePath().replace("\\", "/");
									String filenamePure = filename.substring(filename.lastIndexOf("/"), filename.lastIndexOf("."));
									String databaseIdentifier = getDatabaseIdentifier(filenamePure);
									while (databaseList.contains(databaseIdentifier)) {
										JOptionPane.showMessageDialog(new JFrame(),  String.format("format_popupDuplicateIdentifier", databaseIdentifier), rb.getString("title_popupDuplicateIdentifier"), JOptionPane.WARNING_MESSAGE);
										databaseIdentifier = databaseIdentifier + "_NEW";
									}
									
									DATABASE_IDENTIFIER = databaseIdentifier;
									if (!sqliteExists) {
										// create database and create empty tables
										try {
											// load driver 
											Class.forName("org.sqlite.JDBC");

											try {
												// enable connection to new database
												String urlDB = "jdbc:sqlite:"+filename;
												Connection connectionDB = DriverManager.getConnection(urlDB);
												// create empty tables (load schema)
												Reader reader = new FileReader(String.format(SCHEMA_PATH, "SQLite"));
												SQLReader sqlReader = new SQLReader(connectionDB, reader);
												sqlReader.runScript();
												connectionDB.close();
											}
											catch (SQLException | IOException e) {
												e.printStackTrace();
												JOptionPane.showMessageDialog(new JFrame(), rb.getString("text_popupSchemaFailedSQLite")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_popupSchemaFailedSQLite"), JOptionPane.ERROR_MESSAGE);
												System.exit(1);
											}

										}
										catch (ClassNotFoundException e) {
											e.printStackTrace();
											JOptionPane.showMessageDialog(new JFrame(), rb.getString("text_driverFailedSQLite")+(e.getMessage() != null ? e.getMessage() : e), rb.getString("title_driverFailedSQLite"), JOptionPane.ERROR_MESSAGE);
											System.exit(1);
										}
									}
									
									// database was successfully created -> write parameter file
									String databaseSettingsFileName = Database.getFileName(databaseIdentifier);

									FileWriter writer = new FileWriter(databaseSettingsFileName);
									writer.write("DBTYPE = " + "SQLite");
									writer.write(System.getProperty("line.separator"));
									writer.write("FILENAME = " + filename);
									writer.write(System.getProperty("line.separator"));

									JFileChooser jfc = new JFileChooser();
									jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
									jfc.setDialogTitle(rb.getString("title_jfcOutputfolder"));
									jfc.showOpenDialog(new JFrame());
									writer.write("OUTPUTFOLDER = " + jfc.getSelectedFile().getAbsolutePath().replace("\\", "/"));
									writer.flush();
									writer.close();

									// open DataBaseManagementDialog
									new DatabaseManagementDialog();

								}


							}

							// ask which database shall be edited
							else if (answer.equals(rb.getString("editDatabase_databaseAndOperationsList"))) {
								String answerEditDatabase = (String)JOptionPane.showInputDialog(
										new JFrame(), rb.getString("text_dialogChooseDB"), rb.getString("title_dialogChooseDB"),
										JOptionPane.QUESTION_MESSAGE, null,
										databaseList.toArray(), databaseList.get(0));
								DATABASE_IDENTIFIER = answerEditDatabase;
								if (answerEditDatabase==null) {
									System.exit(1);
								}

								// open DataBaseManagementDialog
								new DatabaseManagementDialog();
							}

							// if an existing database is selected
							else {
								DATABASE_IDENTIFIER = answer;
							}
						}
					}

					GeoLingGUI frame = new GeoLingGUI();
					frame.setVisible(true);
					frame.setTitle(frame.rb.getString("title_frame"));
					ImageIcon icon16 = new ImageIcon("geoling_16x16.png");
					ImageIcon icon32 = new ImageIcon("geoling_32x32.png");
					ImageIcon icon64 = new ImageIcon("geoling_64x64.png");
					frame.setIconImages(Arrays.asList(new Image[] { icon16.getImage(), icon32.getImage(), icon64.getImage() }));
				} catch (IOException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(new JFrame(), "There occured an IOException while reading/writing files: " + (e.getMessage() != null ? e.getMessage() : e), "IOException occured", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	
	
	/**
	 * Opens a <code>JTabbedPane</code> to which several other panels are added. This panels always
	 * contain a <code>JPanel</code> and are defined in other classes.<br>
	 * Moreover, it shows the first panel containing a <code>JNameTree</code> that shows all maps
	 * in their given category.
	 */
	@SuppressWarnings("unchecked")
	public GeoLingGUI() throws IOException {

		// load resource bundle
		rb = ResourceBundle.getBundle("GeoLingGUI", LANGUAGE);


		// read database settings
		InputStream databaseSettingsInputStream = new FileInputStream(Database.getFileName(DATABASE_IDENTIFIER));
		Properties databaseSettings = new Properties();
		databaseSettings.load(databaseSettingsInputStream);

		// connect to database
		if (!Base.hasConnection()) {
			Database.connect(databaseSettings);
		}

		// get path of standard output folder for exports
		outputfolder = databaseSettings.getProperty("OUTPUTFOLDER");


		// now setup GUI

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		if (Toolkit.getDefaultToolkit().getScreenSize().getWidth() < 1024 ||
		    Toolkit.getDefaultToolkit().getScreenSize().getHeight() < 768) {
			setBounds(0, 0, 800, 576);
		} else {
			setBounds(0, 0, 1024, 728);
		}
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPane.add(tabbedPane);

		tabbedPane.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					if (tabbedPane.getSelectedIndex() > TAB_INDEX_EXPORT) {
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item = new JMenuItem(rb.getString("text_popupItem"));
						item.addActionListener(new ActionListener() {

							public void actionPerformed(ActionEvent e) {
								tabbedPane.remove(tabbedPane.getSelectedIndex());
							}
						});
						popup.add(item);
						popup.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}
		});

		panelCategory = new JPanel();
		tabbedPane.addTab(rb.getString("title_panelCategory"), null, panelCategory, null);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 200, 100, 0 };
		gridBagLayout.rowHeights = new int[] { 100, 39, 39, 39, 0 };
		gridBagLayout.columnWeights = new double[] { 0.5, 0.5, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.95, 0.01, 0.01, 0.01, Double.MIN_VALUE };
		panelCategory.setLayout(gridBagLayout);

		// JNameTree for category, 1. Tab
		treeCategory = new JNameTree();
		treeCategory.addKeyListener(new KeyAdapter() {

			/**
			 * @param arg0
			 *            pressed key F2 in the JTree
			 * @return a new frame for editing name of map
			 */
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_F2) {
					if (treeCategory.getLastSelectedPathComponent() != null) {
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeCategory.getLastSelectedPathComponent();
						if (node.getUserObject() instanceof Map) {
							Map map = (Map) node.getUserObject();
							JTextField newMapName = new JTextField();
							newMapName.setText(map.getString("name"));
							Object[] message = { rb.getString("message_dialogMapName"), newMapName };
							JOptionPane dialogMapName = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
							dialogMapName.createDialog(null, rb.getString("title_dialogMapName")).setVisible(true);
							String eingabe = newMapName.getText();
							if (eingabe != null) {
								map.set("name", eingabe);
								map.saveIt();
							}
						}
					}
				}
			}
		});
		treeCategory.addMouseListener(new MouseAdapter() {

			/**
			 * @param arg0
			 *            double-klick on an element in the JTree
			 * @return a new tabbedPane for the selected map
			 */
			public void mouseClicked(MouseEvent arg0) {
				if (arg0.getClickCount() == 2) {
					if (treeCategory.getPathForLocation(arg0.getX(), arg0.getY()) != null) {
						if (selectedMap != null) {
							new MapPanel(tabbedPane, outputfolder, selectedMap);
						}

					}
				}
			}
		});
		treeCategory.addTreeSelectionListener(new TreeSelectionListener() {

			/**
			 * @param arg0
			 *            selected element in the JTree
			 * @return set of selectedMap and tableTags shows tags about the
			 *         selected node
			 */
			public void valueChanged(TreeSelectionEvent arg0) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeCategory.getLastSelectedPathComponent();

				if (node != null && node.getUserObject() instanceof Model) {
					// fill tableTags
					Model model = (Model) node.getUserObject();
					LazyList<Tag> tags = model.getAll(Tag.class);

					if (node.isRoot() && tags.isEmpty() && (infoImageLabel != null)) {
						scrollPaneTagTable.setViewportView(infoImageLabel);
					} else {
						String[][] tagsContents = new String[tags.size()][2];
						String[] columns = new String[] { rb.getString("columnName1_tableTags"), rb.getString("columnName2_tableTags") };
						for (int i = 0; i < tags.size(); i++) {
							tagsContents[i][0] = tags.get(i).getString("name");
							tagsContents[i][1] = tags.get(i).getString("value");
						}
						tableTags = new MultiLineTable(tagsContents, columns);
						MultiLineCellRenderer multiLineCR = new MultiLineCellRenderer();
						tableTags.getColumnModel().getColumn(0).setCellRenderer(multiLineCR);
						tableTags.getColumnModel().getColumn(1).setCellRenderer(multiLineCR);
						scrollPaneTagTable.setViewportView(tableTags);
					}

					if (node.getUserObject() instanceof Map) { // map selected
						selectedMap = (Map) node.getUserObject();
					} else { // category selected
						selectedMap = null;
					}
				} else {
					selectedMap = null;
				}
			}
		});

		LazyList<Map> mapsList = Map.findAll();
		HashMap<Object, Map> maps = ModelHelper.toHashMap(mapsList);

		// Defines a root node of the tree and adds the descendants
		DefaultMutableTreeNode rootNode = null;
		LazyList<Category> categories = Category.findAll().orderBy("lft").include(CategoriesMaps.class);
		LinkedList<DefaultMutableTreeNode> parents = new LinkedList<DefaultMutableTreeNode>();
		for (Category category : categories) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(category);
			Object parent_id = category.get("parent_id");
			if (parent_id == null) {
				parents.clear();
				if (rootNode != null) {
					throw new RuntimeException("Only one root node allowed!");
				}
				rootNode = node;
			} else {
				while (!((Category) (parents.getLast().getUserObject())).getId().equals(parent_id)) {
					parents.removeLast();
				}
				parents.getLast().add(node);
			}
			parents.add(node);

			LazyList<CategoriesMaps> cms = category.getAll(CategoriesMaps.class);
			ArrayList<Map> mapsInCategory = new ArrayList<Map>(cms.size());
			for (CategoriesMaps cm : cms) {
				mapsInCategory.add(maps.get(cm.get("map_id")));
			}
			Collections.sort(mapsInCategory);
			for (Map map : mapsInCategory) {
				node.add(new DefaultMutableTreeNode(map));
			}
		}

		treeCategory.setModel(new DefaultTreeModel(rootNode));

		// JScroll Pane for JNameTree with categories
		GridBagConstraints gbc_scrollPaneJNameTree = new GridBagConstraints();
		gbc_scrollPaneJNameTree.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPaneJNameTree.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneJNameTree.gridheight = 4;
		gbc_scrollPaneJNameTree.gridx = 0;
		gbc_scrollPaneJNameTree.gridy = 0;
		scrollPaneJNameTree.setViewportView(treeCategory);
		panelCategory.add(scrollPaneJNameTree, gbc_scrollPaneJNameTree);

		// show GeoLing info graphics instead of tags (if present)
		if (new File("geoling-info.png").exists()) {
			infoImageLabel = new JLabel(new ImageIcon("geoling-info.png"));
			infoImageLabel.setVerticalAlignment(SwingConstants.TOP);
			infoImageLabel.setBackground(Color.WHITE);
			infoImageLabel.setOpaque(true);
			infoImageLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent event) {
					try {
						Desktop.getDesktop().browse(new URI("http://www.geoling.net/"));
					} catch (URISyntaxException | IOException e) {
					}
				}
			});
			infoImageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		} else {
			// MultiLineTable for tags of category or map in the JNameTree
			Object[][] data = new Object[][] { { "", "" } };
			Object[] columns = new Object[] { rb.getString("columnName1_tableTags"), rb.getString("columnName2_tableTags") };
			tableTags = new MultiLineTable(data, columns);
			MultiLineCellRenderer multiLineCR = new MultiLineCellRenderer();
			tableTags.getColumnModel().getColumn(0).setCellRenderer(multiLineCR);
			tableTags.getColumnModel().getColumn(1).setCellRenderer(multiLineCR);
		}

		// JScroll Pane for MultiLineTable with tags
		GridBagConstraints gbc_scrollPaneTagTable = new GridBagConstraints();
		gbc_scrollPaneTagTable.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPaneTagTable.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneTagTable.gridx = 1;
		gbc_scrollPaneTagTable.gridy = 0;
		if (infoImageLabel != null) {
			scrollPaneTagTable.setViewportView(infoImageLabel);
		} else {
			scrollPaneTagTable.setViewportView(tableTags);
		}
		panelCategory.add(scrollPaneTagTable, gbc_scrollPaneTagTable);
		
		JButton buttonOpenMap = new JButton(rb.getString("text_buttonOpenMap"));
		GridBagConstraints gbc_buttonOpenMap = new GridBagConstraints();
		gbc_buttonOpenMap.insets = new Insets(0, 0, 5, 5);
		gbc_buttonOpenMap.fill = GridBagConstraints.BOTH;
		gbc_buttonOpenMap.gridx = 1;
		gbc_buttonOpenMap.gridy = 1;
		panelCategory.add(buttonOpenMap, gbc_buttonOpenMap);
		buttonOpenMap.addActionListener(new ActionListener() {
			/** Open tab for map. */
			public void actionPerformed(ActionEvent arg0) {
				if (selectedMap != null) {
					new MapPanel(tabbedPane, outputfolder, selectedMap);
				}
			}
		});
		
		JButton buttonOpenFactorAnalysis = new JButton(rb.getString("text_buttonOpenFactorAnalysis"));
		GridBagConstraints gbc_buttonStartFactorAnalysis = new GridBagConstraints();
		gbc_buttonStartFactorAnalysis.insets = new Insets(0, 0, 5, 5);
		gbc_buttonStartFactorAnalysis.fill = GridBagConstraints.BOTH;
		gbc_buttonStartFactorAnalysis.gridx = 1;
		gbc_buttonStartFactorAnalysis.gridy = 2;
		panelCategory.add(buttonOpenFactorAnalysis, gbc_buttonStartFactorAnalysis);
		buttonOpenFactorAnalysis.addActionListener(new ActionListener() {
			/** Open tab for factor analysis. */
			public void actionPerformed(ActionEvent arg0) {
				new FactorAnalysisPanel(tabbedPane, outputfolder);
			}
		});
		
		JButton buttonOpenClusterAnalysis = new JButton(rb.getString("text_buttonOpenClusterAnalysis"));
		GridBagConstraints gbc_buttonOpenClusterAnalysis = new GridBagConstraints();
		gbc_buttonOpenClusterAnalysis.insets = new Insets(0, 0, 5, 5);
		gbc_buttonOpenClusterAnalysis.fill = GridBagConstraints.BOTH;
		gbc_buttonOpenClusterAnalysis.gridx = 1;
		gbc_buttonOpenClusterAnalysis.gridy = 3;
		panelCategory.add(buttonOpenClusterAnalysis, gbc_buttonOpenClusterAnalysis);
		buttonOpenClusterAnalysis.addActionListener(new ActionListener() {
			/** Open tab for cluster analysis. */
			public void actionPerformed(ActionEvent arg0) {
				new ClusterAnalysisPanel(tabbedPane, outputfolder);
			}
		});
		
		
//		JButton buttonOpenTesting = new JButton(rb.getString("text_buttonOpenTesting"));
//		GridBagConstraints gbc_buttonOpenTesting = new GridBagConstraints();
//		gbc_buttonOpenTesting.insets = new Insets(0, 0, 5, 5);
//		gbc_buttonOpenTesting.fill = GridBagConstraints.BOTH;
//		gbc_buttonOpenTesting.gridx = 1;
//		gbc_buttonOpenTesting.gridy = 4;
//		panelCategory.add(buttonOpenTesting, gbc_buttonOpenTesting);
//		buttonOpenTesting.addActionListener(new ActionListener() {
//			/** Open tab for testing. */
//			public void actionPerformed(ActionEvent arg0) {
//				new TestingPanel(tabbedPane, outputfolder);
//			}
//		});
//		buttonOpenTesting.setEnabled(false);

		new GroupPanel(tabbedPane, treeCategory.getModel());

		new ExportPanel(tabbedPane, outputfolder);


		

	}

	
	
	
	/**
	 * Returns an <code>ArrayList</code> with the available <Locale> objects that were found
	 * in jar-package <i>geoling-gui-languages.jar</i>.
	 */
	private static ArrayList<Locale> findAllAvailableLocales() {
		String[] isoLanguages = Locale.getISOLanguages();
		ArrayList<Locale> foundLocales = new ArrayList<Locale>();

		for (String language : isoLanguages) {
			URL rb = ClassLoader.getSystemResource("GeoLingGUI_"+language+".properties");
			if(rb!=null) {
				foundLocales.add(new Locale(language));
			}
		}

		if (foundLocales.size()==0) {
			throw new IllegalArgumentException("No resource bundles (=supported languages) in package geoling-gui-languages.jar were found!");
		}
		else {
			return foundLocales;
		}
	}

	/**
	 * Validates property file, i.e. it looks whether all required fields are available.
	 * 
	 * @param path the path to the property file
	 * @return the name of the database
	 * @throws IOException 
	 */
	private static boolean validateDatabasePropertyFile(String path) throws IOException {
		boolean isValid = true;
		Properties properties = new Properties();
		FileInputStream fis = new FileInputStream(path);
		properties.load(fis);
		String type = properties.getProperty("DBTYPE");
		if ((type==null) || (type.equals(""))) {
			System.err.println("The property file " + path + " must contain the key DBTYPE !");
			isValid = false;
		}
		else {
			if (type.equals("SQLite")) {
				String[] keys = new String[] {"FILENAME", "OUTPUTFOLDER"};
				for (String key : keys) {
					if (!properties.containsKey(key)) {
						System.err.println("The property file " + path + " must contain the key " + key + "!");
						isValid = false;
					}
				}
			}
			else if (type.equals("MySQL")) {
				String[] keys = new String[] {"DBNAME", "DBHOST", "DBUSER", "DBPASSWORD", "OUTPUTFOLDER"};
				for (String key : keys) {
					if (!properties.containsKey(key)) {
						System.err.println("The property file " + path + " must contain the key " + key + "!");
						isValid = false;
					}
				}
			}
			else {
				fis.close();
				throw new IllegalArgumentException("DBTYPE = SQLite or DBTYPE = MySQL required!");
			}
		}
		fis.close();
		return isValid;
	}


	private static String getDatabaseIdentifier(String dbName) {
		String identifier = dbName.replaceAll(" ", "_").replaceAll("[\\\\/:*?\"<>|]", "");
		if (LANGUAGE!=null) {
			identifier = identifier.toUpperCase(LANGUAGE);
		}
		else {
			identifier = identifier.toUpperCase();
		}
		return identifier;
	}

}
