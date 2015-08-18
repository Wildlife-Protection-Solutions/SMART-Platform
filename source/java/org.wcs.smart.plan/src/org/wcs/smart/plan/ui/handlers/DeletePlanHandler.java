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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.internal.Messages;
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
public class DeletePlanHandler{ 

	@Execute
	public void execute(Shell activeShell, @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection) {

		if (thisSelection == null || !(thisSelection instanceof IStructuredSelection )) {
			return;
		}
		List<UUID> toDelete = new ArrayList<UUID>();
		
		for (Iterator<?> iterator = ((IStructuredSelection)thisSelection).iterator(); iterator.hasNext();) {
			Object selected = iterator.next();
			UUID planUuid = null;
			if (selected instanceof Plan) {
				planUuid = ((Plan) selected).getUuid();
			}else if (selected instanceof PlanEditorInput){
				planUuid = ((PlanEditorInput)selected).getUuid();
			}
			if (planUuid != null){
				toDelete.add(planUuid);
			}
		}
		if (toDelete.size() == 0) return;
		
		MessageDialog dialog = new MessageDialog(activeShell,
				Messages.DeletePlanHandler_Confirmation_Message,
				null,
				MessageFormat.format(Messages.DeletePlanHandler_Confirmation_Warning, new Object[]{toDelete.size()}),
				MessageDialog.CONFIRM, 
				new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 1);
						
		if (dialog.open() == MessageDialog.OK) {
			DeletePlanJob deleteJob = new DeletePlanJob(toDelete);
			deleteJob.schedule();
		}
	}

	/**
     * Job is used to delete plan object
     * 
     * @author elitvin
     * @author jeffloun
     *
     */
    private class DeletePlanJob extends Job {
    	
    	private List<UUID> uuids;
  
        public DeletePlanJob(List<UUID> uuids) {
            super(Messages.DeletePlanHandler_DeleteJob_Title);
            this.uuids = uuids;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
        	while(uuids.size() > 0){
        		UUID uuid = uuids.remove(0);
        		Set<Plan> deletedPlans = PlanHibernateManager.deletePlan(uuid);
        		if (deletedPlans != null) {
        			for (Plan deletedPlan : deletedPlans){
        				PlanEventManager.getInstance().planDeleted(deletedPlan);
        			}
        		}	
        	}
            return Status.OK_STATUS;
        }
    }

    public static class DeletePlanHandlerWrapper extends DIHandler<DeletePlanHandler>{
    	public DeletePlanHandlerWrapper(){
    		super(DeletePlanHandler.class);
    	}
    }
}
