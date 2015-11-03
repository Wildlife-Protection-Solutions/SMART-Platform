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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.ConnectStatusManager;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Type;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Engine to upload the a change log file to the server.
 * 
 * @author Emily
 *
 */
public class UploadChangeLogEngine {

	private NothingToUpdateException nothingtoUpdate = new NothingToUpdateException("Server up-to-date.  There are no local changes to upload to the server.");
	
	private SmartConnect connect;
	protected ConnectSyncHistoryRecord record;
	
	public UploadChangeLogEngine(SmartConnect connect){
		this.connect = connect;
	}
	
	/**
	 * Creates an upload package and starts a job
	 * to upload the package to the server.
	 * 
	 * Configured to run in either a progress monitor dialog
	 * or a background job.
	 * 
	 * @param monitor
	 * @throws Exception
	 */
	public void createUpload(IProgressMonitor monitor) throws Exception{
		
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		if (SmartDB.isMultipleAnalysis()) throw new Exception("Cross-ca analysis can not be syncronized with server.");
	
		
		if (!SmartConnect.UPLOAD_LOCK.tryAcquire()){
			throw new Exception("Another process is already uploading changes to SMART Connect.  You must wait until that process is completed to upload change log.");
		}
		
		try{
			long currentRevisionNo = -1;
			Session session = HibernateManager.openSession();
			try{
				if (!DerbyReplicationManager.INSTANCE.isReplicationEnabled(session)){
					throw new Exception("Replication not enabled.  Cannot upload changes from server.");
				}
				currentRevisionNo = ChangeLogTableManager.INSTANCE.getMaxLocalRevision(session, ca);
			}finally{
				session.close();
			}

			monitor.beginTask("Creating sync package", 3);

			ConnectSyncHistoryRecord previous = SyncHistoryManager.INSTANCE.getLastNonErrorSyncRecord(ca, ConnectSyncHistoryRecord.Type.UPLOAD);
			if (previous.getEndRevision() >= currentRevisionNo){
				throw nothingtoUpdate;
			}
			
			record = null;
			
			if (previous == null || previous.getStatus() == Status.DONE ){
				//start a new upload session
			
				record = SyncHistoryManager.INSTANCE.create(ca, connect.getServer(), Type.UPLOAD);
				long startRevision = -1;
				if (previous != null ){
					startRevision = previous.getEndRevision();
				}
				
				record.setStartRevision(startRevision);
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					s.saveOrUpdate(record);
					s.getTransaction().commit();
				}finally{
					s.close();
				}
			}else if (previous != null && previous.getStatus() == Status.ACTIVE){
				//we know that another one is not currently running because of
				//the upload lock
				record = previous;
			}
			
			monitor.worked(1);
			 
			//package changes
			if (record.getStatusUrl() == null){
				//upload has not started 
				if (!Files.exists(FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), record.getChangeLogZipFile() + ".zip"))){
					//package does not exist; we need to create it
					ChangeLogPackager packer = new ChangeLogPackager(record);
					packer.createPackage(new SubProgressMonitor(monitor, 1));
					record.setEndRevision(packer.getLastRevision());

					//throw exception if necessary
					if(record.getStartRevision().longValue() == packer.getLastRevision()){
						record.setStatus(Status.NODATA);
						saveRecord(record);
						//delete file
						try{
							Path p = Paths.get(SmartContext.INSTANCE.getFilestoreLocation(), record.getChangeLogZipFile());
							Files.deleteIfExists(p);
						}catch (IOException ex){
							ConnectPlugIn.log("Could not delete ca uploader export file.", ex);
						}		
						throw nothingtoUpdate;
					}
					
					//save record
					saveRecord(record);
				}
			}
			
			monitor.worked(1);
			
			//upload package to server
			UploadChangeLogJob upload = new UploadChangeLogJob(record, connect);
			upload.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					processComplete();
				}
			});
			upload.schedule();
		}catch (Exception ex){
			SmartConnect.UPLOAD_LOCK.release();
			throw ex;
		}
	}
	

	/**
	 * Called at the end of the process once the file has been
	 * downloaded and applied.
	 */
	protected void processComplete(){
		SmartConnect.UPLOAD_LOCK.release();
		ConnectStatusManager.INSTANCE.localStatusModified(null, null);
	}
	
	private void saveRecord(ConnectSyncHistoryRecord current){
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
