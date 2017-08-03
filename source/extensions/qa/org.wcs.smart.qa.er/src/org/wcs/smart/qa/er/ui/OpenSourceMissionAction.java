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
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.handlers.EditSurveyElementHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.qa.er.ErTrackDataProvider;
import org.wcs.smart.qa.er.ErWaypointDataProvider;
import org.wcs.smart.qa.er.internal.Messages;
import org.wcs.smart.qa.model.IQaAction;
import org.wcs.smart.qa.model.QaError;

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
	public boolean doAction(List<QaError> items) {
		if (items.isEmpty()) return false;
		QaError item = items.get(0);
		if (item.getDataProviderId().equals(ErWaypointDataProvider.ID)){
			SurveyWaypoint pw = null;
			
			try(Session s = HibernateManager.openSession()){
				pw = QueryFactory.buildQuery(s, SurveyWaypoint.class, "id.waypoint.uuid", item.getSourceId()).uniqueResult(); //$NON-NLS-1$
				if (pw != null){
					Mission m = pw.getMissionDay().getMission();
					m.getId();
					m.getUuid();
					m.getStartDate();
					m.getEndDate();
				}
			}
			if (pw == null){
				//not found
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.OpenSourceMissionAction_NotFoundTitle, MessageFormat.format(Messages.OpenSourceMissionAction_WpNotfoundMsg, item.getErrorId()));
				return false;
			}else{
				EditSurveyElementHandler.editMission(context.get(Shell.class), pw.getMissionDay().getMission().getUuid(), pw.getMissionDay().getMission().getId(), pw.getWaypoint().getUuid());
					
			}
		}else if (item.getDataProviderId().equals(ErTrackDataProvider.ID)){
			MissionTrack track = null;
			try(Session s = HibernateManager.openSession()){
				track = (MissionTrack) s.get(MissionTrack.class, item.getSourceId());
				if (track != null){
					Mission m = track.getMissionDay().getMission();
					m.getId();
					m.getUuid();
					m.getStartDate();
					m.getEndDate();
				}
			}
			if (track == null){
				//not found
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.OpenSourceMissionAction_NotFoundTitle, MessageFormat.format(Messages.OpenSourceMissionAction_TrackNotFoundMsg, item.getErrorId()));
				return false;
			}else{
				EditSurveyElementHandler.editMission(context.get(Shell.class),track.getMissionDay().getMission().getUuid(), track.getMissionDay().getMission().getId(), track.getMissionDay().getDate());	
			}
		}
		return false;
	}

	@Override
	public boolean supportsMultiple() {
		return false;
	}

	@Override
	public String getId() {
		return GOTO_ACTION_ID;
	}

	@Override
	public String getName(Locale l) {
		return Messages.OpenSourceMissionAction_ActionName;
	}
}
