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
package org.wcs.smart.cybertracker.patrol.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.model.AbstractCyberTrackerData;
import org.wcs.smart.cybertracker.model.IDataMeta;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.patrol.export.PatrolScreensUtil;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Model representing single patrol imported from CyberTracker application
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerPatrol extends AbstractCyberTrackerData {
	
	public enum PatrolMeta implements IDataMeta {
		START_DATE,
		END_DATE,
		TYPE,
		TRANSPORT,
		ARMED,
		MANDATE,
		TEAM,
		STATION,
		OBJECTIVE,
		COMMENT,
		LEADER,
		PILOT,
		MEMBERS
	}
	
	private Station station;
	private Team team;
	private String objective;
	private PatrolMandate mandate;
	private PatrolType.Type patrolType;
	private PatrolTransportType patrolTransportType;
	private boolean isArmed = false;
	private String comment;
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

	public CyberTrackerPatrol(Map<String, E> elementsMap, List<S> sData) {
		super(elementsMap, sData);
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

	@Override
	public String getType() {
		return PatrolScreensUtil.DATATYPE_PATROL;
	}

	@Override
	public String getDisplayType() {
		return "Patrol";
	}

	@Override
	public String getDetails() {
		// TODO Auto-generated method stub
		return "PLACE PATROL DETAILS HERE!!!!";
	}
	
}
