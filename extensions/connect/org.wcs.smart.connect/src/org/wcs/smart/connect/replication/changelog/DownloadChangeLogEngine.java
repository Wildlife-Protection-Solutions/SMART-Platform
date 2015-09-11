package org.wcs.smart.connect.replication.changelog;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.ca.in.CaImporter;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaInfo;
import org.wcs.smart.connect.api.model.WorkItemStatus;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.replication.DerbyMetadataPackager;
import org.wcs.smart.connect.replication.metadata.MetadataPackager;
import org.wcs.smart.connect.replication.metadata.PackageMetadata;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.UserNamePasswordDialog;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DownloadChangeLogEngine {
	public static final long MAX_WAIT = 5 * 60 * 1000000000l;  //5 minutes in nano seconds
	public static final long STATUS_RETY_WAIT = 1000;
	
	private SmartConnect connect;
	
	public DownloadChangeLogEngine(SmartConnect connect){
		this.connect = connect;
	}
	
	public boolean download(IProgressMonitor monitor) throws Exception{
		monitor.beginTask("Download & Install Data Updates", 4);
		
		if (!UploadChangeLogEngine.UPLOAD_LOCK.tryAcquire()){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), 
							"Error", "Another process is already uploading or downloading changes to SMART Connect.  You must wait until that process is completed to download a change log.");		
				}
				
			});	
			return false;
		}
		try{
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		if (SmartDB.isMultipleAnalysis()) throw new Exception("Cross-ca analysis can not be syncronized with server.");
	
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		ConnectServerStatus serverStatus = (ConnectServerStatus) s.get(ConnectServerStatus.class, ca.getUuid());
		s.getTransaction().commit();
		s.close();
		
		
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
			if ( current - start > MAX_WAIT) throw new Exception("Timed out waiting for export to process.");
			Thread.sleep(STATUS_RETY_WAIT);
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
		
		/* import file */
		monitor.subTask("Applying Updates");
		if (monitor.isCanceled()) return false;
		
		try{
			applyFile(p, serverStatus);
		}catch (Exception ex){
			throw new Exception("Unable to apply change log file. " + ex.getMessage(), ex);
		}
		
		monitor.done();
		}finally{
			UploadChangeLogEngine.UPLOAD_LOCK.release();
		}
		return true;
	}
	
	
	private void applyFile(Path zipFile, ConnectServerStatus info) throws Exception{
		
		//create a temporary location to unzip file
		Path tempDir = SmartUtils.createTemporaryDirectory().toPath();		
		try{
			//unzip file
			ZipUtil.unzipFolder(zipFile.toFile(), tempDir.toFile());

			Path metadataFile = null;
			Path changeLogFile = null;
			try(DirectoryStream<Path> files = Files.newDirectoryStream(tempDir)){
				for (Path file : files){
					if (file.getFileName().toString().endsWith(".changelog.metadata")){
						metadataFile = file;
					}else if (file.getFileName().toString().endsWith(".changelog")){
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
			if (metadata.getServerRevision() <= info.getServerRevision() ){
				throw new Exception("Invalid server revision (the local server revision is less than or equal to the package server revision).  Cannot apply change log package");
			}
			
			Session session = HibernateManager.lockDatabase();
			try{
				session.beginTransaction();
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
				applyChangeLog(session, changeLogFile);
				
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
			}
		
		}finally{
			FileUtils.deleteDirectory(tempDir.toFile());
		}
	}
	
	private void applyChangeLog(Session session, final Path file) throws Exception{
		DerbyChangeLogDeserializer processor = new DerbyChangeLogDeserializer(file);
		processor.processFile(session);
	}
	
}