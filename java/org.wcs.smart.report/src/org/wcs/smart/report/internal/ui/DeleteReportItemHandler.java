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

import java.text.MessageFormat;
import java.util.Collections;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
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
public class DeleteReportItemHandler {

	
	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection, Shell activeShell){
		if (thisSelection == null || !(thisSelection instanceof IStructuredSelection) || ((IStructuredSelection)thisSelection).isEmpty() ){
			return;
		}
		final List<Object> selection= ((IStructuredSelection)thisSelection).toList();
		Collections.reverse(selection);
		
		if (selection.size() == 1 && (selection.get(0) instanceof Report) ){
			Report r = ((Report)selection.get(0)) ;
			if (!MessageDialog.openConfirm(activeShell, 
					Messages.DeleteReportItemHandler_Confirm_DialogTitle, 
					MessageFormat.format(Messages.DeleteReportItemHandler_Confirm_DialogMessage,
					new Object[]{"'"+ r.getName() + " [" + r.getId()  + "]'"})  )){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return;
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
			String message = Messages.DeleteReportItemHandler_Confirm_MultiDialogMessageA;
			if (reportCnt > 0){
				message +=  Messages.DeleteReportItemHandler_Confirm_MultiDialogMessageB;
			}
			if (folderCnt > 0){
				message += Messages.DeleteReportItemHandler_Confirm_MultiDialogMessageC;
			}
			message += "?"; //$NON-NLS-1$
			message = MessageFormat.format(message, new Object[]{reportCnt, folderCnt});
			if (reportCnt > 0){
				if (!MessageDialog.openConfirm(activeShell, Messages.DeleteReportItemHandler_Confirm_DialogTitleB, message )){
					return;
				}	
			}
		}
		
		Job job = new Job(Messages.DeleteReportItemHandler_DeleteJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
					Object o = (Object) iterator.next();
					
					String name = ""; //$NON-NLS-1$
					try {
						if (o instanceof ReportFolder) {
							ReportFolder parent = (ReportFolder) o;
							name = MessageFormat.format(Messages.DeleteReportItemHandler_deletefolder_label, new Object[]{ parent.getName()});
							ReportManager.deleteReportFolder(parent);
							ReportEventManager.getInstance()
								.fireReportFolderDeleted(parent);
						} else if (o instanceof Report) {
							name = MessageFormat.format(Messages.DeleteReportItemHandler_deletereport_label, new Object[]{ ((Report)o).getName()});
							ReportManager.deleteReport((Report) o);
							ReportEventManager.getInstance().fireReportDeleted(
								(Report) o);
						}
					} catch (Exception ex) {
						ReportPlugIn.displayLog(
								MessageFormat.format(Messages.DeleteReportItemHandler_Delete_Error, new Object[]{name}) + ex.getLocalizedMessage(), ex);
					}
				}

				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
	
	public static class DeleteReportItemHandlerWrapper extends DIHandler<DeleteReportItemHandler>{
		public DeleteReportItemHandlerWrapper(){
			super(DeleteReportItemHandler.class);
		}
	}
}