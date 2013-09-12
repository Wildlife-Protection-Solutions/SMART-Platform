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
package org.wcs.smart.plan.ui.handlers;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.report.PlanReportPerspective;
import org.wcs.smart.plan.report.ReportPlan;
/**
 * Handler to revert changes to plan template.
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public class RevertPlanTemplate extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		boolean revert = MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), Messages.RevertPlanTemplate_ConfirmDialogTitle, Messages.RevertPlanTemplate_OverwriteTemplateWarningMessage);
		if (revert){
			File customTemplate = ReportPlan.getCustomPlanTemplateLocation();
			if (customTemplate != null){
				
				boolean open = ReportPlan.closeTemplateEditor();

				//delete
				if (!ReportPlan.getCustomPlanTemplateLocation().delete()){
					MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.RevertPlanTemplate_Error, Messages.RevertPlanTemplate_ErrorRevertingTemplate);
				}
				
				//re-open
				if (open && HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getPerspective().getId().equals(PlanReportPerspective.ID) ){
					ReportPlan.editTemplate(event);
				}
			}
		}
		return null;
	}

}
