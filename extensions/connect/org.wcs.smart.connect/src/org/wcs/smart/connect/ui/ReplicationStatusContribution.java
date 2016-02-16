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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.ConnectStatusManager;
import org.wcs.smart.connect.IConnectStatusListener;
import org.wcs.smart.connect.ConnectStatusManager.ServerStatus;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.internal.server.replication.AutoReplicationJob;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Replication status line contribution.
 * @author Emily
 *
 */
public class ReplicationStatusContribution implements
		IConnectStatusContribution {

	private Label localStatus;
	private Label serverStatus;
	private AutoReplicationJob updateServerNowJob = new AutoReplicationJob(true);
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
	
	public ReplicationStatusContribution(){

	}
	
	@Override
	public void refresh() {
		updateLocalChanges.cancel();
		updateServerNowJob.cancel();
		updateLocalChanges.schedule();
		updateServerNowJob.schedule();
	}

	@Override
	public Control createControl(Composite parent) {
		ConnectStatusManager.INSTANCE.addServerStatusListener(serverListener);
		ConnectStatusManager.INSTANCE.addLocalStatusListener(localListener);
		
		Composite status = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, true);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		status.setLayout(gl);
		
		status.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				ConnectStatusManager.INSTANCE.removeServerStatusListener(serverListener);
				ConnectStatusManager.INSTANCE.removeLocalStatusListener(localListener);
			}
		});
		
		serverStatus = new Label(status, SWT.NONE);
		serverStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_ERROR_ICON));
		
		localStatus = new Label(status, SWT.NONE);
		localStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.LOCAL_ERROR_ICON));

		updateServerStatus(ServerStatus.ERROR, Messages.StatusLineControl_UnknownState);
		updateLocalStatus(ServerStatus.ERROR, Messages.StatusLineControl_UnknownState);
		
		updateLocalChanges.setSystem(true);
		updateLocalChanges.schedule();
		
		return status;
	}

	private void updateServerStatus(ConnectStatusManager.ServerStatus status, String message){
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				if (serverStatus.isDisposed()) return;
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
					serverStatus.setToolTipText(""); //$NON-NLS-1$
				}else{
					serverStatus.setToolTipText(formatMessage(message));
				}
			}
		});
	}
	
	private String formatMessage(String message){
		if (message != null) message = MessageFormat.format( Messages.StatusLineControl_MessageFormatDateString, DateFormat.getTimeInstance().format(new Date()), message);
		return message;
	}
	
	private String formatLocalMessage(ConnectStatusManager.ServerStatus status, String message){
		if (message == null){
			if (status == ServerStatus.CHANGES){
				message = Messages.StatusLineControl_LocalChanges;	
			}else if (status == ServerStatus.UPTODATE){
				message = Messages.StatusLineControl_NoLocalChanges;	
			}else{
				message = Messages.StatusLineControl_LocalError;
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
				if (localStatus.isDisposed()) return;
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
				if (tooltip == null) tooltip = ""; //$NON-NLS-1$
				localStatus.setToolTipText(tooltip);
			}
		});
	}
	
	private Job updateLocalChanges = new Job(Messages.StatusLineControl_jobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			String message = null;
			ServerStatus status = ServerStatus.ERROR;
			
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				if (DerbyReplicationManager.INSTANCE.isReplicationEnabled(SmartDB.getCurrentConservationArea().getUuid(), session)){
					Boolean hasChanges = DerbyReplicationManager.INSTANCE.hasLocalChanges(session);
					if (hasChanges != null){
						if (hasChanges){
							status = ServerStatus.CHANGES;
						}else{
							status = ServerStatus.UPTODATE;
						}
					}
				}else{
					message = Messages.StatusLineControl_ServernotFound;
				}
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
			updateLocalStatus(status, message);

			//schedule every 30 seconds
			schedule(ConnectStatusManager.CHECK_LOCAL_STATUS);
			return Status.OK_STATUS;
		}	
	};
}
