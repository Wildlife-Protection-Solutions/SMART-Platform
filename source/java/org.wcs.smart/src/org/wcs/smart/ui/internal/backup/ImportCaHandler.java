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
import org.wcs.smart.internal.ca.in.CaImporter;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author egouge
 * @since 1.0.0
 */
public class ImportCaHandler {

	public void execute(final Shell shell){
		
//		if (!DerbyRestoreEngine.validateUserRestore(shell)){
//			return;
//		}
		
		MessageDialog confirm = new MessageDialog(
				shell,
				"Confirm Restore",null,
				"Would you like to backup the current database before importing your conservation area?",
				MessageDialog.QUESTION_WITH_CANCEL,
				new String[]{IDialogConstants.YES_LABEL,
						IDialogConstants.NO_LABEL,
						IDialogConstants.CANCEL_LABEL
				},0);
		int ret = confirm.open();
		if (ret == 0){
			//perform backup
			BackupHandler handler = new BackupHandler();
			handler.executeBackup(shell);
			if (!handler.backupOk()){
				MessageDialog.openError(shell, "Error", "Error occurred during backup process.  Restore will not proceed.");
			}
			
		}else if (ret == 1){
			//no - continue without performing backup
		}else if (ret == 2){
			//cancel
			return;
		}
			
		final RestoreDialog dialog = new RestoreDialog(shell,
				"Import Conservation Area",
				"Select the conservation area data file.", 
				"Import Conservation Area", "Import");

		if (dialog.open() != IDialogConstants.OK_ID) {
			return ;
		}
		try {
			ProgressMonitorDialog pmdDialog = new ProgressMonitorDialog(shell);
			pmdDialog.run(false, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					File f = dialog.getSelectedFile();
					try{
						CaImporter.importCa(f, monitor);
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
