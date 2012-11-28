package org.wcs.smart.patrol.internal.ui.views;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.editor.PatrolEditorInput;

public class DeletePatrolHandler extends AbstractHandler {
	
	private boolean canDelete = false;
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		final ISelection lastSelection = HandlerUtil.getCurrentSelection(event);
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException,
						InterruptedException {
					for (Iterator<?> iterator = ((IStructuredSelection) lastSelection).iterator(); iterator.hasNext();) {
						Object selected = (Object) iterator.next();
						if (selected instanceof PatrolEditorInput) {
							final PatrolEditorInput in = (PatrolEditorInput) selected;
							canDelete = false;
							
							Display.getDefault().syncExec(new Runnable(){

								@Override
								public void run() {
									if (MessageDialog
											.openConfirm(
													Display.getCurrent()
															.getActiveShell(),
													Messages.DeletePatrolHandler_ConfirmDeletePatrol_DialogTitle,
													MessageFormat.format(
													Messages.DeletePatrolHandler_ConfirmDeletePatrol_DialogMessage, new Object[]{in.getPatrolId()}))) {
										canDelete = true;
									}
									
								}});
							
							if (!canDelete){
								continue;
							}
							try {
								PatrolManager.getInstance().deletePatrol(
										in.getUuid(), monitor);
								Display.getDefault().syncExec(new Runnable(){

									@Override
									public void run() {
										MessageDialog.openInformation(Display
												.getCurrent().getActiveShell(),
												Messages.DeletePatrolHandler_DeletePatrol_DialogTitle,
												MessageFormat.format(Messages.DeletePatrolHandler_DeletePatrol_DialogMessage, new Object[]{in.getPatrolId()}));
									}});
								
							} catch (final Exception ex) {
								Display.getDefault().syncExec(new Runnable(){
									@Override
									public void run() {
										SmartPatrolPlugIn.displayLog(
												MessageFormat.format(Messages.DeletePatrolHandler_DeletePatrol_ErrorMessage, new Object[]{in.getPatrolId()}),ex);
									}});
							}
						}
					}

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
