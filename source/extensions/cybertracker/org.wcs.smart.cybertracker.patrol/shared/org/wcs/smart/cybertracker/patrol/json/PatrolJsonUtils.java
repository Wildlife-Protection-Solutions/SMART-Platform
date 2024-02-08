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
package org.wcs.smart.cybertracker.patrol.json;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.patrol.model.JsonPatrol;
import org.wcs.smart.cybertracker.patrol.model.PatrolMetadata.JsonPatrolKey;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.patrol.meta.PatrolScreenOptionMeta;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.util.UuidUtils;

/**
 * Parses patrol metadata values from json strings 
 * @author Emily
 *
 */
public class PatrolJsonUtils {
	
	public static DateTimeFormatter DATEFORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd"); //$NON-NLS-1$
	public static DateTimeFormatter TIMEFORMAT = DateTimeFormatter.ofPattern("HH:mm:ss"); //$NON-NLS-1$
	
	public static final String ATTRIBUTE_PREFIX = "SMART_"; //$NON-NLS-1$
	public static final String END_PATROL_KEY = "SMART_EndPatrol"; //$NON-NLS-1$

	/**
	 * 
	 * @param jsonDefaults default values 
	 * @param jsonValues actual selected values (sightings section of feature)
	 * @param session
	 * @return
	 * @throws ParseException 
	 */
	public static JsonPatrol parsePatrolMetadata(JSONObject jsonDefaults, 
			JSONObject jsonValues, ConservationArea ca, Session session, Locale l) throws Exception{
		
		if (jsonValues == null) jsonValues = new JSONObject();
		
		String ptransport = (String)jsonValues.get(PatrolScreenOptionMeta.TRANSPORT.key);
		if (ptransport == null){
			ptransport = (String)jsonDefaults.get(PatrolScreenOptionMeta.TRANSPORT.key);	
		}
		
		Object armed = jsonValues.get(PatrolScreenOptionMeta.ARMED.key);
		if (armed == null) {
			armed = jsonDefaults.get(PatrolScreenOptionMeta.ARMED.key);
		}
		Boolean isArmed = CtJsonUtil.convertToBoolean(armed);
		
		String team = (String)jsonValues.get(PatrolScreenOptionMeta.TEAM.key);
		if (team == null){
			team = (String)jsonDefaults.get(PatrolScreenOptionMeta.TEAM.key);
		}
		String station = (String)jsonValues.get(PatrolScreenOptionMeta.STATION.key);
		if (station == null){
			station = (String)jsonDefaults.get(PatrolScreenOptionMeta.STATION.key);
		}
		String mandate = (String)jsonValues.get(PatrolScreenOptionMeta.MANDATE.key);
		if (mandate == null){
			mandate = (String)jsonDefaults.get(PatrolScreenOptionMeta.MANDATE.key);
		}
		String objective = (String)jsonValues.get(PatrolScreenOptionMeta.OBJECTIVE.key);
		if (objective == null){
			objective = (String)jsonDefaults.get(PatrolScreenOptionMeta.OBJECTIVE.key);
		}
		String comment = (String)jsonValues.get(PatrolScreenOptionMeta.COMMENT.key);
		if (comment == null){
			comment = (String)jsonDefaults.get(PatrolScreenOptionMeta.COMMENT.key);
		}
		String leader = (String)jsonValues.get(PatrolScreenOptionMeta.LEADER.key);
		if (leader == null){
			leader = (String)jsonDefaults.get(PatrolScreenOptionMeta.LEADER.key);
		}
		String pilot = (String)jsonValues.get(PatrolScreenOptionMeta.PILOT.key);
		if (pilot == null){
			pilot = (String)jsonDefaults.get(PatrolScreenOptionMeta.PILOT.key);
		}

		List<String> members = new ArrayList<String>();
		for (Object x : jsonValues.keySet()){
			String key = (String)x;
			if (startsWith(key, CtJsonUtil.JsonDataModelKey.EMPLOYEE.key)) members.add(key);
		}
		if( members.isEmpty()){
			for (Object x : jsonDefaults.keySet()){
				String key = (String)x;
				if (startsWith(key, CtJsonUtil.JsonDataModelKey.EMPLOYEE.key)) members.add(key);
			}	
		}
		JsonPatrol ctPatrol = new JsonPatrol();
		
		if (armed != null){
			ctPatrol.setArmed(isArmed == null ? false : isArmed);
		}
		
		if (team != null && startsWith(team, JsonPatrolKey.TEAM.key)){
			UUID uuid = UuidUtils.stringToUuid(team.substring(JsonPatrolKey.TEAM.key.length() + 1));
			Team teamObj = (Team) session.get(Team.class, uuid);
			if (teamObj == null || !teamObj.getConservationArea().equals(ca)){
				ctPatrol.addWarning(new PatrolJsonImportWarning(PatrolJsonImportWarning.WarningType.TEAM_NOT_FOUND));
			}
			ctPatrol.setTeam(teamObj);
		}
	
		if (station != null && startsWith(station, JsonPatrolKey.STATION.key)){
			UUID uuid = UuidUtils.stringToUuid(station.substring(JsonPatrolKey.STATION.key.length() + 1));
			Station stationObj = (Station) session.get(Station.class, uuid);
			if (stationObj == null || !stationObj.getConservationArea().equals(ca)){
				ctPatrol.addWarning(new PatrolJsonImportWarning(PatrolJsonImportWarning.WarningType.STATION_NOT_FOUND));
			}
			ctPatrol.setStation(stationObj);
		}
		
		if (mandate != null && startsWith(mandate, JsonPatrolKey.MANDATE.key)){
			UUID uuid = UuidUtils.stringToUuid(mandate.substring(JsonPatrolKey.MANDATE.key.length() + 1));
			PatrolMandate mandateObj = (PatrolMandate) session.get(PatrolMandate.class, uuid);
			if (mandateObj == null || !mandateObj.getConservationArea().equals(ca)){
				ctPatrol.addWarning(new PatrolJsonImportWarning(PatrolJsonImportWarning.WarningType.MANDATE_NOT_FOUND));
			}
			ctPatrol.setMandate(mandateObj);
		}
		ctPatrol.setObjective(objective);
		ctPatrol.setComment(comment);
		
		ctPatrol.setMembers(new ArrayList<Employee>());
		for (String member: members){
			UUID uuid = UuidUtils.stringToUuid(member.substring(CtJsonUtil.JsonDataModelKey.EMPLOYEE.key.length() + 1));
			Employee employee = (Employee) session.get(Employee.class, uuid);
			if (employee != null  && employee.getConservationArea().equals(ca)){
				ctPatrol.getMembers().add(employee);
			}else{
				ctPatrol.addWarning(new PatrolJsonImportWarning(PatrolJsonImportWarning.WarningType.MEMBER_NOT_FOUND));
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
			if (transportObj == null || !transportObj.getConservationArea().equals(ca)){
				throw new Exception( (new PatrolJsonImportWarning(PatrolJsonImportWarning.WarningType.TT_NOT_FOUND_ERROR)).getMessage(l));

			}else{
				ctPatrol.setPatrolTransportType(transportObj);
			}
		}
		
		//custom attribute
		List<PatrolAttribute> attributes = QueryFactory.buildQuery(session, PatrolAttribute.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		for (PatrolAttribute pa  : attributes) {
			String key = ATTRIBUTE_PREFIX + UuidUtils.uuidToString( pa.getUuid() );
			
			Object value = jsonValues.get(key);
			if (value == null) continue;
			
			
			if (pa.getType() == Attribute.AttributeType.BOOLEAN) {
				value = CtJsonUtil.convertToBoolean(value);
			}else if (pa.getType() == Attribute.AttributeType.LIST) {
				UUID uuid = UuidUtils.stringToUuid( (String)value );
				for (PatrolAttributeListItem item : pa.getAttributeList()) {
					if (item.getUuid().equals(uuid)) {
						value = item;
						break;
					}
				}
				if (!(value instanceof PatrolAttributeListItem)) value = null;
			}else if (pa.getType() == Attribute.AttributeType.DATE) {
				if (!value.toString().isBlank()) {
					value = LocalDate.parse((String)value, DATEFORMAT);
				}else {
					value = null;
				}
			}
			
			if (value == null) continue;
			
			PatrolAttributeValue pav = new PatrolAttributeValue();
			pav.setPatrolAttribute(pa);
			pav.setAttributeValue(value);
			ctPatrol.addCustomAttributeValue(pav);
		}
		return ctPatrol;
	}

	private static boolean startsWith(String value, String key){
		return value.startsWith(key + CtJsonUtil.KEY_SEP);
	}
}
