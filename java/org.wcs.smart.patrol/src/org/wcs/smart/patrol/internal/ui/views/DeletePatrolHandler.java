package org.wcs.smart.patrol.internal.ui.views;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.ui.editor.PatrolEditorInput;

public class DeletePatrolHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
//		PatrolListView view = (PatrolListView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(PatrolListView.ID);
//		ISelection thisSelection = null;
//		if (view != null){
//			thisSelection = view.getViewSite().getSelectionProvider().getSelection();
//		}
//		if (thisSelection == null){
//			return null;
//		}
//		
		final ISelection lastSelection = HandlerUtil.getCurrentSelection(event);
		
//		final ISelection lastSelection = thisSelection;
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
		try {
			pmd.run(false, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException,
						InterruptedException {
					for (Iterator iterator = ((IStructuredSelection) lastSelection)
							.iterator(); iterator.hasNext();) {
						Object selected = (Object) iterator.next();
						if (selected instanceof PatrolEditorInput) {
							PatrolEditorInput in = (PatrolEditorInput) selected;
							if (!MessageDialog
									.openConfirm(
											Display.getCurrent()
													.getActiveShell(),
											"Delete Patrol",
											"Are you sure you want to delete patrol "
													+ in.getPatrolId()
													+ ".  This action cannot be undone.")) {
								return;
							}
							try {
								PatrolManager.getInstance().deletePatrol(
										in.getUuid(), monitor);
								MessageDialog.openInformation(Display
										.getCurrent().getActiveShell(),
										"Delete Patrol",
										"Patrol " + in.getPatrolId()
												+ " successfully deleted.");
							} catch (Exception ex) {
								SmartPatrolPlugIn.displayLog(
										"Patrol " + in.getPatrolId()
												+ " could not be deleted. ",
										ex);
							}
						}
					}

				}
			});
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(
					"Patrols could not be deleted. ",
					ex);
		}
		
		return null;
	}

}
