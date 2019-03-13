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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.PatrolTrackEditDialog;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.IQaAction;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaError.Status;
import org.wcs.smart.qa.patrol.internal.Messages;

/**
 * Action to open edit track dialog.
 * 
 * @author Emily
 *
 */
public class EditTrackAction  implements IQaAction {

	@Override
	public boolean doAction(List<QaError> items) {
		if (items.isEmpty()) return false;
		QaError item = items.get(0);
		
		Track track = null;
		Patrol p = null;
		try(Session s = HibernateManager.openSession()){
			track = (Track) s.get(Track.class, item.getSourceId());
			if (track != null){
				//load hibernate objects necessary for editing
				p = track.getPatrolLegDay().getPatrolLeg().getPatrol();
				p.equals(null);
				track.getPatrolLegDay().equals(null);
				track.getPatrolLegDay().getTracks().size();
				track.getGeom().equals(null);
				
				//we need to load all tracks for all days as the editor
				//displays all tracks
				track.getPatrolLegDay().getPatrolLeg().getPatrol()
					.getLegs().forEach(e->e.getPatrolLegDays().forEach(d->d.getTracks().size()));
			}
		}
		
		if (track == null){
			item.setStatus(Status.ERROR);
			item.setFixMessage(Messages.EditTrackAction_TrackNotFoundMsg);
			MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.EditTrackAction_NotFoundDialogTitle, Messages.EditTrackAction_NotFoundDialogMsg);
			return true;
		}

		List<LineString> lsList = Collections.emptyList();
		try{
			lsList = track.getLineStrings();
		}catch (Exception ex){
			QaPlugIn.log(ex.getMessage(), ex);
			MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.EditTrackAction_NotFoundDialogTitle, Messages.EditTrackAction_ParseError);
			return false;
		}

		PatrolTrackEditDialog dialog = new PatrolTrackEditDialog(Display.getDefault().getActiveShell(), track.getPatrolLegDay(), true);
		dialog.open();

		try{
			Track editTrack = track.getPatrolLegDay().getTrack();
			List<LineString> editList = editTrack.getLineStrings();
			boolean isSame = editList != null && lsList.size() == editList.size();
			if (isSame) {
				for (int i = 0; i < lsList.size(); i++) {
					if (!lsList.get(i).equals(editList.get(i))) {
						isSame = false;
						break;
					}
				}
			}

			if (!isSame){
				item.setStatus(Status.FIXED);
				item.setFixMessage(Messages.EditTrackAction_ModifiedMsg);
				item.setGeometryObject(editTrack.getGeometry());			
				PatrolEventManager.getInstance().patrolSaved(p, true);
				return true;
			}
		}catch (Exception ex){
			QaPlugIn.log(ex.getMessage(), ex);
			MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.EditTrackAction_NotFoundDialogTitle, Messages.EditTrackAction_ParseError);
			return false;
		}
		return false;
	}

	@Override
	public boolean supportsMultiple() {
		return false;
	}

	@Override
	public String getId() {
		return "org.wcs.smart.qa.patrol.track.edit"; //$NON-NLS-1$
	}

	@Override
	public String getName(Locale l) {
		return Messages.EditTrackAction_ActionName;
	}

}
