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

import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ActiveShellExpression;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.connect.ui.server.DownloadChangeLogDialog;
import org.wcs.smart.connect.ui.server.DownloadChangeLogHandler;
import org.wcs.smart.connect.ui.server.SyncChangeLogDialog;
import org.wcs.smart.connect.ui.server.SyncChangeLogHandler;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * On startup sync processor.  Adds the required startup and
 * shutdown listeners to enable replication on startup
 * or shutdown as configured by the user.
 * 
 * @author Emily
 *
 */
public class SyncOnStartupProcessor {

	private @Inject IEventBroker broker;
	
	public SyncOnStartupProcessor() {
	}
	
	@Execute
	public void execute(){
		// add startup handler
		broker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				broker.unsubscribe(this);
				onStartUp();
			}
		});
		
		//add shutdown handler
		broker.subscribe(UIEvents.UILifeCycle.APP_SHUTDOWN_STARTED, new EventHandler(){
			@Override
			public void handleEvent(Event event) {
				broker.unsubscribe(this);
				onShutDown();
			}	
		});

	}

	private void onStartUp( ){
		
		CleanUpReplicationJob job = new CleanUpReplicationJob();
		job.schedule();
		
		if (!DerbyReplicationManager.INSTANCE.getLocalReplicationState()){
			return;
		}
		
		ConnectServer cs = null;
		Session s = HibernateManager.openSession();
		try{
			cs = ConnectHibernateManager.getConnectServer(s);
		}finally{
			s.close();
		}
		if (cs == null) return;
		
		if (!cs.getOptionAsBoolean(ConnectServerOption.Option.DOWNLOAD_ON_STARTUP)){
			return;
		}
		boolean upload = cs.getOptionAsBoolean(ConnectServerOption.Option.UPLOAD_ON_STARTUP);
	
		Shell activeShell = Display.getDefault().getActiveShell();
		if (promptSync(upload, activeShell)){
			//initiate sync now
			if (!upload){
				//download only
				(new DownloadChangeLogHandler()).execute(activeShell);
			}else{
				//sync
				(new SyncChangeLogHandler()).execute(activeShell);
			}
		}
	}

	private boolean promptSync(boolean upload, Shell activeShell){
		String title = "SMART Connect - Sync Changes";
		String message = "Do you want to sync all changes with SMART Connect now?";
		if (!upload){
			title = "SMART Connect - Download Change";
			message = "Do you want to download changes from SMART Connect now?";
		}
				
		if (MessageDialog.openQuestion(activeShell, title,message)){
			return true;
		}
		return false;
	}
	private void onShutDown(){
		if (!DerbyReplicationManager.INSTANCE.getLocalReplicationState()){
			return;
		}
		
		
		ConnectServer cs = null;
		Session s = HibernateManager.openSession();
		try{
			cs = ConnectHibernateManager.getConnectServer(s);
		}finally{
			s.close();
		}
		if (cs == null) return;
		
		if (!cs.getOptionAsBoolean(ConnectServerOption.Option.DOWNLOAD_ON_SHUTDOWN)){
			//no longer check on shutdown
			return;
		}
		
		boolean upload = cs.getOptionAsBoolean(ConnectServerOption.Option.UPLOAD_ON_SHUTDOWN);
		Shell activeShell = Display.getDefault().getActiveShell();
		if (promptSync(upload, activeShell)){
			runDownloadSync(upload, activeShell);
		}
	}
	
	private void runDownloadSync(final boolean upload, Shell activeShell){
		ConnectDialog dialog = new SyncChangeLogDialog(activeShell);
		if (!upload){
			dialog = new DownloadChangeLogDialog(activeShell);
		}
		
		if (dialog.open() != Window.OK) return;
		final SmartConnect connect = dialog.getConnection();
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try{
			pmd.run(true, false, new DownloadSynRunnable(connect, upload));
		}catch (Exception ex){
			ConnectPlugIn.displayLog(ex.getMessage(), ex);
		}
	}
	
	private class DownloadSynRunnable implements IRunnableWithProgress{
		
		private Boolean lockObject = new Boolean(false);
		private boolean upload;
		private SmartConnect connect;
		
		public DownloadSynRunnable(SmartConnect connect, boolean upload){
			this.upload = upload;
			this.connect = connect;
		}
		
		private void unlock(){
			synchronized (lockObject) {
				lockObject.notifyAll();
				lockObject = true;
			}	
		}
		
		private void lock(){
			synchronized (lockObject) {
				if (!lockObject){
					try {
						lockObject.wait();
					} catch (InterruptedException e) {
						ConnectPlugIn.log(e.getMessage(),e);
					}
				}
			}
		}
		
		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException,
				InterruptedException {
			
			if (upload){
				monitor.beginTask("Syncing changes with SMART Connect", 3);
			}else{
				monitor.beginTask("Downloading and applygin changes from SMART Connect", 3);
			}
			
			DownloadChangeLogEngine engine = new DownloadChangeLogEngine(connect) {
				protected void processComplete() {
					super.processComplete();
					if (upload && (record.getStatus() == Status.DONE || 
							record.getStatus() == Status.NODATA)){

						//upload
						UploadChangeLogEngine engine = new UploadChangeLogEngine(connect){
							protected void processComplete(){
								super.processComplete();
								unlock();
							}
						};
							
						try{
							engine.createUpload(monitor);
						}catch (final NothingToUpdateException ex){
							ConnectSyncHistoryRecord uptodate = new ConnectSyncHistoryRecord();
							uptodate.setStatus(Status.NODATA);
							unlock();
						}catch (Exception ex){
							ConnectPlugIn.displayLog(ex.getMessage(), ex);
							unlock();
						}
					}else{
						unlock();
					}
					
				}			
			};
			try {
				engine.downloadInstall();
				monitor.worked(1);
			} catch (Exception ex) {
				ConnectPlugIn.displayLog(ex.getMessage(), ex);
			}

			lock();
		}
		
	}
}
