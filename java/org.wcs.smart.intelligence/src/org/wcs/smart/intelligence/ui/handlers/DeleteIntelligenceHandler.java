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
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;

/**
/**
 * Handler for handling "Delete Intelligence" command
 *
 * @author elitvin
 * @since 1.0.0
 */
public class DeleteIntelligenceHandler extends AbstractHandler {

	public DeleteIntelligenceHandler() {}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		final IStructuredSelection lastSelection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		
		if (lastSelection == null) {
			return null;
		}
		
		for (Iterator<?> iterator = lastSelection.iterator(); iterator.hasNext();) {
			Object selected = iterator.next();
			if (selected instanceof Intelligence) {
				final Intelligence intelligence = (Intelligence) selected;
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
								Messages.DeleteIntelligenceHandler_ConfirmationDialog_Title,
								null,
								MessageFormat.format(Messages.DeleteIntelligenceHandler_ConfirmationDialog_Message, new Object[]{intelligence.getShortName()}),
								MessageDialog.CONFIRM, 
								new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 1);
						
						if (dialog.open() == MessageDialog.OK) {
							DeleteIntelligenceJob deleteJob = new DeleteIntelligenceJob(intelligence);
							deleteJob.schedule();
						}
					}});
			}
		}
		return null;
	}

	/**
     * Job is used to delete intelligence object
     * 
     * @author elitvin
     *
     */
    private class DeleteIntelligenceJob extends Job {
    	
    	private Intelligence intelligence;
  
        public DeleteIntelligenceJob(Intelligence intelligence) {
            super(Messages.NewIntelligenceWizard_SaveIntelligenceJob_Title);
            this.intelligence = intelligence;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (IntelligenceHibernateManager.deleteIntelligence(DeleteIntelligenceJob.this.intelligence)) {
            	//NOTE: you have to be careful with this call
            	IntelligenceEventManager.getInstance().intelligenceDeleted(DeleteIntelligenceJob.this.intelligence);
            	return Status.OK_STATUS;
            }
            //no need to use other status as hibernate manager will report error in case something is wrong
            return Status.CANCEL_STATUS;
        }
    }
	
}
