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
import java.text.MessageFormat;
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
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.wcs.smart.connect.internal.Messages;
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
		super(Messages.ApplyChangeLogJob_JobName);
		
		this.record = record;
		this.changeLogPackageFile = changeLogPackageFile;
		this.serverInfo = serverInfo;

	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		boolean isLoggedIn = SmartDB.getCurrentEmployee() != null;
		
		monitor.beginTask(Messages.ApplyChangeLogJob_TaskName, 3);
		try{
			monitor.subTask(Messages.ApplyChangeLogJob_validateSubTask);
			//1. check file for updates; if nothing set status to nodata and end
			try{
				unpackValidateFile();
			}catch (NothingToUpdateException ex){
				record.setStatus(Status.NODATA);
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
			monitor.worked(1);

			monitor.subTask(Messages.ApplyChangeLogJob_SaveSubTask);
			// wait for all shells to close
			final boolean[] closed = new boolean[]{false};

			while(!closed[0]){
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						closed[0] = true;
						if (isLoggedIn){
							//otherwise we are running from login screen and we don't care about dialogs
							for (Shell s : Display.getDefault().getShells()){
								if ((s.getStyle() & SWT.APPLICATION_MODAL) == SWT.APPLICATION_MODAL ||
										(s.getStyle() & SWT.SYSTEM_MODAL) == SWT.SYSTEM_MODAL ||
										(s.getStyle() & SWT.PRIMARY_MODAL) == SWT.PRIMARY_MODAL){
									closed[0] = false;
								}
							}
						}

						eventBroker = (IEventBroker) PlatformUI.getWorkbench().getService(IEventBroker.class);
						
						if (closed[0]){
							promptToApplyAndApply(isLoggedIn);
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
			record.setErrorString(Messages.ApplyChangeLogJob_ConstraintError + "\n\n" + constraint.getMessage());    //$NON-NLS-1$
		}
		record.setErrorString(Messages.ApplyChangeLogJob_UnknownError + "\n\n" + ex.getMessage()); //$NON-NLS-1$
	}
	
	//must be called from the UI thread
	private boolean promptToApplyAndApply(final boolean isLoggedIn){
		EPartService pService = null;
		if (isLoggedIn){
			//prompt to apply changes
			if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
					Messages.ApplyChangeLogJob_ApplyTitle, 
					Messages.ApplyChangeLogJob_ApplyMessage)){
				record.setStatus(Status.ERROR);
				record.setErrorString(Messages.ApplyChangeLogJob_Cancelled);
				return false;
			}
			
			pService = (EPartService) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(EPartService.class);
			
			//if yes then saved all closed editors
			if (!pService.saveAll(true)){
				//cannot do this check as there may be parts that do not need saving; just closing
				//informant editor for example
//			if (pService.getDirtyParts().size() > 0){
				record.setStatus(Status.ERROR);
				record.setErrorString(Messages.ApplyChangeLogJob_DirtyPartsNotClosedError);
				return false;
			}
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
		final EPartService fpServer = pService;
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.ApplyChangeLogJob_ApplyTaskName, 4);
					try{
						monitor.worked(1);
						monitor.subTask(Messages.ApplyChangeLogJob_UploadDbSubtask);
						applyFile();					
						record.setStatus(Status.DONE);
						monitor.worked(1);
						
						if (isLoggedIn){
							monitor.subTask(Messages.ApplyChangeLogJob_ValidateUserSubTask);
							if (!checkUser()){
								return;
							}
							monitor.worked(1);
							
							monitor.subTask(Messages.ApplyChangeLogJob_RefreshUiSubTask);
							refreshUi(fpServer);
							monitor.worked(1);
						}
						
						monitor.done();
					}catch (Exception ex){
						processError(ex);
					}	
				}
			});
			
		}catch (Exception ex){
			record.setStatus(Status.ERROR);
			record.setErrorString(Messages.ApplyChangeLogJob_ApplyError + ex.getMessage());
			return false;
		}
		return true;
		
	}
	
	private void cleanUp(){
		try{
			Files.delete(changeLogPackageFile);
		}catch (Exception ex){
			ConnectPlugIn.log("Error cleaning up changelog file." + ex.getMessage(), ex); //$NON-NLS-1$
		}

		try{
			FileUtils.deleteDirectory(tempDirectory.toFile());
		}catch (Exception ex){
			ConnectPlugIn.log("Error cleaning up changelog directory." + ex.getMessage(), ex); //$NON-NLS-1$
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
			throw new Exception(Messages.ApplyChangeLogJob_NoMetadataError);
		}
		if (changeLogFile == null){
			throw new Exception(Messages.ApplyChangeLogJob_NoChangeLogError);
		}
		//check metadata
		metadata = MetadataPackager.INSTANCE.readMetadata(metadataFile);
		//check ca
		if (!serverInfo.getUuid().equals(metadata.getConservationArea())){
			throw new Exception(Messages.ApplyChangeLogJob_CaIdError);
		}
		//check version
		if (!serverInfo.getVersion().equals(metadata.getVersion())){
			throw new Exception(Messages.ApplyChangeLogJob_CaVersionError);
		}
			
		//check revision
		if (metadata.getServerRevision().longValue() == serverInfo.getServerRevision().longValue()){
			throw new NothingToUpdateException(Messages.ApplyChangeLogJob_UpToDate);
		}
		if (metadata.getServerRevision().longValue() <= serverInfo.getServerRevision().longValue() ){
			throw new Exception(Messages.ApplyChangeLogJob_RevisionError);
		}
		
	}
	/*
	 * applies change log file
	 */
	private void applyFile() throws NothingToUpdateException, Exception{
		
		Path filestoreDir = tempDirectory.resolve(ConnectSyncHistoryRecord.PACKAGE_FILESTORE_DIR);
			
		boolean replicationEnabled = DerbyReplicationManager.INSTANCE.getSystemReplicationState();
		
		//gets the current user; for resetting after applying changes
		SmartDB.DbUser currentUser = SmartDB.DbUser.ADMIN;
		if (SmartDB.getCurrentEmployee() != null){
			currentUser = SmartDB.getCurrentUser();
		}
		
		Session session = HibernateManager.lockDatabase(SmartDB.DbUser.ADMIN.getUserName(), SmartDB.DbUser.ADMIN.getPassword());
		try{	
			session.beginTransaction();
			
			//if not logged into a ca the replication won't be enabled
			//and we do not want to re-enable it when complete
			replicationEnabled = DerbyReplicationManager.INSTANCE.isReplicationSystemEnabled(session);
			if (replicationEnabled){
				// disable replication in db so we don't log items twice
				DerbyReplicationManager.INSTANCE.disableReplication(session);
			}
			
			//check plugin versions
			HashMap<String, String> localPlugins = DerbyMetadataPackager.INSTANCE.getLocalPluginVersions(session);
			
			for(String pluginid : metadata.getPluginVersions().keySet()){
				String version = metadata.getPluginVersion(pluginid);
				String dbVersion = localPlugins.get(pluginid);
				if (dbVersion == null){
					throw new Exception(
							MessageFormat.format(Messages.ApplyChangeLogJob_PluginError1, pluginid));
				}
				if (!version.equals(dbVersion)){
					throw new Exception(
							MessageFormat.format(Messages.ApplyChangeLogJob_PluginError2, pluginid, dbVersion, version));
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
			session.close();
			
			HibernateManager.unlockDatabase(currentUser.getUserName(), currentUser.getPassword());
			
			if (replicationEnabled){
				//re-enable replication if it was previously enabled
				session = HibernateManager.openSession();
				try{
					session.beginTransaction();
					DerbyReplicationManager.INSTANCE.enableReplication(session);
					session.getTransaction().commit();
				}catch(Exception ex){
					//replication could not be re-enabled.  This needs to kill the
					//application and restart
					ConnectPlugIn.displayLog(Messages.ApplyChangeLogJob_ReenableReplicationError, ex);
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
				.add(Restrictions.eq("uuid", currentEmployee.getUuid())) //$NON-NLS-1$
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
						MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.ApplyChangeLogJob_RestartDialogTitle, Messages.ApplyChangeLogJob_RestartDialogMessage);
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
								Messages.ApplyChangeLogJob_UserDialogTitle, Messages.ApplyChangeLogJob_UserDialogMessage, IDialogConstants.OK_LABEL);
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
										error = Messages.ApplyChangeLogJob_InvalidUserPassword;
									}
								}catch (Exception ex){
									ConnectPlugIn.log(ex.getMessage(), ex);
									error = ex.getMessage();
								}
								
								if (error != null){
									MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ApplyChangeLogJob_LogInError, error);
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
