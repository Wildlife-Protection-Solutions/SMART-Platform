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

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.ConnectStatusManager;
import org.wcs.smart.connect.ConnectStatusManager.ServerStatus;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Engine to download a change log file from the server and
 * apply it to the local database.
 * 
 * @author Emily
 *
 */
public class DownloadChangeLogEngine {
	
	private SmartConnect connect;
	private ConservationArea ca;
	
	protected ConnectSyncHistoryRecord record = null;
	
	private ConnectServerStatus serverInfo = null;
	
	public DownloadChangeLogEngine(ConservationArea ca, SmartConnect connect){
		this.ca = ca;
		this.connect = connect;
		
	}
	
	/**
	 * Download and apply updates.
	 * 
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	public void downloadInstall() throws Exception{

		/* the ca info */
		if (ca.getUuid().equals(ConservationArea.MULTIPLE_CA)){
			throw new Exception(Messages.DownloadChangeLogEngine_CCAAError);
		}
		
		//acquire lock
		if (!SmartConnect.UPLOAD_LOCK.tryAcquire()){
			throw new Exception(Messages.DownloadChangeLogEngine_AlreadyProcessingError);		
		}

		try{
			setServerStatus(ServerStatus.CONNECTING, Messages.DownloadChangeLogEngine_statusLineValue);
			
			Session session = HibernateManager.openSession();
			try{
				if (!DerbyReplicationManager.INSTANCE.canReplicate(session, ca)){
					throw new Exception(Messages.DownloadChangeLogEngine_ReplicationNotEnabledError);
				}
			}finally{
				session.close();
			}
			ConnectSyncHistoryRecord previous = SyncHistoryManager.INSTANCE.getLastNonErrorSyncRecord(ca, ConnectSyncHistoryRecord.Type.DOWNLOAD);
			if (previous != null && 
					previous.getDatetime().getTime() < ((new Date()).getTime() - DerbyReplicationManager.REPLICATION_MAXTIME_DAYS * 24 * 60 * 60 *1000l)){
				throw new Exception(MessageFormat.format(Messages.DownloadChangeLogEngine_TooOldError, DerbyReplicationManager.REPLICATION_MAXTIME_DAYS));
			}
			
			//create sync history record for database
			record = SyncHistoryManager.INSTANCE.create(connect.getServer().getConservationArea(), connect.getServer(), ConnectSyncHistoryRecord.Type.DOWNLOAD);
			
			
			Session s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				serverInfo = (ConnectServerStatus) s.get(ConnectServerStatus.class, ca.getUuid());
				s.update(record);
				s.getTransaction().commit();
			}finally{
				s.close();
			}
			
			if (serverInfo == null){
				throw new Exception(Messages.DownloadChangeLogEngine_ServerNotFoundError);
			}
			setServerStatus(ServerStatus.DOWNLOADING, Messages.DownloadChangeLogEngine_statusLineValue);
			final DownloadChangeLogJob downloadJob = new DownloadChangeLogJob(connect, serverInfo, record);
			downloadJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					saveStatusRecord();
					if (record.getStatus() == Status.ERROR){
						//we are done
						processComplete();
						return;
					}
					//apply file
					afterDownload(downloadJob.getDownloadFile());
				}
				
			});
			downloadJob.schedule();
		}catch (Exception ex){
			//some error
			try{
				if (record != null){
					Session s = HibernateManager.openSession();
					try{
						s.beginTransaction();
						record.setStatus(Status.ERROR);
						s.getTransaction().commit();
					}finally{
						s.close();
					}
				}
			}catch (Exception ex2){
				ConnectPlugIn.log(Messages.DownloadChangeLogEngine_DownloadError + ex2.getMessage(), ex2);
			}
			
			SmartConnect.UPLOAD_LOCK.release();
			
			setServerStatus(ServerStatus.ERROR, Messages.DownloadChangeLogEngine_statusLineValueError);
			throw ex;
		}
	}

	/**
	 * Called at the end of the process once the file has been
	 * downloaded and applied.
	 * 
	 */
	protected void processComplete(){
		//save status and unlock db
		try{
			saveStatusRecord();
		}finally{
			SmartConnect.UPLOAD_LOCK.release();
		}
		
		if (record.getStatus() == Status.DONE ||
				record.getStatus() == Status.NODATA){
			setServerStatus(ServerStatus.UPTODATE, Messages.DownloadChangeLogEngine_statusLineValueUpToDate);
		}else{
			setServerStatus(ServerStatus.ERROR, record.getErrorString());
		}
	}
	private void saveStatusRecord(){
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.saveOrUpdate(record);
			s.getTransaction().commit();
		}finally{
			s.close();
		}
	}
	
	private void afterDownload(Path changeLogFile){
		ApplyChangeLogJob job = new ApplyChangeLogJob(changeLogFile, serverInfo, record);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				saveStatusRecord();
				processComplete();
			}
		});
		job.schedule();
	}
	
	private void setServerStatus(ConnectStatusManager.ServerStatus status, String message){
		ConnectStatusManager.INSTANCE.serverStatusModified(status, message);
	}
	
}