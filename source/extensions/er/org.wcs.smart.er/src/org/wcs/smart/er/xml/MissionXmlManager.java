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
package org.wcs.smart.er.xml;

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
import org.wcs.smart.er.xml.model.missions.v11.MissionType;
import org.wcs.smart.er.xml.model.missions.v11.ObjectFactory;


/**
 * Manager for reading and writing mission xml files.
 * @author Jeff
 * @since 4.0.0
 */
public class MissionXmlManager {
	
	private static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.er.xml.model.missions.v11"; //$NON-NLS-1$
	
	public static final String ATTACHMENT_DIR_NAME = "attachments"; //$NON-NLS-1$
	
	/**
	 * Writes a xml patrol object to a file.
	 * <p>
	 * User is required to close output stream.
	 * </p>
	 * @param mission xml patrol to write
	 * @param file output stream 
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static void writeDataModel(MissionType mission, OutputStream file) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
		ObjectFactory objFactor = new ObjectFactory();
		
		JAXBElement<MissionType> element = objFactor.createMission(mission);
		marshaller.marshal(element, file);
	}
	
	/**
	 * Finds the xml converter to use based on the version in the xml file.
	 * @param xmlFile
	 * @return
	 */
	public static IXmlToMissionConverter findXmlConverter(Path xmlFile){
		try{
			DocumentBuilderFactory factory =
					DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(xmlFile.toAbsolutePath().toFile());
			String nodeName = doc.getFirstChild().getNodeName();
			String ns = ""; //$NON-NLS-1$
			if (nodeName.indexOf(':') > 0){
				ns = ":" + nodeName.substring(0, nodeName.indexOf(':')); //$NON-NLS-1$
			}
			String version = doc.getFirstChild().getAttributes().getNamedItem("xmlns" + ns).getTextContent(); //$NON-NLS-1$
			if (version.equals(org.wcs.smart.er.xml.model.missions.v12.ObjectFactory._Mission_QNAME.getNamespaceURI())){
				return new org.wcs.smart.er.xml.model.missions.v12.XMLtoMissionConverter();
			}else if (version.equals(org.wcs.smart.er.xml.model.missions.v11.ObjectFactory._Mission_QNAME.getNamespaceURI())){
				return new org.wcs.smart.er.xml.model.missions.v11.XMLtoMissionConverter();
			}else if (version.equals(org.wcs.smart.er.xml.model.missions.v10.ObjectFactory._Mission_QNAME.getNamespaceURI())){
				return new org.wcs.smart.er.xml.model.missions.v10.XMLtoMissionConverter();
				
			}
		}catch (Exception ex){
			//invalid xml file
			return null;
		}
		return null;
	}
}
