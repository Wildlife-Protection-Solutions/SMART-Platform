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
package org.wcs.smart.qa.er;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.handlers.EditSurveyElementHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.routine.IQaAction;

/**
 * Opens the source patrol editor.  Works for
 * results from PatrolWaypointDataProvider or PatrolTrackDataProvider
 * 
 * @author Emily
 *
 */
public class OpenSourceMissionAction implements IQaAction {

	@Inject
	private IEclipseContext context;
	
	public OpenSourceMissionAction(){
	}

	@Override
	public void doAction(List<QaError> items) {
		if (items.isEmpty()) return;
		QaError item = items.get(0);
		if (item.getDataProviderId().equals(ErWaypointDataProvider.ID)){
			SurveyWaypoint pw = null;
			Session s = HibernateManager.openSession();
			try{
				pw = (SurveyWaypoint) s.createCriteria(SurveyWaypoint.class)
				.add(Restrictions.eq("id.waypoint.uuid", item.getSourceId())) //$NON-NLS-1$
				.uniqueResult();
				if (pw != null){
					Mission m = pw.getMissionDay().getMission();
					m.getId();
					m.getUuid();
					m.getStartDate();
					m.getEndDate();
				}
			}finally{
				s.close();
			}
			if (pw == null){
				//not found
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Not Found", MessageFormat.format("Patrol waypoint {0} not found", item.getErrorId()));
				return;
			}else{
				EditSurveyElementHandler.editMission(context.get(Shell.class), pw.getMissionDay().getMission().getUuid(), pw.getMissionDay().getMission().getId(), pw.getWaypoint().getUuid());
					
			}
		}else if (item.getDataProviderId().equals(ErTrackDataProvider.ID)){
			MissionTrack track = null;
			Session s = HibernateManager.openSession();
			try{
				track = (MissionTrack) s.get(MissionTrack.class, item.getSourceId());
				if (track != null){
					Mission m = track.getMissionDay().getMission();
					m.getId();
					m.getUuid();
					m.getStartDate();
					m.getEndDate();
				}
			}finally{
				s.close();
			}
			if (track == null){
				//not found
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Not Found", MessageFormat.format("Patrol track {0} not found", item.getErrorId()));
				return;
			}else{
				EditSurveyElementHandler.editMission(context.get(Shell.class),track.getMissionDay().getMission().getUuid(), track.getMissionDay().getMission().getId(), track.getMissionDay().getDate());	
			}
		}
	}

	@Override
	public boolean supportsMultiple() {
		return false;
	}

	@Override
	public String getId() {
		return "org.wcs.smart.qa.patrol.goto"; //$NON-NLS-1$
	}

	@Override
	public String getName(Locale l) {
		return "Goto Source";
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON);
	}
}
