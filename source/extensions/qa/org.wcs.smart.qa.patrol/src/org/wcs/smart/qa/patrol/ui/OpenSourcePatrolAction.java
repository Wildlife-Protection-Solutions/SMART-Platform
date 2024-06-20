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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.OpenPatrolHandler;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.qa.model.IQaAction;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.patrol.internal.Messages;
import org.wcs.smart.qa.patrol.routine.PatrolDataProvider;
import org.wcs.smart.qa.patrol.routine.PatrolTrackDataProvider;
import org.wcs.smart.qa.patrol.routine.PatrolWaypointDataProvider;

/**
 * Opens the source patrol editor.  Works for
 * results from PatrolWaypointDataProvider or PatrolTrackDataProvider
 * 
 * @author Emily
 *
 */
public class OpenSourcePatrolAction implements IQaAction {

	@Inject
	private IEclipseContext context;
	
	public OpenSourcePatrolAction(){
	}

	@Override
	public boolean doAction(List<QaError> items) {
		if (items.isEmpty()) return false;
		QaError item = items.get(0);
		
		Patrol p = null;
		UUID wpUuid = null;
		UUID legDayUuid = null;
		try(Session s = HibernateManager.openSession()){
		
			if (item.getDataProviderId().equals(PatrolWaypointDataProvider.ID)){
				PatrolWaypoint pw = null;
				
				pw = QueryFactory.buildQuery(s, PatrolWaypoint.class, "id.waypoint.uuid", item.getSourceId()).uniqueResult(); //$NON-NLS-1$
				if (pw == null){
					//not found
					MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.OpenSourcePatrolAction_NotFoundDialogTitle, MessageFormat.format(Messages.OpenSourcePatrolAction_WpNotFound, item.getErrorId()));
					return false;
				}
				p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
				wpUuid = pw.getWaypoint().getUuid();
				
			}else if (item.getDataProviderId().equals(PatrolTrackDataProvider.ID)){
				Track track = s.get(Track.class, item.getSourceId());
				if (track == null){
					//not found
					MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.OpenSourcePatrolAction_NotFoundDialogTitle, MessageFormat.format(Messages.OpenSourcePatrolAction_TrackNotFound, item.getErrorId()));
					return false;
				}
				
				p = track.getPatrolLegDay().getPatrolLeg().getPatrol();
				legDayUuid = track.getPatrolLegDay().getUuid();

			}else if (item.getDataProviderId().equals(PatrolDataProvider.ID)){
				p = s.get(Patrol.class,item.getSourceId());
				
				if (p == null){
					//not found
					MessageDialog.openError(Display.getDefault().getActiveShell(),
							Messages.OpenSourcePatrolAction_NotFoundDialogTitle, 
							MessageFormat.format(Messages.OpenSourcePatrolAction_PatrolNotFound, item.getErrorId()));
					return false;
				}
			}
		
			if (p == null) {
				return false;
			}
			
			p.getId();
			p.getUuid();
			p.getStartDate();
			p.getEndDate();
		}
		
		
		PatrolEditorInput pi = new PatrolEditorInput(p);
		(new OpenPatrolHandler()).openPatrol(pi, wpUuid, legDayUuid, context.get(MWindow.class));
		
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
		return Messages.OpenSourcePatrolAction_ActionName;
	}
}
