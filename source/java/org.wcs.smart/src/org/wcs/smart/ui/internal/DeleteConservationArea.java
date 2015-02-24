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
package org.wcs.smart.ui.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.UserNamePasswordDialog;

/**
 * 
 * Displays the dialog to delete the conservation area.
 * 
 * @author Emily
 *
 */
public class DeleteConservationArea {

	@Execute
	public void execute(final Shell activeShell) throws ExecutionException {
		
		//ensure the user has permission
		if (SmartDB.getCurrentEmployee().getSmartUserLevel() != SmartUserLevel.ADMIN){
			MessageDialog.openInformation(activeShell, Messages.DeleteConservationArea_Delete_DialogTitle, Messages.DeleteConservationArea_Error_Permission);
			return;
		}
		
		if (!MessageDialog.openConfirm(activeShell, Messages.DeleteConservationArea_Delete_DialogTitle, Messages.DeleteConservationArea_Confirm_Delete)){
			return;
		}
		
		UserNamePasswordDialog dialog = new UserNamePasswordDialog(activeShell,
				Messages.DeleteConservationArea_UserNameConfirmation_DialogTitle,
				Messages.DeleteConservationArea_UserNameConfirmation_DialogMessage,
				Messages.DeleteConservationArea_UserNameConfirmation_DialogButton);
		if (dialog.open() == Window.CANCEL){
			return;
		}
		
		if (!(dialog.getUserName().equalsIgnoreCase(SmartDB.getCurrentEmployee().getSmartUserId())
			&& dialog.getPassword().equals(SmartDB.getCurrentEmployee().getSmartPassword())	)){
			
			MessageDialog.openError(activeShell, Messages.DeleteConservationArea_Error_DialogTitle, Messages.DeleteConservationArea_Error_Username_DialogMessage);
			return;
		}
		
		
		//prompt to save dirty editors
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i = 0; i < windows.length; i ++){
			IWorkbenchPage[] pages = windows[i].getPages();
			for (int j = 0; j < pages.length; j ++){
				pages[j].closeAllEditors(false);
			}
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try{
		pmd.run(true, false, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				
				monitor.setTaskName(Messages.DeleteConservationArea_Progress_DeletingCa);
				ConservationArea ca = SmartDB.getCurrentConservationArea();
				try{
					ConservationAreaManager.getInstance().deleteConservationArea(ca, monitor);
				}catch (final Exception ex){
					activeShell.getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							SmartPlugIn.displayLog(Messages.DeleteConservationArea_Error_DeleteError, ex);
						}
						
					});
					
				}
				
			}
		});
		}catch (Exception ex){
			SmartPlugIn.displayLog( Messages.DeleteConservationArea_Error_DeleteJobError, ex);
		}
		return;
	}

	// E3
	public static class DeleteConservationAreaWrapper extends DIHandler<DeleteConservationArea> {
		public DeleteConservationAreaWrapper() {
			super(DeleteConservationArea.class);
		}
	}
}
