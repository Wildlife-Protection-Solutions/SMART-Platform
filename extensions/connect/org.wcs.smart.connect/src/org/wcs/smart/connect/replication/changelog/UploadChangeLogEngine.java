package org.wcs.smart.connect.replication.changelog;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.concurrent.Semaphore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Type;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public class UploadChangeLogEngine {

	public static final Semaphore UPLOAD_LOCK = new Semaphore(1);
	
	private SmartConnect connect;
	
	public UploadChangeLogEngine(SmartConnect connect){
		this.connect = connect;
	}
	
	public void syncUpload(IProgressMonitor monitor) throws Exception{
		if (!UPLOAD_LOCK.tryAcquire()){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), 
							"Error", "Another process is already uploading changes to SMART Connect.  You must wait until that process is completed to upload change log.");		
				}
				
			});	
			return;
		}
		try{
			monitor.beginTask("Creating sync package", 3);
			//check to ensure 
			ConservationArea ca = SmartDB.getCurrentConservationArea();
			if (SmartDB.isMultipleAnalysis()) throw new Exception("Cross-ca analysis can not be syncronized with server.");
		
			ConnectSyncHistoryRecord previous = SyncHistoryManager.INSTANCE.getLastNonErrorSyncRecord(ca, ConnectSyncHistoryRecord.Type.UPLOAD);
			ConnectSyncHistoryRecord current = null;
			
			
			if (previous == null || previous.getStatus() == Status.DONE ){
				//start a new upload session
			
				current = SyncHistoryManager.INSTANCE.create(ca, connect.getServer(), Type.UPLOAD);
				long startRevision = -1;
				if (previous != null ){
					startRevision = previous.getEndRevision();
				}
				
				current.setStartRevision(startRevision);
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					s.saveOrUpdate(current);
					s.getTransaction().commit();
				}finally{
					s.close();
				}
			}else if (previous != null && previous.getStatus() == Status.ACTIVE){
				//we know that another one is not currently running because of
				//the upload lock
				current = previous;
			}
			
			monitor.worked(1);
			
			//package changes
			if (current.getStatusUrl() == null){
				//upload has not started 
				if (!Files.exists(FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), current.getChangeLogZipFile() + ".zip"))){
					//package does not exist; we need to create it
					ChangeLogPackager packer = new ChangeLogPackager(current);
					packer.createPackage(new SubProgressMonitor(monitor, 1));
					if(current.getStartRevision() == packer.getLastRevision()){
						//nothing to upload
						current.setStatus(Status.DONE);
					}else{
						//update sync record revision
						current.setEndRevision(packer.getLastRevision());
					}
					//save record
					Session s = HibernateManager.openSession();
					try{
						s.beginTransaction();
						s.saveOrUpdate(current);
						s.getTransaction().commit();
					}finally{
						s.close();
					}
					//throw exception if necessary
					if(current.getStartRevision() == packer.getLastRevision()){
						throw new Exception("Conservation up to date. Nothing to sync");
					}
				}
			}
			
			monitor.worked(1);
			
			//upload package to server
			UploadChangeLogJob upload = new UploadChangeLogJob(current, connect);
			upload.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					UPLOAD_LOCK.release();
				}
			});
			upload.schedule();
		}catch (Exception ex){
			UPLOAD_LOCK.release();
			throw ex;
		}
	}
}
