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
package org.wcs.smart.cybertracker.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.cybertracker.model.screens.Screens;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Exporter to CyberTracker application
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerExporter {
	
	private static CyberTrackerId rootId;
	private static Elements elements;

	public static File export(File file, IProgressMonitor monitor) throws Exception {
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			elements = buildEmptyElements();
			return performExport(file, monitor, session);
		} finally {
			elements = null;
			rootId = null;
			session.getTransaction().rollback();
			session.close();
		}
	}
		
	private static File performExport(File file, IProgressMonitor monitor, Session session) throws Exception {
		DataModel dataModel = getDataModel(session);
		Category root = CyberTrackerUtil.buildRoot(dataModel);
		Map<Category, CyberTrackerId> keyMap = CyberTrackerUtil.buildMap(root);
		rootId = keyMap.get(root);
		
		List<Node> screenNodes = buildCategoryNodes(root, keyMap);
		Screens screens = ScreensObjectFactory.createScreens(screenNodes);
		BufferedOutputStream outS = new BufferedOutputStream(new FileOutputStream("c:/dev/CyberTracker/out/Screens.xml")); //$NON-NLS-1$
		try {
			writeDataModel(screens, outS, Screens.class);
		} finally {
			outS.close();
		}
		
//		Elements elements = buildEmptyElements();
		addElements(elements, keyMap);
		BufferedOutputStream outE = new BufferedOutputStream(new FileOutputStream("c:/dev/CyberTracker/out/Elements.xml")); //$NON-NLS-1$
		try {
			writeDataModel(elements, outE, Elements.class);
		} finally {
			outE.close();
		}
		
		return file;
	}

	private static Elements buildEmptyElements() {
		Elements elements = new Elements();
		Elements.List list = new Elements.List();
		elements.setList(list);
		Elements.List.Items items = new Elements.List.Items();
		list.setItems(items);
		return elements;
	}

	private static List<Node> buildCategoryNodes(Category category, Map<Category, CyberTrackerId> keyMap) {
		List<Node> result = new ArrayList<Node>();
		if (category == null)
			return result;
		//result.add(CyberTrackerUtil.createRadioNode(category, keyMap));
		
		if (category.getChildren() == null || category.getChildren().isEmpty()) {
			result.addAll(buildAttributeNodes(category, keyMap));
			return result;
		}
		result.add(CyberTrackerUtil.createRadioNode(category, keyMap));
		
		for (Category child : category.getChildren()) {
			result.addAll(buildCategoryNodes(child, keyMap));
		}		
		return result;
	}
	
	private static List<Node> buildAttributeNodes(Category category, Map<Category, CyberTrackerId> keyMap) {
		List<Attribute> attrList = new ArrayList<Attribute>();
		category.getAllAttribute(attrList, true);
		List<Node> result = new ArrayList<Node>();
		CyberTrackerId startId = keyMap.get(category);
		CyberTrackerId id = startId;
		for (Attribute attribute : attrList) {
			CyberTrackerId resultElementId = new CyberTrackerId(); //id for result element in attribute screen node
			switch (attribute.getType()) {
			case NUMERIC:
				result.add(ScreensObjectFactory.createNodeNumber(id.getNodeId(), attribute.getName(), resultElementId.getItemId()));
				break;
			case TEXT:
				result.add(ScreensObjectFactory.createNodeNote(id.getNodeId(), attribute.getName(), resultElementId.getItemId()));
				break;
			case LIST:
			{
				List<String> itemNames = new ArrayList<String>();
				for (AttributeListItem listItem : attribute.getActiveListItems()) {
					itemNames.add(listItem.getName());
				}
				List<CyberTrackerId> ids = addCustomElements(elements, itemNames.toArray(new String[itemNames.size()]));
				List<String> values = CyberTrackerUtil.listItemIds(ids);
				String trElements = CyberTrackerUtil.translateElements(ids);
				String trLinks = CyberTrackerUtil.translateLinks(ids, false);
				Node node = ScreensObjectFactory.createNodeRadio(id.getNodeId(), attribute.getName(), values, trElements, trLinks, resultElementId.getItemId());
				result.add(node);
				break;
			}
			case TREE:
				result.addAll(buildAttributeTreeNodes(attribute, id.getNodeId()));
				break;
			case BOOLEAN:
			{
				List<CyberTrackerId> ids = addCustomElements(elements, "Yes", "No", "Undefined");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				List<String> values = CyberTrackerUtil.listItemIds(ids);
				String trElements = CyberTrackerUtil.translateElements(ids);
				String trLinks = CyberTrackerUtil.translateLinks(ids, false);
				Node node = ScreensObjectFactory.createNodeRadio(id.getNodeId(), attribute.getName(), values, trElements, trLinks, resultElementId.getItemId());
				result.add(node);
				break;
			}
			default:
				throw new IllegalArgumentException("Unknown attribute type"); //$NON-NLS-1$
			}
			
			id = new CyberTrackerId(); //this id will be used for next screen
			if (attribute.getType() != Attribute.AttributeType.TREE && !result.isEmpty()) {
				Node lastNode = result.get(result.size()-1);
				Control control2 = lastNode.getData().getControls().getControl().get(0);
				control2.setTranslateNextScreenId(id.getNodeId());
			}
			addElementsItem(elements, "#"+attribute.getName(), resultElementId.getItemId()); //$NON-NLS-1$
		}
		if (result.size() > 0) { //TODO: if last attribute is tree attribute this code is not correct
			Node lastNode = result.get(result.size()-1);
			Control control2 = lastNode.getData().getControls().getControl().get(0);
			control2.setShowMajor("True"); //$NON-NLS-1$
			control2.setShowMinor("True"); //$NON-NLS-1$
			control2.setShowNext("False"); //$NON-NLS-1$
			control2.setTranslateNextScreenId(null); //no next button at last screen
			control2.setTranslateMajorScreenId(rootId.getNodeId());
			control2.setTranslateMinorScreenId(startId.getNodeId());
		}
		return result;
	}

	/**
	 * Builds top level attribute radio node and calls for recursive child nodes creation.
	 * @param treeAttribute
	 * @param nodeId
	 * @return
	 */
	private static List<Node> buildAttributeTreeNodes(Attribute treeAttribute, String nodeId) {
		List<Node> result = new ArrayList<Node>();
		List<AttributeTreeNode> activeTreeNodes = treeAttribute.getActiveTreeNodes();
		
		Map<AttributeTreeNode, CyberTrackerId> map = CyberTrackerUtil.buildTreeNodeMap(activeTreeNodes);
		List<CyberTrackerId> childIds = CyberTrackerUtil.getChildrenIds(activeTreeNodes, map);
		result.add(CyberTrackerUtil.createRadioNode(nodeId, treeAttribute.getName(), childIds));
		for (AttributeTreeNode treeNode : activeTreeNodes) {
			result.addAll(buildAttributeTreeNodes(treeNode, map));
		}
		addElements(elements, map);
		return result;
	}
	
	private static List<Node> buildAttributeTreeNodes(AttributeTreeNode treeNode, Map<AttributeTreeNode, CyberTrackerId> map) {
		List<Node> result = new ArrayList<Node>();
		if (treeNode == null)
			return result;
		
//		if (treeNode.getChildren() == null || treeNode.getChildren().isEmpty()) {
//			result.addAll(buildAttributeNodes(category, keyMap));
//			return result;
//		}
//		result.add(CyberTrackerUtil.createRadioNode(category, keyMap));

		String id = map.get(treeNode).getNodeId();
		List<CyberTrackerId> childIds = CyberTrackerUtil.getChildrenIds(treeNode.getActiveChildren(), map);
		result.add(CyberTrackerUtil.createRadioNode(id, treeNode.getName(), childIds));
		
		for (AttributeTreeNode child : treeNode.getActiveChildren()) {
			result.addAll(buildAttributeTreeNodes(child, map));
		}		
		return result;
	}
	
	/**
	 * Simply add all from the map to elements
	 * @param elements
	 * @param map
	 */
	private static void addElements(Elements elements, Map<? extends DmObject, CyberTrackerId> map) {
		Set<? extends DmObject> keys = map.keySet();
		for (DmObject dmObject : keys) {
			addElementsItem(elements, dmObject.getName(), map.get(dmObject).getItemId());
		}
	}
	
	/**
	 * For given labels function:
	 *  - creates items
	 *	- adds them to elements
	 *  - returns the list of item ids
	 * @param elements
	 * @return
	 */
	private static List<CyberTrackerId> addCustomElements(Elements elements, String... labels) {
		List<CyberTrackerId> idList = new ArrayList<CyberTrackerId>();
		for (String string : labels) {
			CyberTrackerId id = new CyberTrackerId();
			addElementsItem(elements, string, id.getItemId());
			idList.add(id);
		}
		return idList;
	}
	
	private static void addElementsItem(Elements elements, String name, String id) {
		Elements.List.Items.Item item = new Elements.List.Items.Item();
		item.setName(name);
		item.setId(id);
		elements.getList().getItems().getItem().add(item);
	}
	
	private static void writeDataModel(Object obj, OutputStream file, Class<?> clazz) throws JAXBException, IOException {
		JAXBContext context = JAXBContext.newInstance(clazz);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(obj, file);
	}

	private static DataModel getDataModel(Session session) {
		DataModel dataModel = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
		//load into memory; no-lazy loading here.
		for (Category cat: dataModel.getCategories()){
			visitCategory(cat);
		}
		for (Attribute att: dataModel.getAttributes()){
			att.getAggregations().size();
		}
		return dataModel;
	}
	
	private static void visitCategory(Category cat){
		for (Category child : cat.getActiveChildren()){
			visitCategory(child);
			child.getName();
		}
		for (CategoryAttribute ca: cat.getAttributes()){
			ca.getAttribute().getName();
		}	
	}	
}
