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
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.ConnectStatusManager;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaProxy;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.model.ConnectServerOption.Option;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.ui.server.DownloadChangeLogDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Job to manage auto replication of data to/from the smart
 * connect server.  Only one instance of this job should
 * be created/run.
 * 
 * @author Emily
 *
 */
public class AutoReplicationJob extends Job {

	private SmartConnect smartConnect;
	private Long millisecondsToRepeat = -1l;
	private boolean reschedule = true;
	
	public AutoReplicationJob() {
		super("Backgound Replication Mananger");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try{
			return runInternal(monitor);
		}finally{
			if (reschedule){
				reschedule();
			}
		}
	}

	private IStatus runInternal(IProgressMonitor monitor){
		System.out.println("AUTO REPLICATING");
		reschedule = true;
		monitor.beginTask("Auto updating Connect server status", 3);
		setServerStatus(ConnectStatusManager.ServerStatus.CONNECTING, null);
		
		monitor.subTask("loading server information");
		Session s = HibernateManager.openSession();
		ConnectServer server = null;
		ConnectUser user = null;
		ConnectServerStatus serverStatus = null;
		try{
			server = ConnectHibernateManager.getConnectServer(s);
			serverStatus = ConnectHibernateManager.getConnectServerStatus(s);
			user = ConnectHibernateManager.getConnectUser(SmartDB.getCurrentEmployee(), s);
		}finally{
			s.close();
		}
		if (server != null) millisecondsToRepeat = server.getOptionAsInt(Option.SYNC_MINUTE) * 60 * 1000l;
		if (server == null || serverStatus == null){
			setServerStatus(ConnectStatusManager.ServerStatus.ERROR, "Connect server not configured.");
			return Status.OK_STATUS;
		}
		
		if (!server.getOptionAsBoolean(ConnectServerOption.Option.SYNC_AUTOMATICALLY)){
			reschedule = false;
			setServerStatus(ConnectStatusManager.ServerStatus.ERROR, "Auto updates not configured.");
			return Status.OK_STATUS;
		}
		
		if (user == null || user.getConnectPassword() == null || user.getConnectUsername() == null){
			if (!server.getOptionAsBoolean(Option.SYNC_PROMPT_PASSWORD)){
				setServerStatus(ConnectStatusManager.ServerStatus.ERROR, "Could not connect, user credentials not provided and configuration not set to prompt.");
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
				setServerStatus(ConnectStatusManager.ServerStatus.ERROR, "Could not connect, user credentials not valid.");
				return Status.OK_STATUS;
			}
		}else{
			try {
				smartConnect = SmartConnect.findInstance(server, user.getConnectUsername(), ConnectPlugIn.decryptPassword(user));
			} catch (Exception e) {
				setServerStatus(ConnectStatusManager.ServerStatus.ERROR, "Could not connect, user credentials not valid.");
				return Status.OK_STATUS;
			}
		}
		monitor.worked(1);
		
		//connect to server and determine if there are changes
		monitor.subTask("getting server state");
		ConservationAreaProxy caInfo = null;
		try{
			caInfo = smartConnect.getCaInfo(server.getConservationArea().getUuid());
		}catch (Exception ex){
			ConnectPlugIn.log(ex.getMessage(), ex);
			setServerStatus(ConnectStatusManager.ServerStatus.ERROR, "unable to communicate with server");
			return Status.OK_STATUS;
		}
		monitor.worked(1);
		
		if (!caInfo.getVersion().equals(serverStatus.getVersion())){
			setServerStatus(ConnectStatusManager.ServerStatus.ERROR, "base versions do not match");
			return Status.OK_STATUS;
		}
		
		boolean needsToDownload = false;
		if (caInfo.getRevision() <= serverStatus.getServerRevision()){
			setServerStatus(ConnectStatusManager.ServerStatus.UPTODATE, "local copy up to date");
		}else{
			setServerStatus(ConnectStatusManager.ServerStatus.CHANGES, null);
			needsToDownload = true;
		}
		
		
		//if auto download then lets start that process if not already started
		if (!server.getOptionAsBoolean(Option.SYNC_DOWNLOAD)){
			return Status.OK_STATUS;
		}
		
		final boolean upload = server.getOptionAsBoolean(Option.SYNC_AUTO_UPLOAD);
		if (needsToDownload){
			monitor.subTask("initiating download process");
			downloadChangeLog(upload);
		}else if (!needsToDownload && upload){
			monitor.subTask("initiating upload process");
			uploadChangeLog();
		}
		monitor.done();
		return Status.OK_STATUS;
	}
	
	private void downloadChangeLog(final boolean uploadOnComplete){
		DownloadChangeLogEngine engine = new DownloadChangeLogEngine(smartConnect){
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
			ConnectPlugIn.displayLog("Auto Download Changes from Connect Error: " + ex.getMessage(), ex);
			setServerStatus(ConnectStatusManager.ServerStatus.ERROR, ex.getMessage());
		}
	}
	
	private void uploadChangeLog(){
		reschedule = false;	//we will reschedule after this process is completed
		Job j = new Job("package change log") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				UploadChangeLogEngine engine = new UploadChangeLogEngine(smartConnect){
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
					ConnectPlugIn.displayLog("Auto Upload Changes to Connect Error: " + ex.getMessage(), ex);
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
		ConnectStatusManager.INSTANCE.statusModified(status, message);
	}
}
