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

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.OpenPatrolHandler;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.patrol.routine.PatrolTrackDataProvider;
import org.wcs.smart.qa.patrol.routine.PatrolWaypointDataProvider;
import org.wcs.smart.qa.routine.IQaAction;

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
	public void doAction(List<QaError> items) {
		if (items.isEmpty()) return;
		QaError item = items.get(0);
		if (item.getDataProviderId().equals(PatrolWaypointDataProvider.ID)){
			PatrolWaypoint pw = null;
			Session s = HibernateManager.openSession();
			try{
				pw = (PatrolWaypoint) s.createCriteria(PatrolWaypoint.class)
				.add(Restrictions.eq("id.waypoint.uuid", item.getSourceId())) //$NON-NLS-1$
				.uniqueResult();
				if (pw != null){
					Patrol p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
					p.getId();
					p.getUuid();
					p.getStartDate();
					p.getEndDate();
				}
			}finally{
				s.close();
			}
			if (pw == null){
				//not found
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Not Found", MessageFormat.format("Patrol waypoint {0} not found", item.getErrorId()));
				return;
			}else{
				(new OpenPatrolHandler()).openPatrol(new PatrolEditorInput(pw.getPatrolLegDay().getPatrolLeg().getPatrol()), pw.getWaypoint().getUuid(), context.get(MWindow.class));	
			}
		}else if (item.getDataProviderId().equals(PatrolTrackDataProvider.ID)){
			Track track = null;
			Session s = HibernateManager.openSession();
			try{
				track = (Track) s.get(Track.class, item.getSourceId());
				if (track != null){
					Patrol p = track.getPatrolLegDay().getPatrolLeg().getPatrol();
					p.getId();
					p.getUuid();
					p.getStartDate();
					p.getEndDate();
				}
			}finally{
				s.close();
			}
			if (track == null){
				//not found
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Not Found", MessageFormat.format("Patrol track {0} not found", item.getErrorId()));
				return;
			}else{
				(new OpenPatrolHandler()).openPatrol(new PatrolEditorInput(track.getPatrolLegDay().getPatrolLeg().getPatrol()), track.getPatrolLegDay().getUuid(), context.get(MWindow.class));	
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
