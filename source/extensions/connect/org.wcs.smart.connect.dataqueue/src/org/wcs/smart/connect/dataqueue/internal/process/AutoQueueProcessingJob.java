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
package org.wcs.smart.connect.dataqueue.internal.process;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.internal.Messages;
import org.wcs.smart.connect.dataqueue.internal.server.ConnectDataQueue;
import org.wcs.smart.connect.dataqueue.internal.ui.DataQueueServerDialog;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.DataQueueServerOptions;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Job to automate data queue processing.  This job downloads queue items
 * from the server, adds them to the local data queue and initiates the processing
 * jobs.  Does not do processing directly.
 * 
 * @author Emily
 *
 */
public class AutoQueueProcessingJob extends Job {

	private SmartConnect smartConnect;
	
	private boolean reschedule = true;
	private long millisecondsToReschedule = -1l;
	private boolean runOnce;
	
	/**
	 * Creates a new auto replication job that will run at
	 * the configured reschedule period.
	 * 
	 * Only one instance of this type of job should be created.
	 */
	AutoQueueProcessingJob() {
		this(false);
	}

	/**
	 * Creates a new auto replication job that will run once if runOnce
	 * is <code>true</code>.
	 * 
	 * When running once this will not reschedule after the the job is completed
	 * and it uses the DataQueue STARTUP configuration parameters instead of the 
	 * DataQueue AUTO configuration parameters. 
	 */
	AutoQueueProcessingJob(boolean runOnce) {
		super(Messages.AutoQueueProcessingJob_JobName);
		this.runOnce = runOnce;
	}

	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try{
			return runInternal(monitor);
		}finally{
			if (!runOnce && reschedule){
				reschedule();
			}
		}
	}

	private IStatus runInternal(IProgressMonitor monitor){
		AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.PROCESSING, null);
		reschedule = true;
		monitor.beginTask(Messages.AutoQueueProcessingJob_Task1, 3);
				
		monitor.subTask(Messages.AutoQueueProcessingJob_Task2);
		ConnectServer server = null;
		ConnectUser user = null;
		
		Session s = HibernateManager.openSession();
		try{
			server = ConnectHibernateManager.getConnectServer(s);
			user = ConnectHibernateManager.getConnectUser(SmartDB.getCurrentEmployee(), s);
		}finally{
			s.close();
		}
		
		if (server == null){
			reschedule = false;
			AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.ERROR, Messages.AutoQueueProcessingJob_Status1);
			return Status.OK_STATUS;
		}
		Boolean opSchedule = DataQueueServerOptions.AUTO_CHECK.getBooleanValue(server);
		if (!opSchedule){
			reschedule = false;
			if (!runOnce){
				AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.INACTIVE, Messages.AutoQueueProcessingJob_Status2);
				return Status.OK_STATUS;
			}
		}else{
			millisecondsToReschedule = DataQueueServerOptions.AUTO_MINUTES.getIntegerValue(server) * 60 * 1000l;
		}
		
		if (user == null || user.getConnectPassword() == null || user.getConnectUsername() == null){
			if (!DataQueueServerOptions.PROMPT_USER.getBooleanValue(server)){
				AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.WARNING, Messages.AutoQueueProcessingJob_Status3);
				return Status.OK_STATUS;
			}
			//prompt user
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					DataQueueServerDialog dialog = new DataQueueServerDialog(Display.getDefault().getActiveShell());
					if (dialog.open() == Window.OK){
						smartConnect = dialog.getConnection();
					}
					
				}	
			});
			if (smartConnect == null){
				AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.WARNING, Messages.AutoQueueProcessingJob_Status4);
				return Status.OK_STATUS;
			}
		}else{
			try {
				smartConnect = SmartConnect.findInstance(server, user.getConnectUsername(), ConnectPlugIn.decryptPassword(user));
			} catch (Exception e) {
				ConnectDataQueuePlugin.log("Could not configure SMART Connect Server connection", e); //$NON-NLS-1$
				AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.ERROR, Messages.AutoQueueProcessingJob_Status5);
				return Status.OK_STATUS;
			}
		}
		monitor.worked(1);
		
		//connect to server and determine if there is any work to do
		monitor.subTask(Messages.AutoQueueProcessingJob_Task3);

		List<DataQueueItem> serverItems = null;
		try{
			serverItems = ConnectDataQueue.INSTANCE.getQueuedItems(smartConnect, SmartDB.getCurrentConservationArea());
		}catch (Exception ex){
			ConnectDataQueuePlugin.log("Could not download data queue items from SMART Connect Server.", ex); //$NON-NLS-1$
			AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.ERROR, Messages.AutoQueueProcessingJob_Status6);
			return Status.OK_STATUS;
		}
		try{
			List<LocalDataQueueItem> localItems = DataQueueManager.INSTANCE.getLocalItems(
					LocalDataQueueItem.Status.QUEUED, LocalDataQueueItem.Status.REQUEUED, 
					LocalDataQueueItem.Status.PROCESSING, LocalDataQueueItem.Status.DOWNLOADING);
	
			//remove any items from the serverItems that are in the local Items
			//these have been queued and we do not need to requeue them
			for (LocalDataQueueItem i : localItems){
				for (DataQueueItem serverItem : serverItems){
					if (serverItem.getUuid().equals(i.getServerItemUuid())){
						serverItems.remove(server);
						break;
					}
				}
			}
			
			if (serverItems.isEmpty()){
				AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.OK, null);
				return Status.OK_STATUS;
			}
		
			if ((!runOnce && DataQueueServerOptions.AUTO_PROMPT.getBooleanValue(server)) || 
				(runOnce && DataQueueServerOptions.STARTUP_PROMPT.getBooleanValue(server))){
				//prompt
				boolean[] cont = new boolean[]{false};
				final String message = MessageFormat.format(Messages.AutoQueueProcessingJob_ProcessNow, serverItems.size());
				Display.getDefault().syncExec(new Runnable(){

					@Override
					public void run() {
						if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(), Messages.AutoQueueProcessingJob_DialogTitle, message)){
							cont[0] = true;
						}
					}
					
				});
				if (!cont[0]){
					//user has decided not to process now
					AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.OK, null);
					return Status.OK_STATUS;
				}
			}else if ((!runOnce && !DataQueueServerOptions.AUTO_AUTOPROCESS.getBooleanValue(server)) || 
					(runOnce && !DataQueueServerOptions.STARTUP_AUTOPROCESS.getBooleanValue(server))){
				//should not happen
				return Status.OK_STATUS;
			}
			
			//else add these to the local store and start the processor
			for (DataQueueItem item : serverItems){
				try{
					DataQueueManager.INSTANCE.addItemToQueue(item);
				}catch (Exception ex){
					ConnectDataQueuePlugin.displayLog(Messages.AutoQueueProcessingJob_Error1 + ex.getMessage(), ex);
				}
			}
			ProcessorManager.INSTANCE.processDataQueue(smartConnect);
		}catch (Exception ex){
			ConnectDataQueuePlugin.log("Failed to complete auto processing of SMART Connect data queue.", ex); //$NON-NLS-1$
			AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.ERROR, Messages.AutoQueueProcessingJob_Status7);
		}
			
		//reschedule - or only reschedule when processing done??
		monitor.done();
		AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.OK, null);
		return Status.OK_STATUS;
	}
	
	private void reschedule(){
		//reschedule job
		if (millisecondsToReschedule >= 0 ){
			this.schedule(millisecondsToReschedule);
		}
	}
}
