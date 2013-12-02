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

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.report.IntelligenceReportPerspective;
import org.wcs.smart.intelligence.report.ReportIntelligence;

/**
 * Handler for reverting an intelligence template
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RevertIntelligenceTemplateHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		boolean revert = MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), Messages.RevertIntelligenceTemplateHandler_ConfirmDialog_Title, Messages.RevertIntelligenceTemplateHandler_ConfirmDialog_Message);
		if (revert){
			File customTemplate = ReportIntelligence.getCustomTemplateLocation();
			if (customTemplate != null){
				
				boolean open = ReportIntelligence.closeTemplateEditor();

				//delete
				if (!ReportIntelligence.getCustomTemplateLocation().delete()){
					MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.RevertIntelligenceTemplateHandler_Error, Messages.RevertIntelligenceTemplateHandler_CannotRevert_Error);
				}
				
				//re-open
				if (open && HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getPerspective().getId().equals(IntelligenceReportPerspective.ID) ){
					ReportIntelligence.editTemplate(event);
				}
			}
		}
		return null;
	}

}
