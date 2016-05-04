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
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaProxy;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.internal.server.DownloadCaEngine;
import org.wcs.smart.connect.internal.server.replication.SyncChangesRunnable;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Handler for downloading and installing CCAA analysis from Server
 * @author Emily
 *
 */
public class DownloadInstallCcaaHandler {
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) {
		if (!SmartDB.getCurrentConservationArea().getIsCcaa()) return;
		
		if (!MessageDialog.openQuestion(activeShell, Messages.DownloadInstallCcaaHandler_Title, Messages.DownloadInstallCcaaHandler_Warning)){
			return;
		}
		boolean sync = false;
		Session session = HibernateManager.openSession();
		try{
			//prompt user to sync any existing changes to server before downloading
			if (DerbyReplicationManager.INSTANCE.isReplicationEnabled(SmartDB.getCurrentConservationArea().getUuid(), 
					session)){
				if (DerbyReplicationManager.INSTANCE.hasLocalChanges(session)){
					if (MessageDialog.openQuestion(activeShell, Messages.DownloadInstallCcaaHandler_SyncTitle, Messages.DownloadInstallCcaaHandler_SyncMessage)){
						//sync changes
						sync = true;
					}
				}
			}
		}finally{
			session.close();
		}
		if (sync){
			runDownloadSync(SmartDB.getCurrentConservationArea(), activeShell);
		}
		
		ConnectDialog dialog = new ConnectDialog(activeShell){
			@Override
			protected Control createDialogArea(Composite parent) {
				setTitle(Messages.DownloadInstallCcaaHandler_DialogTitle);
				setMessage(Messages.DownloadInstallCcaaHandler_DialogMessage);
				getShell().setText(Messages.DownloadInstallCcaaHandler_ShellTitle);
				
				return super.createDialogArea(parent);
			}	
		};
		if (dialog.open() != Window.OK){
			return;
		}
		
		SmartConnect connect = dialog.getConnection();
		ConservationAreaProxy proxy = new ConservationAreaProxy();
		proxy.setUuid(ConservationArea.MULTIPLE_CA);
		final DownloadCaEngine installer = new DownloadCaEngine(proxy, connect);
		
		final List<Exception> errors = new ArrayList<Exception>();
		try{
			ProgressMonitorDialog monitor = new ProgressMonitorDialog(activeShell);
			monitor.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try{
						if (!installer.downloadImport(monitor)){
							errors.add(new Exception(Messages.DownloadConnectWizard_Cancelled));
						}
						
					}catch (Exception ex){
						errors.add(ex);
					}
				}
			});
		}catch (Exception ex){
			errors.add(ex);
		}
		if (errors.isEmpty()){
			PlatformUI.getWorkbench().restart();
			return;
		}
		ConnectPlugIn.displayLog(Messages.DownloadConnectWizard_DownloadError + "\n\n" + errors.get(0).getMessage(), errors.get(0)); //$NON-NLS-1$
		
	}

	/* 
	 * run a sync and wait until complete to continue
	 * 
	 */
	private void runDownloadSync(ConservationArea ca, Shell activeShell){
		ConnectDialog dialog = new SyncChangeLogDialog(activeShell);
		
		if (dialog.open() != Window.OK) return;
		final SmartConnect connect = dialog.getConnection();
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try{
			SyncChangesRunnable runnable = new SyncChangesRunnable(connect, ca, true);
			pmd.run(true, false, runnable);
			//throw any exceptions generated while running
			if (runnable.getThrownException() != null){
				throw runnable.getThrownException();
			}
		}catch (Exception ex){
			ConnectPlugIn.displayLog(ex.getMessage(), ex);
		}
	}
	
	public static class DownloadInstallCcaaHandlerWrapper extends DIHandler<DownloadInstallCcaaHandler>{
		public DownloadInstallCcaaHandlerWrapper() {
			super(DownloadInstallCcaaHandler.class);
		}
		
	}
}
