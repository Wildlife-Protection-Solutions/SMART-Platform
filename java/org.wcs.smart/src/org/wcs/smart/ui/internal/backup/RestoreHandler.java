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
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.backup.DerbyRestoreEngine;

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
	public void execute(final Shell shell) {

		if (!MessageDialog.openConfirm(shell, "Restore", "Restoring a backup file will cause" +
				" all conservation areas in the existing database to be removed and replaced" +
				" with the data in the backup file.\n\nAre you sure you want to continue?")){
			return;
		}
		
		if (!DerbyRestoreEngine.validateUserRestore(shell)){
			return;
		}
		
		MessageDialog confirm = new MessageDialog(
				shell,
				"Confirm Restore",null,
				"It is recommeneded you backup the current database before restoring.  Would you like to backup now?",
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
				MessageDialog.openError(shell, "Error", "Error occurred during backup process.  Restore will not proceed.");
			}
			
		}else if (ret == 1){
			//no - continue without preforming backup
		}else if (ret == 2){
			//cancel
			return;
		}
			
		final RestoreDialog dialog = new RestoreDialog(shell,
				"Restore SMART backup",
				"Select the file to restore.", 
				"Restore", "Restore");

		if (dialog.open() != IDialogConstants.OK_ID) {
			return ;
		}
		try {
			final ProgressMonitorDialog pmdDialog = new ProgressMonitorDialog(shell);
			pmdDialog.run(false, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					File f = dialog.getSelectedFile();
					try{
						DerbyRestoreEngine.restoreSystem(f, monitor);	
						MessageDialog.openInformation(shell, "Restore Complete", "System restore completed");
					}catch (Exception ex){
						SmartPlugIn.displayLog(shell,"Restore Failed.\n\n" + ex.getMessage(), ex);
					}
				}
				
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(shell,
					"Restore Failed. " + ex.getMessage(), ex);
		}
		return;
	}

}
