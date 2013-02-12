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

import java.text.MessageFormat;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;

/**
/**
 * Handler for handling "Delete Plan" command
 *
 * @author elitvin
 * @author jeffloun
 * @since 1.0.0
 */
public class DeletePlanHandler extends AbstractHandler {

	public DeletePlanHandler() {}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		final IStructuredSelection lastSelection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		
		if (lastSelection == null) {
			return null;
		}
		
		for (Iterator<?> iterator = lastSelection.iterator(); iterator.hasNext();) {
			Object selected = iterator.next();
			byte[] planUuid = null;
			String name = null;
			if (selected instanceof Plan) {
				planUuid = ((Plan) selected).getUuid();
				name = ((Plan)selected).getLabel();
			}else if (selected instanceof PlanEditorInput){
				planUuid = ((PlanEditorInput)selected).getUuid();
				name = ((PlanEditorInput) selected).getName();
			}
			if (planUuid != null){
				final byte[] uuid = planUuid;
				final String thisname = name;
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
								"Are you sure you wish to delete this Plan?",
								null,
								MessageFormat.format("WARNING: All sub-plans(plans with this plan as their parent, and their children etc) will also be deleted if you continue. Are you sure you wish to delete this plan {0}? ", new Object[]{thisname}),
								MessageDialog.CONFIRM, 
								new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 1);
						
						if (dialog.open() == MessageDialog.OK) {
							DeletePlanJob deleteJob = new DeletePlanJob(uuid);
							deleteJob.schedule();
						}
					}});
			}
		}
		return null;
	}

	/**
     * Job is used to delete plan object
     * 
     * @author elitvin
     * @author jeffloun
     *
     */
    private class DeletePlanJob extends Job {
    	
    	private byte[] uuid;
  
        public DeletePlanJob(byte[] uuid) {
            super("Delete Plan");
            this.uuid = uuid;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            Plan deletedPlan = PlanHibernateManager.deletePlan(uuid);
			if (deletedPlan != null) {
            	//NOTE: you have to be careful with this call
            	PlanEventManager.getInstance().PlanDeleted(deletedPlan);
            	return Status.OK_STATUS;
            }
            //no need to use other status as hibernate manager will report error in case something is wrong
            return Status.CANCEL_STATUS;
        }
    }
	
}
