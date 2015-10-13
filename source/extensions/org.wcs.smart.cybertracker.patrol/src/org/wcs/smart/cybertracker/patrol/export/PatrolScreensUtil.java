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
package org.wcs.smart.cybertracker.patrol.export;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.MetaExportResult;
import org.wcs.smart.cybertracker.export.MetaExportResult.IdNamePair;
import org.wcs.smart.cybertracker.export.ScreensObjectFactory;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.dataentry.model.ScreenOptionUuid;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.meta.PatrolScreenOptionMeta;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.PatrolType.Type;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Util for creating patrol screens for CyberTracker.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolScreensUtil extends ScreensUtil {
	//TODO: DO NOT USE Messages from main plugin!!!!!!!!!!!!!!!!
	
	private static final String GLOBAL_PATROL_TYPE = "GLOBAL_PATROL_TYPE"; //$NON-NLS-1$

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
	
	public static final String DATATYPE_PATROL = "patrol"; //$NON-NLS-1$

	private CyberTrackerUtil ctUtil;

	public PatrolScreensUtil(CyberTrackerUtil ctUtil) {
		super(ctUtil);
		this.ctUtil = ctUtil;
	}
	
	/**
	 * @param screens
	 * @param element
	 * @return root id
	 */
	@Override
	public MetaExportResult buildMetaNodes(Elements elements, CyberTrackerId dmRootId, Session session) {
		registerDatatype(elements, DATATYPE_PATROL);
		MetaExportResult result = new MetaExportResult();
		List<CyberTrackerId> cyberTrackerIds;
		ScreenOption so;
		//start node
		CyberTrackerId startId = new CyberTrackerId();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		Map<PatrolScreenOptionMeta, ScreenOption> screenOptions = PatrolHibernateManager.getScreenOptions(ca, session);
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
		so = screenOptions.get(PatrolScreenOptionMeta.ARMED);
		if (so == null || so.isVisible()) {
			List<CyberTrackerId> armedIds = ElementsUtil.buildBooleanElements(elements);
			id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_IsArmed, RESULT_ARMED, armedIds, false);
		} else {
			boolean value = Boolean.TRUE.equals(so.getBooleanValue());
			String elId = (new CyberTrackerId()).getItemId();
			ElementsUtil.addElementsItem(elements, "", elId, Boolean.toString(value)); //$NON-NLS-1$
			result.defaultValues.add(createDefaultResultElement(RESULT_ARMED, elements, elId));
		}

		so = screenOptions.get(PatrolScreenOptionMeta.TEAM);
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
			ElementsUtil.addElementsItem(elements, ctUtil.getName(team), elId, UuidUtils.uuidToString(team.getUuid()));
			result.defaultValues.add(createDefaultResultElement(RESULT_TEAM, elements, elId));
		}

		so = screenOptions.get(PatrolScreenOptionMeta.STATION);
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
			ElementsUtil.addElementsItem(elements, ctUtil.getName(station), elId, UuidUtils.uuidToString(station.getUuid()));
			result.defaultValues.add(createDefaultResultElement(RESULT_STATION, elements, elId));
		}

		so = screenOptions.get(PatrolScreenOptionMeta.MANDATE);
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
			ElementsUtil.addElementsItem(elements, ctUtil.getName(mandate), elId, UuidUtils.uuidToString(mandate.getUuid()));
			result.defaultValues.add(createDefaultResultElement(RESULT_MANDATE, elements, elId));
		}

		so = screenOptions.get(PatrolScreenOptionMeta.OBJECTIVE);
		if (so == null || so.isVisible()) {
			id = addNoteNextNode(id, result, elements, Messages.PatrolScreens_Objective, RESULT_OBJECTIVE, Patrol.MAX_OBJECTIVE_LENGTH);
		} else {
			result.defaultValues.add(createDefaultResultElement(RESULT_OBJECTIVE, elements, so.getStringValue()));
		}

		so = screenOptions.get(PatrolScreenOptionMeta.COMMENT);
		if (so == null || so.isVisible()) {
			id = addNoteNextNode(id, result, elements, Messages.PatrolScreens_Comments, RESULT_COMMENTS, Patrol.MAX_COMMENT_LENGTH);
		} else {
			result.defaultValues.add(createDefaultResultElement(RESULT_COMMENTS, elements, so.getStringValue()));
		}

		List<Employee> employees = HibernateManager.getActiveEmployees(ca, session);
		Collections.sort(employees, new Comparator<Employee>() {
			@Override
			public int compare(Employee e1, Employee e2) {
				return Collator.getInstance().compare(SmartLabelProvider.getShortLabel(e1), SmartLabelProvider.getShortLabel(e2));
			}
		});
		so = screenOptions.get(PatrolScreenOptionMeta.MEMBERS);
		if (so == null || so.isVisible()) {
			//getting all members names
			//displaying all screens
			List<CyberTrackerId> memberIds = new ArrayList<CyberTrackerId>();
			List<String> members = new ArrayList<String>();
			for (Employee i : employees) {
				members.add(SmartLabelProvider.getShortLabel(i));
				CyberTrackerId mctid = new CyberTrackerId();
				ElementsUtil.addElementsItem(elements, SmartLabelProvider.getShortLabel(i), mctid.getItemId(), UuidUtils.uuidToString(i.getUuid()), ElementsUtil.MEMBER_ELEMENT_TAG);
				memberIds.add(mctid);
				
			}
			
			String filter = buildMembersFilter(id.getNodeId(), memberIds, members);
			if (filter != null) {
				filter = SmartUtils.encodeGeometry(filter.getBytes());
			}
			
			id = addMembersNode(id, result, memberIds);
			id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Leader, RESULT_LEADER, memberIds, filter);
			
			id = addPilotScreen(id, result, elements, screenOptions, memberIds, patrolTypes, filter);
		} else {
			//adding default members
			ScreenOption leader_so = screenOptions.get(PatrolScreenOptionMeta.LEADER);
			ScreenOption pilot_so = screenOptions.get(PatrolScreenOptionMeta.PILOT);
			List<CyberTrackerId> memberIds = new ArrayList<CyberTrackerId>();
			CyberTrackerId leaderCtId = null;
			CyberTrackerId pilotCtId = null;
			for (ScreenOptionUuid sou : so.getUuidList()) {
				for (Employee e : employees) {
					if (sou.getUuidValue().equals(e.getUuid())) {
						CyberTrackerId mctid = new CyberTrackerId();
						ElementsUtil.addElementsItem(elements, SmartLabelProvider.getShortLabel(e), mctid.getItemId(), UuidUtils.uuidToString(e.getUuid()), ElementsUtil.MEMBER_ELEMENT_TAG);
						result.defaultValues.add(mctid.getItemId());
						memberIds.add(mctid);
						if (leader_so.getUuidValue() != null && leader_so.getUuidValue().equals(e.getUuid())) {
							leaderCtId = mctid;
						}
						if (pilot_so.getUuidValue() != null && pilot_so.getUuidValue().equals(e.getUuid())) {
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
		
		addTaskNode(id, result, elements, startId, dmRootId, ctProps);
		result.rootId = id;
		return result;
	}

	private void addTaskNode(CyberTrackerId id, MetaExportResult container, Elements elements, CyberTrackerId startId, CyberTrackerId dmRootId, CyberTrackerProperties ctProps) {
		List<String> nextTaskOptions = new ArrayList<String>();
		List<CyberTrackerId> nodeIds = new ArrayList<CyberTrackerId>();
		
		nextTaskOptions.add(Messages.PatrolScreens_NewObservation);
		nodeIds.add(dmRootId);
		
		nextTaskOptions.add(Messages.PatrolScreens_EndPatrol);
		nodeIds.add(createEndTripNodes(container, startId, Messages.PatrolScreens_ConfirmMessage));
		
		if (ctProps.isCanPause()) {
			nextTaskOptions.add(Messages.PatrolScreens_PausePatrol);
			PauseNodesLabels labels = new PauseNodesLabels();
			labels.resumeOption = Messages.PatrolScreens_ResumePatrol;
			labels.resumeScreenTitle = Messages.PatrolScreens_Paused;
			nodeIds.add(createPauseTripNodes(container, elements, id, ctProps, labels));
		}
		
		buildNextTaskNode(id, container, elements, nextTaskOptions, nodeIds, ctProps);
	}
	
	private CyberTrackerId addPilotScreen(CyberTrackerId id, MetaExportResult container, Elements elements, Map<PatrolScreenOptionMeta, ScreenOption> screenOptions, List<CyberTrackerId> memberIds, List<PatrolType> patrolTypes, String filter) {
		//TYPE is visible						- PILOT displayed with navigation formula
		//TYPE is not visible (set to GROUND)	- PILOT in not displayed
		//TYPE is not visible (set to !GROUND)	- PILOT displayed without navigation formula
		ScreenOption type_so = screenOptions.get(PatrolScreenOptionMeta.TYPE);
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
	
	private CyberTrackerId addStartScreen(CyberTrackerId id, MetaExportResult container, Elements elements, CyberTrackerProperties ctProps) {
		StartScreenLabels labels = new StartScreenLabels();
		labels.startItemLabel = Messages.PatrolScreens_StartPatrol;
		labels.beginTitle = Messages.PatrolScreens_Begin_Title;
		labels.beginItemLabel = Messages.PatrolScreens_Begin;
		return addStartScreen(id, container, elements, ctProps, labels);
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
	
	private CyberTrackerId addTypeTransportNodes(CyberTrackerId id, MetaExportResult container, Elements elements, List<PatrolType> pTypes, Map<PatrolScreenOptionMeta, ScreenOption> screenOptions, Session session) {
		ScreenOption typeOption = screenOptions.get(PatrolScreenOptionMeta.TYPE);
		if (typeOption == null || typeOption.isVisible()) {
			List<String> types = new ArrayList<String>();
			List<String> tag0Types = new ArrayList<String>();
			for (PatrolType patrolType : pTypes) {
				types.add(patrolType.getType().getGuiName(Locale.getDefault()));
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
			
			ScreenOption trOption = screenOptions.get(PatrolScreenOptionMeta.TRANSPORT);
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
				ElementsUtil.addElementsItem(elements, ctUtil.getName(transport), trElId, UuidUtils.uuidToString(transport.getUuid()));
				container.defaultValues.add(createDefaultResultElement(RESULT_TRANSPORT, elements, trElId));
				return id;
			}
			
		}
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
	
}
