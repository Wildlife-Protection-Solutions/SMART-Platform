/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.patrol.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.patrol.routine.PatrolWaypointDataProvider;
import org.wcs.smart.qa.routine.IQaAction;

/**
 * Delete patrol waypoint action.  Applicable for
 * PatrolWaypointDataProvider.
 * 
 * @author Emily
 *
 */
public class DeletePatrolWaypointAction implements IQaAction {

	@Override
	public boolean doAction(List<QaError> items) {
		List<QaError> toProcess = new ArrayList<>();
		for (QaError e : items){
			if (e.getDataProviderId().equals(PatrolWaypointDataProvider.ID)){
				toProcess.add(e);
			}
		}
		if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Delete", MessageFormat.format("Are you sure you want to delete the {0} selected waypoints?  This action cannot be undone.", toProcess.size()))){
			return false;
		}
		
		Set<Patrol> modified = new HashSet<>();
		List<QaError> deleted = new ArrayList<>();
		List<Waypoint> wpDeleted = new ArrayList<>();
		Session s = HibernateManager.openSession(new WaypointAttachmentInterceptor());
		try{
			s.beginTransaction();
			
			for (QaError item : toProcess){
				boolean found = false;
				for (Waypoint wp : wpDeleted){
					if (wp.getUuid().equals(item.getSourceId())){
						//previously deleted
						deleted.add(item);
						found = true;
					}
				}
				if (found) continue;
				
				PatrolWaypoint pw = (PatrolWaypoint) s.createCriteria(PatrolWaypoint.class)
						.add(Restrictions.eq("id.waypoint.uuid", item.getSourceId())) //$NON-NLS-1$
						.uniqueResult();
				
				if (pw == null){
					item.setStatus(QaError.Status.DELETED);
					item.setFixMessage("***Could not delete - Waypoint Not Found*** " + (item.getFixMessage() == null ? "" : " - " + item.getFixMessage()));
				}else{
					s.delete(pw);
					s.delete(pw.getWaypoint());
					modified.add(pw.getPatrolLegDay().getPatrolLeg().getPatrol());
					pw.getPatrolLegDay().getPatrolLeg().getPatrol().equals(null);
					deleted.add(item);
					wpDeleted.add(pw.getWaypoint());
				}
			}
			s.getTransaction().commit();
			

		}catch (Exception ex){
			s.getTransaction().rollback();
			QaPlugIn.displayLog("An error occurred while removing the selected patrol waypoints.  Refresh QA list and try again, or edit try deleting individual patrol waypoints." + "\n\n", ex);
			return false;
		}finally{
			s.close();
		}

		for (QaError item : deleted){
			item.setFixMessage("Waypoint Deleted");
			item.setStatus(QaError.Status.DELETED);
		}
		//fire patrol events
		for (Patrol d : modified){
			PatrolEventManager.getInstance().patrolSaved(d,true);
		}
		//fire waypoint events
		wpDeleted.forEach(w->WaypointEventManager.getInstance().waypointDeleted(w));
		return true;
	}

	@Override
	public boolean supportsMultiple() {
		return true;
	}

	@Override
	public String getId() {
		return DELETE_ACTION_ID;
	}

	@Override
	public String getName(Locale l) {
		return "Delete";
	}
	
	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON);
	}
}
