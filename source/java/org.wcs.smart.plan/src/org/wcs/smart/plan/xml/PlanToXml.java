/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.plan.xml;

import java.nio.file.Path;
import java.time.LocalDate;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTargetPoint;
import org.wcs.smart.plan.xml.model.ObjectFactory;
import org.wcs.smart.plan.xml.model.XmlName;
import org.wcs.smart.plan.xml.model.XmlPlan;
import org.wcs.smart.plan.xml.model.XmlPlanTarget;
import org.wcs.smart.plan.xml.model.XmlPlanTargetPoint;
import org.wcs.smart.util.SmartUtils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

/**
 * Converts SMART plan object to xml file.
 * 
 * The current plan and all children, and parents are included
 * in the export, but siblings are not included.
 * 
 * @author Emily
 *
 */
public class PlanToXml {

	/**
	 * Convert plan object to xml file
	 * @param plan
	 * @param file
	 * @throws Exception
	 */
	public void convertPlan(Plan plan, Path file) throws Exception {
		XmlPlan out = convertPlanInternal(plan, true);
		writeXmlFile(file, out);
	}
	
	private XmlPlan convertPlanInternal(Plan plan, boolean processKids) throws DatatypeConfigurationException {
		XmlPlan xml = new XmlPlan();
		xml.setAvaliableEmployees(plan.getActiveEmployees());
		xml.setUnavaliableEmployees(plan.getUnavailableEmployees());
		xml.setComment(plan.getComment());
		xml.setId(plan.getId());
		
		for (org.wcs.smart.ca.Label l : plan.getNames()) {
			XmlName xname = new XmlName();
			xname.setLanguage(l.getLanguage().getCode());
			xname.setValue(l.getValue());
			xname.setIsdeafult(l.getLanguage().isDefault());
			xml.getNames().add(xname);
		}
		
		xml.setDescription(plan.getDescription());
		if (plan.getParent() != null) xml.setParentid(plan.getParent().getId());
		
		if (plan.getEndDate() != null) xml.setEndDate(convertDateTime(plan.getEndDate()));
		if (plan.getStartDate() != null) xml.setStartDate(convertDateTime(plan.getStartDate()));
		if (plan.getStation() != null) xml.setStation(plan.getStation().getName());
		if (plan.getTeam() != null) xml.setTeam(plan.getTeam().getName());
		xml.setType(plan.getType().name());
		
		if (plan.getParent() != null) {
			XmlPlan parentXml = convertPlanInternal(plan.getParent(), false);
			xml.setParent(parentXml);
		}
		for (PlanTarget t : plan.getTargets()) {
			xml.getTargets().add(convertTarget(t));
		}
		
		if (processKids) {
			for (Plan kid : plan.getChildren()) {
				XmlPlan xmlkid = convertPlanInternal(kid, true);
				xml.getChildren().add(xmlkid);
			}
		}
		
		return xml;
	}
	
	
	private XmlPlanTarget convertTarget(PlanTarget pt) {
		
		XmlPlanTarget xml = new XmlPlanTarget();
		xml.setName(pt.getName());
		
		if (pt instanceof AdministrativePlanTarget) {
			AdministrativePlanTarget at = (AdministrativePlanTarget)pt;
			
			xml.setType(AdministrativePlanTarget.class.getName());
			xml.setCompleted(at.getStatus());
			xml.setDescription(at.getTargetDesc());
			
		}else if (pt instanceof NumericPlanTarget) {
			NumericPlanTarget nt = (NumericPlanTarget)pt;
			
			xml.setType(NumericPlanTarget.class.getName() + ":" + nt.getType().name()); //$NON-NLS-1$
			xml.setDescription(nt.getDescription());
			xml.setOperator(nt.getOp().name());
			xml.setValue(nt.getValue());
			
		}else if (pt instanceof SpatialPlanTarget) {
			SpatialPlanTarget st = (SpatialPlanTarget)pt;

			xml.setType(SpatialPlanTarget.class.getName());
			xml.setDescription(st.getDescription());
			xml.setSuccessDistance(st.getDistanceForCompletion());
			for (SpatialPlanTargetPoint target : st.getPoints()) {
				XmlPlanTargetPoint xt = new XmlPlanTargetPoint();
				xt.setX(target.getX());
				xt.setY(target.getY());
				xml.getPoints().add(xt);
			}
			
		}
		return xml;
		
	}
	
	private void writeXmlFile(Path xmlFile, XmlPlan plan) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		ObjectFactory objFactor = new ObjectFactory();
		JAXBElement<XmlPlan> element = objFactor.createPlan(plan);
		marshaller.marshal(element, xmlFile.toAbsolutePath().normalize().toFile());
	}
	
	
	private XMLGregorianCalendar convertDateTime(LocalDate datetime) throws DatatypeConfigurationException {
		return SmartUtils.toXmlDate(datetime);
	}
}

