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
package org.wcs.smart.er.xml;
	
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.xml.model.missions.MembersType;
import org.wcs.smart.er.xml.model.missions.MissionDayType;
import org.wcs.smart.er.xml.model.missions.MissionPropertyValuesType;
import org.wcs.smart.er.xml.model.missions.MissionType;
import org.wcs.smart.er.xml.model.missions.SurveyType;
import org.wcs.smart.er.xml.model.missions.SurveyWaypointsType;
import org.wcs.smart.er.xml.model.missions.TracksType;
import org.wcs.smart.er.xml.model.missions.WaypointObservationAttributeType;
import org.wcs.smart.er.xml.model.missions.WaypointObservationType;
import org.wcs.smart.er.xml.model.missions.WaypointType;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.util.SmartUtils;

public class MissionToXmlConverter {
	/**
	 * Exports a patrol to xml. The patrol must be entire loaded
	 * or associated with an active session.
	 * 
	 * @param m the patrol convert
	 * @return the patrol converted to xml model
	 * 
	 * @throws DatatypeConfigurationException
	 */
	public static MissionType toXml(Mission m ) throws DatatypeConfigurationException{
		
		MissionType xml = new MissionType();
		
		/* mission id */
		xml.setId(m.getId());
		
		/* start & end dates */
		xml.setEndDate(SmartUtils.toXmlDate(m.getEndDate()));
		xml.setStartDate(SmartUtils.toXmlDate(m.getStartDate()));
		
		/* comment */
		xml.setComment(m.getComment() );

		/* survey id */
		xml.setSurveyId(m.getSurvey().getId());
		
		// Survey Object
		xml.setSurvey(convertSurvey(m.getSurvey()));
		
		// members
		for(MissionMember member : m.getMembers()){
			xml.getMembers().add( convertMissionMember(member) );
		}

		for (MissionDay md : m.getMissionDays()){
			MissionDayType xmlmdt = convertMissionDay(md);
			
			// survey waypoints
			for(SurveyWaypoint swp : md.getWaypoints()){
				xmlmdt.getSurveyWaypoints().add(convertSurveyWaypoint(swp));
			}
			
			//tracks
			for(MissionTrack mt : md.getTracks()){
				xmlmdt.getTracks().add(convertMissionTrack(mt));
			}
			
			xml.getDays().add(xmlmdt);
		}
		
		//mission property values
		for(MissionPropertyValue mpv : m.getMissionPropertyValues()){
			xml.getMissionPropertyValues().add(convertMissionPropertyValues(mpv));
		}
		
		return xml;
		
	}
	
	
	private static SurveyType convertSurvey(Survey survey) throws DatatypeConfigurationException {
		SurveyType xmlSurvey = new SurveyType();
		
		xmlSurvey.setEndDate(SmartUtils.toXmlDate(survey.getEndDate()));
		xmlSurvey.setStartDate(SmartUtils.toXmlDate(survey.getStartDate()) );
		xmlSurvey.setId(survey.getId());
		
		xmlSurvey.setSurveyDesignKeyId(survey.getSurveyDesign().getKeyId());

		
		return xmlSurvey;
	}


	private static MissionPropertyValuesType convertMissionPropertyValues(MissionPropertyValue mpv) {
		MissionPropertyValuesType xmlmpv = new MissionPropertyValuesType ();
		
		if(mpv.getAttributeListItem() != null){
			xmlmpv.setListElementKeyId(mpv.getAttributeListItem().getKeyId());
		}
		xmlmpv.setMissionAttributeKeyId(mpv.getMissionAttribute().getKeyId());
		xmlmpv.setNumberValue(mpv.getNumberValue());
		xmlmpv.setStringValue(mpv.getStringValue());
		return xmlmpv;
	}
	
	private static MissionDayType convertMissionDay(MissionDay md) throws DatatypeConfigurationException {
		MissionDayType xml = new MissionDayType ();
		xml.setDate(SmartUtils.toXmlDate(md.getDate()));
		xml.setStartTime(toXmlTime(md.getStartTime()));
		xml.setEndTime(toXmlTime(md.getEndTime()));
		xml.setRestMinutes(md.getRestMinutes());
		return xml;
		
	}


	private static TracksType convertMissionTrack(MissionTrack mt) throws DatatypeConfigurationException{
		TracksType track = new TracksType();
		track.setGeom(mt.getGeom());
		track.setId(mt.getId());
		SamplingUnit su = mt.getSamplingUnit();
		if(su != null){
			track.setSamplingUnitId(su.getId());
		}
		track.setTrackType(mt.getType().toString());
		return track;
	}


	private static SurveyWaypointsType convertSurveyWaypoint(SurveyWaypoint swp)throws DatatypeConfigurationException {
		SurveyWaypointsType xml = new SurveyWaypointsType();
		if(swp.getMissionTrack() != null){
			xml.setMissionTrackId(swp.getMissionTrack().getId());
		}
		if(swp.getSamplingUnit() != null){
			xml.setSamplingUnitId(swp.getSamplingUnit().getId());
		}
		xml.setWaypoints(convertWaypoint(swp));
		
		return xml;
	}


	private static MembersType convertMissionMember(MissionMember member){
		MembersType xml = new MembersType();
		xml.setEmployeeId(member.getMember().getId());
		xml.setLeader(member.getIsLeader());
		xml.setFamilyName(member.getMember().getFamilyName());
		xml.setGivenName(member.getMember().getGivenName());
		return xml;
	}
	

	private static WaypointType convertWaypoint(SurveyWaypoint wp) throws DatatypeConfigurationException{
		WaypointType xml = new WaypointType();
		xml.setComment(wp.getWaypoint().getComment());
		xml.setDirection(wp.getWaypoint().getDirection());
		xml.setDistance(wp.getWaypoint().getDistance());
		xml.setId(wp.getWaypoint().getId());
		xml.setDateTime( toXmlDateTime(wp.getWaypoint().getDateTime()));
		
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
	private static XMLGregorianCalendar toXmlDateTime(Date dateTime) throws DatatypeConfigurationException {
		GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
		cal.setTime(dateTime);
		
		XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		xgc.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
		
		return xgc;
	}	
}


