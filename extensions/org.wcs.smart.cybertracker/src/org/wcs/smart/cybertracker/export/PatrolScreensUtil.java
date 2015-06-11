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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
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
import org.wcs.smart.patrol.model.PatrolType.Type;
import org.wcs.smart.patrol.model.ScreenOption;
import org.wcs.smart.patrol.model.ScreenOption.ScreenOptionMeta;
import org.wcs.smart.patrol.model.ScreenOptionUuid;
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
	public static final String RESULT_PATROL_START_DATE = "#PatrolStartDate"; //$NON-NLS-1$
	public static final String RESULT_PATROL_START_TIME = "#PatrolStartTime"; //$NON-NLS-1$
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
	public static final String RESULT_DEFAULT_ATTRIBUTE_VALUES = "#DefaultAttributeValues"; //$NON-NLS-1$
	public static final String RESULT_DEFAULT_PATROL_VALUES = "#DefaultPatrolValues"; //$NON-NLS-1$

	public static final String RESULT_PHOTO = "#Photo"; //$NON-NLS-1$
	
	/**
	 * Contains data filled by {@link PatrolScreensUtil}
	 * @author elitvin
	 * @since 1.0.0
	 */
	public static class ParolFilledDataContainer {
		public List<Node> screenNodes = new ArrayList<Node>();
		public List<IdNamePair> resultElements = new ArrayList<IdNamePair>();
		public CyberTrackerId rootId = null;
		protected List<String> defaultValues = new ArrayList<String>();
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
		List<CyberTrackerId> cyberTrackerIds;
		ScreenOption so;
		//start node
		CyberTrackerId startId = new CyberTrackerId();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		Map<ScreenOptionMeta, ScreenOption> screenOptions = PatrolHibernateManager.getScreenOptions(ca, session);
		CyberTrackerProperties ctProps = CyberTrackerHibernateManager.getProperties(session);
		CyberTrackerId id = addStartScreen(startId, result, elements, ctProps);
		//patrol type & transport
		List<PatrolType> patrolTypes = PatrolHibernateManager.getActivePatrolTypes(ca, session);
		String errorMsg = validatePatrolTypes(patrolTypes);
		if (errorMsg != null) {
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, errorMsg, null);
			return null;
		}
		id = addTypeTransportNodes(id, result, elements, patrolTypes, screenOptions, session);
		//patrol armed
		so = screenOptions.get(ScreenOptionMeta.ARMED);
		if (so == null || so.isVisible()) {
			List<CyberTrackerId> armedIds = ElementsUtil.buildBooleanElements(elements);
			id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_IsArmed, RESULT_ARMED, armedIds, false);
		} else {
			boolean value = Boolean.TRUE.equals(so.getBooleanValue());
			String elId = (new CyberTrackerId()).getItemId();
			ElementsUtil.addElementsItem(elements, "", elId, Boolean.toString(value)); //$NON-NLS-1$
			result.defaultValues.add(createDefaultResultElement(RESULT_ARMED, elements, elId));
		}

		so = screenOptions.get(ScreenOptionMeta.TEAM);
		if (so == null || so.isVisible()) {
			List<Team> teams = PatrolHibernateManager.getActiveTeams(ca, session);
			cyberTrackerIds = toCyberTrackerIds(elements, teams);
			id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Team, RESULT_TEAM, cyberTrackerIds, true);
		} else if (so.getUuidValue() != null) {
			Team team = CyberTrackerHibernateManager.fetchByUuid(Team.class, so.getUuidValue(), session);
			if (team == null) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Team, null);
				return null;
			}
			String elId = (new CyberTrackerId()).getItemId();
			ElementsUtil.addElementsItem(elements, ctUtil.getName(team), elId, SmartUtils.encodeHex(team.getUuid()));
			result.defaultValues.add(createDefaultResultElement(RESULT_TEAM, elements, elId));
		}

		so = screenOptions.get(ScreenOptionMeta.STATION);
		if (so == null || so.isVisible()) {
			List<Station> stations = PatrolHibernateManager.getActiveStations(ca, session);
			cyberTrackerIds = toCyberTrackerIds(elements, stations);
			id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Station, RESULT_STATION, cyberTrackerIds, true);
		} else if (so.getUuidValue() != null) {
			Station station = CyberTrackerHibernateManager.fetchByUuid(Station.class, so.getUuidValue(), session);
			if (station == null) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Station, null);
				return null;
			}
			String elId = (new CyberTrackerId()).getItemId();
			ElementsUtil.addElementsItem(elements, ctUtil.getName(station), elId, SmartUtils.encodeHex(station.getUuid()));
			result.defaultValues.add(createDefaultResultElement(RESULT_STATION, elements, elId));
		}

		so = screenOptions.get(ScreenOptionMeta.MANDATE);
		if (so == null || so.isVisible()) {
			List<PatrolMandate> mandates = PatrolHibernateManager.getActiveMandates(ca, session);
			cyberTrackerIds = toCyberTrackerIds(elements, mandates);
			id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Mandate, RESULT_MANDATE, cyberTrackerIds, true);
			
		} else if (so.getUuidValue() != null) {
			PatrolMandate mandate = CyberTrackerHibernateManager.fetchByUuid(PatrolMandate.class, so.getUuidValue(), session);
			if (mandate == null) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Mandate, null);
				return null;
			}
			String elId = (new CyberTrackerId()).getItemId();
			ElementsUtil.addElementsItem(elements, ctUtil.getName(mandate), elId, SmartUtils.encodeHex(mandate.getUuid()));
			result.defaultValues.add(createDefaultResultElement(RESULT_MANDATE, elements, elId));
		}

		so = screenOptions.get(ScreenOptionMeta.OBJECTIVE);
		if (so == null || so.isVisible()) {
			id = addNoteNextNode(id, result, elements, Messages.PatrolScreens_Objective, RESULT_OBJECTIVE, Patrol.MAX_OBJECTIVE_LENGTH);
		} else {
			result.defaultValues.add(createDefaultResultElement(RESULT_OBJECTIVE, elements, so.getStringValue()));
		}

		so = screenOptions.get(ScreenOptionMeta.COMMENT);
		if (so == null || so.isVisible()) {
			id = addNoteNextNode(id, result, elements, Messages.PatrolScreens_Comments, RESULT_COMMENTS, Patrol.MAX_COMMENT_LENGTH);
		} else {
			result.defaultValues.add(createDefaultResultElement(RESULT_COMMENTS, elements, so.getStringValue()));
		}

		List<Employee> employees = PatrolHibernateManager.getActiveEmployees(ca, session);
		Collections.sort(employees, new Comparator<Employee>() {
			@Override
			public int compare(Employee e1, Employee e2) {
				return Collator.getInstance().compare(e1.getFullLabel(), e2.getFullLabel());
			}
		});
		so = screenOptions.get(ScreenOptionMeta.MEMBERS);
		if (so == null || so.isVisible()) {
			//getting all members names
			//displaying all screens
			List<CyberTrackerId> memberIds = new ArrayList<CyberTrackerId>();
			List<String> members = new ArrayList<String>();
			for (Employee i : employees) {
				members.add(i.getFullLabel());
				CyberTrackerId mctid = new CyberTrackerId();
				ElementsUtil.addElementsItem(elements, i.getFullLabel(), mctid.getItemId(), SmartUtils.encodeHex(i.getUuid()), ElementsUtil.MEMBER_ELEMENT_TAG);
				memberIds.add(mctid);
				
			}
			
			String filter = buildMembersFilter(id.getNodeId(), memberIds, members);
			if (filter != null) {
				filter = SmartUtils.encodeHex(filter.getBytes());
			}
			
			id = addMembersNode(id, result, memberIds);
			id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Leader, RESULT_LEADER, memberIds, filter);
			
			id = addPilotScreen(id, result, elements, screenOptions, memberIds, patrolTypes, filter);
		} else {
			//adding default members
			ScreenOption leader_so = screenOptions.get(ScreenOptionMeta.LEADER);
			ScreenOption pilot_so = screenOptions.get(ScreenOptionMeta.PILOT);
			List<CyberTrackerId> memberIds = new ArrayList<CyberTrackerId>();
			CyberTrackerId leaderCtId = null;
			CyberTrackerId pilotCtId = null;
			for (ScreenOptionUuid sou : so.getUuidList()) {
				for (Employee e : employees) {
					if (Arrays.equals(sou.getUuidValue(), e.getUuid())) {
						CyberTrackerId mctid = new CyberTrackerId();
						ElementsUtil.addElementsItem(elements, e.getFullLabel(), mctid.getItemId(), SmartUtils.encodeHex(e.getUuid()), ElementsUtil.MEMBER_ELEMENT_TAG);
						result.defaultValues.add(mctid.getItemId());
						memberIds.add(mctid);
						if (leader_so.getUuidValue() != null && Arrays.equals(leader_so.getUuidValue(), e.getUuid())) {
							leaderCtId = mctid;
						}
						if (pilot_so.getUuidValue() != null && Arrays.equals(pilot_so.getUuidValue(), e.getUuid())) {
							pilotCtId = mctid;
						}
					}
				}
			}
			
			if (leader_so == null || leader_so.isVisible()) {
				id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Leader, RESULT_LEADER, memberIds, false);
			} else {
				if (leaderCtId == null) {
					CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Leader, null);
					return null;
				}
				result.defaultValues.add(createDefaultResultElement(RESULT_LEADER, elements, leaderCtId.getItemId()));
			}

			if (pilot_so == null || pilot_so.isVisible()) {
				id = addPilotScreen(id, result, elements, screenOptions, memberIds, patrolTypes, null);
			} else {
				if (pilotCtId == null) {
					CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Pilot, null);
					return null;
				}
				result.defaultValues.add(createDefaultResultElement(RESULT_PILOT, elements, pilotCtId.getItemId()));
			}
			
		}
		
		StringBuilder defaults = new StringBuilder();
		for (Iterator<String> i = result.defaultValues.iterator(); i.hasNext();) {
			defaults.append(i.next());
			if (i.hasNext())
				defaults.append(ICyberTrackerConstants.ATTRIBUTE_DEFAULT_VALUES_SEPATATOR);
		}
		addTaskNode(id, result, elements, startId, dmRootId, ctProps, defaults.toString());
		result.rootId = id;
		return result;
	}

	private CyberTrackerId addPilotScreen(CyberTrackerId id, ParolFilledDataContainer container, Elements elements, Map<ScreenOptionMeta, ScreenOption> screenOptions, List<CyberTrackerId> memberIds, List<PatrolType> patrolTypes, String filter) {
		//TYPE is visible						- PILOT displayed with navigation formula
		//TYPE is not visible (set to GROUND)	- PILOT in not displayed
		//TYPE is not visible (set to !GROUND)	- PILOT displayed without navigation formula
		ScreenOption type_so = screenOptions.get(ScreenOptionMeta.TYPE);
		if (type_so == null || type_so.isVisible()) {
			String pilotNodeId = id.getNodeId();
			id = addSimpleNextRadioNode(id, container, elements, Messages.PatrolScreens_Pilot, RESULT_PILOT, memberIds, filter);
			//NOTE: if previous screen is transport than we need to update several screens with formula
			for (int i = container.screenNodes.size()-2; i >= 0; i--) {
				//need to change all prev screens that refer to this screen as their "next screen"
				Node prevNode = container.screenNodes.get(i);
				Control control2 = ScreensObjectFactory.getNavigationControl(prevNode);
				if (pilotNodeId.equals(control2.getTranslateNextScreenId())) {
					addNavigationFormula(prevNode, builPilotFormula(patrolTypes), pilotNodeId, id.getNodeId());
				} else {
					break;
				}
			}
			return id;
		} else if (Type.GROUND.name().equals(type_so.getStringValue())) {
			return id;
		} else {
			return addSimpleNextRadioNode(id, container, elements, Messages.PatrolScreens_Pilot, RESULT_PILOT, memberIds, filter);
		}
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

	private String createDefaultResultElement(String name, Elements elements, String defaultValue) {
		CyberTrackerId resultId = new CyberTrackerId();
		ElementsUtil.addElementsItem(elements, name, resultId.getItemId(), null, null, defaultValue);
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
			//we should be here only for radio nodes if we want to allow user press "Next" without selecting anything
			Control control7 = ScreensObjectFactory.getRadioMainControl(node);
			control7.setRadioBlockNext(ICyberTrackerConstants.STR_FALSE);
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
		return toNextScreen(node);
	}

	private CyberTrackerId addStartScreen(CyberTrackerId id, ParolFilledDataContainer container, Elements elements, CyberTrackerProperties ctProps) {
		List<CyberTrackerId> ids = ElementsUtil.addCustomElements(elements, Messages.PatrolScreens_StartPatrol, Messages.PatrolScreens_ExitCyberTracker);
		Node nodeMain = ctUtil.createRadioNode(id.getNodeId(), Messages.PatrolScreens_Start_Title, ids, null, true);
		container.screenNodes.add(nodeMain);

		List<CyberTrackerId> idsBegin = ElementsUtil.addCustomElements(elements, Messages.PatrolScreens_Begin);
		Node nodeBegin = ctUtil.createRadioNode(ids.get(0).getNodeId(), Messages.PatrolScreens_Begin_Title, idsBegin, null, true);
		container.screenNodes.add(nodeBegin);
		
		String resultId = createResultElement(RESULT_PATROL_ID, elements);
		addUniqueAttrubute(nodeBegin, resultId);
		addGpsConfiguration(nodeBegin, ctProps, 0);
		String resultDateId = createResultElement(RESULT_PATROL_START_DATE, elements);
		String resultTimeId = createResultElement(RESULT_PATROL_START_TIME, elements);
		addStartTimeAttrubute(nodeBegin, resultDateId, resultTimeId);
		//if "Use GPS Time" option is enabled than "Snap Date & Time" control needs time from GPS to calculate offset from device time,
		//but it doesn't launch GPS reading itself, so we need to add "GPS" control (see ticket #1304 for details)
		addGPSControl(nodeBegin);
		addGPSRequiredWarning(nodeBegin);
		container.resultElements.add(new IdNamePair(resultId, RESULT_PATROL_ID));
		container.resultElements.add(new IdNamePair(resultDateId, RESULT_PATROL_START_DATE));
		container.resultElements.add(new IdNamePair(resultTimeId, RESULT_PATROL_START_TIME));

		Node pwdNode = screensFactory.createNodePassword(ids.get(1).getNodeId(), Messages.PatrolScreens_Exit_Title);
		container.screenNodes.add(pwdNode);
		Control control2 = ScreensObjectFactory.getNavigationControl(pwdNode);
		control2.setShowNext(ICyberTrackerConstants.STR_FALSE);
		
		return idsBegin.get(0);
		
	}

	private String validatePatrolTypes(List<PatrolType> pTypes) {
		if (pTypes == null || pTypes.isEmpty())
			return Messages.PatrolScreensUtil_Error_TypesNotSet;
		for (Iterator<PatrolType> i = pTypes.iterator(); i.hasNext();) {
			PatrolType patrolType = i.next();
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
				i.remove();
			}
		}
		
		if (pTypes.isEmpty())
			return Messages.PatrolScreensUtil_Error_TransportNotSet;
		
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
	
	private CyberTrackerId addTypeTransportNodes(CyberTrackerId id, ParolFilledDataContainer container, Elements elements, List<PatrolType> pTypes, Map<ScreenOptionMeta, ScreenOption> screenOptions, Session session) {
		ScreenOption typeOption = screenOptions.get(ScreenOptionMeta.TYPE);
		if (typeOption == null || typeOption.isVisible()) {
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
		} else {
			Type value = typeOption.getStringValue() != null ? PatrolType.Type.valueOf(typeOption.getStringValue()) : PatrolType.Type.GROUND;
			String elId = (new CyberTrackerId()).getItemId();
			ElementsUtil.addElementsItem(elements, "", elId, value.name()); //$NON-NLS-1$
			container.defaultValues.add(createDefaultResultElement(RESULT_PATROL_TYPE, elements, elId));
			
			ScreenOption trOption = screenOptions.get(ScreenOptionMeta.TRANSPORT);
			if (trOption == null || trOption.isVisible()) {
				PatrolType pType = null;
				for (PatrolType pt : pTypes) {
					if (value.equals(pt.getType()))
						pType = pt;
				}
				List<CyberTrackerId> trIds = toCyberTrackerIds(elements, getActiveTransportTypes(pType));
				String resultTransportId = createResultElement(RESULT_TRANSPORT, elements);
				Node node = ctUtil.createRadioNode(id.getNodeId(), Messages.PatrolScreens_Transport, trIds, resultTransportId);
				container.screenNodes.add(node);
				CyberTrackerId nextId = new CyberTrackerId();
				Control control2 = ScreensObjectFactory.getNavigationControl(node);
				control2.setTranslateNextScreenId(nextId.getNodeId());
				return nextId;
			} else {
				PatrolTransportType transport = CyberTrackerHibernateManager.fetchByUuid(PatrolTransportType.class, trOption.getUuidValue(), session);
				if (transport == null) {
					CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Transport, null);
					return null;
				}
				String trElId = (new CyberTrackerId()).getItemId();
				ElementsUtil.addElementsItem(elements, ctUtil.getName(transport), trElId, SmartUtils.encodeHex(transport.getUuid()));
				container.defaultValues.add(createDefaultResultElement(RESULT_TRANSPORT, elements, trElId));
				return id;
			}
			
		}
	}

	private CyberTrackerId addMembersNode(CyberTrackerId id, ParolFilledDataContainer container, List<CyberTrackerId> memberIds) {
		List<String> values = ctUtil.listItemIds(memberIds);
		String trElements = ctUtil.translateElements(memberIds);
		String trLinks = ctUtil.translateLinks(memberIds, false);
		Node node = screensFactory.createNodeMultiList(id.getNodeId(), Messages.PatrolScreens_Members, values, trElements, trLinks, 1, false);
		container.screenNodes.add(node);
		return toNextScreen(node);
	}

	private void addTaskNode(CyberTrackerId id, ParolFilledDataContainer container, Elements elements, CyberTrackerId startId, CyberTrackerId dmRootId, CyberTrackerProperties ctProps, String defaultValues) {
		boolean canPause = ctProps.isCanPause();
		
		CyberTrackerId resumeId = new CyberTrackerId();
		List<CyberTrackerId> resScrIds = ElementsUtil.addCustomElements(elements, Messages.PatrolScreens_ResumePatrol);
		List<String> resScrValues = ctUtil.listItemIds(resScrIds);
		String resScrTrElements = ctUtil.translateElements(resScrIds);
		StringBuilder resScrLinks = new StringBuilder();
		// "Resume Patrol" leads to "Next Task" screen
		resScrLinks.append(resScrIds.get(0).getItemTranslatedId()).append(id.getNodeTranslatedId());
		Node resumeNode = screensFactory.createNodeRadio(resumeId.getNodeId(), Messages.PatrolScreens_Paused, resScrValues, resScrTrElements, resScrLinks.toString(), null);
		addGpsConfiguration(resumeNode, ctProps, 0);
		
		CyberTrackerId confId = new CyberTrackerId();
		Node confirmNode = screensFactory.createNodeMsgText(confId.getNodeId(), Messages.PatrolScreens_Confirm, Messages.PatrolScreens_ConfirmMessage);
		//disable next button, enable save button,navigate on save to start point
		Control control2 = ScreensObjectFactory.getNavigationControl(confirmNode);
		control2.setShowNext("False"); //$NON-NLS-1$
		control2.setShowMajor("True"); //$NON-NLS-1$
		control2.setTranslateMajorScreenId(startId.getNodeId());

		List<String> nextTaskOptions = new ArrayList<String>();
		nextTaskOptions.add(Messages.PatrolScreens_NewObservation);
		nextTaskOptions.add(Messages.PatrolScreens_EndPatrol);
		if (canPause) {
			nextTaskOptions.add(Messages.PatrolScreens_PausePatrol);
		}
		
		List<CyberTrackerId> ids = ElementsUtil.addCustomElements(elements, nextTaskOptions.toArray(new String[nextTaskOptions.size()]));
		List<String> values = ctUtil.listItemIds(ids);
		String trElements = ctUtil.translateElements(ids);
		//custom translate links logic
		StringBuilder links = new StringBuilder();
		// "Make observations" leads to datamodel root
		links.append(ids.get(0).getItemTranslatedId()).append(dmRootId.getNodeTranslatedId());
		// "End Patrol" leads to confirmation screen
		links.append(ids.get(1).getItemTranslatedId()).append(confId.getNodeTranslatedId());
		// "Pause Patrol (Rest)" leads to "Paused" screen
		if (canPause) {
			links.append(ids.get(2).getItemTranslatedId()).append(resumeId.getNodeTranslatedId());
		}
		Node node = screensFactory.createNodeRadio(id.getNodeId(), Messages.PatrolScreens_NextTask, values, trElements, links.toString(), null);
		if (defaultValues != null && !defaultValues.isEmpty()) {
			//adding default values
			CyberTrackerId defId = new CyberTrackerId();
			ElementsUtil.addElementsItem(elements, RESULT_DEFAULT_PATROL_VALUES, defId.getItemId());
			Control defaultAttr = screensFactory.createAttrubuteControl14(defId.getItemId(), false, defaultValues);
			ScreensObjectFactory.addControlToNode(node, defaultAttr);
		}
		
		CyberTrackerProperties properties = ctUtil.getCtProperties();
		control2 = ScreensObjectFactory.getNavigationControl(node);
		control2.setShowBack("False"); //$NON-NLS-1$
		if (properties.isShowEdit()) {
			control2.setShowEdit("True"); //$NON-NLS-1$
		}
		if (properties.isShowGPS()) {
			control2.setShowGPS("True"); //$NON-NLS-1$
		}
		
		addGpsConfiguration(node, ctProps);
		container.screenNodes.add(node);
		if (canPause) {
			container.screenNodes.add(resumeNode);
		}
		container.screenNodes.add(confirmNode);
	}

	public List<CyberTrackerId> toCyberTrackerIds(Elements elements, List<? extends NamedItem> items) {
		List<String> labelValues = new ArrayList<String>();
		List<String> tag0Values = new ArrayList<String>();
		for (NamedItem i : items) {
			labelValues.add(ctUtil.getName(i));
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
			CyberTrackerPlugIn.log(e.getMessage(), e);
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

	private void addGpsConfiguration(Node node, CyberTrackerProperties props) {
		addGpsConfiguration(node, props, null);
	}
	
	private void addGpsConfiguration(Node node, CyberTrackerProperties props, Integer timerOverride) {
		Control gpsConf = screensFactory.createConfigureGPSControl13(props);
		if (timerOverride != null) {
			gpsConf.setWaypointTimer(timerOverride);
		}
		node.getData().getControls().getControl().add(gpsConf);
	}
	
	private void addUniqueAttrubute(Node node, String resultElementId) {
		Control uniqueAttr = screensFactory.createAttrubuteControl14(resultElementId, true, null);
		ScreensObjectFactory.addControlToNode(node, uniqueAttr);
	}

	private void addStartTimeAttrubute(Node node, String resultDateId, String resultTimeId) {
		Control dtAttr = screensFactory.createSnapDateTimeControl15(resultDateId, resultTimeId);
		ScreensObjectFactory.addControlToNode(node, dtAttr);
	}
	
	private void addGPSControl(Node node) {
		Control gpsControl = screensFactory.createGPSControl16();
		ScreensObjectFactory.addControlToNode(node, gpsControl);
	}

	private void addGPSRequiredWarning(Node node) {
		Control msgControl = screensFactory.createBottomMemoControl17(Messages.PatrolScreens_Begin_GPSRequiredMessage);
		ScreensObjectFactory.addControlToNode(node, msgControl);
	}

}
