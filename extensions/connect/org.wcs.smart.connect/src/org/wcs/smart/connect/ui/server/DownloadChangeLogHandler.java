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
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.server.replication.DownloadChangeLogEngine;
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
			downloadChangeLog(activeShell, dialog.getConnection());
		}
	}

	/**
	 * download change log and apply
	 * @param activeShell
	 * @param pService
	 * @param connect
	 * @param events
	 */
	public void downloadChangeLog(final Shell activeShell, final SmartConnect connect) {
		MessageDialog
				.openInformation(
						activeShell,
						"Download",
						"SMART will download changes in the background.  Once download, if there are changes to apply, you will be prompted before changes are applied.");
		
		DownloadChangeLogEngine engine = new DownloadChangeLogEngine(SmartDB.getCurrentConservationArea(), connect) {
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
					String title = "Download";
					String message = "";
					boolean error = false;
					if(record.getStatus() == Status.DONE) {
						message = "Download and apply completed successfully.";
					}else if(record.getStatus() == Status.NODATA) {
						message = "Local database up-to-date. Nothing to apply.";
					}else {
						title = "Download Error";
						message = "Error: " + record.getErrorString();
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
