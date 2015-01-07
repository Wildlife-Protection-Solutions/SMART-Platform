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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.intelligence.informant.editor.InformantDataEditor;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.ui.IntelligencePerspective;
import org.wcs.smart.patrol.SmartPatrolPlugIn;

/**
 * Handler to open Informant Data editor
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class ShowInformantDataHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		//Open Intelligence Perspective
		try {
			HandlerUtil.getActiveWorkbenchWindow(event).getWorkbench()
					.showPerspective(IntelligencePerspective.ID, HandlerUtil.getActiveWorkbenchWindow(event));
		} catch (WorkbenchException e) {
			SmartPatrolPlugIn.displayLog(Messages.IntelligenceHandler_LoadPerspective_Error, e);
		}
		
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().openEditor(new InformantEditorInput(), InformantDataEditor.ID);						
		} catch (Throwable t) {
			SmartPatrolPlugIn.displayLog(t.getLocalizedMessage(), t);
		}
		return null;
	}

	private class InformantEditorInput implements IEditorInput {
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
			return "Name";
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return "Tooltip";
		}
		
		@Override
		public int hashCode() {
			return 0;
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof InformantEditorInput;
		}
	}
	
}
