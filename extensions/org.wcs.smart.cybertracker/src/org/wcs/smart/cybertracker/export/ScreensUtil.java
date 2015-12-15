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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.hibernate.Session;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.export.MetaExportResult.IdNamePair;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.filter.Categories;
import org.wcs.smart.cybertracker.model.filter.ElementFilters;
import org.wcs.smart.cybertracker.model.filter.Filter;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.util.UuidUtils;

/**
 * Util for creating screens based on metadata for CyberTracker.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ScreensUtil {
	
	//NOTE: Naming with "Patrol" are required for backward compatibility with CT exports from version 3.2 or lower
	public static final String RESULT_ID = "#PatrolID"; //$NON-NLS-1$
	public static final String RESULT_START_DATE = "#PatrolStartDate"; //$NON-NLS-1$
	public static final String RESULT_START_TIME = "#PatrolStartTime"; //$NON-NLS-1$

	public static final String RESULT_NEW_WAYPOINT = "#NewWaypoint"; //$NON-NLS-1$
	public static final String RESULT_ENG_WAYPOINT_GROUP = "#WaypointGroupEnd"; //$NON-NLS-1$
	public static final String RESULT_DEFAULT_ATTRIBUTE_VALUES = "#DefaultAttributeValues"; //$NON-NLS-1$

	public static final String RESULT_DEFAULT_META_VALUES = "#DefaultPatrolValues"; //$NON-NLS-1$

	public static final String RESULT_OBSERVER = "#Observer"; //$NON-NLS-1$

	public static final String RESULT_PHOTO = "#Photo"; //$NON-NLS-1$
	
	public static final String RESULT_DATATYPE = "#DataType"; //$NON-NLS-1$

	private ScreensObjectFactory screensFactory;
	private CyberTrackerUtil ctUtil;
	
	protected ScreensUtil(CyberTrackerUtil ctUtil) {
		this.ctUtil = ctUtil;
		this.screensFactory = ctUtil.getScreensFactory();
	}
	
	public MetaExportResult buildMetaNodes(Elements elements, CyberTrackerId dmRootId, Session session) {
//		MetaExportResult result = new MetaExportResult();
		return null; //TODO: in case we want to export without any meta screens logic for that should be placed here
	}

	/**
	 * Required to determine what type of data we are recording (patrol vs survey)
	 * @param elements
	 * @param datatype
	 */
	protected void registerDatatype(Elements elements, String datatype) {
		CyberTrackerId id = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, RESULT_DATATYPE, id.getItemId(), datatype);
	}
	
	protected CyberTrackerId addStartScreen(CyberTrackerId id, MetaExportResult container, Elements elements, CyberTrackerProperties ctProps, StartScreensContent content) {
		List<CyberTrackerId> ids = new ArrayList<CyberTrackerId>();
		ids.add(content.getStartScreenItemId());
		ids.addAll(ElementsUtil.addCustomElements(elements, Messages.PatrolScreens_ExitCyberTracker));
		Node nodeMain = ctUtil.createRadioNode(id.getNodeId(), Messages.PatrolScreens_Start_Title, ids, null, true);
		container.screenNodes.add(nodeMain);
		addGpsConfiguration(nodeMain, ctProps, 0);
		addBatteryControl(nodeMain);

		List<CyberTrackerId> idsBegin = new ArrayList<CyberTrackerId>();
		idsBegin.add(content.getBeginScreenItemId());
		Node nodeBegin = ctUtil.createRadioNode(ids.get(0).getNodeId(), content.getBeginScreenName(), idsBegin, null, true);
		Control beginControl2 = ScreensObjectFactory.getNavigationControl(nodeBegin);
		beginControl2.setShowGPS(ICyberTrackerConstants.STR_TRUE);
		container.screenNodes.add(nodeBegin);
		
		String resultId = createResultElement(RESULT_ID, elements);
		addUniqueAttrubute(nodeBegin, resultId);
		String resultDateId = createResultElement(RESULT_START_DATE, elements);
		String resultTimeId = createResultElement(RESULT_START_TIME, elements);
		addStartTimeAttrubute(nodeBegin, resultDateId, resultTimeId);
		//if "Use GPS Time" option is enabled than "Snap Date & Time" control needs time from GPS to calculate offset from device time,
		//but it doesn't launch GPS reading itself, so we need to add "GPS" control (see ticket #1304 for details)
		addGPSControl(nodeBegin);
		addGPSRequiredWarning(nodeBegin);
		container.resultElements.add(new IdNamePair(resultId, RESULT_ID));
		container.resultElements.add(new IdNamePair(resultDateId, RESULT_START_DATE));
		container.resultElements.add(new IdNamePair(resultTimeId, RESULT_START_TIME));

		Node pwdNode = screensFactory.createNodePassword(ids.get(1).getNodeId(), Messages.PatrolScreens_Exit_Title);
		container.screenNodes.add(pwdNode);
		Control control2 = ScreensObjectFactory.getNavigationControl(pwdNode);
		control2.setShowNext(ICyberTrackerConstants.STR_FALSE);
		
		return idsBegin.get(0);
	}

	protected void buildNextTaskNode(CyberTrackerId id, MetaExportResult container, Elements elements, List<String> nextTaskOptions, List<CyberTrackerId> nodeIds, CyberTrackerProperties ctProps) {
		if (nextTaskOptions.size() != nodeIds.size()) {
			throw new IllegalArgumentException("Unable to build next task node. Number of task options is not equal to the number of referenced nodes."); //$NON-NLS-1$
		}
		StringBuilder defaults = new StringBuilder();
		for (Iterator<String> i = container.defaultValues.iterator(); i.hasNext();) {
			defaults.append(i.next());
			if (i.hasNext())
				defaults.append(ICyberTrackerConstants.ATTRIBUTE_DEFAULT_VALUES_SEPATATOR);
		}
		String defaultValues = defaults.toString();
		
		List<CyberTrackerId> ids = ElementsUtil.addCustomElements(elements, nextTaskOptions.toArray(new String[nextTaskOptions.size()]));
		List<String> values = ctUtil.listItemIds(ids);
		String trElements = ctUtil.translateElements(ids);
		//custom translate links logic
		StringBuilder links = new StringBuilder();
		for (int i = 0; i < ids.size(); i++) {
			links.append(ids.get(i).getItemTranslatedId()).append(nodeIds.get(i).getNodeTranslatedId());
		}
		Node node = screensFactory.createNodeRadio(id.getNodeId(), Messages.PatrolScreens_NextTask, values, trElements, links.toString(), null);
		if (defaultValues != null && !defaultValues.isEmpty()) {
			//adding default values
			CyberTrackerId defId = new CyberTrackerId();
			ElementsUtil.addElementsItem(elements, RESULT_DEFAULT_META_VALUES, defId.getItemId());
			Control defaultAttr = screensFactory.createAttrubuteControl14(defId.getItemId(), false, defaultValues);
			ScreensObjectFactory.addControlToNode(node, defaultAttr);
		}
		addBatteryControl(node);
		
		CyberTrackerProperties properties = ctUtil.getCtProperties();
		Control control2 = ScreensObjectFactory.getNavigationControl(node);
		control2.setShowBack("False"); //$NON-NLS-1$
		if (properties.isShowEdit()) {
			control2.setShowEdit("True"); //$NON-NLS-1$
		}
		if (properties.isShowGPS()) {
			control2.setShowGPS("True"); //$NON-NLS-1$
		}
		
		addGpsConfiguration(node, ctProps);
		container.screenNodes.add(node);
	}
	
	protected CyberTrackerId createEndTripNodes(MetaExportResult container, CyberTrackerId appStartId, String confirmMsg) {
		CyberTrackerId endId = new CyberTrackerId();
		//navigate on save to start point
		Node confirmNode = ctUtil.createSaveNode(endId, appStartId, Messages.PatrolScreens_Confirm, confirmMsg, true);
		container.screenNodes.add(confirmNode);
		return endId;
	}

	protected CyberTrackerId createPauseTripNodes(MetaExportResult container, Elements elements, CyberTrackerId nextTaskId, CyberTrackerProperties ctProps, PauseNodesLabels labels) {
		if (!ctProps.isCanPause()) {
			return null;
		}
		CyberTrackerId resumeId = new CyberTrackerId();
		List<CyberTrackerId> resScrIds = ElementsUtil.addCustomElements(elements, labels.resumeOption);
		List<String> resScrValues = ctUtil.listItemIds(resScrIds);
		String resScrTrElements = ctUtil.translateElements(resScrIds);
		StringBuilder resScrLinks = new StringBuilder();
		// "Resume" leads to "Next Task" screen
		resScrLinks.append(resScrIds.get(0).getItemTranslatedId()).append(nextTaskId.getNodeTranslatedId());
		Node resumeNode = screensFactory.createNodeRadio(resumeId.getNodeId(), labels.resumeScreenTitle, resScrValues, resScrTrElements, resScrLinks.toString(), null);
		addGpsConfiguration(resumeNode, ctProps, 0);
		container.screenNodes.add(resumeNode);
		return resumeId;
	}

	/**
	 * @param name
	 * @param elements
	 * @return String id of newly created element
	 */
	protected String createResultElement(String name, Elements elements) {
		CyberTrackerId resultId = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, name, resultId.getItemId());
		return resultId.getItemId();
	}

	protected String createResultElement(String name, Elements elements, String tag0) {
		CyberTrackerId resultId = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, name, resultId.getItemId(), tag0);
		return resultId.getItemId();
	}
	
	protected String createDefaultResultElement(String name, Elements elements, String defaultValue) {
		CyberTrackerId resultId = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, name, resultId.getItemId(), null, null, defaultValue);
		return resultId.getItemId();
	}
	
	protected CyberTrackerId toNextScreen(Node node) {
		return toNextScreen(node, false);
	}
	
	protected CyberTrackerId toNextScreen(Node node, boolean canSkip) {
		CyberTrackerId nextId = new CyberTrackerId();
		Control control2 = ScreensObjectFactory.getNavigationControl(node);
		control2.setTranslateNextScreenId(nextId.getNodeId());
		if (canSkip) {
			//we should be here only for radio nodes if we want to allow user press "Next" without selecting anything
			Control control7 = ScreensObjectFactory.getRadioMainControl(node);
			control7.setRadioBlockNext(ICyberTrackerConstants.STR_FALSE);
		}
		return nextId;
	}

	protected void applyFilter(Node node, String filter) {
		if (filter != null) {
			Control control7 = ScreensObjectFactory.getRadioMainControl(node);
			control7.setFilterEnabled("True"); //$NON-NLS-1$
			control7.setTranslateFilter(filter);
		}
	}
	
	protected CyberTrackerId addSimpleNextRadioNode(CyberTrackerId id, MetaExportResult container, Elements elements, String name, String resultElName,  List<CyberTrackerId> ids, boolean canSkip) {
		return addSimpleNextRadioNode(id, container, elements, name, resultElName, null, ids, canSkip);
	}

	protected CyberTrackerId addSimpleNextRadioNode(CyberTrackerId id, MetaExportResult container, Elements elements, String name, String resultElName, String tag0, List<CyberTrackerId> ids, boolean canSkip) {
		String resultId = createResultElement(resultElName, elements, tag0);
		Node node = ctUtil.createRadioNode(id.getNodeId(), name, ids, resultId);
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultId, resultElName));
		return toNextScreen(node, canSkip);
	}

	protected CyberTrackerId addSimpleNextRadioNode(CyberTrackerId id, MetaExportResult container, Elements elements, String name, String resultElName,  List<CyberTrackerId> ids, String filter) {
		String resultId = createResultElement(resultElName, elements);
		Node node = ctUtil.createRadioNode(id.getNodeId(), name, ids, resultId);
		applyFilter(node, filter);
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultId, resultElName));
		return toNextScreen(node);
	}

	protected CyberTrackerId addNoteNextNode(CyberTrackerId id, MetaExportResult container, Elements elements, String name, String resultElName, int maxLength) {
		return addNoteNextNode(id, container, elements, name, resultElName, null, maxLength);
	}
	
	protected CyberTrackerId addNoteNextNode(CyberTrackerId id, MetaExportResult container, Elements elements, String name, String resultElName, String tag0, int maxLength) {
		String resultId = createResultElement(resultElName, elements, tag0);
		Node node = screensFactory.createNodeNote(id.getNodeId(), name,  resultId);
		
		Control textControl = ScreensObjectFactory.getNoteMainControl(node);
		textControl.setMaxLength(maxLength);
		
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultId, resultElName));
		return toNextScreen(node);
	}
	
	protected CyberTrackerId addNumberNode(CyberTrackerId id, MetaExportResult container, Elements elements, String name, String resultElName, String tag0) {
		String resultId = createResultElement(resultElName, elements, tag0);
		Node node = screensFactory.createNodeNumber(id.getNodeId(), name,  resultId);
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultId, resultElName));
		return toNextScreen(node);
	}

	protected CyberTrackerId addMembersNode(CyberTrackerId id, MetaExportResult container, List<CyberTrackerId> memberIds) {
		List<String> values = ctUtil.listItemIds(memberIds);
		String trElements = ctUtil.translateElements(memberIds);
		String trLinks = ctUtil.translateLinks(memberIds, false);
		Node node = screensFactory.createNodeMultiList(id.getNodeId(), Messages.PatrolScreens_Members, values, trElements, trLinks, 1, false);
		container.screenNodes.add(node);
		return toNextScreen(node);
	}
	
	public List<CyberTrackerId> toCyberTrackerIds(Elements elements, List<? extends NamedItem> items) {
		List<String> labelValues = new ArrayList<String>();
		List<String> tag0Values = new ArrayList<String>();
		for (NamedItem i : items) {
			labelValues.add(ctUtil.getName(i));
			tag0Values.add(UuidUtils.uuidToString(i.getUuid()));
		}
		return ElementsUtil.addCustomElements(elements, labelValues, tag0Values);
	}

	protected String buildMembersFilter(String memberNodeId, List<CyberTrackerId> memberIds, List<String> memberNames) {
		Filter filter = new Filter();
		filter.setVersion(1);
		
		Categories categories = new Categories();
		filter.setCategories(categories);
		Categories.Items cItems = new Categories.Items();
		categories.setItems(cItems);
		Categories.Items.Item cIt = new Categories.Items.Item();
		cIt.setId(new CyberTrackerId().getNodeId());
		cIt.setName("Members"); //$NON-NLS-1$
		cIt.setCategoryId(memberNodeId);
		cIt.setFilterType(1); // 1 for "Any" filter type
		cItems.getItem().add(cIt);
		
		ElementFilters elFilter = new ElementFilters();
		filter.setElementFilters(elFilter);
		ElementFilters.Items eItems = new ElementFilters.Items();
		elFilter.setItems(eItems);
		for (int i = 0; i < memberIds.size(); i++) {
			CyberTrackerId id = memberIds.get(i);
			String name = (memberNames != null && memberNames.size() > i) ? memberNames.get(i) : null;
			ElementFilters.Items.Item eIt = new ElementFilters.Items.Item();
			eIt.setId(id.getItemId());
			eIt.setName(name);
			ElementFilters.Items.Item.CheckedElements chEl = new ElementFilters.Items.Item.CheckedElements();
			eIt.setCheckedElements(chEl);
			chEl.getValue().add(id.getItemId());
			eItems.getItem().add(eIt);
		}
		
		try {
			JAXBContext context = JAXBContext.newInstance(Filter.class);
			Marshaller marshaller = context.createMarshaller();
			final StringWriter stringWriter = new StringWriter();
			//marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(filter, stringWriter);
			String filterStr = stringWriter.toString();
			int index = filterStr.indexOf("<Filter>"); //$NON-NLS-1$
			return "<?xml version=\"1.0\"?>\r\n" + filterStr.substring(index); //$NON-NLS-1$ //this is REQUIRED as CyberTracker expects EXACTLY "<?xml version=\"1.0\"?>\r\n<Filter>" at the begining
		} catch (JAXBException e) {
			CyberTrackerPlugIn.log(e.getMessage(), e);
		}
		return null;
	}
	
	protected void addNavigationFormula(Node node, String formula, String successId, String failId) {
		Control formulaControl = screensFactory.createNavFormulaControl12(formula, failId, successId);
		node.getData().getControls().getControl().add(formulaControl);
	}

	protected void addGpsConfiguration(Node node, CyberTrackerProperties props) {
		addGpsConfiguration(node, props, null);
	}
	
	protected void addGpsConfiguration(Node node, CyberTrackerProperties props, Integer timerOverride) {
		Control gpsConf = screensFactory.createConfigureGPSControl13(props);
		if (timerOverride != null) {
			gpsConf.setWaypointTimer(timerOverride);
		}
		node.getData().getControls().getControl().add(gpsConf);
	}
	
	protected void addUniqueAttrubute(Node node, String resultElementId) {
		Control uniqueAttr = screensFactory.createAttrubuteControl14(resultElementId, true, null);
		ScreensObjectFactory.addControlToNode(node, uniqueAttr);
	}

	protected void addStartTimeAttrubute(Node node, String resultDateId, String resultTimeId) {
		Control dtAttr = screensFactory.createSnapDateTimeControl15(resultDateId, resultTimeId);
		ScreensObjectFactory.addControlToNode(node, dtAttr);
	}
	
	protected void addGPSControl(Node node) {
		Control gpsControl = screensFactory.createGPSControl16();
		ScreensObjectFactory.addControlToNode(node, gpsControl);
	}

	protected void addGPSRequiredWarning(Node node) {
		Control msgControl = screensFactory.createBottomMemoControl17(Messages.PatrolScreens_Begin_GPSRequiredMessage);
		ScreensObjectFactory.addControlToNode(node, msgControl);
	}

	private void addBatteryControl(Node node) {
		Control stateControl = screensFactory.createSystemStateControl19();
		ScreensObjectFactory.addControlToNode(node, stateControl);
	}

	public CyberTrackerUtil getCtUtil() {
		return ctUtil;
	}
	
	public ScreensObjectFactory getScreensFactory() {
		return screensFactory;
	}
	
	protected class PauseNodesLabels {
		public String resumeScreenTitle = "Paused"; //$NON-NLS-1$
		public String resumeOption = "Resume"; //$NON-NLS-1$
		public PauseNodesLabels() {}
	}
}
