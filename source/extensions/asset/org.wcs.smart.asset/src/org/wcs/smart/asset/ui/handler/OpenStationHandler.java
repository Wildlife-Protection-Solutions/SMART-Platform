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
package org.wcs.smart.asset.ui.handler;

import java.text.MessageFormat;
import java.util.UUID;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.ui.views.asset.AssetListView;
import org.wcs.smart.asset.ui.views.station.StationEditor;
import org.wcs.smart.asset.ui.views.station.StationEditorInput;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;

/**
 * Open asset handler 
 * 
 * @author Emily
 *
 */
public class OpenStationHandler {	
	
	public static final String STATION_PARAM = "stationinput"; //$NON-NLS-1$
	public static final String INIT_SELECTION_WP_UUID = "waypointuuid"; //$NON-NLS-1$
	

	
	@Execute
	public void openStation(@Named(STATION_PARAM) StationEditorInput input,
			@Optional @Named(INIT_SELECTION_WP_UUID) UUID waypointUuid,
			MWindow activeWindow){
		(new ShowFieldDataPerspective()).execute(AssetListView.ID, activeWindow);
		
		
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();				
			IEditorPart part = page.openEditor(input, StationEditor.ID);	
			if (part instanceof StationEditor && waypointUuid != null){
				((StationEditor)part).findAndShow(waypointUuid);
			}
			
		} catch (PartInitException e) {
			AssetPlugIn.displayLog(MessageFormat.format("Error opening station editor: {0}", e.getMessage()), e);
		}
	}
	
}

