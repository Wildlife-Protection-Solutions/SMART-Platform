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
package org.wcs.smart.patrol.ui;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;

/**
 * Job for updating patrol waypoints in the database.  This job
 * will fire waypointModified event after the waypoint is saved to
 * the database.
 * 
 * @author Emily
 *
 */
public class UpdateWaypointJob extends Job {
	
	private Collection<PatrolWaypoint> waypoints;
	private Consumer<Waypoint> updateFunction;
	private Consumer<PatrolWaypoint> postSave;
	
	/**
	 * 
	 * @param wp patrol waypoint to update
	 * 
	 * @param updateFunction this function is called against the waypoint
	 * in the database and the waypoint provided
	 * 
	 * @param postSave after the waypoint is updated and saved to the database this function 
	 * is called
	 */
	public UpdateWaypointJob(PatrolWaypoint wp, Consumer<Waypoint> updateFunction,
			Consumer<PatrolWaypoint> postSave) {
		super(Messages.PatrolEditor_SaveWaypoints_JobName);
		
		waypoints = Collections.singletonList(wp);
		this.updateFunction = updateFunction;
		this.postSave = postSave;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {

		try (Session saveSession = HibernateManager.openSession(new WaypointAttachmentInterceptor())){
			saveSession.beginTransaction();
			try{
				for (PatrolWaypoint wp : waypoints) {					
					Waypoint toUpdate = saveSession.get(Waypoint.class, wp.getWaypoint().getUuid());
					updateFunction.accept(wp.getWaypoint());
					updateFunction.accept(toUpdate);
				}
				saveSession.getTransaction().commit();
			
				for (PatrolWaypoint wp : waypoints) {					
					Waypoint toUpdate = saveSession.get(Waypoint.class, wp.getWaypoint().getUuid());
					wp.getWaypoint().setLastModified(toUpdate.getLastModified());
					wp.getWaypoint().setLastModifiedBy(toUpdate.getLastModifiedBy());
				}
			} catch (Exception ex) {
				if (saveSession.getTransaction().isActive()) {
					saveSession.getTransaction().rollback();
				}
				SmartPatrolPlugIn
						.displayLog(
								Messages.PatrolEditor_Error_SavingWaypoints
										+ ex.getLocalizedMessage(), ex);
			}
		}
		for (PatrolWaypoint wp : waypoints){
			postSave.accept(wp);
		}
		
		for (PatrolWaypoint wp : waypoints){
			try{
				PatrolEventManager.getInstance().waypointModified(wp);
			}catch (Exception ex){
				SmartPatrolPlugIn.log("Error firing event after waypoint save.", ex); //$NON-NLS-1$
			}
		}
		return Status.OK_STATUS;
	}

}
