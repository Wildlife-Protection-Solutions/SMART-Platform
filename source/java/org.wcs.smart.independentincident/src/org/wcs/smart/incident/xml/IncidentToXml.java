package org.wcs.smart.incident.xml;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.incident.xml.model.WaypointObservationAttributeType;
import org.wcs.smart.incident.xml.model.WaypointObservationType;
import org.wcs.smart.incident.xml.model.WaypointType;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

public class IncidentToXml {

	
	public static WaypointType toXml(Waypoint incident) throws DatatypeConfigurationException{
		WaypointType wt = new WaypointType();
		
		Calendar c = Calendar.getInstance();
		c.setTime(incident.getDateTime());
		wt.setDateTime(DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar)c));
		
		wt.setComment(incident.getComment());
		wt.setDirection(incident.getDirection());
		wt.setDistance(incident.getDistance());
		wt.setId(incident.getId());
		wt.setX(incident.getX());
		wt.setY(incident.getY());
		
		for (WaypointAttachment attach : incident.getAttachments()){
			wt.getAttachments().add(attach.getFilename());
		}
		
		
		for (WaypointObservation ob  : incident.getObservations()){
			WaypointObservationType wot = new WaypointObservationType();
			wot.setCategoryKey(ob.getCategory().getHkey());
			
			for (ObservationAttachment attach : ob.getAttachments()){
				wot.getAttachments().add(attach.getFilename());
			}
			
			for (WaypointObservationAttribute att : ob.getAttributes()){
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
				}else if (att.getAttribute().getType().equals(AttributeType.TEXT)){
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
					wot.getAttributes().add(xml2);
				}
			}		
			wt.getObservations().add(wot);
		}
		
		return wt;
	}
	
}
