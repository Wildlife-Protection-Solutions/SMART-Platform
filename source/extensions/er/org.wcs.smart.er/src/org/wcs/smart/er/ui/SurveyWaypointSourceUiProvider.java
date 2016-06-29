/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.er.ui;

import java.util.UUID;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.handlers.EditSurveyElementHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.IWaypointSourceUiProvider;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;

/**
 * Source provider for survey waypoints.
 * 
 * @author Emily
 *
 */
public class SurveyWaypointSourceUiProvider implements
		IWaypointSourceUiProvider {

	@Override
	public void findAndShow(UUID waypointUuid) {
		SurveyWaypoint pw = null;
		Session s = HibernateManager.openSession();
		try{
			pw = (SurveyWaypoint)s.createCriteria(SurveyWaypoint.class)
					.add(Restrictions.eq("id.waypoint.uuid", waypointUuid)) //$NON-NLS-1$
					.uniqueResult();
			if (pw == null){
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						ERROR_STR, 
						Messages.SurveyWaypointSourceUiProvider_WaypointNotFound);
				return;
			}
			pw.getMissionDay().getMission().getId();
			pw.getMissionDay().getMission().getUuid();
		}finally{
			s.close();
		}
		
		IEclipseContext ctx = ((IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class)).getActiveLeaf();
		(new ShowFieldDataPerspective()).execute(SurveyDesignListView.ID,ctx.get(MWindow.class));
		EditSurveyElementHandler.editMission(ctx.get(Shell.class), pw.getMissionDay().getMission().getUuid(), pw.getMissionDay().getMission().getId(), pw.getWaypoint().getUuid());
	}

}
