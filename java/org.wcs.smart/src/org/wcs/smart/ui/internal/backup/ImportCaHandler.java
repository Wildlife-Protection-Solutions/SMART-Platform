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
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.in.CaImporter;

/**
 * Handler for importing a conservation area.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ImportCaHandler {

	public void execute(final Shell shell){

		MessageDialog confirm = new MessageDialog(
				shell,
				Messages.ImportCaHandler_Confirm_DialogTitle,null,
				Messages.ImportCaHandler_Confirm_DialogMessage,
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
				MessageDialog.openError(shell, Messages.ImportCaHandler_Error_DialogTitle, Messages.ImportCaHandler_Error_BackupMessage);
			}
			
		}else if (ret == 1){
			//no - continue without performing backup
		}else if (ret == 2){
			//cancel
			return;
		}
			
		final RestoreDialog dialog = new RestoreDialog(shell,
				Messages.ImportCaHandler_DialogTitle,
				Messages.ImportCaHandler_DialogMessage, 
				Messages.ImportCaHandler_DialogTitle, Messages.ImportCaHandler_ImportButton);

		if (dialog.open() != IDialogConstants.OK_ID) {
			return ;
		}
		try {
			ProgressMonitorDialog pmdDialog = new ProgressMonitorDialog(shell);
			pmdDialog.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					DisplayAccess.accessDisplayDuringStartup();
					File f = dialog.getSelectedFile();
					try{
						CaImporter.importCa(f, monitor);
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								MessageDialog.openInformation(shell, Messages.ImportCaHandler_Complete_DialogTitle, Messages.ImportCaHandler_Complete_DialogMessage);
							}});
								
					}catch (final Exception ex){
						shell.getDisplay().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPlugIn.displayLog(Messages.ImportCaHandler_ImportFailed_Message + ex.getLocalizedMessage(), ex);
							}});
					}

				}
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.ImportCaHandler_ImportFailed_Message + ex.getLocalizedMessage(), ex);
		}
		return;
	}
}
