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
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.DisplayAccess;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.backup.DerbyBackupEngine;
import org.wcs.smart.internal.Messages;

/**
 * Handler for performing backup command.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class BackupHandler {

	private int backupState = 0;
	private File backupFile = null;
	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Execute
	public void execute(Shell activeShell) {
		executeBackup(activeShell, true);
	}

	/**
	 * Execute the backup commend; prompting user as required
	 * @param shell current shell
	 * @param fork - <code>true</code> if not called from within splash screen thread, 
	 * <code>false</code> if called from the splash screen
	 */
	/*
	 * fork is true and called from within the splash screen thread causes application
	 * deadlock.
	 */
	public void executeBackup(final Shell shell, final boolean fork){
		backupState = 0;
		
		//prompt to save dirty editors
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i = 0; i < windows.length; i ++){
			IWorkbenchPage[] pages = windows[i].getPages();
			for (int j = 0; j < pages.length; j ++){
				if (!pages[j].saveAllEditors(true)){
					return;
				}
			}
		}
		
		final BackupDialog dialog = new BackupDialog(shell,
				Messages.BackupHandler_Backup_DialogTitle, 
				Messages.BackupHandler_Backup_DialotMessage, 
				Messages.BackupHandler_Backup_DialogButton,
				"org.wcs.smart.backup.location", //$NON-NLS-1$
				DerbyBackupEngine.getDefaultFileName(), true); 
		
		if (dialog.open() != IDialogConstants.OK_ID) {
			return ;
		}
		try {
			ProgressMonitorDialog pmdDialog = new ProgressMonitorDialog(shell);
			pmdDialog.run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					if (!fork){
						DisplayAccess.accessDisplayDuringStartup();
					}
					backupFile = dialog.getSelectedFile();
					try {
						final boolean ok = DerbyBackupEngine.backupSystem(backupFile,monitor);						
						if (ok){
							backupState = 1;
						}else if (monitor.isCanceled()){
							backupState = 2;
						}

					} catch (final Exception ex) {
						backupState = 0;
						SmartPlugIn.log(Messages.BackupHandler_Error_BackupError, ex);
					}

				}
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.BackupHandler_Error_BackupError + ex.getLocalizedMessage(), ex);
		}
		
		if (backupState == 1){
			MessageDialog.openInformation(shell,
					Messages.BackupHandler_BackupComplete_DialogTitle,
					Messages.BackupHandler_BackupComplete_DialogMessage + "\n\n" //$NON-NLS-1$
							+ backupFile.getAbsolutePath());
		}else if (backupState == 2){
			MessageDialog.openError(shell,
					Messages.BackupHandler_BackupFailed_DialogTitle,
					Messages.BackupHandler_BackupCancelled_DialogMessage);
		}else{
			MessageDialog
			.openError(shell, Messages.BackupHandler_BackupFailed_DialogTitle,
					Messages.BackupHandler_BackupFailed_DialogMessage);
		}

	}
	
	
	public boolean backupOk(){
		return backupState == 1;
	}
	
	// E3
	public static class BackupHandlerWrapper extends DIHandler<BackupHandler> {
		public BackupHandlerWrapper() {
			super(BackupHandler.class);
		}
	}
}
