package org.wcs.smart.patrol.xml;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointAttachment;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
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
		/* start & end dates */
		xml.setEndDate(toXmlDate(p.getEndDate()));
		xml.setStartDate(toXmlDate(p.getStartDate()));
		
		/* armed */
		xml.setIsArmed(p.isArmed());
		
		/* objective */
		ObjectiveType obj = new ObjectiveType();
		obj.setDescription(p.getObjective());
		obj.setRating(p.getObjectiveRating());
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
		return xml;
		
	}
	
	private static LabelType createLabel(Patrol p, SimpleListItem object){
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
		xml.setEndDate(toXmlDate(leg.getEndDate()));
		xml.setStartDate(toXmlDate(leg.getStartDate()));
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
		xml.setDate(toXmlDate(legDay.getDate()));
		xml.setEndTime(toXmlDate(legDay.getEndTime()));
		xml.setStartTime(toXmlDate(legDay.getStartTime()));
		if (legDay.getRestMinutes() ==  null){
			xml.setRestMinutes(0.0);
		}else{
			xml.setRestMinutes((double)legDay.getRestMinutes());
		}
		
		if (legDay.getTrack() != null){
			TrackType track = new TrackType();
			track.setDistance((double)legDay.getTrack().getDistance());
			
			track.setGeom(SmartUtils.encodeHex(legDay.getTrack().getGeom()) );
			xml.setTrack(track);
		}
		
		for (Waypoint wp  : legDay.getWaypoints()){
			xml.getWaypoints().add(convertWaypoint(wp));
		}
		return xml;
	}
	
	private static WaypointType convertWaypoint(Waypoint wp) throws DatatypeConfigurationException{
		WaypointType xml = new WaypointType();
		xml.setComment(wp.getComment());
		xml.setDirection(wp.getDirection());
		xml.setDistance(wp.getDistance());
		xml.setId(wp.getId());
		xml.setTime( toXmlDate(wp.getTime()));
		xml.setX(wp.getX());
		xml.setY(wp.getY());
		
		for (WaypointAttachment attach : wp.getAttachments()){
			xml.getAttachments().add(attach.getFilename());
		}
		
		for (WaypointObservation ob : wp.getObservations()){
			xml.getObservations().add(convertObservation(ob));
		}
		return xml;
	}
	
	
	private static WaypointObservationType convertObservation (WaypointObservation observation){
		WaypointObservationType xml = new WaypointObservationType();
		xml.setCategoryKey(observation.getCategory().getHkey());
		
		for (WaypointObservationAttribute att : observation.getAttributes()){
			WaypointObservationAttributeType xml2 = new WaypointObservationAttributeType();
			xml2.setAttributeKey(att.getAttribute().getKeyId());
			if (att.getAttribute().getType().equals(AttributeType.BOOLEAN)){
				xml2.setBValue(att.getNumberValue() >= 0.5);
			}else if (att.getAttribute().getType().equals(AttributeType.LIST)){
				xml2.setItemKey(att.getAttributeListItem().getKeyId());
			}else if (att.getAttribute().getType().equals(AttributeType.NUMERIC)){
				xml2.setDValue(att.getNumberValue());
			}else if (att.getAttribute().getType().equals(AttributeType.TEXT)){
				xml2.setSValue(att.getStringValue());
			}else if (att.getAttribute().getType().equals(AttributeType.TREE)){
				xml2.setItemKey(att.getAttributeTreeNode().getFullKey());
			}
			xml.getAttributes().add(xml2);
		}
		return xml;
	}
	
	
	private static XMLGregorianCalendar toXmlDate(Date d) throws DatatypeConfigurationException{
		GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
		cal.setTime(d);
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
	}
	

}
