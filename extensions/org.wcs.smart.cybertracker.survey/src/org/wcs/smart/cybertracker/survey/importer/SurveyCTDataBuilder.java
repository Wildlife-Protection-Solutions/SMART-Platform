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
package org.wcs.smart.cybertracker.survey.importer;

import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.AbstractSmartImporter;
import org.wcs.smart.cybertracker.importer.CyberTrackerDataBuilder;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.ICyberTrackerData;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.survey.export.SurveyScreensUtil;
import org.wcs.smart.cybertracker.survey.model.CyberTrackerSurvey;
import org.wcs.smart.cybertracker.survey.model.CyberTrackerSurvey.SurveyMeta;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * Builds specific survey objects that suitable for further import
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class SurveyCTDataBuilder extends CyberTrackerDataBuilder {

	@Override
	protected ICyberTrackerData createDataRecord(Session session, Map<String, E> elementsMap, List<S> sData) {
		CyberTrackerSurvey data = new CyberTrackerSurvey(elementsMap, sData);
		initMetaData(data, session);
		return data;
	}

	private void initMetaData(CyberTrackerSurvey ctSurvey, Session session) {
		Map<String, E> eMap = ctSurvey.getElementsMap();
		recordSurveyDesign(ctSurvey, eMap, session);
		
		List<S> patrolData = ctSurvey.getSData();
		if (patrolData.isEmpty())
			return;
		
		S s = patrolData.get(0); //init metadata from the first sight (as all other sighs MUST have the same metadata by design)
		Date date = null;
		Time time = null;
		Date start_date = null;
		Time start_time = null;
		DateFormat formatter = AbstractSmartImporter.createCyberTrackerDateFormatter();
		
		for (S.A a : s.getA()) {
			String i = a.getI();
			String n = a.getN();
			String v = a.getV();
			
			if (ICyberTrackerConstants.DATE.equals(i)) {
				try {
					date = formatter.parse(v);
				} catch (ParseException e) {
					CyberTrackerPlugIn.log(e.getMessage(), e);
				}
			} else if (ICyberTrackerConstants.TIME.equals(i)) {
				time = Time.valueOf(v);
			} else if (ScreensUtil.RESULT_ID.equals(n)) {
				ctSurvey.setId(v);
			} else if (ScreensUtil.RESULT_START_DATE.equals(n)) {
				try {
					start_date = formatter.parse(v);
				} catch (ParseException e) {
					CyberTrackerPlugIn.log(e.getMessage(), e);
				}
			} else if (ScreensUtil.RESULT_START_TIME.equals(n)) {
				start_time = Time.valueOf(v);
			} else {
				E ei = eMap.get(i);
				if (ei != null) {
					recordSurveyData(ctSurvey, ei, v, eMap, session);
				} else {
					ctSurvey.addMissingKey(i);
				}
			}
		}
		
		date = AbstractSmartImporter.combine(date, time);
		start_date = AbstractSmartImporter.combine(start_date, start_time);
		if (date != null && start_date != null) {
			if (start_date.before(date)) {
				ctSurvey.setStartDate(start_date);
			} else {
				ctSurvey.setStartDate(date);
			}
		} else {
			if (date == null) {
				ctSurvey.setStartDate(start_date);
			} else {
				ctSurvey.setStartDate(date);
			}
		}
		
		date = null;
		time = null;
		
		s = patrolData.get(patrolData.size()-1); //need to find end date
		for (S.A a : s.getA()) {
			String i = a.getI();
			if (ICyberTrackerConstants.DATE.equals(i)) {
				try {
					date = formatter.parse(a.getV());
					if (time != null)
						break;
				} catch (ParseException e) {
					CyberTrackerPlugIn.log(e.getMessage(), e);
				}
			} else if (ICyberTrackerConstants.TIME.equals(i)) {
				time = Time.valueOf(a.getV());
				if (date != null)
					break;
			}
		}
		ctSurvey.setEndDate(AbstractSmartImporter.combine(date, time));
	}
	
	private void recordSurveyDesign(CyberTrackerSurvey ctSurvey, Map<String, E> eMap, Session session) {
		for (E e : eMap.values()) {
			if (SurveyScreensUtil.RESULT_SURVEY_DESIGN.equals(e.getN())) {
				String key = e.getTag0();
				SurveyDesign sd = SurveyHibernateManager.getInstance().getSurveyDesign(key, session);
				if (sd != null) {
					ctSurvey.setSurveyDesign(sd);
				} else {
					ctSurvey.addError(SurveyMeta.SURVEY_DESIGN, MessageFormat.format("Survey Design not found. Conservation area doesn''t contain survey design with key = ''{0}''.", e.getN()));
				}
				return;
			}
		}
		ctSurvey.addError(SurveyMeta.SURVEY_DESIGN, "Reference to Survey Design is missing in imported file");
	}
	
	private void recordSurveyData(CyberTrackerSurvey ctSurvey, E i, String v, Map<String, E> eMap, Session session) {
		String n = i.getN();
		if (ScreensUtil.RESULT_DEFAULT_META_VALUES.equals(n)) {
			String[] ctIdArray = v.split(ICyberTrackerConstants.ATTRIBUTE_DEFAULT_VALUES_SEPATATOR);
			for (String ctid : ctIdArray) {
				E di = eMap.get(ctid); //default "E" element, we need to emulate as if it is set in a.i with a.v = di.tag2 ... ;)
				recordSurveyData(ctSurvey, di, di.getTag2(), eMap, session);
			}
		} else if (ScreensUtil.RESULT_ID.equals(n)) {
			ctSurvey.setId(v);
		} else if (SurveyScreensUtil.RESULT_MISSION_COMMENTS.equals(n)) {
			ctSurvey.setComment(v);
		} else if (SurveyScreensUtil.RESULT_MISSION_LEADER.equals(n)) {
			E e = eMap.get(v);
			Employee emp = fetchFromTag0(Employee.class, e, session);
			if (emp == null && e.getTag0() != null)
				ctSurvey.addError(SurveyMeta.LEADER, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Leader, e.getN()));
			ctSurvey.setCtLeader(e.getN());
			ctSurvey.setLeader(emp);
		} else if (ElementsUtil.MEMBER_ELEMENT_TAG.equals(i.getTag1())) { //check that this is a member record
			Employee emp = fetchFromTag0(Employee.class, i, session);
			if (emp == null && i.getTag0() != null)
				ctSurvey.addWarning(SurveyMeta.MEMBERS, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Member, i.getN()));
			ctSurvey.getCtMembers().add(i.getN());
			if (emp != null) {
				ctSurvey.getMembers().add(emp);
			}
		} else if (n != null && n.startsWith(SurveyScreensUtil.RESULT_MISSION_PROPETY_PREFIX)) {
			String tag0 = i != null ? i.getTag0() : null;
			MissionAttribute ma = tag0 != null ? SurveyHibernateManager.getInstance().getMissionAttributeByKey(tag0, session) : null;
			if (ma == null) {
				ctSurvey.addWarning(SurveyMeta.MISSION_PROPERTY, MessageFormat.format("Conservation area doesn''t contain mission property with key = ''{0}''. Mission property will not be recorded.", tag0));
				return;
			}
			//if everything is ok, that survey design should be already in place
			SurveyDesign sd = ctSurvey.getSurveyDesign();
			if (sd!= null && sd.getMissionProperties() != null) {
				boolean found = false;
				for (MissionProperty mp : sd.getMissionProperties()) {
					if (ma.equals(mp.getAttribute())) {
						found = true;
						break;
					}
				}
				if (!found) {
					ctSurvey.addWarning(SurveyMeta.MISSION_PROPERTY, MessageFormat.format("Survey design ''{0}'' doesn''t contain mission property with key = ''{1}''. Mission property will not be recorded.", sd.getName(), tag0));
					return;
				}
			}
			if (ma.getType() != null) {
				MissionPropertyValue mpv = new MissionPropertyValue();
				mpv.setMissionAttribute(ma);
				switch (ma.getType()) {
				case LIST:
				{
					E itemE = eMap.get(v);
					MissionAttributeListItem item = fetchFromTag0(MissionAttributeListItem.class, itemE, session);
					if (item != null) {
						mpv.setAttributeListItem(item);
						ctSurvey.getMissionProperties().add(mpv);
					} else {
						String itemName = itemE != null ? itemE.getN() : v;
						ctSurvey.addWarning(SurveyMeta.MISSION_PROPERTY, MessageFormat.format("Mission property with key = ''{0}'' do not contain list item ''{1}''. Mission property will not be recorded.", tag0, itemName));
					}
					return;
					
				}
				case TEXT:
					mpv.setStringValue(v);
					ctSurvey.getMissionProperties().add(mpv);
					return;
				case NUMERIC:
					mpv.setNumberValue(Double.valueOf(v));
					ctSurvey.getMissionProperties().add(mpv);
					return;
				default:
					break;
				}
			}
			ctSurvey.addWarning(SurveyMeta.MISSION_PROPERTY, MessageFormat.format("Unsupported mission attribute type ''{0}'' for mission property with key = ''{1}''. Mission property will not be recorded.", ma.getType(), tag0));
		}
	}
}