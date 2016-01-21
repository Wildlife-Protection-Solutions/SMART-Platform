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
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.ConnectStatusManager;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Type;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Engine to upload the a change log file to the server.
 * 
 * @author Emily
 *
 */
public class UploadChangeLogEngine {

	private NothingToUpdateException nothingtoUpdate = new NothingToUpdateException(Messages.UploadChangeLogEngine_NothingtoDo);
	
	private SmartConnect connect;
	protected ConnectSyncHistoryRecord record;
	private ConservationArea ca;
	
	public UploadChangeLogEngine(ConservationArea ca, SmartConnect connect){
		this.connect = connect;
		this.ca = ca;
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
	public void createUpload(IProgressMonitor monitor) throws NothingToUpdateException, PackageToLargeException, Exception{
		if (!SmartConnect.UPLOAD_LOCK.tryAcquire()){
			throw new Exception(Messages.UploadChangeLogEngine_AlreadyProcessing);
		}
		
		try{
			Long currentRevisionNo = -1l;
			Session session = HibernateManager.openSession();
			try{
				if (!DerbyReplicationManager.INSTANCE.canReplicate(session, ca)){
					throw new Exception(Messages.UploadChangeLogEngine_ReplicationNotEnabled);
				}
				currentRevisionNo = ChangeLogTableManager.INSTANCE.getMaxLocalRevision(session, ca);
				if (currentRevisionNo == null){
					currentRevisionNo = -1l;
				}
			}finally{
				session.close();
			}

			monitor.beginTask(Messages.UploadChangeLogEngine_TaskName, 3);

			ConnectSyncHistoryRecord previous = SyncHistoryManager.INSTANCE.getLastNonErrorSyncRecord(ca, ConnectSyncHistoryRecord.Type.UPLOAD);
			if ((previous == null && currentRevisionNo == -1) ||
				(previous != null && previous.getEndRevision() >= currentRevisionNo)){
				throw nothingtoUpdate;
			}
			if (previous != null && 
					previous.getDatetime().getTime() < ((new Date()).getTime() - DerbyReplicationManager.REPLICATION_MAXTIME_DAYS * 24 * 60 * 60 *1000l)){
				throw new Exception(MessageFormat.format(Messages.UploadChangeLogEngine_TooOld, DerbyReplicationManager.REPLICATION_MAXTIME_DAYS));
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
				if (!Files.exists(FileSystems.getDefault().getPath(SmartContext.INSTANCE.getFilestoreLocation(), record.getChangeLogZipFile() + ".zip"))){ //$NON-NLS-1$
					//package does not exist; we need to create it
					ChangeLogPackager packer = new ChangeLogPackager(record);
					packer.createPackage(new SubProgressMonitor(monitor, 1));
					record.setEndRevision(packer.getLastRevision());

					//throw exception if necessary
					if(record.getStartRevision().longValue() == packer.getLastRevision()){
						record.setStatus(Status.NODATA);
						saveRecord(record);
						//delete file
						deletePackageFile();
						throw nothingtoUpdate;
					}
					
					//check package size
					if (connect.getServer().getOptionAsBoolean(ConnectServerOption.Option.PACKAGE_PROMPT)){
						long sizeInBytes = Files.size(Paths.get(SmartContext.INSTANCE.getFilestoreLocation(), record.getChangeLogZipFile()));
						long maxSizeInBytes = connect.getServer().getOptionAsInt(ConnectServerOption.Option.PACKAGE_PROMPT_SIZE) * 1000000l;
						
						if (sizeInBytes > maxSizeInBytes){
							//prompt to continue
							final boolean[] cont = new boolean[]{false};
							Display.getDefault().syncExec(new Runnable(){

								@Override
								public void run() {
									// TODO Auto-generated method stub
									cont[0] = MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
											Messages.UploadChangeLogEngine_UploadSizeDialogTitle, 
											MessageFormat.format(Messages.UploadChangeLogEngine_UploadSizeDialogMessage, sizeInBytes / 1000000.0, maxSizeInBytes/1000000.0));		
								}
								
								
							});
							if (!cont[0]){
								//end
								record.setStatus(Status.ERROR);
								saveRecord(record);
								//delete file
								deletePackageFile();
								throw new PackageToLargeException(Messages.UploadChangeLogEngine_UploadToBig);
							}
							
						}
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
	
	private void deletePackageFile(){
		try{
			Path p = Paths.get(SmartContext.INSTANCE.getFilestoreLocation(), record.getChangeLogZipFile());
			Files.deleteIfExists(p);
		}catch (IOException ex){
			ConnectPlugIn.log("Could not delete ca uploader export file.", ex); //$NON-NLS-1$
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
