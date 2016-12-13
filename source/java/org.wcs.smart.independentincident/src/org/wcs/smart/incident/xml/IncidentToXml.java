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
package org.wcs.smart.incident.xml;

import java.text.SimpleDateFormat;

import javax.xml.datatype.DatatypeConfigurationException;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.incident.xml.model.EmployeeType;
import org.wcs.smart.incident.xml.model.WaypointObservationAttributeType;
import org.wcs.smart.incident.xml.model.WaypointObservationType;
import org.wcs.smart.incident.xml.model.WaypointType;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

/**
 * Utilities for converting a waypoint to xml waypoint.
 * 
 * @author Emily
 *
 */
public class IncidentToXml {

	public static final String DATE_FORMAT_STR = ("yyyy-MM-dd HH:mm:ss.SSS");  //$NON-NLS-1$
	
	public static WaypointType toXml(Waypoint incident) throws DatatypeConfigurationException{
		WaypointType wt = new WaypointType();
		wt.setDateTime(new SimpleDateFormat(DATE_FORMAT_STR).format(incident.getDateTime()));
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
			
			if (ob.getObserver() != null){
				EmployeeType observer = new EmployeeType();
				observer.setEmployeeId(ob.getObserver().getId());
				observer.setFamilyName(ob.getObserver().getFamilyName());
				observer.setGivenName(ob.getObserver().getGivenName());
				wot.setObserver(observer);
			}
			
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
					wot.getAttributes().add(xml2);
				}
			}		
			wt.getObservations().add(wot);
		}
		
		return wt;
	}
	
}
