/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.incident.patrol;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.PatrolLegDay;

import jakarta.persistence.LockTimeoutException;

public class IncidentToPatrolProcessorJob extends Job{

	private static IncidentToPatrolProcessorJob instance = new IncidentToPatrolProcessorJob();
	
	public static IncidentToPatrolProcessorJob getInstance() {
		return instance;
	}
	
	private IncidentToPatrolProcessorJob() {
		super("linking incidents to patrols"); //$NON-NLS-1$
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		try {
			IncidentToPatrolProcessor processor = new IncidentToPatrolProcessor(SmartDB.getCurrentConservationArea(), false);
			
			try(Session session = HibernateManager.openSession()){
				processor.doWork(session);	
			
				if (!processor.getUpdatedPatrols().isEmpty() || !processor.getUpdatedWaypoints().isEmpty()) {
					Display.getDefault().syncExec(()->{
						
						for (PatrolLegDay p : processor.getUpdatedPatrols()) {
							PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_WAYPOINTS, p);
							PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_TRACKS, p);
						}
						for (Waypoint wp : processor.getUpdatedWaypoints()) {
							IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_DELETED, wp);
							WaypointEventManager.getInstance().waypointModified(wp);
						}
					});
				}
			}catch(LockTimeoutException timeout) {
				//Ticket: 3773 
				//some lock is prevent this tasks from finishing
				//log a warning and reschedule task
				//I can reproduce this when I load two smart mobile patrols at the same time
				//for the first one, select create a new patrol, for the second one just wait
				//eventually this error will come up
				IncidentPlugIn.log("Timeout linking incidents to patrol. Trying again in 30 seconds", null);
				schedule(30*1000);
			}
				
		}catch (Exception ex) {
			IncidentPlugIn.displayLog(Messages.IncidentToPatrolProcessorJob_LinkError + ex.getMessage(), ex);
		}

		return Status.OK_STATUS;
	}

}
