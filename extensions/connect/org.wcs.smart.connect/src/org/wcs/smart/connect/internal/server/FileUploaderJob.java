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
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.io.CopyProgressMonitor;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.api.model.WorkItemStatus.Status;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServerOption;

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
		this.file = file;
		this.connect = connect;
	}
	
	
	protected void uploadFile(IProgressMonitor monitor) throws Exception{
		monitor.beginTask(Messages.FileUploaderJob_TaskName, 4);
		// get current status
		WorkItemStatus serverStatus = connect.getWorkItemStatus(url);
		try{
			if (checkServerStatus(serverStatus, monitor)){
				return ;
			}
			int cnt = 0;			
			long waitTime = ConnectServerOption.ConnectionOption.RETY_WAIT_TIME.getIntegerValue(connect.getServer());
			
			monitor.subTask(Messages.FileUploaderJob_subTaskName);
			CopyProgressMonitor copyMonitor = new CopyProgressMonitor(new SubProgressMonitor(monitor,3), Files.size(file));
			
			while(cnt < ConnectServerOption.ConnectionOption.MAX_RETRY_UPLOAD.getIntegerValue(connect.getServer())){
				//upload file
				try{
					cnt++;
					connect.uploadFile(url, file, 
							serverStatus.getCurrentSize(),
							copyMonitor);
				}catch (Exception ex){
					ConnectPlugIn.log(ex.getMessage(), ex);
				}
				
				serverStatus = connect.getWorkItemStatus(url);
				if (checkServerStatus(serverStatus, monitor)){
					return ;
				}
				Thread.sleep(waitTime);
				if (monitor.isCanceled()) throw new Exception(Messages.FileUploaderJob_Cancelled);
			}
			//if we are here we have tried max_retry times and the file has still not been uploaded
			throw new Exception(MessageFormat.format(Messages.FileUploaderJob_ToManyTried, ConnectServerOption.ConnectionOption.MAX_RETRY_UPLOAD.getIntegerValue(connect.getServer())));
		}catch(Exception ex){
			serverStatus.setMessage(ex.getMessage());
			onError(serverStatus.getMessage());
			throw ex;
		}finally{
			monitor.done();
		}
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
	protected boolean checkServerStatus(WorkItemStatus serverStatus,
			IProgressMonitor monitor) throws Exception{
		monitor.subTask(Messages.FileUploaderJob_StatusCheckSubTaskName);
	
		if (serverStatus.getStatus() == Status.COMPLETE){
			onUploadComplete(serverStatus);
			onProcessingComplete(serverStatus);
			
			//we are done
			return true;
			
		}else if (serverStatus.getStatus() == Status.PROCESSING ){
			onUploadComplete(serverStatus);
			
			//upload was successful but we need to wait for processing
			if (!waitProcessing(monitor)){
				//we waited 5 minutes and we do not know how to proceed
				throw new Exception(
						MessageFormat.format(
								Messages.FileUploaderJob_ToLong, ConnectServerOption.ConnectionOption.MAX_PROCESSING_WAIT_TIME.getIntegerValue(connect.getServer()) / (1000 *60.0) ));
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
	protected boolean waitProcessing(IProgressMonitor monitor) throws Exception{
		
		//wait for processing 
		monitor.subTask(Messages.FileUploaderJob_Waiting);
		
		Long startTime = System.nanoTime();
		Long currentTime = System.nanoTime();
		long waitTime = ConnectServerOption.ConnectionOption.RETY_WAIT_TIME.getIntegerValue(connect.getServer());
		while( (currentTime - startTime)  < ConnectServerOption.ConnectionOption.MAX_PROCESSING_WAIT_TIME.getIntegerValue(connect.getServer()) * 1000000l){
			Thread.sleep(waitTime);
			try{
				WorkItemStatus serverStatus = connect.getWorkItemStatus(url);
			
				if(serverStatus.getStatus() == Status.COMPLETE){
					onProcessingComplete(serverStatus);
					return true;
				}else if (serverStatus.getStatus() == Status.ERROR){
					onError(serverStatus.getMessage());
					return true;
				}
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
			currentTime = System.nanoTime();
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
}
