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
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.patrol.routine.PatrolTrackDataProvider;
import org.wcs.smart.qa.routine.IQaAction;

/**
 * Delete patrol waypoint action.  Applicable for
 * PatrolWaypointDataProvider.
 * 
 * @author Emily
 *
 */
public class DeletePatrolTrackAction implements IQaAction {

	@Override
	public void doAction(List<QaError> items) {
		List<QaError> toProcess = new ArrayList<>();
		for (QaError e : items){
			if (e.getDataProviderId().equals(PatrolTrackDataProvider.ID)){
				toProcess.add(e);
			}
		}
		if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Delete", MessageFormat.format("Are you sure you want to delete the {0} selected tracks?  This action cannot be undone.", toProcess.size()))){
			return;
		}
		
		Set<Patrol> modified = new HashSet<>();
		List<QaError> deleted = new ArrayList<>();
		List<Track> trackDeleted = new ArrayList<>();
		Session s = HibernateManager.openSession(new WaypointAttachmentInterceptor());
		try{
			s.beginTransaction();
			
			for (QaError item : toProcess){
				boolean found = false;
				for (Track track : trackDeleted){
					if (track.getUuid().equals(item.getSourceId())){
						//previously deleted
						deleted.add(item);
						found = true;
					}
				}
				if (found) continue;
				
				Track t = (Track)s.get(Track.class, item.getSourceId());
				
				if (t == null){
					item.setStatus(QaError.Status.DELETED);
					item.setFixMessage("***Could not delete - Track Not Found*** ");
				}else{
					deleted.add(item);
					trackDeleted.add(t);
					modified.add(t.getPatrolLegDay().getPatrolLeg().getPatrol());
					t.getPatrolLegDay().getPatrolLeg().getPatrol().equals(null);
					
					t.getPatrolLegDay().setTrack(null);
					t.setPatrolLegDay(null);
					s.delete(t);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			QaPlugIn.displayLog("An error occurred while removing the selected patrol tracks.  Refresh QA list and try again, or edit try deleting individual patrol tracks." + "\n\n", ex);
			return;
		}finally{
			s.close();
		}

		for (QaError item : deleted){
			item.setFixMessage("Track Deleted");
			item.setStatus(QaError.Status.DELETED);
		}
		//fire patrol events
		for (Patrol d : modified){
			PatrolEventManager.getInstance().patrolSaved(d,true);
		}			
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
