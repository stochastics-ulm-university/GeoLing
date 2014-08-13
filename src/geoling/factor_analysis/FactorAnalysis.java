package geoling.factor_analysis;

import geoling.factor_analysis.util.Factor;
import geoling.factor_analysis.util.FactorLoadings;
import geoling.factor_analysis.util.ReconstructedVariantWeights;
import geoling.maps.weights.VariantWeights;
import geoling.models.Location;
import geoling.models.Map;
import geoling.models.Variant;
import geoling.util.ProgressOutput;
import geoling.util.XMLExport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Factor analysis for maps.
 * 
 * @author student assistant, Institute of Stochastics, Ulm University
 * @see "S. Pröll, S. Pickl and A. Spettl, Latente Strukturen in geolinguistischen Korpora.
 *       In: M. Elmentaler and M. Hundt (eds.): Deutsche Dialekte. Konzepte, Probleme,
 *       Handlungsfelder. 4. Kongress der Internationalen Gesellschaft für Dialektologie des
 *       Deutschen (IGDD), Kiel vom 13.-15. September 2012. Steiner, Stuttgart (to appear)."
 * @see "Julius Vogelbacher, Statistische Analyse von Wortvarianten des Sprachatlas von
 *       Bayerisch-Schwaben, Universität Ulm, Diplomarbeit, Kapitel 3"
 * @see "Algorithumus Varimax-Rotation: Multivariate Statistik von Hartung und Elpelt,
 *       5. Auflage, S. 551"
 */
public class FactorAnalysis {
	
	private double dataMatrix[][];
	private double[][] lRot;
	private double[][] factorScores;
	private int varSize;
	private int dataSize;
	private ArrayList<VariantWeights> variantWeightsList;
	private ArrayList<Location> locationsAll;
	private ArrayList<Variant> variantsAll;
	private Integer numberOfFactors;
	
	/**
	 * Constructs the new object for the factor analysis for the given maps data.
	 * 
	 * @param variantWeightsList  a list of the <code>VariantWeights</code> objects
	 *                            for the maps which are used in the factor analysis
	 * @param numberOfFactors     the number of factors, will be selected with
	 *                            Kaiser criterion if <code>null</code>
	 */
	public FactorAnalysis(List<VariantWeights> variantWeightsList, Integer numberOfFactors) {
		this.variantWeightsList = new ArrayList<VariantWeights>(variantWeightsList);
		this.numberOfFactors    = numberOfFactors;
		
		HashSet<Location> locationsAllSet = new HashSet<Location>();
		dataSize = 0;
		HashMap<Variant, HashMap<Location, Double>> data = new HashMap<Variant, HashMap<Location, Double>>();
		for (VariantWeights variantWeights : variantWeightsList) {
			HashSet<Variant> variants = variantWeights.getVariants();
			ArrayList<Location> locations = new ArrayList<Location>(variantWeights.getLocations());
			locationsAllSet.addAll(locations);
			dataSize += variants.size();
			for (Variant variant : variants) {
				HashMap<Location, Double> d = new HashMap<Location, Double>();
				for (Location location : locations) {
					d.put(location, variantWeights.getWeight(variant, location));
				}
				data.put(variant, d);
			}
		}
		
		// sort locations by ID
		locationsAll = new ArrayList<Location>(locationsAllSet);
		Collections.sort(locationsAll, new Comparator<Location>() {
			public int compare(Location o1, Location o2) {
				return (int)(o1.getLongId()-o2.getLongId());
			}
		});
		
		// sort variants by map (and variant, if maps are equal), using natural ordering
		variantsAll = new ArrayList<Variant>(data.keySet());
		Collections.sort(variantsAll, new Comparator<Variant>() {
			public int compare(Variant o1, Variant o2) {
				int result = o1.parent(Map.class).compareTo(o2.parent(Map.class));
				if (result == 0) {
					result = o1.compareTo(o2);
				}
				return result;
			}
		});
		
		varSize = locationsAll.size();
		dataMatrix = new double[varSize][dataSize];
		int i = 0;
		for (Variant variant : variantsAll) {
			HashMap<Location, Double> d = data.get(variant);
			for (int j = 0; j < varSize; j++) {
				Double w = d.get(locationsAll.get(j));
				if (w != null) {
					dataMatrix[j][i] = w;
				}
			}
			i++;
		}
	}
	
	
	/**
	 * Returns the <code>VariantWeights</code> objects for the maps
	 * which are used in the factor analysis.
	 * 
	 * @return the data of the maps
	 */
	public List<VariantWeights> getVariantWeightsList() {
		return Collections.unmodifiableList(variantWeightsList);
	}
	
	/**
	 * Returns all locations as a list.
	 * 
	 * @return all locations as a list, in the order used in matrices
	 */
	public ArrayList<Location> getLocations() {
		return locationsAll;
	}
	
	/**
	 * Returns all variants as a list.
	 * 
	 * @return all variants as a list, in the order used in matrices
	 */
	public ArrayList<Variant> getVariants() {
		return variantsAll;
	}
	
	/**
	 * Returns the number of factors.
	 * 
	 * @return the number of factors, <code>null</code> if not yet determined
	 */
	public Integer getNumberOfFactors() {
		return numberOfFactors;
	}
	
	/**
	 * Returns the number of variables, i.e., the number of locations.
	 * 
	 * @return the number of variables
	 */
	public int getVarSize() {
		return varSize;
	}
	
	/**
	 * Returns the number of observations, i.e., the number of variants.
	 * 
	 * @return the number of observations
	 */
	public int getDataSize() {
		return dataSize;
	}
	
	/**
	 * Calculates the rotated factor loadings and writes it in lrot
	 * 
	 * @param progress  object for output of current progress
	 */
	public void calculateFactorLoadings(ProgressOutput progress) {
		progress.reset(10);
		progress.initCurrent();
		
		double[][] z = standardise(dataMatrix);
		Matrix m = Matrix.constructWithCopy(z);
		progress.incrementCurrent();
		
		Matrix cor = (m.times(m.transpose())).times(1 / (double) (dataMatrix[0].length - 1));
		progress.incrementCurrent();
		
		Matrix inverscor = cor.inverse();
		double[][] uqa = new double[inverscor.getColumnDimension()][inverscor.getColumnDimension()];
		for (int i = 0; i < inverscor.getColumnDimension(); i++) {
			uqa[i][i] = 1 / inverscor.getArray()[i][i];
		}
		progress.incrementCurrent();
		
		Matrix corh = cor.minus(Matrix.constructWithCopy(uqa));
		EigenvalueDecomposition corhEig = corh.eig();
		double[] e = corhEig.getRealEigenvalues();
		double[] eimag = corhEig.getImagEigenvalues();
		Matrix ev = corhEig.getV();
		corhEig = null;
		progress.incrementCurrent();
		
		if (numberOfFactors == null) {
			// Kaiser criterion
			int faktorenzahl = 0;
			for (int i = 0; i < e.length; i++) {
				if (e[i] > 1) {
					faktorenzahl++;
				}
			}
			numberOfFactors = faktorenzahl;
		}
		
		Matrix ds = Matrix.identity(numberOfFactors, numberOfFactors);
		for (int i = 0; i < ds.getColumnDimension(); i++) {
			ds.set(ds.getColumnDimension() - (i + 1), ds.getColumnDimension() - (i + 1), Math.sqrt(e[e.length - (i + 1)]));
			if (eimag[e.length - (i + 1)] != 0) {
				throw new ArithmeticException("Komplexe Eigenwerte - Faktorenanalyse der Form Hauptkomponentenanalyse kann nicht angewendet werden:");
			}
		}
		
		Matrix l = ev.getMatrix(0, (dataMatrix.length - 1), (dataMatrix.length - numberOfFactors), (dataMatrix.length - 1)).times(ds);
		progress.incrementCurrent();
		
		double com[] = null;
		double max = 1;
		// iterative computation of loading matrix L
		while (max > Math.pow(10, -6)) {
			double[][] ll = l.times(l.transpose()).getArray();
			for (int i = 0; i < corh.getColumnDimension(); i++) {
				corh.set(i, i, ll[i][i]);
			}
			com = new double[ll.length];
			for (int i = 0; i < ll.length; i++) {
				com[i] = ll[i][i];
			}
			
			corhEig = corh.eig();
			e = corhEig.getRealEigenvalues();
			ev = corhEig.getV();
			corhEig = null;
			
			for (int i = 0; i < ds.getColumnDimension(); i++) {
				ds.set(ds.getColumnDimension() - (i + 1), ds.getColumnDimension() - (i + 1), Math.sqrt(e[e.length - (i + 1)]));
				if (eimag[e.length - (i + 1)] != 0) {
					throw new ArithmeticException("Komplexe Eigenwerte - Faktorenanalyse der Form Hauptkomponentenanalyse kann nicht angewendet werden:");
				}
			}
			
			l = ev.getMatrix(0, (dataMatrix.length - 1), (dataMatrix.length - numberOfFactors), (dataMatrix.length - 1)).times(ds);
			max = max(com, l.times(l.transpose()));
		}
		progress.incrementCurrent();
		progress.incrementCurrent();
		
		// rotation of loading matrix L
		lRot = varimax(l.getArray(), Math.pow(10, -5));
		// get as many positive values as possible (by multiplication with -1 or 1)
		// this multiplication has no effect on the factors
		lRot = multiplyColumsToObtainManyPositiveValues(lRot);
		// columns are sorted such that the factors explaining the highest variance form the first columns
		lRot = sortArrayByVariance(lRot);
		
		progress.incrementCurrent();
		
		// compute values of factors
		Matrix V = new Matrix(cor.getRowDimension(), cor.getColumnDimension());
		for (int i = 0; i < V.getRowDimension(); i++) {
			V.set(i, i, cor.get(i, i) - com[i]);
		}
		Matrix lRotM = new Matrix(lRot);
		Matrix lRotT = lRotM.transpose();
		Matrix invV = V.inverse();
		progress.incrementCurrent();
		
		Matrix F = lRotT.times(invV).times(lRotM).inverse().times(lRotT).times(invV).times(m).transpose();
		progress.incrementCurrent();
		
		factorScores = F.getArray();
	}
	
	/**
	 * Returns the matrix containing the factor loadings.
	 * 
	 * @return the rotated factor loadings; at position <code>[i][j]</code>, <code>i</code>
	 *         stands for the variable (i.e., the location) and <code>j</code> for the factor
	 */
	public double[][] getFactorLoadings() {
		return lRot;
	}
	
	/**
	 * Returns a <code>FactorLoadings</code> object containing the factor loadings
	 * prepared for visualization.
	 * 
	 * @return <code>FactorLoadings</code> object containing the factor loadings
	 *         prepared for visualization
	 */
	public FactorLoadings getFactorLoadingsObj() {
		FactorLoadings factorLoadings = new FactorLoadings();
		
		Factor[] factorPositive = new Factor[lRot[0].length];
		Factor[] factorNegative = new Factor[lRot[0].length];
		for (int j = 0; j < lRot[0].length; j++) {
			// compute explained variance
			double varPositive = 0.0;
			double varNegative = 0.0;
			for (int i = 0; i < lRot.length; i++) {
				if (lRot[i][j] >= 0.0) {
					varPositive += lRot[i][j]*lRot[i][j];
				} else {
					varNegative += lRot[i][j]*lRot[i][j];
				}
			}
			varPositive /= lRot.length;
			varNegative /= lRot.length;
			
			factorPositive[j] = new Factor(j + 1, false, varPositive);
			factorNegative[j] = new Factor(j + 1, true, varNegative);
		}
		
		for (int i = 0; i < lRot.length; i++) {
			Location location = locationsAll.get(i);
			HashMap<Factor, Double> h = new HashMap<Factor, Double>();
			for (int j = 0; j < lRot[i].length; j++) {
				h.put((lRot[i][j] >= 0.0) ? factorPositive[j] : factorNegative[j], Math.abs(lRot[i][j]));
			}
			factorLoadings.putVariantCounter(location, h);
		}
		
		return factorLoadings;
	}
	
	/**
	 * Returns the matrix containing the factor scores for all variants.
	 * 
	 * @return the factor scores; at position <code>[i][j]</code>, <code>i</code>
	 *         stands for the variant and <code>j</code> for the factor
	 */
	public double[][] getFactorScores() {
		return factorScores;
	}
	
	/**
	 * Reconstructs all the maps from the factor scores and factor loadings.
	 * 
	 * @return the reconstructed maps
	 */
	public HashMap<Map,ReconstructedVariantWeights> getReconstructedWeights() {
		double[] meanValues = new double[locationsAll.size()];
		double[] stddevValues = new double[locationsAll.size()];
		for (int i = 0; i < locationsAll.size(); i++) {
			meanValues[i] = mean(dataMatrix[i]);
			stddevValues[i] = standardDeviation(dataMatrix[i]);
		}
		
		double[][] reconstructedNormalized = new Matrix(lRot).times(new Matrix(factorScores).transpose()).getArray();
		
		HashMap<Map,ReconstructedVariantWeights> result = new HashMap<Map,ReconstructedVariantWeights>();
		for (VariantWeights original : variantWeightsList) {
			ReconstructedVariantWeights reconstructed = new ReconstructedVariantWeights(original.getMap());
			reconstructed.enforceLocations(locationsAll);
			
			for (Variant variant : original.getVariants()) {
				int j = variantsAll.indexOf(variant);
				for (int i = 0; i < locationsAll.size(); i++) {
					double weight = reconstructedNormalized[i][j] * stddevValues[i] + meanValues[i];
					reconstructed.putInformation(locationsAll.get(i), variant, weight);
				}				
			}
			
			result.put(original.getMap(), reconstructed);
		}
		
		return result;
	}
	
	/**
	 * Exports the results of this factor analysis to a XML file.
	 * 
	 * @param fileName  the file name for the new XML file
	 * @throws IOException if an I/O error occurs
	 */
	public void toXML(String fileName) throws IOException {
		try {
			XMLExport writer = new XMLExport(fileName);
			
			writer.XML.writeStartElement("factorAnalysis");
			writer.newLine();
			
			writer.XML.writeStartElement("option");
			writer.XML.writeAttribute("numberOfFactors", ""+this.numberOfFactors);
			writer.XML.writeEndElement();
			writer.newLine();
			
			writer.XML.writeStartElement("data");
			writer.newLine();
			
			writer.XML.writeStartElement("maps");
			writer.newLine();
			for (VariantWeights weights : this.variantWeightsList) {
				writer.XML.writeStartElement("map");
				writer.XML.writeAttribute("id", weights.getMap().getId().toString());
				writer.XML.writeAttribute("name", weights.getMap().getString("name"));
				writer.XML.writeAttribute("weights", weights.getIdentificationString());
				writer.XML.writeEndElement();
				writer.newLine();
			}
			writer.XML.writeEndElement(); // </maps>
			writer.newLine();
			
			writer.XML.writeStartElement("locations");
			writer.newLine();
			for (Location location : this.locationsAll) {
				writer.XML.writeStartElement("location");
				writer.XML.writeAttribute("id", location.getId().toString());
				writer.XML.writeAttribute("name", location.getString("name"));
				writer.XML.writeAttribute("latitude", String.format(Locale.ENGLISH, "%f", location.getLatLong().getLatitude()));
				writer.XML.writeAttribute("longitude", String.format(Locale.ENGLISH, "%f", location.getLatLong().getLongitude()));
				writer.XML.writeEndElement();
				writer.newLine();
			}
			writer.XML.writeEndElement(); // </locations>
			writer.newLine();
			
			writer.XML.writeEndElement(); // </data>
			writer.newLine();
			
			writer.XML.writeStartElement("results");
			writer.newLine();
			
			writer.XML.writeStartElement("factorLoadings");
			writer.newLine();
			for (int j = 0; j < this.lRot[0].length; j++) {
				writer.XML.writeStartElement("factor");
				writer.XML.writeAttribute("index", ""+j);
				writer.newLine();
				for (int i = 0; i < this.lRot.length; i++) {
					writer.XML.writeStartElement("location");
					writer.XML.writeAttribute("location_id", this.locationsAll.get(i).getId().toString());
					writer.XML.writeAttribute("value", String.format(Locale.ENGLISH, "%f", this.lRot[i][j]));
					writer.XML.writeEndElement();
					writer.newLine();
				}
				writer.XML.writeEndElement();
				writer.newLine();
			}
			writer.XML.writeEndElement(); // </factorLoadings>
			writer.newLine();
			
			writer.XML.writeStartElement("factorScores");
			writer.newLine();
			for (int i = 0; i < this.factorScores.length; i++) {
				Variant variant = this.variantsAll.get(i);
				Map map = variant.parent(Map.class);
				writer.XML.writeStartElement("variant");
				writer.XML.writeAttribute("id", variant.getId().toString());
				writer.XML.writeAttribute("name", variant.getString("name"));
				writer.XML.writeAttribute("map_id", map.getId().toString());
				writer.XML.writeAttribute("map_name", map.getString("name"));
				writer.newLine();
				for (int j = 0; j < this.factorScores[i].length; j++) {
					writer.XML.writeStartElement("factor");
					writer.XML.writeAttribute("index", ""+j);
					writer.XML.writeAttribute("value", String.format(Locale.ENGLISH, "%f", this.factorScores[i][j]));
					writer.XML.writeEndElement();
					writer.newLine();
				}
				writer.XML.writeEndElement();
				writer.newLine();
			}
			writer.XML.writeEndElement(); // </factorScores>
			writer.newLine();
			
			writer.XML.writeEndElement(); // </results>
			writer.newLine();
			
			writer.XML.writeEndElement(); // </factorAnalysis>
			
			writer.close();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}
	
	/**
	 * Sorts array such that the first column explains the highest variance
	 * @param l1 array that shall be sorted
	 * @return a sorted array
	 */
	private static double[][] sortArrayByVariance(double[][] l1) {
		double[][] lrot = l1.clone();
		double[][] l = new double[lrot[0].length][lrot.length];
		double[][] lrotcopy = new double[lrot.length][lrot[0].length];
		double[] explainedVariance = new double[l.length];
		double[] explainedVariance2 = new double[l.length];
		for (int i = 0; i < lrot.length; i++) {
			for (int j = 0; j < lrot[0].length; j++) {
				l[j][i] = lrot[i][j] * lrot[i][j];
			}
		}
		
		for (int i = 0; i < l.length; i++) {
			explainedVariance[i] = sum(l[i]);
			explainedVariance2[i] = sum(l[i]);
		}
		Arrays.sort(explainedVariance2);
		int zuordnung[] = new int[l.length];
		
		for (int i = 0; i < l.length; i++) {
			double ekv = explainedVariance[i];
			for (int j = 0; j < l.length; j++) {
				if (ekv == explainedVariance2[j]) {
					zuordnung[i] = l.length - (j + 1);
				}
			}
		}
		
		for (int i = 0; i < lrot.length; i++) {
			for (int j = 0; j < lrot[0].length; j++) {
				lrotcopy[i][zuordnung[j]] = lrot[i][j];
			}
		}
		
		return (lrotcopy);
	}
	
	/**
	 * Computes sum of the values in the array.
	 * @param m the array with values
	 * 
	 * @return the sum
	 */
	private static Double sum(double[] m) {
		double s = 0;
		for (int i = 0; i < m.length; i++) {
			s += m[i];
		}
		return (s);
	}
	
	/**
	 * Computes quadratic sum of the values in the array.
	 * @param m the array with values
	 * 
	 * @return the quadratic sum
	 */
	private static Double sumqad(double[] m) {
		double s = 0;
		for (int i = 0; i < m.length; i++) {
			s += m[i] * m[i];
		}
		return (s);
	}
	
	/**
	 * Multiplies columns by -1 to obtain as many positive values as possible.
	 * This operation has no effect on the factors.
	 * @param l array of loading matrix
	 * @return the new loading matrix with as as many positive values as possible
	 */
	private static double[][] multiplyColumsToObtainManyPositiveValues(double[][] l) {
		double[][] lrot = l.clone();
		int dim = lrot.length;
		int counter = 0;
		for (int i = 0; i < lrot[0].length; i++) {
			counter = 0;
			for (int j = 0; j < dim; j++) {
				if (lrot[j][i] > 0) {
					counter++;
				}
			}
			if (counter < dim * 0.5) {
				for (int j = 0; j < dim; j++) {
					lrot[j][i] = -lrot[j][i];
				}
			}
			counter = 0;
		}
		return (lrot);
	}
	
	/**
	 * Performs the varimax-method.
	 * 
	 * @param l1 the array of loading matrix
	 * @param pow the precision of rotation
	 * 
	 * @return the rotated array of loading matrix
	 * 
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static double[][] varimax(double[][] l1, double pow) {
		
		double[][] z = new double[l1.length][l1[0].length];
		double[] k = new double[z.length];
		
		for (int i = 0; i < k.length; i++) {
			k[i] = sumqad(l1[i]);
		}
		
		for (int i = 0; i < k.length; i++) {
			for (int j = 0; j < z[i].length; j++) {
				z[i][j] = l1[i][j] / Math.sqrt(k[i]);
			}
		}
		
		double[][] A = new double[z[0].length][z[0].length];
		double[][] B = new double[z[0].length][z[0].length];
		double[][] C = new double[z[0].length][z[0].length];
		double[][] D = new double[z[0].length][z[0].length];
		double[][] P = new double[z[0].length][z[0].length];
		double[][] Q = new double[z[0].length][z[0].length];
		double[][] theta = new double[z[0].length][z[0].length];
		
		for (int h = 0; h < 1000; h++) {
			double V = V(z);
			
			for (int k1 = 0; k1 < z[0].length; k1++) {
				for (int k2 = k1 + 1; k2 < z[0].length; k2++) {
					A[k1][k2] = A(z, k1, k2);
					B[k1][k2] = B(z, k1, k2);
					C[k1][k2] = C(z, k1, k2);
					D[k1][k2] = D(z, k1, k2);
				}
			}
			
			for (int k1 = 0; k1 < z[0].length; k1++) {
				for (int k2 = k1 + 1; k2 < z[0].length; k2++) {
					P[k1][k2] = P(z.length, D[k1][k2], A[k1][k2], B[k1][k2]);
					Q[k1][k2] = Q(z.length, C[k1][k2], A[k1][k2], B[k1][k2]);
					theta[k1][k2] = theta(P[k1][k2], Q[k1][k2]);
				}
			}
			
			Matrix dreieck = getTriangle(theta);
			Matrix Z = Matrix.constructWithCopy(z);
			Z = Z.times(dreieck);
			z = Z.getArray();
			double dif = Math.abs(V - V(z));
			// System.out.println("Iterationsschritt h: " + h + " V(h)-V(h-1): "
			// + Math.round(100000 * dif) / 100000. + " V(h): "
			// + Math.round(V(z)));
			if (dif < pow) {
				h = 200000;
			}
		}
		
		for (int i = 0; i < k.length; i++) {
			for (int j = 0; j < z[i].length; j++) {
				z[i][j] = z[i][j] * Math.sqrt(k[i]);
			}
		}
		
		return z;
	}
	
	/**
	 * 
	 * @param theta theta
	 * @return the array containing the triangle
	 * 
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static Matrix getTriangle(double[][] theta) {
		int q = theta.length;
		Matrix w = Matrix.identity(q, q);
		
		for (int i = 0; i < q - 1; i++) {
			for (int j = i + 1; j < q; j++) {
				Matrix d = Matrix.constructWithCopy(getTriangleSmall(theta[i][j], i, j, q));
				w = w.times(d);
			}
		}
		return (w);
	}
	
	/**
	 * @return the array containing the small triangle
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static double[][] getTriangleSmall(double theta, int i, int j, int q) {
		double werte[][] = new double[q][q];
		for (int i1 = 0; i1 < werte.length; i1++) {
			werte[i1][i1] = 1;
		}
		werte[i][i] = Math.cos(theta);
		werte[i][j] = -Math.sin(theta);
		werte[j][i] = Math.sin(theta);
		werte[j][j] = Math.cos(theta);
		return (werte);
		
	}
	
	/**
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static double A(double[][] z, int k1, int k2) {
		double s = 0;
		for (int j = 0; j < z.length; j++) {
			s += z[j][k1] * z[j][k1] - z[j][k2] * z[j][k2];
		}
		return (s);
	}
	
	/**
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static double B(double[][] z, int k1, int k2) {
		double s = 0;
		for (int j = 0; j < z.length; j++) {
			s += 2 * z[j][k1] * z[j][k2];
		}
		return (s);
	}
	
	/**
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static double C(double[][] z, int k1, int k2) {
		double s1 = 0;
		double s2 = 0;
		for (int j = 0; j < z.length; j++) {
			s1 += (z[j][k1] * z[j][k1] - z[j][k2] * z[j][k2]) * (z[j][k1] * z[j][k1] - z[j][k2] * z[j][k2]);
		}
		for (int j = 0; j < z.length; j++) {
			s2 += (2 * z[j][k1] * z[j][k2]) * (2 * z[j][k1] * z[j][k2]);
		}
		double s = s1 - s2;
		return (s);
	}
	
	/**
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static double D(double[][] z, int k1, int k2) {
		double s = 0;
		for (int j = 0; j < z.length; j++) {
			s += (z[j][k1] * z[j][k1] - z[j][k2] * z[j][k2]) * (2 * z[j][k1] * z[j][k2]);
		}
		s = 2 * s;
		return (s);
	}
	
	/**
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static double P(double p1, double D, double A, double B) {
		double P = 0;
		P = p1 * D - 2 * A * B;
		
		return (P);
	}
	
	/**
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static double Q(double p, double C, double A, double B) {
		double Q = 0;
		
		Q = p * C + A * A + B * B;
		
		return (Q);
	}
	
	/**
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static double E(double P, double Q) {
		if (P > 0) {
			if (Q > 0) {
				return (0);
			} else {
				return (Math.PI);
			}
		} else {
			if (Q > 0) {
				return (0);
			} else {
				return (-Math.PI);
			}
		}
	}
	
	/**
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static double theta(double P, double Q) {
		double theta = 0;
		theta = 0.25 * (E(P, Q) + Math.atan(P / Q));
		return (theta);
	}
	
	/**
	 * @see "Hartung Elpelt Multivariate Statistik 5 Auflage, page 551 ff., chapter 2.1.1"
	 */
	private static double V(double[][] z) {
		double s = 0;
		double s1 = 0;
		double s2 = 0;
		
		for (int k = 0; k < z.length; k++) {
			for (int j = 0; j < z[k].length; j++) {
				s1 += Math.pow(z[k][j], 4);
			}
		}
		
		for (int k = 0; k < z[0].length; k++) {
			double sq = 0;
			for (int j = 0; j < z.length; j++) {
				sq += z[j][k] * z[j][k];
			}
			s2 += sq * sq;
		}
		
		s = s1 * z.length - s2;
		
		return (s);
	}
	
	/**
	 * Computes the value which terminates the iteration.
	 * This iteration determines the loading matrix.
	 * 
	 * @param com commonality at iteration (n-1)
	 * @param times (loading matrix) * t(loading matrix) at iteration n
	 * 
	 * @return max(abs(commonality_n(i)-commonality_n-1(i))), where
	 *         i=1,...,number of factors
	 */
	private static double max(double[] com, Matrix times) {
		double[][] m = times.getArray();
		double diag[] = new double[m.length];
		for (int i = 0; i < diag.length; i++) {
			diag[i] = m[i][i];
		}
		double max = 0;
		for (int i = 0; i < com.length; i++) {
			if (max < Math.abs(com[i] - diag[i])) {
				max = Math.abs(com[i] - diag[i]);
			}
		}
		return max;
	}
	
	/**
	 * 
	 * @param m the data matrix
	 * 
	 * @return Z 
	 * 
	 * @see "Julius Vogelbacher, Statistische Analyse von Wortvarianten des Sprachatlas von
	 *       Bayerisch-Schwaben, Universität Ulm, Diplomarbeit, Kapitel 3.3.2"
	 */
	private static double[][] standardise(double[][] m) {
		double[][] ret = new double[m.length][m[0].length];
		double[] spalte = new double[ret[0].length];
		
		for (int i = 0; i < m.length; i++) {
			spalte = m[i];
			double mean = mean(spalte);
			double sd = standardDeviation(spalte);
			for (int j = 0; j < spalte.length; j++) {
				ret[i][j] = (m[i][j] - mean) / sd;
			}
		}
		
		return (ret);
	}
	
	/**
	 * Computes mean of the values in the array,
	 * 
	 * @param m the array with values
	 * 
	 * @return the mean
	 */
	private static double mean(double[] m) {
		double s = sum(m);
		double l = m.length;
		s = s / l;
		return (s);
	}
	
	/**
	 * Computes standard deviation of values in the array.
	 * @param m the array with values
	 * 
	 * @return the standard deviation
	 */
	private static double standardDeviation(double[] m) {
		double s = 0;
		double mean = mean(m);
		for (int i = 0; i < m.length; i++) {
			s += (m[i] - mean) * (m[i] - mean);
		}
		double l = m.length;
		s = Math.sqrt(s / (l - 1));
		return (s);
	}
	
}