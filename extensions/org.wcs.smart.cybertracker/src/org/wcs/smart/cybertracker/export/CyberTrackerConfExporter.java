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
import java.util.HashMap;
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
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.export.PatrolScreensUtil.IdNamePair;
import org.wcs.smart.cybertracker.export.PatrolScreensUtil.ParolFilledDataContainer;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.reports.Items;
import org.wcs.smart.cybertracker.model.reports.Reports;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.cybertracker.model.screens.Screens;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerConfExporter {//extends CyberTrackerExporter {

	private static final String NODE_DEPTH_RESULT_PREFIX = "node"; //$NON-NLS-1$
	private static final String NODE_HEADER_COLOR = "0000FF00"; //$NON-NLS-1$
	
	private ScreensObjectFactory screensFactory;
	private CyberTrackerUtil ctUtil;
	
	private CyberTrackerId rootId;
	private Elements elements;
	private Map<Attribute, CyberTrackerId> attr2resultId = new HashMap<Attribute, CyberTrackerUtil.CyberTrackerId>();
	private Map<Integer, CyberTrackerId> nodeLevel2resultId = new HashMap<Integer, CyberTrackerUtil.CyberTrackerId>();

	private CyberTrackerId newWpResultId;
	private List<CyberTrackerId> newWpElementsIds;
	
	public File export(File destFolder, ConfigurableModel model, IProgressMonitor monitor) {
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			elements = ElementsUtil.buildEmptyElements();
			newWpResultId = new CyberTrackerId();
			ElementsUtil.addElementsItem(elements, PatrolScreensUtil.RESULT_NEW_WAYPOINT, newWpResultId.getItemId());
			newWpElementsIds = createNewWpElementsIds(elements);
			return performExport(destFolder, model, monitor, session);
		} finally {
			newWpResultId = null;
			newWpElementsIds = null;
			elements = null;
			rootId = null;
			attr2resultId.clear();
			nodeLevel2resultId.clear();
			session.getTransaction().rollback();
			session.close();
		}
	}

	private File performExport(File file, ConfigurableModel model, IProgressMonitor monitor, Session session) {
		monitor.subTask(Messages.CyberTrackerExporter_Progress_Fetch_Configuration);
		CyberTrackerProperties ctProperties = CyberTrackerHibernateManager.getProperties(session);
		screensFactory = new ScreensObjectFactory(ctProperties);
		ctUtil = new CyberTrackerUtil(screensFactory);
		monitor.subTask(Messages.CyberTrackerExporter_Progress_FetchDataModel);
		model = DataentryHibernateManager.getFullConfigurableModel(model.getUuid(), session);
		monitor.worked(10);
		
		monitor.subTask(Messages.CyberTrackerExporter_Progress_Build_Mappings);
		CmNode root = ctUtil.buildRoot(model);
		Map<CmNode, CyberTrackerId> keyMap = ctUtil.buildMap(root);

		monitor.subTask(Messages.CyberTrackerExporter_Progress_Build_Content);
		ParolFilledDataContainer patrolScreensData = ctUtil.buildPatrolNodes(elements, keyMap.get(root), session);
		if (patrolScreensData == null) {
			//failed to generate patrol data
			return null;
		}
		List<Node> screenNodes = new ArrayList<Node>();
		screenNodes.addAll(patrolScreensData.screenNodes);
		rootId = patrolScreensData.rootId;
		monitor.worked(5);

		screenNodes.addAll(buildCategoryNodes(root, keyMap, 0));
		monitor.worked(70);
		
		Screens screens = screensFactory.createScreens(screenNodes, ctProperties);
		monitor.worked(5);

		try {
			//----------------creating Screens.xml----------------
			monitor.subTask(Messages.CyberTrackerExporter_Progress_Generate_Screens);
			BufferedOutputStream outS = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()+"\\"+ICyberTrackerConstants.XML_SCREENS)); //$NON-NLS-1$
			try {
				writeDataModel(screens, outS, Screens.class);
			} finally {
				outS.close();
			}
			monitor.worked(10);
			
			//----------------creating Elements.xml----------------
			monitor.subTask(Messages.CyberTrackerExporter_Progress_Generate_Elements);
			ElementsUtil.addElements(elements, keyMap);
			BufferedOutputStream outE = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()+"\\"+ICyberTrackerConstants.XML_ELEMENTS)); //$NON-NLS-1$
			try {
				writeDataModel(elements, outE, Elements.class);
			} finally {
				outE.close();
			}
			
			//----------------creating Reports.xml----------------
			monitor.subTask(Messages.CyberTrackerExporter_Progress_Generate_Reports);
			List<Items.Item> columnItems = new ArrayList<Items.Item>();
			columnItems.add(ReportsObjectFactory.createColumnItem(ICyberTrackerConstants.DATE, Messages.CyberTrackerExporter_Report_Column_Date));
			columnItems.add(ReportsObjectFactory.createColumnItem(ICyberTrackerConstants.TIME, Messages.CyberTrackerExporter_Report_Column_Time));
			for (IdNamePair pair : patrolScreensData.resultElements) {
				columnItems.add(ReportsObjectFactory.createColumnItem(pair.id, pair.name));
			}
			for (Attribute attribute : attr2resultId.keySet()) {
				columnItems.add(ReportsObjectFactory.createColumnItem(attr2resultId.get(attribute).getItemId(), attribute.getName()));
			}
			for (Integer level : nodeLevel2resultId.keySet()) {
				columnItems.add(ReportsObjectFactory.createColumnItem(nodeLevel2resultId.get(level).getItemId(), NODE_DEPTH_RESULT_PREFIX+String.valueOf(level)));
			}
			columnItems.add(ReportsObjectFactory.createColumnItem(newWpResultId.getItemId(), PatrolScreensUtil.RESULT_NEW_WAYPOINT));
			Reports reports = ReportsObjectFactory.createReports(columnItems);
			BufferedOutputStream outR = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()+"\\"+ICyberTrackerConstants.XML_REPORTS)); //$NON-NLS-1$
			try {
				writeDataModel(reports, outR, Reports.class);
			} finally {
				outR.close();
			}
			
		} catch (Exception e) {
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.CyberTrackerExporter_Error_WriteXMmlFail, e);
			return null;
		}
		
		monitor.subTask(Messages.CyberTrackerExporter_Progress_GenerateCTX);
		try {
			String appPath = PdaUtil.getCTAppPath();
			String[] createCommands = {appPath, ICyberTrackerConstants.COMMAND_CREATE, file.getAbsolutePath(),file.getAbsolutePath()+"\\"+ICyberTrackerConstants.SMART_CTX_FILENEME}; //$NON-NLS-1$
			Process proc = Runtime.getRuntime().exec(createCommands);
			proc.waitFor();
		} catch (Exception e) {
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.CyberTrackerExporter_Error_GenerateCtxFail, e);
			return null;
		}

		return new File(file.getAbsolutePath()+"\\"+ICyberTrackerConstants.SMART_CTX_FILENEME); //$NON-NLS-1$
	}

	private List<Node> buildCategoryNodes(CmNode category, Map<CmNode, CyberTrackerId> keyMap, Integer level) {
		List<Node> result = new ArrayList<Node>();
		if (category == null)
			return result;
		
		if (category.getChildren() == null || category.getChildren().isEmpty()) {
			List<Node> attributeNodes = buildAttributeNodes(category, keyMap);
			if (!attributeNodes.isEmpty()) {
				result.addAll(attributeNodes);
			} else {
				//it appeared that category has not attributes to display -> show warning screen for that case
				result.add(createNoAttributeWarnNode(category, keyMap));
			}
			return result;
		}
		Node categoryNode = ctUtil.createRadioNode(category, keyMap, getNodeLevelResultElementId(level).getItemId());
		Control headerControl = ScreensObjectFactory.getHeaderControl(categoryNode);
		headerControl.setColor(NODE_HEADER_COLOR);
		result.add(categoryNode);
		
		Integer nextLevel = level + 1;
		for (CmNode child : category.getChildren()) {
			result.addAll(buildCategoryNodes(child, keyMap, nextLevel));
		}		
		return result;
	}
	
	private List<Node> buildAttributeNodes(CmNode category, Map<CmNode, CyberTrackerId> keyMap) {
		List<Attribute> attrList = new ArrayList<Attribute>();
		List<CyberTrackerId> boolRqAttrElementIDs = null;
		category.getCategory().getAllAttribute(attrList, true);
		List<Node> result = new ArrayList<Node>();
		CyberTrackerId startId = keyMap.get(category);
		CyberTrackerId id = startId;
		int attrListLastIndex = attrList.size() - 1;
		for (int i = 0; i <= attrListLastIndex; i++) {
			Attribute attribute = attrList.get(i);
			CyberTrackerId resultElementId = getAttributeResultElementId(attribute); //id for result element in attribute screen node
			switch (attribute.getType()) {
			case NUMERIC:
			{
				Node numberNode = screensFactory.createNodeNumber(id.getNodeId(), attribute.getName(), resultElementId.getItemId());
				if (attribute.getIsRequired()) {
					Control numControl = ScreensObjectFactory.getNumberMainControl(numberNode);
					numControl.setRequireSetValue(ICyberTrackerConstants.STR_TRUE);
				}
				result.add(numberNode);
				break;
			}
			case TEXT:
			{
				Node textNode = screensFactory.createNodeNote(id.getNodeId(), attribute.getName(), resultElementId.getItemId());
				Control textControl = ScreensObjectFactory.getNoteMainControl(textNode);
				if (attribute.getIsRequired()) {
					textControl.setRequired(ICyberTrackerConstants.STR_TRUE);
				}
				textControl.setMaxLength(ICyberTrackerConstants.MAX_TEXT_ATTRIBUTE_LENGTH);
				result.add(textNode);
				break;
			}
			case LIST:
			{
				if (attribute.getActiveListItems() == null || attribute.getActiveListItems().isEmpty()) {
					//skip invalid attribute (attribute without any possible value)
					continue;
				}
				List<String> itemNames = new ArrayList<String>();
				List<String> tag0Values = new ArrayList<String>();
				for (AttributeListItem listItem : attribute.getActiveListItems()) {
					itemNames.add(listItem.getName());
					tag0Values.add(SmartUtils.encodeHex(listItem.getUuid()));
				}
				List<CyberTrackerId> ids = ElementsUtil.addCustomElements(elements, itemNames, tag0Values);
				List<String> values = ctUtil.listItemIds(ids);
				String trElements = ctUtil.translateElements(ids);
				String trLinks = ctUtil.translateLinks(ids, false);
				Node node = screensFactory.createNodeRadio(id.getNodeId(), attribute.getName(), values, trElements, trLinks, resultElementId.getItemId());
				result.add(node);
				break;
			}
			case TREE:
			{
				if (attribute.getActiveTreeNodes() == null || attribute.getActiveTreeNodes().isEmpty()) {
					//skip invalid attribute (attribute without any possible value)
					continue;
				}
				String nodeId = id.getNodeId();
				id = new CyberTrackerId(); //this id will be used for next screen
				result.addAll(buildAttributeTreeNodes(attribute, nodeId, id, resultElementId.getItemId()));
				break;
			}
			case BOOLEAN:
			{
				if (boolRqAttrElementIDs == null) {
					boolRqAttrElementIDs = ElementsUtil.buildBooleanElements(elements);
				}
				result.add(ctUtil.createRadioNode(id.getNodeId(), attribute.getName(), boolRqAttrElementIDs, resultElementId.getItemId()));
				break;
			}
			default:
				throw new IllegalArgumentException("Unknown attribute type"); //$NON-NLS-1$
			}

			//tracking navigation for non-tree attributes (tree attributes are handle separately)
			if (!Attribute.AttributeType.TREE.equals(attribute.getType())) {
				//handle only cases for non-tree attributes, as all the have single ending screen
				id = new CyberTrackerId(); //this id will be used for next screen
				if (!result.isEmpty()) {
					Node lastNode = result.get(result.size()-1);
					Control control2 = ScreensObjectFactory.getNavigationControl(lastNode);
					//reference to "Next" screen
					control2.setTranslateNextScreenId(id.getNodeId());
					if (!attribute.getIsRequired()) {
						//skip button
						control2.setTranslateSkipScreenId(id.getNodeId());
					}
				}
			}
		}
		result.add(createLastNode(id, startId));
		return result;
	}
	
	private List<CyberTrackerId> createNewWpElementsIds(Elements elements2) {
		List<String> labelValues = new ArrayList<String>();
		labelValues.add(Messages.CyberTrackerExporter_Waypoint_SaveAsNew);
		labelValues.add(Messages.CyberTrackerExporter_Waypoint_AddToLast);
		List<String> tag0Values = new ArrayList<String>();
		tag0Values.add("true"); //$NON-NLS-1$
		tag0Values.add("false"); //$NON-NLS-1$
		return ElementsUtil.addCustomElements(elements, labelValues, tag0Values);
	}

	private CyberTrackerId getNodeLevelResultElementId(Integer level) {
		CyberTrackerId id = nodeLevel2resultId.get(level);
		if (id == null) {
			id = new CyberTrackerId();
			ElementsUtil.addElementsItem(elements, NODE_DEPTH_RESULT_PREFIX+String.valueOf(level), id.getItemId(), String.valueOf(level), ElementsUtil.CATEGORY_ELEMENT_TAG);
			nodeLevel2resultId.put(level, id);
		}
		return id;
	}

	private Node createNoAttributeWarnNode(CmNode category, Map<CmNode, CyberTrackerId> keyMap) {
		CyberTrackerId warnId = keyMap.get(category);
		Node warnNode = screensFactory.createNodeMsgText(warnId.getNodeId(), Messages.CyberTrackerExporter_NoAttributesNode_Title, Messages.CyberTrackerExporter_NoAttributesNode_Message);
		//disable next button, enable save button, navigate on save to root point
		Control control2 = ScreensObjectFactory.getNavigationControl(warnNode);
		control2.setShowNext("False"); //$NON-NLS-1$
		control2.setShowMajor("True"); //$NON-NLS-1$
		control2.setTranslateMajorScreenId(rootId.getNodeId());
		return warnNode;
	}
	
	//----------------------------exact copies below
	private void writeDataModel(Object obj, OutputStream file, Class<?> clazz) throws JAXBException, IOException {
		JAXBContext context = JAXBContext.newInstance(obj.getClass());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(obj, file);
	}

	private CyberTrackerId getAttributeResultElementId(Attribute attribute) {
		CyberTrackerId id = attr2resultId.get(attribute);
		if (id == null) {
			id = new CyberTrackerId();
			String uuid = SmartUtils.encodeHex(attribute.getUuid());
			ElementsUtil.addElementsItem(elements, attribute.getKeyId(), id.getItemId(), uuid, ElementsUtil.ATTRIBUTE_ELEMENT_TAG);
			attr2resultId.put(attribute, id);
		}
		return id;
	}

	/**
	 * Builds top level attribute radio node and calls for recursive child nodes creation.
	 * @param treeAttribute
	 * @param nodeId
	 * @return
	 */
	private List<Node> buildAttributeTreeNodes(Attribute treeAttribute, String nodeId, CyberTrackerId navId, String resultElementId) {
		List<Node> result = new ArrayList<Node>();
		List<AttributeTreeNode> activeTreeNodes = new ArrayList<AttributeTreeNode>();
		activeTreeNodes.addAll(treeAttribute.getActiveTreeNodes());
		
		Map<AttributeTreeNode, CyberTrackerId> map = ctUtil.buildTreeNodeMap(activeTreeNodes);
		List<CyberTrackerId> childIds = ctUtil.getChildrenIds(activeTreeNodes, map);
		Node treeRootNode = ctUtil.createRadioNode(nodeId, treeAttribute.getName(), childIds, null);
		if (!treeAttribute.getIsRequired()) {
			Control navControl = ScreensObjectFactory.getNavigationControl(treeRootNode);
			navControl.setTranslateSkipScreenId(navId.getNodeId());
		}
		result.add(treeRootNode);
		for (AttributeTreeNode treeNode : activeTreeNodes) {
			result.addAll(buildAttributeTreeNodes(treeNode, map, navId, resultElementId));
		}
		ElementsUtil.addElements(elements, map);
		return result;
	}
	
	private List<Node> buildAttributeTreeNodes(AttributeTreeNode treeNode, Map<AttributeTreeNode, CyberTrackerId> map, CyberTrackerId navId, String resultElementId) {
		List<Node> result = new ArrayList<Node>();
		if (treeNode == null)
			return result;
		
		if (treeNode.getActiveChildren() == null || treeNode.getActiveChildren().isEmpty()) {
			//if we are here that means that it was a screen with leaf and non-leaf elements above and treeNode is a leaf element
			//adding fake screen that contains only this element
			CyberTrackerId id = map.get(treeNode);
			List<CyberTrackerId> childIds = new ArrayList<CyberTrackerId>();
			childIds.add(id);
			Node node = ctUtil.createRadioNode(id.getNodeId(), treeNode.getName(), childIds, resultElementId);
			Control control2 = ScreensObjectFactory.getNavigationControl(node);
			control2.setTranslateNextScreenId(navId.getNodeId());
			result.add(node);
			return result;
		}

		boolean isEndScreen = true;
		//NOTE: there might be issues if at the save depth level leaf and non-leaf elements are present
		for (AttributeTreeNode child : treeNode.getActiveChildren()) {
			if (child.getActiveChildren() != null && !child.getActiveChildren().isEmpty()) {
				isEndScreen = false;
				break;
			}
		}		
		
		String id = map.get(treeNode).getNodeId();
		List<CyberTrackerId> childIds = ctUtil.getChildrenIds(treeNode.getActiveChildren(), map);
		if (isEndScreen) {
			Node node = ctUtil.createRadioNode(id, treeNode.getName(), childIds, resultElementId);
			Control control2 = ScreensObjectFactory.getNavigationControl(node);
			control2.setTranslateNextScreenId(navId.getNodeId());
			result.add(node);
			return result;
		}
		
		//this is NOT an end screen, proceed recursively till the end
		result.add(ctUtil.createRadioNode(id, treeNode.getName(), childIds, null));
		
		for (AttributeTreeNode child : treeNode.getActiveChildren()) {
			result.addAll(buildAttributeTreeNodes(child, map, navId, resultElementId));
		}		
		return result;
	}

	/**
	 * Creates last node where user can specify if he want to save observation as new waypoint or attach to previous
	 * 
	 * @param id
	 * @param startId
	 */
	private Node createLastNode(CyberTrackerId id, CyberTrackerId startId) {
		Node node = ctUtil.createRadioNode(id.getNodeId(), Messages.CyberTrackerExporter_Waypoint_ScreenTitle, newWpElementsIds, newWpResultId.getItemId());
		Control menoControl = screensFactory.createBottomMemoControl13(Messages.CyberTrackerExporter_SaveButtonsInfo);
		ScreensObjectFactory.addControlToNode(node, menoControl);
		
		Control control2 = ScreensObjectFactory.getNavigationControl(node);
		//this is the last screen and we need to show "Save" button
		control2.setShowMajor("True"); //$NON-NLS-1$
		control2.setShowMinor("True"); //$NON-NLS-1$
		control2.setShowNext("False"); //$NON-NLS-1$
		control2.setTranslateNextScreenId(null); //no next button at last screen
		control2.setTranslateMajorScreenId(rootId.getNodeId());
		control2.setTranslateMinorScreenId(startId.getNodeId());
		return node;
	}

	public int uploadPda(File file) throws Exception {
		String appPath = PdaUtil.getCTAppPath();
		String[] uploadCommands = {appPath, ICyberTrackerConstants.COMMAND_SILENT, ICyberTrackerConstants.COMMAND_UPLOAD, file.getAbsolutePath()};
		Process proc = Runtime.getRuntime().exec(uploadCommands);
		int code = proc.waitFor();
		return code;
	}
	
}
