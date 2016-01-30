package org.wcs.smart.connect.dataqueue.process;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem.Type;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.server.ConnectDataQueue;
import org.wcs.smart.connect.dataqueue.server.DataQueueApi;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.hibernate.HibernateManager;

public class DataQueueItemProcessor extends Job {

	private SmartConnect connect;
	private LocalDataQueueItem item;
	
	private ProgressMonitorWatcher progressWrapper;
	
	
	DataQueueItemProcessor(SmartConnect connect, ProgressMonitorWatcher progressWrapper) {
		super("Data Queue Processor");
		this.connect = connect;
		this.progressWrapper = progressWrapper;
	}
	
	public ProgressMonitorWatcher getWatcher(){
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
					toLog = new BadRequestException("Item on server already been processed.  Cannot reprocess without manually resetting status on Connect." + ex.getMessage(), ex);
				}
				toLog = new Exception("Error processing item; unable to confirm status with Connect server. " + ex.getMessage(), ex);
				
				ConnectDataQueuePlugin.log(toLog.getMessage(), toLog);
				updateLocalStatus(LocalDataQueueItem.Status.ERROR, toLog.getMessage());
				return Status.OK_STATUS;
			}
			monitor.worked(10);
			
			try{
				updateLocalStatus(LocalDataQueueItem.Status.DOWNLOADING, null);
				downloadFile(new SubProgressMonitor(monitor, 10));
				
				updateLocalStatus(LocalDataQueueItem.Status.PROCESSING, null);
				processItem(new SubProgressMonitor(monitor, 10));
				
			}catch (Exception ex){
				ConnectDataQueuePlugin.log(ex.getMessage(), ex);
				updateLocalStatus(LocalDataQueueItem.Status.ERROR, ex.getMessage());
			
				try{
					ConnectDataQueue.INSTANCE.updateStatus(connect, item, DataQueueApi.ServerStatus.ERROR);
				}catch (Exception ex2){
					ConnectDataQueuePlugin.displayLog("Processing completed, but status could not be updated on Connect.  This will need to be manually reset for this item. ", ex);	
				}
				return Status.OK_STATUS;
			}
			try{
				updateLocalStatus(LocalDataQueueItem.Status.COMPLETE, null);
				//TODO: thinks about what to do if this fails;
				ConnectDataQueue.INSTANCE.updateStatus(connect, item, DataQueueApi.ServerStatus.COMPLETE);
			}catch (Exception ex){
				ConnectDataQueuePlugin.displayLog("Processing completed, but status fields could not be updated.  This may result in duplicate data if items are reprocessed.", ex);
			}
			monitor.worked(10);
		}finally{
			if (reschedule){
				ProcessorManager.INSTANCE.startProcessing(connect);
			}
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private void downloadFile(IProgressMonitor monitor) throws Exception{
//		if (true) throw new Exception("download failes");
		monitor.beginTask("dome",1);
		monitor.worked(1);
		monitor.done();
		if (true) return;
		//download the file
		String down = ConnectDataQueue.INSTANCE.getFileDownloadUrl(connect, item);
		Path localFile = connect.downloadFileFromUrl(down, null, monitor);
		
		Path moveTo = FileSystems.getDefault()
				.getPath(SmartContext.INSTANCE.getFilestoreLocation())
				.resolve(ConnectSyncHistoryRecord.CONNECT_FILESTORE_DIR)
				.resolve("/dataqueue/")
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
	}
	
	private void processItem(IProgressMonitor monitor) throws Exception{
		IItemProcessor processor = getItemProcessor();
		processor.process(item, monitor);
	}
	
	
	private IItemProcessor getItemProcessor(){
		return new IItemProcessor() {
			
			@Override
			public void process(DataQueueItem item, IProgressMonitor monitor)
					throws Exception {
				monitor.beginTask("Something", 5);
				// delay 10 seconds
				for (int i = 0; i < 5; i ++){
					Thread.sleep(1000);
					monitor.worked(1);
				}
				monitor.done();
			}
			
			@Override
			public boolean canProcess(Type type) {
				// TODO Auto-generated method stub
				return true;
			}
		};
	}
	
	private void updateLocalStatus(LocalDataQueueItem.Status newStatus, String errorMsg){
		
		item.setStatus(newStatus);
		item.setErrorMessage(errorMsg);
		
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
