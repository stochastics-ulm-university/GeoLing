/**
 * GeoLing - a statistical software package for geolinguistic data.
 * 
 * <h3>What is GeoLing and what does it do?</h3>
 * <p>
 * In short, GeoLing is a handy tool for performing statistical analyses on spatial
 * data: You can use data from dialect surveys, transform them into smoothed maps
 * (via density estimation), detect structures that run through the data and find
 * groups of maps that share spatial features.
 * We developed this program with linguistic applications in mind, but that should
 * not stop you from using it for any kind of spatially conditioned data.
 * 
 * <h3>Acknowledgements</h3>
 * <p>
 * GeoLing was written as a part of the project <i>Neue Dialektometrie mit Methoden der
 * stochastischen Bildanalyse</i> ("new dialectometry using methods of stochastic image
 * analysis"), financed by the Deutsche Forschungsgemeinschaft (DFG) between 2008 and
 * 2014. All work has been carried out by people associated with three institutions:
 * <ul>
 * <li>Institute of Stochastics (Ulm University)
 * <li>Lehrstuhl fuer Deutsche Sprachwissenschaft (University of Augsburg)
 * <li>Fachbereich Germanistik (University of Salzburg)
 * </ul>
 * 
 * <h3>License and terms of usage</h3>
 * This software is licensed under the GNU General Public License v3.0 (published on
 * 29 June 2007). The full text of GPL 3 is available at <a href="https://www.gnu.org/licenses/gpl-3.0">gnu.org</a>.
 * Although we took reasonable precautions and testing, we like to stress that the
 * software comes without any warranty or guarantee.
 * 
 * <h3>First steps</h3>
 * <p>
 * To start the graphical user interface (GUI), execute <code>geoling.gui.GeoLingGUI</code>.
 * <p>
 * Note that the <code>activejdbc</code> library is used to work with databases.
 * It requires an instrumentation of the generated Java class files, which can be
 * simply done manually by executing <code>geoling.tools.MakeInstrumentationUtil</code>.
 * <p>
 * Have a look at <code>geoling.tools.PlotExampleMap</code> to see how
 * to establish a database connection, load map data, perform kernel density
 * estimation and plot images with your own code. This example covers already
 * the most important classes.
 * 
 * @author Institute of Stochastics, Ulm University
 * @see <a href="http://www.geoling.net/">GeoLing homepage</code>
 */
package geoling;