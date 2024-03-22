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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;

/**
 * Job fo saving a set of waypoints to the database.
 * 
 * @author Emily
 *
 */
public class SaveWaypointJob extends Job {
	private volatile Collection<SurveyWaypoint> waypoints;

	public SaveWaypointJob() {
		super(Messages.SaveWaypointJob_Title);
	}

	public void setWaypoints(Collection<SurveyWaypoint> points) {
		synchronized (this) {
			this.waypoints = points;
		}

	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//if (true) return Status.OK_STATUS;
		ArrayList<SurveyWaypoint> pnts = new ArrayList<SurveyWaypoint>();
		synchronized (this) {
			pnts.addAll(waypoints);
		}
		ArrayList<Waypoint> updated = new ArrayList<>();
		try(Session saveSession = HibernateManager.openSession(new WaypointAttachmentInterceptor())){
			
			try {
				saveSession.beginTransaction();
				for (SurveyWaypoint wp : pnts) {
					if (wp.getWaypoint().getAttachments() != null) wp.getWaypoint().setAttachments(new ArrayList<>(wp.getWaypoint().getAttachments()));
					
					Waypoint pnt = wp.getWaypoint();
					
					pnt.setSourceId(SurveyWaypointSource.KEY);
					pnt.setConservationArea(SmartDB.getCurrentConservationArea());
					
					//merge here messed up attachments ("copy from location" doesn't get merged) 
					//so need to save attachments before merge
					pnt.saveNewAttachments(saveSession);
					saveSession.flush();
					
					
					if (pnt.getUuid() == null) {
						saveSession.persist(pnt);
						saveSession.persist(wp);
					}else {
						pnt = saveSession.merge(pnt);
						
						//required to prevent duplicate loading of survey waypoint object
						saveSession.get(SurveyWaypoint.class, wp.getId()).getMissionDay().getUuid();
						wp = saveSession.merge(wp);
						//saveSession.merge(wp);
					}
					updated.add(pnt);
					
								
					// remove observations with no data
					for (WaypointObservation wo : pnt.getAllObservations()) {
						List<WaypointObservationAttribute> toDelete = new ArrayList<WaypointObservationAttribute>();
						for (WaypointObservationAttribute att : wo.getAttributes()) {
							if (!att.hasValue()) {
								toDelete.add(att);
							}
						}
						wo.getAttributes().removeAll(toDelete);
					}
					//remove groups with no data
					List<WaypointObservationGroup> gdelete = new ArrayList<>();
					if (pnt.getObservationGroups() == null) pnt.setObservationGroups(new ArrayList<>());
					for (WaypointObservationGroup g : pnt.getObservationGroups()) {
						if (g.getObservations() == null || g.getObservations().isEmpty()) gdelete.add(g);
					}
					pnt.getObservationGroups().removeAll(gdelete);
					

					
				}
				saveSession.getTransaction().commit();
				
				for (SurveyWaypoint wp : pnts) {
					Waypoint saved = saveSession.get(Waypoint.class, wp.getWaypoint().getUuid());
					wp.getWaypoint().setLastModified(saved.getLastModified());
					wp.getWaypoint().setLastModifiedBy(saved.getLastModifiedBy());
					Hibernate.initialize(saved.getLastModifiedBy());
				}					
				
				
			} catch (Exception ex) {
				if (saveSession.getTransaction().isActive()) {
					saveSession.getTransaction().rollback();
				}
				EcologicalRecordsPlugIn.displayLog(Messages.SaveWaypointJob_Error + ex.getLocalizedMessage(), ex);
			}
		}
		for (Waypoint wp : updated) {
			try{
				WaypointEventManager.getInstance().waypointModified(wp);
			}catch (Exception ex){
				EcologicalRecordsPlugIn.log("Error firing event after waypoint save.", ex); //$NON-NLS-1$
			}
		}
		return Status.OK_STATUS;
	}

}
