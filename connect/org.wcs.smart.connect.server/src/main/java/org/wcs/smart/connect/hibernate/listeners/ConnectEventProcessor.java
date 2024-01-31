/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.connect.hibernate.listeners;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.hibernate.AttachmentInterceptor;
import org.wcs.smart.event.EventProcessor;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.WaypointObservation;

/**
 * Thread for executing events when new waypoint observations are created
 * 
 * @author Emily
 *
 */
public class ConnectEventProcessor implements Runnable {

	private WaypointObservation wo;
	private SessionFactory sessionFactory;
	private Locale l;
	
	public ConnectEventProcessor(WaypointObservation wo, SessionFactory sessionFactory, Locale l) {
		this.wo = wo;
		this.sessionFactory = sessionFactory;
		this.l = l;
	}
	
	@Override
	public void run() {
		AttachmentInterceptor interceptor = new AttachmentInterceptor(false);
		try(Session session = sessionFactory.withOptions().interceptor(interceptor).openSession()){
			interceptor.setSession(session, l);
			wo = session.get(WaypointObservation.class, wo.getUuid());
			if (wo == null) return;
		
			for(EActionEvent event : getEventActions(session, wo.getWaypoint().getConservationArea())) {
				if (!event.isEnabled()) continue;
				try {
					EventProcessor.INSTANCE.processEvent(event, wo, null, session);
				}catch (Exception ex) {
					Logger.getLogger(ConnectEventProcessor.class.getName()).log(Level.WARNING, "Unable to process events associated with new waypoint observation: " + ex.getMessage(), ex); //$NON-NLS-1$
				}
			}
		}
	}
	
	private List<EActionEvent> getEventActions(Session session, ConservationArea ca){
		List<EActionEvent> events = QueryFactory.buildQuery(session, EActionEvent.class, 
					new Object[] {"action.conservationArea", ca}).list(); //$NON-NLS-1$
		events.forEach(evt->{
			evt.getAction().getParameters().forEach(p->p.getId().getParameterKey());
			evt.getFilter().getFilterString();
		});
		return events;
	}

}
