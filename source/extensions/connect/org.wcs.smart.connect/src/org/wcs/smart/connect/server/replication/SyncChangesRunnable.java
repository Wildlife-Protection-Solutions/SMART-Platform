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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;

/**
 * Progress monitored runnable that downloads changes from a connect 
 * instance then uploads changes for a given conservation area.
 * 
 * Will not finish until entire process is complete.  Once complete
 * the getStatus() function will return a string describing any errors that
 * may have occurred during the process.  
 * 
 * @author Emily
 *
 */
public class SyncChangesRunnable implements IRunnableWithProgress{
	
	private Boolean lockObject = new Boolean(false);
	private boolean upload;
	private SmartConnect connect;
	private ConservationArea ca;
	
	private ConnectSyncHistoryRecord downloadRecord;
	private ConnectSyncHistoryRecord uploadRecord;
	private Exception thrownException;
	
	public SyncChangesRunnable(SmartConnect connect, ConservationArea ca, boolean upload){
		this.upload = upload;
		this.connect = connect;
		this.ca = ca;
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
			monitor.beginTask(Messages.SyncChangesRunnable_SyncTaskName, 3);
		}else{
			monitor.beginTask(Messages.SyncChangesRunnable_DownloadTaskName, 3);
		}
		
		DownloadChangeLogEngine engine = new DownloadChangeLogEngine(ca, connect) {
			protected void processComplete() {
				downloadRecord = record;
				super.processComplete();
				if (upload && (record.getStatus() == Status.DONE || 
						record.getStatus() == Status.NODATA)){

					//upload
					UploadChangeLogEngine engine = new UploadChangeLogEngine(ca, connect){
						protected void processComplete(){
							uploadRecord = record;
							super.processComplete();
							unlock();
						}
					};
						
					try{
						engine.createUpload(monitor);
					}catch (final NothingToUpdateException ex){
						ConnectSyncHistoryRecord uptodate = new ConnectSyncHistoryRecord();
						uptodate.setStatus(Status.NODATA);
						uploadRecord = uptodate;
						unlock();
					}catch (Exception ex){
						thrownException = ex;
						ConnectPlugIn.log(ex.getMessage(), ex);
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
			lock();
		} catch (Exception ex) {
			thrownException = ex;
			ConnectPlugIn.log(ex.getMessage(), ex);
		}
	}
	
	/**
	 * The upload/download may fail without an exception being thrown here.  In this
	 * case the status record will be updated and getStatus will return
	 * an appropriate error message.
	 * 
	 * @return any exception thrown during the process
	 */
	public Exception getThrownException(){
		return thrownException;
	}
	
	/**
	 * 
	 * @return null if both upload and download sync records completed ok
	 * otherwise returns error message representing what went wrong
	 */
	public String getStatus(){
		if (thrownException != null){
			return thrownException.getMessage();
		}
		if (downloadRecord == null){
			return Messages.SyncChangesRunnable_UnknownError;
		}
		if (downloadRecord.getStatus() == Status.ACTIVE){
			return Messages.SyncChangesRunnable_DownloadStillWorking;
		}
		if (downloadRecord.getStatus() == Status.ERROR){
			return Messages.SyncChangesRunnable_DownloadError + downloadRecord.getErrorString();
		}
		//download is either ok or nodata which are all fine; lets check upload
		if (!upload){
			return null;
		}
		if (uploadRecord == null){
			return Messages.SyncChangesRunnable_UploadUnknownError;
		}
		if (uploadRecord.getStatus() == Status.ACTIVE){
			return Messages.SyncChangesRunnable_UploadStillActive;
		}
		if (uploadRecord.getStatus() == Status.ERROR){
			return Messages.SyncChangesRunnable_UploadError + uploadRecord.getErrorString();
		}
		//upload is either ok or nodata which are all file; return null;
		return null;
	}

}
