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
package org.wcs.smart.incident.ui;

import java.util.UUID;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;

/**
 * Edit incident handler
 * @author Emily
 *
 */
public class OpenIncidentHandler {

	public static final String UUID_PARAM = "incidentUuid"; //$NON-NLS-1$
	@Execute
	public void openIncident(@Optional @Named(UUID_PARAM) UUID incidentUuid,
			MWindow activeWindow){
		if (incidentUuid == null) return;
		
		//get the context here as this is not pure e4
		(new ShowFieldDataPerspective()).execute(IndIncidentListView.ID, activeWindow);
		
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			page.openEditor(new IncidentEditorInput(incidentUuid), IncidentEditor.ID);
		} catch (PartInitException e) {
			IncidentPlugIn.displayLog(Messages.OpenIncidentHandler_IncidentOpenError + e.getLocalizedMessage(), e);
		}
	}
}
