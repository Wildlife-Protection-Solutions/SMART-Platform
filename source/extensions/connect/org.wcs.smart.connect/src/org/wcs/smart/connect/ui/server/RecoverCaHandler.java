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
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.internal.server.RecoverCaEngine;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Download change log handler for manually downloading 
 * change log from server.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class RecoverCaHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) {

		RecoverCaDialog dialog = new RecoverCaDialog(activeShell);
		if (dialog.open() != Window.OK) return;
		if (dialog.getOption() == null) return;


		if (dialog.getOption() == RecoverCaDialog.Option.RECOVER) {
			doRecover(activeShell, dialog.getApplyNew());
		}else if (dialog.getOption() == RecoverCaDialog.Option.REPLACE) {
			(new DownloadReplaceCaHandler()).execute(activeShell, dialog.getApplyNew());
		}
	}

	private void doRecover(Shell activeShell, boolean applyNew) {
		ConnectDialog cdialog = new ConnectDialog(activeShell, true) {
			@Override
			protected Control createDialogArea(Composite parent) {
				setTitle(Messages.RecoverCaHandler_Title);
				setMessage(Messages.RecoverCaHandler_Message);
				getShell().setText(Messages.RecoverCaHandler_Shell);
			
				return super.createDialogArea(parent);
			}	
		};
		if (cdialog.open() != Window.OK) return;
		
		SmartConnect connect = cdialog.getConnection();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try {
			pmd.run(true, true, monitor->{
				RecoverCaEngine engine = new RecoverCaEngine(SmartDB.getCurrentConservationArea(), connect, applyNew, pmd);
				try {
					engine.downloadImport(monitor);
				} catch (Exception e) {
					ConnectPlugIn.displayLog(e.getMessage(), e);
				}
			});
		}catch (Exception ex) {
			ConnectPlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}
	
	public static class RecoverCaHandlerWrapper extends DIHandler<RecoverCaHandler>{
		public RecoverCaHandlerWrapper() {
			super(RecoverCaHandler.class);
		}
		
	}
}
