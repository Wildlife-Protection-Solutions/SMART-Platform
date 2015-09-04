package org.wcs.smart.connect.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.api.model.WorkItemStatus.Status;

public abstract class FileUploaderJob extends Job {
	

	public static final int MAX_RETRY = 100;
	public static final int STATUS_WAIT_TIME = 1000; //1 sec
	public static final long MAX_WAIT = 5 * 60 * 1000000000l;  //5 minutes in nano seconds
	
	protected String url;
	protected Path file;
	protected SmartConnect connect;
	
	protected FileUploaderJob(String url, Path file, SmartConnect connect, String name){
		super(name);
		this.url = url;
		this.file = file;
		this.connect = connect;
	}
	
	
	protected void uploadFile( IProgressMonitor monitor) throws Exception{
		// get current status
		WorkItemStatus serverStatus = connect.getWorkItemStatus(url);
		if (checkServerStatus(serverStatus, monitor)){
			return ;
		}
		int cnt = 0;
		while(cnt < MAX_RETRY){
			//upload file
			try{
				cnt++;
				connect.uploadFile(url, file, 
						serverStatus.getCurrentSize());
			}catch (Exception ex){
				//we failed;
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
					serverStatus = connect.getWorkItemStatus(url);
			if (checkServerStatus(serverStatus, monitor)){
				return ;
			}
			//TODO: we may want to wait between tries????
		}
		//if we are here we have tried max_retry times and the file has still not been uploaded
		throw new Exception("Upload reached max tries of " + MAX_RETRY + ".  Please validate server connection and try again.");
	}
	
	
	/*
	 * delete the local ca upload file
	 */
	protected void deleteLocalFile(){
		try{
			Files.deleteIfExists(file);
		}catch (IOException ex){
			ConnectPlugIn.log("Could not delete ca uploader export file.", ex);
		}
	}
	
	protected boolean checkServerStatus(WorkItemStatus serverStatus,
			IProgressMonitor monitor) throws Exception{
		monitor.subTask("Checking server status");
		if (serverStatus.getStatus() == Status.COMPLETE){
			onUploadComplete(serverStatus);
			onProcessingComplete(serverStatus);
			
			//we are done
			return true;
			
		}else if (serverStatus.getStatus() == Status.PROCESSING ){
			//attempt to delete local file
			onUploadComplete(serverStatus);
			
			//upload was successful but we need to wait for processing
			if (!waitProcessing(monitor)){
				//we waited 5 minutes and we do not know how to proceed
				throw new Exception("Server unable to process file after 5 minutes.  Check status on SMART Connect");
			}
			return true;
			
		}else if (serverStatus.getStatus() == Status.ERROR){
			onError(serverStatus);
			//we are done; something went wrong but we do not know what
			return true;
		}
		//continue processing
		return false;
	}
	
	/*
	 * Poll status waiting for processing to complete.
	 */
	protected boolean waitProcessing(IProgressMonitor monitor) throws Exception{
		
		//wait for processing 
		monitor.subTask("Waiting for Server to complete processing...");
		
		Long startTime = System.nanoTime();
		Long currentTime = System.nanoTime();
		while( (currentTime - startTime) < MAX_WAIT){
			Thread.sleep(STATUS_WAIT_TIME);
			WorkItemStatus serverStatus = connect.getWorkItemStatus(url);
			
			if(serverStatus.getStatus() == Status.COMPLETE){
				onProcessingComplete(serverStatus);
				return true;
			}else if (serverStatus.getStatus() == Status.ERROR){
				onError(serverStatus);
				return true;
			}
			currentTime = System.nanoTime();
		}
		return false;
	}
	
	protected abstract void onUploadComplete(WorkItemStatus status);
	
	protected abstract void onProcessingComplete(WorkItemStatus status);
	
	protected abstract void onError(WorkItemStatus status);
}
