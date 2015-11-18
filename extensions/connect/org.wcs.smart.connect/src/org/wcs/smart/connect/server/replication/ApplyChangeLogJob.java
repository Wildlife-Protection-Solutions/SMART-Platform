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

import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ErrorEditorPart;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.LogoutHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord.Status;
import org.wcs.smart.connect.replication.DerbyMetadataPackager;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.connect.replication.metadata.MetadataPackager;
import org.wcs.smart.connect.replication.metadata.PackageMetadata;
import org.wcs.smart.hibernate.ConservationAreaConfiguration;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.UserNamePasswordDialog;
import org.wcs.smart.util.E3Utils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * This job validates the a change log package.  If it contains
 * valid changes, the user is prompted to apply the changes then
 * the changes are applied while.  Once changes are applied the user
 * is re-validated, and the ui refreshed. 
 * 
 * @author Emily
 *
 */
public class ApplyChangeLogJob extends Job {

	private Path changeLogPackageFile;
	private Path changeLogFile;
	private ConnectServerStatus serverInfo;
	private ConnectSyncHistoryRecord record;

	private Path tempDirectory;
	private PackageMetadata metadata;
	private IEventBroker eventBroker;
	
	public ApplyChangeLogJob(Path changeLogPackageFile, 
			ConnectServerStatus serverInfo, 
			ConnectSyncHistoryRecord record){
		super("Applying Change Log File");
		
		this.record = record;
		this.changeLogPackageFile = changeLogPackageFile;
		this.serverInfo = serverInfo;

	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Applying change to local database.", 3);
		try{
			monitor.subTask("validating change package");
			//1. check file for updates; if nothing set status to nodata and end
			try{
				unpackValidateFile();
			}catch (NothingToUpdateException ex){
				record.setStatus(Status.NODATA);
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
			monitor.worked(1);

			monitor.subTask("saving current work");
			// wait for all shells to close
			final boolean[] closed = new boolean[]{false};

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

						eventBroker = (IEventBroker) PlatformUI.getWorkbench().getService(IEventBroker.class);
						
						if (closed[0]){
							promptToApplyAndApply();
						}
					}
				});
				if (!closed[0]) Thread.sleep(500);
			}
		}catch (Exception ex){
			processError(ex);
		}finally{
			monitor.done();
			cleanUp();
		}
		return org.eclipse.core.runtime.Status.OK_STATUS;
	}
	
	private void processError(Exception ex){
		if (ex instanceof NothingToUpdateException){
			record.setStatus(Status.ERROR);
			return;
		}
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
	}
	
	//must be called from the UI thread
	private boolean promptToApplyAndApply(){
		//prompt to apply changes
		if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
				"SMART Connect - Apply Changes", 
				"Changes have been downloaded and are ready to apply. Do you want to apply the changes now?")){
			record.setStatus(Status.ERROR);
			record.setErrorString("User cancelled.");
			return false;
		}
		
		final EPartService pService = (EPartService) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(EPartService.class);
		
		//if yes then saved all closed editors
		pService.saveAll(true);
		
		if (pService.getDirtyParts().size() > 0){
			record.setStatus(Status.ERROR);
			record.setErrorString("All dirty parts must be saved before you can download from the server.");
			return false;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
		
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask("Applying Updates", 4);
					try{
						monitor.worked(1);
						monitor.subTask("updating database");
						applyFile();					
						record.setStatus(Status.DONE);
						monitor.worked(1);
						
						monitor.subTask("validating user");
						if (!checkUser()){
							return;
						}
						monitor.worked(1);
						
						monitor.subTask("refreshing ui");
						refreshUi(pService);
						monitor.worked(1);
						
						monitor.done();
					}catch (Exception ex){
						processError(ex);
					}	
				}
			});
			
		}catch (Exception ex){
			record.setStatus(Status.ERROR);
			record.setErrorString("Error applying update: " + ex.getMessage());
			return false;
		}
		return true;
		
	}
	
	private void cleanUp(){
		try{
			Files.delete(changeLogPackageFile);
		}catch (Exception ex){
			ConnectPlugIn.log("Error cleaning up changelog file." + ex.getMessage(), ex);
		}

		try{
			FileUtils.deleteDirectory(tempDirectory.toFile());
		}catch (Exception ex){
			ConnectPlugIn.log("Error cleaning up changelog directory." + ex.getMessage(), ex);
		}
	}
	
	private void unpackValidateFile() throws Exception{
		tempDirectory = SmartUtils.createTemporaryDirectory().toPath();		
		
		//unzip file
		ZipUtil.unzipFolder(changeLogPackageFile.toFile(), tempDirectory.toFile());

		Path metadataFile = null;
		
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
		if (!serverInfo.getUuid().equals(metadata.getConservationArea())){
			throw new Exception("Conservation area uuids do not match");
		}
		//check version
		if (!serverInfo.getVersion().equals(metadata.getVersion())){
			throw new Exception("Conservation area versions do not match");
		}
			
		//check revision
		if (metadata.getServerRevision().longValue() == serverInfo.getServerRevision().longValue()){
			throw new NothingToUpdateException("Local copy is up to date.");
		}
		if (metadata.getServerRevision().longValue() <= serverInfo.getServerRevision().longValue() ){
			throw new Exception("Invalid server revision (the local server revision is less than or equal to the package server revision).  Cannot apply change log package");
		}
		
	}
	/*
	 * applies change log file
	 */
	private void applyFile() throws NothingToUpdateException, Exception{
		
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
			applyChangeLog(filestoreDir, session);
			
			//update server revision
			serverInfo.setServerRevision(metadata.getServerRevision());
			session.saveOrUpdate(serverInfo);
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
	
	private void applyChangeLog(Path changelogFilestore, Session session) throws Exception{
		DerbyChangeLogDeserializer processor = new DerbyChangeLogDeserializer(changeLogFile, changelogFilestore, record.getConservationArea());
		processor.processFile(session);
	}
	
	
	/*
	 * validates that the current user still has same username/password
	 * and permissions after download sync.  If not then the
	 * application must restart or ask user to re-enter credentials.
	 */
	private boolean checkUser(){
		if (SmartDB.getCurrentEmployee() == null) return true;
		final Employee currentEmployee = SmartDB.getCurrentEmployee();
		Session s = HibernateManager.openSession();
		try{
			final Employee afterDownload = (Employee) s.createCriteria(Employee.class)
				.add(Restrictions.eq("uuid", currentEmployee.getUuid()))
				.uniqueResult();
			final boolean[] cont = new boolean[]{true};
			if (afterDownload == null ||
					afterDownload.getSmartUserLevel() == null ||
					!afterDownload.getSmartUserLevel().equals(currentEmployee.getSmartUserLevel())){
				//current employee no longer exists; 
				//or no longer a smart user
				//or user level has changed
				//logout
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openError(Display.getDefault().getActiveShell(), "SMART Connect - Restart", "Your SMART user account has been removed or modified.  The system will restart and you must log back in.");
						(new LogoutHandler()).execute();
						cont[0] = false;
					}
				});
			}
			if (!cont[0]) return false;
			
			//ensure usernames & passwords are the same
			//we are comparing the hashed values here; not the unhashed values
			//this is the best we can do but will not be perfect
			if (!afterDownload.getSmartUserId().equals(currentEmployee.getSmartUserId()) ||
				!afterDownload.getSmartPassword().equals(currentEmployee.getSmartPassword())){
				//the username or password are different we need to prompt for the new password
				//if they cannot provide the necessary info, we need to logout
				Display.getDefault().syncExec(new Runnable(){
	
					@Override
					public void run() {
						boolean ok = false;
						String password = null;
						while(!ok){
							UserNamePasswordDialog dialog = new UserNamePasswordDialog(Display.getCurrent().getActiveShell(), 
								"SMART Connect - User Credentials", "Your SMART account was changed, you need to re-enter your credentials before you can continue", "OK");
							if (dialog.open() == Window.CANCEL){
								//logout
								(new LogoutHandler()).execute();
								cont[0] = false;
								return;
							}else{
								//validate
								String username = dialog.getUserName();
								password = dialog.getPassword();
								
								String error = null;
								try{
									Employee validated = HibernateManager.validateUser(username, password, afterDownload.getConservationArea());
									if(validated == null || 
										!validated.getUuid().equals(afterDownload.getUuid())){
										error = "Invalid username/password.";
									}
								}catch (Exception ex){
									ConnectPlugIn.log(ex.getMessage(), ex);
									error = ex.getMessage();
								}
								
								if (error != null){
									MessageDialog.openError(Display.getCurrent().getActiveShell(), "Login Error", error);
								}else{
									ok = true;
								}
							}
						}
	
						//update configuration to use new employee
						ConservationAreaConfiguration cc = new ConservationAreaConfiguration(
								Collections.singleton(currentEmployee.getConservationArea()), 
								Collections.singleton(afterDownload), s);
						SmartDB.setConservationAreaConfiguration(afterDownload, 
								password, currentEmployee.getConservationArea(), cc);
					}
				});
			}
			if (!cont[0]) return false;
			return true;
		}finally{
			s.close();
		}
		
	}
	private void refreshUi(final EPartService pService) {
		eventBroker.post(SmartPlugIn.E4_DATABASE_CHANGED_EVENT, null);

		//find all editors, close and reopen
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				Collection<MPart> parts = pService.getParts();
				for (MPart part : parts){
					if (E3Utils.isCompatibilityEditor(part)){
						PartState state = PartState.CREATE;
						if (pService.isPartVisible(part)){
							state = PartState.VISIBLE;
						}

						pService.hidePart(part, false);
						try{
							pService.showPart(part, state);
							Object e3part = E3Utils.getSourceObject(part);
//							if (e3part instanceof EditorPart && )
							if (e3part instanceof ErrorEditorPart){
//								System.out.println(((EditorPart) e3part).getEditorSite().getId());
								pService.hidePart(part, true);
							}
						}catch (Throwable ex){
							//eat me; likely the input behind the part was removed
							pService.hidePart(part, true);
						}
					}
				}
			}
		});
	}

}
