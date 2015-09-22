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

import java.lang.reflect.InvocationTargetException;

import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.server.replication.DownloadChangeLogEngine;

/**
 * Download change log handler.
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

	public void downloadChangeLog(Shell activeShell, final SmartConnect connect){
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					DownloadChangeLogEngine engine = new DownloadChangeLogEngine(connect);
					try{
						engine.downloadInstall(monitor);
					}catch (Exception ex){
						ConnectPlugIn.displayLog(ex.getMessage(), ex);
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			ConnectPlugIn.displayLog(e.getMessage(), e);
		}
	}

	public static class DownloadChangeLogHandlerWrapper extends DIHandler<DownloadChangeLogHandler>{
		public DownloadChangeLogHandlerWrapper() {
			super(DownloadChangeLogHandler.class);
		}
		
	}
}
