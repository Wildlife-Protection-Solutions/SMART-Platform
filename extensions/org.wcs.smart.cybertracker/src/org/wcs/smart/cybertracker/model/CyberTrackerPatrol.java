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
package org.wcs.smart.cybertracker.model;

import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.export.PatrolScreensUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S.A;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.PatrolType.Type;
import org.wcs.smart.patrol.model.Team;

/**
 * Model representing single patrol imported from CyberTracker application
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerPatrol {
	
	private List<String> errors = new ArrayList<String>();
	private List<String> warnings = new ArrayList<String>();
	
	private Map<String, E> elementsMap;
	private List<S> patrolData;
	
	private String id;
	private Station station;
	private Team team;
	private String objective;
	private PatrolMandate mandate;
	private PatrolType.Type patrolType;
	private PatrolTransportType patrolTransportType;
	private boolean isArmed = false;
	private String comment;
	private Date startDate;
	private Date endDate;
	private Employee leader;
	private Employee pilot;
	private List<Employee> members;
	
	//used only for gui representation after initial load
	private String ctTransport;
	private String ctStation;
	private String ctTeam;
	private String ctLeader;
	private String ctPilot;
	private List<String> ctMembers;
	
	public CyberTrackerPatrol(Map<String, E> elementsMap, List<S> patrolData) {
		this.elementsMap = elementsMap;
		this.patrolData = patrolData;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			initMetaData(session);
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
	}
	
	private void initMetaData(Session session) {
		if (patrolData.isEmpty())
			return;
		S s = patrolData.get(0); //init metadata from the first sight (as all other sighs MUST have the same metadata by design)
		
		for (A a : s.getA()) {
			String i = a.getI();
			String n = a.getN();
			String v = a.getV();
			
			if (ICyberTrackerConstants.DATE.equals(i)) {
				DateFormat formatter = new SimpleDateFormat(ICyberTrackerConstants.CT_DATE_FORMAT); //TODO: will this always work or CT might provide different format depending on locale settings?
				try {
					Date date = combine(formatter.parse(v), getStartDate());
					setStartDate(date);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else if (ICyberTrackerConstants.TIME.equals(i)) {
				Date date = combine(getStartDate(), Time.valueOf(v));
				setStartDate(date);
			} else if (PatrolScreensUtil.RESULT_PATROL_ID.equals(n)) {
				setId(v);
			} else if (PatrolScreensUtil.RESULT_PATROL_TYPE.equals(n)) {
				E e = getElementsMap().get(v);
				String tag0 = e != null ? e.getTag0() : null;
				if (tag0 != null) {
					setPatrolType(Type.valueOf(e.getTag0()));
				}
			} else if (PatrolScreensUtil.RESULT_TRANSPORT.equals(n)) {
				E e = getElementsMap().get(v);
				PatrolTransportType transportType = fetchFromTag0(PatrolTransportType.class, e, session);
				if (transportType == null)
					addError(MessageFormat.format(Messages.CyberTrackerPatrol_Error_Transport, e.getN()));
				setCtTransport(e.getN());
				setPatrolTransportType(transportType);
			} else if (PatrolScreensUtil.RESULT_ARMED.equals(n)) {
				E e = getElementsMap().get(v);
				String tag0 = e != null ? e.getTag0() : null;
				if (tag0 != null) {
					setArmed("true".equals(tag0.toLowerCase())); //$NON-NLS-1$
				}				
			} else if (PatrolScreensUtil.RESULT_TEAM.equals(n)) {
				E e = getElementsMap().get(v);
				Team t = fetchFromTag0(Team.class, e, session);
				if (t == null && e.getTag0() != null)
					addWarning(MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Team, e.getN()));
				setCtTeam(e.getN());
				setTeam(t);
			} else if (PatrolScreensUtil.RESULT_STATION.equals(n)) {
				E e = getElementsMap().get(v);
				Station st = fetchFromTag0(Station.class, e, session);
				if (st == null && e.getTag0() != null)
					addWarning(MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Station, e.getN()));
				setCtStation(e.getN());
				setStation(st);
			} else if (PatrolScreensUtil.RESULT_MANDATE.equals(n)) {
				E e = getElementsMap().get(v);
				PatrolMandate m = fetchFromTag0(PatrolMandate.class, e, session);
				if (m == null && e.getTag0() != null)
					addWarning(MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Mandate, e.getN()));
				setMandate(m);
			} else if (PatrolScreensUtil.RESULT_OBJECTIVE.equals(n)) {
				setObjective(v);
			} else if (PatrolScreensUtil.RESULT_COMMENTS.equals(n)) {
				setComment(v);
			} else if (PatrolScreensUtil.RESULT_LEADER.equals(n)) {
				E e = getElementsMap().get(v);
				Employee emp = fetchFromTag0(Employee.class, e, session);
				if (emp == null && e.getTag0() != null)
					addWarning(MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Leader, e.getN()));
				setCtLeader(e.getN());
				setLeader(emp);
			} else if (PatrolScreensUtil.RESULT_PILOT.equals(n)) {
				E e = getElementsMap().get(v);
				Employee emp = fetchFromTag0(Employee.class, e, session);
				if (emp == null && e.getTag0() != null)
					addWarning(MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Pilot, e.getN()));
				setCtPilot(e.getN());
				setPilot(emp);
			} else if (isMemberRecord(a)) {
				E e = getElementsMap().get(i);
				Employee emp = fetchFromTag0(Employee.class, e, session);
				if (emp == null && e.getTag0() != null)
					addWarning(MessageFormat.format(Messages.CyberTrackerPatrol_Warn_Member, e.getN()));
				getCtMembers().add(e.getN());
				if (emp != null) {
					getMembers().add(emp);
				}
			}
		}
		
		s = patrolData.get(patrolData.size()-1); //need to find end date
		for (A a : s.getA()) {
			String i = a.getI();
			if (ICyberTrackerConstants.DATE.equals(i)) {
				DateFormat formatter = new SimpleDateFormat(ICyberTrackerConstants.CT_DATE_FORMAT);
				try {
					boolean shouldBreak = getEndDate() != null; //this mean that time was recorded
					Date date = combine(formatter.parse(a.getV()), getEndDate());
					setEndDate(date);
					if (shouldBreak)
						break;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else if (ICyberTrackerConstants.TIME.equals(i)) {
				boolean shouldBreak = getEndDate() != null; //this mean that date was recorded
				Date date = combine(getEndDate(), Time.valueOf(a.getV()));
				setEndDate(date);
				if (shouldBreak)
					break;
			}
		}
	}

	private Date combine(Date date, Date time) {
		if (date == null)
			return time;
		if (time == null)
			return date;
		return new Date(date.getTime() + time.getTime());
	}
	
	private boolean isMemberRecord(A a) {
		if (!ICyberTrackerConstants.STR_TRUE.equals(a.getV()))
			return false;
		//TODO: check if this is really member (use tag1?)
		return true;
	}

	private <T> T fetchFromTag0(Class<T> clazz, E e, Session session) {
		String tag0 = e != null ? e.getTag0() : null;
		if (tag0 != null) {
			return CyberTrackerHibernateManager.fetchByUuid(clazz, tag0, session);
		}
		return null;
	}

	protected void addError(String error) {
		errors.add(error);
	}
	
	protected void addWarning(String warning) {
		warnings.add(warning);
	}

	public List<String> getErrors() {
		return errors;
	}
	
	public List<String> getWarnings() {
		return warnings;
	}
	
	public Map<String, E> getElementsMap() {
		return elementsMap;
	}

	public List<S> getPatrolData() {
		if (patrolData == null)
			patrolData = new ArrayList<S>();
		return patrolData;
	}

	public Station getStation() {
		return station;
	}

	public Team getTeam() {
		return team;
	}

	public String getObjective() {
		return objective;
	}

	public PatrolMandate getMandate() {
		return mandate;
	}

	public PatrolType.Type getPatrolType() {
		return patrolType;
	}

	public PatrolTransportType getPatrolTransportType() {
		return patrolTransportType;
	}
	
	public boolean isArmed() {
		return isArmed;
	}

	public String getComment() {
		return comment;
	}

	public void setStation(Station station) {
		this.station = station;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public void setObjective(String objective) {
		this.objective = objective;
	}

	public void setMandate(PatrolMandate mandate) {
		this.mandate = mandate;
	}

	public void setPatrolType(PatrolType.Type patrolType) {
		this.patrolType = patrolType;
	}

	public void setPatrolTransportType(PatrolTransportType patrolTransportType) {
		this.patrolTransportType = patrolTransportType;
	}
	
	public void setArmed(boolean isArmed) {
		this.isArmed = isArmed;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Employee getLeader() {
		return leader;
	}

	public void setLeader(Employee leader) {
		this.leader = leader;
	}

	public Employee getPilot() {
		return pilot;
	}

	public void setPilot(Employee pilot) {
		this.pilot = pilot;
	}

	public List<Employee> getMembers() {
		if (members == null)
			members = new ArrayList<Employee>();
		return members;
	}

	public void setMembers(List<Employee> members) {
		this.members = members;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getCtTransport() {
		return ctTransport;
	}

	public void setCtTransport(String ctTransport) {
		this.ctTransport = ctTransport;
	}

	public String getCtStation() {
		return ctStation;
	}

	public void setCtStation(String ctStation) {
		this.ctStation = ctStation;
	}

	public String getCtTeam() {
		return ctTeam;
	}

	public void setCtTeam(String ctTeam) {
		this.ctTeam = ctTeam;
	}

	public String getCtLeader() {
		return ctLeader;
	}

	public void setCtLeader(String ctLeader) {
		this.ctLeader = ctLeader;
	}

	public String getCtPilot() {
		return ctPilot;
	}

	public void setCtPilot(String ctPilot) {
		this.ctPilot = ctPilot;
	}

	public List<String> getCtMembers() {
		if (ctMembers == null)
			ctMembers = new ArrayList<String>();
		return ctMembers;
	}

	public void setCtMembers(List<String> ctMembers) {
		this.ctMembers = ctMembers;
	}
	
}
