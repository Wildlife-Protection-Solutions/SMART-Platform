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

import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.MetaExportResult;
import org.wcs.smart.cybertracker.export.ScreensObjectFactory;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.cybertracker.model.elements.Elements.List.Items.Item;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnit.State;
import org.wcs.smart.er.model.SurveyDesign;
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
		//start node
		CyberTrackerId startId = new CyberTrackerId();
		CyberTrackerProperties ctProps = CyberTrackerHibernateManager.getProperties(session);
		CyberTrackerId id = addStartScreen(startId, result, elements, ctProps);
		
		
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		List<Employee> employees = HibernateManager.getActiveEmployees(ca, session);
		Collections.sort(employees, new Comparator<Employee>() {
			@Override
			public int compare(Employee e1, Employee e2) {
				return Collator.getInstance().compare(SmartLabelProvider.getShortLabel(e1), SmartLabelProvider.getShortLabel(e2));
			}
		});
		
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
		id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Leader, RESULT_MISSION_LEADER, memberIds, filter);

		id = addNoteNextNode(id, result, elements, Messages.PatrolScreens_Comments, RESULT_MISSION_COMMENTS, Mission.MAX_LENGTH_COMMENT);

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
			SmartPlugIn.displayLog(MessageFormat.format("SMART CyberTracker plugin do not support mission properties of type {0}. Property ''{1}'' will be ignored.", missionAttribute.getType(), missionAttribute.getName()), null);
		}
		
		List<SamplingUnit> samplingUnits = SurveyHibernateManager.getInstance().getSamplingUnits(surveyDesign, session, State.ACTIVE);
		SamplingUnit noneSu = new SamplingUnit();
		noneSu.setId("None");
		samplingUnits.add(noneSu);
		cyberTrackerIds = suToCtIds(elements, samplingUnits);
		CyberTrackerId suScreenId = id;
		id = addSimpleNextRadioNode(id, result, elements, "Sampling Unit", RESULT_MISSION_SAMPLING_UNIT, cyberTrackerIds, true);
		Node suNode = result.screenNodes.get(result.screenNodes.size()-1);
		Control control2 = ScreensObjectFactory.getNavigationControl(suNode);
		control2.setShowBack("False"); //$NON-NLS-1$

		addTaskNode(id, result, elements, startId, dmRootId, suScreenId, ctProps);
		result.rootId = id;
		return result;
	}

	private void addTaskNode(CyberTrackerId id, MetaExportResult container, Elements elements, CyberTrackerId startId, CyberTrackerId dmRootId, CyberTrackerId suId, CyberTrackerProperties ctProps) {
		List<String> nextTaskOptions = new ArrayList<String>();
		List<CyberTrackerId> nodeIds = new ArrayList<CyberTrackerId>();
		
		nextTaskOptions.add(Messages.PatrolScreens_NewObservation);
		nodeIds.add(dmRootId);
		
		nextTaskOptions.add("Start New Sampling Unit");
		nodeIds.add(suId);
		
		
		nextTaskOptions.add("End Survey");
		nodeIds.add(createEndTripNodes(container, startId, "Press 'Save' to confirm ending survey or use back button."));
		
		if (ctProps.isCanPause()) {
			nextTaskOptions.add("Pause Survey (Rest)");
			PauseNodesLabels labels = new PauseNodesLabels();
			labels.resumeOption = "Resume Survey";
			labels.resumeScreenTitle = "Paused Survey";
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

	private CyberTrackerId addStartScreen(CyberTrackerId id, MetaExportResult container, Elements elements, CyberTrackerProperties ctProps) {
		StartScreenLabels labels = new StartScreenLabels();
		labels.startItemLabel = "Start New Survey";
		labels.beginTitle = "Survey Start";
		labels.beginItemLabel = "Begin Survey";
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
