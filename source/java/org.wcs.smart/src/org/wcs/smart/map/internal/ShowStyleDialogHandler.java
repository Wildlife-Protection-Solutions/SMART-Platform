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
package org.wcs.smart.map.internal;

import java.util.Iterator;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.style.sld.editor.EditorPageManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.udig.style.StyleEditorPageManager;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.ui.map.SmartStyleEditorDialog;

/**
 * Show style dialog handler.
 * 
 * @author Emily
 *
 */
public class ShowStyleDialogHandler {

	@Execute
	public void execute(Shell activeShell, ESelectionService selectionService) {
		Object currentSelection = selectionService.getSelection();
		if (currentSelection == null){
			SmartPlugIn.logInfo("Style selection is null"); //$NON-NLS-1$
			return;
		}
		if (!(currentSelection instanceof IStructuredSelection)){
			SmartPlugIn.logInfo("Style selection is not structured selection"); //$NON-NLS-1$
			return;
		}
		if (((IStructuredSelection) currentSelection).isEmpty()){
			SmartPlugIn.logInfo("Style selection is empty"); //$NON-NLS-1$
			return;
		}
		
		Layer selectedLayer = null;
		for (Iterator<?> iterator = ((IStructuredSelection)currentSelection).iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof Layer){
				selectedLayer = (Layer)type;
				break;
			}
			
		}
		if (selectedLayer == null){
			SmartPlugIn.logInfo("No layer found in current selection."); //$NON-NLS-1$
			return;	
		}

		String pageId = StyleManager.INSTANCE.findInitialStylePageId(selectedLayer);
	    EditorPageManager manager = StyleEditorPageManager.createEditorPageManager(selectedLayer);

		SmartStyleEditorDialog dialog = new SmartStyleEditorDialog(activeShell,manager);
		dialog.setSelectedNode(pageId);
		dialog.setSelectedLayer(selectedLayer);
		dialog.create();
		dialog.open();
	}

	public static class ShowStyleDialogHandlerWrapper extends
			DIHandler<ShowStyleDialogHandler> {
		public ShowStyleDialogHandlerWrapper() {
			super(ShowStyleDialogHandler.class);
		}
	}
}
