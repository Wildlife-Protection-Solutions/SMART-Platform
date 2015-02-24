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

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.intelligence.informant.InformantEditorInput;
import org.wcs.smart.intelligence.informant.editor.InformantDataEditor;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.ui.IntelligencePerspective;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.ui.ShowPerspectiveHandler;

/**
 * Handler to open Informant Data editor
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class ShowInformantDataHandler {

	@Execute
	public void execute(EModelService mService, MWindow window, @Optional Informant toEdit){
		//Open Intelligence Perspective
		
		(new ShowPerspectiveHandler()).execute(IntelligencePerspective.ID, mService, window);
		
		try {
			InformantDataEditor editor = (InformantDataEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().openEditor(new InformantEditorInput(), InformantDataEditor.ID);
			
			if (editor != null && toEdit != null){
				editor.setSelection(toEdit);
			}
		} catch (Throwable t) {
			SmartPatrolPlugIn.displayLog(t.getLocalizedMessage(), t);
		}
	}

	
	public static class ShowInformantDataHandlerWrapper extends DIHandler<ShowInformantDataHandler>{
		public ShowInformantDataHandlerWrapper(){
			super(ShowInformantDataHandler.class);
		}
	}
}
