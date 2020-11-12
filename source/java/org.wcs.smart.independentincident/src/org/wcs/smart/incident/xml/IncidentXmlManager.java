/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.incident.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.wcs.smart.incident.xml.model.v22.WaypointType;

/**
 * Manager for reading and writing patrol xml files.
 * @author Emily
 * @since 1.0.0
 */
public class IncidentXmlManager {
	private static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.incident.xml.model.v22"; //$NON-NLS-1$
	
	public static final String ATTACHMENT_DIR_NAME = "attachments"; //$NON-NLS-1$
	public static final String OBSERVATION_ATTACHMENT_DIR_NAME = "attachments/observations"; //$NON-NLS-1$
	
	
	/**
	 * Writes a xml patrol object to a file.
	 * <p>
	 * User is required to close output stream.
	 * </p>
	 * @param patrol xml patrol to write
	 * @param file output stream 
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static void writeIncident(WaypointType type, OutputStream file) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
		org.wcs.smart.incident.xml.model.v22.ObjectFactory objFactor = new org.wcs.smart.incident.xml.model.v22.ObjectFactory();
		
		JAXBElement<WaypointType> element = objFactor.createWaypoint(type);
		marshaller.marshal(element, file);
	}
	
	
	/**
	 * Finds the xml converter to use based on the version in the xml file.
	 * @param xmlFile
	 * @return
	 */
	public static IXmlToIncidentConverter findVersion(Path xmlFile){
		try{
			DocumentBuilderFactory factory =
					DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(xmlFile.toFile());
			String nodeName = doc.getFirstChild().getNodeName();
			String ns = ""; //$NON-NLS-1$
			if (nodeName.indexOf(':') > 0){
				ns = ":" + nodeName.substring(0, nodeName.indexOf(':')); //$NON-NLS-1$
			}
			String version = doc.getFirstChild().getAttributes().getNamedItem("xmlns" + ns).getTextContent(); //$NON-NLS-1$
			if (version.equals(org.wcs.smart.incident.xml.model.v20.ObjectFactory._Waypoint_QNAME.getNamespaceURI())){
				return new org.wcs.smart.incident.xml.model.v20.XmlToIncident();
			}else if (version.equals(org.wcs.smart.incident.xml.model.v21.ObjectFactory._Waypoint_QNAME.getNamespaceURI())){
				return new org.wcs.smart.incident.xml.model.v21.XmlToIncident();
			}else if (version.equals(org.wcs.smart.incident.xml.model.v22.ObjectFactory._Waypoint_QNAME.getNamespaceURI())){
				return new org.wcs.smart.incident.xml.model.v22.XmlToIncident();
			}
		}catch (Exception ex){
			//invalid xml file
			return null;
		}
		return null;
	}
}
