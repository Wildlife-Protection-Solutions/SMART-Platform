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
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
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
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.cybertracker.model.screens.Screens;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * Exporter from {@link ConfigurableModel} to CyberTracker application
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class CyberTrackerConfExporter {

	private static final String NODE_DEPTH_RESULT_PREFIX = "node"; //$NON-NLS-1$
	private static final String NODE_HEADER_COLOR = "0000FF00"; //$NON-NLS-1$
	
	private ScreensObjectFactory screensFactory;
	private CyberTrackerUtil ctUtil;
	
	private CyberTrackerId rootId;
	private Elements elements;
	private Map<Attribute, Map<Integer, CyberTrackerId>> attr2resultId = new HashMap<Attribute, Map<Integer, CyberTrackerId>>();
	private Map<Integer, CyberTrackerId> nodeLevel2resultId = new HashMap<Integer, CyberTrackerId>();

	private CyberTrackerId newWpResultId;
	private List<CyberTrackerId> newWpElementsIds;
	
	private CyberTrackerId defaultAttrValuesResultId;

	public File export(File destFolder, ConfigurableModel model, IProgressMonitor monitor) {
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			elements = ElementsUtil.buildEmptyElements();
			newWpResultId = new CyberTrackerId();
			ElementsUtil.addElementsItem(elements, PatrolScreensUtil.RESULT_NEW_WAYPOINT, newWpResultId.getItemId());
			newWpElementsIds = createNewWpElementsIds(elements);
			defaultAttrValuesResultId = new CyberTrackerId();
			ElementsUtil.addElementsItem(elements, PatrolScreensUtil.RESULT_DEFAULT_ATTRIBUTE_VALUES, defaultAttrValuesResultId.getItemId(), null, ElementsUtil.DEFAULT_VALUES_ELEMENT_TAG);
			return performExport(destFolder, model, monitor, session);
		} finally {
			defaultAttrValuesResultId = null;
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
			ElementsUtil.addNodeElements(elements, keyMap);
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
				Map<Integer, CyberTrackerId> map = attr2resultId.get(attribute);
				for (Integer i : map.keySet()) {
					columnItems.add(ReportsObjectFactory.createColumnItem(map.get(i).getItemId(), attribute.getName() + "#" + i)); //$NON-NLS-1$
				}
			}
			columnItems.add(ReportsObjectFactory.createColumnItem(defaultAttrValuesResultId.getItemId(), PatrolScreensUtil.RESULT_DEFAULT_ATTRIBUTE_VALUES));
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

	private List<Node> buildCategoryNodes(CmNode node, Map<CmNode, CyberTrackerId> keyMap, Integer level) {
		List<Node> result = new ArrayList<Node>();
		if (node == null)
			return result;
		
		if (node.getChildren() == null || node.getChildren().isEmpty()) {
			List<Node> attributeNodes = buildAttributeNodes(node, keyMap);
			if (!attributeNodes.isEmpty()) {
				result.addAll(attributeNodes);
			} else {
				//it appeared that category has not attributes to display -> show warning screen for that case
				result.add(createNoAttributeWarnNode(node, keyMap));
			}
			return result;
		}
		Node categoryNode = ctUtil.createRadioNode(node, keyMap, getNodeLevelResultElementId(level).getItemId());
		Control headerControl = ScreensObjectFactory.getHeaderControl(categoryNode);
		headerControl.setColor(NODE_HEADER_COLOR);
		result.add(categoryNode);
		
		Integer nextLevel = level + 1;
		for (CmNode child : node.getChildren()) {
			result.addAll(buildCategoryNodes(child, keyMap, nextLevel));
		}		
		return result;
	}

	private List<Node> buildAttributeNodes(CmNode cmNode, Map<CmNode, CyberTrackerId> keyMap) {
		List<Node> result = new ArrayList<Node>();
		if (cmNode.isGroup())
			return result;
		
		List<CmAttribute> fullList = cmNode.getCmAttributes();
		CyberTrackerId startId = keyMap.get(cmNode);
		List<CmAttribute> toShow = new ArrayList<CmAttribute>();
		List<CmAttribute> invisibleList = new ArrayList<CmAttribute>();
		split(fullList, toShow, invisibleList);
		
		if (!toShow.isEmpty()) {
			//check fir multiselect list
			CmAttribute cmAttr = toShow.get(0);
			Attribute attribute = cmAttr.getAttribute();
			if (AttributeType.LIST.equals(attribute.getType())) {
				if (cmAttr.isMultiselect() && cmAttr.isVisible()) {
					CmAttribute numAttr = toShow.size() > 1 && toShow.get(1).isNumeric() ? toShow.get(1) : null;
					List<CyberTrackerId> multiIds = addListElements(cmAttr, true, numAttr); 
					Node mNode = buildMultiSelectNode(cmAttr, startId, multiIds, numAttr != null);
					Control control2 = ScreensObjectFactory.getNavigationControl(mNode);
					CyberTrackerId nextId = new CyberTrackerId();
					//reference to "Next" screen
					control2.setTranslateNextScreenId(nextId.getNodeId());
					result.add(mNode);
					if (numAttr != null)
						toShow.remove(1);
					toShow.remove(0); //as we just added a node for it
					List<AttributeListItem> activeItems = attribute.getActiveListItems();
					for (int i = 0; i < multiIds.size(); i++) {
						result.addAll(buildBasicAttributeNodes(toShow, keyMap, multiIds.get(i), i, false, " ("+activeItems.get(i).getName()+")", null)); //$NON-NLS-1$ //$NON-NLS-2$
					}
					result.add(createLastNode(nextId, startId, recordDefaultValues(invisibleList)));
					return result;
				}
			}
			return buildBasicAttributeNodes(toShow, keyMap, startId, 0, true, null, recordDefaultValues(invisibleList));
		}
		return result;
		
	}

	private List<CyberTrackerId> addListElements(CmAttribute cmAttr) {
		return addListElements(cmAttr, false, null);
	}

	private List<CyberTrackerId> addListElements(CmAttribute listAttr, boolean isMulti, CmAttribute numAttr) {
		List<CyberTrackerId> ids = new ArrayList<CyberTrackerId>();
		Attribute attribute = listAttr.getAttribute();
		if (!AttributeType.LIST.equals(attribute.getType())) {
			throw new IllegalArgumentException("This operation can be performed only on lists"); //$NON-NLS-1$
		}
		
		List<AttributeListItem> activeItems = attribute.getActiveListItems();
		if (activeItems == null || activeItems.isEmpty()) {
			//development validation: this MUST NEVER happen as it is tracked by split(...) logic!!!
			throw new IllegalArgumentException("Cannot add a screen without any items to display"); //$NON-NLS-1$
		}
		List<String> itemNames = new ArrayList<String>();
		List<String> tag0Values = new ArrayList<String>();
		for (AttributeListItem listItem : activeItems) {
			itemNames.add(listItem.getName());
			tag0Values.add(SmartUtils.encodeHex(listItem.getUuid()));
			
		}
		//tag0 - listAttrValue uuid
		//tag1 - identifier
		//tag2 - order
		//tag3 - reference to listAttr in Elements.xml
		//tag4 - reference to numAttr in Elements.xml

		String tag1 = isMulti ? ElementsUtil.MULISELECT_ELEMENT_TAG : null;
		String tag3 = isMulti ? getAttributeResultElementId(attribute, 0).getItemId() : null;
		String tag4 = numAttr != null ? getAttributeResultElementId(numAttr.getAttribute(), 0).getItemId() : null;
		for (int i = 0; i < activeItems.size(); i++) {
			AttributeListItem listItem = activeItems.get(i);
			String name = listItem.getName();
			String tag0 = SmartUtils.encodeHex(listItem.getUuid());
			String tag2 = isMulti ? String.valueOf(i) : null;
			CyberTrackerId id = new CyberTrackerId();
			ElementsUtil. addElementsItem(elements, name, id.getItemId(), tag0, tag1, tag2, tag3, tag4);
			ids.add(id);
		}
		return ids;
		
	}
	
	private Node buildMultiSelectNode(CmAttribute cmAttr, CyberTrackerId id /*=startId*/, List<CyberTrackerId> childIds, boolean withNumbers) {
		if (childIds == null || childIds.isEmpty())
			return null;
		List<String> values = ctUtil.listItemIds(childIds);
		String trElements = ctUtil.translateElements(childIds);
		String trLinks = ctUtil.translateLinks(childIds, true);
		Node node = screensFactory.createNodeMultiList(id.getNodeId(), cmAttr.getName(), values, trElements, trLinks, 1, withNumbers);
		//NOTE: next screen is not set here
		return node;
	}

	private String recordDefaultValues(List<CmAttribute> invisibleList) {
		String defaultValues = ""; //$NON-NLS-1$
		for (int i = 0; i < invisibleList.size(); i++) {
			CmAttribute cmAttr = invisibleList.get(i);
			if (cmAttr.isVisible()) {
				//should NEVER happen
				throw new IllegalArgumentException("Arguments passed to this method MUST be invisible"); //$NON-NLS-1$
			}
			//this attributes are configured as invisible
			//record "default value" data
			String newData = recordDefaultValue(cmAttr);
			if (newData != null && !newData.isEmpty()) {
				if (!defaultValues.isEmpty())
					defaultValues += ICyberTrackerConstants.ATTRIBUTE_DEFAULT_VALUES_SEPATATOR;
				defaultValues += newData;
			}
		}
		return defaultValues;
	}
	
	private List<Node> buildBasicAttributeNodes(List<CmAttribute> attrList, Map<CmNode, CyberTrackerId> keyMap, CyberTrackerId startId, int index, boolean terminare, String label, String defaultValues) {
		List<Node> result = new ArrayList<Node>();
		if (label == null)
			label = ""; //$NON-NLS-1$
		
		List<CyberTrackerId> boolRqAttrElementIDs = null;
		CyberTrackerId id = startId;
		for (int i = 0; i < attrList.size(); i++) {
			CmAttribute cmAttr = attrList.get(i);
			boolean linkToNext = terminare || i < attrList.size() - 1;
			if (!cmAttr.isVisible()) {
				//should NEVER happen
				//development validation! remove throw block after testing
				throw new IllegalArgumentException("Arguments passed to this method MUST be visible"); //$NON-NLS-1$
			}
			Attribute attribute = cmAttr.getAttribute();
			CyberTrackerId resultElementId = getAttributeResultElementId(attribute, index); //id for result element in attribute screen node
			switch (attribute.getType()) {
			case NUMERIC:
			{
				Node numberNode = screensFactory.createNodeNumber(id.getNodeId(), attribute.getName() + label, resultElementId.getItemId());
				if (attribute.getIsRequired()) {
					Control numControl = ScreensObjectFactory.getNumberMainControl(numberNode);
					numControl.setRequireSetValue(ICyberTrackerConstants.STR_TRUE);
				}
				result.add(numberNode);
				break;
			}
			case TEXT:
			{
				Node textNode = screensFactory.createNodeNote(id.getNodeId(), attribute.getName() + label, resultElementId.getItemId());
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
				List<CyberTrackerId> ids = addListElements(cmAttr);
				List<String> values = ctUtil.listItemIds(ids);
				String trElements = ctUtil.translateElements(ids);
				String trLinks = ctUtil.translateLinks(ids, false);
				Node node = screensFactory.createNodeRadio(id.getNodeId(), attribute.getName() + label, values, trElements, trLinks, resultElementId.getItemId());
				result.add(node);
				break;
			}
			case TREE:
			{
				if (cmAttr.isFlattenTree()) {
					List<CyberTrackerId> ids = addFinalTreeNodes(cmAttr);
					List<String> values = ctUtil.listItemIds(ids);
					String trElements = ctUtil.translateElements(ids);
					String trLinks = ctUtil.translateLinks(ids, false);
					Node node = screensFactory.createNodeRadio(id.getNodeId(), attribute.getName() + label, values, trElements, trLinks, resultElementId.getItemId());
					result.add(node);
				} else {
					String nodeId = id.getNodeId();
					id = linkToNext ? new CyberTrackerId() : null; //this id will be used for next screen
					result.addAll(buildAttributeTreeNodes(attribute, nodeId, id, resultElementId.getItemId(), label));
					linkToNext = false; //for this case we track linking separately, we don't want any linking logic to be executed further for this attribute
				}
				break;
			}
			case BOOLEAN:
			{
				if (boolRqAttrElementIDs == null) {
					boolRqAttrElementIDs = ElementsUtil.buildBooleanElements(elements);
				}
				result.add(ctUtil.createRadioNode(id.getNodeId(), attribute.getName() + label, boolRqAttrElementIDs, resultElementId.getItemId()));
				break;
			}
			default:
				throw new IllegalArgumentException("Unknown attribute type"); //$NON-NLS-1$
			}

			//tracking navigation for non-tree or float trees attributes (tree attributes are handle separately)
			if (linkToNext) {
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
		if (terminare) {
			result.add(createLastNode(id, startId, defaultValues));
		}
		return result;
	}

	private List<CyberTrackerId> addFinalTreeNodes(CmAttribute cmAttr) {
		Attribute attribute = cmAttr.getAttribute();
		List<AttributeTreeNode> activeTreeNodes = attribute.getActiveTreeNodes();
		if (activeTreeNodes == null || activeTreeNodes.isEmpty()) {
			//development validation: this MUST NEVER happen as it is tracked by split(...) logic!!!
			throw new IllegalArgumentException("Cannot add a flat tree screen without any items to display"); //$NON-NLS-1$
		}
		List<AttributeTreeNode> finalTreeNodes = listFinalTreeNodes(activeTreeNodes);
		
		List<CyberTrackerId> ids = new ArrayList<CyberTrackerId>();
		for (AttributeTreeNode treeNode : finalTreeNodes) {
			String name = treeNode.getName();
			String tag0 = SmartUtils.encodeHex(treeNode.getUuid());
			CyberTrackerId id = new CyberTrackerId();
			ElementsUtil. addElementsItem(elements, name, id.getItemId(), tag0);
			ids.add(id);
		}
		return ids;
	}

	private List<AttributeTreeNode> listFinalTreeNodes(List<AttributeTreeNode> activeTreeNodes) {
		List<AttributeTreeNode> result = new ArrayList<AttributeTreeNode>();
		for (AttributeTreeNode treeNode : activeTreeNodes) {
			List<AttributeTreeNode> activeChildren = treeNode.getActiveChildren();
			if (activeChildren == null || activeChildren.isEmpty()) {
				result.add(treeNode);
			} else {
				result.addAll(listFinalTreeNodes(activeChildren));
			}
		}
		return result;
	}
	
	private String recordDefaultValue(CmAttribute cmAttr) {
		//tag0 - key (attribute uuid); tag1 - value (default value for this attribute in given observation)
		Map<String, CmAttributeOption> options = cmAttr.getCmAttributeOptions();
		CmAttributeOption defaultValueOption = options.get(CmAttributeOption.ID_DEFAULT_VALUE);
		if (defaultValueOption == null)
			return null;

		Attribute attribute = cmAttr.getAttribute();
		switch (attribute.getType()) {
		case NUMERIC:
			if (defaultValueOption.getDoubleValue() != null) 
				return recordDefaultValue(attribute, defaultValueOption.getDoubleValue().toString());
			break;
		case TEXT:
		{
			String strValue = defaultValueOption.getStringValue();
			if (strValue != null && !strValue.isEmpty())
				return recordDefaultValue(attribute, strValue);
			break;
		}
		case LIST:
		case TREE:
			if (defaultValueOption.getUuidValue() != null)
				return recordDefaultValue(attribute, SmartUtils.encodeHex(defaultValueOption.getUuidValue()));
			break;
		case BOOLEAN:
			if (defaultValueOption.getBooleanValue() != null)
				return recordDefaultValue(attribute, defaultValueOption.getBooleanValue().toString());
			break;
		}
		return null;
	}

	private String recordDefaultValue(Attribute attribute, String defaultValue) {
		//tag0 - key (attribute uuid); tag1 - value (default value for this attribute in given observation)
		String ctid = (new CyberTrackerId()).getItemId();
		ElementsUtil.addElementsItem(elements, attribute.getName(), ctid, SmartUtils.encodeHex(attribute.getUuid()), null, defaultValue);
		return ctid;
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

	private Node createNoAttributeWarnNode(CmNode node, Map<CmNode, CyberTrackerId> keyMap) {
		CyberTrackerId warnId = keyMap.get(node);
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

	/**
	 * @param attribute
	 * @param index - for multiselect list it is possible to have same attribute several times,
	 * this is why we need several result ids to handle that case; index show for which entry in
	 * multiselect list this attribute belong 
	 * @return
	 */
	private CyberTrackerId getAttributeResultElementId(Attribute attribute, Integer index) {
		Map<Integer, CyberTrackerId> map = attr2resultId.get(attribute);
		if (map == null) {
			map = new HashMap<Integer, CyberTrackerId>();
			attr2resultId.put(attribute, map);
		}
		CyberTrackerId id = map.get(index);
		if (id == null) {
			id = new CyberTrackerId();
			String uuid = SmartUtils.encodeHex(attribute.getUuid());
			ElementsUtil.addElementsItem(elements, attribute.getKeyId()+"#"+index, id.getItemId(), uuid, ElementsUtil.ATTRIBUTE_ELEMENT_TAG, index.toString()); //$NON-NLS-1$
			map.put(index, id);
		}
		return id;
	}

	/**
	 * Builds top level attribute radio node and calls for recursive child nodes creation.
	 * @param treeAttribute
	 * @param nodeId
	 * @return
	 */
	private List<Node> buildAttributeTreeNodes(Attribute treeAttribute, String nodeId, CyberTrackerId navId, String resultElementId, String label) {
		List<Node> result = new ArrayList<Node>();
		List<AttributeTreeNode> activeTreeNodes = new ArrayList<AttributeTreeNode>();
		activeTreeNodes.addAll(treeAttribute.getActiveTreeNodes());
		
		Map<AttributeTreeNode, CyberTrackerId> map = ctUtil.buildTreeNodeMap(activeTreeNodes);
		List<CyberTrackerId> childIds = ctUtil.getChildrenIds(activeTreeNodes, map);
		Node treeRootNode = ctUtil.createRadioNode(nodeId, treeAttribute.getName() + label, childIds, null);
		if (!treeAttribute.getIsRequired() && navId != null) {
			Control navControl = ScreensObjectFactory.getNavigationControl(treeRootNode);
			navControl.setTranslateSkipScreenId(navId.getNodeId());
		}
		result.add(treeRootNode);
		for (AttributeTreeNode treeNode : activeTreeNodes) {
			result.addAll(buildAttributeTreeNodes(treeNode, map, navId, resultElementId, label));
		}
		ElementsUtil.addElements(elements, map);
		return result;
	}
	
	private List<Node> buildAttributeTreeNodes(AttributeTreeNode treeNode, Map<AttributeTreeNode, CyberTrackerId> map, CyberTrackerId navId, String resultElementId, String label) {
		List<Node> result = new ArrayList<Node>();
		if (treeNode == null)
			return result;
		
		if (treeNode.getActiveChildren() == null || treeNode.getActiveChildren().isEmpty()) {
			//if we are here that means that it was a screen with leaf and non-leaf elements above and treeNode is a leaf element
			//adding fake screen that contains only this element
			CyberTrackerId id = map.get(treeNode);
			List<CyberTrackerId> childIds = new ArrayList<CyberTrackerId>();
			childIds.add(id);
			Node node = ctUtil.createRadioNode(id.getNodeId(), treeNode.getName() + label, childIds, resultElementId);
			if (navId != null) {
				Control control2 = ScreensObjectFactory.getNavigationControl(node);
				control2.setTranslateNextScreenId(navId.getNodeId());
			}
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
			Node node = ctUtil.createRadioNode(id, treeNode.getName() + label, childIds, resultElementId);
			if (navId != null) {
				Control control2 = ScreensObjectFactory.getNavigationControl(node);
				control2.setTranslateNextScreenId(navId.getNodeId());
			}
			result.add(node);
			return result;
		}
		
		//this is NOT an end screen, proceed recursively till the end
		result.add(ctUtil.createRadioNode(id, treeNode.getName() + label, childIds, null));
		
		for (AttributeTreeNode child : treeNode.getActiveChildren()) {
			result.addAll(buildAttributeTreeNodes(child, map, navId, resultElementId, label));
		}		
		return result;
	}

	/**
	 * Creates last node where user can specify if he want to save observation as new waypoint or attach to previous
	 * 
	 * @param id
	 * @param startId
	 */
	private Node createLastNode(CyberTrackerId id, CyberTrackerId startId, String defaultAttrValues) {
		Node node = ctUtil.createRadioNode(id.getNodeId(), Messages.CyberTrackerExporter_Waypoint_ScreenTitle, newWpElementsIds, newWpResultId.getItemId());
		Control menoControl = screensFactory.createBottomMemoControl13(Messages.CyberTrackerExporter_SaveButtonsInfo);
		ScreensObjectFactory.addControlToNode(node, menoControl);

		if (defaultAttrValues != null && !defaultAttrValues.isEmpty()) {
			Control defaultAttr = screensFactory.createAttrubuteControl14(defaultAttrValuesResultId.getItemId(), false, defaultAttrValues);
			ScreensObjectFactory.addControlToNode(node, defaultAttr);
		}
		
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

	private void split(List<CmAttribute> fullList, List<CmAttribute> toShow, List<CmAttribute> invisibleList) {
		for (CmAttribute attr : fullList) {
			if (attr.isVisible()) {
				Attribute attribute = attr.getAttribute();
				switch (attribute.getType()) {
				case LIST:
				{
					List<AttributeListItem> activeItems = attribute.getActiveListItems();
					if (activeItems == null || activeItems.isEmpty()) {
						continue;
					}
					break;
				}
				case TREE:
				{
					List<AttributeTreeNode> activeTreeNodes = attribute.getActiveTreeNodes();
					if (activeTreeNodes == null || activeTreeNodes.isEmpty()) {
						//skip invalid attribute (attribute without any possible value)
						continue;
					}
					break;
				}
				default:
					break;
				}
				toShow.add(attr);
			} else {
				invisibleList.add(attr);
			}
		}
	}
	
}
