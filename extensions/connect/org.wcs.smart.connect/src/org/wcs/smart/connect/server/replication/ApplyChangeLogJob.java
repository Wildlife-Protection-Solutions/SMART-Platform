package org.wcs.smart.connect.server.replication;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.replication.DerbyMetadataPackager;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.connect.replication.metadata.MetadataPackager;
import org.wcs.smart.connect.replication.metadata.PackageMetadata;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

public class ApplyChangeLogJob extends Job {

	private Path changeLogFile;
	private ConnectServerStatus serverInfo;
	private ConnectSyncHistoryRecord record;
	private EPartService pService;

	private Path tempDirectory;
	private PackageMetadata metadata;
	
	public ApplyChangeLogJob(Path changeLogFile, 
			ConnectServerStatus serverInfo, 
			ConnectSyncHistoryRecord record,
			EPartService pService) {
		super("Applying Change Log File");
		
		this.record = record;
		this.changeLogFile = changeLogFile;
		this.serverInfo = serverInfo;
		this.pService = pService;
	}

	private IStatus cancelled(){
		return org.eclipse.core.runtime.Status.CANCEL_STATUS;
	}
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//TODO: progress monitor
		try{
			//1. check file for updates; if nothing set status to nodata and end
			try{
				unpackValidateFile(changeLogFile, serverInfo);
			}catch (NothingToUpdateException ex){
//				record.setStatus(Status.NODATA);
//				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
			
			// wait for all shells to close
			final boolean[] closed = new boolean[]{false};
			final boolean[] cont = new boolean[]{true};
			while(!closed[0]){
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						closed[0] = true;
						for (Shell s : Display.getDefault().getShells()){
							if ((s.getStyle() & SWT.APPLICATION_MODAL) == SWT.APPLICATION_MODAL ||
								(s.getStyle() & SWT.SYSTEM_MODAL) == SWT.SYSTEM_MODAL ||
								(s.getStyle() & SWT.PRIMARY_MODAL) == SWT.PRIMARY_MODAL){
								closed[0] = false;
							}
						}
						
						if (closed[0]){
							//prompt to apply changes
							if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
									"Apply Changes", 
									"Changes have been downloaded and are ready to apply. Do you want to apply the changes now?")){
								record.setStatus(Status.ERROR);
								record.setErrorString("User cancelled.");
							}
							//if yes then saved all closed editors
							if (!pService.saveAll(true)){
								cont[0] = false;
							}
						}
					}
				});
				if (!closed[0]) Thread.sleep(500);
			}
			if (record.getStatus() == Status.ERROR){
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
			
			if (!cont[0] || pService.getDirtyParts().size() > 0){
				record.setStatus(Status.ERROR);
				record.setErrorString("All dirty parts must be saved before you can download from the server.");
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
			
			/* import file */
			monitor.subTask("Applying Updates");
			if (monitor.isCanceled()) return cancelled();
			applyFile(changeLogFile, serverInfo);
			record.setStatus(Status.DONE);
		}catch (NothingToUpdateException ex){
			record.setStatus(Status.NODATA);
		}catch (Exception ex){
			record.setStatus(Status.ERROR);
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
				record.setErrorString("Unable to apply changes from server due to constraint violation.  This is likely a result of another user creating the same key for an object (ex. same data model category or patrol team key). You will need to delete your item and redownload updates or delete your conservation area and download a fresh copy from SMART Connect." + "\n\n" + constraint.getMessage());   
			}
			record.setErrorString("Unable to apply changes from server:" + "\n\n" + ex.getMessage());
		}finally{
			cleanUp();
		}
		return org.eclipse.core.runtime.Status.OK_STATUS;
	}
	
	private void cleanUp(){
		try{
			Files.delete(changeLogFile);
		}catch (Exception ex){
			ConnectPlugIn.log("Error cleaning up changelog file." + ex.getMessage(), ex);
		}

		try{
			FileUtils.deleteDirectory(tempDirectory.toFile());
		}catch (Exception ex){
			ConnectPlugIn.log("Error cleaning up changelog directory." + ex.getMessage(), ex);
		}
	}
	
	private void unpackValidateFile(Path zipFile, ConnectServerStatus info) throws Exception{
		tempDirectory = SmartUtils.createTemporaryDirectory().toPath();		
		
		//unzip file
		ZipUtil.unzipFolder(zipFile.toFile(), tempDirectory.toFile());

		Path metadataFile = null;
		Path changeLogFile = null;
		
		try(DirectoryStream<Path> files = Files.newDirectoryStream(tempDirectory)){
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
		metadata = MetadataPackager.INSTANCE.readMetadata(metadataFile);
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
		
	}
	/*
	 * applies change log file
	 */
	private void applyFile(Path zipFile, ConnectServerStatus info) throws NothingToUpdateException, Exception{
		
		Path filestoreDir = tempDirectory.resolve(ConnectSyncHistoryRecord.PACKAGE_FILESTORE_DIR);
			
		//lock database
		Session session = HibernateManager.lockDatabase();
		try{
			session.beginTransaction();
			
			// disable replication in db so we don't log items twice
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
	}
	
	private void applyChangeLog(Path changelogFile, Path changelogFilestore, Session session) throws Exception{
		DerbyChangeLogDeserializer processor = new DerbyChangeLogDeserializer(changelogFile, changelogFilestore);
		processor.processFile(session);
	}

}
