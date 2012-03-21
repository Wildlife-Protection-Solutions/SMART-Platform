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
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.ui.editor.PatrolEditorInput;

/**
 * Action for deleting patrols;
 * @author Emily
 * @since 1.0.0
 */
public class DeletePatrolAction implements IViewActionDelegate {

	ISelection lastSelection = null;
	
	@Override
	public void run(IAction action) {
		if (lastSelection instanceof IStructuredSelection){
			
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
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		action.setEnabled(!selection.isEmpty());
		this.lastSelection = selection;
	}

	@Override
	public void init(IViewPart view) {
	}

}
