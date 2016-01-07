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
package org.wcs.smart.connect.server.replication;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.ConnectStatusManager;
import org.wcs.smart.connect.ConnectStatusManager.ServerStatus;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaProxy;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.model.ConnectServerOption.Option;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.connect.ui.server.DownloadChangeLogDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Job to manage auto replication of data to/from the smart
 * connect server. 
 * This replicates data for the current Conservation Area only.
 * @author Emily
 *
 */
public class AutoReplicationJob extends Job {

	private SmartConnect smartConnect;
	private Long millisecondsToRepeat = -1l;
	private boolean reschedule = true;
	private ConservationArea caToReplicate = null;
	private boolean statusOnly;
	
	/**
	 * Creates a new auto replication job that will run at
	 * the configured reschedule period.
	 * 
	 * Only one instance of this type of job should be created.
	 */
	public AutoReplicationJob() {
		this(false);
	}

	/**
	 * Creates a new auto replication job that runs once and 
	 * only updates the local status.
	 * This is not rescheduled nor does it update any data
	 * 
	 * These can be created and run on demand.
	 */
	public AutoReplicationJob(boolean statusOnly) {
		super(Messages.AutoReplicationJob_jobname);
		this.statusOnly = statusOnly;
		this.caToReplicate = SmartDB.getCurrentConservationArea();
	}

	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try{
			return runInternal(monitor);
		}finally{
			if (!statusOnly && reschedule){
				reschedule();
			}
		}
	}

	private IStatus runInternal(IProgressMonitor monitor){
		reschedule = true;
		monitor.beginTask(Messages.AutoReplicationJob_TaskName, 3);
		setServerStatus(ConnectStatusManager.ServerStatus.CONNECTING, null);
		
		monitor.subTask(Messages.AutoReplicationJob_ServerSubTaskName);
		ConnectServer server = null;
		ConnectUser user = null;
		ConnectServerStatus serverStatus = null;
		
		Session s = HibernateManager.openSession();
		try{
			if (!DerbyReplicationManager.INSTANCE.isReplicationEnabled(caToReplicate.getUuid(), s)){
				setServerStatus(ServerStatus.ERROR, Messages.AutoReplicationJob_ReplicationNoEnabledError);
				return Status.OK_STATUS;
			}
			
			server = ConnectHibernateManager.getConnectServer(s);
			serverStatus = ConnectHibernateManager.getConnectServerStatus(s);
			user = ConnectHibernateManager.getConnectUser(SmartDB.getCurrentEmployee(), s);
		}finally{
			s.close();
		}
		if (server != null) millisecondsToRepeat = server.getOptionAsInt(Option.SYNC_MINUTE) * 60 * 1000l;
		if (server == null || serverStatus == null){
			setServerStatus(ConnectStatusManager.ServerStatus.ERROR, Messages.AutoReplicationJob_ServerError);
			return Status.OK_STATUS;
		}
		
		if (!statusOnly && !server.getOptionAsBoolean(ConnectServerOption.Option.SYNC_AUTOMATICALLY)){
			reschedule = false;
			setServerStatus(ConnectStatusManager.ServerStatus.ERROR, Messages.AutoReplicationJob_AutoConfigError);
			return Status.OK_STATUS;
		}
		
		if (!statusOnly && (user == null || user.getConnectPassword() == null || user.getConnectUsername() == null)){
			if (!server.getOptionAsBoolean(Option.SYNC_PROMPT_PASSWORD)){
				setServerStatus(ConnectStatusManager.ServerStatus.ERROR, Messages.AutoReplicationJob_NoCredentialsError);
				return Status.OK_STATUS;
			}
			//prompt user
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					DownloadChangeLogDialog dialog = new DownloadChangeLogDialog(Display.getDefault().getActiveShell());
					if (dialog.open() == Window.OK){
						smartConnect = dialog.getConnection();
					}
					
				}	
			});
			if (smartConnect == null){
				setServerStatus(ConnectStatusManager.ServerStatus.ERROR, Messages.AutoReplicationJob_InvalidCredentialsError);
				return Status.OK_STATUS;
			}
		}else{
			try {
				smartConnect = SmartConnect.findInstance(server, user.getConnectUsername(), ConnectPlugIn.decryptPassword(user));
			} catch (Exception e) {
				setServerStatus(ConnectStatusManager.ServerStatus.ERROR, Messages.AutoReplicationJob_InvalidCredentialsError);
				return Status.OK_STATUS;
			}
		}
		monitor.worked(1);
		
		//connect to server and determine if there are changes
		monitor.subTask(Messages.AutoReplicationJob_LoadingServerSubTask);
		ConservationAreaProxy caInfo = null;
		try{
			caInfo = smartConnect.getCaInfo(server.getConservationArea().getUuid());
		}catch (Exception ex){
			ConnectPlugIn.log(ex.getMessage(), ex);
			setServerStatus(ConnectStatusManager.ServerStatus.ERROR, Messages.AutoReplicationJob_CommunicationError);
			return Status.OK_STATUS;
		}
		monitor.worked(1);
		if (caInfo == null){
			setServerStatus(ConnectStatusManager.ServerStatus.ERROR, Messages.AutoReplicationJob_CaDoesNotExistError);
			return Status.OK_STATUS;
		}
		if (!caInfo.getVersion().equals(serverStatus.getVersion())){
			setServerStatus(ConnectStatusManager.ServerStatus.ERROR, Messages.AutoReplicationJob_VersionsDoNoMatchError);
			return Status.OK_STATUS;
		}
		
		boolean needsToDownload = false;
		if (caInfo.getRevision() <= serverStatus.getServerRevision()){
			setServerStatus(ConnectStatusManager.ServerStatus.UPTODATE, Messages.AutoReplicationJob_UpToDateError);
		}else{
			setServerStatus(ConnectStatusManager.ServerStatus.CHANGES, null);
			needsToDownload = true;
		}
		
		if (statusOnly) return Status.OK_STATUS;
		
		//if auto download then lets start that process if not already started
		if (!server.getOptionAsBoolean(Option.SYNC_DOWNLOAD)){
			return Status.OK_STATUS;
		}
		
		final boolean upload = server.getOptionAsBoolean(Option.SYNC_AUTO_UPLOAD);
		if (needsToDownload){
			monitor.subTask(Messages.AutoReplicationJob_downloadSubTaskName);
			downloadChangeLog(upload);
		}else if (!needsToDownload && upload){
			monitor.subTask(Messages.AutoReplicationJob_uploadSubTaskName);
			uploadChangeLog();
		}
		monitor.done();
		return Status.OK_STATUS;
	}
	
	private void downloadChangeLog(final boolean uploadOnComplete){
		DownloadChangeLogEngine engine = new DownloadChangeLogEngine(SmartDB.getCurrentConservationArea(), smartConnect){
			protected void processComplete(){
				super.processComplete();

				if (uploadOnComplete){
					uploadChangeLog();
				}else{
					//done all the work reschedule the job
					reschedule();
				}
			}
		};
		try{
			engine.downloadInstall();
			reschedule = false;	//we will reschedule after this process is completed
		}catch(Exception ex){
			ConnectPlugIn.displayLog(Messages.AutoReplicationJob_AutoDownloadError + ex.getMessage(), ex);
			setServerStatus(ConnectStatusManager.ServerStatus.ERROR, ex.getMessage());
		}
	}
	
	private void uploadChangeLog(){
		reschedule = false;	//we will reschedule after this process is completed
		Job j = new Job(Messages.AutoReplicationJob_packJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				UploadChangeLogEngine engine = new UploadChangeLogEngine(SmartDB.getCurrentConservationArea(), smartConnect){
					protected void processComplete(){
						super.processComplete();
						reschedule();
					}
				};
				try{
					engine.createUpload(monitor);
				}catch(NothingToUpdateException ex){
					reschedule();
				}catch (Exception ex){
					reschedule();
					ConnectPlugIn.displayLog(Messages.AutoReplicationJob_AutoUploadError + ex.getMessage(), ex);
				}
				return Status.OK_STATUS;
			}
		};
		j.schedule();		
	}

	private void reschedule(){
		//reschedule job
		if (millisecondsToRepeat >= 0 ){
			AutoReplicationJob.this.schedule(millisecondsToRepeat);
		}
	}
	
	private void setServerStatus(ConnectStatusManager.ServerStatus status, String message){
		ConnectStatusManager.INSTANCE.serverStatusModified(status, message);
	}
}
