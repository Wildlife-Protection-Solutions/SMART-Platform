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

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.internal.server.ConnectDataQueue;
import org.wcs.smart.connect.dataqueue.internal.server.DataQueueApi;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Processes and item from the data queue.
 * @author Emily
 *
 */
public class DataQueueItemProcessor extends Job {

	/**
	 * Mutex so only one job is processing at a time.
	 */
	public static ISchedulingRule MUTEX = new ISchedulingRule() {
		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}
		
		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}
	};
	
	private SmartConnect connect;
	private LocalDataQueueItem item;
	
	private DataQueueProcessMonitor progressWrapper;
	
	
	/**
	 * Should not be called directly.  Users who want to schedule the data queue processor should
	 * use ProcessorManager.processDataQueue() function.
	 * 
	 * @param connect
	 * @param progressWrapper
	 */
	DataQueueItemProcessor(SmartConnect connect, DataQueueProcessMonitor progressWrapper) {
		super("Data Queue Processor");
		this.connect = connect;
		this.progressWrapper = progressWrapper;
	}
	
	public DataQueueProcessMonitor getWatcher(){
		return this.progressWrapper;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor = progressWrapper.setProgressMonitor(monitor);
		
		boolean reschedule = true;
		try{
		
			item = DataQueueManager.INSTANCE.checkOutNextQueueItem();
			progressWrapper.setDataQueueItem(item);
			
			if (item == null){
				//nothing to do
				reschedule = false;
				return Status.OK_STATUS;
			}
			
			monitor.beginTask("Processing Item: " + item.getName(), 40);
			
			monitor.subTask("confirming status with server....");
			//go to the server and update the status on the server to processing
			//if this fails then we skip this item as it is likely processed by someone else
			try{
				ConnectDataQueue.INSTANCE.updateStatus(connect, item, DataQueueApi.ServerStatus.PROCESSING);
			}catch (Exception ex){
				Exception toLog = ex;
				if (ex instanceof NotFoundException){
					toLog = new NotFoundException("Failed to find data queue item on Connect. " + ex.getMessage(), ex);
				}
				if (ex instanceof BadRequestException){
					toLog = new BadRequestException("Data queue item is being processed by another user." + ex.getMessage(), ex);
				}
				toLog = new Exception("Error processing item; unable to confirm status with Connect server. " + ex.getMessage(), ex);
				
				ConnectDataQueuePlugin.log(toLog.getMessage(), toLog);
				updateLocalStatus(LocalDataQueueItem.Status.ERROR, toLog.getMessage());
				return Status.OK_STATUS;
			}
			monitor.worked(10);
			
			IItemProcessor.ProcessingStatus processingStatus = null;
			try{
				monitor.subTask("downloading file...");
				updateLocalStatus(LocalDataQueueItem.Status.DOWNLOADING, null);
				downloadFile(new SubProgressMonitor(monitor, 10));
				
				monitor.subTask("processing file...");
				updateLocalStatus(LocalDataQueueItem.Status.PROCESSING, null);
				processingStatus = processItem(new SubProgressMonitor(monitor, 10));
				
			}catch (Exception ex){
				ConnectDataQueuePlugin.log(ex.getMessage(), ex);
				updateLocalStatus(LocalDataQueueItem.Status.ERROR, ex.getMessage());
			
				try{
					ConnectDataQueue.INSTANCE.updateStatus(connect, item, DataQueueApi.ServerStatus.ERROR);
				}catch (Exception ex2){
					ConnectDataQueuePlugin.displayLog("Processing failed and statue could not be updated on CONNECT. This status will need to be manually reset on the server and processing restarted.", ex);	
				}
				return Status.OK_STATUS;
			}
			monitor.subTask("updating status...");
			try{
				updateLocalStatus(processingStatus.getStatus(), processingStatus.getMessage());
			}catch (Exception ex){
				ConnectDataQueuePlugin.displayLog("Processing completed local status is not updated.  Item should be removed from local queue and server queue manually, otherwise data may be duplicated if items are reprocessed.", ex);
			}
			
			try{
				ConnectDataQueue.INSTANCE.updateStatus(connect, item, DataQueueApi.ServerStatus.COMPLETE);
			}catch (Exception ex){
				ConnectDataQueuePlugin.displayLog("Processing completed but the state on the Connect Server not be updated.  Item status will need to be manually updated on server, otherwise data may be duplicated if items are reprocessed.", ex);
			}
			monitor.worked(10);
		}finally{
			if (reschedule){
				ProcessorManager.INSTANCE.processDataQueue(connect);
			}
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private void downloadFile(IProgressMonitor monitor) throws Exception{
		//download the file
		String down = ConnectDataQueue.INSTANCE.getFileDownloadUrl(connect, item);
		Path localFile = connect.downloadFileFromUrl(down, null, monitor);
		
		Path moveTo = FileSystems.getDefault()
				.getPath(SmartContext.INSTANCE.getFilestoreLocation())
				.resolve(ConnectSyncHistoryRecord.CONNECT_FILESTORE_DIR)
				.resolve("dataqueue/")
				.resolve(localFile.getFileName());
		if (!Files.exists(moveTo.getParent())){
			Files.createDirectories(moveTo.getParent());
		}
		
		Files.move(localFile, moveTo);
		
		String relativeFile =FileSystems.getDefault()
				.getPath(SmartContext.INSTANCE.getFilestoreLocation())
				.relativize(moveTo)
				.toString();
		
		item.setFile(relativeFile);
		saveItem();
	}
	
	private IItemProcessor.ProcessingStatus processItem(IProgressMonitor monitor) throws Exception{
		IItemProcessor processor = getItemProcessor();
		return processor.process(item, monitor);
	}
	
	
	private IItemProcessor getItemProcessor() throws Exception{
		for(IItemProcessor p : ProcessorManager.INSTANCE.getProcessors()){
			if (p.canProcess(item.getType())){
				return p;
			}
		}
		throw new Exception(MessageFormat.format("Could not find a processor for file type {0}.", item.getType()));
	}
	
	private void updateLocalStatus(LocalDataQueueItem.Status newStatus, String errorMsg){
		item.setStatus(newStatus);
		item.setErrorMessage(errorMsg);	
		saveItem();
	}
	
	private void saveItem(){
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			s.saveOrUpdate(item);
			s.getTransaction().commit();
		}finally{
			s.close();
		}
	}

}
