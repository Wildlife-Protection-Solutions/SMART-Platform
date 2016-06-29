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
package org.wcs.smart.patrol.ui;

import java.util.UUID;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.views.PatrolListView;

/**
 * Simple POJO handler for opening patrol editor.
 * @author Emily
 *
 */
public class OpenPatrolHandler {

	public static final String PATROL_PARAM = "patrolinput"; //$NON-NLS-1$
	public static final String INIT_SELECTION_WP_UUID = "waypointuuid"; //$NON-NLS-1$
	
	@Execute
	public void openPatrol(@Optional @Named(PATROL_PARAM) PatrolEditorInput patrolInput,
			@Optional @Named(INIT_SELECTION_WP_UUID) UUID initSelection,
			MWindow activeWindow){
		
		if (patrolInput == null) return;
		
		(new ShowFieldDataPerspective()).execute(PatrolListView.ID,activeWindow);
		
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			IEditorPart part = page.openEditor(patrolInput, PatrolEditor.ID);
			if (part instanceof PatrolEditor && initSelection != null){
				((PatrolEditor)part).findAndShow(initSelection);
			}
		} catch (PartInitException e) {
			SmartPatrolPlugIn.displayLog(Messages.OpenPatrolHandler_OpenPatrolError + e.getLocalizedMessage(), e);
		}
	}
	
	public void openPatrol(@Optional @Named(PATROL_PARAM) PatrolEditorInput patrolInput,
			MWindow activeWindow){
		openPatrol(patrolInput, null, activeWindow);
	}
}
