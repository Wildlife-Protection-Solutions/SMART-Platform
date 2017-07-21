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

import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.ui.mision.editor.MissionTrackEditDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.er.internal.Messages;
import org.wcs.smart.qa.model.IQaAction;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaError.Status;

/**
 * Edit mission track action.
 * 
 * @author Emily
 *
 */
public class EditTrackAction  implements IQaAction {

	@Override
	public boolean doAction(List<QaError> items) {
		if (items.isEmpty()) return false;
		QaError item = items.get(0);
		
		MissionTrack track = null;
		Mission p = null;
		Session s = HibernateManager.openSession();
		try{
			track = (MissionTrack) s.get(MissionTrack.class, item.getSourceId());
			if (track != null){
				//load hibernate objects necessary for editing
				p = track.getMissionDay().getMission();
				p.equals(null);
				track.getMissionDay().equals(null);
				track.getMissionDay().getTracks().size();
				track.getGeom().equals(null);
			}
		}finally{
			s.close();
		}
		
		if (track == null){
			item.setStatus(Status.ERROR);
			item.setFixMessage(Messages.EditTrackAction_TrackNotFoundError);
			MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.EditTrackAction_NotFoundTitle, Messages.EditTrackAction_TrackNotFoundError);
			return true;
		}
		
		try{
			track.getLineString();
		}catch (Exception ex){
			QaPlugIn.log(ex.getMessage(), ex);
			MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.EditTrackAction_NotFoundTitle, Messages.EditTrackAction_ParseError);
			return false;
		}

		boolean[] changes = new boolean[]{false};
		MissionTrackEditDialog dialog = new MissionTrackEditDialog(Display.getDefault().getActiveShell(),track.getMissionDay()){
			public boolean saveChanges() {
				if (super.saveChanges()){
					changes[0] = true;
					return true;
				}
				return false;
			}
		};
		dialog.open();
		if (changes[0]){
			item.setStatus(Status.FIXED);
			item.setFixMessage(Messages.EditTrackAction_FixMessage);
			//you can edit multiple tracks so we cannot do this 
			//item.setGeometryObject(dialog.getEditTrackLineString());
			return true;
		}
		return false;
	}

	@Override
	public boolean supportsMultiple() {
		return false;
	}

	@Override
	public String getId() {
		return "org.wcs.smart.qa.mission.track.edit"; //$NON-NLS-1$
	}

	@Override
	public String getName(Locale l) {
		return Messages.EditTrackAction_ActionName;
	}

}
