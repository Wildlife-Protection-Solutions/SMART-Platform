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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.internal.Messages;
import org.wcs.smart.connect.dataqueue.internal.server.ConnectDataQueue;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Job to update data queue status.
 * 
 * @author Emily
 *
 */
public class DataQueueStatusJob extends Job {

	private SmartConnect smartConnect;
		
	/**
	 * Updates the status of the data queue.  This runs only once 
	 */
	public DataQueueStatusJob() {
		super(Messages.DataQueueStatusJob_jobname);
	}

	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.PROCESSING, null);
				
		ConnectServer server = null;
		ConnectUser user = null;
		try(Session s = HibernateManager.openSession()){
			server = ConnectHibernateManager.getConnectServer(s);
			user = ConnectHibernateManager.getConnectUser(SmartDB.getCurrentEmployee(), s);
		}
		
		
		if (user == null || user.getConnectPassword() == null || user.getConnectUsername() == null){		
			AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.ERROR, Messages.DataQueueStatusJob_connectCommunicationError);
			return Status.OK_STATUS;
		
		}else{
			try {
				smartConnect = SmartConnect.findInstance(server, user.getConnectUsername(), ConnectPlugIn.decryptPassword(user));
			} catch (Exception e) {
				AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.ERROR, Messages.DataQueueStatusJob_connectCommunicationError);
				return Status.OK_STATUS;
			}
		}
		
		List<DataQueueItem> serverItems = null;
		try{
			serverItems = ConnectDataQueue.INSTANCE.getQueuedItems(smartConnect, SmartDB.getCurrentConservationArea());
		}catch (Exception ex){
			AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.ERROR, Messages.DataQueueStatusJob_connectCommunicationError);
			return Status.OK_STATUS;
		}
		
		if (serverItems.isEmpty()) {
			AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.OK, null);
		}else {
			AutoProcessingManager.INSTANCE.updateLastStatus(AutoProcessingStatus.Status.WARNING, null);
		}
		
		schedule(60*1000);
		return Status.OK_STATUS;
	}
	
	
}
