package org.wcs.smart.connect.replication.changelog;

import java.nio.file.FileSystems;
import java.nio.file.Files;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Type;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public class UploadSyncEngine {

	private SmartConnect connect;
	
	public UploadSyncEngine(SmartConnect connect){
		this.connect = connect;
	}
	
	public void syncUpload(IProgressMonitor monitor) throws Exception{
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
			//we want to continue this session
			
			//TODO: make sure we are not already running
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
					throw new Exception("Conservation up to date. Nothing to sync");
				}
				
				//update sync record revision
				current.setEndRevision(packer.getLastRevision());
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					s.saveOrUpdate(current);
					s.getTransaction().commit();
				}finally{
					s.close();
				}
			}
		}
		
		monitor.worked(1);
		//upload package to server
		UploadSyncJob upload = new UploadSyncJob(current, connect);
		upload.schedule();
	}
}
