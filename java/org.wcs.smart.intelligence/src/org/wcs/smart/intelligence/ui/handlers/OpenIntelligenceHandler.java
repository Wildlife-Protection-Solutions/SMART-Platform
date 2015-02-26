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
package org.wcs.smart.intelligence.ui.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.ui.IntelligencePerspective;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditor;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditorInput;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.ui.ShowPerspectiveHandler;

/**
 * Simple POJO handler for opening patrol editor.
 * @author Emily
 *
 */
public class OpenIntelligenceHandler {

	public static final String INTELLUUID_PARAM = "intelligenceuuid"; //$NON-NLS-1$
	@Execute
	public void openIntelligence(@Optional @Named(INTELLUUID_PARAM) byte[] intellUuid,
			MWindow activeWindow){
		if (intellUuid == null) return;
		
		(new ShowPerspectiveHandler()).execute(IntelligencePerspective.ID, activeWindow);
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			page.openEditor(new IntelligenceEditorInput(intellUuid, null, null), IntelligenceEditor.ID);
		} catch (PartInitException e) {
			SmartPatrolPlugIn.displayLog(Messages.OpenIntelligenceHandler_PlanEditor + e.getLocalizedMessage(), e);
		}
	}
}
