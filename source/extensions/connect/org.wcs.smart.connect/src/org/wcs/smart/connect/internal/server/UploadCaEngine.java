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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectDatastore;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaProxy;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.internal.server.replication.ChangeLogTableManager;
import org.wcs.smart.connect.internal.server.replication.SyncHistoryManager;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectServerStatus.Status;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Upload Conservation Area engine that exports
 * the Conservation Area, uploading it to the server.
 * 
 * @author Emily
 *
 */
public class UploadCaEngine {
	/**
	 * Creates local database records 
	 * and starts a job to upload the export to the server  
	 * 
	 * @param sc
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor
	 * @throws Exception 
	 */
	public void upload(SmartConnect connect, IProgressMonitor monitor) throws Exception{

		SubMonitor progress = SubMonitor.convert(monitor, Messages.UploadCaEngine_TaskName, 3);
		
		if (!SmartConnect.UPLOAD_LOCK.tryAcquire()){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), 
							Messages.UploadCaEngine_ErrorDialogTitle, Messages.UploadCaEngine_AlreadyProcessing);		
				}
			});	
			return;
		}
		try{
			progress.subTask(Messages.UploadCaEngine_ConnectSubtaskName);
			
			ConnectServer server = connect.getServer();
			
			ConservationAreaProxy serverInfo = connect.getCaInfo(server.getConservationArea().getUuid());
			progress.worked(1);
			
			progress.subTask(Messages.UploadCaEngine_ConfigureSubtask);
			ConnectServerStatus localStatus = null;
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try {
					localStatus = getLocalStatus(s, server.getConservationArea());
			
					if (localStatus == null && serverInfo != null ) {
						//conservation area already exists on server
						throw new Exception(Messages.UploadCaEngine_CaAlreadyExists);
					}
					//check status
					if (serverInfo != null){
						if (serverInfo.getStatus() == ConservationAreaProxy.Status.ERROR){
							throw new Exception(Messages.UploadCaEngine_unknownState);
						}
						if (serverInfo.getStatus() == ConservationAreaProxy.Status.DATA){
							
							if (localStatus.getStatus() == Status.UPLOAD) {
								//was uploading but done on connect see if we can re-link the two
								if (localStatus.getVersion().equals(serverInfo.getVersion()) && localStatus.getServerRevision() < 0) {
									//can link the two
									localStatus.setStatus(Status.DONE);
									saveStatus(localStatus);
									showMessage(Messages.UploadCaEngine_UploadComplete);
									SmartConnect.UPLOAD_LOCK.release();
									return;
								}
							}
							
							throw new Exception(Messages.UploadCaEngine_CaAlreadyExists);
						}
						
						if (serverInfo.getStatus() == ConservationAreaProxy.Status.UPLOADING){
							//somebody is uploading data;  is it us (check versions)?
							if (localStatus == null 
									|| !(localStatus.getVersion().equals(serverInfo.getVersion()))){
								throw new Exception(Messages.UploadCaEngine_AlreadyUploading);
							}
							//continue using local file
							if (localStatus != null){
								if (localStatus.getStatus() == Status.ERROR || 
									localStatus.getStatus() == Status.CANCEL ||
										localStatus.getStatus() == Status.BACKUP){
									//clear this file because we want to start over
									throw new Exception(Messages.UploadCaEngine_7);
								}
								
								
								//lets see what the state is on the server
								WorkItemStatus serverStatus = connect.getWorkItemStatus(localStatus.getUploadUrl());
								if (serverStatus.getStatus() == WorkItemStatus.Status.COMPLETE) {
									//should be done try again
									showMessage(Messages.UploadCaEngine_InconsistantStatus);
									SmartConnect.UPLOAD_LOCK.release();
									return;
								}else if (serverStatus.getStatus() == WorkItemStatus.Status.PROCESSING) {
									//waiting for processing
								
								}else if (serverStatus.getStatus() == WorkItemStatus.Status.UPLOADING) {
									//continue with upload
									if (localStatus.getLocalFile() == null){
										//we have a problem because we do not have a local file to upload anymore
										throw new Exception(Messages.UploadCaEngine_FileNotFound);
									}else{
										Path f = Paths.get(SmartContext.INSTANCE.getFilestoreLocation())
												.resolve(localStatus.getLocalFile());
										if (!Files.exists(f)){
											throw new Exception(Messages.UploadCaEngine_FileDeleted);	
										}
									}
								}else {
									throw new Exception(Messages.UploadCaEngine_7);
								}
							}
						}
					}
					//create db records
					if (serverInfo == null || 
						(serverInfo != null && serverInfo.getStatus() == ConservationAreaProxy.Status.NODATA)){
						//update ca to server (new ca will be created if required; otherwise we update existing)
						if (localStatus != null){
							s.remove(localStatus);
							localStatus = null;
						}
						localStatus = createNewLocalStatus(server, s);
						localStatus.setLocalFile(getExportFilename(server.getConservationArea()));
						s.persist(localStatus);
						
						//clean up change log and upload table
						ChangeLogTableManager.INSTANCE.deleteAll(s, server.getConservationArea());
						SyncHistoryManager.INSTANCE.deleteAll(s, server.getConservationArea());
					}
					
					s.getTransaction().commit();
					
					progress.worked(1);
					
				}catch(Exception ex){
					throw new Exception(Messages.UploadCaEngine_ConfigureError + ex.getMessage(), ex);
				}
			}
			
			//create export package
			try{
				if (localStatus.getUploadUrl() == null){
					progress.subTask(Messages.UploadCaEngine_ExportCaSubtaskName);
					packageCa(localStatus.getLocalFile(), progress.split(1));
					
					localStatus.setStatus(Status.UPLOAD);
					saveStatus(localStatus);

					DerbyReplicationManager.INSTANCE.clearCachedReplicationState();
					
					String uploadURL = connect.getCaUploadUrl(localStatus.getUuid(), 
							localStatus.getVersion(), 
							Paths.get(SmartContext.INSTANCE.getFilestoreLocation()).resolve(localStatus.getLocalFile()));
					
					localStatus.setUploadUrl(uploadURL);
					saveStatus(localStatus);
				}else{
					progress.setWorkRemaining(0);
				}
			}catch(OperationCanceledException ex) {
				//cancelled by user
				localStatus.setStatus(Status.CANCEL);
				saveStatus(localStatus);
				SmartConnect.UPLOAD_LOCK.release();
				showMessage(Messages.UploadCaEngine_CanceledMessage);				
				return;
			}catch(Exception ex){
				throw new Exception(Messages.UploadCaEngine_ConfigureError + (ex.getMessage() == null ? "" : ex.getMessage()), ex); //$NON-NLS-1$
			}
			
			showMessage(Messages.UploadCaEngine_BackgroundProcess);
			
			UploadCaJob job = new UploadCaJob(connect, localStatus);
			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					SmartConnect.UPLOAD_LOCK.release();
				}
			});
			job.schedule();
			
		}catch (Exception ex){
			SmartConnect.UPLOAD_LOCK.release();
			throw ex;
		}
	}
	
	private void saveStatus(ConnectServerStatus localStatus) {
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				if (localStatus.getUuid() == null) {
					s.persist(localStatus);
				}else {
					s.merge(localStatus);
				}
				s.getTransaction().commit();
			}catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}
	}
	private void showMessage(String message) {
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.UploadCaEngine_UploadDialogTitle, message);		
			}
			
		});
	}
	/**
	 * Re-acquires a lock and continues and upload job that was terminated early.  Will not re-create upload package.
	 */
	public void continueUpload(SmartConnect connect, ConnectServerStatus localStatus){
		if (localStatus.getStatus() != ConnectServerStatus.Status.UPLOAD) throw new IllegalStateException(
				MessageFormat.format(Messages.UploadCaEngine_StatusFailed, ConnectServerStatus.Status.UPLOAD.name()));

		if (!SmartConnect.UPLOAD_LOCK.tryAcquire()){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), 
							Messages.UploadCaEngine_ErrorDialogTitle, Messages.UploadCaEngine_AlreadyProcessing);		
				}
			});	
			return;
		}
		try{
			UploadCaJob job = new UploadCaJob(connect, localStatus);
			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					SmartConnect.UPLOAD_LOCK.release();
				}
			});
			job.schedule();
		}catch (Exception ex){
			SmartConnect.UPLOAD_LOCK.release();
			throw ex;
		}
	}
	
	/**
	 * The export filename.
	 * @return
	 */
	private String getExportFilename(ConservationArea ca){
		return ConnectDatastore.CONNECT_FILESTORE_DIR 
				+ File.separator
				+ ConnectDatastore.REPLICATION_FILESTORE_DIR
				+ File.separator
				+ "sc_" +UuidUtils.uuidToString(ca.getUuid())+ "_" + System.nanoTime() + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Get the local status object from the database for the current conservation area
	 * @param s
	 * @return
	 */
	private ConnectServerStatus getLocalStatus(Session s, ConservationArea ca){
		return (ConnectServerStatus) s.get(ConnectServerStatus.class, ca.getUuid());
	}
	
	/**
	 * Creates a new local status object
	 * @param server
	 * @param s
	 * @return
	 */
	private ConnectServerStatus createNewLocalStatus(ConnectServer server, Session s){

//		Properties prop = new Properties();
//		prop.put(UUIDGenerator.UUID_GEN_STRATEGY, StandardRandomStrategy.INSTANCE);
//		prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS, UUIDGenerationStrategy.class.getName());
//		UUIDGenerator uuidGenerator = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();
//		uuidGenerator.configure(new UUIDBinaryType(), prop, null);
		
		ConnectServerStatus status = new ConnectServerStatus();
	
		UUID version = (UUID) UuidUtils.generateUuid((SessionImplementor) s);
		status.setVersion(version);
	
		status.setServerRevision(-1l);
		status.setServer(server);
		status.setConservationArea(server.getConservationArea());
		status.setStatus(ConnectServerStatus.Status.BACKUP);
		return status;

	}
	
	
	/**
	 * Export the current conservation area to a temporary file.
	 * 
	 * This needs to lock the database so this must prevent the
	 * users from doing anything else while processing
	 * @throws Exception 
	 * 
	 */
	private void packageCa(String filename, IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor, Messages.UploadCaEngine_packingTaskName, 1);
		
		Path f = Paths.get(SmartContext.INSTANCE.getFilestoreLocation()).resolve(filename);
		if (!Files.exists(f.getParent())){
			SmartUtils.createDirectory(f.getParent());
		}
	
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("informant_delete_no_prompt", Boolean.TRUE.toString()); //$NON-NLS-1$
		
		ConnectCaExporter exporter = new ConnectCaExporter();
		exporter.export(f, options, progress.split(1));
	}
}
