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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartQuery;
import org.wcs.smart.data.oda.smart.impl.GeometryColumn;
import org.wcs.smart.data.oda.smart.impl.QueryDatasetExtensionManager;
import org.wcs.smart.data.oda.smart.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.util.UuidUtils;

/**
 * Script to upgrade report files from the "old" map report structure to the new
 * map report structure implemented in 4.0
 * 
 * @author Emily
 *
 */
public class Report400Upgrader implements IDatabaseUpgrader {

	private static final String JAVA_OBJECT_TYPE = "javaObject"; //$NON-NLS-1$
	private static final String POSITION_ATT_VALUE = "position"; //$NON-NLS-1$
	private static final String EXTENSION_ID_ATT_NAME = "extensionID"; //$NON-NLS-1$
	private static final String ODA_DATA_SET_TAG_NAME = "oda-data-set"; //$NON-NLS-1$
	private static final String SIMPLE_PROPERTY_LIST_TAG_NAME = "simple-property-list"; //$NON-NLS-1$
	private static final String EXTENSION_NAME_TAG_NAME = "extensionName"; //$NON-NLS-1$
	private static final String EXTENDED_ITEM_TAG_NAME = "extended-item"; //$NON-NLS-1$
	private static final String PROPERTY_TAG_NAME = "property"; //$NON-NLS-1$
	private static final String STRUCTURE_TAG_NAME = "structure"; //$NON-NLS-1$
	private static final String NAME_ATT_NAME = "name"; //$NON-NLS-1$
	private static final String LIST_PROPERTY_TAG_NAME = "list-property"; //$NON-NLS-1$

	private enum Type {TABLE, QUERY, PLAN, INTEL};
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		Session session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			upgradeReportFiles(session);
			
			final List<String> warnings = new ArrayList<String>();
			
			//NOTE: we are not allowed to use hibernate objects attached to session (see )
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
					warnings.add(MessageFormat.format(Messages.Report400Upgrader_PlanTemplateError, ca.getName()));
					ReportPlugIn.log(ex.getMessage(), ex);
				}
				
				try{
					upgradeIntelligence(ca);
				} catch (Exception ex) {
					warnings.add(MessageFormat.format(Messages.Report400Upgrader_IntelTemplateError, ca.getName()));
					ReportPlugIn.log(ex.getMessage(), ex);
				}
				
				
			}
			

			if (warnings.size() > 0){
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), Messages.Report400Upgrader_ErrorTitle, Messages.Report400Upgrader_TemplateUpdateError, warnings);
						wd.open();
					}});
			}
			session.getTransaction().commit();
		} finally {
			session.close();
		}
	}

	private void upgradePlan(ConservationArea ca) throws Exception{
		File planFile = new File(ca.getFileDataStoreLocation() + File.separator + "plans" + File.separator + "planTemplate.rptdesign"); //$NON-NLS-1$ //$NON-NLS-2$
		if (planFile.exists()){
			xmlUpdater(planFile);
		}
	}
	private void upgradeIntelligence(ConservationArea ca) throws Exception{
		File planFile = new File(ca.getFileDataStoreLocation() + File.separator + "intelligence" + File.separator + "intelligenceTemplate.rptdesign"); //$NON-NLS-1$ //$NON-NLS-2$
		if (planFile.exists()){
			xmlUpdater(planFile);
		}
	}
	
	private void upgradeReportFiles(Session session) throws Exception {
		final List<String> warnings = new ArrayList<>();

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
							xmlUpdater(new File(ReportPlugIn.getReportDirectory(caFileDataStoreLocation), reportFilename));
						} catch (Exception ex) {
							String reportName = reportId;
							PreparedStatement ps = c.prepareStatement("select lbl.VALUE from smart.REPORT rpt left join smart.I18N_LABEL lbl on lbl.ELEMENT_UUID=rpt.uuid left join smart.LANGUAGE lng on lbl.LANGUAGE_UUID=lng.UUID where rpt.UUID=? and lng.ISDEFAULT"); //$NON-NLS-1$
							ps.setBytes(1, uuid);
							try (ResultSet name_rs = ps.executeQuery()) {
								if (name_rs.next()) {
									reportName = name_rs.getString(1);
								}
							} catch (Exception e) {
								ReportPlugIn.log("Failed to find a name for report with uuid=" + UuidUtils.uuidToString(UuidUtils.byteToUUID(uuid)), e); //$NON-NLS-1$
							}
							warnings.add(MessageFormat.format(Messages.Report400Upgrader_UpgradeError, reportName, ex.getMessage()));
							ReportPlugIn.log(ex.getMessage(), ex);
						}
					}
				}
			}
		});

		if (warnings.size() > 0){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), Messages.Report400Upgrader_ErrorTitle, Messages.Report400Upgrader_ErrorMessage, warnings);
					wd.open();
				}});
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
	public static void xmlUpdater(File file) throws Exception {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(file);

		// update library
		NodeList nl = doc.getElementsByTagName(LIST_PROPERTY_TAG_NAME);
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n.getAttributes().getNamedItem(NAME_ATT_NAME).getNodeValue().equalsIgnoreCase("libraries")) { //$NON-NLS-1$
				Node structure = findNode(n, STRUCTURE_TAG_NAME);
				NodeList properties = structure.getChildNodes();
				Node toUpdate = null;
				boolean isSmart = false;
				for (int j = 0; j < nl.getLength(); j++) {
					Node prop = properties.item(j);
					if (prop == null)
						continue;
					if (prop.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME)) {
						if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getNodeValue().equalsIgnoreCase("fileName")) { //$NON-NLS-1$  
							toUpdate = prop;
						}
						if (prop.getAttributes().getNamedItem(NAME_ATT_NAME).getNodeValue().equalsIgnoreCase("namespace")) { //$NON-NLS-1$  
							isSmart = true;
						}
					}
				}
				if (toUpdate != null && isSmart) {
					String library = toUpdate.getTextContent();
					Path p = FileSystems.getDefault().getPath(library);
					Path remove = FileSystems.getDefault().getPath(".\\data\\filestore\\"); //$NON-NLS-1$
					Path newp = remove.relativize(p);
					toUpdate.setTextContent(newp.toString());
				}
			}

		}

		//map elements
		HashMap<String, String> datasetname2extension = new HashMap<>();		

		updateDataSets(doc, datasetname2extension);
		updateMap(doc, datasetname2extension);

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		transformer.setOutputProperty(
				"{http://xml.apache.org/xslt}indent-amount", "2"); //$NON-NLS-1$ //$NON-NLS-2$

		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
	}
	
	private static void updateMap(Document doc, HashMap<String,String> dataSetTypeMapping) throws Exception{
		NodeList mapItems = doc.getElementsByTagName(EXTENDED_ITEM_TAG_NAME);
		
		for (int i = 0; i < mapItems.getLength(); i++) {
			Node mapItem = mapItems.item(i);

			if (mapItem.getAttributes().getNamedItem(EXTENSION_NAME_TAG_NAME)
					.getTextContent().equalsIgnoreCase("org.wcs.smart.report.birt.SmartMap")) { //$NON-NLS-1$  

				List<String> layers = new ArrayList<String>();
				List<String> dataset = new ArrayList<String>();
				List<String> names = new ArrayList<String>();
				List<String> styles = new ArrayList<String>();

				List<Node> toDelete = new ArrayList<Node>();

				for (int k = 0; k < mapItem.getChildNodes().getLength(); k++) {
					Node kid = mapItem.getChildNodes().item(k);
					if (kid.getNodeName().equalsIgnoreCase(SIMPLE_PROPERTY_LIST_TAG_NAME)) {
						String name = kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent();
						List<String> values = new ArrayList<String>();

						for (int j = 0; j < kid.getChildNodes().getLength(); j++) {
							Node valueNode = kid.getChildNodes().item(j);
							if (valueNode.getNodeName().equalsIgnoreCase("value")) { //$NON-NLS-1$
								values.add(valueNode.getTextContent());
							}
						}

						if (name.equals("org.wcs.smart.birt.map.layers")) { //$NON-NLS-1$
							layers.addAll(values);
						} else if (name.equals("org.wcs.smart.birt.map.layerDataSet")) { //$NON-NLS-1$
							dataset.addAll(values);
						} else if (name.equals("org.wcs.smart.birt.map.layerNames")) { //$NON-NLS-1$
							names.addAll(values);
						} else if (name.equals("org.wcs.smart.birt.map.layerStyles")) { //$NON-NLS-1$
							styles.addAll(values);
						}
						toDelete.add(kid);
					}
				}
				
				for (Node n : toDelete) {
					n.getParentNode().removeChild(n);
				}

				if (dataset.size() != layers.size()){
					dataset.clear();
					for (String l : layers){
						NodeList allnodes = doc.getElementsByTagName("xml-property");
						boolean found = false;
						for (int x = 0; x < allnodes.getLength(); x ++){
							Node node = allnodes.item(x);
							if (!node.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equals("queryText")) continue;
							
							if (!node.getTextContent().contains(l)) continue;
							//this is the dataset
							found = true;
							dataset.add(node.getParentNode().getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent());
							break;
						}
						
						if (!found){
							//cannot upgrade report blah blah balh
							throw new Exception ("Cannot find dataset for report map layer.");
						}
					}
				}
				
				
				if (layers.size() > 0) {
					Node newProp = doc.createElement(PROPERTY_TAG_NAME);
					Node attribute = doc.createAttribute(NAME_ATT_NAME);
					attribute.setTextContent("org.wcs.smart.birt.map.layers2"); //$NON-NLS-1$
					newProp.getAttributes().setNamedItem(attribute);

					mapItem.appendChild(newProp);

					for (int k = 0; k < layers.size(); k++) {
						Node extItem = doc.createElement(EXTENDED_ITEM_TAG_NAME);
						Node extNameAtt = doc.createAttribute(EXTENSION_NAME_TAG_NAME);
						extNameAtt.setTextContent("org.wcs.smart.report.birt.MapLayer"); //$NON-NLS-1$
						extItem.getAttributes().setNamedItem(extNameAtt);
						
						newProp.appendChild(extItem);
						String[] propNames = new String[] {
								"org.wcs.smart.birt.map.layerName",  //$NON-NLS-1$
								"org.wcs.smart.birt.map.layerStyle",  //$NON-NLS-1$
								"org.wcs.smart.birt.map.layerType",  //$NON-NLS-1$
								"org.wcs.smart.birt.map.geomColumn",  //$NON-NLS-1$
								"dataSet" }; //$NON-NLS-1$ 
					
						String[] dataValues = new String[] { 
								names.get(k),
								styles.get(k),
								parseQueryType(layers.get(k), dataSetTypeMapping.get(dataset.get(k))),
								parseGeometryColumn(layers.get(k), dataSetTypeMapping.get(dataset.get(k))),
								dataset.get(k) };

						for (int x = 0; x < propNames.length; x++) {
							if (dataValues[x] != null) {
								Node extProp = doc.createElement(PROPERTY_TAG_NAME);
								Node propName = doc.createAttribute(NAME_ATT_NAME);
								propName.setTextContent(propNames[x]);
								extProp.getAttributes().setNamedItem(propName);
								extProp.setTextContent(dataValues[x]);
								extItem.appendChild(extProp);
							}
						}
					}
				}
			}
		}
	}
	private static void updateDataSets(Document doc,  HashMap<String,String> dataSetTypeMapping) throws Exception{
		// update dataset columns - we need to add Geometry column to various
				// query types
				
				NodeList dataSetItems = doc.getElementsByTagName(ODA_DATA_SET_TAG_NAME); 
				for (int i = 0; i < dataSetItems.getLength(); i++) {
					Type dataSetType = null;
					Node dataSetItem = dataSetItems.item(i);
					String extid = dataSetItem.getAttributes().getNamedItem(EXTENSION_ID_ATT_NAME).getTextContent(); 
					
					if (extid.equalsIgnoreCase("org.wcs.smart.data.oda.smart.smartTableDataset")) { //$NON-NLS-1$
						dataSetType = Type.TABLE;
					} else if (extid.equalsIgnoreCase("org.wcs.smart.data.oda.smart.smartQueryDataset")) { //$NON-NLS-1$
						dataSetType = Type.QUERY;
					}else if (extid.equals("org.wcs.smart.plan.report.oda.SmartPlanTargets")){ //$NON-NLS-1$
						dataSetType = Type.PLAN;
					}else if (extid.equals("org.wcs.smart.intelligence.report.oda.SmartIntelligencePoints")){ //$NON-NLS-1$
						dataSetType = Type.INTEL;
					}

					if (dataSetType == null) continue;
					dataSetTypeMapping.put(dataSetItem.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent(), extid);
					Node hints = null;
					Node metadata = null;
					Node resultSet = null;
					Node queryText = null;
					int maxColumnNum = -1;
					for (int j = 0; j < dataSetItem.getChildNodes().getLength(); j++) {
						Node property = dataSetItem.getChildNodes().item(j);
						if (property.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)) {
							String name = property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent();
							if (name.equalsIgnoreCase("columnHints")) { //$NON-NLS-1$
								hints = property;
							}
							if (name.equalsIgnoreCase("resultSet")) { //$NON-NLS-1$
								resultSet = property;

								for (int k = 0; k < property.getChildNodes().getLength(); k++) {
									Node struct = property.getChildNodes().item(k);
									for (int l = 0; l < struct.getChildNodes().getLength(); l++) {
										Node prop = struct.getChildNodes().item(l);
										if (prop.getNodeName().equals(PROPERTY_TAG_NAME) && 
												prop.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase(POSITION_ATT_VALUE)) {
											Integer value = Integer.parseInt(prop.getTextContent());
											if (value > maxColumnNum) {
												maxColumnNum = value;
											}
										}
									}
								}
							}
						}
						if (property.getNodeName().equalsIgnoreCase(STRUCTURE_TAG_NAME)) {
							String tname = property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent();
							if (tname.equalsIgnoreCase("cachedMetaData")) { //$NON-NLS-1$
								for (int k = 0; k < property.getChildNodes().getLength(); k++) {
									Node subproperty = property.getChildNodes().item(k);
									if (subproperty.getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)) {
										String name = subproperty.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent();
										if (name.equalsIgnoreCase("resultSet")) { //$NON-NLS-1$
											metadata = subproperty;
										}
									}
								}
							}
						}
						if (property.getNodeName().equalsIgnoreCase("xml-property")) { //$NON-NLS-1$
							if (property.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("queryText")) { //$NON-NLS-1$
								queryText = property;
							}
						}
					}
					
					if (dataSetType == Type.PLAN){
						//rename the Status column to targetStatus
						//resultSet name=Status to name=targetStatus; nativeName=Status to nativeName=targetStatus
						//cachedMetadata name=Status to name=targetStatus
						
						for (int x = 0; x < resultSet.getChildNodes().getLength(); x++) {
							Node structure = resultSet.getChildNodes().item(x);
							if (structure.getNodeName().equalsIgnoreCase(STRUCTURE_TAG_NAME)) {
								for (int y = 0; y < structure.getChildNodes().getLength(); y++) {
									Node kid = structure.getChildNodes().item(y);
									if (kid.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME) && 
											kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase(NAME_ATT_NAME) && 
											kid.getTextContent().equalsIgnoreCase("Status")) { //$NON-NLS-1$
										kid.setTextContent("targetStatus"); //$NON-NLS-1$
									}
									if (kid.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME) && 
											kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("nativeName") &&  //$NON-NLS-1$
											kid.getTextContent().equalsIgnoreCase("Status")) { //$NON-NLS-1$
										kid.setTextContent("targetStatus"); //$NON-NLS-1$
									}
								}		
							}
						}
						
						for (int x = 0; x < metadata.getChildNodes().getLength(); x++) {
							Node structure = metadata.getChildNodes().item(x);
							if (structure.getNodeName().equalsIgnoreCase(STRUCTURE_TAG_NAME)) {
								for (int y = 0; y < structure.getChildNodes().getLength(); y++) {
									Node kid = structure.getChildNodes().item(y);
									if (kid.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME) && 
											kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase(NAME_ATT_NAME) && 
											kid.getTextContent().equalsIgnoreCase("Status")) { //$NON-NLS-1$
										kid.setTextContent("targetStatus"); //$NON-NLS-1$
									}
								}		
							}
						}
						
						for (int x = 0; x < hints.getChildNodes().getLength(); x++) {
							Node structure = hints.getChildNodes().item(x);
							if (structure.getNodeName().equalsIgnoreCase(STRUCTURE_TAG_NAME)) {
								Node toDelete = null;
								String alias = null;
								for (int y = 0; y < structure.getChildNodes().getLength(); y++) {
									Node kid = structure.getChildNodes().item(y);
									if (kid.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME) && 
											kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("columnName") &&  //$NON-NLS-1$
											kid.getTextContent().equalsIgnoreCase("Status")) { //$NON-NLS-1$
										kid.setTextContent("targetStatus"); //$NON-NLS-1$
									}
									if (kid.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME) && 
											kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("analysis")){ //$NON-NLS-1$
										toDelete = kid;
									}
									if (kid.getNodeName().equalsIgnoreCase("text-property") &&  //$NON-NLS-1$
											kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("displayName")){ //$NON-NLS-1$
										alias = kid.getTextContent();
									}
								}
								if (toDelete != null) toDelete.getParentNode().removeChild(toDelete);
								if (alias != null){
									if (alias.equalsIgnoreCase("Status")){ //$NON-NLS-1$
										alias = "targetStatus"; //$NON-NLS-1$
									}
									Node propNode = doc.createElement(PROPERTY_TAG_NAME);
									Attr attribute = doc.createAttribute(NAME_ATT_NAME);
									attribute.setValue("alias"); //$NON-NLS-1$
									propNode.getAttributes().setNamedItem(attribute);
									propNode.setTextContent(alias);
									structure.appendChild(propNode);
								}
							}
						}
						NodeList structures = doc.getElementsByTagName("expression");  //$NON-NLS-1$
						for (int j = 0; j < structures.getLength(); j++) {
							Node n = structures.item(j);
							if (n != null && n.getParentNode() != null && n.getParentNode().getParentNode() != null &&
							  n.getParentNode().getNodeName().equalsIgnoreCase(STRUCTURE_TAG_NAME)
								&& n.getParentNode().getParentNode().getNodeName().equalsIgnoreCase(LIST_PROPERTY_TAG_NAME)
									&& n.getParentNode().getParentNode().getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("boundDataColumns") //$NON-NLS-1$
									&& n.getTextContent().equalsIgnoreCase("dataSetRow[\"Status\"]")){ //$NON-NLS-1$
								n.setTextContent("dataSetRow[\"targetStatus\"]"); //$NON-NLS-1$
							}
									
						}
						
						
					}
					if (queryText != null) {
						GeometryColumn[] columns = null;
						if (dataSetType == Type.QUERY) {
							String queryType = queryText.getTextContent().split(":")[0]; //$NON-NLS-1$
							if (queryText.getTextContent().trim().isEmpty()){
								//assume patrol plan query
								queryType = "patrolquery"; //$NON-NLS-1$
							}
							
							queryType = QueryTypeManager.INSTANCE.findDeprecatedQueryTypeString(queryType);
							AbstractSmartQuery qq = QueryDatasetExtensionManager.getInstance().getDatasetHandler(queryType);
							columns = qq.getGeometryColumns(queryType, Locale.getDefault());

							//gridded queries we need to add aliases to hints as
							//we renamed the tile_x columns
							if (queryType.equalsIgnoreCase("entitygrid") || //$NON-NLS-1$
								queryType.equalsIgnoreCase("observationgrid") ||//$NON-NLS-1$ 
								queryType.equalsIgnoreCase("patrolgrid") || //$NON-NLS-1$
								queryType.equalsIgnoreCase("surveygrid")) { //$NON-NLS-1$
								
								for (int x = 0; x < hints.getChildNodes().getLength(); x++) {
									Node structure = hints.getChildNodes().item(x);
									
									if (structure.getNodeName().equalsIgnoreCase(STRUCTURE_TAG_NAME)){
										boolean alais = false;
										String heading = null;
										for (int y = 0; y < structure.getChildNodes().getLength(); y++) {
											Node kid = structure.getChildNodes().item(y);
											if (kid.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME) &&
													kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("alias")) { //$NON-NLS-1$
												alais = true;
											}
											if (kid.getNodeName().equalsIgnoreCase("text-property") && //$NON-NLS-1$
													kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("displayName")) { //$NON-NLS-1$
												heading = kid.getTextContent();
											}
										}
										if (!alais){
											Node propNode = doc.createElement(PROPERTY_TAG_NAME);
											Attr attribute = doc.createAttribute(NAME_ATT_NAME);
											attribute.setValue("alias"); //$NON-NLS-1$
											propNode.getAttributes().setNamedItem(attribute);
											propNode.setTextContent(heading);
											structure.appendChild(propNode); 
										}
											
									}
								}
							}
							
							// for patrol queries we need to remove observer column as
							// this is not valid in the new query but is in old query
							// type
							if (queryType.equalsIgnoreCase("patrolquery")) { //$NON-NLS-1$
								Node toDelete = null;
								for (int x = 0; x < hints.getChildNodes().getLength(); x++) {
									Node structure = hints.getChildNodes().item(x);
									if (structure.getNodeName().equalsIgnoreCase(STRUCTURE_TAG_NAME)) {
										for (int y = 0; y < structure.getChildNodes().getLength(); y++) {
											Node kid = structure.getChildNodes().item(y);
											if (kid.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME )&& 
													kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase("columnName") &&  //$NON-NLS-1$
													kid.getTextContent().equalsIgnoreCase("ob:observer")) { //$NON-NLS-1$
												toDelete = structure;
												break;
											}
										}
									}
									if (toDelete != null) break;
								}
								if (toDelete != null) {
									toDelete.getParentNode().removeChild(toDelete);
								}

								toDelete = null;
								int index = -1;
								for (int x = 0; x < resultSet.getChildNodes().getLength(); x++) {
									Node structure = resultSet.getChildNodes().item(x);
									int thisindex = -1;
									if (structure.getNodeName().equalsIgnoreCase(STRUCTURE_TAG_NAME)) {
										for (int y = 0; y < structure.getChildNodes().getLength(); y++) {
											Node kid = structure.getChildNodes().item(y);
											if (kid.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME) && 
													kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase(NAME_ATT_NAME) && 
													kid.getTextContent().equalsIgnoreCase("ob:observer")) { //$NON-NLS-1$
												toDelete = structure;
												if (thisindex != -1) {
													index = thisindex;
													break;
												}
											}
											if (kid.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME) && 
													kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase(POSITION_ATT_VALUE)) {
												thisindex = Integer.parseInt(kid.getTextContent());
												if (toDelete != null) {
													index = thisindex;
													break;
												}
											}
										}
									}
									if (toDelete != null) break;
								}
								
								if (toDelete != null){
									toDelete.getParentNode().removeChild(toDelete);
								}

								toDelete = null;
								for (int x = 0; x < metadata.getChildNodes().getLength(); x++) {
									Node structure = metadata.getChildNodes().item(x);
									if (structure.getNodeName().equalsIgnoreCase(STRUCTURE_TAG_NAME)) {
										for (int y = 0; y < structure.getChildNodes().getLength(); y++) {
											Node kid = structure.getChildNodes().item(y);
											if (kid.getNodeName().equalsIgnoreCase(PROPERTY_TAG_NAME) && 
													kid.getAttributes().getNamedItem(NAME_ATT_NAME).getTextContent().equalsIgnoreCase(POSITION_ATT_VALUE)) {
												int thisindex = Integer.parseInt(kid.getTextContent());
												if (thisindex == index) {
													toDelete = structure;
													break;
												}
											}
										}
									}
									if (toDelete != null)
										break;
								}
								if (toDelete != null){
									toDelete.getParentNode().removeChild(toDelete);
								}
							}
							
						} else if (dataSetType == Type.TABLE){
							String geom = parseGeometryColumn(queryText
									.getTextContent(), ""); //$NON-NLS-1$
							if (geom != null){
								GeometryColumn gc = new GeometryColumn(geom, geom);
								columns = new GeometryColumn[] { gc };
							}
						} else if (dataSetType == Type.PLAN || dataSetType == Type.INTEL){
							GeometryColumn gc = new GeometryColumn("Geometry", "geometry"); //$NON-NLS-1$ //$NON-NLS-2$
							columns = new GeometryColumn[] { gc };
						}

						if (columns != null) {
							for (GeometryColumn c : columns) {
								maxColumnNum++;
								Node dsstructure = doc.createElement(STRUCTURE_TAG_NAME);
								String[] prop = new String[] { 
										POSITION_ATT_VALUE,
										NAME_ATT_NAME, 
										"nativeName",  //$NON-NLS-1$
										"dataType", //$NON-NLS-1$
										"nativeDataType" }; //$NON-NLS-1$
								String[] propValues = new String[] {
										String.valueOf(maxColumnNum), 
										c.getKey(),
										c.getKey(), 
										JAVA_OBJECT_TYPE, 
										"2000" }; //$NON-NLS-1$
								
								for (int x = 0; x < prop.length; x++) {
									Node propNode = doc.createElement(PROPERTY_TAG_NAME);
									Attr attribute = doc.createAttribute(NAME_ATT_NAME);
									attribute.setValue(prop[x]);
									propNode.getAttributes().setNamedItem(attribute);
									propNode.setTextContent(propValues[x]);
									dsstructure.appendChild(propNode);
								}
								resultSet.appendChild(dsstructure);

								Node mdstructure = doc.createElement(STRUCTURE_TAG_NAME);
								prop = new String[] { POSITION_ATT_VALUE,
										NAME_ATT_NAME,
										"dataType" }; //$NON-NLS-1$
								propValues = new String[] {
										String.valueOf(maxColumnNum), 
										c.getLabel(),
										JAVA_OBJECT_TYPE };
								
								for (int x = 0; x < prop.length; x++) {
									Node propNode = doc.createElement(PROPERTY_TAG_NAME);
									Attr attribute = doc.createAttribute(NAME_ATT_NAME);
									attribute.setValue(prop[x]);
									propNode.getAttributes().setNamedItem(attribute);
									propNode.setTextContent(propValues[x]);
									mdstructure.appendChild(propNode);

								}
								metadata.appendChild(mdstructure);

								Node hintstructure = doc.createElement(STRUCTURE_TAG_NAME);
								prop = new String[] { "columnName", "alias" }; //$NON-NLS-1$ //$NON-NLS-2$
								propValues = new String[] { c.getKey(), c.getLabel() };
								
								for (int x = 0; x < prop.length; x++) {
									Node propNode = doc.createElement(PROPERTY_TAG_NAME);
									Attr attribute = doc.createAttribute(NAME_ATT_NAME);
									attribute.setValue(prop[x]);
									propNode.getAttributes().setNamedItem(attribute);
									propNode.setTextContent(propValues[x]);
									hintstructure.appendChild(propNode);
								}
								
								prop = new String[] { "displayName", "heading" }; //$NON-NLS-1$ //$NON-NLS-2$
								propValues = new String[] { c.getLabel(), c.getLabel() };
								for (int x = 0; x < prop.length; x++) {
									Node propNode = doc.createElement("text-property"); //$NON-NLS-1$
									Attr attribute = doc.createAttribute(NAME_ATT_NAME);
									attribute.setValue(prop[x]);
									propNode.getAttributes().setNamedItem(attribute);
									propNode.setTextContent(propValues[x]);
									hintstructure.appendChild(propNode);

								}
								hints.appendChild(hintstructure);
							}

						}

					}

				}
	}
	private static String parseQueryType(String queryText, String datasetExtId) {
		if (queryText.trim().isEmpty()){
			if (datasetExtId.equalsIgnoreCase("org.wcs.smart.intelligence.report.oda.SmartIntelligencePointst")){ //$NON-NLS-1$
				return "MULTIPOINT"; //$NON-NLS-1$
			}else if (datasetExtId.equalsIgnoreCase("org.wcs.smart.data.oda.smart.smartQueryDataset")){ //$NON-NLS-1$
				//assume patrol plan query for plan template
				return "MULTILINE"; //$NON-NLS-1$
			}
		}
		String[] bits = queryText.split(":"); //$NON-NLS-1$
		if (bits[0].equals("ENTITY")) { //$NON-NLS-1$
			return "POINT"; //$NON-NLS-1$
		}
		if (bits[0].equals("subplan") || bits[0].equals("plan")){ //$NON-NLS-1$ //$NON-NLS-2$
			return "MULTIPOINT"; //$NON-NLS-1$
		}
		if (bits[0].equals("SD_SU")) { //$NON-NLS-1$
			if (bits[1].equalsIgnoreCase("TRANSECT")) { //$NON-NLS-1$
				return "LINE"; //$NON-NLS-1$
			}
			if (bits[1].equalsIgnoreCase("PLOT")) { //$NON-NLS-1$
				return "POINT"; //$NON-NLS-1$
			}
		}
		String queryType = bits[0];
		queryType = QueryTypeManager.INSTANCE.findDeprecatedQueryTypeString(bits[0]);
		
		if (queryType.equalsIgnoreCase("entitygrid") //$NON-NLS-1$
				|| queryType.equalsIgnoreCase("observationgrid") //$NON-NLS-1$
				|| queryType.equalsIgnoreCase("patrolgrid") //$NON-NLS-1$
				|| queryType.equalsIgnoreCase("surveygrid")) { //$NON-NLS-1$
			return "RASTER"; //$NON-NLS-1$
		}

		if (queryType.equalsIgnoreCase("surveymission") //$NON-NLS-1$
				|| queryType.equalsIgnoreCase("surveymissiontrack") //$NON-NLS-1$
				|| queryType.equalsIgnoreCase("patrolquery")) { //$NON-NLS-1$
			return "MULTILINE"; //$NON-NLS-1$
		}

		if (queryType.equalsIgnoreCase("intelligencerecord")) { //$NON-NLS-1$
			return "MULTIPOINT"; //$NON-NLS-1$
		}
		return "POINT"; //$NON-NLS-1$
	}

	private static String parseGeometryColumn(String queryText, String datasetExtId)
			throws Exception {
		if (queryText.trim().isEmpty()){
			if (datasetExtId.equalsIgnoreCase("org.wcs.smart.intelligence.report.oda.SmartIntelligencePoints")){ //$NON-NLS-1$
				return "geometry"; //$NON-NLS-1$
			}else if (datasetExtId.equalsIgnoreCase("org.wcs.smart.data.oda.smart.smartQueryDataset")){ //$NON-NLS-1$
				AbstractSmartQuery qq = QueryDatasetExtensionManager.getInstance().getDatasetHandler("patrolquery"); //$NON-NLS-1$
				GeometryColumn[] columns = qq.getGeometryColumns("patrolquery", Locale.getDefault()); //$NON-NLS-1$
				if (columns == null)
					return null;
				return columns[0].getKey();
			}
		}
		
		String[] bits = queryText.split(":"); //$NON-NLS-1$
		if (bits[0].equals("ENTITY")) { //$NON-NLS-1$
			return "entity:geometry"; //$NON-NLS-1$
		}
		if (bits[0].equals("SD_SU")) { //$NON-NLS-1$
			return "su:geometry"; //$NON-NLS-1$
		}
		if (bits[0].equals("subplan") //$NON-NLS-1$
				|| bits[0].equals("plan")){ //$NON-NLS-1$ 
			return "geometry"; //$NON-NLS-1$
		}
		// plans are multipoint with the name geometry
		// planpatrol is multiline with name track:geometry

		// deprecated query types
		String queryType = QueryTypeManager.INSTANCE.findDeprecatedQueryTypeString(bits[0]);

		AbstractSmartQuery qq = QueryDatasetExtensionManager.getInstance().getDatasetHandler(queryType);
		if (qq == null) return null; //this is not a valid query type;likely a table with no geometry column (employee)
		
		GeometryColumn[] columns = qq.getGeometryColumns(queryType, Locale.getDefault());
		if (columns == null)
			return null;
		return columns[0].getKey();
	}

	private static Node findNode(Node parent, String type) {
		for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
			Node kid = parent.getChildNodes().item(i);
			if (kid.getNodeName().equalsIgnoreCase(type)) {
				return kid;
			}
		}
		return null;
	}
	
}
