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
package org.wcs.smart.entity.ui;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.ccca.EntityTypeCcaaPerspective;
import org.wcs.smart.entity.ui.editor.EntityTypeEditor;
import org.wcs.smart.entity.ui.editor.EntityTypeEditorInput;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;
import org.wcs.smart.ui.ShowPerspectiveHandler;

/**
 * Open entity type handler
 * @author Emily
 *
 */
public class OpenEntityTypeHandler {

	@Execute
	public void openEntityType(@Optional EntityTypeEditorInput entityType){
		if (entityType == null) return;
		
		//get the context here as this is not pure e4
		if (!SmartDB.isMultipleAnalysis()){
			IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			ContextInjectionFactory.invoke(new ShowFieldDataPerspective(), Execute.class, context);
		}else{
			IEclipseContext context = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			IEclipseContext local = EclipseContextFactory.create();
			local.set(ShowPerspectiveHandler.PERSPECTIVE_ID_PARAM, EntityTypeCcaaPerspective.ID);
			local.setParent(context.get(EPartService.class).getActivePart().getContext());
			ContextInjectionFactory.invoke(new ShowPerspectiveHandler(), Execute.class, local);
		}
				
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			page.openEditor(entityType, EntityTypeEditor.ID);
		} catch (PartInitException t) {
			EntityPlugIn.displayLog(t.getLocalizedMessage(), t);
		}
	}
}
