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
import org.wcs.smart.connect.dataqueue.internal.Messages;
import org.wcs.smart.connect.dataqueue.internal.server.ConnectDataQueue;
import org.wcs.smart.connect.dataqueue.internal.server.DataQueueApi;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor.ProcessingStatus;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

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
		super(Messages.DataQueueItemProcessor_JobName);
		this.connect = connect;
		this.progressWrapper = progressWrapper;
	}
	
	public DataQueueProcessMonitor getWatcher(){
		return this.progressWrapper;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (ProcessorManager.INSTANCE.isProcessingDisabled()) return Status.CANCEL_STATUS;
		
		monitor = progressWrapper.setProgressMonitor(monitor);
		
		boolean reschedule = true;
		try{
		
			monitor.beginTask(Messages.DataQueueItemProcessor_Task1, 30);
			item = DataQueueManager.INSTANCE.checkOutNextQueueItem(connect);
			
			if (item == null){
				//nothing to do
				reschedule = false;
				return Status.OK_STATUS;
			}
			
			progressWrapper.setDataQueueItem(item);
			monitor.worked(5);
			if (item.getStatus() != LocalDataQueueItem.Status.PROCESSING){
				//could not checkout next item for whatever reason
				//do not process
				return Status.OK_STATUS;
			}
			
			
			monitor.setTaskName(Messages.DataQueueItemProcessor_Task2 + item.getName());
			boolean requeue = false;
			IItemProcessor.ProcessingStatus processingStatus = null;
			try{
				monitor.subTask(Messages.DataQueueItemProcessor_Task3);
				if (item.getFullFilePath() == null  || !Files.exists(item.getFullFilePath())){
					//it may have been already downloaded if we are reprocessing
					updateLocalStatus(LocalDataQueueItem.Status.DOWNLOADING, null);
					downloadFile(new SubProgressMonitor(monitor, 10));
				}
				
				monitor.subTask(Messages.DataQueueItemProcessor_Task4);
				updateLocalStatus(LocalDataQueueItem.Status.PROCESSING, null);
				processingStatus = processItem(new SubProgressMonitor(monitor, 10));
				
				
				if (processingStatus.getStatus() == LocalDataQueueItem.Status.REQUEUED){
					requeue = true;
					processingStatus = new ProcessingStatus(LocalDataQueueItem.Status.COMPLETE_WARN, Messages.DataQueueItemProcessor_RequeueOnServer);
				}
					
			}catch (Exception ex){
				ConnectDataQueuePlugin.log(ex.getMessage(), ex);
				updateLocalStatus(LocalDataQueueItem.Status.ERROR, ex.getMessage());
			
				try{
					ConnectDataQueue.INSTANCE.updateStatus(connect, item, DataQueueApi.ServerStatus.ERROR);
				}catch (Exception ex2){
					ConnectDataQueuePlugin.displayLog(Messages.DataQueueItemProcessor_Error1, ex);	
				}
				return Status.OK_STATUS;
			}
			monitor.subTask(Messages.DataQueueItemProcessor_Task5);
			try{
				if (requeue){
					//requeuing action takes place below
					if (item.getCheckOutStatus() != LocalDataQueueItem.Status.REQUEUED){
						processingStatus.appendToMessage(Messages.DataQueueItemProcessor_RequeueMessage);
					}
				}
				updateLocalStatus(processingStatus.getStatus(), processingStatus.getMessage());
			}catch (Exception ex){
				ConnectDataQueuePlugin.displayLog(Messages.DataQueueItemProcessor_Error2, ex);
			}
			
			try{
				if (requeue){
					if (item.getCheckOutStatus() == LocalDataQueueItem.Status.REQUEUED){
						//this is a local item that was requeued so we do not want to requeue it on the server
					}else{
						//this was downloaded from the server; so lets requeue it on the server
						ConnectDataQueue.INSTANCE.updateStatus(connect, item, DataQueueApi.ServerStatus.QUEUED);
					}
				}else{
					ConnectDataQueue.INSTANCE.updateStatus(connect, item, DataQueueApi.ServerStatus.COMPLETE);
				}
			}catch (Exception ex){
				ConnectDataQueuePlugin.displayLog(Messages.DataQueueItemProcessor_Error3, ex);
			}
			monitor.worked(5);
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
				.getPath(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
				.resolve(ConnectDataQueuePlugin.DATA_QUEUE_DIR)
				.resolve(localFile.getFileName());
		if (!Files.exists(moveTo.getParent())){
			Files.createDirectories(moveTo.getParent());
		}
		
		//configure file
		String relativeFile =FileSystems.getDefault()
				.getPath(SmartContext.INSTANCE.getFilestoreLocation())
				.relativize(moveTo)
				.toString();
		item.setFile(relativeFile);
		saveItem();
		
		//move to expected location
		Files.move(localFile, moveTo);
		
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
		throw new Exception(MessageFormat.format(Messages.DataQueueItemProcessor_Error4, item.getType()));
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
