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
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter.JsonKey;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.MetaExportResult;
import org.wcs.smart.cybertracker.export.MetaExportResult.IdNamePair;
import org.wcs.smart.cybertracker.export.ScreensObjectFactory;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.export.StartScreensContent;
import org.wcs.smart.cybertracker.export.alert.AlertData;
import org.wcs.smart.cybertracker.export.data.CtDataKeyValueRecord;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.elements.Elements.List.Items.Item;
import org.wcs.smart.cybertracker.model.elements.GpsSightingAccuracy;
import org.wcs.smart.cybertracker.model.elements.GpsWaypointAccuracy;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
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
	
	private static final String GLOBAL_PATROL_TYPE = "GLOBAL_PATROL_TYPE"; //$NON-NLS-1$

	public static enum JsonPatrolKey {
		PATROL_TYPE("pt"), //$NON-NLS-1$
		TRANSPORT_TYPE("tt"), //$NON-NLS-1$
		MANDATE("pm"), //$NON-NLS-1$
		STATION("ps"), //$NON-NLS-1$
		TEAM("pt"); //$NON-NLS-1$
		
		public String key;
		
		private JsonPatrolKey(String key){
			this.key = key;
		}
	}
	
	public static final String RESULT_PATROL_TYPE = ScreensUtil.COMMON_PREFIX + "PatrolType"; //$NON-NLS-1$
	public static final String RESULT_TRANSPORT = ScreensUtil.COMMON_PREFIX + "PatrolTransport"; //$NON-NLS-1$
	public static final String RESULT_ARMED = ScreensUtil.COMMON_PREFIX + "Armed"; //$NON-NLS-1$
	public static final String RESULT_TEAM = ScreensUtil.COMMON_PREFIX + "Team"; //$NON-NLS-1$
	public static final String RESULT_STATION = ScreensUtil.COMMON_PREFIX + "Station"; //$NON-NLS-1$
	public static final String RESULT_MANDATE = ScreensUtil.COMMON_PREFIX + "Mandate"; //$NON-NLS-1$
	public static final String RESULT_OBJECTIVE = ScreensUtil.COMMON_PREFIX + "Objective"; //$NON-NLS-1$
	public static final String RESULT_COMMENTS = ScreensUtil.COMMON_PREFIX + "Comments"; //$NON-NLS-1$
	public static final String RESULT_LEADER = ScreensUtil.COMMON_PREFIX + "Leader"; //$NON-NLS-1$
	public static final String RESULT_PILOT = ScreensUtil.COMMON_PREFIX + "Pilot"; //$NON-NLS-1$
	
	public static final String END_PATROL_KEY = "SMART_EndPatrol"; //$NON-NLS-1$
	
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
	public MetaExportResult buildMetaNodes(Elements elements, CyberTrackerId dmRootId, Session session, CyberTrackerPropertiesProfile ctProps) {
		CyberTrackerId dataType = registerDatatype(elements, DATATYPE_PATROL);
		
		MetaExportResult result = new MetaExportResult();
		List<CyberTrackerId> cyberTrackerIds;
		ScreenOption so;
		//start node
		CyberTrackerId startId = new CyberTrackerId();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		Map<PatrolScreenOptionMeta, ScreenOption> screenOptions = PatrolHibernateManager.getScreenOptions(ca, session);
		ScreenOption so_type = screenOptions.get(PatrolScreenOptionMeta.TYPE);
		CyberTrackerId id = addStartScreen(startId, result, elements, ctProps, so_type, ca, session, dataType, DATATYPE_PATROL);
		//patrol type & transport
		List<PatrolType> patrolTypes = PatrolHibernateManager.getActivePatrolTypes(ca, session);
		String errorMsg = validatePatrolTypes(patrolTypes);
		if (errorMsg != null) {
			CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, errorMsg, null);
			return null;
		}
		id = addTypeTransportNodes(id, result, elements, ctProps, patrolTypes, screenOptions, session);
		//patrol armed
		so = screenOptions.get(PatrolScreenOptionMeta.ARMED);
		if (so == null || so.isVisible()) {
			List<CyberTrackerId> armedIds = ElementsUtil.buildBooleanElements(elements);
			id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_IsArmed, RESULT_ARMED, armedIds, false);
		} else {
			boolean value = Boolean.TRUE.equals(so.getBooleanValue());
			String elId = (new CyberTrackerId()).getItemId();
			Elements.List.Items.Item aValue = ElementsUtil.addElementsItem(elements, "", elId, Boolean.toString(value)); //$NON-NLS-1$
			aValue.setJsonId(Boolean.toString(value));
			result.defaultValues.add(createDefaultResultRecord(RESULT_ARMED, elements, aValue));
		}

		so = screenOptions.get(PatrolScreenOptionMeta.TEAM);
		if (so == null || so.isVisible()) {
			List<Team> teams = PatrolHibernateManager.getActiveTeams(ca, session);
			cyberTrackerIds = toCyberTrackerIds(elements, teams, JsonPatrolKey.TEAM.key);
			id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Team, RESULT_TEAM, cyberTrackerIds, true);
		} else if (so.getUuidValue() != null) {
			Team team = CyberTrackerHibernateManager.fetchByUuid(Team.class, so.getUuidValue(), session);
			if (team == null) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Team, null);
				return null;
			}
			String elId = (new CyberTrackerId()).getItemId();
			Elements.List.Items.Item teamValue = ElementsUtil.addElementsItem(elements, ctUtil.getName(team), elId, UuidUtils.uuidToString(team.getUuid()));
			teamValue.setJsonId(JsonPatrolKey.TEAM.key + CyberTrackerConfExporter.KEY_SEP + UuidUtils.uuidToString(team.getUuid()));
			result.defaultValues.add(createDefaultResultRecord(RESULT_TEAM, elements, teamValue));
		}

		so = screenOptions.get(PatrolScreenOptionMeta.STATION);
		if (so == null || so.isVisible()) {
			List<Station> stations = PatrolHibernateManager.getActiveStations(ca, session);
			cyberTrackerIds = toCyberTrackerIds(elements, stations, JsonPatrolKey.STATION.key);
			id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Station, RESULT_STATION, cyberTrackerIds, true);
		} else if (so.getUuidValue() != null) {
			Station station = CyberTrackerHibernateManager.fetchByUuid(Station.class, so.getUuidValue(), session);
			if (station == null) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Station, null);
				return null;
			}
			String elId = (new CyberTrackerId()).getItemId();
			Elements.List.Items.Item stValue = ElementsUtil.addElementsItem(elements, ctUtil.getName(station), elId, UuidUtils.uuidToString(station.getUuid()));
			stValue.setJsonId(JsonPatrolKey.STATION.key + CyberTrackerConfExporter.KEY_SEP + UuidUtils.uuidToString(station.getUuid()));
			result.defaultValues.add(createDefaultResultRecord(RESULT_STATION, elements, stValue));
		}

		so = screenOptions.get(PatrolScreenOptionMeta.MANDATE);
		if (so == null || so.isVisible()) {
			List<PatrolMandate> mandates = PatrolHibernateManager.getActiveMandates(ca, session);
			cyberTrackerIds = toCyberTrackerIds(elements, mandates, JsonPatrolKey.MANDATE.key);
			id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Mandate, RESULT_MANDATE, cyberTrackerIds, true);
			
		} else if (so.getUuidValue() != null) {
			PatrolMandate mandate = CyberTrackerHibernateManager.fetchByUuid(PatrolMandate.class, so.getUuidValue(), session);
			if (mandate == null) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Mandate, null);
				return null;
			}
			String elId = (new CyberTrackerId()).getItemId();
			Elements.List.Items.Item mndValue = ElementsUtil.addElementsItem(elements, ctUtil.getName(mandate), elId, UuidUtils.uuidToString(mandate.getUuid()));
			mndValue.setJsonId(JsonPatrolKey.MANDATE.key + CyberTrackerConfExporter.KEY_SEP + UuidUtils.uuidToString(mandate.getUuid()));
			result.defaultValues.add(createDefaultResultRecord(RESULT_MANDATE, elements, mndValue));
		}

		so = screenOptions.get(PatrolScreenOptionMeta.OBJECTIVE);
		if (so == null || so.isVisible()) {
			id = addNoteNextNode(id, result, elements, Messages.PatrolScreens_Objective, RESULT_OBJECTIVE, Patrol.MAX_OBJECTIVE_LENGTH);
		} else {
			result.defaultValues.add(createDefaultResultRecord(RESULT_OBJECTIVE, elements, so.getStringValue()));
		}

		so = screenOptions.get(PatrolScreenOptionMeta.COMMENT);
		if (so == null || so.isVisible()) {
			id = addNoteNextNode(id, result, elements, Messages.PatrolScreens_Comments, RESULT_COMMENTS, Patrol.MAX_COMMENT_LENGTH);
		} else {
			result.defaultValues.add(createDefaultResultRecord(RESULT_COMMENTS, elements, so.getStringValue()));
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
				Item employee = ElementsUtil.addElementsItem(elements, SmartLabelProvider.getShortLabel(i), mctid.getItemId(), UuidUtils.uuidToString(i.getUuid()), ElementsUtil.MEMBER_ELEMENT_TAG);
				employee.setJsonId(JsonKey.EMPLOYEE.key + CyberTrackerConfExporter.KEY_SEP + UuidUtils.uuidToString(i.getUuid()));
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
			Elements.List.Items.Item leaderItem = null;
			Elements.List.Items.Item pilotItem = null;
			for (ScreenOptionUuid sou : so.getUuidList()) {
				for (Employee e : employees) {
					if (sou.getUuidValue().equals(e.getUuid())) {
						CyberTrackerId mctid = new CyberTrackerId();
						Elements.List.Items.Item memberValue = ElementsUtil.addElementsItem(elements, SmartLabelProvider.getShortLabel(e), mctid.getItemId(), UuidUtils.uuidToString(e.getUuid()), ElementsUtil.MEMBER_ELEMENT_TAG);
						memberValue.setJsonId(JsonKey.EMPLOYEE.key + CyberTrackerConfExporter.KEY_SEP + UuidUtils.uuidToString(e.getUuid()));
						result.defaultValues.add(new CtDataKeyValueRecord(memberValue, ICyberTrackerConstants.STR_TRUE));
						memberIds.add(mctid);
						if (leader_so.getUuidValue() != null && leader_so.getUuidValue().equals(e.getUuid())) {
							leaderItem = memberValue;
						}
						if (pilot_so.getUuidValue() != null && pilot_so.getUuidValue().equals(e.getUuid())) {
							pilotItem = memberValue;
						}
					}
				}
			}
			
			if (leader_so == null || leader_so.isVisible()) {
				id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Leader, RESULT_LEADER, memberIds, false);
			} else {
				if (leaderItem == null) {
					CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Leader, null);
					return null;
				}
				result.defaultValues.add(createDefaultResultRecord(RESULT_LEADER, elements, leaderItem));
			}

			if (pilot_so == null || pilot_so.isVisible()) {
				id = addPilotScreen(id, result, elements, screenOptions, memberIds, patrolTypes, null);
			} else {
				if (pilotItem == null) {
					CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Pilot, null);
					return null;
				}
				result.defaultValues.add(createDefaultResultRecord(RESULT_PILOT, elements, pilotItem));
			}
			
		}
		
		addTaskNode(id, result, elements, startId, dmRootId, ctProps);
		result.rootId = id;
		return result;
	}

	private void addTaskNode(CyberTrackerId id, MetaExportResult container, Elements elements, CyberTrackerId startId, CyberTrackerId dmRootId, CyberTrackerPropertiesProfile ctProps) {
		List<String> nextTaskOptions = new ArrayList<String>();
		List<CyberTrackerId> nodeIds = new ArrayList<CyberTrackerId>();
		List<String> jsonValues = new ArrayList<String>();
		
		nextTaskOptions.add(Messages.PatrolScreens_NewObservation);
		jsonValues.add(null);
		nodeIds.add(dmRootId);
		
		nextTaskOptions.add(Messages.PatrolScreens_EndPatrol);
		jsonValues.add(END_PATROL_KEY);
		nodeIds.add(createEndTripNodes(container, startId, Messages.PatrolScreens_ConfirmMessage));
		
		if (ctProps.isCanPause()) {
			nextTaskOptions.add(Messages.PatrolScreens_PausePatrol);
			jsonValues.add(null);
			PauseNodesLabels labels = new PauseNodesLabels();
			labels.pauseScreenTitle = Messages.PatrolScreensUtil_PauseScreen_Title;
			labels.pauseScreenMessage = Messages.PatrolScreensUtil_PauseScreen_Message;
			labels.resumeScreenTitle = Messages.PatrolScreens_Paused;
			labels.resumeScreenMessage = Messages.PatrolScreensUtil_ResumeScreen_Message;
			nodeIds.add(createPauseTripNodes(container, elements, id, ctProps, labels));
		}
		
		buildNextTaskNode(id, container, elements, nextTaskOptions, nodeIds, ctProps, jsonValues);
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
	
	private CyberTrackerId addStartScreen(CyberTrackerId id, MetaExportResult container, Elements elements, CyberTrackerPropertiesProfile ctProps, ScreenOption so_type, ConservationArea ca, Session session, CyberTrackerId dataType, String strDataType) {
		CyberTrackerId elId = null;
		if (so_type != null  && !so_type.isVisible() && so_type.getStringValue() != null) {
			//patrol type is configured as a default value, but we still need to force speed limitation
			//that is why we add this limitation here - for "Begin Patrol" screen option
			Type type = PatrolType.Type.valueOf(so_type.getStringValue());
			PatrolType pType = PatrolHibernateManager.getPatrolType(ca, session, type);
			int maxSpeed = getMaxSpeed(pType);
			elId = addElementsGpsAccuracyItem(elements, Messages.PatrolScreens_Begin, null, ctProps.getDilutionOfPrecision(), maxSpeed, pType.getType().name());
		} else {
			elId = ElementsUtil.addCustomElements(elements, Messages.PatrolScreens_Begin).get(0);
			
		}
		StartScreensContent content = StartScreensContent.create(elements, Messages.PatrolScreens_StartPatrol, Messages.PatrolScreens_Begin_Title, elId);
		return addStartScreen(id, container, elements, ctProps, content, dataType, strDataType);
	}

	private int getMaxSpeed(PatrolType type) {
		if (type == null) {
			SmartPlugIn.log("PatrolType is undefined when exporting to CyberTracker. Max value is used.", null); //$NON-NLS-1$
			return PatrolType.MAX_SPEED_MAX_VALUE;
		}
		if (type.getMaxSpeed() == null) {
			SmartPlugIn.log("MaxSpeed for PatrolType is undefined when exporting to CyberTracker. Max value is used.", null); //$NON-NLS-1$
			return PatrolType.MAX_SPEED_MAX_VALUE;
		}
		return type.getMaxSpeed();
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
	
	private CyberTrackerId addTypeTransportNodes(CyberTrackerId id, MetaExportResult container, Elements elements, CyberTrackerPropertiesProfile ctProps, List<PatrolType> pTypes, Map<PatrolScreenOptionMeta, ScreenOption> screenOptions, Session session) {
		ScreenOption typeOption = screenOptions.get(PatrolScreenOptionMeta.TYPE);
		if (typeOption == null || typeOption.isVisible()) {
			List<CyberTrackerId> typeIds = new ArrayList<CyberTrackerId>();
			List<String> types = new ArrayList<String>();
			for (PatrolType patrolType : pTypes) {
				Type type = patrolType.getType();
				final String name = type.getGuiName(Locale.getDefault());
				final String tag0 = type.name();
				types.add(name);
				typeIds.add(addElementsGpsAccuracyItem(elements, name, tag0, ctProps.getDilutionOfPrecision(), getMaxSpeed(patrolType), patrolType.getType().name()));
			}
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
				List<CyberTrackerId> trIds = toCyberTrackerIds(elements, getActiveTransportTypes(pTypes.get(i)), JsonPatrolKey.TRANSPORT_TYPE.key);
				node = ctUtil.createRadioNode(typeIds.get(i).getNodeId(), types.get(i), trIds, resultTransportId);
				container.screenNodes.add(node);
				Control control2 = ScreensObjectFactory.getNavigationControl(node);
				control2.setTranslateNextScreenId(nextId.getNodeId());
			}
			return nextId;
		} else {
			Type value = typeOption.getStringValue() != null ? PatrolType.Type.valueOf(typeOption.getStringValue()) : null;
			if (value == null) {
				CyberTrackerPlugIn.displayError(Messages.CyberTrackerExportHandler_ErrDialog_Title, Messages.PatrolScreensUtil_Error_Meta_Type, null);
				return null;
			}
			String elId = (new CyberTrackerId()).getItemId();
			Elements.List.Items.Item tValue = ElementsUtil.addElementsItem(elements, "", elId, value.name()); //$NON-NLS-1$
			tValue.setJsonId(JsonPatrolKey.PATROL_TYPE.key + CyberTrackerConfExporter.KEY_SEP + value.name());
			container.defaultValues.add(createDefaultResultRecord(RESULT_PATROL_TYPE, elements, tValue));
			
			ScreenOption trOption = screenOptions.get(PatrolScreenOptionMeta.TRANSPORT);
			if (trOption == null || trOption.isVisible()) {
				PatrolType pType = null;
				for (PatrolType pt : pTypes) {
					if (value.equals(pt.getType()))
						pType = pt;
				}
				List<CyberTrackerId> trIds = toCyberTrackerIds(elements, getActiveTransportTypes(pType), JsonPatrolKey.TRANSPORT_TYPE.key);
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
				Elements.List.Items.Item trValue = ElementsUtil.addElementsItem(elements, ctUtil.getName(transport), trElId, UuidUtils.uuidToString(transport.getUuid()));
				trValue.setJsonId(JsonPatrolKey.TRANSPORT_TYPE.key + CyberTrackerConfExporter.KEY_SEP + UuidUtils.uuidToString(transport.getUuid()));
				container.defaultValues.add(createDefaultResultRecord(RESULT_TRANSPORT, elements, trValue));
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
	
	public static CyberTrackerId addElementsGpsAccuracyItem(Elements elements, String name, String tag0, Integer dop, Integer maxSpeed, String jsonValue) {
		CyberTrackerId elemId = new CyberTrackerId();
		
		GpsSightingAccuracy gsa = new GpsSightingAccuracy();
		gsa.setDilutionOfPrecision(dop);
		gsa.setMaximumSpeed(maxSpeed);
		
		GpsWaypointAccuracy gwa = new GpsWaypointAccuracy();
		gwa.setDilutionOfPrecision(dop);
		gwa.setMaximumSpeed(maxSpeed);

		Elements.List.Items.Item item = new Elements.List.Items.Item();
		item.setName(name);
		item.setId(elemId.getItemId());
		item.setTag0(tag0);
		item.setGpsAccuracyEnabled(ICyberTrackerConstants.STR_TRUE);
		item.setGpsSightingAccuracy(gsa);
		item.setGpsWaypointAccuracy(gwa);
		item.setJsonId(jsonValue);
		elements.getList().getItems().getItem().add(item);
		return elemId;
	}
	
	
}
