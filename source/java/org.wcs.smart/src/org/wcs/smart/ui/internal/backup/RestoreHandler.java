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
package org.wcs.smart.ui.internal.backup;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.application.DisplayAccess;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.backup.DerbyRestoreEngine;
import org.wcs.smart.internal.Messages;

/**
 * Handler for restoring backup file.
 * @author egouge
 * @since 1.0.0
 */
public class RestoreHandler {

	/**
	 * Runs the handler
	 * @param shell the current shell
	 * 
	 */
	public boolean execute(final Shell shell) {

		if (!MessageDialog.openConfirm(shell, Messages.RestoreHandler_ConfirmRestore_DialogTitle, Messages.RestoreHandler_ConfirmRestore_DialogMessage )){
			return false;
		}
		
		if (!DerbyRestoreEngine.validateUserRestore(shell)){
			return false;
		}
		
		MessageDialog confirm = new MessageDialog(
				shell,
				Messages.RestoreHandler_ConfirmRestore_DialogTitle,null,
				Messages.RestoreHandler_ConfirmRestore_BackupMessage,
				MessageDialog.QUESTION_WITH_CANCEL,
				new String[]{IDialogConstants.YES_LABEL,
						IDialogConstants.NO_LABEL,
						IDialogConstants.CANCEL_LABEL
				},0);
		int ret = confirm.open();
		if (ret == 0){
			//perform backup
			BackupHandler handler = new BackupHandler();
			handler.executeBackup(shell, false);
			if (!handler.backupOk()){
				MessageDialog.openError(shell, Messages.RestoreHandler_Error_DialogTitle, Messages.RestoreHandler_Error_Message);
			}
			
		}else if (ret == 1){
			//no - continue without performing backup
		}else if (ret == 2){
			//cancel
			return false;
		}
			
		final RestoreDialog dialog = new RestoreDialog(shell,
				Messages.RestoreHandler_DialogTitle,
				Messages.RestoreHandler_DialogMessage, 
				Messages.RestoreHandler_DialogShellTitle, Messages.RestoreHandler_RestoreButton);

		if (dialog.open() != IDialogConstants.OK_ID) {
			return false;
		}
		final boolean[] ok = new boolean[]{false};
		try {
			final ProgressMonitorDialog pmdDialog = new ProgressMonitorDialog(shell);
			pmdDialog.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					DisplayAccess.accessDisplayDuringStartup();
					File f = dialog.getSelectedFile();
					try{
						DerbyRestoreEngine.restoreSystem(f, monitor);
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								MessageDialog.openInformation(shell, Messages.RestoreHandler_ReportComplete_DialogTitle, Messages.RestoreHandler_ReportComplete_DialogMessage2);
								
							}});		
						ok[0] = true;
					}catch (final Exception ex){
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPlugIn.displayLog(shell,Messages.RestoreHandler_ReportFailed_Message + ex.getLocalizedMessage(), ex);
							}});
					}
				}
				
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(shell,
					Messages.RestoreHandler_ReportFailed_Message + ex.getLocalizedMessage(), ex);
			return false;
		}
		return ok[0];
	}

}
