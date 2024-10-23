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
package org.wcs.smart.connect.internal.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.api.model.WorkItemStatus.Status;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServerOption;

import jakarta.ws.rs.InternalServerErrorException;

/**
 * Job for uploading a file to a SMART connect URL.
 * 
 * @author Emily
 *
 */
public abstract class FileUploaderJob extends Job {
	
	protected String url;
	protected Path file;
	protected SmartConnect connect;
	
	/**
	 * Creates a new job.
	 * @param url the upload url
	 * @param file the file to upload
	 * @param connect the smart connect instance
	 * @param name job name
	 */
	protected FileUploaderJob(String url, Path file, SmartConnect connect, String name){
		super(name);
		this.url = url;
		this.file = file.toAbsolutePath().normalize();
		this.connect = connect;
	}
	
	
	/**
	 * 
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor
	 * @throws Exception
	 */
	protected void uploadFile(IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor, Messages.FileUploaderJob_TaskName, 70);
		// get current status
		WorkItemStatus serverStatus = connect.getWorkItemStatus(url);
		
		try{
			if (checkServerStatus(serverStatus, progress.split(10))) return;
			
			int cnt = 0;			
			long waitTime = ConnectServerOption.ConnectionOption.RETY_WAIT_TIME.getIntegerValue(connect.getServer());
			long initWaitTime = waitTime;
			
			//upload file
			progress.setTaskName(Messages.FileUploaderJob_subTaskName);
			
			int maxRetry = ConnectServerOption.ConnectionOption.MAX_RETRY_UPLOAD.getIntegerValue(connect.getServer());
			
			SubMonitor uploadProgress = progress.split(30);
			uploadProgress.beginTask(MessageFormat.format(ConnectPlugIn.PERCENT_UPLOAD_PROGRESS_MESSAGE, 0),  100);
			while(maxRetry == -1 || cnt < maxRetry){
				progress.setTaskName(MessageFormat.format(Messages.FileUploaderJob_AttemptStatusMessage, (cnt+1)));

				//upload file
				try{
					cnt++;
					if (serverStatus != null) {
						connect.uploadFile(url, file, serverStatus.getCurrentSize(), uploadProgress);
					}
				}catch (OperationCanceledException ex) {
					if (doUploadCancelled()) return;
				}catch (InternalServerErrorException ex) {
					String info  = ex.getResponse().readEntity(String.class);
					ConnectPlugIn.log(info, ex);
				}catch (Throwable ex){
					ConnectPlugIn.log(ex.getMessage(), ex);
				}
				
				if (cnt % 10 == 0 && waitTime < 1_800_000 ) {
					//add increment to wait time to max 30 minutes;
					waitTime = waitTime + initWaitTime;
				}
				try {
					uploadProgress.checkCanceled();
					serverStatus = null;
					serverStatus = connect.getWorkItemStatus(url);
					if (serverStatus.getStatus() != Status.UPLOADING) {
						break;
					}
					Thread.sleep(waitTime);
				}catch (OperationCanceledException ex) {
					if (doUploadCancelled()) return;
				}
			}
			
			if (serverStatus == null || serverStatus.getStatus() == Status.UPLOADING) {
				//if we are here we have tried max_retry times and the file has still not been uploaded
				throw new Exception(MessageFormat.format(Messages.FileUploaderJob_ToManyTried, ConnectServerOption.ConnectionOption.MAX_RETRY_UPLOAD.getIntegerValue(connect.getServer())));
			}
			
			
			//wait for processing to finish
			//really we shouldn't allow cancelling here but that isn't supported by the
			//api at this time so lets catch and ignore cancel requests
			//https://bugs.eclipse.org/bugs/show_bug.cgi?id=155479
			
			progress.setWorkRemaining(30);
			if (!checkServerStatus(serverStatus, progress.split(30))){
				throw new Exception(Messages.FileUploaderJob_UnknownErrorMessage);
			}
		}catch(Exception ex){
			if (serverStatus != null) {
				serverStatus.setMessage(ex.getMessage());
				onError(serverStatus.getMessage());
			}else {
				onError(ex.getMessage());
			}
			throw ex;
		}
	}
	
	/**
	 * Can only cancel if the server status is uploading otherwise
	 * can't cancel job.
	 * 
	 * @return
	 * @throws  
	 */
	private boolean doUploadCancelled()  {
		boolean canCancel = false;
		try {
			WorkItemStatus serverStatus = connect.getWorkItemStatus(url);
			if (serverStatus.getStatus() == Status.UPLOADING) {
				canCancel = true;
			}
		}catch (Exception ex) {
			ConnectPlugIn.log(ex.getMessage(), ex);
		}
		if (!canCancel) return false;
		
		//cancel
		this.onUploadCancelled();
		return true;
	}
	
	/*
	 * delete the local ca upload file
	 */
	protected void deleteLocalFile(){
		try{
			Files.deleteIfExists(file);
		}catch (IOException ex){
			ConnectPlugIn.log("Could not delete ca uploader export file.", ex); //$NON-NLS-1$
		}
	}
	
	/*
	 * checks the upload status on the server
	 */
	/**
	 * 
	 * @param serverStatus
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor
	 * @return
	 * @throws Exception
	 */
	protected boolean checkServerStatus(WorkItemStatus serverStatus,
			IProgressMonitor monitor) throws ProcessingTimeoutException, Exception{
		
		if (serverStatus == null) return false;
		
		if (serverStatus.getStatus() == Status.COMPLETE){
			onUploadComplete(serverStatus);
			onProcessingComplete(serverStatus);
			
			//we are done
			return true;
			
		}else if (serverStatus.getStatus() == Status.PROCESSING ){
			onUploadComplete(serverStatus);
			
			//upload was successful but we need to wait for processing
			if (!waitProcessing(monitor)){
				//we waited specified timeout and we do not know how to proceed
				onProcessingTimeOut();
			}
			return true;
			
		}else if (serverStatus.getStatus() == Status.ERROR){
			onError(serverStatus.getMessage());
			//we are done; something went wrong but we do not know what
			return true;
		}
		//continue processing
		return false;
	}
	
	/*
	 * Poll status waiting for processing to complete.
	 */
	protected boolean waitProcessing(IProgressMonitor pmonitor) throws Exception{
		
		//wait for processing
		SubMonitor monitor = SubMonitor.convert(pmonitor);
		monitor.beginTask(Messages.FileUploaderJob_Waiting, 100);
		
		try {
			Long startTime = System.nanoTime();
			Long currentTime = System.nanoTime();
			long waitTime = ConnectServerOption.ConnectionOption.RETY_WAIT_TIME.getIntegerValue(connect.getServer());
			int last = 0;
			while( (currentTime - startTime)  < ConnectServerOption.ConnectionOption.MAX_PROCESSING_WAIT_TIME.getIntegerValue(connect.getServer()) * 1000000l){
				Thread.sleep(waitTime);
				try{
					WorkItemStatus serverStatus = connect.getWorkItemStatus(url);
				
					if(serverStatus.getStatus() == Status.COMPLETE){
						monitor.setTaskName(Messages.FileUploaderJob_CompleteTaskName);
						monitor.worked(100 - last);
						monitor.done();
						onProcessingComplete(serverStatus);
						return true;
					}else if (serverStatus.getStatus() == Status.ERROR){
						monitor.setTaskName(Messages.FileUploaderJob_ErrorTaskName);
						monitor.worked(100 - last);
						monitor.done();
						onError(serverStatus.getMessage());
						return true;
					}
					monitor.setTaskName(
							MessageFormat.format("{0} ({1}% - {2})", //$NON-NLS-1$
							Messages.FileUploaderJob_Waiting,
							serverStatus.getPercentComplete(),
							serverStatus.getMessage()));
					monitor.worked(serverStatus.getPercentComplete() - last);
					last = serverStatus.getPercentComplete();
					monitor.checkCanceled();
				}catch(OperationCanceledException ex) {
					//do not allow cancelling at this stage in the process
				}catch (Exception ex){
					ConnectPlugIn.log(ex.getMessage(), ex);
				}
				currentTime = System.nanoTime();
			}
		}finally {
			monitor.done();
		}
		return false;
	}
	
	/**
	 * Called when the upload is completed, but the processing of the
	 * upload on the server is not complete.
	 * 
	 * @param status
	 */
	protected abstract void onUploadComplete(WorkItemStatus status);
	
	/**
	 * Called when the processing of the upload item
	 * on the server is complete.
	 * @param status
	 */
	protected abstract void onProcessingComplete(WorkItemStatus status);
	
	/**
	 * Called when an error occurs processing item on server.
	 * 
	 * @param status
	 */
	protected abstract void onError(String errorMessage);
	
	
	/**
	 * Called when upload is cancelled by the user
	 * 
	 * @param status
	 */
	protected abstract void onUploadCancelled();
	
	/**
	 * Called when timeout occurs while waiting for SMART Connect processing.
	 * But default this throws an excpetion but subclasses can overwrite
	 * 
	 */
	protected void onProcessingTimeOut() throws ProcessingTimeoutException{
		throw new ProcessingTimeoutException(
				MessageFormat.format(
						Messages.FileUploaderJob_ToLong, ConnectServerOption.ConnectionOption.MAX_PROCESSING_WAIT_TIME.getIntegerValue(connect.getServer()) / (1000 *60.0) ));
	}
}
