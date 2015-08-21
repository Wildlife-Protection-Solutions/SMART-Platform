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
package org.wcs.smart.connect.server;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.UploadStatus;
import org.wcs.smart.connect.api.model.UploadStatus.Status;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job to upload conservation area export to connect server
 * and monitor results until complete.
 * 
 * @author Emily
 *
 */
public class UploadCaJob extends Job {

	public static final int MAX_RETRY = 100;
	public static final int STATUS_WAIT_TIME = 1000; //1 sec
	public static final long MAX_WAIT = 5 * 60 * 1000000000l;  //5 minutes in nano seconds
	
	private SmartConnect connect;
	private File file;
	private ConnectServerStatus status;
	
	public UploadCaJob(SmartConnect connect, ConnectServerStatus status){
		super("Upload Conservation Area To SMART Connect");
	
		this.connect = connect;
		this.file = new File(SmartContext.INSTANCE.getFilestoreLocation(), status.getLocalFile());
		this.status = status;
	}
	
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		try{
			// get current status
			UploadStatus serverStatus = connect.getUploadStatus(status.getUploadUrl());
			if (checkServerStatus(serverStatus, monitor)){
				displayComplete();
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
		
			int cnt = 0;
			while(cnt < MAX_RETRY){
				//upload file
				try{
					cnt++;
					connect.uploadFile(status.getUploadUrl(), file, serverStatus.getCurrentSize());
				}catch (Exception ex){
					//we failed;
					ConnectPlugIn.log(ex.getMessage(), ex);
				}
		
				serverStatus = connect.getUploadStatus(status.getUploadUrl());
				if (checkServerStatus(serverStatus, monitor)){
					displayComplete();
					return org.eclipse.core.runtime.Status.OK_STATUS;
				}
				//TODO: we may want to wait between tries????
			}
			//if we are here we have tried max_retry times and the file has still not been uploaded
			throw new Exception("Upload reached max tries of 100.  Please validate server connection and try again.");
		}catch (Exception ex){
			//something failed; we don't know what
			ConnectPlugIn.displayLog("Error occurred uploading conservation area to Connect.  Please try again or review state on SMART Connect." + ex.getMessage(), ex);
		}finally{
			connect.close();
		}
		return org.eclipse.core.runtime.Status.OK_STATUS;

	}
	
	private void displayComplete(){
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				if (status.getStatus() == org.wcs.smart.connect.model.ConnectServerStatus.Status.DONE){
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), 
						"SMART Connect Upload", 
						"Upload to SMART Connect complete.");
				}else if (status.getStatus() == org.wcs.smart.connect.model.ConnectServerStatus.Status.ERROR){
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), 
							"SMART Connect Upload", 
							"An error occurred during uploading Conservation Area to SMART Connect. Check status on SMART Connect.");
				}
			}});
	}
	
	/*
	 * check and status of the server; uploading the local status as necessary
	 * @return true if upload is completed; false if we need to continue with upload
	 */
	private boolean checkServerStatus(UploadStatus serverStatus, IProgressMonitor monitor) throws Exception{
		monitor.subTask("Checking server status");
		if (serverStatus.getStatus() == Status.COMPLETE){
			this.status.setStatus(ConnectServerStatus.Status.DONE);
			updateDatabaseStatus();
			
			//attempt to delete local file
			deleteLocalFile();
			
			//we are done
			return true;
			
		}else if (serverStatus.getStatus() == Status.PROCESSING ){
			//attempt to delete local file
			deleteLocalFile();
			
			//upload was successful but we need to wait for processing
			if (!waitProcessing(monitor)){
				//we waited 5 minutes and we do not know how to proceed
				throw new Exception("Server unable to process file after 5 minutes.  Check status on SMART Connect");
			}
			return true;
			
		}else if (serverStatus.getStatus() == Status.ERROR){
			this.status.setStatus(ConnectServerStatus.Status.ERROR);
			updateDatabaseStatus();
			//we are done; something went wrong but we do not know what
			return true;
		}
		//continue processing
		return false;
	}
	
	/*
	 * delete the local ca upload file
	 */
	private void deleteLocalFile(){
		if (file.exists()) file.delete();
	}
	
	/*
	 * Poll status waiting for processing to complete.
	 */
	private boolean waitProcessing(IProgressMonitor monitor) throws Exception{
		
		//wait for processing 
		monitor.subTask("Waiting for Server to complete processing...");
		
		Long startTime = System.nanoTime();
		Long currentTime = System.nanoTime();
		while( (currentTime - startTime) < MAX_WAIT){
			Thread.sleep(STATUS_WAIT_TIME);
			UploadStatus serverStatus = connect.getUploadStatus(status.getUploadUrl());
			
			if(serverStatus.getStatus() == Status.COMPLETE){
				this.status.setStatus(ConnectServerStatus.Status.DONE);
				updateDatabaseStatus();
				return true;
			}else if (serverStatus.getStatus() == Status.ERROR){
				this.status.setStatus(ConnectServerStatus.Status.ERROR);
				updateDatabaseStatus();
				return true;
			}
			currentTime = System.nanoTime();
		}
		return false;
		
	}
	
	
	/*
	 * Save sthe current status of the status object to the database
	 */
	private void updateDatabaseStatus(){
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			s.saveOrUpdate(status);
			s.getTransaction().commit();
		}catch (Exception ex){
			ConnectPlugIn.log(ex.getMessage(),ex);
		}finally{
			s.close();
		}
	}

}
