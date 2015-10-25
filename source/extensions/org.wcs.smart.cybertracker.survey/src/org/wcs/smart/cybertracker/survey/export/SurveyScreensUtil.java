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
package org.wcs.smart.cybertracker.survey.export;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.MetaExportResult;
import org.wcs.smart.cybertracker.export.ScreensObjectFactory;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.elements.Elements.List.Items.Item;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.dataentry.model.ScreenOptionUuid;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.State;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.meta.MissionScreenOptionMeta;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Util for creating survey/mission screens for CyberTracker.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class SurveyScreensUtil extends ScreensUtil {

	public static final String RESULT_SURVEY_DESIGN = "#SurveyDesign"; //$NON-NLS-1$

	public static final String RESULT_MISSION_LEADER = "#Leader"; //$NON-NLS-1$
	public static final String RESULT_MISSION_COMMENTS = "#Comments"; //$NON-NLS-1$
	
	public static final String RESULT_MISSION_START_SAMPLING_UNIT = "#StartSamplingUnit"; //$NON-NLS-1$
	public static final String RESULT_MISSION_SAMPLING_UNIT = "#SamplingUnit"; //$NON-NLS-1$

	public static final String RESULT_MISSION_PROPETY_PREFIX = "#MP#"; //$NON-NLS-1$
	
	public static final String DATATYPE_SURVEY = "survey"; //$NON-NLS-1$

	protected SurveyScreensUtil(CyberTrackerUtil ctUtil) {
		super(ctUtil);
	}

	@Override
	public MetaExportResult buildMetaNodes(Elements elements, CyberTrackerId dmRootId, Session session) {
		registerDatatype(elements, DATATYPE_SURVEY);
		MetaExportResult result = new MetaExportResult();
		List<CyberTrackerId> cyberTrackerIds;
		ScreenOption so;
		//start node
		CyberTrackerId startId = new CyberTrackerId();
		CyberTrackerProperties ctProps = CyberTrackerHibernateManager.getProperties(session);
		CyberTrackerId id = addStartScreen(startId, result, elements, ctProps);
		
		
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		Map<MissionScreenOptionMeta, ScreenOption> screenOptions = SurveyHibernateManager.getMissionScreenOptions(ca, session);
		List<Employee> employees = HibernateManager.getActiveEmployees(ca, session);
		Collections.sort(employees, new Comparator<Employee>() {
			@Override
			public int compare(Employee e1, Employee e2) {
				return Collator.getInstance().compare(SmartLabelProvider.getShortLabel(e1), SmartLabelProvider.getShortLabel(e2));
			}
		});
		
		so = screenOptions.get(MissionScreenOptionMeta.MEMBERS);
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
			id = addSimpleNextRadioNode(id, result, elements, Messages.SurveyScreensUtil_Leader, RESULT_MISSION_LEADER, memberIds, filter);
		} else {
			//adding default members
			ScreenOption leader_so = screenOptions.get(MissionScreenOptionMeta.LEADER);
			List<CyberTrackerId> memberIds = new ArrayList<CyberTrackerId>();
			CyberTrackerId leaderCtId = null;
			for (ScreenOptionUuid sou : so.getUuidList()) {
				for (Employee e : employees) {
					if (e.getUuid().equals(sou.getUuidValue())) {
						CyberTrackerId mctid = new CyberTrackerId();
						ElementsUtil.addElementsItem(elements, SmartLabelProvider.getShortLabel(e), mctid.getItemId(), UuidUtils.uuidToString(e.getUuid()), ElementsUtil.MEMBER_ELEMENT_TAG);
						result.defaultValues.add(mctid.getItemId());
						memberIds.add(mctid);
						if (leader_so.getUuidValue() != null && leader_so.getUuidValue().equals(e.getUuid())) {
							leaderCtId = mctid;
						}
					}
				}
			}
			
			if (leader_so == null || leader_so.isVisible()) {
				id = addSimpleNextRadioNode(id, result, elements, Messages.SurveyScreensUtil_Leader, RESULT_MISSION_LEADER, memberIds, false);
			} else {
				if (leaderCtId == null) {
					CyberTrackerPlugIn.displayError(Messages.SurveyScreensUtil_ErrorDialog_Title, Messages.SurveyScreensUtil_Error_Leader, null);
					return null;
				}
				result.defaultValues.add(createDefaultResultElement(RESULT_MISSION_LEADER, elements, leaderCtId.getItemId()));
			}
		}

		so = screenOptions.get(MissionScreenOptionMeta.COMMENT);
		if (so == null || so.isVisible()) {
			id = addNoteNextNode(id, result, elements, Messages.SurveyScreensUtil_Comments, RESULT_MISSION_COMMENTS, Mission.MAX_LENGTH_COMMENT);
		} else {
			result.defaultValues.add(createDefaultResultElement(RESULT_MISSION_COMMENTS, elements, so.getStringValue()));
		}

		String sdKey = getSurveyDesignKeyId(elements);
		SurveyDesign surveyDesign = SurveyHibernateManager.getInstance().getSurveyDesign(sdKey, session);
		
		List<MissionProperty> missionProperties = surveyDesign.getMissionProperties();
		for (MissionProperty missionProperty : missionProperties) {
			MissionAttribute missionAttribute = missionProperty.getAttribute();
			String tag0 = missionAttribute.getKeyId();
			String resultElName = RESULT_MISSION_PROPETY_PREFIX + missionAttribute.getKeyId();
			if (missionAttribute.getType() != null) {
				switch (missionAttribute.getType()) {
				case LIST:
					cyberTrackerIds = toCyberTrackerIds(elements, missionAttribute.getAttributeList());
					id = addSimpleNextRadioNode(id, result, elements, missionAttribute.getName(), resultElName, tag0, cyberTrackerIds, true);
					continue;
				case NUMERIC:
					id = addNumberNode(id, result, elements, missionAttribute.getName(), resultElName, tag0);
					continue;
				case TEXT:
					id = addNoteNextNode(id, result, elements, missionAttribute.getName(), resultElName, tag0, Mission.MAX_LENGTH_COMMENT);
					continue;
				default:
					break;
				}
			}
			SmartPlugIn.displayLog(MessageFormat.format(Messages.SurveyScreensUtil_Error_InvalidMissionPropertyType, missionAttribute.getType(), missionAttribute.getName()), null);
		}
		
		List<SamplingUnit> samplingUnits = SurveyHibernateManager.getInstance().getSamplingUnits(surveyDesign, session, State.ACTIVE);
		SamplingUnit noneSu = new SamplingUnit();
		noneSu.setId(Messages.SurveyScreensUtil_NoSamplingUnit);
		samplingUnits.add(noneSu);
		cyberTrackerIds = suToCtIds(elements, samplingUnits);
		id = addSimpleNextRadioNode(id, result, elements, Messages.SurveyScreensUtil_StartSamplingUnit, RESULT_MISSION_START_SAMPLING_UNIT, cyberTrackerIds, true);

		addTaskNode(id, result, elements, startId, dmRootId, cyberTrackerIds, ctProps);
		result.rootId = id;
		return result;
	}

	private void addTaskNode(CyberTrackerId id, MetaExportResult container, Elements elements, CyberTrackerId startId, CyberTrackerId dmRootId, List<CyberTrackerId> ctElemIds, CyberTrackerProperties ctProps) {
		List<String> nextTaskOptions = new ArrayList<String>();
		List<CyberTrackerId> nodeIds = new ArrayList<CyberTrackerId>();
		
		nextTaskOptions.add(Messages.SurveyScreensUtil_NewObservation);
		nodeIds.add(dmRootId);
		
		nextTaskOptions.add(Messages.SurveyScreensUtil_NewSamplingUnit);
		nodeIds.add(createSamplingUnitNodes(container, elements, id, ctElemIds));
		
		
		nextTaskOptions.add(Messages.SurveyScreensUtil_EndSurvey);
		nodeIds.add(createEndTripNodes(container, startId, Messages.SurveyScreensUtil_EndSurveyMessage));
		
		if (ctProps.isCanPause()) {
			nextTaskOptions.add(Messages.SurveyScreensUtil_PauseSurvey);
			PauseNodesLabels labels = new PauseNodesLabels();
			labels.resumeOption = Messages.SurveyScreensUtil_ResumeSurvey;
			labels.resumeScreenTitle = Messages.SurveyScreensUtil_PausedSurveyTitle;
			nodeIds.add(createPauseTripNodes(container, elements, id, ctProps, labels));
		}
		
		buildNextTaskNode(id, container, elements, nextTaskOptions, nodeIds, ctProps);
	}
	
	//Not the best design, but we can obtain required data from Elements in this case
	private String getSurveyDesignKeyId(Elements elements) {
		for (Item item : elements.getList().getItems().getItem()) {
			if (SurveyScreensUtil.RESULT_SURVEY_DESIGN.equals(item.getName())) {
				return item.getTag0();
			}
		}
		return null;
	}


	private CyberTrackerId createSamplingUnitNodes(MetaExportResult container, Elements elements, CyberTrackerId nextTaskId, List<CyberTrackerId> ctElemIds) {
		CyberTrackerId id = new CyberTrackerId();
		addSimpleNextRadioNode(id, container, elements, Messages.SurveyScreensUtil_SamplingUnit, RESULT_MISSION_SAMPLING_UNIT, ctElemIds, false);
		Node suNode = container.screenNodes.get(container.screenNodes.size()-1);
		Control control2 = ScreensObjectFactory.getNavigationControl(suNode);
		control2.setShowNext("False"); //$NON-NLS-1$
		control2.setShowMajor("True"); //$NON-NLS-1$
		control2.setTranslateMajorScreenId(nextTaskId.getNodeId());
		control2.setTakeGPS("False"); //$NON-NLS-1$
		return id;
	}
	
	private CyberTrackerId addStartScreen(CyberTrackerId id, MetaExportResult container, Elements elements, CyberTrackerProperties ctProps) {
		StartScreenLabels labels = new StartScreenLabels();
		labels.startItemLabel = Messages.SurveyScreensUtil_StartSurvey;
		labels.beginTitle = Messages.SurveyScreensUtil_StartSurveyTitle;
		labels.beginItemLabel = Messages.SurveyScreensUtil_BeginSurvey;
		return addStartScreen(id, container, elements, ctProps, labels);
	}

	private List<CyberTrackerId> suToCtIds(Elements elements, List<SamplingUnit> items) {
		List<String> labelValues = new ArrayList<String>();
		List<String> tag0Values = new ArrayList<String>();
		for (SamplingUnit su : items) {
			labelValues.add(su.getId());
			tag0Values.add(UuidUtils.uuidToString(su.getUuid()));
		}
		return ElementsUtil.addCustomElements(elements, labelValues, tag0Values);
	}
}
