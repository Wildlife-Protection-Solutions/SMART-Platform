package org.wcs.smart.incident;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.util.UuidUtils;

/**
 * For upgrading the incident report template
 * 
 */
public class IncidentReport800Upgrader {
	private static final String EXTENSION_ID_ATT_NAME = "extensionID"; //$NON-NLS-1$
	private static final String ODA_DATA_SET_TAG_NAME = "oda-data-set"; //$NON-NLS-1$
	private static final String EXTENSION_NAME_TAG_NAME = "extensionName"; //$NON-NLS-1$
	private static final String EXTENDED_ITEM_TAG_NAME = "extended-item"; //$NON-NLS-1$
	private static final String PROPERTY_TAG_NAME = "property"; //$NON-NLS-1$
	private static final String NAME_ATT_NAME = "name"; //$NON-NLS-1$
	private static final String LIST_PROPERTY_TAG_NAME = "list-property"; //$NON-NLS-1$


	public List<String> upgrade(Session session) throws SQLException {
		return upgradeReportFiles(session);		
	}
	
	private List<String> upgradeReportFiles(Session session) throws SQLException {
		List<String> warnings = new ArrayList<>();

		//NOTE: we are not allowed to use hibernate objects attached to session (see 1924 )
		//This is why we fetch everything manually and fill object with fields that will be used later
		final List<ConservationArea> cas = new ArrayList<>();
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				try(ResultSet ca_rs = c.createStatement().executeQuery("select uuid, name from smart.conservation_area")){ //$NON-NLS-1$
					while (ca_rs.next()) {
						ConservationArea ca = new ConservationArea();
						byte[] uuid = ca_rs.getBytes(1);
						ca.setUuid(UuidUtils.byteToUUID(uuid));
						ca.setName(ca_rs.getString(2));
						cas.add(ca);
						
						
						Path incidentTemplate = 
								Paths.get(SmartContext.INSTANCE.getFilestoreLocation())
								.resolve(UuidUtils.getDirectoryPath(ca.getUuid()))
								.resolve(IndepedentIncidentSource.FILESTORE_LOC)
								.resolve("incidentTemplate.rptdesign"); //$NON-NLS-1$
						
						
						if (Files.exists(incidentTemplate)) {
							//upgrade it 
							try {
								upgradeIncidentTemplate(incidentTemplate);
							} catch (Exception e) {
								Logger.getLogger(IncidentReport800Upgrader.class.getName()).log(Level.WARNING, e.getMessage(), e);
								warnings.add(MessageFormat.format("Unable to upgrade incident template for Conservation Area {0}. You will need review and update this template after the upgrade", ca.getName())); //$NON-NLS-1$
							}
						}
					}
				}
			}
		});

		
		return warnings;

	}
	


	/**
	 * Updates a report xml file from the pre 4.0.0 version to the next 4.0.0
	 * version. This updates the SMART map structure and removes the observer
	 * field from patrol queries.
	 * 
	 * @param file
	 * @throws Exception
	 */
	private void upgradeIncidentTemplate(Path file) throws Exception {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(file.toAbsolutePath().toFile());

		//map elements
		updateDataSets(doc);
		updateMap(doc);

		final DOMImplementationLS dom = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS"); //$NON-NLS-1$
		final LSSerializer serializer = dom.createLSSerializer();
		serializer.setNewLine("\n"); //$NON-NLS-1$
	
		final LSOutput destination = dom.createLSOutput();
		destination.setEncoding(StandardCharsets.UTF_8.name());
		try(OutputStream fos = Files.newOutputStream(file)){
			destination.setByteStream(fos);
			serializer.write(doc, destination);
		}
	}
	
	
	private static void updateDataSets(Document doc) throws Exception{

		NodeList dataSetItems = doc.getElementsByTagName(ODA_DATA_SET_TAG_NAME);
		for (int i = 0; i < dataSetItems.getLength(); i++) {
			
			Node dataSetItem = dataSetItems.item(i);
			String extid = dataSetItem.getAttributes().getNamedItem(EXTENSION_ID_ATT_NAME).getTextContent();

			if (extid.equalsIgnoreCase("org.wcs.smart.incident.birt.incident.details")) { //$NON-NLS-1$
				//update the native data type for the geometry columns
				for (int j = 0; j < dataSetItem.getChildNodes().getLength(); j++) {
					Node property = dataSetItem.getChildNodes().item(j);
					if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME) &&
							property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("resultSet")) { //$NON-NLS-1$
						
						//find wp:rawgeometry and wp:getomry and set nativeDataType to 8200
						NodeList items = property.getChildNodes();
						for (int k = 0; k < items.getLength(); k++) {
							
							Node structure = items.item(k);
							Node nativeDataType = null;
							boolean update = false;
							for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
								Node kid = structure.getChildNodes().item(p);
								if (kid.getAttributes() == null) continue;
								
								
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("nativeName")) { //$NON-NLS-1$
									if (kid.getFirstChild().getNodeValue().equals("wp:rawgeometry") || //$NON-NLS-1$
											kid.getFirstChild().getNodeValue().equals("wp:geometry")) { //$NON-NLS-1$
										update = true;
									}
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("nativeDataType")) { //$NON-NLS-1$
									nativeDataType = kid.getFirstChild();
								}
							}
							if (update && nativeDataType != null) {
								nativeDataType.setNodeValue("8200"); //$NON-NLS-1$
							}
							
						}
						
					}
				}
			}//end updating details dataset

			if (extid.equalsIgnoreCase("org.wcs.smart.incident.birt.incident.observations.attributes")) { //$NON-NLS-1$
				//need to add two column
				for (int j = 0; j < dataSetItem.getChildNodes().getLength(); j++) {
					Node property = dataSetItem.getChildNodes().item(j);
					if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME) &&
							property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("columnHints")) { //$NON-NLS-1$
						
						Node clone = property.getChildNodes().item(property.getChildNodes().getLength()  - 2);
						clone = clone.cloneNode(true);
						
						for (int p = 0; p < clone.getChildNodes().getLength(); p ++) {
							Node kid = clone.getChildNodes().item(p);
							if (kid.getNodeName() == null) continue;
							if (kid.getAttributes() != null) {
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("columnName")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("attribute:linegeometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("alias")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("Line Geometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("displayName")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("Line Geometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("heading")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("Line Geometry"); //$NON-NLS-1$
								}
							}
						}
						Node clone2 = property.getChildNodes().item(property.getChildNodes().getLength()  - 1);
						property.appendChild(clone);
						property.appendChild(clone2.cloneNode(true));
						
						clone = property.getChildNodes().item(property.getChildNodes().getLength()  - 2);
						clone = clone.cloneNode(true);
						
						for (int p = 0; p < clone.getChildNodes().getLength(); p ++) {
							Node kid = clone.getChildNodes().item(p);
							if (kid.getNodeName() == null) continue;
							if (kid.getAttributes() != null) {
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("columnName")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("attribute:polygongeometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("alias")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("Polygon Geometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("displayName")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("Polygon Geometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("heading")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("Polygon Geometry"); //$NON-NLS-1$
								}
							}
						}
						clone2 = property.getChildNodes().item(property.getChildNodes().getLength()  - 1);
						property.appendChild(clone);
						property.appendChild(clone2.cloneNode(true));
						
					}
					
					if (property.getNodeName().equalsIgnoreCase("structure") && //$NON-NLS-1$
							property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("cachedMetaData")) { //$NON-NLS-1$
						
						
						Node lproperty = null;
						for (int l = 0; l < property.getChildNodes().getLength(); l ++) {
							Node kid = property.getChildNodes().item(l);
							if (kid.getAttributes() != null && kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("resultSet")) { //$NON-NLS-1$
								lproperty = kid;
								break;
							}
						}
						
						
						Node clone = lproperty.getChildNodes().item(lproperty.getChildNodes().getLength()  - 2);
						clone = clone.cloneNode(true);
						
						for (int p = 0; p < clone.getChildNodes().getLength(); p ++) {
							Node kid = clone.getChildNodes().item(p);
							if (kid.getNodeName() == null) continue;
							if (kid.getAttributes() != null) {
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("position")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("8"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("name")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("Line Geometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("dataType")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("javaObject"); //$NON-NLS-1$
								}
								
							}
						}
						Node clone2 = lproperty.getChildNodes().item(lproperty.getChildNodes().getLength()  - 1);
						lproperty.appendChild(clone);
						lproperty.appendChild(clone2.cloneNode(true));
						
						clone = lproperty.getChildNodes().item(lproperty.getChildNodes().getLength()  - 2);
						clone = clone.cloneNode(true);
						
						for (int p = 0; p < clone.getChildNodes().getLength(); p ++) {
							Node kid = clone.getChildNodes().item(p);
							if (kid.getNodeName() == null) continue;
							if (kid.getAttributes() != null) {
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("position")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("9"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("name")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("Polygon Geometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("dataType")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("javaObject"); //$NON-NLS-1$
								}
							}
						}
						clone2 = lproperty.getChildNodes().item(lproperty.getChildNodes().getLength()  - 1);
						lproperty.appendChild(clone);
						lproperty.appendChild(clone2.cloneNode(true));
						
					}
					
					if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME) &&
							property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("resultSet")) { //$NON-NLS-1$
						
						Node clone = property.getChildNodes().item(property.getChildNodes().getLength()  - 2);
						clone = clone.cloneNode(true);
						
						for (int p = 0; p < clone.getChildNodes().getLength(); p ++) {
							Node kid = clone.getChildNodes().item(p);
							if (kid.getNodeName() == null) continue;
							if (kid.getAttributes() != null) {
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("position")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("8"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("name")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("attribute:linegeometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("nativeName")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("attribute:linegeometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("dataType")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("javaObject"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("nativeDataType")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("8203"); //$NON-NLS-1$
								}
							}
						}
						Node clone2 = property.getChildNodes().item(property.getChildNodes().getLength()  - 1);
						property.appendChild(clone);
						property.appendChild(clone2.cloneNode(true));
						
						clone = property.getChildNodes().item(property.getChildNodes().getLength()  - 2);
						clone = clone.cloneNode(true);
						
						for (int p = 0; p < clone.getChildNodes().getLength(); p ++) {
							Node kid = clone.getChildNodes().item(p);
							if (kid.getNodeName() == null) continue;
							if (kid.getAttributes() != null) {
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("position")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("9"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("name")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("attribute:polygongeometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("nativeName")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("attribute:polygongeometry"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("dataType")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("javaObject"); //$NON-NLS-1$
								}
								if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("nativeDataType")) { //$NON-NLS-1$
									kid.getFirstChild().setNodeValue("8205"); //$NON-NLS-1$
								}
							}
						}
						clone2 = property.getChildNodes().item(property.getChildNodes().getLength()  - 1);
						property.appendChild(clone);
						property.appendChild(clone2.cloneNode(true));
						
					}
				}
				
			}// end updates attributes dataset
		}

	}

	private static void updateMap(Document doc) throws Exception{
		NodeList mapItems = doc.getElementsByTagName(EXTENDED_ITEM_TAG_NAME);
		
		for (int i = 0; i < mapItems.getLength(); i++) {
			Node mapItem = mapItems.item(i);

			if (mapItem.getAttributes().getNamedItem(EXTENSION_NAME_TAG_NAME)
					.getTextContent().equalsIgnoreCase("org.wcs.smart.report.birt.SmartMap")) { //$NON-NLS-1$  

				for (int k = 0; k < mapItem.getChildNodes().getLength(); k++) {
					Node kid = mapItem.getChildNodes().item(k);
					
					if (kid.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME)) {
						
						if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("org.wcs.smart.birt.map.layers2")) { //$NON-NLS-1$

							Node toclone = null;
							
							for (int l = 0; l < kid.getChildNodes().getLength(); l ++) {
								Node extendeditem = kid.getChildNodes().item(l);
								
								Node geomNode = null;
								Node typeNode = null;
								for (int m  = 0 ; m < extendeditem.getChildNodes().getLength(); m ++) {
									Node prop = extendeditem.getChildNodes().item(m);
									if (prop.getAttributes() == null) continue;
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("org.wcs.smart.birt.map.geomColumn")) { //$NON-NLS-1$
										geomNode = prop;
									}
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("org.wcs.smart.birt.map.layerName")) { //$NON-NLS-1$
										typeNode = prop;
									}
								}
								if (geomNode != null && typeNode != null) {
									toclone = extendeditem;
									if (geomNode.getFirstChild().getNodeValue().equals("wp:rawgeometry")) { //not a typo //$NON-NLS-1$
										typeNode.getFirstChild().setNodeValue("details - Raw Geometry"); //$NON-NLS-1$
									}
									if (geomNode.getFirstChild().getNodeValue().equals("wp:geometry")) { //not a typo //$NON-NLS-1$
										typeNode.getFirstChild().setNodeValue("details - Geometry"); //$NON-NLS-1$
									}
								}
								
							}
							
							if (toclone != null) {
								Node styleNode = null;

								Node clone = toclone.cloneNode(true);
								for (int m  = 0 ; m < clone.getChildNodes().getLength(); m ++) {
									Node prop = clone.getChildNodes().item(m);
									if (prop.getAttributes() == null) continue;
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("org.wcs.smart.birt.map.layerName")) { //$NON-NLS-1$
										prop.getFirstChild().setNodeValue("attributes - Polygon Geometry"); //$NON-NLS-1$
									}
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("org.wcs.smart.birt.map.layerType")) { //$NON-NLS-1$
										prop.getFirstChild().setNodeValue("MULTIPOLYGON"); //$NON-NLS-1$
									}
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("org.wcs.smart.birt.map.geomColumn")) { //$NON-NLS-1$
										prop.getFirstChild().setNodeValue("attribute:polygongeometry"); //$NON-NLS-1$
									}
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("dataSet")) { //$NON-NLS-1$
										prop.getFirstChild().setNodeValue("attributes"); //$NON-NLS-1$
									}
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("org.wcs.smart.birt.map.layerStyle")) { //$NON-NLS-1$
										styleNode = prop; 
									}
								}
								if (styleNode != null) {
									clone.removeChild(styleNode);
								}
								
								kid.insertBefore(clone, kid.getFirstChild());
								
								clone = toclone.cloneNode(true);
								styleNode = null;
								for (int m  = 0 ; m < clone.getChildNodes().getLength(); m ++) {
									Node prop = clone.getChildNodes().item(m);
									if (prop.getAttributes() == null) continue;
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("org.wcs.smart.birt.map.layerName")) { //$NON-NLS-1$
										prop.getFirstChild().setNodeValue("attributes - Line Geometry"); //$NON-NLS-1$
									}
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("org.wcs.smart.birt.map.layerType")) { //$NON-NLS-1$
										prop.getFirstChild().setNodeValue("MULTILINE"); //$NON-NLS-1$
									}
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("org.wcs.smart.birt.map.geomColumn")) { //$NON-NLS-1$
										prop.getFirstChild().setNodeValue("attribute:linegeometry"); //$NON-NLS-1$
									}
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("dataSet")) { //$NON-NLS-1$
										prop.getFirstChild().setNodeValue("attributes"); //$NON-NLS-1$
									}
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("org.wcs.smart.birt.map.layerStyle")) { //$NON-NLS-1$
										styleNode = prop; 
									}
								}
								if (styleNode != null) {
									clone.removeChild(styleNode);
								}
								kid.insertBefore(clone, kid.getFirstChild());
							}
						}
					}
				}
			}
		}
	}
	
}
