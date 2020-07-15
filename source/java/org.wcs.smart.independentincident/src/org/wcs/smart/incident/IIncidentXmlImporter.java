/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.incident;

import java.io.InputStream;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Interface for incident xml imports for plugins that
 * contribute additional incidents
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public interface IIncidentXmlImporter {

	/**
	 * Returns true if this xml importer can import the given file
	 * @param file
	 * @return
	 */
	public boolean canImport(Path file);
	
	/**
	 * Imports the incidents from the file.  File can be a zip package
	 * or indiviual xml file
	 * 
	 * @param file
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	public Waypoint importIncident(Path file, IProgressMonitor monitor) throws Exception;

	
	/**
	 * Assumes the stream represents an xml file and attempts to
	 * read the namespace associated with the root object.  Returns
	 * null if any exception occurs or cannot determine namespace.
	 * 
	 * @param stream
	 * @return
	 */
	public static String findNamespace(InputStream stream){
		try{
			DocumentBuilderFactory factory =
					DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(stream);
			String nodeName = doc.getFirstChild().getNodeName();
			String ns = ""; //$NON-NLS-1$
			if (nodeName.indexOf(':') > 0){
				ns = ":" + nodeName.substring(0, nodeName.indexOf(':')); //$NON-NLS-1$
			}
			String version = doc.getFirstChild().getAttributes().getNamedItem("xmlns" + ns).getTextContent(); //$NON-NLS-1$
			return version;
			
		}catch (Exception ex){
			//invalid xml file
			return null;
		}
	}
}
