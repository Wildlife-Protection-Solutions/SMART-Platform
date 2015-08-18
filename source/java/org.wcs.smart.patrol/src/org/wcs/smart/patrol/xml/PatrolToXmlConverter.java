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

package org.wcs.smart.patrol.xml;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.xml.model.LabelType;
import org.wcs.smart.patrol.xml.model.ObjectiveType;
import org.wcs.smart.patrol.xml.model.PatrolLegDayType;
import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolMemberType;
import org.wcs.smart.patrol.xml.model.PatrolType;
import org.wcs.smart.patrol.xml.model.TrackType;
import org.wcs.smart.patrol.xml.model.WaypointObservationAttributeType;
import org.wcs.smart.patrol.xml.model.WaypointObservationType;
import org.wcs.smart.patrol.xml.model.WaypointType;
import org.wcs.smart.util.SmartUtils;

public class PatrolToXmlConverter {

	/**
	 * Exports a patrol to xml. The patrol must be entire loaded
	 * or associated with an active session.
	 * 
	 * @param p the patrol convert
	 * @return the patrol converted to xml model
	 * 
	 * @throws DatatypeConfigurationException
	 */
	public static PatrolType toXml(Patrol p ) throws DatatypeConfigurationException{
		
		PatrolType xml = new PatrolType();
		
		/* patrol id */
		xml.setId(p.getId());
		
		/* start & end dates */
		xml.setEndDate(SmartUtils.toXmlDate(p.getEndDate()));
		xml.setStartDate(SmartUtils.toXmlDate(p.getStartDate()));
		
		/* armed */
		xml.setIsArmed(p.isArmed());
		
		/* objective */
		ObjectiveType obj = new ObjectiveType();
		obj.setDescription(p.getObjective());
//		obj.setRating(p.getObjectiveRating());
		xml.setObjective(obj);
		
		/* patrol type */
		xml.setPatrolType(p.getPatrolType().name());
		
		/* station */
		xml.setStation(createLabel(p, p.getStation()));
		
		/* team */
		xml.setTeam(createLabel(p, p.getTeam()));

		/* mandate */
		xml.setMandate(createLabel(p, p.getMandate()));

		/* legs */
		for (PatrolLeg leg: p.getLegs()){
			xml.getLegs().add(convertPatrolLeg(leg));
		}
		
		/* comment */
		xml.setComment(p.getComment());
		
		return xml;
		
	}
	
	private static LabelType createLabel(Patrol p, NamedItem object){
		if (object == null){
			return null;
		}
		
		LabelType lbl = new LabelType();
		lbl.setLanguageCode(p.getConservationArea().getDefaultLanguage().getCode());
		lbl.setValue(object.findName(p.getConservationArea().getDefaultLanguage()));
		return lbl;
	}
	
	private static PatrolLegType convertPatrolLeg(PatrolLeg leg) throws DatatypeConfigurationException{
		PatrolLegType xml = new PatrolLegType();
		xml.setEndDate(SmartUtils.toXmlDate(leg.getEndDate()));
		xml.setStartDate(SmartUtils.toXmlDate(leg.getStartDate()));
		xml.setId(leg.getId());
		xml.setTransportType(createLabel(leg.getPatrol(), leg.getType()));
		
		for (PatrolLegMember member : leg.getMembers()){
			xml.getMembers().add(convertPatrolMemeber(member));
		}
		for (PatrolLegDay legday : leg.getPatrolLegDays()){
			xml.getDays().add(convertPatrolLegDay(legday));
		}
		return xml;
		
	}

	private static PatrolMemberType convertPatrolMemeber(PatrolLegMember member){
		PatrolMemberType xml = new PatrolMemberType();
		xml.setIsPilot(member.getIsPilot());
		xml.setIsLeader(member.getIsLeader());
		xml.setGivenName(member.getMember().getGivenName());
		xml.setFamilyName(member.getMember().getFamilyName());
		xml.setEmployeeId(member.getMember().getId());
		
		return xml;
	}
	
	private static PatrolLegDayType convertPatrolLegDay(PatrolLegDay legDay) throws DatatypeConfigurationException{
		
		PatrolLegDayType xml = new PatrolLegDayType();
		xml.setDate(SmartUtils.toXmlDate(legDay.getDate()));
		
		xml.setEndTime(toXmlTime(legDay.getEndTime()));
		xml.setStartTime(toXmlTime(legDay.getStartTime()));
		
		if (legDay.getRestMinutes() ==  null){
			xml.setRestMinutes(0.0);
		}else{
			xml.setRestMinutes((double)legDay.getRestMinutes());
		}
		
		if (legDay.getTrack() != null){
			TrackType track = new TrackType();
			track.setDistance((double)legDay.getTrack().getDistance());
			
			track.setGeom(SmartUtils.encodeGeometry(legDay.getTrack().getGeom()) );
			xml.setTrack(track);
		}
		
		for (PatrolWaypoint wp  : legDay.getWaypoints()){
			xml.getWaypoints().add(convertWaypoint(wp));
		}
		return xml;
	}
	
	private static WaypointType convertWaypoint(PatrolWaypoint wp) throws DatatypeConfigurationException{
		WaypointType xml = new WaypointType();
		xml.setComment(wp.getWaypoint().getComment());
		xml.setDirection(wp.getWaypoint().getDirection());
		xml.setDistance(wp.getWaypoint().getDistance());
		xml.setId(wp.getWaypoint().getId());
		xml.setTime( toXmlTime(wp.getWaypoint().getDateTime()));
		xml.setX(wp.getWaypoint().getX());
		xml.setY(wp.getWaypoint().getY());
		
		for (WaypointAttachment attach : wp.getWaypoint().getAttachments()){
			xml.getAttachments().add(attach.getFilename());
		}
		
		for (WaypointObservation ob : wp.getWaypoint().getObservations()){
			xml.getObservations().add(convertObservation(ob));
		}
		return xml;
	}
	
	
	private static WaypointObservationType convertObservation (WaypointObservation observation){
		WaypointObservationType xml = new WaypointObservationType();
		xml.setCategoryKey(observation.getCategory().getHkey());
		
		for (ObservationAttachment attach : observation.getAttachments()){
			xml.getAttachments().add(attach.getFilename());
		}
		
		if (observation.getObserver() != null){
			PatrolMemberType member = new PatrolMemberType();
			member.setEmployeeId(observation.getObserver().getId());
			member.setFamilyName(observation.getObserver().getFamilyName());
			member.setGivenName(observation.getObserver().getGivenName());
			xml.setObserver(member);
		}
		
		for (WaypointObservationAttribute att : observation.getAttributes()){
			WaypointObservationAttributeType xml2 = new WaypointObservationAttributeType();
			xml2.setAttributeKey(att.getAttribute().getKeyId());
			boolean add = false;
			if (att.getAttribute().getType().equals(AttributeType.BOOLEAN)){
				if (att.getNumberValue() != null){
					xml2.setBValue(att.getNumberValue() >= 0.5);
					add = true;
				}
			}else if (att.getAttribute().getType().equals(AttributeType.LIST)){
				if (att.getAttributeListItem() != null){
					xml2.setItemKey(att.getAttributeListItem().getKeyId());
					add = true;
				}
			}else if (att.getAttribute().getType().equals(AttributeType.NUMERIC)){
				if (att.getNumberValue() != null){
					xml2.setDValue(att.getNumberValue());
					add = true;
				}
			}else if (att.getAttribute().getType().equals(AttributeType.TEXT) ||
					att.getAttribute().getType().equals(AttributeType.DATE)){
				if (att.getStringValue() != null){
					xml2.setSValue(att.getStringValue());
					add = true;
				}
			}else if (att.getAttribute().getType().equals(AttributeType.TREE)){
				if (att.getAttributeTreeNode() != null){
					xml2.setItemKey(att.getAttributeTreeNode().getHkey());
					add = true;
				}
			}
			if (add){
				xml.getAttributes().add(xml2);
			}
		}
		return xml;
	}
	
	/**
	 * Converts a time to an xml time.  Sets the
	 * XML timezone field to undefined so timezone information
	 * is not included in output.
	 * 
	 * @param d
	 * @return
	 * @throws DatatypeConfigurationException
	 */
	private static XMLGregorianCalendar toXmlTime(Date d) throws DatatypeConfigurationException{
		GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
		cal.setTime(d);
		
		XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		xgc.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setYear(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setMonth(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setDay(DatatypeConstants.FIELD_UNDEFINED);
		
		return xgc;
	}
	
}
