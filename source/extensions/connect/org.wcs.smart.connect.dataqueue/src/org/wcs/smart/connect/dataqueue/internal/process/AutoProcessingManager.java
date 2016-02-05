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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.WorkbenchJob;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.dataqueue.model.DataQueueServerOptions;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Manager for auto data queue processing.  Responsible for starting auto processing
 * job as required.
 * @author Emily
 *
 */
public enum AutoProcessingManager {

	INSTANCE;
	
	//single instance of auto replication job
	private AutoQueueProcessingJob autoReplication = new AutoQueueProcessingJob();
	private AutoProcessingStatus lastStatus = new AutoProcessingStatus();
	private List<IProcessingStatusListener> listeners = new ArrayList<IProcessingStatusListener>();
	
	public void onStartUp(){
		Session s = HibernateManager.openSession();
		try{
			ConnectServer cs = ConnectHibernateManager.getConnectServer(s);
			if (cs == null) return;
			
			if (DataQueueServerOptions.AUTO_CHECK.getBooleanValue(cs)){
				int delay = DataQueueServerOptions.AUTO_MINUTES.getIntegerValue(cs);
				enableAutoProcessing(delay);
			}else{
				lastStatus.updateStatus(AutoProcessingStatus.Status.INACTIVE, "auto data queue processing not enabled");
			}

			if (DataQueueServerOptions.CHECK_ONSTARTUP.getBooleanValue(cs)){
				WorkbenchJob wj = new WorkbenchJob("data queue startup check") {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						runOnce();
						return Status.OK_STATUS;
					}
				};
				wj.schedule();
				
			}
		}finally{
			s.close();
		}
	}
	
	/**
	 * Runs the auto process to download items from the server and start
	 * processing once.  Is not rescheduled.
	 */
	private void runOnce(){
		(new AutoQueueProcessingJob(true)).schedule();
	}
	
	/**
	 * Starts the background auto replication job after the given
	 * delay; if it's already scheduled this has no effect
	 * 
	 * @param delayMinutes delay in mintues
	 */
	public void enableAutoProcessing(int delayMinutes){
		lastStatus.updateStatus(AutoProcessingStatus.Status.OK, null);
		statusModified();
		long delaysec = delayMinutes * 60 * 1000l;
		autoReplication.schedule(delaysec);
	}
	
	public void updateLastStatus(AutoProcessingStatus.Status status, String message){
		lastStatus.updateStatus(status, message);
		statusModified();
	}
	public AutoProcessingStatus getLastStatus(){
		return lastStatus;
	}
	
	public void addStatusListener(IProcessingStatusListener listener){
		this.listeners.add(listener);
	}
	public void removeStatusListener(IProcessingStatusListener listener){
		this.listeners.remove(listener);
	}
	private void statusModified(){
		for (IProcessingStatusListener l : listeners){
			l.statusModified(lastStatus);
		}
	}
	
	public static interface IProcessingStatusListener{
		public void statusModified(AutoProcessingStatus lastStatus);
	}
}
