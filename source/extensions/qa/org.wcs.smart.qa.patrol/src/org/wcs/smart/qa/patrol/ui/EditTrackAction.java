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

import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.PatrolTrackPointDialog;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaError.Status;
import org.wcs.smart.qa.routine.IQaAction;

import com.vividsolutions.jts.geom.LineString;

/**
 * Action to open edit track dialog.
 * 
 * @author Emily
 *
 */
public class EditTrackAction  implements IQaAction {

	@Override
	public void doAction(List<QaError> items) {
		if (items.isEmpty()) return;
		QaError item = items.get(0);
		
		Track track = null;
		Patrol p = null;
		Session s = HibernateManager.openSession();
		try{
			track = (Track) s.get(Track.class, item.getSourceId());
			if (track != null){
				//load hibernate objects necessary for editing
				p = track.getPatrolLegDay().getPatrolLeg().getPatrol();
				p.equals(null);
				track.getPatrolLegDay().equals(null);
				track.getPatrolLegDay().getTracks().size();
				int x = track.getGeom().length;
			}
		}finally{
			s.close();
		}
		
		if (track == null){
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Not found", "Track not found");
			return ;
		}
		
		LineString ls = null;
		try{
			ls = track.getLineString();
		}catch (Exception ex){
			QaPlugIn.log(ex.getMessage(), ex);
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Not found", "Unable to parse track linestring.  Track should be regenerated or re-imported in the patrol editor.");
			return ;
		}

		PatrolTrackPointDialog dialog = new PatrolTrackPointDialog(Display.getDefault().getActiveShell(), track, true);
		dialog.open();
		if (!ls.equalsExact(dialog.getEditTrackLineString())){
			item.setStatus(Status.FIXED);
			item.setFixMessage("Track manually modified.");
			item.setGeometryObject(dialog.getEditTrackLineString());			
			PatrolEventManager.getInstance().patrolSaved(p, true);
		}
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
		return "Edit Track...";
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON);
	}

}
