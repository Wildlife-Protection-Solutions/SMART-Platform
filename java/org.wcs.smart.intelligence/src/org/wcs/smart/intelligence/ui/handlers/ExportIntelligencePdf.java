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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.intelligence.report.ReportIntelligence;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditorInput;

/**
 * Handler for exporting intelligence to pdf files.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ExportIntelligencePdf extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IntelligenceEditorInput in = null;
		if (event.getCommand().getId().equals("org.wcs.smart.intelligence.exportPdfEditor")){ //$NON-NLS-1$
			IEditorInput ein = HandlerUtil.getActiveEditor(event).getEditorInput();
			if (ein instanceof IntelligenceEditorInput){
				in = (IntelligenceEditorInput) ein;
			}
		}else{
			final IStructuredSelection lastSelection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
			if (lastSelection != null && lastSelection.getFirstElement() instanceof IntelligenceEditorInput){
				in = (IntelligenceEditorInput) lastSelection.getFirstElement();	
			}			
		}
		if (in != null){
			ReportIntelligence.export(in.getUuid());
		}
		return null;
	}
	
}
