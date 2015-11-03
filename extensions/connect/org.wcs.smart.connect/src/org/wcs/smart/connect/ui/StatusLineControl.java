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
package org.wcs.smart.connect.ui;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.ConnectStatusManager;
import org.wcs.smart.connect.ConnectStatusManager.ServerStatus;
import org.wcs.smart.connect.IConnectStatusListener;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Status control that is displayed in the status bar
 * that informs the user of the status of the replication 
 * 
 * @author Emily
 *
 */
public class StatusLineControl extends WorkbenchWindowControlContribution {

	private Label localStatus;
	private Label serverStatus;
	
	private IConnectStatusListener serverListener = new IConnectStatusListener() {
		
		@Override
		public void statusModified(ServerStatus status, String message) {
			updateServerStatus(status, message);
		}
	};
	
	private IConnectStatusListener localListener = new IConnectStatusListener() {
		
		@Override
		public void statusModified(ServerStatus status, String message) {
			updateLocalStatus(status, message);
		}
	};
	
	public StatusLineControl() {	
		ConnectStatusManager.INSTANCE.addServerStatusListener(serverListener);
		ConnectStatusManager.INSTANCE.addLocalStatusListener(localListener);
	}

	@Override
	protected Control createControl(Composite parent) {
		Composite status = new Composite(parent, SWT.NONE);
		status.setLayout(new GridLayout(2, true));

		serverStatus = new Label(status, SWT.NONE);
		serverStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_ERROR_ICON));
		
		localStatus = new Label(status, SWT.NONE);
		localStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.LOCAL_ERROR_ICON));
		
		updateLocalChanges.schedule();
		return status;
	}

	private void updateServerStatus(ConnectStatusManager.ServerStatus status, String message){
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				if (status == ServerStatus.CHANGES){
					serverStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_CHANGES_ICON));
				}else if (status == ServerStatus.UPTODATE){
					serverStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_OK_ICON));
				}else if (status == ServerStatus.CONNECTING){
					serverStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_PROCESSING_ICON));
				}else if (status == ServerStatus.DOWNLOADING){
					serverStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_PROCESSING_ICON));
				}else if (status == ServerStatus.ERROR){
					serverStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_ERROR_ICON));
				}
				if (message == null){
					serverStatus.setToolTipText("");
				}else{
					serverStatus.setToolTipText(formatMessage(message));
				}
			}
		});
	}
	
	private String formatMessage(String message){
		if (message != null) message = MessageFormat.format( "({0}) {1}", DateFormat.getTimeInstance().format(new Date()), message);
		return message;
	}
	
	private String formatLocalMessage(ConnectStatusManager.ServerStatus status, String message){
		if (message == null){
			if (status == ServerStatus.CHANGES){
				message = "There are local changes that need to be uploaded to server.";	
			}else if (status == ServerStatus.UPTODATE){
				message = "All local changes have been applied to the server.";	
			}else{
				message = "Error determining state of local database. ";
			}
		}
		return formatMessage(message);
	}
	
	private void updateLocalStatus(ConnectStatusManager.ServerStatus status, String message){
		if (status == null){
			updateLocalChanges.schedule(0);
			return;
		}
		
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				if (status == ServerStatus.CHANGES){
					localStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.LOCAL_CHANGES_ICON));
				}else if (status == ServerStatus.UPTODATE){
					localStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.LOCAL_OK_ICON));
				}else if (status == ServerStatus.CONNECTING){
					localStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.LOCAL_PROCESSING_ICON));
				}else if (status == ServerStatus.DOWNLOADING){
					localStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.LOCAL_PROCESSING_ICON));
				}else if (status == ServerStatus.ERROR){
					localStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.LOCAL_ERROR_ICON));
				}
				String tooltip = formatLocalMessage(status, message);
				if (tooltip == null) tooltip = "";
				localStatus.setToolTipText(tooltip);
			}
		});
	}
	
	private Job updateLocalChanges = new Job("update local database replication state"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String message = null;
			ServerStatus status = ServerStatus.ERROR;
					
			if (DerbyReplicationManager.INSTANCE.getLocalReplicationState()){
				Session session = HibernateManager.openSession();
				try{
					Boolean hasChanges = DerbyReplicationManager.INSTANCE.hasLocalChanges(session);
					if (hasChanges != null){
						if (hasChanges){
							status = ServerStatus.CHANGES;
						}else{
							status = ServerStatus.UPTODATE;
						}
					}
				}finally{
					session.close();
				}
			}else{
				message = "Connect server not configured.";
			}
			updateLocalStatus(status, message);

			//schedule every 30 seconds
			schedule(ConnectStatusManager.CHECK_LOCAL_STATUS);
			return Status.OK_STATUS;
		}	
	};
}
