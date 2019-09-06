/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.wcomm.ui;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;
import org.wcs.smart.patrol.SmartPatrolPlugIn;

/**
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class ImportDataHandler {
	
	@Execute
	public void openPatrol(MWindow activeWindow){
		(new ShowFieldDataPerspective()).execute("org.wcs.smart.patrol.ui.PatrolListView", activeWindow); //$NON-NLS-1$
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			page.openEditor(DataImportEditorInput.INSTANCE, WCommImportEditor.ID);	
		} catch (PartInitException e) {
			SmartPatrolPlugIn.displayLog(e.getLocalizedMessage(), e);
		}
	}
	
	
	//E3
	public static class ImportDataHandlerWrapper extends DIHandler<ImportDataHandler>{
		public ImportDataHandlerWrapper(){
			super(ImportDataHandler.class);
		}
	}
}
