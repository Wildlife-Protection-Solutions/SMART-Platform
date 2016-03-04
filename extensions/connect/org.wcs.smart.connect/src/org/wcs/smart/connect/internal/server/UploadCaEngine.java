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
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.type.UUIDBinaryType;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectDatastore;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaProxy;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.internal.server.replication.ChangeLogTableManager;
import org.wcs.smart.connect.internal.server.replication.SyncHistoryManager;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectServerStatus.Status;
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
	 * @param monitor
	 * @throws Exception 
	 */
	public void upload(SmartConnect connect, IProgressMonitor monitor) throws Exception{

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
			monitor.beginTask(Messages.UploadCaEngine_TaskName, 3);
			monitor.subTask(Messages.UploadCaEngine_ConnectSubtaskName);
			
			ConnectServer server = connect.getServer();
			
			ConservationAreaProxy serverInfo = connect.getCaInfo(server.getConservationArea().getUuid());
			monitor.worked(1);
			
			monitor.subTask(Messages.UploadCaEngine_ConfigureSubtask);
			ConnectServerStatus localStatus = null;
			Session s = HibernateManager.openSession();
	
			try{
				s.beginTransaction();
				localStatus = getLocalStatus(s, server.getConservationArea());
		
				//check status
				if (serverInfo != null){
					if (serverInfo.getStatus() == ConservationAreaProxy.Status.DATA){
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
									localStatus.getStatus() == Status.BACKUP){
								//clear this file because we want to start over
								throw new Exception(Messages.UploadCaEngine_7);
							}
							if (localStatus.getLocalFile() == null){
								//we have a problem because we do not have a local file to upload anymore
								throw new Exception(Messages.UploadCaEngine_FileNotFound);
							}else{
								File f = new File(SmartContext.INSTANCE.getFilestoreLocation(), localStatus.getLocalFile());
								if (!f.exists()){
									throw new Exception(Messages.UploadCaEngine_FileDeleted);	
								}
							}
						}
					}
				}
				
				//create db records
				if (serverInfo == null || 
						(serverInfo != null && serverInfo.getStatus() == ConservationAreaProxy.Status.NODATA)){
					//update ca to server (new ca will be created if required; otherwise we update existing)
					if (localStatus != null){
						s.delete(localStatus);
						localStatus = null;
					}
					localStatus = createNewLocalStatus(server, s);
					localStatus.setLocalFile(getExportFilename(server.getConservationArea()));
					s.save(localStatus);
					
					//clean up change log and upload table
					ChangeLogTableManager.INSTANCE.deleteAll(s, server.getConservationArea());
					SyncHistoryManager.INSTANCE.deleteAll(s, server.getConservationArea());
				}
				
				s.getTransaction().commit();
				
				monitor.worked(1);
				
			}catch(Exception ex){
				throw new Exception(Messages.UploadCaEngine_ConfigureError + ex.getMessage(), ex);
			}finally{
				s.close();
			}
			
			//create export package
			try{
				if (localStatus.getUploadUrl() == null){
					monitor.subTask(Messages.UploadCaEngine_ExportCaSubtaskName);
					packageCa(localStatus.getLocalFile(), new SubProgressMonitor(monitor, 1));
					
					localStatus.setStatus(Status.UPLOAD);
					s = HibernateManager.openSession();
					try{
						s.beginTransaction();
						s.saveOrUpdate(localStatus);
						s.getTransaction().commit();
					}finally{
						s.close();
					}
					
					String uploadURL = connect.getCaUploadUrl(localStatus.getUuid(), 
							localStatus.getVersion(), 
							new File(SmartContext.INSTANCE.getFilestoreLocation(), localStatus.getLocalFile()));
					
					localStatus.setUploadUrl(uploadURL);
					s = HibernateManager.openSession();
					try{
						s.beginTransaction();
						s.saveOrUpdate(localStatus);
						s.getTransaction().commit();
					}finally{
						s.close();
					}
				}else{
					monitor.worked(1);
				}
			}catch(Exception ex){
				throw new Exception(Messages.UploadCaEngine_ConfigureError + ex.getMessage(), ex);
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.UploadCaEngine_UploadDialogTitle, Messages.UploadCaEngine_BackgroundProcess);		
				}
				
			});
			
			UploadCaJob job = new UploadCaJob(connect, localStatus);
			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					SmartConnect.UPLOAD_LOCK.release();
				}
			});
			job.schedule();
			
			monitor.done();
		}catch (Exception ex){
			SmartConnect.UPLOAD_LOCK.release();
			throw ex;
		}
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

		Properties prop = new Properties();
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY, StandardRandomStrategy.INSTANCE);
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS, UUIDGenerationStrategy.class.getName());
		UUIDGenerator uuidGenerator = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();
		uuidGenerator.configure(new UUIDBinaryType(), prop, null);
		
		ConnectServerStatus status = new ConnectServerStatus();
	
		UUID version = (UUID) uuidGenerator.generate((SessionImplementor) s, status);
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
		monitor.beginTask(Messages.UploadCaEngine_packingTaskName, 1);
		
		File f = new File(SmartContext.INSTANCE.getFilestoreLocation(), filename);
		if (!f.getParentFile().exists()){
			SmartUtils.createDirectory(f.getParentFile());
		}
	
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("informant_delete_no_prompt", Boolean.TRUE.toString()); //$NON-NLS-1$
		
		ConnectCaExporter exporter = new ConnectCaExporter();
		exporter.export(f, options, new SubProgressMonitor(monitor, 1));
	}
}
