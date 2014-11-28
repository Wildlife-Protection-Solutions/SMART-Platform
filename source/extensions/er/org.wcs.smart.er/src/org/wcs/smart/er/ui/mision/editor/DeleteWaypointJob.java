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
package org.wcs.smart.er.ui.mision.editor;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.events.WaypointEventManager;

/**
 * Job fo saving a set of waypoints to the database.
 * 
 * @author Emily
 *
 */
public class DeleteWaypointJob extends Job {
	private Collection<SurveyWaypoint> waypoints;

	public DeleteWaypointJob() {
		super(Messages.DeleteWaypointJob_Title);
	}

	public void setWaypoints(Collection<SurveyWaypoint> points) {
		synchronized (this) {
			this.waypoints = points;
		}

	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		ArrayList<SurveyWaypoint> pnts = new ArrayList<SurveyWaypoint>();
		synchronized (this) {
			pnts.addAll(waypoints);
		}
		
		Session session = HibernateManager.openSession(new WaypointAttachmentInterceptor());
		try{
			session.beginTransaction();
			for (SurveyWaypoint wp : pnts) {
				session.delete(wp);
				session.delete(wp.getWaypoint());
			}
			session.getTransaction().commit();
		} catch (Exception ex) {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			EcologicalRecordsPlugIn.displayLog(Messages.DeleteWaypointJob_Error + ex.getLocalizedMessage(), ex);
		}finally{
			session.close();
		}
		for (SurveyWaypoint wp : waypoints){
			try{
				WaypointEventManager.getInstance().waypointDeleted(wp.getWaypoint());
			}catch (Exception ex){
				EcologicalRecordsPlugIn.log("Error firing event after waypoint delete.", ex); //$NON-NLS-1$
			}
		}
		
		
		return Status.OK_STATUS;
	}

}
