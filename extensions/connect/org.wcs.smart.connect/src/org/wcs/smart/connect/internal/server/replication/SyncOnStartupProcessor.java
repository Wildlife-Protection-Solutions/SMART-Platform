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
package org.wcs.smart.connect.internal.server.replication;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.connect.ui.server.DownloadChangeLogDialog;
import org.wcs.smart.connect.ui.server.DownloadChangeLogHandler;
import org.wcs.smart.connect.ui.server.SyncChangeLogDialog;
import org.wcs.smart.connect.ui.server.SyncChangeLogHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

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
		
		ConnectServer cs = null;
		Session s = HibernateManager.openSession();
		try{
			if (!DerbyReplicationManager.INSTANCE.isReplicationEnabled(SmartDB.getCurrentConservationArea().getUuid(), s)){
				//not replicating therefore nothing to do
				return;
			}
			cs = ConnectHibernateManager.getConnectServer(s);
		}finally{
			s.close();
		}
		if (cs == null) return;
		
		if (!ConnectServerOption.ConnectionOption.DOWNLOAD_ON_STARTUP.getBooleanValue(cs)){
			return;
		}
		boolean upload = ConnectServerOption.ConnectionOption.UPLOAD_ON_STARTUP.getBooleanValue(cs);
	
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
		String title = Messages.SyncOnStartupProcessor_Title;
		String message = Messages.SyncOnStartupProcessor_Message;
		if (!upload){
			title = Messages.SyncOnStartupProcessor_DownTitle;
			message = Messages.SyncOnStartupProcessor_DownMessage;
		}
				
		if (MessageDialog.openQuestion(activeShell, title,message)){
			return true;
		}
		return false;
	}
	
	private void onShutDown(){
		
		ConnectServer cs = null;
		Session s = HibernateManager.openSession();
		try{
			if (!DerbyReplicationManager.INSTANCE.isReplicationEnabled(SmartDB.getCurrentConservationArea().getUuid(), s)){
				//not replicating therefore nothing to do
				return;
			}
			cs = ConnectHibernateManager.getConnectServer(s);
		}finally{
			s.close();
		}
		if (cs == null) return;
		
		if (!ConnectServerOption.ConnectionOption.DOWNLOAD_ON_SHUTDOWN.getBooleanValue(cs)){
			//no longer check on shutdown
			return;
		}
		
		boolean upload = ConnectServerOption.ConnectionOption.UPLOAD_ON_SHUTDOWN.getBooleanValue(cs);
		Shell activeShell = Display.getDefault().getActiveShell();
		if (promptSync(upload, activeShell)){
			runDownloadSync(upload, SmartDB.getCurrentConservationArea(), activeShell);
		}
	}
	
	private void runDownloadSync(final boolean upload, ConservationArea ca, Shell activeShell){
		ConnectDialog dialog = new SyncChangeLogDialog(activeShell);
		if (!upload){
			dialog = new DownloadChangeLogDialog(activeShell);
		}
		
		if (dialog.open() != Window.OK) return;
		final SmartConnect connect = dialog.getConnection();
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try{
			SyncChangesRunnable runnable = new SyncChangesRunnable(connect, ca, upload);
			pmd.run(true, false, runnable);
			//throw any exceptions generated while running
			if (runnable.getThrownException() != null){
				throw runnable.getThrownException();
			}
		}catch (Exception ex){
			ConnectPlugIn.displayLog(ex.getMessage(), ex);
		}
	}
	
}
