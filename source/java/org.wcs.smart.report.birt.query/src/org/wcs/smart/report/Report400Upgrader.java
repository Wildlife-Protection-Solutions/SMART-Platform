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
import java.text.MessageFormat;
import java.util.ArrayList;
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
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartQuery;
import org.wcs.smart.data.oda.smart.impl.GeometryColumn;
import org.wcs.smart.data.oda.smart.impl.QueryDatasetExtensionManager;
import org.wcs.smart.data.oda.smart.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.upgrade.IDatabaseUpgrader;

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

	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		Session session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			upgradeReportFiles(session);
			session.getTransaction().commit();
		} finally {
			session.close();
		}
	}

	@SuppressWarnings("unchecked")
	private void upgradeReportFiles(Session session) throws Exception {
		List<Report> reports = session.createCriteria(Report.class).list();

		final List<String> warnings = new ArrayList<String>();
		for (Report r : reports) {
			try {
				xmlUpdater(new File(ReportPlugIn.getReportDirectory(r
						.getConservationArea()), r.getFilename()));
			} catch (Exception ex) {
				warnings.add(MessageFormat.format(Messages.Report400Upgrader_UpgradeError, r.getName() + " [" + r.getConservationArea().getName() + "]", ex.getMessage())); //$NON-NLS-1$ //$NON-NLS-2$
				ReportPlugIn.log(ex.getMessage(), ex);
			}
		}

		if (warnings.size() > 0){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), Messages.Report400Upgrader_ErrorTitle, Messages.Report400Upgrader_ErrorMessage, warnings);
					wd.open();
				}});
		}
		// TODO: upgrade customized plan templates
		// TODO: upgrade customized intelligence templates
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
								parseQueryType(layers.get(k)),
								parseGeometryColumn(layers.get(k)),
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

		// update dataset columns - we need to add Geometry column to various
		// query types
		NodeList dataSetItems = doc.getElementsByTagName(ODA_DATA_SET_TAG_NAME); 
		for (int i = 0; i < dataSetItems.getLength(); i++) {
			boolean isTable = false;
			Node dataSetItem = dataSetItems.item(i);
			String extid = dataSetItem.getAttributes().getNamedItem(EXTENSION_ID_ATT_NAME).getTextContent(); 
			if (extid.equalsIgnoreCase("org.wcs.smart.data.oda.smart.smartTableDataset")) { //$NON-NLS-1$
				isTable = true;
			} else if (extid.equalsIgnoreCase("org.wcs.smart.data.oda.smart.smartQueryDataset")) { //$NON-NLS-1$
				isTable = false;
			}

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
			if (queryText != null) {
				GeometryColumn[] columns = null;
				if (!isTable) {
					String queryType = queryText.getTextContent().split(":")[0]; //$NON-NLS-1$
					queryType = QueryTypeManager.INSTANCE.findDeprecatedQueryTypeString(queryType);
					AbstractSmartQuery qq = QueryDatasetExtensionManager.getInstance().getDatasetHandler(queryType);
					columns = qq.getGeometryColumns(queryType, Locale.getDefault());

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
					
				} else {
					String geom = parseGeometryColumn(queryText
							.getTextContent());
					GeometryColumn gc = new GeometryColumn(geom, geom);
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

	private static String parseQueryType(String queryText) {
		String[] bits = queryText.split(":"); //$NON-NLS-1$
		if (bits[0].equals("ENTITY")) { //$NON-NLS-1$
			return "POINT"; //$NON-NLS-1$
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

	private static String parseGeometryColumn(String queryText)
			throws Exception {
		String[] bits = queryText.split(":"); //$NON-NLS-1$
		if (bits[0].equals("ENTITY")) { //$NON-NLS-1$
			return "entity:geometry"; //$NON-NLS-1$
		}
		if (bits[0].equals("SD_SU")) { //$NON-NLS-1$
			return "su:geometry"; //$NON-NLS-1$
		}
		// plans are multipoint with the name geometry
		// planpatrol is multiline with name track:geometry

		// deprecated query types
		String queryType = QueryTypeManager.INSTANCE.findDeprecatedQueryTypeString(bits[0]);

		AbstractSmartQuery qq = QueryDatasetExtensionManager.getInstance().getDatasetHandler(queryType);
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
