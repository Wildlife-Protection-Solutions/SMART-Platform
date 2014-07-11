package org.wcs.smart.patrol.internal.ui.views;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.ui.PatrolEditorInput;

public class DeletePatrolHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		final IStructuredSelection lastSelection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		if (lastSelection.size() == 0){
			return null;	//nothing to delete
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
			return null;
		}
		
		MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
				Messages.DeletePatrolHandler_ConfirmDeletePatrol_DialogTitle,
				null,
				message,
				MessageDialog.CONFIRM, 
				new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 1);
				
		if (dialog.open() != MessageDialog.OK){
			return null;
		}
			
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
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
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(Display
									.getCurrent().getActiveShell(),
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
		
		return null;
	}

}
