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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter.JsonKey;
import org.wcs.smart.cybertracker.patrol.export.PatrolScreensUtil.JsonPatrolKey;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol.PatrolMeta;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.PatrolType.Type;
import org.wcs.smart.util.UuidUtils;

/**
 * Parses patrol metadata values from json strings 
 * @author Emily
 *
 */
public class PatrolJsonUtils {
	
	/**
	 * 
	 * @param jsonDefaults default values 
	 * @param jsonValues actual selected values
	 * @param session
	 * @return
	 */
	public static CyberTrackerPatrol parsePatrolMetadata(JSONObject jsonDefaults, JSONObject jsonValues, Session session){
		
		if (jsonValues == null) jsonValues = new JSONObject();
		
		String ptype = (String)jsonValues.get(PatrolScreensUtil.RESULT_PATROL_TYPE);
		if (ptype == null){
			ptype = (String)jsonDefaults.get(PatrolScreensUtil.RESULT_PATROL_TYPE);	
		}
		String ptransport = (String)jsonValues.get(PatrolScreensUtil.RESULT_TRANSPORT);
		if (ptransport == null){
			ptransport = (String)jsonDefaults.get(PatrolScreensUtil.RESULT_TRANSPORT);	
		}
		
		String armed = (String)jsonValues.get(PatrolScreensUtil.RESULT_ARMED);
		if (armed == null){
			armed = (String)jsonDefaults.get(PatrolScreensUtil.RESULT_ARMED);
		}
		String team = (String)jsonValues.get(PatrolScreensUtil.RESULT_TEAM);
		if (team == null){
			team = (String)jsonDefaults.get(PatrolScreensUtil.RESULT_TEAM);
		}
		String station = (String)jsonValues.get(PatrolScreensUtil.RESULT_STATION);
		if (station == null){
			station = (String)jsonDefaults.get(PatrolScreensUtil.RESULT_STATION);
		}
		String mandate = (String)jsonValues.get(PatrolScreensUtil.RESULT_MANDATE);
		if (mandate == null){
			mandate = (String)jsonDefaults.get(PatrolScreensUtil.RESULT_MANDATE);
		}
		String objective = (String)jsonValues.get(PatrolScreensUtil.RESULT_OBJECTIVE);
		if (objective == null){
			objective = (String)jsonDefaults.get(PatrolScreensUtil.RESULT_OBJECTIVE);
		}
		String comment = (String)jsonValues.get(PatrolScreensUtil.RESULT_COMMENTS);
		if (comment == null){
			comment = (String)jsonDefaults.get(PatrolScreensUtil.RESULT_COMMENTS);
		}
		String leader = (String)jsonValues.get(PatrolScreensUtil.RESULT_LEADER);
		if (leader == null){
			leader = (String)jsonDefaults.get(PatrolScreensUtil.RESULT_LEADER);
		}
		String pilot = (String)jsonValues.get(PatrolScreensUtil.RESULT_PILOT);
		if (pilot == null){
			pilot = (String)jsonDefaults.get(PatrolScreensUtil.RESULT_PILOT);
		}
		List<String> members = new ArrayList<String>();
		for (Object x : jsonValues.keySet()){
			String key = (String)x;
			if (startsWith(key, JsonKey.EMPLOYEE.key)) members.add(key);
		}
		if( members.isEmpty()){
			for (Object x : jsonDefaults.keySet()){
				String key = (String)x;
				if (startsWith(key, JsonKey.EMPLOYEE.key)) members.add(key);
			}	
		}
		CyberTrackerPatrol ctPatrol = new CyberTrackerPatrol(null, null);
		if (armed != null){
			ctPatrol.setArmed(Boolean.valueOf(armed));
		}
		
		if (team != null && startsWith(team, JsonPatrolKey.TEAM.key)){
			UUID uuid = UuidUtils.stringToUuid(team.substring(JsonPatrolKey.TEAM.key.length() + 1));
			Team teamObj = (Team) session.get(Team.class, uuid);
			if (teamObj == null){
				ctPatrol.addWarning(PatrolMeta.TEAM, Messages.PatrolJsonUtils_TeamNotFound);
			}
			ctPatrol.setTeam(teamObj);
		}
	
		if (station != null && startsWith(station, JsonPatrolKey.STATION.key)){
			UUID uuid = UuidUtils.stringToUuid(station.substring(JsonPatrolKey.STATION.key.length() + 1));
			Station stationObj = (Station) session.get(Station.class, uuid);
			if (stationObj == null){
				ctPatrol.addWarning(PatrolMeta.STATION, Messages.PatrolJsonUtils_StationNotFound);
			}
			ctPatrol.setStation(stationObj);
		}
		
		if (mandate != null && startsWith(mandate, JsonPatrolKey.MANDATE.key)){
			UUID uuid = UuidUtils.stringToUuid(mandate.substring(JsonPatrolKey.MANDATE.key.length() + 1));
			PatrolMandate mandateObj = (PatrolMandate) session.get(PatrolMandate.class, uuid);
			if (mandateObj == null){
				ctPatrol.addWarning(PatrolMeta.MANDATE, Messages.PatrolJsonUtils_MandatenotFound);
			}
			ctPatrol.setMandate(mandateObj);
		}
		if (ptype != null){
			if (ptype.startsWith(JsonPatrolKey.PATROL_TYPE.key)){
				ptype = ptype.substring(JsonPatrolKey.PATROL_TYPE.key.length() + 1);
			}
			ctPatrol.setPatrolType(Type.valueOf(ptype));
		}
		ctPatrol.setObjective(objective);
		ctPatrol.setComment(comment);
		
		ctPatrol.setMembers(new ArrayList<Employee>());
		for (String member: members){
			UUID uuid = UuidUtils.stringToUuid(member.substring(JsonKey.EMPLOYEE.key.length() + 1));
			Employee employee = (Employee) session.get(Employee.class, uuid);
			if (employee != null){
				ctPatrol.getMembers().add(employee);
			}else{
				ctPatrol.addWarning(PatrolMeta.MEMBERS, Messages.PatrolJsonUtils_MemberNotFound);
			}
			
			if (member.equals(leader)){
				ctPatrol.setLeader(employee);
			}
			if (member.equals(pilot)){
				ctPatrol.setPilot(employee);
			}
		}
	
		if (ptransport != null && startsWith(ptransport, JsonPatrolKey.TRANSPORT_TYPE.key)){
			UUID uuid = UuidUtils.stringToUuid(ptransport.substring(JsonPatrolKey.TRANSPORT_TYPE.key.length() + 1));
			PatrolTransportType transportObj = (PatrolTransportType) session.get(PatrolTransportType.class, uuid);
			if (transportObj == null){
				ctPatrol.addError(PatrolMeta.TRANSPORT, Messages.PatrolJsonUtils_TTNotFound);
			}else{
				ctPatrol.setPatrolTransportType(transportObj);
			}
			
			
		}
		return ctPatrol;
	}

	private static boolean startsWith(String value, String key){
		return value.startsWith(key + CyberTrackerConfExporter.KEY_SEP);
	}
}
