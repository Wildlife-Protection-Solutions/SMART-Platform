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

import java.util.ArrayList;
import java.util.Collections;
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

	private SessionFactory sessionFactory = null;
	private List<Object[]> tasks = Collections.synchronizedList(new ArrayList<>());
	private Thread currentRunnable = null;

	
	public ConnectEventProcessor(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * Register a new observation attribute for events 
	 */
	public void addTask(WaypointObservation wo , Locale l){
		tasks.add(new Object[] {wo, l});
		start();
	}
	
	private synchronized void start() {
		if (currentRunnable != null && currentRunnable.isAlive()) return;
		currentRunnable = new Thread(this);
		currentRunnable.start();
	}
	
	
	@Override
	public void run() {
		AttachmentInterceptor interceptor = new AttachmentInterceptor(false);
		try(Session session = sessionFactory.withOptions().interceptor(interceptor).openSession()){
			while(!tasks.isEmpty()) {
				Object[] data = tasks.remove(0);
				WaypointObservation wo = (WaypointObservation) data[0];
				Locale l = (Locale)data[1];
			
				interceptor.setSession(session, l);
				
				wo = session.get(WaypointObservation.class, wo.getUuid());
				
				if (wo == null) continue;
		
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
	}
	
	private List<EActionEvent> getEventActions(Session session, ConservationArea ca){
		return QueryFactory.buildQuery(session, EActionEvent.class, 
					new Object[] {"action.conservationArea", ca}).list(); //$NON-NLS-1$
	}

}
