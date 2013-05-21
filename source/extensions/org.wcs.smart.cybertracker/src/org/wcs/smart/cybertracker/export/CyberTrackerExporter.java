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
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.reports.Items;
import org.wcs.smart.cybertracker.model.reports.Reports;
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
			elements = ElementsUtil.buildEmptyElements();
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
		monitor.worked(10);
		
		Category root = CyberTrackerUtil.buildRoot(dataModel);
		Map<Category, CyberTrackerId> keyMap = CyberTrackerUtil.buildMap(root);
		List<Node> screenNodes = new ArrayList<Node>();
		rootId = PatrolScreensUtil.addPatrolNodes(screenNodes, elements, keyMap.get(root), session);
		monitor.worked(5);

		screenNodes.addAll(buildCategoryNodes(root, keyMap));
		monitor.worked(70);
		
		Screens screens = ScreensObjectFactory.createScreens(screenNodes);
		monitor.worked(5);

		BufferedOutputStream outS = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()+"\\Screens.xml")); //$NON-NLS-1$
		try {
			writeDataModel(screens, outS, Screens.class);
		} finally {
			outS.close();
		}
		monitor.worked(10);
		
		ElementsUtil.addElements(elements, keyMap);
		BufferedOutputStream outE = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()+"\\Elements.xml")); //$NON-NLS-1$
		try {
			writeDataModel(elements, outE, Elements.class);
		} finally {
			outE.close();
		}
		
		monitor.done();
		return file;
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
//		for (Attribute attribute : attrList) {
		int attrListLastIndex = attrList.size() - 1;
		for (int i = 0; i <= attrListLastIndex; i++) {
			Attribute attribute = attrList.get(i);
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
				List<CyberTrackerId> ids = ElementsUtil.addCustomElements(elements, itemNames.toArray(new String[itemNames.size()]));
				List<String> values = CyberTrackerUtil.listItemIds(ids);
				String trElements = CyberTrackerUtil.translateElements(ids);
				String trLinks = CyberTrackerUtil.translateLinks(ids, false);
				Node node = ScreensObjectFactory.createNodeRadio(id.getNodeId(), attribute.getName(), values, trElements, trLinks, resultElementId.getItemId());
				result.add(node);
				break;
			}
			case TREE:
			{
				//TODO: test without "Species"
//				if (attribute.getName().equals("Species"))
//					break;
				//NOTE: This is a special case as we might have multiple ending screens!!!
				String nodeId = id.getNodeId();
				id = new CyberTrackerId(); //this id will be used for next screen
				boolean hasNext = i != attrListLastIndex;
				CyberTrackerId navId = hasNext ? id : startId;

				result.addAll(buildAttributeTreeNodes(attribute, nodeId, navId, resultElementId.getItemId(), hasNext));
				break;
			}
			case BOOLEAN:
			{
				List<CyberTrackerId> ids = ElementsUtil.addCustomElements(elements, "Yes", "No", "Undefined");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				result.add(CyberTrackerUtil.createRadioNode(id.getNodeId(), attribute.getName(), ids, resultElementId.getItemId()));
				break;
			}
			default:
				throw new IllegalArgumentException("Unknown attribute type"); //$NON-NLS-1$
			}

			ElementsUtil.addElementsItem(elements, "#"+attribute.getName(), resultElementId.getItemId()); //$NON-NLS-1$
			//tracking navigation for non-tree attributes (tree attributes are handle separately)
			if (!Attribute.AttributeType.TREE.equals(attribute.getType())) {
				//handle only cases for non-tree attributes, as all the have single ending screen
				id = new CyberTrackerId(); //this id will be used for next screen
				if (!result.isEmpty()) {
					Node lastNode = result.get(result.size()-1);
					boolean hasNext = i != attrListLastIndex;
					CyberTrackerId navId = hasNext ? id : startId;
					buildNodeNavigation(lastNode, navId, hasNext);
				}
			}
		}
		return result;
	}

	/**
	 * Adds "Next" or "Save" button to screen depending on input parameters
	 * 
	 * @param node
	 * @param navigateId
	 * @param hasNext
	 */
	private static void buildNodeNavigation(Node node, CyberTrackerId navigateId, boolean hasNext) {
		Control control2 = ScreensObjectFactory.getNavigationControl(node);
		if (hasNext) {
			//we have some screens after this, so displaying "Next" button
			control2.setTranslateNextScreenId(navigateId.getNodeId());
		} else {
			//this is the last screen and we need to show "Save" button
			control2.setShowMajor("True"); //$NON-NLS-1$
			control2.setShowMinor("True"); //$NON-NLS-1$
			control2.setShowNext("False"); //$NON-NLS-1$
			control2.setTranslateNextScreenId(null); //no next button at last screen
			control2.setTranslateMajorScreenId(rootId.getNodeId());
			control2.setTranslateMinorScreenId(navigateId.getNodeId());
		}
	}
	
	/**
	 * Builds top level attribute radio node and calls for recursive child nodes creation.
	 * @param treeAttribute
	 * @param nodeId
	 * @return
	 */
	private static List<Node> buildAttributeTreeNodes(Attribute treeAttribute, String nodeId, CyberTrackerId navId, String resultElementId, boolean hasNext) {
		List<Node> result = new ArrayList<Node>();
		List<AttributeTreeNode> activeTreeNodes = treeAttribute.getActiveTreeNodes();
		
		Map<AttributeTreeNode, CyberTrackerId> map = CyberTrackerUtil.buildTreeNodeMap(activeTreeNodes);
		List<CyberTrackerId> childIds = CyberTrackerUtil.getChildrenIds(activeTreeNodes, map);
		result.add(CyberTrackerUtil.createRadioNode(nodeId, treeAttribute.getName(), childIds, null));
		for (AttributeTreeNode treeNode : activeTreeNodes) {
			result.addAll(buildAttributeTreeNodes(treeNode, map, navId, resultElementId, hasNext));
		}
		ElementsUtil.addElements(elements, map);
		return result;
	}
	
	private static List<Node> buildAttributeTreeNodes(AttributeTreeNode treeNode, Map<AttributeTreeNode, CyberTrackerId> map, CyberTrackerId navId, String resultElementId, boolean hasNext) {
		List<Node> result = new ArrayList<Node>();
		if (treeNode == null)
			return result;
		
		if (treeNode.getChildren() == null || treeNode.getChildren().isEmpty()) {
			//if we are here that means that it was a screen with leaf and non-leaf elements above and treeNode is a leaf element
			//adding fake screen that contains only this element
			CyberTrackerId id = map.get(treeNode);
			List<CyberTrackerId> childIds = new ArrayList<CyberTrackerId>();
			childIds.add(id);
			Node node = CyberTrackerUtil.createRadioNode(id.getNodeId(), treeNode.getName(), childIds, resultElementId);
			buildNodeNavigation(node, navId, hasNext);
			result.add(node);
			return result;
		}

		boolean isEndScreen = true;
		//NOTE: there might be issues if at the save depth level leaf and non-leaf elements are present
		for (AttributeTreeNode child : treeNode.getActiveChildren()) {
			if (child.getChildren() != null && !child.getChildren().isEmpty()) {
				isEndScreen = false;
				break;
			}
		}		
		
		String id = map.get(treeNode).getNodeId();
		List<CyberTrackerId> childIds = CyberTrackerUtil.getChildrenIds(treeNode.getActiveChildren(), map);
		if (isEndScreen) {
			Node node = CyberTrackerUtil.createRadioNode(id, treeNode.getName(), childIds, resultElementId);
			buildNodeNavigation(node, navId, hasNext);
			result.add(node);
			return result;
		}
		
		//this is NOT an end screen, proceed recursively till the end
		result.add(CyberTrackerUtil.createRadioNode(id, treeNode.getName(), childIds, null));
		
		for (AttributeTreeNode child : treeNode.getActiveChildren()) {
			result.addAll(buildAttributeTreeNodes(child, map, navId, resultElementId, hasNext));
		}		
		return result;
	}
	
	private static void writeDataModel(Object obj, OutputStream file, Class<?> clazz) throws JAXBException, IOException {
		JAXBContext context = JAXBContext.newInstance(obj.getClass());
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
