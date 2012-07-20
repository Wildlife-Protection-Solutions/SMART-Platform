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
package org.wcs.smart.report.internal.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;

/**
 * Handler for deleting a report or a report folder.
 * <p>Handler attempts to delete the first 
 * item in the current selection.</p>
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DeleteReportItemHandler extends AbstractHandler implements IHandler {

	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection thisSelection = HandlerUtil.getCurrentSelection(event);
		if (thisSelection == null || thisSelection.isEmpty() || !(thisSelection instanceof IStructuredSelection) ){
			return null;
		}
		
		Object obj = ((IStructuredSelection)thisSelection).getFirstElement();
		final Object o = obj;
		
		if (o instanceof RootReportFolder){
			return null;
		}
		
		if (o instanceof Report) {
			if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Delete Report", "Are you sure you want to delete the report " + ((Report)o).getName() + ".  This action cannot be undone.")){
				return null; 
			}
			
		}
		Job job = new Job("Delete Item Job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (o instanceof ReportFolder) {
						ReportFolder parent = (ReportFolder) o;
						ReportManager.deleteReportFolder(parent);
						ReportEventManager.getInstance()
								.fireReportFolderDeleted(parent);
					} else if (o instanceof Report) {
						ReportManager.deleteReport((Report) o);
						ReportEventManager.getInstance().fireReportDeleted(
								(Report) o);
					}
				} catch (Exception ex) {
					ReportPlugIn.displayLog(
							"Error deleting item : " + ex.getMessage(), ex);
				}

				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}
}