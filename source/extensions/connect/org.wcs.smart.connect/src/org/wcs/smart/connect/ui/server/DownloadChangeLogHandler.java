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
package org.wcs.smart.connect.ui.server;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.internal.server.replication.DownloadChangeLogEngine;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Download change log handler for manually downloading 
 * change log from server.
 * 
 * @author Emily
 *
 */
public class DownloadChangeLogHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) {
		DownloadChangeLogDialog dialog = new DownloadChangeLogDialog(activeShell);
		if (dialog.open() == Window.OK){
			downloadChangeLog(activeShell, dialog.getConnection(), SmartDB.getCurrentConservationArea());
		}
	}

	/**
	 * Prompts the user with a message informing user download will happen in background then
	 * starts the download process.
	 * @param activeShell
	 * @param pService
	 * @param connect
	 * @param events
	 */
	public void downloadChangeLog(final Shell activeShell, final SmartConnect connect, ConservationArea ca) {
		MessageDialog
				.openInformation(
						activeShell,
						Messages.DownloadChangeLogHandler_DialogTitle,
						Messages.DownloadChangeLogHandler_BackgroundProcessMessage);
		
		DownloadChangeLogEngine engine = new DownloadChangeLogEngine(ca, connect) {
			protected void processComplete() {
				super.processComplete();
				displayStatus(record);
			}			
		};
		try {
			engine.downloadInstall();
		} catch (Exception ex) {
			ConnectPlugIn.displayLog(ex.getMessage(), ex);
		}
	}
	

	/* 
	 * displays status when complete
	 * 
	 */
	protected void displayStatus(final ConnectSyncHistoryRecord record) {
		Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					String title = Messages.DownloadChangeLogHandler_DialogTitle;
					String message = ""; //$NON-NLS-1$
					boolean error = false;
					if(record.getStatus() == Status.DONE) {
						message = Messages.DownloadChangeLogHandler_SuccessMessage;
					}else if(record.getStatus() == Status.NODATA) {
						message = Messages.DownloadChangeLogHandler_NothingToDoMessage;
					}else {
						title = Messages.DownloadChangeLogHandler_ErrorDialogTitle;
						message = Messages.DownloadChangeLogHandler_ErrorMessage + record.getErrorString();
						error=true;
					}
					if (error){
						MessageDialog.openError(Display.getDefault().getActiveShell(), title, message);	
					}else{
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), title, message);
					}
					
				}

		});
	}
	
	
	public static class DownloadChangeLogHandlerWrapper extends DIHandler<DownloadChangeLogHandler>{
		public DownloadChangeLogHandlerWrapper() {
			super(DownloadChangeLogHandler.class);
		}
		
	}
}
