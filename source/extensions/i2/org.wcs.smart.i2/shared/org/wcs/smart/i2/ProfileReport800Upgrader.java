package org.wcs.smart.i2;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
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
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.util.UuidUtils;

/**
 * For upgrading the incident report template
 * 
 */
public class ProfileReport800Upgrader {
	
	private static final String EXTENSION_ID_ATT_NAME = "extensionID"; //$NON-NLS-1$
	private static final String ODA_DATA_SET_TAG_NAME = "oda-data-set"; //$NON-NLS-1$
	private static final String EXTENSION_NAME_TAG_NAME = "extensionName"; //$NON-NLS-1$
	private static final String EXTENDED_ITEM_TAG_NAME = "extended-item"; //$NON-NLS-1$
	private static final String PROPERTY_TAG_NAME = "property"; //$NON-NLS-1$
	private static final String NAME_ATT_NAME = "name"; //$NON-NLS-1$
	private static final String LIST_PROPERTY_TAG_NAME = "list-property"; //$NON-NLS-1$


	public List<String> upgrade(Session session) throws SQLException {
		List<String> warnings = new ArrayList<>();

		warnings.addAll( upgradeReportFiles(session));
		warnings.addAll( upgradeBirtTemplates(session));
		//TODO: record template
		return warnings;
	}
	
	private List<String> upgradeBirtTemplates(Session session) throws SQLException {
		List<String> warnings = new ArrayList<>();

		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				
				try(ResultSet r_rs = c.createStatement().executeQuery(
						"select birt_template, keyId, ca_uuid from smart.i_entity_type where birt_template is not null")){ //$NON-NLS-1$
					while (r_rs.next()) {
						String file = r_rs.getString(1);
						String entityTypeId = r_rs.getString(2);
						UUID ca_uuid = UuidUtils.byteToUUID(r_rs.getBytes(3));
						
						Path p = Paths.get(SmartContext.INSTANCE.getFilestoreLocation())
								.resolve(UuidUtils.getDirectoryPath(ca_uuid))
								.resolve("intelligence2")
								.resolve("entitytypes")
								.resolve(file);
						
						try {
							upgradeFile(p);
						}catch (Exception ex) {
							Logger.getLogger(ProfileReport800Upgrader.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
							warnings.add(MessageFormat.format("Unable to update report file {0}. Report may need updating before running", p.toString()));
						}
					}
				}
				
				try(ResultSet r_rs = c.createStatement().executeQuery(
						"select uuid from smart.conservation_area")){ //$NON-NLS-1$
					while (r_rs.next()) {
						UUID ca_uuid = UuidUtils.byteToUUID(r_rs.getBytes(1));
						
						Path p = Paths.get(SmartContext.INSTANCE.getFilestoreLocation())
								.resolve(UuidUtils.getDirectoryPath(ca_uuid))
								.resolve("intelligence2")
								.resolve("record.rptdesign");
						
						try {
							if (Files.exists(p)) {
								upgradeFile(p);
							}
						}catch (Exception ex) {
							Logger.getLogger(ProfileReport800Upgrader.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
							warnings.add(MessageFormat.format("Unable to update report file {0}. Report may need updating before running", p.toString()));
						}
					}
				}
			}
		});
		return warnings;
	}
	private List<String> upgradeReportFiles(Session session) throws SQLException {
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
							Path path = Paths.get(caFileDataStoreLocation).resolve("reports").resolve(reportFilename);
							upgradeFile(path);
						} catch (Exception ex) {
							String reportName = reportId;
							PreparedStatement ps = c.prepareStatement("select lbl.VALUE from smart.REPORT rpt left join smart.I18N_LABEL lbl on lbl.ELEMENT_UUID=rpt.uuid left join smart.LANGUAGE lng on lbl.LANGUAGE_UUID=lng.UUID where rpt.UUID=? and lng.ISDEFAULT"); //$NON-NLS-1$
							ps.setBytes(1, uuid);
							try (ResultSet name_rs = ps.executeQuery()) {
								if (name_rs.next()) {
									reportName = name_rs.getString(1);
								}
							} catch (Exception e) {
								Logger.getLogger(ProfileReport800Upgrader.class.getName()).log(Level.WARNING, "Failed to find a name for report with uuid=" + UuidUtils.uuidToString(UuidUtils.byteToUUID(uuid)), e); //$NON-NLS-1$
							}
							warnings.add(MessageFormat.format("Unable to upgrade Report {0} - {1}.", reportName, ex.getMessage())); //$NON-NLS-1$
							Logger.getLogger(ProfileReport800Upgrader.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
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
	public void upgradeFile(Path file) throws Exception {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(file.toAbsolutePath().toFile());

		//map elements
		updateDataSets(doc);
		
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

			if (extid.equalsIgnoreCase("org.wcs.smart.i2.birt.dataset.entity.attribute.location")){
				updateEntityLocationAttributeDataset(dataSetItem);
			}else if (extid.equalsIgnoreCase("org.wcs.smart.i2.birt.dataset.entity.location")){
				updateEntityLocationDataset(dataSetItem, doc);
			}else if (extid.equalsIgnoreCase("org.wcs.smart.i2.birt.dataset.query")) {
				updateQueryDataset(dataSetItem, doc);
			}else if (extid.equalsIgnoreCase("org.wcs.smart.i2.birt.dataset.record.location")) {
				updateRecordLocationDataset(dataSetItem, doc);
			}
		}
	}
	
	private static void updateEntityLocationAttributeDataset(Node dataSetItem) {
		
		// update the native data type for the geometry columns
		for (int j = 0; j < dataSetItem.getChildNodes().getLength(); j++) {
			Node property = dataSetItem.getChildNodes().item(j);
			if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)
					&& property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("resultSet")) { //$NON-NLS-1$

				// find wp:rawgeometry and wp:getomry and set nativeDataType to 8200
				NodeList items = property.getChildNodes();
				for (int k = 0; k < items.getLength(); k++) {

					Node structure = items.item(k);
					Node nativeDataType = null;
					boolean update = false;
					for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
						Node kid = structure.getChildNodes().item(p);
						if (kid.getAttributes() == null)
							continue;

						if (kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("nativeName")) { //$NON-NLS-1$
							if (kid.getFirstChild().getNodeValue().equals("attribute:geometry")) { //$NON-NLS-1$
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
	}
	
	private static boolean nameAttributeEquals(Node node, String text) {
		if (node.getAttributes() == null) return false;
		return node.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals(text);
	}
	private static boolean nodeValue(Node node, String text) {
		return node.getFirstChild().getNodeValue().equals(text);
	}
	
	
	private static void updateEntityLocationDataset(Node dataSetItem, Document doc) {
		
		//replace location:geom with location:polygon 8204 and location:point 8200
		// update the native data type for the geometry columns
		
		String datasetName = dataSetItem.getAttributes().getNamedItem("name").getNodeValue();
		
		for (int j = 0; j < dataSetItem.getChildNodes().getLength(); j++) {
			Node property = dataSetItem.getChildNodes().item(j);
		
			if (property.getNodeName().equalsIgnoreCase("text-property")
					&& nameAttributeEquals(property, "displayName")) {
				datasetName = property.getFirstChild().getNodeValue();
			}
			
			if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)
					&& property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("columnHints")) { //$NON-NLS-1$
				
				NodeList items = property.getChildNodes();
				Node location = null;
				for (int k = 0; k < items.getLength(); k++) {
					Node structure = items.item(k);
					for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
						Node kid = structure.getChildNodes().item(p);
						if (kid.getAttributes() == null)
							continue;

						if (nameAttributeEquals(kid, "columnName") && nodeValue(kid,"location:geom")) { //$NON-NLS-1$
							location = structure;
							break;
						}
					}
				}
				if (location != null) {
					Node clone = location.cloneNode(true);

					for (int p = 0; p < clone.getChildNodes().getLength(); p++) {
						Node kid = clone.getChildNodes().item(p);
						if (kid.getAttributes() == null) continue;

						if (nameAttributeEquals(kid, "columnName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("location:polygon");
						}else if (nameAttributeEquals(kid, "alias") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Polygon Geometry");
						}else if (nameAttributeEquals(kid, "displayName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Polygon Geometry");
						}else if (nameAttributeEquals(kid, "heading") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Polygon Geometry");
						}
					}
					property.appendChild(clone);
					
					
					for (int p = 0; p < location.getChildNodes().getLength(); p++) {
						Node kid = location.getChildNodes().item(p);
						if (kid.getAttributes() == null) continue;
							if (nameAttributeEquals(kid, "columnName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("location:point");
						}else if (nameAttributeEquals(kid, "alias") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Point Geometry");
						}else if (nameAttributeEquals(kid, "displayName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Point Geometry");
						}else if (nameAttributeEquals(kid, "heading") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Point Geometry");
						}
					}
				}
			}

			if (property.getNodeName().equalsIgnoreCase("structure")
					&& nameAttributeEquals(property, "cachedMetaData")) {
				//find resultSetList
				Node rs = null;
				for (int k = 0; k < property.getChildNodes().getLength(); k++) {
					Node kid = property.getChildNodes().item(k);
					if (kid.getAttributes() == null) continue;
					if (nameAttributeEquals(kid, "resultSet")) {
						rs = kid;
						break;
					}
				}
				if (rs != null) {
					// find location:geom update to point; close and update to polygon
					NodeList items = rs.getChildNodes();
					Node location = null;
					for (int k = 0; k < items.getLength(); k++) {
						for (int p = 0; p < items.item(k).getChildNodes().getLength(); p++) {
							Node kid = items.item(k).getChildNodes().item(p);
							if (kid.getAttributes() == null)
								continue;

							if (nameAttributeEquals(kid, "name") && nodeValue(kid,"Geometry")) { //$NON-NLS-1$
								location = items.item(k);
								break;
							}
						}
					}					
					if (location != null) {
						Node clone = location.cloneNode(true);

						for (int p = 0; p < clone.getChildNodes().getLength(); p++) {
							Node kid = clone.getChildNodes().item(p);
							if (kid.getAttributes() == null) continue;

							if (nameAttributeEquals(kid, "position") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("8");
							}else if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("Polygon Geometry");
							}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("javaObject");
							}
						}
						rs.appendChild(clone);
							
							
						for (int p = 0; p < location.getChildNodes().getLength(); p++) {
							Node kid = location.getChildNodes().item(p);
							if (kid.getAttributes() == null) continue;
								if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("Point Geometry");
							}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("javaObject");
							}
						}
					}
				}
			}
				
			if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)
					&& property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("resultSet")) { //$NON-NLS-1$

				// find location:geom update to point; close and update to polygon
				NodeList items = property.getChildNodes();
				Node geometry = null;

				for (int k = 0; k < items.getLength(); k++) {
					Node structure = items.item(k);
					for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
						Node kid = structure.getChildNodes().item(p);
						if (kid.getAttributes() == null)
							continue;

						if (nameAttributeEquals(kid, "name") && nodeValue(kid, "location:geom")) {
							geometry = structure;
							break;
						}
					}
				}
				for (int p = 0; p < geometry.getChildNodes().getLength(); p++) {
					Node kid = geometry.getChildNodes().item(p);
					if (kid.getAttributes() == null) continue;

					if (nameAttributeEquals(kid, "position") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("7");
					}else if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("location:point");
					}else if (nameAttributeEquals(kid, "nativeName") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("location:point");
					}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("javaObject");
					}else if (nameAttributeEquals(kid, "nativeDataType") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("8200");
					}
				}
					
				Node clone = geometry.cloneNode(true);
				property.appendChild(clone);
					
				for (int p = 0; p < clone.getChildNodes().getLength(); p++) {
					Node kid = clone.getChildNodes().item(p);
					if (kid.getAttributes() == null) continue;

					if (nameAttributeEquals(kid, "position") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("8");
					}else if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("location:polygon");
					}else if (nameAttributeEquals(kid, "nativeName") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("location:polygon");
					}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("javaObject");
					}else if (nameAttributeEquals(kid, "nativeDataType") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("8204");
					}
				}
			}
		}
		
		if (datasetName != null) {
			updateTable(doc, datasetName, "Geometry", "Point Geometry");
			updateMap(doc, datasetName, "location:point", "location:polygon");
		}
	}
	
	private static void updateMap(Document doc, String datasetName, String pointGeometry, String polygonGeometry) {
		
		NodeList extensions = doc.getElementsByTagName("extended-item");
		for (int i = 0; i < extensions.getLength(); i ++) {
			Node extension = extensions.item(i);
			
			if (extension.getAttributes() == null) continue;
			
			if (!extension.getAttributes().getNamedItem("extensionName").getTextContent().equals("org.wcs.smart.report.birt.SmartMap")) continue;
			
			NodeList properties = extension.getChildNodes();
			Node layerProperty = null;
			for (int k = 0; k < properties.getLength(); k ++) {
				Node property = properties.item(k);
				if (nameAttributeEquals(property, "org.wcs.smart.birt.map.layers2")) {
					layerProperty = property;
					break;
				}
				
			}
			if (layerProperty == null) continue;
			NodeList layers = layerProperty.getChildNodes();
			for (int k = 0; k < layers.getLength(); k ++) {
				Node layer = layers.item(k);
				
				boolean processLayer = false;
				Node geomColumn = null;
				Node layerName = null;
				String layerType = null;
				for (int m = 0; m < layer.getChildNodes().getLength(); m ++) {
					Node property = layer.getChildNodes().item(m);
					if (nameAttributeEquals(property, "dataSet")
							&& property.getFirstChild().getNodeValue().equals(datasetName)) {
						processLayer = true;
					}
					if (nameAttributeEquals(property, "org.wcs.smart.birt.map.layerName")) {
						layerName = property;
					}
					if (nameAttributeEquals(property, "org.wcs.smart.birt.map.geomColumn")) {
						geomColumn = property;
					}
					if (nameAttributeEquals(property, "org.wcs.smart.birt.map.layerType")) {
						layerType = property.getFirstChild().getNodeValue();
					}
				}
				
				if (processLayer) {
					if (layerType.equalsIgnoreCase("POINT")) {
						geomColumn.getFirstChild().setNodeValue(pointGeometry);
						layerName.getFirstChild().setNodeValue(layerName.getFirstChild().getNodeValue() + " - Point Geometry");
					}
					if (layerType.equalsIgnoreCase("POLYGON")) {
						geomColumn.getFirstChild().setNodeValue(polygonGeometry);
						layerName.getFirstChild().setNodeValue(layerName.getFirstChild().getNodeValue() + " - Polygon Geometry");
					}
					
				}
			}

			
		}
		
	}
	private static void updateTable(Document doc, String datasetName, String oldGeometry, String newGeometry) {
	
		NodeList tables = doc.getElementsByTagName("table");
		for (int i = 0; i < tables.getLength(); i ++) {
			Node table = tables.item(i);
		
			boolean processTable = false;
			NodeList kids = table.getChildNodes();
			for (int j = 0; j < kids.getLength(); j++) {
				Node kid = kids.item(j);
				
				//from dataset property
				if (kid.getAttributes() == null) continue;
				if (kid.getNodeName().equalsIgnoreCase("property")
						&& nameAttributeEquals(kid, "dataSet")
						&& kid.getFirstChild().getNodeValue().equalsIgnoreCase(datasetName)) {
					processTable = true;
					break;
				}
				
			}
			if (!processTable) continue;
			
			Node dataColumnsNode = null;
			for (int j = 0; j < kids.getLength(); j++) {
				Node kid = kids.item(j);
				
				//from dataset property
				if (kid.getAttributes() == null) continue;
				if (kid.getNodeName().equalsIgnoreCase("list-property")
						&& nameAttributeEquals(kid, "boundDataColumns")) {
					dataColumnsNode = kid;
					break;
				}
				
			}
			if (dataColumnsNode == null) continue;
			NodeList structures = dataColumnsNode.getChildNodes();
			
			Node geometryStructure = null;
			for (int j = 0; j < structures.getLength(); j ++) {
				Node structure = structures.item(j);
				
				
				for (int k = 0; k < structure.getChildNodes().getLength(); k ++) {
					Node kid = structure.getChildNodes().item(k);
				
					if (kid.getNodeName().equalsIgnoreCase("property")
							&& nameAttributeEquals(kid, "name")
							&& nodeValue(kid, oldGeometry)) {
						geometryStructure = structure;
						break;
					}
					
					
				}
				if (geometryStructure != null) break;
			}
			if (geometryStructure == null) continue;
			
			for (int j = 0; j < geometryStructure.getChildNodes().getLength(); j ++) {
				Node kid = geometryStructure.getChildNodes().item(j);
				
				if (nameAttributeEquals(kid, "name")) {
					kid.getFirstChild().setNodeValue(newGeometry);
				}else if (nameAttributeEquals(kid, "displayName")) {
					kid.getFirstChild().setNodeValue(newGeometry);
				}else if (nameAttributeEquals(kid, "expression")) {
					kid.getFirstChild().setNodeValue("dataSetRow[\"" + newGeometry + "\"]");

				}
			}			
			
			List<Node> toProcess = new ArrayList<>();
			toProcess.add(table);
			while(!toProcess.isEmpty()) {
				Node x = toProcess.remove(0);
				
				if (x.getNodeName().equalsIgnoreCase("data")) {
					//process
					for (int k = 0; k < x.getChildNodes().getLength(); k ++) {
						Node kid = x.getChildNodes().item(k);
						if (kid.getNodeName().equals("property") && nameAttributeEquals(kid, "resultSetColumn")
								&& nodeValue(kid, oldGeometry)) {
							kid.getFirstChild().setNodeValue(newGeometry);
						}
					}
				}
				if (x.getNodeName().equalsIgnoreCase("label")) {
					//process
					for (int k = 0; k < x.getChildNodes().getLength(); k ++) {
						Node kid = x.getChildNodes().item(k);
						if (kid.getNodeName().equals("text-property") && nameAttributeEquals(kid, "text")
								&& nodeValue(kid, oldGeometry)) {
							kid.getFirstChild().setNodeValue(newGeometry);
						}
					}
				}
				for (int k = 0; k < x.getChildNodes().getLength(); k ++) {
					toProcess.add(x.getChildNodes().item(k));
				}
				
			}
		}
		
		
	}
	
	private static void updateRecordLocationDataset(Node dataSetItem, Document doc) {
		
		String datasetName = dataSetItem.getAttributes().getNamedItem("name").getNodeValue();

		//replace location:geom with location:polygon 8204 and location:point 8200
		// update the native data type for the geometry columns
		for (int j = 0; j < dataSetItem.getChildNodes().getLength(); j++) {
			Node property = dataSetItem.getChildNodes().item(j);
			
			
			if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)
					&& property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("columnHints")) { //$NON-NLS-1$
				
				NodeList structures = property.getChildNodes();
				Node location = null;
				for (int k = 0; k < structures.getLength(); k++) {
					Node structure = structures.item(k);
					for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
						Node kid = structure.getChildNodes().item(p);
						if (kid.getAttributes() == null)
							continue;

						if (nameAttributeEquals(kid, "columnName") && nodeValue(kid, "recordlocation:geom")) {
							location = structure;
							break;
						}
					}
				}
				
				if (location != null) {
					Node clone = location.cloneNode(true);

					for (int p = 0; p < clone.getChildNodes().getLength(); p++) {
						Node kid = clone.getChildNodes().item(p);
						if (kid.getAttributes() == null) continue;
						if (nameAttributeEquals(kid, "columnName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("recordlocation:polygon");
						}else if (nameAttributeEquals(kid, "alias") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Polygon Geometry");
						}else if (nameAttributeEquals(kid, "displayName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Polygon Geometry");
						}else if (nameAttributeEquals(kid, "heading") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Polygon Geometry");
						}
					}
					property.insertBefore(clone, location.getNextSibling());
					
					for (int p = 0; p < location.getChildNodes().getLength(); p++) {
						Node kid = location.getChildNodes().item(p);
						if (kid.getAttributes() == null) continue;
						if (nameAttributeEquals(kid, "columnName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("recordlocation:point");
						}else if (nameAttributeEquals(kid, "alias") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Point Geometry");
						}else if (nameAttributeEquals(kid, "displayName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Point Geometry");
						}else if (nameAttributeEquals(kid, "heading") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Point Geometry");
						}
					}
				}
				
				
			}

			if (property.getNodeName().equalsIgnoreCase("structure")
					&& nameAttributeEquals(property, "cachedMetaData")) {
				//find resultSetList
				Node rs = null;
				for (int k = 0; k < property.getChildNodes().getLength(); k++) {
					Node kid = property.getChildNodes().item(k);
					if (kid.getAttributes() == null) continue;
					if (nameAttributeEquals(kid, "resultSet")) {
						rs = kid;
						break;
					}
				}
				if (rs != null) {
					// find location:geom update to point; close and update to polygon
					Node location = null;
					
					NodeList structures = rs.getChildNodes();
					for (int k = 0; k < structures.getLength(); k++) {
						Node structure = structures.item(k);
						for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
							Node kid = structure.getChildNodes().item(p);
							if (kid.getAttributes() == null)
								continue;

							if (nameAttributeEquals(kid, "name") && nodeValue(kid,"Geometry")) { //$NON-NLS-1$
								location = structure;
								break;
							}
						}
					}
					if (location != null) {
						Node clone = location.cloneNode(true);

						for (int p = 0; p < clone.getChildNodes().getLength(); p++) {
							Node kid = clone.getChildNodes().item(p);
							if (kid.getAttributes() == null) continue;
							if (nameAttributeEquals(kid, "position") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("7");
							}else if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("Polygon Geometry");
							}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("javaObject");
							}
						}
						rs.insertBefore(clone, location.getNextSibling());
							
						
						for (int p = 0; p < location.getChildNodes().getLength(); p++) {
							Node kid = location.getChildNodes().item(p);
							if (kid.getAttributes() == null) continue;
							if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("Point Geometry");
							}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("javaObject");
							}
						}
					}
					//renumber
					structures = rs.getChildNodes();
					int cnt = 1;
					for (int k = 0; k < structures.getLength(); k++) {
						
						Node structure = structures.item(k);
						for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
							Node kid = structure.getChildNodes().item(p);
							if (kid.getAttributes() == null)
								continue;

							if (nameAttributeEquals(kid, "position")) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue(String.valueOf(cnt));
								cnt++;
							}
						}
					}
				}

			}
				
			if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)
					&& property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("resultSet")) { //$NON-NLS-1$

				// find location:geom update to point; close and update to polygon
				NodeList items = property.getChildNodes();
				Node geometry = null;
				for (int k = 0; k < items.getLength(); k++) {
					Node structure = items.item(k);
					for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
						Node kid = structure.getChildNodes().item(p);
						if (kid.getAttributes() == null)
							continue;

						if (nameAttributeEquals(kid, "name") && nodeValue(kid, "recordlocation:geom")) {
							geometry = structure;
							break;
						}
					}
				}
					
					for (int p = 0; p < geometry.getChildNodes().getLength(); p++) {
						Node kid = geometry.getChildNodes().item(p);
						if (kid.getAttributes() == null) continue;

						if (nameAttributeEquals(kid, "position") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("3");
						}else if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("recordlocation:point");
						}else if (nameAttributeEquals(kid, "nativeName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("recordlocation:point");
						}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("javaObject");
						}else if (nameAttributeEquals(kid, "nativeDataType") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("8200");
						}
					}
					
					Node clone = geometry.cloneNode(true);
					property.insertBefore(clone, geometry.getNextSibling());
					
					for (int p = 0; p < clone.getChildNodes().getLength(); p++) {
						Node kid = clone.getChildNodes().item(p);
						if (kid.getAttributes() == null) continue;

						if (nameAttributeEquals(kid, "position") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("7");
						}else if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("recordlocation:polygon");
						}else if (nameAttributeEquals(kid, "nativeName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("recordlocation:polygon");
						}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("javaObject");
						}else if (nameAttributeEquals(kid, "nativeDataType") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("8204");
						}
					}
					//renumber
					int cnt = 1;
					for (int k = 0; k < items.getLength(); k++) {
						Node structure = items.item(k);
						for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
							Node kid = structure.getChildNodes().item(p);
							if (kid.getAttributes() == null)
								continue;

							if (nameAttributeEquals(kid, "position") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue(String.valueOf(cnt));
								cnt++;
							}
						}
					}
			}
		}
		
		updateTable(doc, datasetName, "Geometry", "Point Geometry");
		updateMap(doc, datasetName, "recordlocation:point", "recordlocation:polygon");

	}
	
	private static void updateQueryDataset(Node dataSetItem, Document doc) {
		
		String datasetName = dataSetItem.getAttributes().getNamedItem("name").getNodeValue();

		//replace loc:geom with loc:polygon 8204 and location:point 8200
		// update the native data type for the geometry columns
		for (int j = 0; j < dataSetItem.getChildNodes().getLength(); j++) {
			Node property = dataSetItem.getChildNodes().item(j);
					
					
			if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)
					&& property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("columnHints")) { //$NON-NLS-1$
						
				NodeList structures = property.getChildNodes();
				Node previous = null;
				Node location = null;
				for (int k = 0; k < structures.getLength(); k++) {
					Node structure = structures.item(k);
					for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
						Node kid = structure.getChildNodes().item(p);
						
						if (kid.getAttributes() == null)
							continue;

						if (nameAttributeEquals(kid, "columnName") && nodeValue(kid, "loc:geom")) {
							location = structure;
							previous = structures.item(k-1);
							break;
						}
					}
				}
						
				if (location != null) {
					Node clone = location.cloneNode(true);

					for (int p = 0; p < clone.getChildNodes().getLength(); p++) {
						Node kid = clone.getChildNodes().item(p);
						if (kid.getAttributes() == null) continue;
						if (nameAttributeEquals(kid, "columnName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("loc:polygon");
						}else if (nameAttributeEquals(kid, "alias") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Polygon Geometry");
						}else if (nameAttributeEquals(kid, "displayName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Polygon Geometry");
						}else if (nameAttributeEquals(kid, "heading") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Polygon Geometry");
						}
					}
								
					for (int p = 0; p < location.getChildNodes().getLength(); p++) {
						Node kid = location.getChildNodes().item(p);
						if (kid.getAttributes() == null) continue;
						if (nameAttributeEquals(kid, "columnName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("loc:point");
						}else if (nameAttributeEquals(kid, "alias") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Point Geometry");
						}else if (nameAttributeEquals(kid, "displayName") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Point Geometry");
						}else if (nameAttributeEquals(kid, "heading") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue("Point Geometry");
						}
					}
					
					property.removeChild(location);
					property.appendChild(location);
					property.appendChild(clone);
				}
			}

			if (property.getNodeName().equalsIgnoreCase("structure")
					&& nameAttributeEquals(property, "cachedMetaData")) {
				//find resultSetList
				Node rs = null;
				for (int k = 0; k < property.getChildNodes().getLength(); k++) {
					Node kid = property.getChildNodes().item(k);
					if (kid.getAttributes() == null) continue;
					if (nameAttributeEquals(kid, "resultSet")) {
						rs = kid;
						break;
					}
				}
				if (rs != null) {
					// find geometry column and replace with two
					Node location = null;
							
					NodeList structures = rs.getChildNodes();
					for (int k = 0; k < structures.getLength(); k++) {
						Node structure = structures.item(k);
						for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
							Node kid = structure.getChildNodes().item(p);
							if (kid.getAttributes() == null)
								continue;

							if (nameAttributeEquals(kid, "name") && nodeValue(kid,"Geometry")) { //$NON-NLS-1$
								location = structure;
								break;
							}
						}
					}
					if (location != null) {
						Node clone = location.cloneNode(true);

						for (int p = 0; p < clone.getChildNodes().getLength(); p++) {
							Node kid = clone.getChildNodes().item(p);
							if (kid.getAttributes() == null) continue;
							if (nameAttributeEquals(kid, "position") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("7");
							}else if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("Polygon Geometry");
							}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("javaObject");
							}
						}
									
								
						for (int p = 0; p < location.getChildNodes().getLength(); p++) {
							Node kid = location.getChildNodes().item(p);
							if (kid.getAttributes() == null) continue;
							if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("Point Geometry");
							}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue("javaObject");
							}
						}
						
						rs.removeChild(location);
						rs.appendChild(location);
						rs.appendChild(clone);
					}
					
					//renumber
					structures = rs.getChildNodes();
					int cnt = 1;
					for (int k = 0; k < structures.getLength(); k++) {
						
						Node structure = structures.item(k);
						for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
							Node kid = structure.getChildNodes().item(p);
							if (kid.getAttributes() == null)
								continue;

							if (nameAttributeEquals(kid, "position")) { //$NON-NLS-1$
								kid.getFirstChild().setNodeValue(String.valueOf(cnt));
								cnt++;
							}
						}
					}
				}
			}
						
			if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)
					&& property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("resultSet")) { //$NON-NLS-1$

						// find location:geom update to point; close and update to polygon
				NodeList items = property.getChildNodes();
				Node geometry = null;
				for (int k = 0; k < items.getLength(); k++) {
					Node structure = items.item(k);
					for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
						Node kid = structure.getChildNodes().item(p);
						if (kid.getAttributes() == null)
							continue;

						if (nameAttributeEquals(kid, "name") && nodeValue(kid, "loc:geom")) {
							geometry = structure;
							break;
						}
					}
				}
							
				for (int p = 0; p < geometry.getChildNodes().getLength(); p++) {
					Node kid = geometry.getChildNodes().item(p);
					if (kid.getAttributes() == null) continue;

					if (nameAttributeEquals(kid, "position") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("3");
					}else if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("loc:point");
					}else if (nameAttributeEquals(kid, "nativeName") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("loc:point");
					}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("javaObject");
					}else if (nameAttributeEquals(kid, "nativeDataType") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("8200");
					}
				}
				
				Node clone = geometry.cloneNode(true);
							
				for (int p = 0; p < clone.getChildNodes().getLength(); p++) {
					Node kid = clone.getChildNodes().item(p);
					if (kid.getAttributes() == null) continue;

					if (nameAttributeEquals(kid, "position") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("7");
					}else if (nameAttributeEquals(kid, "name") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("loc:polygon");
					}else if (nameAttributeEquals(kid, "nativeName") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("loc:polygon");
					}else if (nameAttributeEquals(kid, "dataType") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("javaObject");
					}else if (nameAttributeEquals(kid, "nativeDataType") ) { //$NON-NLS-1$
						kid.getFirstChild().setNodeValue("8204");
					}
				}
				
				property.removeChild(geometry);
				property.appendChild(geometry);
				property.appendChild(clone);
				
				//renumber
				int cnt = 1;
				for (int k = 0; k < items.getLength(); k++) {
					Node structure = items.item(k);
					for (int p = 0; p < structure.getChildNodes().getLength(); p++) {
						Node kid = structure.getChildNodes().item(p);
						if (kid.getAttributes() == null)
							continue;

						if (nameAttributeEquals(kid, "position") ) { //$NON-NLS-1$
							kid.getFirstChild().setNodeValue(String.valueOf(cnt));
							cnt++;
						}
					}
				}
			}
		}
		updateTable(doc, datasetName, "Geometry", "Point Geometry");
		updateMap(doc, datasetName, "loc:point", "loc:polygon");

	
	}
	
	
	public static void main (String[] args) throws Exception{
		
		ProfileReport800Upgrader me = new ProfileReport800Upgrader();
		
		Path input = Paths.get("C:\\temp\\smart8reports\\000004.rptdesign");
		Path output = Paths.get("C:\\temp\\smart8reports\\000004.out.rptdesign");
		
		Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
		me.upgradeFile(output);
	}
}
