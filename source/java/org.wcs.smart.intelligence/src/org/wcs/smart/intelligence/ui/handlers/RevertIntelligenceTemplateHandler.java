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

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.report.IntelligenceReportPerspective;
import org.wcs.smart.intelligence.report.ReportIntelligence;

/**
 * Handler for reverting an intelligence template
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RevertIntelligenceTemplateHandler {

	@Execute
	public void execute(Shell activeShell,  EModelService modelService, MWindow activeWindow){
		boolean revert = MessageDialog.openConfirm(activeShell, Messages.RevertIntelligenceTemplateHandler_ConfirmDialog_Title, Messages.RevertIntelligenceTemplateHandler_ConfirmDialog_Message);
		if (!revert) return;
		
		File customTemplate = ReportIntelligence.getCustomTemplateLocation();
		if (customTemplate == null) return;

		boolean open = ReportIntelligence.closeTemplateEditor();

		//delete
		if (!ReportIntelligence.getCustomTemplateLocation().delete()){
			MessageDialog.openError(activeShell, Messages.RevertIntelligenceTemplateHandler_Error, Messages.RevertIntelligenceTemplateHandler_CannotRevert_Error);
		}
				
		//re-open
		if (open && 
				modelService.getActivePerspective(activeWindow).getElementId().equals(IntelligenceReportPerspective.ID)){
			ReportIntelligence.editTemplate();
		}
	}
	
	public static class RevertIntelligenceTemplateHandlerWrapper extends DIHandler<RevertIntelligenceTemplateHandler>{
		public RevertIntelligenceTemplateHandlerWrapper(){
			super(RevertIntelligenceTemplateHandler.class);
		}
	}

}
