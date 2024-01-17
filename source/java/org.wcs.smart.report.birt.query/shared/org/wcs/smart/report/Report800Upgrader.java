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
package org.wcs.smart.report;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.wcs.smart.report.model.Report;
import org.wcs.smart.util.UuidUtils;

/**
 * Script to upgrade report files from the "old" map report structure to the new
 * map report structure implemented in 4.0
 * 
 * @author Emily
 *
 */
public class Report800Upgrader {

	private static final String EXTENSION_ID_ATT_NAME = "extensionID"; //$NON-NLS-1$
	private static final String ODA_DATA_SET_TAG_NAME = "oda-data-set"; //$NON-NLS-1$
	private static final String EXTENSION_NAME_TAG_NAME = "extensionName"; //$NON-NLS-1$
	private static final String EXTENDED_ITEM_TAG_NAME = "extended-item"; //$NON-NLS-1$
	private static final String PROPERTY_TAG_NAME = "property"; //$NON-NLS-1$
	private static final String NAME_ATT_NAME = "name"; //$NON-NLS-1$
	private static final String LIST_PROPERTY_TAG_NAME = "list-property"; //$NON-NLS-1$

	private enum Type {TABLE, QUERY, PLAN, INTEL};

	public List<String> upgrade(Session session) throws Exception {
		return upgradeReportFiles(session);		
	}
	
	private List<String> upgradeReportFiles(Session session) throws Exception {
		List<String> warnings = new ArrayList<>();

		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				try(ResultSet r_rs = c.createStatement().executeQuery("select uuid, ca_uuid, filename, id from smart.report")){ //$NON-NLS-1$
					while (r_rs.next()) {
						byte[] uuid = r_rs.getBytes(1);
						byte[] ca_uuid = r_rs.getBytes(2);
						String reportFilename = r_rs.getString(3);
						String reportId = r_rs.getString(4);
						String caFileDataStoreLocation = SmartContext.INSTANCE.getFilestoreLocation() + File.separator + UuidUtils.getDirectoryPath(UuidUtils.byteToUUID(ca_uuid));
						try {
							xmlUpdater(Paths.get(caFileDataStoreLocation).resolve(Report.REPORT_DIR).resolve(reportFilename));
						} catch (Exception ex) {
							String reportName = reportId;
							PreparedStatement ps = c.prepareStatement("select lbl.VALUE from smart.REPORT rpt left join smart.I18N_LABEL lbl on lbl.ELEMENT_UUID=rpt.uuid left join smart.LANGUAGE lng on lbl.LANGUAGE_UUID=lng.UUID where rpt.UUID=? and lng.ISDEFAULT"); //$NON-NLS-1$
							ps.setBytes(1, uuid);
							try (ResultSet name_rs = ps.executeQuery()) {
								if (name_rs.next()) {
									reportName = name_rs.getString(1);
								}
							} catch (Exception e) {
								Logger.getLogger(Report800Upgrader.class.getName()).log(Level.WARNING, "Failed to find a name for report with uuid=" + UuidUtils.uuidToString(UuidUtils.byteToUUID(uuid)), e); //$NON-NLS-1$
							}
							warnings.add(MessageFormat.format("Unable to upgrade Report {0} - {1}.", reportName, ex.getMessage())); //$NON-NLS-1$
							Logger.getLogger(Report800Upgrader.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
						}
					}
				}
			}
		});
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
					}
				}
			}
		});

		for (ConservationArea ca : cas){
			try{
				upgradePlan(ca);
			} catch (Exception ex) {
				warnings.add(MessageFormat.format("Could not upgrade customized Plan pdf template for Conservation Area: {0}. If you used plan templates you may need to reset this template or resolve errors manually.", ca.getName())); //$NON-NLS-1$
				Logger.getLogger(Report800Upgrader.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
			}
		}
		return warnings;

	}
	
	private void upgradePlan(ConservationArea ca) throws Exception{
		Path planFile = Paths.get(ca.getFileDataStoreLocation()).resolve("plans") //$NON-NLS-1$
				.resolve("planTemplate.rptdesign"); //$NON-NLS-1$
		if (Files.exists(planFile)){
			xmlUpdater(planFile);
		}
	}

	/**
	 * Updates a report xml file from the pre 4.0.0 version to the next 4.0.0
	 * version. This updates the SMART map structure and removes the observer
	 * field from patrol queries.
	 * 
	 * @param file
	 * @throws Exception
	 */
	public static void xmlUpdater(Path file) throws Exception {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(file.toAbsolutePath().toFile());

		//map elements
		HashMap<String, String> datasetname2extension = new HashMap<>();		

		updateDataSets(doc, datasetname2extension);
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
	
	
	private static void updateDataSets(Document doc,  HashMap<String,String> dataSetTypeMapping) throws Exception{

		NodeList dataSetItems = doc.getElementsByTagName(ODA_DATA_SET_TAG_NAME);
		for (int i = 0; i < dataSetItems.getLength(); i++) {
			Type dataSetType = null;
			Node dataSetItem = dataSetItems.item(i);
			String extid = dataSetItem.getAttributes().getNamedItem(EXTENSION_ID_ATT_NAME).getTextContent();

			if (extid.equalsIgnoreCase("org.wcs.smart.data.oda.smart.smartTableDataset")) { //$NON-NLS-1$
				dataSetType = Type.TABLE;
			} else if (extid.equalsIgnoreCase("org.wcs.smart.data.oda.smart.smartQueryDataset")) { //$NON-NLS-1$
				dataSetType = Type.QUERY;
			} else if (extid.equals("org.wcs.smart.plan.report.oda.SmartPlanTargets")) { //$NON-NLS-1$
				dataSetType = Type.PLAN;
			}

			if (dataSetType == null)
				continue;
						
			dataSetTypeMapping.put(dataSetItem.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent(), extid);
						
			String queryText = null;
			Node hints = null;
			
			for (int j = 0; j < dataSetItem.getChildNodes().getLength(); j++) {
				Node property = dataSetItem.getChildNodes().item(j);
				
				if (property.getNodeName().equalsIgnoreCase("xml-property")) { //$NON-NLS-1$
					if (property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent()
							.equalsIgnoreCase("queryText")) { //$NON-NLS-1$
						queryText = property.getTextContent();
					}
				}
				
				if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)) {
					String name = property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent();
					if (name.equals("columnHints")) { //$NON-NLS-1$
						hints = property;
					}
				}
				
			}
			
	
	
			for (int j = 0; j < dataSetItem.getChildNodes().getLength(); j++) {
				Node property = dataSetItem.getChildNodes().item(j);
			
				if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)) {
					String name = property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent();
					
					if (name.equalsIgnoreCase("resultSet")) { //$NON-NLS-1$
						for (int k = 0; k < property.getChildNodes().getLength(); k++) {
							Node struct = property.getChildNodes().item(k);

							int datatype = -1;
							Node datatypenode = null;
							Node fieldnamenode = null;
							for (int l = 0; l < struct.getChildNodes().getLength(); l++) {
								Node prop = struct.getChildNodes().item(l);
								if (prop.getNodeName().equals(PROPERTY_TAG_NAME)
										&& prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent()
												.equalsIgnoreCase("nativeDataType")) { //$NON-NLS-1$
									datatype = Integer.parseInt(prop.getTextContent());
									datatypenode = prop;
								}
								if (prop.getNodeName().equals(PROPERTY_TAG_NAME) && prop.getAttributes()
										.getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("name")) { //$NON-NLS-1$
									fieldnamenode = prop;
								}
							}
							
							if (datatype == 2000) {
								if (dataSetType == Type.QUERY) {
									String fieldname = fieldnamenode.getTextContent();
									if (fieldname.equals("wp:geometry")) { //$NON-NLS-1$
										datatypenode.getFirstChild().setNodeValue("8200"); // point //$NON-NLS-1$
									} else if (fieldname.equals("track:geometry")) { //$NON-NLS-1$
										datatypenode.getFirstChild().setNodeValue("8203"); // multilinestring //$NON-NLS-1$
									} else if (fieldname.equals("TrackGeomtry")) { //this is not a typo - the original code used this //$NON-NLS-1$
										datatypenode.getFirstChild().setNodeValue("8201"); // linestring //$NON-NLS-1$
										fieldnamenode.getFirstChild().setNodeValue("track:geometry"); //$NON-NLS-1$
										
										//go through hints and change TrackGeomtry to track:geometry
										if (hints != null) {
											
											for (int l = 0; l < hints.getChildNodes().getLength(); l++) {
												Node structure = hints.getChildNodes().item(k);

												for (int m = 0; m < structure.getChildNodes().getLength(); m++) {
													Node prop = structure.getChildNodes().item(m);
													if (prop.getNodeName().equals(PROPERTY_TAG_NAME)
															&& prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("columnName")) { //$NON-NLS-1$
														
														if (prop.getFirstChild().getNodeValue().equals("TrackGeomtry")) { //$NON-NLS-1$
															prop.getFirstChild().setNodeValue("track:geometry"); //$NON-NLS-1$
														}
													}													
												}
											}
										}
										
									}else if (fieldname.equals("Geometry")) { //$NON-NLS-1$
										//plan patrol query geometry
										datatypenode.getFirstChild().setNodeValue("8203"); // multi-linestring //$NON-NLS-1$
										fieldnamenode.getFirstChild().setNodeValue("track:geometry"); //$NON-NLS-1$
									}
								}else if (dataSetType == Type.TABLE) {
									if (queryText.startsWith("smartareas:")) { //$NON-NLS-1$
										datatypenode.getFirstChild().setNodeValue("8205"); // multipolygon //$NON-NLS-1$
									}else if (queryText.startsWith("asset")) { //$NON-NLS-1$
										datatypenode.getFirstChild().setNodeValue("8200"); // point //$NON-NLS-1$
									}else if (queryText.startsWith("SD_SU:TRANSECT")) { //$NON-NLS-1$
										datatypenode.getFirstChild().setNodeValue("8201"); // line //$NON-NLS-1$
									}else if (queryText.startsWith("SD_SU:PLOT")) { //$NON-NLS-1$
										datatypenode.getFirstChild().setNodeValue("8200"); // point //$NON-NLS-1$
									}
								}else if (dataSetType == Type.PLAN) {
									String fieldname = fieldnamenode.getTextContent();
									if (fieldname.equals("geometry")) { //$NON-NLS-1$
										datatypenode.getFirstChild().setNodeValue("8202"); // multi-point //$NON-NLS-1$
									}
								}
							}
						}
					}
				}
			}
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
									if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("org.wcs.smart.birt.map.layerType")) { //$NON-NLS-1$
										typeNode = prop;
									}
								}
								if (geomNode != null && typeNode != null) {
									if (geomNode.getFirstChild().getNodeValue().equals("TrackGeomtry")) { //not a typo //$NON-NLS-1$
										geomNode.getFirstChild().setNodeValue("track:geometry"); //$NON-NLS-1$
										typeNode.getFirstChild().setNodeValue("LINE"); //$NON-NLS-1$
									}
								}
								
							}
							
						}
					}
				}
			}
		}
	}
}
