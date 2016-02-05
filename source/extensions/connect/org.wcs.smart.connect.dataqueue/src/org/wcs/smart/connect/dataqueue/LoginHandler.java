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
package org.wcs.smart.connect.dataqueue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ILoginHandler;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.internal.process.AutoProcessingManager;
import org.wcs.smart.connect.dataqueue.internal.process.DataQueueManager;
import org.wcs.smart.connect.dataqueue.internal.process.ProcessorManager;
import org.wcs.smart.connect.dataqueue.internal.ui.DataQueueServerDialog;
import org.wcs.smart.connect.dataqueue.model.DataQueueServerOptions;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Login handler for smart connect data queue.  This will clean up the queue
 * and restart processing if necessary.
 * @author Emily
 *
 */
public class LoginHandler implements ILoginHandler {

	@Override
	public void onLogin() throws Exception {
		
		//cleanup tasks
		//this needs to reset any tasks that with a status of processing or downloading to QUEUED (to start again)
		//we either need to auto restart the processing or prompt the user to finish processing items
		resetDataQueue();

		//cleanup history 
		cleanUpDataQueue();

		//configure auto startup
		AutoProcessingManager.INSTANCE.onStartUp();
	}

	/*
	 * checks for items that are current processing, queued, or downloading
	 * if there are any it resets them to queued and starts the processing again
	 */
	//TODO: this is not working yet
	private void resetDataQueue(){
		
		List<LocalDataQueueItem> itemsToReset = DataQueueManager.INSTANCE.getLocalItems(new LocalDataQueueItem.Status[]{
				LocalDataQueueItem.Status.DOWNLOADING,
				LocalDataQueueItem.Status.PROCESSING,
				LocalDataQueueItem.Status.QUEUED
		});
		if (itemsToReset.isEmpty()) return;

		//update item status and clear any non complete downloaded files
		List<Path> toDelete = new ArrayList<Path>();
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			int order = 0;
			for (LocalDataQueueItem i : itemsToReset){
				if (i.getStatus() == LocalDataQueueItem.Status.DOWNLOADING){
					if (i.getFullFilePath() != null){
						toDelete.add(i.getFullFilePath());
					}
				}
				i.setStatus(LocalDataQueueItem.Status.QUEUED);
				i.setOrder(order++);
				s.saveOrUpdate(i);
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			ConnectDataQueuePlugin.log(ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		for (Path p : toDelete){
			try{
				Files.deleteIfExists(p);
			}catch (Exception ex){
				ConnectDataQueuePlugin.log("Error deleting file " + p.toString(), ex);
			}
		}
			
		ConnectServer server = null;
		ConnectUser user = null;
		SmartConnect smartConnect = null;
		s = HibernateManager.openSession();
		try{
			server = ConnectHibernateManager.getConnectServer(s);
			user = ConnectHibernateManager.getConnectUser(SmartDB.getCurrentEmployee(), s);
		}finally{
			s.close();
		}
		
		if (server == null){
			return ;
		}
		if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
				"Data Queue Processor", 
				"There are unfinished items in the data processing queue from previous application launch.  Do you want to process these now?")){
			return ;
		}
		
		if (user == null || user.getConnectPassword() == null || user.getConnectUsername() == null){
			DataQueueServerDialog dialog = new DataQueueServerDialog(Display.getDefault().getActiveShell());
			if (dialog.open() == Window.OK){
				smartConnect = dialog.getConnection();
			}else{
				return;
			}
		}else{
			try {
				smartConnect = SmartConnect.findInstance(server, user.getConnectUsername(), ConnectPlugIn.decryptPassword(user));
			} catch (Exception e) {
				ConnectDataQueuePlugin.log(e.getMessage(), e);
				return;
			}
		}
		ProcessorManager.INSTANCE.processDataQueue(smartConnect);
	}
	
	/*
	 * cleansup data queue items that are old
	 */
	private void cleanUpDataQueue(){
		
		int olderThan = -1;
		Session s = HibernateManager.openSession();
		try{
			ConnectServer cs = ConnectHibernateManager.getConnectServer(s);
			if (cs == null) return;
			olderThan = DataQueueServerOptions.CLEANUP_DAYS.getIntegerValue(cs);
		}finally{
			s.close();
		}
		
		if (olderThan >= 0){
			final int days = olderThan;
			Job j = new Job("data queue history cleaner"){
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					DataQueueManager.INSTANCE.deleteOldItems(days);
					return Status.OK_STATUS;
				}
			};
			j.schedule();
		}
			
	}
}
