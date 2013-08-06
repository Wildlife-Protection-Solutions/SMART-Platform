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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.filter.Categories;
import org.wcs.smart.cybertracker.model.filter.ElementFilters;
import org.wcs.smart.cybertracker.model.filter.Filter;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.util.SmartUtils;

/**
 * Util for creating patrol screens for CyberTracker.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolScreensUtil {
	
	private static final String GLOBAL_PATROL_TYPE = "GLOBAL_PATROL_TYPE"; //$NON-NLS-1$

	public static final String RESULT_PATROL_ID = "#PatrolID"; //$NON-NLS-1$
	public static final String RESULT_PATROL_TYPE = "#PatrolType"; //$NON-NLS-1$
	public static final String RESULT_TRANSPORT = "#PatrolTransport"; //$NON-NLS-1$
	public static final String RESULT_ARMED = "#Armed"; //$NON-NLS-1$
	public static final String RESULT_TEAM = "#Team"; //$NON-NLS-1$
	public static final String RESULT_STATION = "#Station"; //$NON-NLS-1$
	public static final String RESULT_MANDATE = "#Mandate"; //$NON-NLS-1$
	public static final String RESULT_OBJECTIVE = "#Objective"; //$NON-NLS-1$
	public static final String RESULT_COMMENTS = "#Comments"; //$NON-NLS-1$
	public static final String RESULT_LEADER = "#Leader"; //$NON-NLS-1$
	public static final String RESULT_PILOT = "#Pilot"; //$NON-NLS-1$
	
	public static final String RESULT_NEW_WAYPOINT = "#NewWaypoint"; //$NON-NLS-1$

	/**
	 * Contains data filled by {@link PatrolScreensUtil}
	 * @author elitvin
	 * @since 1.0.0
	 */
	public static class ParolFilledDataContainer {
		List<Node> screenNodes = new ArrayList<Node>();
		List<IdNamePair> resultElements = new ArrayList<IdNamePair>();
		CyberTrackerId rootId = null;
	}
	public static class IdNamePair {
		public String id;
		public String name;
		public IdNamePair(String id, String name) {
			super();
			this.id = id;
			this.name = name;
		}
	}

	
	private ScreensObjectFactory screensFactory;
	private CyberTrackerUtil ctUtil;
	
	protected PatrolScreensUtil(CyberTrackerUtil ctUtil) {
		this.ctUtil = ctUtil;
		this.screensFactory = ctUtil.getScreensFactory();
	}
	
	/**
	 * @param screens
	 * @param element
	 * @return root id
	 */
	public ParolFilledDataContainer buildPatrolNodes(Elements elements, CyberTrackerId dmRootId, Session session) {
		ParolFilledDataContainer result = new ParolFilledDataContainer();
		//start node
		CyberTrackerId startId = new CyberTrackerId();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		CyberTrackerId id = addStartScreen(startId, result, elements);
		//patrol type & transport
		List<PatrolType> patrolTypes = PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session);
		String errorMsg = validatePatrolTypes(patrolTypes);
		if (errorMsg != null) {
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, errorMsg);
			return null;
		}
		id = addTypeTransportNodes(id, result, elements, patrolTypes);
		//patrol armed
		List<CyberTrackerId> armedIds = ElementsUtil.buildBooleanElements(elements);
		id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_IsArmed, RESULT_ARMED, armedIds, false);

		List<Team> teams = PatrolHibernateManager.getActiveTeams(ca, session);
		List<CyberTrackerId> cyberTrackerIds = toCyberTrackerIds(elements, teams);
		id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Team, RESULT_TEAM, cyberTrackerIds, true);

		cyberTrackerIds.clear();
		List<Station> stations = PatrolHibernateManager.getActiveStations(ca, session);
		cyberTrackerIds.addAll(toCyberTrackerIds(elements, stations));
		id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Station, RESULT_STATION, cyberTrackerIds, true);

		cyberTrackerIds.clear();
		List<PatrolMandate> mandates = PatrolHibernateManager.getActiveMandates(ca, session);
		cyberTrackerIds.addAll(toCyberTrackerIds(elements, mandates));
		id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Mandate, RESULT_MANDATE, cyberTrackerIds, true);

		id = addNoteNextNode(id, result, elements, Messages.PatrolScreens_Objective, RESULT_OBJECTIVE, Patrol.MAX_OBJECTIVE_LENGTH);
		id = addNoteNextNode(id, result, elements, Messages.PatrolScreens_Comments, RESULT_COMMENTS, Patrol.MAX_COMMENT_LENGTH);

		//getting all members names
		List<Employee> employees = PatrolHibernateManager.getActiveEmployees(ca, session);
		List<String> members = new ArrayList<String>();
		List<String> memberTag0s = new ArrayList<String>();
		for (Employee i : employees) {
			members.add(i.getFullLabel());
			memberTag0s.add(SmartUtils.encodeHex(i.getUuid()));
		}
		List<CyberTrackerId> memberIds = ElementsUtil.addCustomElements(elements, members, memberTag0s);
		String filter = buildMembersFilter(id.getNodeId(), memberIds, members);
		if (filter != null) {
			filter = SmartUtils.encodeHex(filter.getBytes());
		}
		
		id = addMembersNode(id, result, memberIds);
		id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Leader, RESULT_LEADER, memberIds, filter);
		Node leaderNode = result.screenNodes.get(result.screenNodes.size()-1);
		String pilotNodeId = id.getNodeId();
		id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Pilot, RESULT_PILOT, memberIds, filter);
		addNavigationFormula(leaderNode, builPilotFormula(patrolTypes), pilotNodeId, id.getNodeId());
		
		CyberTrackerProperties ctProps = CyberTrackerHibernateManager.getProperties(session);
		addTaskNode(id, result, elements, startId, dmRootId, ctProps.getWaypointTimer());
		result.rootId = id;
		return result;
	}

	/**
	 * @param name
	 * @param elements
	 * @return String id of newly created element
	 */
	private String createResultElement(String name, Elements elements) {
		CyberTrackerId resultId = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, name, resultId.getItemId());
		return resultId.getItemId();
	}

	private CyberTrackerId toNextScreen(Node node) {
		return toNextScreen(node, false);
	}
	
	private CyberTrackerId toNextScreen(Node node, boolean canSkip) {
		CyberTrackerId nextId = new CyberTrackerId();
		Control control2 = ScreensObjectFactory.getNavigationControl(node);
		control2.setTranslateNextScreenId(nextId.getNodeId());
		if (canSkip) {
			control2.setTranslateSkipScreenId(nextId.getNodeId());
		}
		return nextId;
	}

	private void applyFilter(Node node, String filter) {
		if (filter != null) {
			Control control7 = ScreensObjectFactory.getRadioMainControl(node);
			control7.setFilterEnabled("True"); //$NON-NLS-1$
			control7.setTranslateFilter(filter);
		}
	}
	
	private CyberTrackerId addSimpleNextRadioNode(CyberTrackerId id, ParolFilledDataContainer container, Elements elements, String name, String resultElName,  List<CyberTrackerId> ids, boolean canSkip) {
		String resultId = createResultElement(resultElName, elements);
		Node node = ctUtil.createRadioNode(id.getNodeId(), name, ids, resultId);
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultId, resultElName));
		return toNextScreen(node, canSkip);
	}

	private CyberTrackerId addSimpleNextRadioNode(CyberTrackerId id, ParolFilledDataContainer container, Elements elements, String name, String resultElName,  List<CyberTrackerId> ids, String filter) {
		String resultId = createResultElement(resultElName, elements);
		Node node = ctUtil.createRadioNode(id.getNodeId(), name, ids, resultId);
		applyFilter(node, filter);
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultId, resultElName));
		return toNextScreen(node);
	}
	
	private CyberTrackerId addNoteNextNode(CyberTrackerId id, ParolFilledDataContainer container, Elements elements, String name, String resultElName, int maxLength) {
		String resultId = createResultElement(resultElName, elements);
		Node node = screensFactory.createNodeNote(id.getNodeId(), name,  resultId);
		
		Control textControl = ScreensObjectFactory.getNoteMainControl(node);
		textControl.setMaxLength(maxLength);
		
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultId, resultElName));
		return toNextScreen(node, true);
	}

	private CyberTrackerId addStartScreen(CyberTrackerId id, ParolFilledDataContainer container, Elements elements) {
		String resultId = createResultElement(RESULT_PATROL_ID, elements);
		List<CyberTrackerId> ids = ElementsUtil.addCustomElements(elements, Messages.PatrolScreens_StartPatrol, Messages.PatrolScreens_ExitCyberTracker);
		Node node = ctUtil.createRadioNode(id.getNodeId(), Messages.PatrolScreens_Start_Title, ids, null, true);
		addUniqueAttrubute(node, resultId);
		addGpsConfiguration(node, 0);
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultId, RESULT_PATROL_ID));

		Node pwdNode = screensFactory.createNodePassword(ids.get(1).getNodeId(), Messages.PatrolScreens_Exit_Title);
		container.screenNodes.add(pwdNode);
		Control control2 = ScreensObjectFactory.getNavigationControl(pwdNode);
		control2.setShowNext(ICyberTrackerConstants.STR_FALSE);
		
		return ids.get(0);
		
	}

	private String validatePatrolTypes(List<PatrolType> pTypes) {
		if (pTypes == null || pTypes.isEmpty())
			return Messages.PatrolScreensUtil_Error_TypesNotSet;
		for (PatrolType patrolType : pTypes) {
			boolean invalid = true;
			if (patrolType.getTransportTypes() != null) {
				for (PatrolTransportType transportType : patrolType.getTransportTypes()) {
					if (transportType.getIsActive()) {
						//there is an active transport for this patrol type -> it is valid
						invalid = false;
						break;
					}
				}
			}
			
			if (invalid) {
				return MessageFormat.format(Messages.PatrolScreensUtil_Error_TransportNotSet, patrolType.getType().getGuiName());
			}
		}
		return null;
	}

	private List<PatrolTransportType> getActiveTransportTypes(PatrolType patrolType) {
		List<PatrolTransportType> list = new ArrayList<PatrolTransportType>();
		if (patrolType.getTransportTypes() == null)
			return list;
		for (PatrolTransportType transportType : patrolType.getTransportTypes()) {
			if (transportType.getIsActive()) {
				list.add(transportType);
			}
		}
		return list;
	}
	
	private CyberTrackerId addTypeTransportNodes(CyberTrackerId id, ParolFilledDataContainer container, Elements elements, List<PatrolType> pTypes) {
		List<String> types = new ArrayList<String>();
		List<String> tag0Types = new ArrayList<String>();
		for (PatrolType patrolType : pTypes) {
			types.add(patrolType.getType().getGuiName());
			tag0Types.add(patrolType.getType().name());
		}
		List<CyberTrackerId> typeIds = ElementsUtil.addCustomElements(elements, types, tag0Types);
		String resultTypeElemId = createResultElement(RESULT_PATROL_TYPE, elements);
		Node node = ctUtil.createRadioNode(id.getNodeId(), Messages.PatrolScreens_PatrolType, typeIds, resultTypeElemId, true);
		Control control7 = ScreensObjectFactory.getRadioMainControl(node);
		control7.setResultGlobalValue(GLOBAL_PATROL_TYPE);
		container.screenNodes.add(node);
		container.resultElements.add(new IdNamePair(resultTypeElemId, RESULT_PATROL_TYPE));
		CyberTrackerId nextId = new CyberTrackerId();
		String resultTransportId = createResultElement(RESULT_TRANSPORT, elements);
		container.resultElements.add(new IdNamePair(resultTransportId, RESULT_TRANSPORT));
		for (int i = 0; i < pTypes.size(); i++) {
			List<CyberTrackerId> trIds = toCyberTrackerIds(elements, getActiveTransportTypes(pTypes.get(i)));
			node = ctUtil.createRadioNode(typeIds.get(i).getNodeId(), types.get(i), trIds, resultTransportId);
			container.screenNodes.add(node);
			Control control2 = ScreensObjectFactory.getNavigationControl(node);
			control2.setTranslateNextScreenId(nextId.getNodeId());
		}
		return nextId;
	}

	private CyberTrackerId addMembersNode(CyberTrackerId id, ParolFilledDataContainer container, List<CyberTrackerId> memberIds) {
		List<String> values = ctUtil.listItemIds(memberIds);
		String trElements = ctUtil.translateElements(memberIds);
		String trLinks = ctUtil.translateLinks(memberIds, false);
		Node node = screensFactory.createNodeChecklist(id.getNodeId(), Messages.PatrolScreens_Members, values, trElements, trLinks, 1);
		container.screenNodes.add(node);
		return toNextScreen(node);
	}

	private void addTaskNode(CyberTrackerId id, ParolFilledDataContainer container, Elements elements, CyberTrackerId startId, CyberTrackerId dmRootId, Integer timer) {
		CyberTrackerId resumeId = new CyberTrackerId();
		List<CyberTrackerId> resScrIds = ElementsUtil.addCustomElements(elements, Messages.PatrolScreens_ResumePatrol);
		List<String> resScrValues = ctUtil.listItemIds(resScrIds);
		String resScrTrElements = ctUtil.translateElements(resScrIds);
		StringBuilder resScrLinks = new StringBuilder();
		// "Resume Patrol" leads to "Next Task" screen
		resScrLinks.append(resScrIds.get(0).getItemTranslatedId()).append(id.getNodeTranslatedId());
		Node resumeNode = screensFactory.createNodeRadio(resumeId.getNodeId(), Messages.PatrolScreens_Paused, resScrValues, resScrTrElements, resScrLinks.toString(), null);
		addGpsConfiguration(resumeNode, 0);
		
		CyberTrackerId confId = new CyberTrackerId();
		Node confirmNode = screensFactory.createNodeMsgText(confId.getNodeId(), Messages.PatrolScreens_Confirm, Messages.PatrolScreens_ConfirmMessage);
		//disable next button, enable save button,navigate on save to start point
		Control control2 = ScreensObjectFactory.getNavigationControl(confirmNode);
		control2.setShowNext("False"); //$NON-NLS-1$
		control2.setShowMajor("True"); //$NON-NLS-1$
		control2.setTranslateMajorScreenId(startId.getNodeId());
		
		List<CyberTrackerId> ids = ElementsUtil.addCustomElements(elements, Messages.PatrolScreens_NewObservation, Messages.PatrolScreens_PausePatrol, Messages.PatrolScreens_EndPatrol);
		List<String> values = ctUtil.listItemIds(ids);
		String trElements = ctUtil.translateElements(ids);
		//custom translate links logic
		StringBuilder links = new StringBuilder();
		// "Make observations" leads to datamodel root
		links.append(ids.get(0).getItemTranslatedId()).append(dmRootId.getNodeTranslatedId());
		// "Pause Patrol (Rest)" leads to "Paused" screen
		links.append(ids.get(1).getItemTranslatedId()).append(resumeId.getNodeTranslatedId());
		// "End Patrol" leads to confirmation screen
		links.append(ids.get(2).getItemTranslatedId()).append(confId.getNodeTranslatedId());
		Node node = screensFactory.createNodeRadio(id.getNodeId(), Messages.PatrolScreens_NextTask, values, trElements, links.toString(), null);
		addGpsConfiguration(node, timer);
		container.screenNodes.add(node);
		container.screenNodes.add(resumeNode);
		container.screenNodes.add(confirmNode);
	}

	public static List<CyberTrackerId> toCyberTrackerIds(Elements elements, List<? extends SimpleListItem> items) {
		List<String> labelValues = new ArrayList<String>();
		List<String> tag0Values = new ArrayList<String>();
		for (SimpleListItem i : items) {
			labelValues.add(i.getName());
			tag0Values.add(SmartUtils.encodeHex(i.getUuid()));
		}
		return ElementsUtil.addCustomElements(elements, labelValues, tag0Values);
	}

	private String buildMembersFilter(String memberNodeId, List<CyberTrackerId> memberIds, List<String> memberNames) {
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
			e.printStackTrace();
		}
		return null;
	}
	
	private String builPilotFormula(List<PatrolType> patrolTypes) {
		String result = ""; //$NON-NLS-1$
		for (int i = 0; i < patrolTypes.size(); i++) {
			patrolTypes.get(i).getType();
			switch (patrolTypes.get(i).getType()) {
			case AIR:
			case MARINE:
				if (!result.isEmpty())
					result += " || "; //$NON-NLS-1$
				result += GLOBAL_PATROL_TYPE+"=="+String.valueOf(i+1); //$NON-NLS-1$
				break;
			default:
				break;
			}
		}
		return result;
	}
	
	private void addNavigationFormula(Node node, String formula, String successId, String failId) {
		Control formulaControl = screensFactory.createNavFormulaControl12(formula, failId, successId);
		node.getData().getControls().getControl().add(formulaControl);
	}

	private void addGpsConfiguration(Node node, Integer timer) {
		Control gpsConf = screensFactory.createConfigureGPSControl13(timer);
		node.getData().getControls().getControl().add(gpsConf);
	}

	private void addUniqueAttrubute(Node node, String resultElementId) {
		Control uniqueAttr = screensFactory.createUniqueAttrubuteControl12(resultElementId);
		node.getData().getControls().getControl().add(uniqueAttr);
	}
	
}
