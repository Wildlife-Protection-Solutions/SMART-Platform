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
package org.wcs.smart.er.query.model;

import java.text.MessageFormat;
import java.util.Date;
import java.util.UUID;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.er.ui.SurveyDesignListView;
import org.wcs.smart.er.ui.handlers.EditSurveyElementHandler;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;
import org.wcs.smart.query.model.IQueryResultInfoProvider;

/**
 * Intel info provider than opens up the intelligence record associated with the
 * result item.
 * 
 * @author Emily
 *
 */
public class SurveyResultInfoProvider implements IQueryResultInfoProvider {

	@Override
	public String getName() {
		return GOTO_SOURCE_STR;
	}

	@Override
	public void doWork(Object resultItem) {
		UUID missionUuid = null;
		String missionId = null;
		UUID waypointUuid = null;
		Date date = null;
		
		if (resultItem instanceof MissionTrackResultItem) {
			missionUuid = ((MissionTrackResultItem)resultItem).getMissionUuid();
			missionId = ((MissionTrackResultItem)resultItem).getMissionId();
			date = ((MissionTrackResultItem)resultItem).getTrackDate();
		}
		
		if (resultItem instanceof SurveyQueryResultItem){
			missionUuid = ((SurveyQueryResultItem)resultItem).getMissionUuid();
			missionId= ((SurveyQueryResultItem)resultItem).getMissionId();
			waypointUuid = ((SurveyQueryResultItem)resultItem).getWaypointUuid();
		}
		
		if (missionUuid != null){
			MWindow mwindow = ((IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class)).getActiveLeaf().get(MWindow.class);
			
			(new ShowFieldDataPerspective()).execute(SurveyDesignListView.ID,mwindow);
			if (waypointUuid != null){
				EditSurveyElementHandler.editMission(Display.getDefault().getActiveShell(),
					missionUuid, missionId, waypointUuid);
			}else if (date != null){
				EditSurveyElementHandler.editMission(Display.getDefault().getActiveShell(),
						missionUuid, missionId, date);
			}else{
				EditSurveyElementHandler.editMission(Display.getDefault().getActiveShell(),
						missionUuid, missionId);
			}
			return;
		}
		
		MessageDialog.openError(
					Display.getDefault().getActiveShell(),
					ERROR_STR,
					MessageFormat.format(OP_NOT_SUPPORTED_STR, resultItem.getClass().getName()));

	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON);
	}

}
