/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.internal.ca.datamodel.xml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.util.ZipUtilCommon;

/**
 * Data model XML importer
 * 
 */
public class XmlDataModelImporter {
	
	public static enum I18NMessages{
		NO_DM_XML_FOUND,
		INVALID_XML
	};
		
	private Locale l;
	private Collection<Icon> icons;
	private Collection<IconSet> iconSets;
	
	private SimpleDataModel importedModel;
	private Path workingLocation;
	
	/**
	 * 
	 * @param icons list of existing icons that can be used
	 * @param iconSets list of existing icon sets 
	 * @param l
	 * @param workingLocation
	 */
	public XmlDataModelImporter(Collection<Icon> icons, Collection<IconSet> iconSets, 
			Locale l, Path workingLocation) {
		this.l = l;
		this.icons = icons;
		this.iconSets = iconSets;
		this.workingLocation = workingLocation;
	}

	/**
	 * 
	 * @return the imported data model
	 */
	public SimpleDataModel getImportedDataModel() {
		return this.importedModel;
	}
	
	/**
	 * Processes and input stream. Assumes the input stream represents an xml file 
	 * in the newest format
	 * 
	 * @param is
	 * @throws Exception
	 */
	
	public void processInputStream(InputStream is) throws Exception {
		processXmlFile(is);
	}
	
	/**
	 * Processes an zip or xml file 
	 * 
	 * @param path
	 * @throws Exception
	 */
	public void processFile(Path path) throws Exception {
		
		if (path.getFileName().toString().toLowerCase(Locale.getDefault()).endsWith(".zip")) { //$NON-NLS-1$
			//try zip file
			processZip(path);
		}else {
			//try plain xml
			processXmlFile(path);
		}
	}
	
	
	private void processZip(Path zipFiles) throws Exception {
		ZipUtilCommon.unzipFolder(zipFiles, workingLocation);
		
		List<Path> xmlFiles = new ArrayList<>();
		try(Stream<Path> file = Files.list(workingLocation)){
			file.forEach(f->{
				if (f.getFileName().toString().toLowerCase(Locale.getDefault()).endsWith("xml")) { //$NON-NLS-1$
					xmlFiles.add(f);
				}
			});
		}
			
			
		if (xmlFiles.isEmpty()) throw new Exception(getLabel(I18NMessages.NO_DM_XML_FOUND));
			
		IXmlToDataModelConverter converter = null;
		Path dataModelFile = null;
		for (Path xmlFile : xmlFiles) {
			converter = findVersion(xmlFile);
			if (converter != null) {
				dataModelFile = xmlFile;
				break;
			}
		}
		if (converter == null) throw new Exception(getLabel(I18NMessages.NO_DM_XML_FOUND));
		
		try(InputStream is = Files.newInputStream(dataModelFile)){
			this.importedModel = converter.convert(is, icons, iconSets, workingLocation, l);
		}
	}

	private void processXmlFile(Path xmlFile) throws Exception{
		
		IXmlToDataModelConverter converter = findVersion(xmlFile);
		if (converter == null) throw new Exception(getLabel(I18NMessages.INVALID_XML));
		
		try(InputStream is = Files.newInputStream(xmlFile)){
			this.importedModel = converter.convert(is, icons, iconSets, null, l);
		}
		 
	}
	
	private void processXmlFile(InputStream is) throws Exception{
		IXmlToDataModelConverter converter = new org.wcs.smart.internal.ca.datamodel.xml.generate.v11.DataModelXmlToSimpleDataModelConverter();
		this.importedModel = converter.convert(is, icons, iconSets, null, l);
	}

	
	
	private IXmlToDataModelConverter findVersion(Path xmlFile){
		try{
			DocumentBuilderFactory factory =
					DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(xmlFile.toAbsolutePath().toFile());
			
			NodeList first = doc.getChildNodes();
			Node dmNode = null;
			for (int i = 0; i < first.getLength(); i ++) {
				String nn = first.item(i).getNodeName();
				if (nn.equalsIgnoreCase("DataModel")) { //$NON-NLS-1$
					dmNode = first.item(i);
					break;
				}
			}
			//not a valid xml file
			if (dmNode == null) throw new Exception(getLabel(I18NMessages.INVALID_XML));
			
			String nodeName = dmNode.getNodeName();
			String ns = ""; //$NON-NLS-1$
			if (nodeName.indexOf(':') > 0){
				ns = ":" + nodeName.substring(0, nodeName.indexOf(':')); //$NON-NLS-1$
			}
			String version = dmNode.getAttributes().getNamedItem("xmlns" + ns).getTextContent(); //$NON-NLS-1$
			if (version.equals(org.wcs.smart.internal.ca.datamodel.xml.generate.v10.ObjectFactory._DataModel_QNAME.getNamespaceURI())){
				return new org.wcs.smart.internal.ca.datamodel.xml.generate.v10.DataModelXmlToSimpleDataModelConverter();
			}else if (version.equals(org.wcs.smart.internal.ca.datamodel.xml.generate.v11.ObjectFactory._DataModel_QNAME.getNamespaceURI())){
				return new org.wcs.smart.internal.ca.datamodel.xml.generate.v11.DataModelXmlToSimpleDataModelConverter();
			}else if (version.equals(org.wcs.smart.internal.ca.datamodel.xml.generate.v12.ObjectFactory._DataModel_QNAME.getNamespaceURI())){
				return new org.wcs.smart.internal.ca.datamodel.xml.generate.v12.DataModelXmlToSimpleDataModelConverter();
			}
		}catch (Exception ex){
			//invalid xml file
			return null;
		}
		return null;
	}
	
	private String getLabel(I18NMessages message) {
		return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(message, l);
	}
}
