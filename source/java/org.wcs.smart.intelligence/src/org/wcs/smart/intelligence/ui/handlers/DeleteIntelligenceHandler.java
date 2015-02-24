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
import java.util.List;

import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditorInput;

/**
/**
 * Handler for handling "Delete Intelligence" command
 *
 * @author elitvin
 * @since 1.0.0
 */
public class DeleteIntelligenceHandler {

	@Execute
	public void execute (@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object lastSelection, final Shell activeShell){

		if (lastSelection == null || !(lastSelection instanceof IStructuredSelection)) {
			return;
		}
		
		for (Iterator<?> iterator = ((IStructuredSelection)lastSelection).iterator(); iterator.hasNext();) {
			Object selected = iterator.next();
			if (selected instanceof IntelligenceEditorInput) {
				final IntelligenceEditorInput editorInput = (IntelligenceEditorInput) selected;
				activeShell.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						MessageDialog dialog = new MessageDialog(activeShell,
								Messages.DeleteIntelligenceHandler_ConfirmationDialog_Title,
								null,
								MessageFormat.format(Messages.DeleteIntelligenceHandler_ConfirmationDialog_Message, editorInput.getName()),
								MessageDialog.CONFIRM, 
								new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 1);
						
						if (dialog.open() == MessageDialog.OK && checkIsPatrolSource(editorInput)) {
							DeleteIntelligenceJob deleteJob = new DeleteIntelligenceJob(editorInput.getUuid());
							deleteJob.schedule();
						}
					}

					private boolean checkIsPatrolSource(IntelligenceEditorInput input) {
						List<?> items = IntelligenceHibernateManager.fetchRelatedPatrolIDs(input.getUuid());
						if (items== null || items.isEmpty()) {
							return true;
						}
						MessageDialog dialog = new MessageDialog(activeShell,
								Messages.DeleteIntelligenceHandler_ConfirmationDialog_Title,
								null,
								MessageFormat.format(Messages.DeleteIntelligenceHandler_ConfirmationDialog_PatrolChanges_Message, items.toString(), editorInput.getName()),
								MessageDialog.CONFIRM, 
								new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 1);
						
						return dialog.open() == MessageDialog.OK;
					}
					
				});
			}
		}
		return;
	}

	/**
     * Job is used to delete intelligence object
     * 
     * @author elitvin
     *
     */
    private class DeleteIntelligenceJob extends Job {
    	
    	private byte[] uuid;
  
        public DeleteIntelligenceJob(byte[] uuid) {
            super(Messages.NewIntelligenceWizard_SaveIntelligenceJob_Title);
            this.uuid = uuid;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            Intelligence deletedIntelligence = IntelligenceHibernateManager.deleteIntelligence(uuid);
			if (deletedIntelligence != null) {
            	//NOTE: you have to be careful with this call
            	IntelligenceEventManager.getInstance().intelligenceDeleted(deletedIntelligence);
            	return Status.OK_STATUS;
            }
            //no need to use other status as hibernate manager will report error in case something is wrong
            return Status.CANCEL_STATUS;
        }
    }
	
    public static class DeleteIntelligenceHandlerWrapper extends DIHandler<DeleteIntelligenceHandler>{
    	public DeleteIntelligenceHandlerWrapper(){
    		super(DeleteIntelligenceHandler.class);
    	}
    }
}
