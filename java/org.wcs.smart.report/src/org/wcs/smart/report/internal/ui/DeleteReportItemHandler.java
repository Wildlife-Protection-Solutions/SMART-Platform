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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;

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
		
		
		final List<Object> selection= ((IStructuredSelection)thisSelection).toList();
		Collections.reverse(selection);
		
		
		if (selection.size() == 1 && (selection.get(0) instanceof Report) ){
			Report r = ((Report)selection.get(0)) ;
			if (!MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "Confirm Delete", "Are you sure you want to delete the report '" + r.getName() + " [" + r.getId()  + "]'?"  )){
				return null;
			}
		}else{
			int reportCnt = 0;
			int folderCnt = 0;
			for (int i = 0; i < selection.size(); i ++){
				if (selection.get(i) instanceof Report){
					reportCnt++;
				}else if (selection.get(i) instanceof ReportFolder){
					folderCnt++;
				}
			}
			String message = "Are you sure you want to delete the ";
			if (reportCnt > 0){
				message += reportCnt + " selected reports";
			}
			if (folderCnt > 0){
				message += " and the " + folderCnt + " selected folders";
			}
			message += "?";
			if (reportCnt > 0){
				if (!MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "Confirm Delete", message )){
					return null;
				}	
			}
		}
		
		Job job = new Job("Delete Items Job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
					Object o = (Object) iterator.next();
					
					String name = "";
					try {
						if (o instanceof ReportFolder) {
							ReportFolder parent = (ReportFolder) o;
							name = "folder '" + parent.getName() + "'";
							ReportManager.deleteReportFolder(parent);
							ReportEventManager.getInstance()
								.fireReportFolderDeleted(parent);
						} else if (o instanceof Report) {
							name = " report '" + ((Report) o).getName() + "'";
							ReportManager.deleteReport((Report) o);
							ReportEventManager.getInstance().fireReportDeleted(
								(Report) o);
						}
					} catch (Exception ex) {
						ReportPlugIn.displayLog(
								"Error deleting " + name + ".\n\n" + ex.getMessage(), ex);
					}
				}

				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}
}