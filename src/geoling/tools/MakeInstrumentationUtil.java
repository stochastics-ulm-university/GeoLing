package geoling.tools;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.javalite.instrumentation.Instrumentation;

/**
 * Instrumentation step for the model class files.
 * <p>
 * ActiveJDBC requires instrumentation of class files after they are compiled.
 * This is accomplished with an instrumentation package ("activejdbc-instrumentation-1.4.9.jar")
 * provided by the project, which can be found in the "jars" folder. Note that there
 * is a problem with paths containing e.g. spaces. Use "activejdbc-instrumentation-1.4.9_fix*.jar",
 * it contains a fix.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 * @see <a href="http://code.google.com/p/activejdbc/wiki/Instrumentation">Informations about instrumentation</a>
 */
public class MakeInstrumentationUtil {

	/**
	 * Detects the directory of the binary Java files (which is the working directory
	 * if started from Eclipse) and performs the instrumentation step.
	 * 
	 * @param args  command line parameters (not required)
	 */
	public static void main(String[] args) {
		try {
			Instrumentation instrumentation = new Instrumentation();
			String outputDir = ClassLoader.getSystemResource("").getPath();
			try {
				// replace e.g. %20 by space, which is required for the instrumentation to work
				outputDir = URLDecoder.decode(outputDir, "UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
			instrumentation.setOutputDirectory(outputDir);
			instrumentation.instrument();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}