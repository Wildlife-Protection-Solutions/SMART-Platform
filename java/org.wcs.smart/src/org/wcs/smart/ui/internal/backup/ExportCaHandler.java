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
import java.text.MessageFormat;

import org.eclipse.core.commands.ExecutionException;
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
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.export.CaExporter;

/**
 * Handler for exporting conservation area
 * data.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ExportCaHandler {

	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Execute
	public void execute(Shell activeShell) throws ExecutionException {
		exportCa(activeShell);
	}

	/**
	 * Execute the backup commend; prompting user as required
	 * @param shell current shell
	 */
	public void exportCa(final Shell shell){
		// prompt to save dirty editors
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			IWorkbenchPage[] pages = windows[i].getPages();
			for (int j = 0; j < pages.length; j++) {
				if (!pages[j].saveAllEditors(true)) {
					return;
				}
			}
		}
				
		final BackupDialog dialog = new BackupDialog(shell,
				Messages.ExportCaHandler_DialogTitle, 
				MessageFormat.format(Messages.ExportCaHandler_DialogMessage, 
						new Object[]{ SmartDB.getCurrentConservationArea().getName() }),
				Messages.ExportCaHandler_ExportButton, 
				"org.wcs.smart.exportca.location", //$NON-NLS-1$
				CaExporter.getDefaultFileName(), false);
		
		if (dialog.open() != IDialogConstants.OK_ID) {
			return ;
		}
		try {
			ProgressMonitorDialog pmdDialog = new ProgressMonitorDialog(shell);
			pmdDialog.run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					File f = dialog.getSelectedFile();

					CaExporter exporter = new CaExporter();
					try{
						exporter.export(f, monitor);
						
						String message =  Messages.ExportCaHandler_ExportComplete_DialogMessage;
						if (monitor.isCanceled()){
							message = Messages.ExportCaHandler_ExportCancelledMessage;
						}
						final String fmessage = message;
						shell.getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								MessageDialog.openInformation(shell, Messages.ExportCaHandler_ExportComplete_DialogTitle, fmessage);
							}
						});
					}catch (final Exception ex){
						shell.getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								SmartPlugIn.displayLog(Messages.ExportCaHandler_Error_ExportFailedMessage + ex.getLocalizedMessage(), ex);
							}
						});
					}
				}
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.ExportCaHandler_Error_ExportFailedMessage + ex.getLocalizedMessage(), ex);
		}
	}
	
	// E3
	public static class ExportCaHandlerWrapper extends DIHandler<ExportCaHandler> {
		public ExportCaHandlerWrapper() {
			super(ExportCaHandler.class);
		}
	}
}
