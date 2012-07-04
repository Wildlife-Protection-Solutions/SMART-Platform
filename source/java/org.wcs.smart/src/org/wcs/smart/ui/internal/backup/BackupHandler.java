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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.backup.DerbyBackupEngine;

/**
 * Handler for performing backup command.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class BackupHandler extends AbstractHandler {

	private boolean backupState = false;
	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		executeBackup(HandlerUtil.getActiveShell(event));
		return null;
	}

	/**
	 * Execute the backup commend; prompting user as required
	 * @param shell current shell
	 */
	public void executeBackup(final Shell shell){
		backupState = false;
		
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
		
		final BackupDialog dialog = new BackupDialog(shell,"Backup SMART System", "Select the file to backup the system to.", "Backup",DerbyBackupEngine.getDefaultFileName());
		if (dialog.open() != IDialogConstants.OK_ID) {
			return ;
		}
		try {
			ProgressMonitorDialog pmdDialog = new ProgressMonitorDialog(shell);
			pmdDialog.run(false, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					File f = dialog.getSelectedFile();
					try{
						if(DerbyBackupEngine.backupSystem(f, monitor)){					
							MessageDialog.openInformation(shell, "Backup Complete", "System backed up successfully to file: \n\n" +  f.getAbsolutePath());
							backupState = true;
						}else if (monitor.isCanceled()){
							MessageDialog.openError(shell, "Backup Failed", "Backup process cancelled");
						}else{
							MessageDialog.openError(shell, "Backup Failed", "Backup did not complete.  Please try again.");
						}
					}catch (Exception ex){
						SmartPlugIn.displayLog(shell,
								"Backup Failed. " + ex.getMessage(), ex);
					}

				}
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(shell,
					"Backup Failed. " + ex.getMessage(), ex);
		}
	}
	
	
	public boolean backupOk(){
		return backupState;
	}
}
