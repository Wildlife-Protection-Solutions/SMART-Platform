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

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.replication.DerbyMetadataPackager;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.connect.replication.metadata.MetadataPackager;
import org.wcs.smart.connect.replication.metadata.PackageMetadata;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Engine to download a change log file from the server and
 * apply it to the local database.
 * 
 * @author Emily
 *
 */
public class DownloadChangeLogEngine {
	
	private SmartConnect connect;
	
	public DownloadChangeLogEngine(SmartConnect connect){
		this.connect = connect;
	}
	
	/**
	 * Download and apply updates.
	 * 
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	public boolean downloadInstall(final EPartService pService, IProgressMonitor monitor) throws Exception{

		monitor.beginTask("Download && Install Data Updates", 4);
		
		//acquire lock
		if (!SmartConnect.UPLOAD_LOCK.tryAcquire()){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), 
							"Error", "Another process is already uploading or downloading changes to SMART Connect.  You must wait until that process is completed to download a change log.");		
				}
				
			});	
			return false;
		}

		
		ConnectSyncHistoryRecord record = null;
		try{
			//prompt to save or close all dirty editors
			final boolean[] cont = new boolean[]{true};
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (!pService.saveAll(true)){
						cont[0] = false;
					}		
				}
			});
			if (!cont[0] || pService.getDirtyParts().size() > 0){
				throw new Exception("All dirty parts must be saved before you can download from the server.");
			}
			
			Session session = HibernateManager.openSession();
			try{
				if (!DerbyReplicationManager.INSTANCE.isReplicationEnabled(session)){
					throw new Exception("Replication not enabled.  Cannot download changes from server.");
				}
			}finally{
				session.close();
			}

			//create sync history record for database
			record = SyncHistoryManager.INSTANCE.create(connect.getServer().getConservationArea(), connect.getServer(), ConnectSyncHistoryRecord.Type.DOWNLOAD);
			
			/* the ca info */
			ConservationArea ca = SmartDB.getCurrentConservationArea();
			if (SmartDB.isMultipleAnalysis()) throw new Exception("Cross-ca analysis can not be syncronized with server.");
			
			ConnectServerStatus serverStatus = null;
			Session s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				serverStatus = (ConnectServerStatus) s.get(ConnectServerStatus.class, ca.getUuid());
				s.update(record);
				s.getTransaction().commit();
			}finally{
				s.close();
			}
			
			if (serverStatus == null){
				throw new Exception("SMART Connect server not found.");
			}
			
			/* request ca */
			monitor.subTask("Initializing download");
			String statusUrl = connect.startChangeLogDownload(serverStatus.getUuid(), serverStatus.getVersion(), serverStatus.getServerRevision());
			monitor.worked(1);
			if (monitor.isCanceled()) return false;
			
			/* wait for ca export to be created */
			monitor.subTask("Waiting for CONNECT server to create package");
			Long start = System.nanoTime();
			WorkItemStatus status = null ;
			while(status == null || (status.getStatus() == WorkItemStatus.Status.PROCESSING || 
					status.getStatus() == WorkItemStatus.Status.PROCESSING)){
				Long current = System.nanoTime();
				if ( current - start > connect.getServer().getWaitProcessingTime() * 1000000) throw new Exception("Timed out waiting for export to process.");
				Thread.sleep(connect.getServer().getRetryWaitTime());
				try{
					status = connect.getWorkItemStatus(statusUrl);
				}catch (Exception ex){
					ConnectPlugIn.log("Error requesting ca update download status.", ex);
				}
				if (monitor.isCanceled()) return false;
			}
			monitor.worked(1);
			
			if (status.getStatus() == WorkItemStatus.Status.ERROR){
				throw new Exception("Error downloading change log package:\n\n" + SmartConnect.parseErrorMessage(status.getMessage()));
			}
	
			/* download file */
			monitor.subTask("Downloading Upload Package");
			String message = status.getMessage();
			JsonNode nd = (new ObjectMapper()).readTree(message);
			String downloadUrl = nd.get("file_url").asText();
			if (monitor.isCanceled()) return false;
			Path p = connect.downloadFileFromUrl(downloadUrl);
			monitor.worked(1);
			
			try{
				/* import file */
				monitor.subTask("Applying Updates");
				if (monitor.isCanceled()) return false;
				applyFile(p, serverStatus);
				record.setStatus(Status.DONE);
			}catch (NothingToUpdateException ex){
				record.setStatus(Status.NODATA);
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);

				//look for unique constraint errors; likely due to duplicate keys 
				Throwable parent = ex;
				Exception constraint = null;
				while(parent != null){
					if (parent instanceof SQLIntegrityConstraintViolationException){
						constraint = (Exception) parent;
						break;
					}
					if (parent == parent.getCause()) break;	//TODO: test this
					parent = parent.getCause();
				}
				if (constraint != null){
					throw new Exception("Unable to apply changes from server due to constraint violation.  This is likely a result of another user creating the same key for an object (ex. same data model category or patrol team key). You will need to delete your item and redownload updates or delete your conservation area and download a fresh copy from SMART Connect." + "\n\n" + constraint.getMessage());   
				}
				throw new Exception("Unable to apply changes from server:" + "\n\n" + ex.getMessage(), ex);
			}finally{
				Files.delete(p);
			}

			s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				s.saveOrUpdate(record);
				s.getTransaction().commit();
			}finally{
				s.close();
			}
			
			monitor.done();
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
				ConnectPlugIn.log("Could not set download record status to error: " + ex2.getMessage(), ex2);
			}
			throw ex;
		}finally{
			SmartConnect.UPLOAD_LOCK.release();
		}
		return true;
	}

	/*
	 * applies change log file
	 */
	private void applyFile(Path zipFile, ConnectServerStatus info) throws NothingToUpdateException, Exception{
		
		//create a temporary location to unzip file
		Path tempDir = SmartUtils.createTemporaryDirectory().toPath();		
		try{
			//unzip file
			ZipUtil.unzipFolder(zipFile.toFile(), tempDir.toFile());

			Path metadataFile = null;
			Path changeLogFile = null;
			Path filestoreDir = tempDir.resolve(ConnectSyncHistoryRecord.PACKAGE_FILESTORE_DIR);
			try(DirectoryStream<Path> files = Files.newDirectoryStream(tempDir)){
				for (Path file : files){
					if (file.getFileName().toString().endsWith(ConnectSyncHistoryRecord.METADATA_FILE_SUFFIX)){
						metadataFile = file;
					}else if (file.getFileName().toString().endsWith(ConnectSyncHistoryRecord.CHANGELOG_FILE_SUFFIX)){
						changeLogFile = file;
					}
				}
			}
			if (metadataFile == null){
				throw new Exception("Invalid sync package, no metadata file provided.");
			}
			if (changeLogFile == null){
				throw new Exception("Invalid sync package, no change log file provided.");
			}
			//check metadata
			PackageMetadata metadata = MetadataPackager.INSTANCE.readMetadata(metadataFile);
			//check ca
			if (!info.getUuid().equals(metadata.getConservationArea())){
				throw new Exception("Conservation area uuids do not match");
			}
			//check version
			if (!info.getVersion().equals(metadata.getVersion())){
				throw new Exception("Conservation area versions do not match");
			}
			
			//check revision
			if (metadata.getServerRevision().longValue() == info.getServerRevision().longValue()){
				throw new NothingToUpdateException("Local copy is up to date.");
			}
			if (metadata.getServerRevision().longValue() <= info.getServerRevision().longValue() ){
				throw new Exception("Invalid server revision (the local server revision is less than or equal to the package server revision).  Cannot apply change log package");
			}
			
			//close all editors
			Session session = HibernateManager.lockDatabase();
			try{
				session.beginTransaction();
				
				//disable replication
				/* disable replication in db so we don't log items twice */
				DerbyReplicationManager.INSTANCE.disableReplication(session);
				
				//check plugin versions
				HashMap<String, String> localPlugins = DerbyMetadataPackager.INSTANCE.getLocalPluginVersions(session);
				
				for(String pluginid : metadata.getPluginVersions().keySet()){
					String version = metadata.getPluginVersion(pluginid);
					String dbVersion = localPlugins.get(pluginid);
					if (dbVersion == null){
						throw new Exception("The connect server has plugin " + pluginid + " installed.  The local SMART Desktop does not.  You must install this plugin before you can apply connect server change log package.");
					}
					if (!version.equals(dbVersion)){
						throw new Exception("The connect server has different version for plugin " + pluginid + ". (client: " + dbVersion + " / server:" + version + ".  Versions must be the same to apply connect server change log.");
					}
				}
				//apply change log
				applyChangeLog(changeLogFile, filestoreDir, session);
				
				//update server revision
				info.setServerRevision(metadata.getServerRevision());
				session.saveOrUpdate(info);
				session.getTransaction().commit();
			}catch(Exception ex){
				if (session.getTransaction().isActive()){
					session.getTransaction().rollback();
				}
				throw ex;
			}finally{
				HibernateManager.unlockDatabase();
				session.close();
				
				session = HibernateManager.openSession();
				try{
					session.beginTransaction();
					DerbyReplicationManager.INSTANCE.enableReplication(session);
					session.getTransaction().commit();
				}catch(Exception ex){
					//replication could not be re-enabled.  This needs to kill the
					//application and restart
					ConnectPlugIn.displayLog("Replication could not be enabled after applying changes.  This application will restart.", ex);
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							PlatformUI.getWorkbench().restart();
						}});
				}finally{
					session.close();
				}
			}
		
		}finally{
			FileUtils.deleteDirectory(tempDir.toFile());
		}
	}
	
	private void applyChangeLog(Path changelogFile, Path changelogFilestore, Session session) throws Exception{
		DerbyChangeLogDeserializer processor = new DerbyChangeLogDeserializer(changelogFile, changelogFilestore);
		processor.processFile(session);
		
	}
	
}