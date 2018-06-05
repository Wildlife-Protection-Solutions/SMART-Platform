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
package org.wcs.smart.event.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

/**
 * Tools for exporting action, filters, and action events for the events
 * plug in to xml format.  (Allows for exporting and importing of
 * configurations).
 * 
 * 
 * @author Emily
 *
 */
public class EventsToXml {
	
	private static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.event.xml.model"; //$NON-NLS-1$
		
	/**
	 * Writes a xml patrol object to a file.
	 * <p>
	 * User is required to close output stream.
	 * </p>
	 * @param patrol xml patrol to write
	 * @param file output stream 
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static void writeEventXml(org.wcs.smart.event.xml.model.Configuration evtConfig, OutputStream file) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		
		org.wcs.smart.event.xml.model.ObjectFactory objFactory = new org.wcs.smart.event.xml.model.ObjectFactory();
		
		JAXBElement<org.wcs.smart.event.xml.model.Configuration> element = objFactory.createConfiguration(evtConfig);
		marshaller.marshal(element, file);
	}
	
	private ConservationArea ca;
	
	public EventsToXml(ConservationArea ca) {
		this.ca = ca;
	}
	
	/**
	 * Converts all actions, filter and action events to xml format
	 * and writes to the provided output file.
	 * 
	 * @param outputFile
	 */
	public void toXml(Path outputFile) throws Exception
	{
		org.wcs.smart.event.xml.model.Configuration xml = toXml();
		
		try(OutputStream stream = Files.newOutputStream(outputFile)){
			writeEventXml(xml, stream);
		}
	}
	
	/**
	 * Converts all actions, filter, and action events to xml format for
	 * the provided conservation area.
	 * 
	 * @return
	 */
	public org.wcs.smart.event.xml.model.Configuration toXml() {
		org.wcs.smart.event.xml.model.Configuration xmlConfig = new org.wcs.smart.event.xml.model.Configuration();
		try(Session session = HibernateManager.openSession()){
			convertActions(session, xmlConfig);
			convertFilters(session, xmlConfig);
			convertActionEvents(session, xmlConfig);
		}
		return xmlConfig;
	}
	
	private void convertActions(Session session, org.wcs.smart.event.xml.model.Configuration xmlConfig) {
		List<EAction> actions = QueryFactory.buildQuery(session, EAction.class, new Object[] {"conservationArea", ca}).list();
		for (EAction a : actions) {
			org.wcs.smart.event.xml.model.EAction xmlAction = new org.wcs.smart.event.xml.model.EAction();
			xmlAction.setActionTypeKey(a.getActionTypeKey());
			xmlAction.setId(a.getId());
			xmlAction.setUuid( UuidUtils.uuidToString(a.getUuid()));
			
			for (EActionParameterValue p : a.getParameters()) {
				org.wcs.smart.event.xml.model.EActionParameter xmlParameter = new org.wcs.smart.event.xml.model.EActionParameter();
				xmlParameter.setKey(p.getId().getParameterKey());
				xmlParameter.setValue(p.getParameterValue());
				
				xmlAction.getParameters().add(xmlParameter);
			}
			xmlConfig.getActions().add(xmlAction);
		}
	}
	
	private void convertFilters(Session session, org.wcs.smart.event.xml.model.Configuration xmlConfig) {
		List<EFilter> filters = QueryFactory.buildQuery(session, EFilter.class, new Object[] {"conservationArea", ca}).list();
		for (EFilter f : filters) {
			org.wcs.smart.event.xml.model.EFilter xmlFilter = new org.wcs.smart.event.xml.model.EFilter();
			xmlFilter.setId(f.getId());
			xmlFilter.setFilterString(f.getFilterString());
			xmlFilter.setUuid( UuidUtils.uuidToString(f.getUuid()) );
			xmlConfig.getFilters().add(xmlFilter);
		}
	}
	
	private void convertActionEvents(Session session, org.wcs.smart.event.xml.model.Configuration xmlConfig) {
		List<EActionEvent> actionEvents = QueryFactory.buildQuery(session, EActionEvent.class, new Object[] {"action.conservationArea", ca}).list();
		for (EActionEvent ae : actionEvents) {
			org.wcs.smart.event.xml.model.EActionEvent xmlActionEvent = new org.wcs.smart.event.xml.model.EActionEvent();
			
			xmlActionEvent.setEFilterUuid( UuidUtils.uuidToString( ae.getFilter().getUuid() ));
			xmlActionEvent.setEActionUuid( UuidUtils.uuidToString( ae.getAction().getUuid() ));
			xmlActionEvent.setEnabled(ae.isEnabled());
			
			xmlConfig.getActionEvents().add(xmlActionEvent);
		}
	}
	
}
