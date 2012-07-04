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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;

/**
 * 
 * Displays the dialog to delete the conservation area.
 * 
 * @author Emily
 *
 */
public class DeleteConservationArea extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		
		//ensure the user has permission
		if (SmartDB.getCurrentEmployee().getSmartUserLevel() != SmartUserLevel.ADMIN){
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Delete", "You do not have permission to delete conservation areas.  Please contact an administrator.");
			return null;
		}
		
		if (!MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Delete", "Are you sure you want to delete this conservation area.  This action cannot be undone.")){
			return null;
		}
		
		UserNamePasswordDialog dialog = new UserNamePasswordDialog(Display.getCurrent().getActiveShell(),
				"Delete Conservation Area",
				"Enter your username and password to confirm that you want to delete this conservation area.",
				"Delete");
		if (dialog.open() == Window.CANCEL){
			return null;
		}
		
		if (!(dialog.getUserName().equalsIgnoreCase(SmartDB.getCurrentEmployee().getSmartUserId())
			&& dialog.getPassword().equals(SmartDB.getCurrentEmployee().getSmartPassword())	)){
			
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "Invalid username and password.");
			return null;
		}
		
		
		//prompt to save dirty editors
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i = 0; i < windows.length; i ++){
			IWorkbenchPage[] pages = windows[i].getPages();
			for (int j = 0; j < pages.length; j ++){
				pages[j].closeAllEditors(false);
			}
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
		try{
		pmd.run(false, false, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				
				monitor.setTaskName("Deleting Conservation Area");
				ConservationArea ca = SmartDB.getCurrentConservationArea();
				try{
					ConservationAreaManager.getInstance().deleteConservationArea(ca, monitor);
				}catch (Exception ex){
					SmartPlugIn.displayLog(Display.getCurrent().getActiveShell(), "Error deleting conservation area.", ex);
				}
				
			}
		});
		}catch (Exception ex){
			SmartPlugIn.displayLog(Display.getCurrent().getActiveShell(), "Error running delete job.", ex);
		}
		
		
		return null;
	}

}
