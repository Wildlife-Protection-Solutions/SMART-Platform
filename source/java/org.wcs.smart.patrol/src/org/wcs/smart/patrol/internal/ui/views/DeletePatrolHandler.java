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
package org.wcs.smart.patrol.internal.ui.views;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.ui.PatrolEditorInput;

/**
 * Delete patrol handler
 * @author Emily
 *
 */
public class DeletePatrolHandler {
	
	@Execute
	public void execute(ESelectionService selectionService, final Shell activeShell){
		
		Object selobj = selectionService.getSelection();
		if (!(selobj instanceof IStructuredSelection)) return;
		
		final IStructuredSelection lastSelection = (IStructuredSelection) selobj;
		if (lastSelection.size() == 0){
			return;	//nothing to delete
		}
		
		final List<PatrolEditorInput> toDelete = new ArrayList<PatrolEditorInput>();
		for (Iterator<?> iterator = lastSelection.iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof PatrolEditorInput){
				toDelete.add((PatrolEditorInput)x);
			}
		}
		
		String message = null;
		if (toDelete.size() == 1){
			message = MessageFormat.format(
					Messages.DeletePatrolHandler_ConfirmDeletePatrol_DialogMessage, new Object[]{toDelete.get(0).getPatrolId()});
		}else if (toDelete.size() > 1){
			StringBuilder sb = new StringBuilder();
			for (PatrolEditorInput in : toDelete){
				sb.append(in.getPatrolId());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			if (sb.length() > 1000){
				sb.delete(1000, sb.length()-1);
				sb.append(" ..."); //$NON-NLS-1$
			}
			message = MessageFormat.format(Messages.DeletePatrolHandler_MultiDeleteConfirmation + "\n\n" + sb.toString(), new Object[]{toDelete.size()}); //$NON-NLS-1$
		}else{
			return;
		}
		
		MessageDialog dialog = new MessageDialog(activeShell,
				Messages.DeletePatrolHandler_ConfirmDeletePatrol_DialogTitle,
				null,
				message,
				MessageDialog.CONFIRM, 
				new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 1);
				
		if (dialog.open() != MessageDialog.OK){
			return;
		}
			
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException,
						InterruptedException {
					int deleted = 0;
					monitor.beginTask(Messages.DeletePatrolHandler_ProgressTaskName, toDelete.size());
					for (PatrolEditorInput delete : toDelete){
						try {
							if (PatrolManager.getInstance().deletePatrol(delete.getUuid(), new SubProgressMonitor(monitor, 1))){
								deleted++;
							}
						}catch (Exception ex){
							SmartPatrolPlugIn.displayLog(
									MessageFormat.format(Messages.DeletePatrolHandler_DeletePatrol_ErrorMessage, new Object[]{delete.getPatrolId()}) + "\n\n" + ex.getLocalizedMessage(),ex); //$NON-NLS-1$

						}
					}
					String message = null;
					if (deleted == toDelete.size()){
						message = MessageFormat.format(Messages.DeletePatrolHandler_DeleteSuccessMessage, new Object[]{deleted});;
					}else{
						message = MessageFormat.format(Messages.DeletePatrolHandler_DeleteErrorMessage, new Object[]{deleted, toDelete.size()});
					}
					final String dMessage = message;
					activeShell.getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(activeShell,
									Messages.DeletePatrolHandler_DeletePatrol_DialogTitle,
									dMessage);
						}});
					monitor.done();
				}
			});
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(
					Messages.DeletePatrolHandler_Error_CouldNotDeletePatrol,
					ex);
		}
		return;
	}

	//E3
	public static class DeletePatrolHandlerWrapper extends DIHandler<DeletePatrolHandler>{
		public DeletePatrolHandlerWrapper(){
			super(DeletePatrolHandler.class);
		}
	}
}
