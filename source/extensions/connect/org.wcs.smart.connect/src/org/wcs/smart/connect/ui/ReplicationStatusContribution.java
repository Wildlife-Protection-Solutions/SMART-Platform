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

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

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
import org.hibernate.jdbc.ReturningWork;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.ConnectStatusManager;
import org.wcs.smart.connect.ConnectStatusManager.ServerStatus;
import org.wcs.smart.connect.IConnectStatusListener;
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

	private Label lblStatus;
	
	private ServerStatus localStatus = ServerStatus.DISABLED;
	private ServerStatus serverStatus = ServerStatus.DISABLED;
	private String localMsg = null;
	private String serverMsg = null;
	
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
		GridLayout gl = new GridLayout();
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
		
		lblStatus = new Label(status, SWT.NONE);
		lblStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_ERROR_ICON));
		
		updateServerStatus(ServerStatus.ERROR, Messages.StatusLineControl_UnknownState);
		updateLocalStatus(ServerStatus.ERROR, Messages.StatusLineControl_UnknownState);
		
		updateLocalChanges.setSystem(true);
		updateLocalChanges.schedule();
		
		updateServerNowJob.schedule();
		
		return status;
	}

	private void updateServerStatus(ConnectStatusManager.ServerStatus status, String message){
		serverStatus = status;
		serverMsg = message;
		updateGui();
	}
	
	private String formatMessage(String message){
		if (message != null) 
			message = MessageFormat.format( Messages.StatusLineControl_MessageFormatDateString, 
				DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(LocalDateTime.now()), message);
		return message;
	}
	
	private String formatLocalMessage(ConnectStatusManager.ServerStatus status, String message){
		if (message == null){
			if (status == ServerStatus.CHANGES){
				message = Messages.ReplicationStatusContribution_syncrequired;	
			}else if (status == ServerStatus.UPTODATE){
				message = Messages.ReplicationStatusContribution_uptodate;
			}else if (status == ServerStatus.DISABLED){
				message = Messages.ReplicationStatusContribution_notconfigured;
			}else{
				message = Messages.ReplicationStatusContribution_error;
			}
		}
		return formatMessage(message);
	}
	
	private void updateGui() {
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				if (lblStatus.isDisposed()) return;
				
				if (serverStatus == ServerStatus.DISABLED) {
					lblStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_DISABLED_ICON));
					lblStatus.setToolTipText(formatLocalMessage(ServerStatus.DISABLED, serverMsg));
					
				}else if (serverStatus == ServerStatus.ERROR) {
					lblStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_ERROR_ICON));
					lblStatus.setToolTipText(formatLocalMessage(ServerStatus.DISABLED, serverMsg));
					
				}else if (serverStatus == ServerStatus.CONNECTING || serverStatus == ServerStatus.DOWNLOADING){
					lblStatus.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
					lblStatus.setToolTipText(formatLocalMessage(ServerStatus.DISABLED, serverMsg));
					
				}else if (serverStatus == ServerStatus.CHANGES || localStatus == ServerStatus.CHANGES){
					lblStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_CHANGES_ICON));
					lblStatus.setToolTipText(formatLocalMessage(ServerStatus.CHANGES, serverMsg));
				}else if (serverStatus == ServerStatus.UPTODATE && localStatus == ServerStatus.UPTODATE) {
					lblStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_OK_ICON));
					lblStatus.setToolTipText(formatLocalMessage(ServerStatus.UPTODATE, null));
				}else {
					if (localStatus == ServerStatus.CONNECTING) {
						lblStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_DISABLED_ICON));	
					}else {
						lblStatus.setImage(ConnectPlugIn.getDefault().getImageRegistry().get(ConnectPlugIn.SERVER_ERROR_ICON));
					}
					
					String msg = serverMsg;
					if (msg == null) {
						msg = localMsg;
					}else if (localMsg != null){
						msg = msg + "\n" + localMsg; //$NON-NLS-1$
					}
					lblStatus.setToolTipText(formatLocalMessage(ServerStatus.ERROR, msg));
				}
				
			}
		});
	}
	private void updateLocalStatus(ConnectStatusManager.ServerStatus status, String message){
		if (status == null){
			updateLocalChanges.schedule(0);
		}else {
			localMsg = message;
			localStatus = status;
		}
		updateGui();
		
	}
	
	private Job updateLocalChanges = new Job(Messages.StatusLineControl_jobName){

		private int iso;
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String message = null;
			ServerStatus status = ServerStatus.ERROR;
			
			Boolean state = DerbyReplicationManager.INSTANCE.getCachedReplicationState();
			if (state == null) {
				status = ServerStatus.CONNECTING;
				message = "Replication state not known";
			}else if (!state) {
				status = ServerStatus.DISABLED;
				message = Messages.ReplicationStatusContribution_notenabled;
			}else {
				try(Session session = HibernateManager.openSession()){
					if (DerbyReplicationManager.INSTANCE.isReplicationEnabled(SmartDB.getCurrentConservationArea().getUuid(), session)){
						
						//set the transaction level so it doesn't interfere with other actions
						//we don't care if other action changes; this is just a status
						Boolean hasChanges = session.doReturningWork(new ReturningWork<Boolean>(){
							@Override
							public Boolean execute(Connection connection)
									throws SQLException {
								iso = connection.getTransactionIsolation();
								connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
								try{
									return DerbyReplicationManager.INSTANCE.hasLocalChanges(session);
								}catch (Exception ex){
									return false;
								}finally {
									//reset transaction level
									connection.setTransactionIsolation(iso);
									connection.commit();
								}
							}
						});
	
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
				}
			}
			updateLocalStatus(status, message);

			//schedule 
			if (state == null) {
				schedule(500);
			}else {
				schedule(ConnectStatusManager.CHECK_LOCAL_STATUS);
			}
			return Status.OK_STATUS;
		}	
	};
}
