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
package org.wcs.smart.cybertracker.handler;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.cybertracker.importer.CyberTrackerImportEditor;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;
import org.wcs.smart.patrol.SmartPatrolPlugIn;

/**
 * Handler for importing data from CyberTracker application.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerImportHandler {

	@Execute
	public void execute(EModelService mService, EPartService pService){
		(new ShowFieldDataPerspective()).execute("org.wcs.smart.patrol.ui.PatrolListView", mService, pService); //$NON-NLS-1$
		
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().openEditor(new CTImportEditorInput(), CyberTrackerImportEditor.ID);						
		} catch (Throwable t) {
			SmartPatrolPlugIn.displayLog(t.getLocalizedMessage(), t);
		}
	}

	private class CTImportEditorInput implements IEditorInput {
		@SuppressWarnings("rawtypes")
		@Override
		public Object getAdapter(Class adapter) {
			return null;
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		@Override
		public String getName() {
			return Messages.CyberTrackerImportDialog_Title;
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return Messages.CyberTrackerImportDialog_Message;
		}
		
		@Override
		public int hashCode() {
			return 0;
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof CTImportEditorInput;
		}
	}
	
	public static class CyberTrackerImportHandlerWrapper extends DIHandler<CyberTrackerImportHandler>{
		public CyberTrackerImportHandlerWrapper(){
			super(CyberTrackerImportHandler.class);
		}
	}
}
