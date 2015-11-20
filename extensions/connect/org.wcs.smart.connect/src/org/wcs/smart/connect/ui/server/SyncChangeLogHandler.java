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
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.internal.Messages;
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
public class SyncChangeLogHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) {
		SyncChangeLogDialog dialog = new SyncChangeLogDialog(activeShell);
		if (dialog.open() == Window.OK){
			syncChangeLog(activeShell, dialog.getConnection(), SmartDB.getCurrentConservationArea());
		}
	}

	/*
	 * download change log and apply
	 */
	public void syncChangeLog(final Shell activeShell, final SmartConnect connect, ConservationArea ca) {
		DownloadChangeLogHandler downhandler = new DownloadChangeLogHandler(){
			protected void displayStatus(final ConnectSyncHistoryRecord record) {
				if (record.getStatus() == Status.DONE ||
						record.getStatus() == Status.NODATA){
					//upload
					uploadChangeLog(activeShell, connect, ca);
				}else{
					super.displayStatus(record);
				}
			}
		};
		downhandler.downloadChangeLog(activeShell, connect, ca);
	
	}
	
	/*
	 * upload change log and apply
	 */
	private void uploadChangeLog(final Shell activeShell, final SmartConnect connect, final ConservationArea ca) {
		
		activeShell.getDisplay().syncExec(new Runnable(){
			@Override
			public void run() {
				UploadChangeLogHandler uphandler = new UploadChangeLogHandler(){
					protected void displayStatus(final ConnectSyncHistoryRecord record) {
						if (record.getStatus() == Status.DONE ||
								record.getStatus() == Status.NODATA){
							Display.getDefault().syncExec(new Runnable(){

								@Override
								public void run() {
									MessageDialog.openInformation(activeShell, Messages.SyncChangeLogHandler_DialogTitle, Messages.SyncChangeLogHandler_DialogMessage);
								}
								
							});
						}else{
							super.displayStatus(record);
						}
					}
				};
				uphandler.uploadChangeLog(activeShell, connect, ca);		
			}
			
		});
		
	}

	
	
	public static class SyncChangeLogHandlerWrapper extends DIHandler<SyncChangeLogHandler>{
		public SyncChangeLogHandlerWrapper() {
			super(SyncChangeLogHandler.class);
		}
		
	}
}
