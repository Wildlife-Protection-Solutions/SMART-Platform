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
package org.wcs.smart.event;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.event.internal.Messages;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.WaypointObservation;

import jakarta.persistence.LockTimeoutException;

/**
 * Job for processing observation events. There is a single job
 * accessed through the getInstance() function.
 * 
 * @author Emily
 *
 */
public class EventProcessingJob extends Job {

	private static EventProcessingJob INSTANCE = null;
	
	public static synchronized EventProcessingJob getInstance() {
		if (INSTANCE == null) INSTANCE = new EventProcessingJob();
		return INSTANCE;
	}
	
	private List<EActionEvent> cachedEvents = null;
	private List<WaypointObservation> observations = Collections.synchronizedList(new ArrayList<>());
	
	private EventProcessingJob() {
		super(Messages.EventProcessingJob_JobName);
	}

	public void addObservation(WaypointObservation observation) {
		synchronized(observations) {
			boolean schedule = observations.isEmpty();
			observations.add(observation);
			if (schedule) schedule();
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		while(!observations.isEmpty()) {
			monitor.setTaskName(Messages.EventProcessingJob_RemainingTaskLabel + observations.size());
			WaypointObservation o = observations.remove(0);
			try(Session session = HibernateManager.openSession(new AttachmentInterceptor(false))){
				WaypointObservation temp = session.get(WaypointObservation.class, o.getUuid());
				
				Employee e = session.get(Employee.class, SmartDB.getCurrentEmployee().getUuid());
				if (e != null && !e.getConservationArea().equals(temp.getWaypoint().getConservationArea())) e = null;
				if (temp != null) o = temp;
				for(EActionEvent event : getEventActions()) {
					if (!event.isEnabled()) continue;
					try {
						
						Object data = EventProcessor.INSTANCE.processEvent(event, o, e, session);
						if (data != null) {
							ActionTypeManagerInternal.INSTANCE.getActionType(event.getAction().getActionTypeKey()).afterExecuted(data);
						}
					}catch (Exception ex) {
						EventPlugIn.displayLog(MessageFormat.format(Messages.EventProcessingJob_EventProcessingError, ex.getMessage()), ex);
					}
				}
			}catch (LockTimeoutException | org.hibernate.exception.LockTimeoutException ex2) {
				EventPlugIn.log("Lock time with event processing - trying again in 5 seconds", null);
				observations.add(0, o);
				schedule(5000);
				return Status.OK_STATUS;
			}
		}
		return Status.OK_STATUS;
	}
	
	/**
	 * Clears the trigger cache
	 */
	public synchronized void reset(){
		cachedEvents = null;
	}
	
	private synchronized List<EActionEvent> getEventActions(){
		if (cachedEvents != null) return cachedEvents;
		
		cachedEvents = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			cachedEvents.addAll( QueryFactory.buildQuery(session, EActionEvent.class, 
					new Object[] {"action.conservationArea", SmartDB.getCurrentConservationArea()}).list() ); //$NON-NLS-1$
			cachedEvents.forEach(evt->{
				evt.getAction().getParameters().forEach(p->p.getId().getParameterKey());
				evt.getFilter().getFilterString();
			});
		}
		return cachedEvents;
	}

}
