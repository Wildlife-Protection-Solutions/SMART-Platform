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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * Job fo saving a set of waypoints to the database.
 * 
 * @author Emily
 *
 */
public class SaveWaypointJob extends Job {
	private Collection<Waypoint> waypoints;

	public SaveWaypointJob() {
		super(Messages.PatrolEditor_SaveWaypoints_JobName);
	}

	public void setWaypoints(Collection<Waypoint> points) {
		synchronized (this) {
			this.waypoints = points;
		}

	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		ArrayList<Waypoint> pnts = new ArrayList<Waypoint>();
		synchronized (this) {
			pnts.addAll(waypoints);
		}
		Session saveSession = HibernateManager
				.openSession(new WaypointAttachmentInterceptor());
		try {
			saveSession.beginTransaction();
			for (Waypoint wp : pnts) {
				saveSession.saveOrUpdate(wp);
				saveSession.flush();
				// remove observations with no data
				if (wp.getObservations() != null) {
					for (WaypointObservation wo : wp.getObservations()) {
						List<WaypointObservationAttribute> toDelete = new ArrayList<WaypointObservationAttribute>();
						for (WaypointObservationAttribute att : wo
								.getAttributes()) {
							if (!att.hasValue()) {
								toDelete.add(att);
							}
						}
						wo.getAttributes().removeAll(toDelete);
					}
				}
			}
			saveSession.getTransaction().commit();
		} catch (Exception ex) {
			if (saveSession.getTransaction().isActive()) {
				saveSession.getTransaction().rollback();
			}
			SmartPatrolPlugIn
					.displayLog(
							Messages.PatrolEditor_Error_SavingWaypoints
									+ ex.getLocalizedMessage(), ex);
		} finally {
			saveSession.close();
		}
		for (Waypoint wp : waypoints){
			try{
				PatrolEventManager.getInstance().waypointModified(wp);
			}catch (Exception ex){
				SmartPatrolPlugIn.log("Error firing event after waypoint save.", ex); //$NON-NLS-1$
			}
		}
		return Status.OK_STATUS;
	}

}
