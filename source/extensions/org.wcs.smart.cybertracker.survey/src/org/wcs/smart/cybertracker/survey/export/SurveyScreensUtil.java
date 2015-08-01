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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil;
import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.MetaExportResult;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.elements.Elements;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;

/**
 * Util for creating survey/mission screens for CyberTracker.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class SurveyScreensUtil extends ScreensUtil {

	public static final String RESULT_MISSION_LEADER = "#Leader"; //$NON-NLS-1$
	public static final String RESULT_MISSION_COMMENTS = "#Comments"; //$NON-NLS-1$
	
	public static final String DATATYPE_SURVEY = "survey"; //$NON-NLS-1$

	protected SurveyScreensUtil(CyberTrackerUtil ctUtil) {
		super(ctUtil);
	}

	@Override
	public MetaExportResult buildMetaNodes(Elements elements, CyberTrackerId dmRootId, Session session) {
		registerDatatype(elements, DATATYPE_SURVEY);
		MetaExportResult result = new MetaExportResult();
		//start node
		CyberTrackerId startId = new CyberTrackerId();
		CyberTrackerProperties ctProps = CyberTrackerHibernateManager.getProperties(session);
		CyberTrackerId id = addStartScreen(startId, result, elements, ctProps);
		
		
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		List<Employee> employees = HibernateManager.getActiveEmployees(ca, session);
		Collections.sort(employees, new Comparator<Employee>() {
			@Override
			public int compare(Employee e1, Employee e2) {
				return Collator.getInstance().compare(e1.getFullLabel(), e2.getFullLabel());
			}
		});
		
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
		id = addSimpleNextRadioNode(id, result, elements, Messages.PatrolScreens_Leader, RESULT_MISSION_LEADER, memberIds, filter);

		id = addNoteNextNode(id, result, elements, Messages.PatrolScreens_Comments, RESULT_MISSION_COMMENTS, Mission.MAX_LENGTH_COMMENT);
		
		addTaskNode(id, result, elements, startId, dmRootId, ctProps);
		result.rootId = id;
		return result;
	}
	
	private CyberTrackerId addStartScreen(CyberTrackerId id, MetaExportResult container, Elements elements, CyberTrackerProperties ctProps) {
		StartScreenLabels labels = new StartScreenLabels();
		labels.startItemLabel = "Start New Survey";
		labels.beginTitle = "Survey Start";
		labels.beginItemLabel = "Begin Survey";
		return addStartScreen(id, container, elements, ctProps, labels);
	}

}
