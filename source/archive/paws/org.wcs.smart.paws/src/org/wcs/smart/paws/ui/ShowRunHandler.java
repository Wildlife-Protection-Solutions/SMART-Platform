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
package org.wcs.smart.paws.ui;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.ui.run.RunEditor;
import org.wcs.smart.paws.ui.run.RunEditorInput;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.ui.ShowPerspectiveHandler;

/**
 * Handler opening PAWS Analysis results
 * 
 * @author Emily
 *
 */
public class ShowRunHandler {
	
	@Execute
	public void execute(MWindow window, PawsRun pawsRun ) {
		(new ShowPerspectiveHandler()).execute(QueryPlugIn.getActivePerspectiveId(), window);
		
		try {
			IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = ww.getActivePage();
			RunEditorInput rinput = new RunEditorInput(pawsRun);
			page.openEditor(rinput, RunEditor.ID);
		} catch (PartInitException e) {
			PawsPlugIn.displayLog(Messages.ShowRunHandler_LoadError + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
		}
	}
}
