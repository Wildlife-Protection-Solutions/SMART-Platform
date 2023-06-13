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
package org.wcs.smart.qa.er.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.ui.mision.editor.WaypointAttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.er.ErTrackDataProvider;
import org.wcs.smart.qa.er.internal.Messages;
import org.wcs.smart.qa.model.IQaAction;
import org.wcs.smart.qa.model.QaError;

/**
 * Delete patrol waypoint action.  Applicable for
 * PatrolWaypointDataProvider.
 * 
 * @author Emily
 *
 */
public class DeleteErTrackAction implements IQaAction {

	@Override
	public boolean doAction(List<QaError> items) {
		List<QaError> toProcess = new ArrayList<>();
		for (QaError e : items){
			if (e.getDataProviderId().equals(ErTrackDataProvider.ID)){
				toProcess.add(e);
			}
		}
		if (toProcess.isEmpty()) return false;
		if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), Messages.DeleteErTrackAction_DeleteTitle, MessageFormat.format(Messages.DeleteErTrackAction_DeleteMsg, toProcess.size()))){
			return false;
		}
		
		Set<Mission> modified = new HashSet<>();
		List<QaError> deleted = new ArrayList<>();
		List<MissionTrack> trackDeleted = new ArrayList<>();
		try(Session s = HibernateManager.openSession(new WaypointAttachmentInterceptor())){
			try{
				s.beginTransaction();
				
				for (QaError item : toProcess){
					boolean found = false;
					for (MissionTrack track : trackDeleted){
						if (track.getUuid().equals(item.getSourceId())){
							//previously deleted
							deleted.add(item);
							found = true;
						}
					}
					if (found) continue;
					
					MissionTrack t = (MissionTrack)s.get(MissionTrack.class, item.getSourceId());
					
					if (t == null){
						item.setStatus(QaError.Status.DELETED);
						item.setFixMessage(Messages.DeleteErTrackAction_DeleteErrorNotFound);
					}else{
						deleted.add(item);
						trackDeleted.add(t);
						modified.add(t.getMissionDay().getMission());
						//lazy load for events
						t.getMissionDay().getMission().equals(null);
						
						t.getMissionDay().getTracks().remove(t);
						t.setMissionDay(null);
						s.remove(t);
					}
				}
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				QaPlugIn.displayLog(Messages.DeleteErTrackAction_DeleteError + "\n\n", ex); //$NON-NLS-1$
				return false;
			}
		}

		for (QaError item : deleted){
			item.setFixMessage(Messages.DeleteErTrackAction_DeleteMessage);
			item.setStatus(QaError.Status.DELETED);
		}
		//fire patrol events
		for (Mission m : modified){
			SurveyEventHandler.getInstance().fireEvent(SurveyEventHandler.EventType.MISSION_MODIFIED, m);
		}			
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
		return Messages.DeleteErTrackAction_ActionName;
	}

}
