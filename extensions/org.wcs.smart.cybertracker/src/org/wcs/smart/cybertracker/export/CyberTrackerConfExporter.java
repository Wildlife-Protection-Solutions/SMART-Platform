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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.export.MetaExportResult.IdNamePair;
import org.wcs.smart.cybertracker.export.data.IAttributeListItemProxy;
import org.wcs.smart.cybertracker.export.data.IAttributeTreeNodeProxy;
import org.wcs.smart.cybertracker.export.data.ListItemsDataProvider;
import org.wcs.smart.cybertracker.export.data.TreeNodeDataProvider;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.reports.Items;
import org.wcs.smart.cybertracker.model.reports.Reports;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.cybertracker.model.screens.Screens;
import org.wcs.smart.cybertracker.util.LanguageUtil;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.UuidUtils;

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
	private Map<CmAttribute, ListItemsDataProvider> listAttr2ItemData = new HashMap<CmAttribute, ListItemsDataProvider>();
	private Map<CmAttribute, TreeNodeDataProvider> treeAttr2ItemData = new HashMap<CmAttribute, TreeNodeDataProvider>();

	private CyberTrackerId newWpResultId;
	private List<CyberTrackerId> newWpElementsIds;
	
	private CyberTrackerId wpEndGroupResultId;
	private List<CyberTrackerId> wpEndGroupElementsIds;

	private CyberTrackerId defaultAttrValuesResultId;
	
	private List<CyberTrackerId> photoResultIds;
	private List<CyberTrackerId> addPhotoElementIds;
	
	private Session session;
	private ConfigurableModel configurableModel;
	
	private Language currentLanguage;
	
	public void setCurrentLanguage(Language currentLanguage) {
		this.currentLanguage = currentLanguage;
	}

	public File export(File destFolder, IConfigurableModelProvider cmProvider, IProgressMonitor monitor) throws Exception {
		session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			if (cmProvider != null) {
				configurableModel = cmProvider.getConfigurableModel(session, monitor);
			}
			if (configurableModel == null) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.CyberTrackerExporter_Error_InvalidSource, null);
				return null;
			}
			
			monitor.beginTask(Messages.CyberTrackerExportHandler_TaskName, 100);
			
			elements = ElementsUtil.buildEmptyElements();
			newWpResultId = new CyberTrackerId();
			ElementsUtil.addElementsItem(elements, ScreensUtil.RESULT_NEW_WAYPOINT, newWpResultId.getItemId());
			newWpElementsIds = createNewWpElementsIds(elements);
			defaultAttrValuesResultId = new CyberTrackerId();
			ElementsUtil.addElementsItem(elements, ScreensUtil.RESULT_DEFAULT_ATTRIBUTE_VALUES, defaultAttrValuesResultId.getItemId(), null, ElementsUtil.DEFAULT_VALUES_ELEMENT_TAG);
			photoResultIds = new ArrayList<CyberTrackerId>();
			addPhotoElementIds = ElementsUtil.buildBooleanElements(elements);
			wpEndGroupResultId = new CyberTrackerId();
			ElementsUtil.addElementsItem(elements, ScreensUtil.RESULT_ENG_WAYPOINT_GROUP, wpEndGroupResultId.getItemId());
			wpEndGroupElementsIds = createEndWpGroupElementsIds(elements);
			processExportSource(elements, cmProvider.getExportSource());
			return performExport(destFolder, monitor);
		} finally {
			screensFactory = null;
			ctUtil = null;
			configurableModel = null;
			defaultAttrValuesResultId = null;
			newWpResultId = null;
			newWpElementsIds = null;
			wpEndGroupResultId = null;
			wpEndGroupElementsIds = null;
			photoResultIds = null;
			addPhotoElementIds = null;
			elements = null;
			rootId = null;
			attr2resultId.clear();
			nodeLevel2resultId.clear();
			listAttr2ItemData.clear();
			treeAttr2ItemData.clear();
			session.getTransaction().rollback();
			session.close();
			session = null;
		}
	}

	protected void processExportSource(Elements elems, Object exportSource) {
		// nothing by default
	}

	protected ScreensUtil createScreensUtil(CyberTrackerUtil ctu) {
		return new ScreensUtil(ctu);
	}

	private File performExport(File file, IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CyberTrackerExporter_Progress_Fetch_Configuration);
		CyberTrackerProperties ctProperties = CyberTrackerHibernateManager.getProperties(session);
		screensFactory = new ScreensObjectFactory(ctProperties);
		ctUtil = new CyberTrackerUtil(screensFactory, currentLanguage);
		monitor.worked(10);
		
		monitor.subTask(Messages.CyberTrackerExporter_Progress_Build_Mappings);
		CmNode root = ctUtil.buildRoot(configurableModel);
		Map<CmNode, CyberTrackerId> keyMap = ctUtil.buildMap(root);

		monitor.subTask(Messages.CyberTrackerExporter_Progress_Build_Content);
		ScreensUtil screensUtil = createScreensUtil(ctUtil);
		MetaExportResult metaScreensData = screensUtil.buildMetaNodes(elements, keyMap.get(root), session);
		if (metaScreensData == null) {
			//failed to generate patrol data
			//error message is expected to be displayed be PatrolScreensUtil
			return null;
		}
		List<Node> screenNodes = new ArrayList<Node>();
		screenNodes.addAll(metaScreensData.screenNodes);
		rootId = metaScreensData.rootId;
		monitor.worked(5);

		screenNodes.addAll(buildCategoryNodes(root, keyMap, 0));
		monitor.worked(70);
		
		Screens screens = screensFactory.createScreens(screenNodes, ctProperties, configurableModel.getName());
		monitor.worked(5);

		try {
			//----------------creating Screens.xml----------------
			monitor.subTask(Messages.CyberTrackerExporter_Progress_Generate_Screens);
			
			try (BufferedOutputStream outS = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath() + File.separator + ICyberTrackerConstants.XML_SCREENS))){
				writeDataModel(screens, outS, Screens.class);
			}
			monitor.worked(10);
			
			//----------------creating Elements.xml----------------
			monitor.subTask(Messages.CyberTrackerExporter_Progress_Generate_Elements);
			ElementsUtil.addNodeElements(elements, keyMap, currentLanguage);
			
			try (BufferedOutputStream outE = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath() + File.separator + ICyberTrackerConstants.XML_ELEMENTS))){ 
				writeDataModel(elements, outE, Elements.class);
			}
			
			//----------------creating Reports.xml----------------
			monitor.subTask(Messages.CyberTrackerExporter_Progress_Generate_Reports);
			List<Items.Item> columnItems = new ArrayList<Items.Item>();
			columnItems.add(ReportsObjectFactory.createColumnItem(ICyberTrackerConstants.DATE, Messages.CyberTrackerExporter_Report_Column_Date));
			columnItems.add(ReportsObjectFactory.createColumnItem(ICyberTrackerConstants.TIME, Messages.CyberTrackerExporter_Report_Column_Time));
			for (IdNamePair pair : metaScreensData.resultElements) {
				columnItems.add(ReportsObjectFactory.createColumnItem(pair.id, pair.name));
			}
			for (Attribute attribute : attr2resultId.keySet()) {
				Map<Integer, CyberTrackerId> map = attr2resultId.get(attribute);
				for (Integer i : map.keySet()) {
					columnItems.add(ReportsObjectFactory.createColumnItem(map.get(i).getItemId(), LanguageUtil.getName(attribute, currentLanguage) + "#" + i)); //$NON-NLS-1$
				}
			}
			columnItems.add(ReportsObjectFactory.createColumnItem(defaultAttrValuesResultId.getItemId(), ScreensUtil.RESULT_DEFAULT_ATTRIBUTE_VALUES));
			for (Integer level : nodeLevel2resultId.keySet()) {
				columnItems.add(ReportsObjectFactory.createColumnItem(nodeLevel2resultId.get(level).getItemId(), NODE_DEPTH_RESULT_PREFIX+String.valueOf(level)));
			}
			columnItems.add(ReportsObjectFactory.createColumnItem(newWpResultId.getItemId(), ScreensUtil.RESULT_NEW_WAYPOINT));
			Reports reports = ReportsObjectFactory.createReports(columnItems);
			
			try (BufferedOutputStream outR = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath() + File.separator + ICyberTrackerConstants.XML_REPORTS))){
				writeDataModel(reports, outR, Reports.class);
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

	private List<Node> buildCategoryNodes(CmNode node, Map<CmNode, CyberTrackerId> keyMap, Integer level) throws Exception {
		List<Node> result = new ArrayList<Node>();
		if (node == null)
			return result;
		
		if (node.getChildren() == null || node.getChildren().isEmpty()) {
			List<Node> attributeNodes = buildAttributeNodes(node, keyMap);
			if (!attributeNodes.isEmpty()) {
				result.addAll(attributeNodes);
			} else {
				//it appeared that category has no attributes to display -> show warning screen for that case
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

	private List<Node> buildAttributeNodes(CmNode cmNode, Map<CmNode, CyberTrackerId> keyMap) throws Exception {
		List<Node> result = new ArrayList<Node>();
		if (cmNode.isGroup())
			return result;
		
		CyberTrackerId startId = keyMap.get(cmNode);
		AttributeSplitResult splitResult = splitAttributes(cmNode); //NOTE: showOncesBefore/showOnceAfter will be empty if collectMultipleObservations is not set! this is important for logic below
		
		List<Node> nodeList = new ArrayList<Node>();
		BuildNodesResult buildResult = null;
		CyberTrackerId nextId = startId;
		
		if (cmNode.isUseSingleGpsPoint()) {
			CyberTrackerId saveTargetId = new CyberTrackerId();
			Node singleGpsNode = ctUtil.createSaveNode(nextId, saveTargetId, "Single GPS Point", "This category will use single GPS point for all observations. Press \"Save\" to record a point.", true);
			nodeList.add(singleGpsNode);
			nextId = saveTargetId;
		}
		
		//showOncesBefore will be empty if collectMultipleObservations is not set!!!
		buildResult = buildBasicAttributeNodes(splitResult.getToShowOncesBefore(), keyMap, nextId, 0, true, null);
		nextId = buildResult.getNextId(); //id for next screen that will follow this group of attributes
		nodeList.addAll(buildResult.getNodes());
		
		//adding all attributes that are supposed to be displayed
		CyberTrackerId loopBackId = nextId;
		buildResult = buildBasicAttributeNodes(splitResult.getToShow(), keyMap, nextId, 0, true, null);
		nodeList.addAll(buildResult.getNodes());
		nextId = buildResult.getNextId(); //id for next screen that will follow this group of attributes
		
		
		//add photo nodes if required
		if (cmNode.isPhotoAllowed()) {
			nextId = addPhotos(nextId, nodeList, cmNode.isPhotoRequired(), ctUtil.getCtProperties().getMaxPhotoCount());
		}
		
		if (cmNode.isCollectMultipleObservations()) {
			//loop for collecting multiple observations
			buildResult = createEndGroupNodes(nextId, loopBackId, !cmNode.isUseSingleGpsPoint());
			nodeList.addAll(buildResult.getNodes());
			nextId = buildResult.getNextId(); //id for next screen
		}
		
		//showOncesAfter will be empty if collectMultipleObservations is not set!!!
		buildResult = buildBasicAttributeNodes(splitResult.getToShowOncesAfter(), keyMap, nextId, 0, true, null);
		nodeList.addAll(buildResult.getNodes());
		nextId = buildResult.getNextId(); //id for next screen that will follow this group of attributes
		
		//adding nodes with final "save" button for observation or observation group
		String defaultAttrValues = recordDefaultValues(splitResult.getInvisibleList());
		if (cmNode.isCollectMultipleObservations()) {
			//this is observation group and we show simple "save" screen as it will always be added as new waypoint
			Node saveGrpNode = ctUtil.createSaveNode(nextId, rootId, "End observation group", "Press \"Save\" to confirm the end of entering data for this observation group", !cmNode.isUseSingleGpsPoint());
			addAttributesDefaultValues(saveGrpNode, defaultAttrValues);
			nodeList.add(saveGrpNode);
		} else {
			//this is a regular single observation -> show "save as new" / "add to last" options
			nodeList.addAll(createSaveWaypointNodes(nextId, defaultAttrValues));
		}
		return nodeList;
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
		
		List<IAttributeListItemProxy> activeItems = getActiveListItems(listAttr);
		if (activeItems == null || activeItems.isEmpty()) {
			//development validation: this MUST NEVER happen as it is tracked by split(...) logic!!!
			throw new IllegalArgumentException("Cannot add a screen without any items to display"); //$NON-NLS-1$
		}
		List<String> itemNames = new ArrayList<String>();
		List<String> tag0Values = new ArrayList<String>();
		for (IAttributeListItemProxy listItem : activeItems) {
			itemNames.add(listItem.getName());
			tag0Values.add(UuidUtils.uuidToString(listItem.getUuid()));
			
		}

		String tag1 = isMulti ? ElementsUtil.MULISELECT_ELEMENT_TAG : null;
		String tag3 = isMulti ? getAttributeResultElementId(attribute, 0).getItemId() : null;
		String tag4 = numAttr != null ? getAttributeResultElementId(numAttr.getAttribute(), 0).getItemId() : null;
		for (int i = 0; i < activeItems.size(); i++) {
			IAttributeListItemProxy listItem = activeItems.get(i);
			String name = listItem.getName();
			String tag0 = UuidUtils.uuidToString(listItem.getUuid());
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
		Node node = screensFactory.createNodeMultiList(id.getNodeId(), LanguageUtil.getName(cmAttr, currentLanguage), values, trElements, trLinks, 1, withNumbers);
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
	
	private BuildNodesResult buildBasicAttributeNodes(List<CmAttribute> attrList, Map<CmNode, CyberTrackerId> keyMap, CyberTrackerId startId, int index, boolean terminate, String namePostfix) throws Exception {
		List<Node> result = new ArrayList<Node>();
		if (namePostfix == null)
			namePostfix = ""; //$NON-NLS-1$
		
		List<CyberTrackerId> boolRqAttrElementIDs = null;
		CyberTrackerId id = startId;
		for (int i = 0; i < attrList.size(); i++) {
			CmAttribute cmAttr = attrList.get(i);
			Attribute attribute = cmAttr.getAttribute();
			if (!cmAttr.isVisible()) {
				//should NEVER happen
				//development validation! remove throw block after testing
				throw new IllegalArgumentException("Arguments passed to this method MUST be visible"); //$NON-NLS-1$
			}

			//block to check for multi-select / numeric multi-select
			if (cmAttr.isMultiselect() && cmAttr.isVisible() && AttributeType.LIST.equals(attribute.getType())) {
				CmAttribute numAttr = attrList.size() > i+1 && attrList.get(i+1).isNumeric() ? attrList.get(i+1) : null;
				List<CyberTrackerId> multiIds = addListElements(cmAttr, true, numAttr); 
				Node mNode = buildMultiSelectNode(cmAttr, id, multiIds, numAttr != null);
				Control control2 = ScreensObjectFactory.getNavigationControl(mNode);
				CyberTrackerId nextId = new CyberTrackerId();
				//reference to "Next" screen
				control2.setTranslateNextScreenId(nextId.getNodeId());
				result.add(mNode);
				int cutIndex = numAttr == null ? i+1 : i+2;
				List<CmAttribute> toShow = attrList.subList(cutIndex, attrList.size()); //sublist of everything after multi-select / numeric multi-select
				List<IAttributeListItemProxy> activeItems = getActiveListItems(cmAttr);
				for (int x = 0; x < multiIds.size(); x++) {
					BuildNodesResult buildResult = buildBasicAttributeNodes(toShow, keyMap, multiIds.get(x), x, false, " ("+activeItems.get(x).getName()+")"); //$NON-NLS-1$ //$NON-NLS-2$
					result.addAll(buildResult.getNodes());
				}
				return new BuildNodesResult(nextId, result);
			}
			//end of multi-select / numeric multi-select block
			
			boolean linkToNext = terminate || i < attrList.size() - 1;
			CyberTrackerId resultElementId = getAttributeResultElementId(attribute, index); //id for result element in attribute screen node
			switch (attribute.getType()) {
			case NUMERIC:
			{
				Node numberNode = screensFactory.createNodeNumber(id.getNodeId(), LanguageUtil.getName(cmAttr, currentLanguage) + namePostfix, resultElementId.getItemId());
				if (attribute.getIsRequired()) {
					Control numControl = ScreensObjectFactory.getNumberMainControl(numberNode);
					numControl.setRequireSetValue(ICyberTrackerConstants.STR_TRUE);
				}
				result.add(numberNode);
				break;
			}
			case TEXT:
			{
				Node textNode = screensFactory.createNodeNote(id.getNodeId(), LanguageUtil.getName(cmAttr, currentLanguage) + namePostfix, resultElementId.getItemId());
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
				Node node = screensFactory.createNodeRadio(id.getNodeId(), LanguageUtil.getName(cmAttr, currentLanguage) + namePostfix, values, trElements, trLinks, resultElementId.getItemId());
				if (!attribute.getIsRequired()) {
					Control control7 = ScreensObjectFactory.getRadioMainControl(node);
					control7.setRadioBlockNext(ICyberTrackerConstants.STR_FALSE);
				}
				result.add(node);
				break;
			}
			case TREE:
			{
				if (cmAttr.isFlattenTree() || isSingleLevelTree(cmAttr)) {
					List<CyberTrackerId> ids = addFinalTreeNodes(cmAttr, cmAttr.isFlattenTree() ? new NameTreeNodeComparator() : null);
					List<String> values = ctUtil.listItemIds(ids);
					String trElements = ctUtil.translateElements(ids);
					String trLinks = ctUtil.translateLinks(ids, false);
					Node node = screensFactory.createNodeRadio(id.getNodeId(), LanguageUtil.getName(cmAttr, currentLanguage) + namePostfix, values, trElements, trLinks, resultElementId.getItemId());
					if (!attribute.getIsRequired()) {
						Control control7 = ScreensObjectFactory.getRadioMainControl(node);
						control7.setRadioBlockNext(ICyberTrackerConstants.STR_FALSE);
					}
					result.add(node);
				} else {
					String nodeId = id.getNodeId();
					id = linkToNext ? new CyberTrackerId() : null; //this id will be used for next screen
					result.addAll(buildAttributeTreeNodes(cmAttr, nodeId, id, resultElementId.getItemId(), namePostfix, terminate));
					linkToNext = false; //for this case we track linking separately, we don't want any linking logic to be executed further for this attribute
				}
				break;
			}
			case BOOLEAN:
			{
				if (boolRqAttrElementIDs == null) {
					boolRqAttrElementIDs = ElementsUtil.buildBooleanElements(elements);
				}
				Node node = ctUtil.createRadioNode(id.getNodeId(), LanguageUtil.getName(cmAttr, currentLanguage) + namePostfix, boolRqAttrElementIDs, resultElementId.getItemId());
				if (!attribute.getIsRequired()) {
					Control control7 = ScreensObjectFactory.getRadioMainControl(node);
					control7.setRadioBlockNext(ICyberTrackerConstants.STR_FALSE);
				}
				result.add(node);
				break;
			}
			case DATE:
			{
				Node dateNode = screensFactory.createDate(id.getNodeId(), LanguageUtil.getName(cmAttr, currentLanguage) + namePostfix, resultElementId.getItemId(), attribute.getIsRequired());
				result.add(dateNode);
				break;
			}
			default:
				throw new Exception(
						MessageFormat.format(
								Messages.CyberTrackerConfExporter_AttributeTypeNotSupported, new Object[]{attribute.getType().name(), attribute.getName()}));
			}

			//tracking navigation for non-tree or float trees attributes (tree attributes are handle separately)
			if (linkToNext) {
				//handle only cases for non-tree attributes, as all the have single ending screen
				if (!result.isEmpty()) {
					Node lastNode = result.get(result.size()-1);
					id = linkToNext(lastNode); //this id will be used for next screen
				} else {
					id = new CyberTrackerId(); //this id will be used for next screen
				}
			}
		}
		return new BuildNodesResult(id, result);
	}

	/**
	 * @return id for next screen(node)
	 */
	private CyberTrackerId linkToNext(Node node) {
		CyberTrackerId id = new CyberTrackerId(); //this id will be used for next screen
		Control control2 = ScreensObjectFactory.getNavigationControl(node);
		//reference to "Next" screen
		control2.setTranslateNextScreenId(id.getNodeId());
		return id;
	}
	
	private List<CyberTrackerId> addFinalTreeNodes(CmAttribute cmAttr, Comparator<IAttributeTreeNodeProxy> comparator) {
		List<IAttributeTreeNodeProxy> activeTreeNodes = getActiveTreeNodes(cmAttr);
		if (activeTreeNodes == null || activeTreeNodes.isEmpty()) {
			//development validation: this MUST NEVER happen as it is tracked by split(...) logic!!!
			throw new IllegalArgumentException("Cannot add a flat tree screen without any items to display"); //$NON-NLS-1$
		}
		List<IAttributeTreeNodeProxy> finalTreeNodes = listFinalTreeNodes(activeTreeNodes);
		if (comparator != null) {
			Collections.sort(finalTreeNodes, comparator);
		}
		
		List<CyberTrackerId> ids = new ArrayList<CyberTrackerId>();
		for (IAttributeTreeNodeProxy treeNode : finalTreeNodes) {
			String name = treeNode.getName();
			String tag0 = UuidUtils.uuidToString(treeNode.getUuid());
			CyberTrackerId id = new CyberTrackerId();
			ElementsUtil. addElementsItem(elements, name, id.getItemId(), tag0);
			ids.add(id);
		}
		return ids;
	}
	
	private List<IAttributeTreeNodeProxy> listFinalTreeNodes(List<IAttributeTreeNodeProxy> activeTreeNodes) {
		List<IAttributeTreeNodeProxy> result = new ArrayList<IAttributeTreeNodeProxy>();
		for (IAttributeTreeNodeProxy treeNode : activeTreeNodes) {
			List<IAttributeTreeNodeProxy> activeChildren = treeNode.getActiveChildren();
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
		{
			UUID uuidValue = defaultValueOption.getUuidValue();
			if (uuidValue != null) {
				AttributeListItem def = null;
				for (AttributeListItem item : attribute.getAttributeList()) { //check all including disabled
					if (uuidValue.equals(item.getUuid())) {
						def = item; 
						break;
					}
				}
				if (def == null) {
					CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, MessageFormat.format(Messages.CyberTrackerConfExporter_DefaultValue_List_Error, attribute.getName()), null);
					return null;
				}
				String elId = (new CyberTrackerId()).getItemId();
				ElementsUtil.addElementsItem(elements, LanguageUtil.getName(def, currentLanguage), elId, UuidUtils.uuidToString(def.getUuid()));
				return recordDefaultValue(attribute, elId);
			}
			break;
		}
		case TREE:
		{
			UUID uuidValue = defaultValueOption.getUuidValue();
			if (uuidValue != null) {
				AttributeTreeNode def = null;
				for (AttributeTreeNode item : attribute.getActiveTreeNodes()) { //check dm items
					if (uuidValue.equals(item.getUuid())) {
						def = item; 
						break;
					} else {
						def = findTreeNode(item, uuidValue);
						if (def != null) {
							break;
						}
					}
				}
				if (def == null) {
					CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, MessageFormat.format(Messages.CyberTrackerConfExporter_DefaultValue_Tree_Error, attribute.getName()), null);
					return null;
				}
				String elId = (new CyberTrackerId()).getItemId();
				ElementsUtil.addElementsItem(elements, LanguageUtil.getName(def, currentLanguage), elId, UuidUtils.uuidToString(def.getUuid()));
				return recordDefaultValue(attribute, elId);
			}
			break;
		}
		case BOOLEAN:
			if (defaultValueOption.getBooleanValue() != null)
				return recordDefaultValue(attribute, defaultValueOption.getBooleanValue().toString());
			break;
		default:
			return null;
		}
		
		return null;
	}

	private AttributeTreeNode findTreeNode(AttributeTreeNode node, UUID uuidValue) {
		if (node.getActiveChildren() == null)
			return null;

		AttributeTreeNode result = null;
		for (AttributeTreeNode item : node.getActiveChildren()) {
			if (uuidValue.equals(item.getUuid())) {
				return item;
			}
			result = findTreeNode(item, uuidValue);
			if (result != null) {
				return result;
			}
		}
		return result;
	}
	
	private String recordDefaultValue(Attribute attribute, String defaultValue) {
		//tag0 - key (attribute uuid); tag1 - value (default value for this attribute in given observation)
		String ctid = (new CyberTrackerId()).getItemId();
		ElementsUtil.addElementsItem(elements, LanguageUtil.getName(attribute, currentLanguage), ctid, UuidUtils.uuidToString(attribute.getUuid()), null, defaultValue);
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

	private List<CyberTrackerId> createEndWpGroupElementsIds(Elements elements2) {
		List<String> labelValues = new ArrayList<String>();
		labelValues.add("Make Another Observation");
		labelValues.add("End Observation Group");
		List<String> tag0Values = new ArrayList<String>();
		tag0Values.add("false"); //$NON-NLS-1$
		tag0Values.add("true"); //$NON-NLS-1$
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
			String uuid = UuidUtils.uuidToString(attribute.getUuid());
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
	private List<Node> buildAttributeTreeNodes(CmAttribute treeCmAttribute, String nodeId, CyberTrackerId navId, String resultElementId, String label, boolean terminate) {
		List<Node> result = new ArrayList<Node>();
		List<IAttributeTreeNodeProxy> activeTreeNodes = new ArrayList<IAttributeTreeNodeProxy>();
		activeTreeNodes.addAll(getActiveTreeNodes(treeCmAttribute));
		
		Map<IAttributeTreeNodeProxy, CyberTrackerId> map = ctUtil.buildTreeNodeMap(activeTreeNodes);
		List<CyberTrackerId> childIds = ctUtil.getChildrenIds(activeTreeNodes, map);
		Node treeRootNode = ctUtil.createRadioNode(nodeId, LanguageUtil.getName(treeCmAttribute, currentLanguage) + label, childIds, null);
		if (!treeCmAttribute.getAttribute().getIsRequired() && navId != null) {
			Control navControl = ScreensObjectFactory.getNavigationControl(treeRootNode);
			navControl.setTranslateNextScreenId(navId.getNodeId());
			Control control7 = ScreensObjectFactory.getRadioMainControl(treeRootNode);
			control7.setRadioBlockNext(ICyberTrackerConstants.STR_FALSE);
			if (!terminate) {
				navId = null; //we are under mutli-select and if two next screens are defined that causes stacking problem (so we need to avoid next screen for leaves)
			}
		}
		result.add(treeRootNode);
		for (IAttributeTreeNodeProxy treeNode : activeTreeNodes) {
			result.addAll(buildAttributeTreeNodes(treeNode, map, navId, resultElementId, label));
		}
		
		//below is same as ElementsUtil.addElements(elements, map);
		for (IAttributeTreeNodeProxy dmObject : map.keySet()) {
			ElementsUtil.addElementsItem(elements, dmObject.getName(), map.get(dmObject).getItemId(), UuidUtils.uuidToString(dmObject.getUuid()));
		}

		return result;
	}
	
	private List<Node> buildAttributeTreeNodes(IAttributeTreeNodeProxy treeNode, Map<IAttributeTreeNodeProxy, CyberTrackerId> map, CyberTrackerId navId, String resultElementId, String label) {
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
		for (IAttributeTreeNodeProxy child : treeNode.getActiveChildren()) {
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
		
		for (IAttributeTreeNodeProxy child : treeNode.getActiveChildren()) {
			result.addAll(buildAttributeTreeNodes(child, map, navId, resultElementId, label));
		}		
		return result;
	}

	private boolean isSingleLevelTree(CmAttribute cmAttr) {
		boolean isSingleLevel = true;
		List<IAttributeTreeNodeProxy> children = getActiveTreeNodes(cmAttr);
		for (IAttributeTreeNodeProxy child : children) {
			if (child.getActiveChildren() != null && !child.getActiveChildren().isEmpty()) {
				isSingleLevel = false;
				break;
			}
		}		
		return isSingleLevel;
	}
	
	/**
	 * Creates last node where user can specify if he want to save observation as new waypoint or attach to previous
	 * 
	 * @param id
	 * @param startId
	 */
	private List<Node> createSaveWaypointNodes_old(CyberTrackerId id, CyberTrackerId startId, String defaultAttrValues) {
		//TODO:
		List<Node> nodeList = new ArrayList<Node>();
		Node node = ctUtil.createRadioNode(id.getNodeId(), Messages.CyberTrackerExporter_Waypoint_ScreenTitle, newWpElementsIds, newWpResultId.getItemId());
//		Control menoControl = screensFactory.createBottomMemoControl13(Messages.CyberTrackerExporter_SaveButtonsInfo);
//		ScreensObjectFactory.addControlToNode(node, menoControl);

		if (defaultAttrValues != null && !defaultAttrValues.isEmpty()) {
			Control defaultAttr = screensFactory.createAttrubuteControl14(defaultAttrValuesResultId.getItemId(), false, defaultAttrValues);
			ScreensObjectFactory.addControlToNode(node, defaultAttr);
		}
		
		Control control2 = ScreensObjectFactory.getNavigationControl(node);
		//this is the last screen and we need to show "Save" button
		control2.setShowMajor("True"); //$NON-NLS-1$
//		control2.setShowMinor("True"); //$NON-NLS-1$
		control2.setShowNext("False"); //$NON-NLS-1$
		control2.setTranslateNextScreenId(null); //no next button at last screen
		control2.setTranslateMajorScreenId(rootId.getNodeId());
//		control2.setTranslateMinorScreenId(startId.getNodeId());
		nodeList.add(node);
		return nodeList;
	}	

	/**
	 * Creates last node where user can specify if he want to save observation as new waypoint or attach to previous
	 * 
	 * @param id
	 * @param startId
	 */
	private List<Node> createSaveWaypointNodes(CyberTrackerId id, String defaultAttrValues) {
		List<Node> nodeList = new ArrayList<Node>();
		
		CyberTrackerId saveAsNewId = new CyberTrackerId();
		Node saveAsNewNode = ctUtil.createSaveNode(saveAsNewId, rootId, Messages.CyberTrackerExporter_Waypoint_SaveAsNew, "Save as new taking new GPS reading", true);
		
		CyberTrackerId addToLastId = new CyberTrackerId();
		Node addToLastNode = ctUtil.createSaveNode(addToLastId, rootId, Messages.CyberTrackerExporter_Waypoint_AddToLast, "Add to last without GPS reading", false);

		List<CyberTrackerId> ids = new ArrayList<CyberTrackerId>(2);
		ids.add(new CyberTrackerIdMap(saveAsNewId, newWpElementsIds.get(0)));
		ids.add(new CyberTrackerIdMap(addToLastId, newWpElementsIds.get(1)));

		Node node = ctUtil.createRadioNode(id.getNodeId(),  Messages.CyberTrackerExporter_Waypoint_ScreenTitle, ids, newWpResultId.getItemId(), true);
		addAttributesDefaultValues(node, defaultAttrValues);
		
		nodeList.add(node);
		nodeList.add(saveAsNewNode);
		nodeList.add(addToLastNode);
		
		return nodeList;
	}	

	/**
	 * Creates last node where user can specify if he want to save observation as new waypoint or attach to previous
	 * 
	 * @param id
	 * @param startId
	 */
	private BuildNodesResult createEndGroupNodes(CyberTrackerId id, CyberTrackerId loopBackId, boolean takeGPsReading) {
		List<Node> nodeList = new ArrayList<Node>(2);
		CyberTrackerId nextId = new CyberTrackerId();
		CyberTrackerId loopSaveId = new CyberTrackerId();
		
		Node loopSaveNode = ctUtil.createSaveNode(loopSaveId, loopBackId, "Save Group Observation", "Press \"Save\" to confirm saving current observation and proceed to record observations for this group.", takeGPsReading);

		List<CyberTrackerId> ids = new ArrayList<CyberTrackerId>(2);
		ids.add(new CyberTrackerIdMap(loopSaveId, wpEndGroupElementsIds.get(0))); //"Make Another Observation" navigate loop start screen
		ids.add(new CyberTrackerIdMap(nextId, wpEndGroupElementsIds.get(1))); //"End Group" navigate to to next screen (exit from loop)

		Node groupTaskNode = ctUtil.createRadioNode(id.getNodeId(), "Next Group Task", ids, wpEndGroupResultId.getItemId(), true);
		
		nodeList.add(groupTaskNode);
		nodeList.add(loopSaveNode);
		
		return new BuildNodesResult(nextId, nodeList);
	}	

	private CyberTrackerId addPhotos(CyberTrackerId id, List<Node> nodeList, boolean photoRequired, int count) {
		List<CyberTrackerId> ctIdList = new ArrayList<CyberTrackerId>(2*count);
		for (int i = 0; i < 2*count; i++) {
			ctIdList.add(new CyberTrackerId());
		}
		CyberTrackerId lastId = ctIdList.get(ctIdList.size()-1);
		
		for (int i = 0; i < count; i++) {
			if ((i > 0 || !photoRequired) && (count > 1)) {
				CyberTrackerId nextId = ctIdList.get(2*i);
				List<CyberTrackerId> ids = new ArrayList<CyberTrackerId>(2);
				ids.add(new CyberTrackerIdMap(nextId, addPhotoElementIds.get(0))); //"Yes" navigate to next photo
				ids.add(new CyberTrackerIdMap(lastId, addPhotoElementIds.get(1))); //"No" navigate to save screen
				String msg = (i == 0) ? Messages.CyberTrackerExporter_ScreenTitle_AddPhoto : Messages.CyberTrackerExporter_ScreenTitle_AddAnotherPhoto;
				Node node = ctUtil.createRadioNode(id.getNodeId(), msg, ids, null, true);
				id = nextId;
				nodeList.add(node);
			}

			String title = count > 1 ? MessageFormat.format(Messages.CyberTrackerExporter_ScreenTitle_PhotoNum, i+1) : Messages.CyberTrackerExporter_ScreenTitle_Photo;
			Node photoNode = screensFactory.createPhoto(id.getNodeId(), title, getPhotoResultId(i).getItemId(), i==0 && photoRequired);
			id = ctIdList.get(2*i+1);
			Control control2 = ScreensObjectFactory.getNavigationControl(photoNode);
			control2.setTranslateNextScreenId(id.getNodeId());
			nodeList.add(photoNode);

		}
		return lastId;
	}
	
	private CyberTrackerId getPhotoResultId(int index) {
		if (photoResultIds.size() >= index) {
			for (int i = photoResultIds.size(); i <= index; i++) {
				CyberTrackerId id = new CyberTrackerId();
				ElementsUtil.addElementsItem(elements, ScreensUtil.RESULT_PHOTO + i, id.getItemId());
				photoResultIds.add(id);
			}
		}
		return photoResultIds.get(index);
	}

	private void addAttributesDefaultValues(Node node, String defaultAttrValues) {
		if (defaultAttrValues != null && !defaultAttrValues.isEmpty()) {
			Control defaultAttr = screensFactory.createAttrubuteControl14(defaultAttrValuesResultId.getItemId(), false, defaultAttrValues);
			ScreensObjectFactory.addControlToNode(node, defaultAttr);
		}
	}
	
	private AttributeSplitResult splitAttributes(CmNode cmNode) {
		List<CmAttribute> fullList = cmNode.getCmAttributes();
		List<CmAttribute> invisibleList = new ArrayList<CmAttribute>();
		List<CmAttribute> toShow = new ArrayList<CmAttribute>();
		List<CmAttribute> toShowOncesBefore = new ArrayList<CmAttribute>();
		List<CmAttribute> toShowOncesAfter = new ArrayList<CmAttribute>();
		
		for (CmAttribute attr : fullList) {
			if (attr.isVisible()) {
				Attribute attribute = attr.getAttribute();
				//validation logic
				switch (attribute.getType()) {
				case LIST:
				{
					List<IAttributeListItemProxy> activeItems = getActiveListItems(attr);
					if (activeItems == null || activeItems.isEmpty()) {
						continue;
					}
					break;
				}
				case TREE:
				{
					List<IAttributeTreeNodeProxy> activeTreeNodes = getActiveTreeNodes(attr);
					if (activeTreeNodes == null || activeTreeNodes.isEmpty()) {
						//skip invalid attribute (attribute without any possible value)
						continue;
					}
					break;
				}
				default:
					break;
				}

				//actual assignment to a split group
				if (!cmNode.isCollectMultipleObservations()) {
					toShow.add(attr);
				} else {
					switch (attr.getEnterOnce()) {
					case NONE:
						toShow.add(attr);
						break;
					case START:
						toShowOncesBefore.add(attr);
						break;
					case END:
						toShowOncesAfter.add(attr);
						break;
					}
				}
			} else {
				invisibleList.add(attr);
			}
		}
		
		return new AttributeSplitResult(invisibleList, toShow, toShowOncesBefore, toShowOncesAfter);
	}

	private List<IAttributeListItemProxy> getActiveListItems(CmAttribute attribute) {
		if (attribute.getAttribute().getType() != AttributeType.LIST)
			return null;
		ListItemsDataProvider dataProvider = listAttr2ItemData.get(attribute);
		if (dataProvider == null) {
			dataProvider = new ListItemsDataProvider(attribute, configurableModel, currentLanguage, session);
			listAttr2ItemData.put(attribute, dataProvider);
		}
		return dataProvider.getActiveListItems();
	}

	private List<IAttributeTreeNodeProxy> getActiveTreeNodes(CmAttribute attribute) {
		if (attribute.getAttribute().getType() != AttributeType.TREE)
			return null;
		TreeNodeDataProvider dataProvider = treeAttr2ItemData.get(attribute);
		if (dataProvider == null) {
			dataProvider = new TreeNodeDataProvider(attribute, configurableModel, currentLanguage, session);
			treeAttr2ItemData.put(attribute, dataProvider);
		}
		return dataProvider.getActiveTreeNodes();
	}

	/**
	 * Comparator that sorts TreeNodes ({@link IAttributeTreeNodeProxy}) alphabetically
	 * @author elitvin
	 * @since 4.0.0
	 */
	private final class NameTreeNodeComparator implements Comparator<IAttributeTreeNodeProxy> {
		@Override
		public int compare(IAttributeTreeNodeProxy arg0, IAttributeTreeNodeProxy arg1) {
			String n0 = arg0.getName();
			if (n0 == null)
				return -1;
			return n0.compareTo(arg1.getName());
		}
	}

	/**
	 * Result of nodes generation for a sequence of screens.
	 * @author elitvin
	 * @since 4.0.0
	 */
	private final class BuildNodesResult {
		/** id for next node in sequence */
		private CyberTrackerId nextId;
		
		/** list of nodes generated for passed attributes */
		private List<Node> nodes;

		public BuildNodesResult(CyberTrackerId nextId, List<Node> nodes) {
			this.nextId = nextId;
			this.nodes = nodes;
		}
		
		public CyberTrackerId getNextId() {
			return nextId;
		}
		
		public List<Node> getNodes() {
			return nodes;
		}
	}

	/**
	 * Result of split for a {@link CmNode} attributes based on their properties.
	 * @author elitvin
	 * @since 4.0.0
	 */
	private final class AttributeSplitResult {
		private List<CmAttribute> invisibleList;
		private List<CmAttribute> toShow;
		private List<CmAttribute> toShowOncesBefore;
		private List<CmAttribute> toShowOncesAfter;

		public AttributeSplitResult(List<CmAttribute> invisibleList,
				List<CmAttribute> toShow,
				List<CmAttribute> toShowOncesBefore,
				List<CmAttribute> toShowOncesAfter) {
			this.invisibleList = invisibleList;
			this.toShow = toShow;
			this.toShowOncesBefore = toShowOncesBefore;
			this.toShowOncesAfter = toShowOncesAfter;
		}

		public List<CmAttribute> getInvisibleList() {
			return invisibleList;
		}
		public List<CmAttribute> getToShow() {
			return toShow;
		}
		public List<CmAttribute> getToShowOncesBefore() {
			return toShowOncesBefore;
		}
		public List<CmAttribute> getToShowOncesAfter() {
			return toShowOncesAfter;
		}
	}

}
