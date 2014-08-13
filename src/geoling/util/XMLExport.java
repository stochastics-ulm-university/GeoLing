package geoling.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Helper class to export information to XML files.
 * Currently, this class only provides easy access to a <code>XMLStreamWriter</code> object
 * and automatically starts the document tags.
 * 
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class XMLExport {
	
	/** UTF-8 encoding identifier string. */
	public static final String ENCODING = "UTF-8";
	
	/** The XML file stream writer. */
	public XMLStreamWriter XML;
	
	/**
	 * Constructs the object for XML export, starts the document tag.
	 * 
	 * @param fileName  the file name for the new XML file
	 * @throws FileNotFoundException if the file exists but is a directory rather than a regular
	 *                               file, does not exist but cannot be created, or cannot be
	 *                               opened for any other reason
	 * @throws XMLStreamException if there is an XML problem
	 */
	public XMLExport(String fileName) throws FileNotFoundException, XMLStreamException {
		try {
			XML = XMLOutputFactory.newInstance().createXMLStreamWriter(new BufferedOutputStream(new FileOutputStream(fileName)), ENCODING);
			XML.writeStartDocument(ENCODING, "1.0");
			this.newLine();
		} catch (FactoryConfigurationError e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Writes a new-line character to the XML stream.
	 * 
	 * @throws XMLStreamException if there is an XML problem
	 */
	public void newLine() throws XMLStreamException {
		XML.writeCharacters("\n");
	}
	
	/**
	 * Ends the document tag and closes the XML file.
	 * 
	 * @throws XMLStreamException if there is an XML problem
	 */
	public void close() throws XMLStreamException {
		XML.writeEndDocument();
		XML.close();
	}
	
}
