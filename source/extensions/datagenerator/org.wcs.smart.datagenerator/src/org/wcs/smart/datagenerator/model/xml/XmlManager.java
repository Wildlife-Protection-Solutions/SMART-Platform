/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.datagenerator.model.xml;

import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.datagenerator.model.ObservationConfiguration;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Tools for reading and writing xml files to/from patrol configurations.  Warnings
 * are currently consumed by the program and not shown to the user. 
 * 
 * @author Emily
 *
 */
public enum XmlManager {
	INSTANCE;
	
	public static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.datagenerator.model.xml"; //$NON-NLS-1$

	
	public void writeXmlFile(Path file, org.wcs.smart.datagenerator.model.PatrolConfiguration config) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
		ObjectFactory objFactor = new ObjectFactory();
		
		DataGeneratorConfiguration xml = toXml(config);
		
		JAXBElement<DataGeneratorConfiguration> element = objFactor.createDataGeneratorConfiguration(xml);
		marshaller.marshal(element, file.toFile());
	}
	
	public org.wcs.smart.datagenerator.model.PatrolConfiguration readXmlFile(Path file, Session session) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Unmarshaller un = context.createUnmarshaller();	
		@SuppressWarnings("unchecked")
		JAXBElement<DataGeneratorConfiguration> o = (JAXBElement<DataGeneratorConfiguration>) un.unmarshal(file.toFile());
		DataGeneratorConfiguration x = o.getValue();
		return toLocal(x,session);
	}

	
	private  org.wcs.smart.datagenerator.model.PatrolConfiguration toLocal(DataGeneratorConfiguration c, Session session){
		 org.wcs.smart.datagenerator.model.PatrolConfiguration local = new  org.wcs.smart.datagenerator.model.PatrolConfiguration();
		 
		 local.setDaysPerPatrolMax(c.getDaysPerPatrolMax());
		 local.setDaysPerPatrolMin(c.getDaysPerPatrolMin());
		 local.setEmployeesPerPatrolMax(c.getEmployeesPerPatrolMax());
		 local.setEmployeesPerPatrolMin(c.getEmployeesPerPatrolMin());
		 
		 local.setEndDate( LocalDate.parse(c.getEndDate()) );
		 local.setStartDate( LocalDate.parse(c.getStartDate()) );
		 
		 local.setNumberOfPatrols(c.getNumberOfPatrols());
		 local.setObservationsPerWaypointMax(c.getObservationsPerWaypointMax());
		 local.setObservationsPerWaypointMin(c.getObservationsPerWaypointMin());
		 local.setWaypointsPerDayMax(c.getWaypointsPerDayMax());
		 local.setWaypointsPerDayMin(c.getWaypointsPerDayMin());
		 
		 if (c.getBoundingBox().getAreaType() != null && !c.getBoundingBox().getAreaType().isEmpty()) {
			 Area.AreaType t = Area.AreaType.valueOf(c.getBoundingBox().getAreaType());
			 local.setBboxArea(t);
		 }else {
			 local.setBboxEnvelope(new Envelope(c.getBoundingBox().getXMin(), c.getBoundingBox().getXMax(), c.getBoundingBox().getYMin(), c.getBoundingBox().getYMax()));
		 }
		 
		 local.setMappings(new ArrayList<>());
		 
		 for (ObservationMappingType mapping : c.getMappings()) {
			 String ckey = mapping.getCategory();
			 int weight = mapping.getWeight();
			 
			 //search session for category key
			 Category category = session.createQuery("FROM Category WHERE conservationArea = :ca and hkey = :hkey", Category.class ) //$NON-NLS-1$
					 .setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					 .setParameter("hkey", ckey) //$NON-NLS-1$
					 .uniqueResult();
			 if (category == null) {
				 //TODO: warning message
				 continue;
			 }
			 
			 WaypointObservation wo = new WaypointObservation();
			 wo.setCategory(category);
			 wo.setAttributes(new ArrayList<>());
			 
			 HashMap<Attribute, ObservationConfiguration.Type> types = new HashMap<>();
			 List<Attribute> all = new ArrayList<>();
			 category.getAllAttribute(all, null);
			 
			 for (AttributeType a : mapping.getAttributes()) {
				 for (Attribute attribute : all) {
					 if (attribute.getKeyId().equals(a.getAttribute())) {
						 ObservationConfiguration.Type type = ObservationConfiguration.Type.valueOf(a.getMappingType());
						 types.put(attribute, type);
						 if (type == ObservationConfiguration.Type.FIXED) {
							 boolean add = false;
							 WaypointObservationAttribute woa = new WaypointObservationAttribute();
							 woa.setAttribute(attribute);
							 if (attribute.getType() == Attribute.AttributeType.BOOLEAN) {
								 woa.setNumberValue(a.getDoubleValue());
								 add = true;
							 }else if (attribute.getType() == Attribute.AttributeType.DATE) {
								 woa.setDateValue(Date.valueOf(a.getStringValue()));
								 add = true;
							 }else if (attribute.getType() == Attribute.AttributeType.LIST) {
								 AttributeListItem item = null;
								 for (AttributeListItem i : attribute.getAttributeList()) {
									 if (i.getKeyId().equals(a.getStringValue())) {
										 item = i;
										 break;
									 }
								 }
								 if (item != null) {
									 add = true;
									 woa.setAttributeListItem(item);
								 }
							 }else if (attribute.getType() == Attribute.AttributeType.NUMERIC) {
								 woa.setNumberValue(a.getDoubleValue());
								 add = true;
							 }else if (attribute.getType() == Attribute.AttributeType.TEXT) {
								 woa.setStringValue(a.getStringValue());
								 add = true;
							 }else if (attribute.getType() == Attribute.AttributeType.TREE) {
								 ArrayDeque<AttributeTreeNode> search = new ArrayDeque<>();
								 search.addAll(attribute.getTree());
								 AttributeTreeNode item = null;
								 while(!search.isEmpty()) {
									 AttributeTreeNode n = search.removeFirst();
									 if (n.getHkey().equals(a.getStringValue())) {
										 item = n;
										 break;
									 }
									 if (n.getChildren() != null) search.addAll(n.getChildren());
								 }
								 
								 if (item != null) {
									 add = true;
									 woa.setAttributeTreeNode(item);
								 }
							 }
							 
							 if (add) {
								 wo.getAttributes().add(woa);
							 }else {
								 types.put(attribute, ObservationConfiguration.Type.EMPTY);
							 }
						 }
					 }
					 
				 }
			 }
			 ObservationConfiguration m = new ObservationConfiguration(wo, types);
			 m.setWeight(weight);
			 local.getMappings().add(m);
		 }
		 return local;
	}
	
	private DataGeneratorConfiguration toXml(org.wcs.smart.datagenerator.model.PatrolConfiguration config) {
		DataGeneratorConfiguration xml = new DataGeneratorConfiguration();
		
		xml.setDaysPerPatrolMax(config.getDaysPerPatrolMax());
		xml.setDaysPerPatrolMin(config.getDaysPerPatrolMin());
		xml.setEmployeesPerPatrolMax(config.getEmployeesPerPatrolMax());
		xml.setEmployeesPerPatrolMin(config.getEmployeesPerPatrolMin());
		 
		xml.setEndDate(  config.getEndDate().toString() );
		xml.setStartDate( config.getStartDate().toString() );
		 
		xml.setNumberOfPatrols(config.getNumberOfPatrols());
		xml.setObservationsPerWaypointMax(config.getObservationsPerWaypointMax());
		xml.setObservationsPerWaypointMin(config.getObservationsPerWaypointMin());
		xml.setWaypointsPerDayMax(config.getWaypointsPerDayMax());
		xml.setWaypointsPerDayMin(config.getWaypointsPerDayMin());
		 
		xml.setBoundingBox(new BoundingBoxType());
		
		if (config.getBboxArea() != null) {
			xml.getBoundingBox().setAreaType(config.getBboxArea().name());
		}else {
			xml.getBoundingBox().setXMax(config.getBboxEnvelope().getMaxX());
			xml.getBoundingBox().setXMin(config.getBboxEnvelope().getMinX());
			xml.getBoundingBox().setYMax(config.getBboxEnvelope().getMaxX());
			xml.getBoundingBox().setYMin(config.getBboxEnvelope().getMinY());
		}
		 
		for (ObservationConfiguration m : config.getMappings()) {
			ObservationMappingType xmlMapping = new ObservationMappingType();
			xmlMapping.setCategory(m.getObservation().getCategory().getHkey());
			xmlMapping.setWeight(m.getWeight());
			 
			xml.getMappings().add(xmlMapping);
			 
			for (Attribute a : m.getAttributes()) {
				AttributeType xmlAttribute = new AttributeType();
				xmlAttribute.setMappingType(m.getType(a).name());
				xmlAttribute.setAttribute(a.getKeyId());
				 
				xmlMapping.getAttributes().add(xmlAttribute);
				if (m.getType(a) == ObservationConfiguration.Type.FIXED) {
					WaypointObservationAttribute woa = null;
					for (WaypointObservationAttribute ww : m.getObservation().getAttributes()) {
						if (ww.getAttribute().equals(a)) {
							woa = ww;
							break;
						}
					}
					if (woa == null) {
						xmlAttribute.setMappingType(ObservationConfiguration.Type.EMPTY.name());
						break;
					}

					switch (a.getType()) {
					case BOOLEAN:
						xmlAttribute.setDoubleValue(woa.getNumberValue());
						break;
					case DATE:
						xmlAttribute.setStringValue(woa.getStringValue());
						break;
					case LIST:
						xmlAttribute.setStringValue(woa.getAttributeListItem().getKeyId());
						break;
					case NUMERIC:
						xmlAttribute.setDoubleValue(woa.getNumberValue());
						break;
					case TEXT:
						xmlAttribute.setStringValue(woa.getStringValue());
						break;
					case TREE:
						xmlAttribute.setStringValue(woa.getAttributeTreeNode().getHkey());
						break;
					}
				}
			}
		}
		return xml;
	}
}
