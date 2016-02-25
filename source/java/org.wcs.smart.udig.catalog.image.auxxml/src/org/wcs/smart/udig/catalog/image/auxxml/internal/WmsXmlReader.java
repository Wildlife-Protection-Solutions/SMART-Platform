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
package org.wcs.smart.udig.catalog.image.auxxml.internal;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.geotools.data.PrjFileReader;
import org.geotools.referencing.CRS;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.URLUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Read the projection from aux.xml file.
 * @author Emily
 *
 */
public class WmsXmlReader {

	private File fileUrl;
	
	public WmsXmlReader(URL url) throws URISyntaxException{
		this(new File(url.toURI()));
	}
	
	public WmsXmlReader(File file){
		this.fileUrl = file;
	}
	
	/**
	 * Reads the projection from the aux.xml file returning
	 * null if error occurs or not found.
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws FactoryException
	 */
	public CoordinateReferenceSystem getCoordinateReferenceSystem() {
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder(); 
			Document doc = db.parse(fileUrl);
	
			NodeList element = doc.getElementsByTagName("SRS"); //$NON-NLS-1$
			if (element != null && element.getLength() > 0){
				String crs = element.item(0).getTextContent();
				return CRS.parseWKT(crs);
			}
			return null;
		}catch (Exception ex){
			CatalogPlugin.log(ex.getMessage(), ex);
		}
		return null;
	}
	
	public static File findFile(File root){
		File tmp = new File(root.toString() + ".aux.xml");
		if (tmp.exists()) return tmp;
		
		File[] files = URLUtils.findRelatedFiles(tmp, ".aux.xml");
		for (File f : files){
			if (f.exists()) return f;
		}
		return null;
	}
	
	public static String findPrjFile(File basefile){
        File[] f = URLUtils.findRelatedFiles(basefile, ".prj");
        if (f.length > 0){
        	File prj = f[0];
        	if (prj.exists()){
        		//see if it is readable
        		try{
        			PrjFileReader prjReader = new PrjFileReader(Files.newByteChannel(prj.toPath()));
        			prjReader.getCoordinateReferenceSystem();
        			return "Prj file exists and should be used.";
        		}catch (Exception ex){
        			///could not read file; lets assume no valid prj files
        		}
        	}
        }
        return null;
	}
}
