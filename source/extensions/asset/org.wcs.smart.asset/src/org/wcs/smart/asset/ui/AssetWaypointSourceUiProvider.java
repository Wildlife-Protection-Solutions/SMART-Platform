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
package org.wcs.smart.asset.ui;

import java.util.UUID;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.IWaypointSourceUiProvider;

/**
 * Source provider for survey waypoints.
 * 
 * @author Emily
 *
 */
public class AssetWaypointSourceUiProvider implements
		IWaypointSourceUiProvider {

	@Override
	public void findAndShow(UUID waypointUuid) {
		AssetWaypoint pw = null;
		
		try(Session s = HibernateManager.openSession()){
			pw = QueryFactory.buildQuery(s, AssetWaypoint.class, "id.waypoint.uuid", waypointUuid).uniqueResult(); //$NON-NLS-1$
			if (pw == null){
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						ERROR_STR, 
						"Asset waypoint waypoint not found.");
				return;
			}
		}
		
		//TODO:
//		IEclipseContext ctx = ((IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class)).getActiveLeaf();
//		(new ShowFieldDataPerspective()).execute(SurveyDesignListView.ID,ctx.get(MWindow.class));
//		EditSurveyElementHandler.editMission(ctx.get(Shell.class), pw.getMissionDay().getMission().getUuid(), pw.getMissionDay().getMission().getId(), pw.getWaypoint().getUuid());
	}

}
