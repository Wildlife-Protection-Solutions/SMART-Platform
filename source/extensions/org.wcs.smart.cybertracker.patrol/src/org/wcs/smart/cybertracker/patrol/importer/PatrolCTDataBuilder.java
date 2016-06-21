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
package org.wcs.smart.cybertracker.patrol.importer;

import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.AbstractSmartImporter;
import org.wcs.smart.cybertracker.importer.CyberTrackerDataBuilder;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.patrol.export.PatrolScreensUtil;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol.PatrolMeta;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.PatrolType.Type;
import org.wcs.smart.patrol.model.Team;

/**
 * Builds specific patrol objects that suitable for further import
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class PatrolCTDataBuilder extends CyberTrackerDataBuilder {

	protected CyberTrackerPatrol createDataRecord(Session session, Map<String, E> elementsMap, List<S> sData) {
		CyberTrackerPatrol ctPatrol = new CyberTrackerPatrol(elementsMap, sData);
		initMetaData(ctPatrol, session);
		return ctPatrol;
		
	}

	private void initMetaData(CyberTrackerPatrol ctPatrol, Session session) {
		List<S> patrolData = ctPatrol.getSData();
		if (patrolData.isEmpty())
			return;
		
		 Map<String, E> eMap = ctPatrol.getElementsMap();
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
				ctPatrol.setId(v);
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
					recordPatrolData(ctPatrol, ei, v, eMap, session);
				} else {
					ctPatrol.addMissingKey(i);
				}
			}
		}
		
		date = AbstractSmartImporter.combine(date, time);
		start_date = AbstractSmartImporter.combine(start_date, start_time);
		if (date != null && start_date != null) {
			if (start_date.before(date)) {
				ctPatrol.setStartDate(start_date);
			} else {
				ctPatrol.setStartDate(date);
			}
		} else {
			if (date == null) {
				ctPatrol.setStartDate(start_date);
			} else {
				ctPatrol.setStartDate(date);
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
		ctPatrol.setEndDate(AbstractSmartImporter.combine(date, time));
		
		//it is possible that pilot is configured as default value, but pilot doesn't make sense for ground patrols
		if (PatrolType.Type.GROUND.equals(ctPatrol.getPatrolType())) {
			ctPatrol.setCtPilot(null);
			ctPatrol.setPilot(null);
		}
	}

	private void recordPatrolData(CyberTrackerPatrol ctPatrol, E i, String v, Map<String, E> eMap, Session session) {
		String n = i.getN();
		if (ScreensUtil.RESULT_DEFAULT_META_VALUES.equals(n)) {
			String[] ctIdArray = v.split(ICyberTrackerConstants.ATTRIBUTE_DEFAULT_VALUES_SEPATATOR);
			for (String ctid : ctIdArray) {
				E di = eMap.get(ctid); //default "E" element, we need to emulate as if it is set in a.i with a.v = di.tag2 ... ;)
				if (di != null) {
					recordPatrolData(ctPatrol, di, di.getTag2(), eMap, session);
				} else {
					ctPatrol.addError(PatrolMeta.GENERAL, MessageFormat.format(Messages.PatrolCTDataBuilder_Error_DefaultValue, ctid));
				}
			}
		} else if (ScreensUtil.RESULT_ID.equals(n)) {
			ctPatrol.setId(v);
		} else if (PatrolScreensUtil.RESULT_PATROL_TYPE.equals(n)) {
			E e = eMap.get(v);
			String tag0 = e != null ? e.getTag0() : null;
			if (tag0 != null) {
				ctPatrol.setPatrolType(Type.valueOf(e.getTag0()));
			}
		} else if (PatrolScreensUtil.RESULT_TRANSPORT.equals(n)) {
			E e = eMap.get(v);
			PatrolTransportType transportType = fetchFromTag0(PatrolTransportType.class, e, session);
			if (transportType == null)
				ctPatrol.addError(PatrolMeta.TRANSPORT, MessageFormat.format(Messages.CyberTrackerPatrol_Error_Transport, e.getN()));
			ctPatrol.setCtTransport(e.getN());
			ctPatrol.setPatrolTransportType(transportType);
		} else if (PatrolScreensUtil.RESULT_ARMED.equals(n)) {
			E e = eMap.get(v);
			String tag0 = e != null ? e.getTag0() : null;
			if (tag0 != null) {
				ctPatrol.setArmed(ElementsUtil.BOOL_TRUE.equals(tag0.toLowerCase()));
			}				
		} else if (PatrolScreensUtil.RESULT_TEAM.equals(n)) {
			E e = eMap.get(v);
			Team t = fetchFromTag0(Team.class, e, session);
			if (t == null && e.getTag0() != null)
				ctPatrol.addWarning(PatrolMeta.TEAM, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Team, e.getN()));
			ctPatrol.setCtTeam(e.getN());
			ctPatrol.setTeam(t);
		} else if (PatrolScreensUtil.RESULT_STATION.equals(n)) {
			E e = eMap.get(v);
			Station st = fetchFromTag0(Station.class, e, session);
			if (st == null && e.getTag0() != null)
				ctPatrol.addWarning(PatrolMeta.STATION, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Station, e.getN()));
			ctPatrol.setCtStation(e.getN());
			ctPatrol.setStation(st);
		} else if (PatrolScreensUtil.RESULT_MANDATE.equals(n)) {
			E e = eMap.get(v);
			PatrolMandate m = fetchFromTag0(PatrolMandate.class, e, session);
			if (m == null && e.getTag0() != null)
				ctPatrol.addWarning(PatrolMeta.MANDATE, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Mandate, e.getN()));
			ctPatrol.setMandate(m);
		} else if (PatrolScreensUtil.RESULT_OBJECTIVE.equals(n)) {
			ctPatrol.setObjective(v);
		} else if (PatrolScreensUtil.RESULT_COMMENTS.equals(n)) {
			ctPatrol.setComment(v);
		} else if (PatrolScreensUtil.RESULT_LEADER.equals(n)) {
			E e = eMap.get(v);
			Employee emp = fetchFromTag0(Employee.class, e, session);
			if (emp == null && e.getTag0() != null)
				ctPatrol.addError(PatrolMeta.LEADER, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Leader, e.getN()));
			ctPatrol.setCtLeader(e.getN());
			ctPatrol.setLeader(emp);
		} else if (PatrolScreensUtil.RESULT_PILOT.equals(n)) {
			E e = eMap.get(v);
			Employee emp = fetchFromTag0(Employee.class, e, session);
			if (emp == null && e.getTag0() != null)
				ctPatrol.addError(PatrolMeta.PILOT, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Pilot, e.getN()));
			ctPatrol.setCtPilot(e.getN());
			ctPatrol.setPilot(emp);
		} else if (ElementsUtil.MEMBER_ELEMENT_TAG.equals(i.getTag1())) { //check that this is a member record
			Employee emp = fetchFromTag0(Employee.class, i, session);
			if (emp == null && i.getTag0() != null)
				ctPatrol.addWarning(PatrolMeta.MEMBERS, MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Member, i.getN()));
			ctPatrol.getCtMembers().add(i.getN());
			if (emp != null) {
				ctPatrol.getMembers().add(emp);
			}
		}		
	}
	
}
