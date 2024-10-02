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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.model.IncidentType;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;

/**
 * Open incident handler.
 * 
 * If providing the uuid then both the incidentUuid and sourceKey must 
 * be provided.  If using the selectionService selections must be of
 * type IncidentEditorInput
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class OpenIncidentHandler {

	public static final String UUID_PARAM = "incidentUuid"; //$NON-NLS-1$
	public static final String SOURCE_PARAM = "incidentSource"; //$NON-NLS-1$
	
	public void openIncident(@Optional @Named(UUID_PARAM) UUID incidentUuid, @Optional @Named(SOURCE_PARAM) String sourceKey, MWindow activeWindow){
		openIncident(incidentUuid, sourceKey, null, activeWindow);
	}
	
	@Execute
	public void openIncident(@Optional @Named(UUID_PARAM) UUID incidentUuid,
			@Optional @Named(SOURCE_PARAM) String sourceKey,
			ESelectionService selectionService, MWindow activeWindow){
		
		List<IncidentEditorInput> incidents = new ArrayList<>();
	
		if (incidentUuid == null) {
			if (selectionService == null) return;
			if (!(selectionService.getSelection() instanceof IStructuredSelection)) return;
			for (Iterator<?> iterator = ((IStructuredSelection)selectionService.getSelection()).iterator(); iterator.hasNext();) {
				Object type = (Object) iterator.next();
				if (type instanceof IncidentEditorInput) {
					incidents.add((IncidentEditorInput)type);
				}	
			}
		}else {
			
			try(Session session = HibernateManager.openSession()){
				Waypoint wp = session.get(Waypoint.class, incidentUuid);
				if (wp != null) {
					IncidentType type = null;
					if (wp.getIncidentTypeUuid() != null) {
						type = session.get(IncidentType.class, wp.getIncidentTypeUuid());
					}
					IncidentEditorInput ii = new IncidentEditorInput(wp.getUuid(), wp.getId(), wp.getDateTime(), wp.getSourceId(), type);
					incidents.add(ii);
				}
			}
		}
		
		//get the context here as this is not pure e4
		(new ShowFieldDataPerspective()).execute(IndIncidentListView.ID, activeWindow);
		
		for (IncidentEditorInput input : incidents) {
			try {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				page.openEditor(input, IncidentManager.getInstance().getIncidentProvider(input.getSourceKey()).getEditorID());
			} catch (PartInitException e) {
				IncidentPlugIn.displayLog(Messages.OpenIncidentHandler_IncidentOpenError + e.getLocalizedMessage(), e);
			}
		}
	}
	
	public static class OpenIncidentHandlerWrapper extends DIHandler<OpenIncidentHandler>{
		public OpenIncidentHandlerWrapper(){
			super(OpenIncidentHandler.class);
		}
	}
}
