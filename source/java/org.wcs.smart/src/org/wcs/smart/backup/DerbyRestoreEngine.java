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
package org.wcs.smart.backup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.hibernate.SmartDB.DbUser;
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.UserNamePasswordDialog;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.user.UserLevelManager;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Backup restore engine for restoring
 * backup files.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbyRestoreEngine {

	/**
	 * Validates that a restore can occur. 
	 * <p>Prompts the user as required.</p>
	 * 
	 * <p>A backup can be restored if:</p>
	 * <li> there are no conservation areas (except ccaa) in the database (empty database)</li>
	 * <li>the user enters a username/password that is an admin user in one of the 
	 * conservation areas in the database.</li>
	 * 
	 * @param currentShell the current shell for displaying input boxes
	 * @return <code>true</code> if can restore, <code>false</code> otherwise
	 */
	public static boolean validateUserRestore(Shell currentShell) {
		// a backup can be restored if:
		// 1. there are no conservation areas (except ccaa)
		// 2. the user is an admin user in at least one conservation area (we
		// don't care which one).

		
		try(Session session = HibernateManager.openSession()) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			
			CriteriaQuery<Long> cq = cb.createQuery(Long.class);
			Root<ConservationArea> root = cq.from(ConservationArea.class);
			cq.select(cb.count(root));
			cq.where(cb.notEqual(root.get("uuid"), ConservationArea.MULTIPLE_CA)); //$NON-NLS-1$
			Long cnt = session.createQuery(cq).getSingleResult();
			
			if (cnt == 0) {
				//there are no conservation areas
				return true;
			}

			// prompt for username and password
			boolean valid = false;
			UserNamePasswordDialog dialog = new UserNamePasswordDialog(
					currentShell,
					Messages.DerbyRestoreEngine_RestoreDialogTitle,
					Messages.DerbyRestoreEngine_RestoreDialogMessage,
					Messages.DerbyRestoreEngine_ButtonText);

			while (!valid) {
				if (dialog.open() == Window.CANCEL) {
					return false;
				}

				String username = dialog.getUserName();
				String password = dialog.getPassword();

				String query = "FROM Employee WHERE UPPER(smartUserId) = UPPER(:userid) and endEmploymentDate is null"; //$NON-NLS-1$
				List<Employee> matching = session.createQuery(query, Employee.class)
						.setParameter("userid", username) //$NON-NLS-1$
						.getResultList();
				
				boolean found = false;
				for (Employee e : matching){
					if (UserLevelManager.INSTANCE.supportsUser(e, UserLevelManager.ADMIN) &&
							HibernateManager.validatePassword(password, e)){
						found = true;
						break;
					}
				}
				if (!found) {
					MessageDialog
							.openError(
									currentShell,
									Messages.DerbyRestoreEngine_InvalidUserDialogTitle,
									Messages.DerbyRestoreEngine_InvalidUserDialogMessage);
				} else {
					valid = true;
				}
			}
			return true;
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.DerbyRestoreEngine_UserValidationError, ex);
			return false;
		}
	}
	

	/**
	 * Restores a backup file using the following process:
	 * 
	 * <ul>
	 * <li> extracts the contents of the backup file to a temporary directory</li>
	 * <li>ensures that both the database and filestore files exist in the backup file</li>
	 * <li>shuts down the existing database</li>
	 * <li>connects to the new database and performs upgrades if required</li>
	 * <li>moves the existing database and filestore to a temporary location</li>
	 * <li>copies the database and filestore from the temporary directory to the SMART location</li>
	 * <li>deletes the temporary directory, copied original database and filestore</li>
	 * </ul>
	 * 
	 * <p>If an error occurs it does it's best to restore the system to the original state;
	 * however this is not always possible.  If this occurs then the system must
	 * be restored manually.</p>
	 * 
	 * @param backupFile the backup file to restore
	 * @param monitor the progress monitor
	 */
	public static void restoreSystem(Path backupFile, IProgressMonitor monitor)
			throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbyRestoreEngine_Progress_RestoringFile, 14);
		if (!Files.exists(backupFile)) {
			throw new Exception(Messages.DerbyRestoreEngine_Error_NoBackupFile + " '" + backupFile.normalize().toAbsolutePath().toString() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		progress.subTask(Messages.DerbyRestoreEngine_Progress_ExtractingBackup);
		/* extract contents of backup file to temporary directory */
		Path temp = SmartUtils.createTemporaryDirectory();
		
		try {
			ZipUtil.unzipFolder(backupFile, temp);
		} catch (Exception ex) {
			String cleanUpErr = cleanUp(new Path[] { temp });
			if (cleanUpErr.length() > 0) {
				throw new Exception(
						Messages.DerbyRestoreEngine_Error_CouldNotExtractFile
						 + "\n\n"  //$NON-NLS-1$
						+ Messages.DerbyRestoreEngine_Error_CouldNotCleanup
						+ cleanUpErr
						+ "\n\n" //$NON-NLS-1$
						+ ex.getLocalizedMessage(), ex);
			}
			throw new Exception(Messages.DerbyRestoreEngine_BackupExtractionError
					+ ex.getLocalizedMessage(), ex);
		}
		progress.worked(1);

		Path dbFile = Paths.get(SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB));
		Path dataFile = Paths.get(SmartProperties.getInstance().getProperty( SmartProperties.PROP_FILESTORE));
		
		if (!Files.exists(dataFile)){
			SmartUtils.createDirectory(dataFile);
		}
		Path extractedDb = temp.normalize().resolve(dbFile.getFileName().toString());
		Path extractedFilestore = temp.normalize().resolve(dataFile.getFileName().toString());
				
		if (!Files.exists(extractedDb)){
			String cleanUpErr = cleanUp(new Path[] { temp });
			if (cleanUpErr.length() > 0) {
				throw new Exception(
						Messages.DerbyRestoreEngine_Error_NoDbInBackupFile
						+ "\n\n"  //$NON-NLS-1$
						+ Messages.DerbyRestoreEngine_Error_CouldNotCleanup
						+ cleanUpErr);
			}
			throw new Exception(Messages.DerbyRestoreEngine_Error_NoDbInBackupFile);
		}
		if (!Files.exists(extractedFilestore)){
			Display.getDefault().syncExec(()->{
				MessageDialog.openWarning(Display.getDefault().getActiveShell(), Messages.DerbyRestoreEngine_FileStoreMissingTitle,
						Messages.DerbyRestoreEngine_FileStoreMissingMessage);	
			});
			
			SmartUtils.createDirectory(extractedFilestore);
		}
		
		/* get database versions */
		HashMap<String, String> versions = new HashMap<String, String>();
		
		try(Session s = HibernateManager.openSession()){
			List<?> tmpversions = s.createNativeQuery("SELECT plugin_id, version FROM " + SmartDB.PLUGIN_VERSION_TBL).list(); //$NON-NLS-1$
			for (Object x : tmpversions){
				String pluginid = (String) ((Object[])x)[0];
				String version = (String) ((Object[])x)[1];
				versions.put(pluginid, version);
			}
		}
		
		/* shut down the database */
		progress.subTask(Messages.DerbyRestoreEngine_Progress_ShutDown);
		HibernateManager.endSessionFactory(true, false);
		progress.worked(1);

		/* connect to the extractedDb and verify version */
		SmartHibernateManager.setDatabaseParameter(extractedDb.normalize().toAbsolutePath().toString());
		/* need to login as admin user to perform upgrade */
		SmartHibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		
		//check to install all plugins in backup and also installed in current version
		StringBuilder missingPlugins = new StringBuilder();
			
		try(Session s = HibernateManager.openSession()){
			Map<String,String> backupVersions = UpgradeEngine.getVersions(s);
			
			for (Entry<String, String[]> mapping : UpgradeEngine.getPluginMappings().entrySet()) {
				String oldId = mapping.getKey();
				String oldVersion = mapping.getValue()[0];
				String newId = mapping.getValue()[1];
				
				String version = backupVersions.get(oldId);
				if (version != null && version.equals(oldVersion)) {
					backupVersions.remove(oldId);
					backupVersions.put(newId, oldVersion);
				}
			}
			
			for (String pluginId : backupVersions.keySet()){
				if (pluginId.equalsIgnoreCase("org.wcs.smart.intelligence") || //$NON-NLS-1$
						 pluginId.equalsIgnoreCase("org.wcs.smart.intelligence.query") ) { //$NON-NLS-1$
					//these get remove in version 7.0 and up so we don't care if they exist
					continue;
				}
				if (!versions.keySet().contains(pluginId)){
					missingPlugins.append(pluginId);
					missingPlugins.append("\n"); //$NON-NLS-1$
				}
			}	
		}
		
		if (missingPlugins.length() > 0){
			HibernateManager.endSessionFactory(true, true);
			cleanUp(new Path[] { temp });
			throw new Exception(Messages.DerbyRestoreEngine_MissingSystemPlugins + "\n\n" + missingPlugins.toString()); //$NON-NLS-1$
		}
		
		/* Do the upgrade/restore */
		String datastore = SmartContext.INSTANCE.getFilestoreLocation();
		SmartContext.INSTANCE.setFilestoreLocation(SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE));
		try{
			SmartContext.INSTANCE.setFilestoreLocation(extractedFilestore.normalize().toAbsolutePath().toString());
			UpgradeEngine upgrader = new UpgradeEngine();
			upgrader.upgradeSystem(progress.split(7), versions);
			validateConfiguration(versions);
			upgrader.postProcess(progress.split(1));
		}catch (Exception ex){
			HibernateManager.endSessionFactory(true, true);
			String cleanUpErr = cleanUp(new Path[] { temp });
			if (cleanUpErr.length() > 0) {
				throw new Exception(
						ex.getLocalizedMessage()
						+ "\n\n"  //$NON-NLS-1$
						+ Messages.DerbyRestoreEngine_Error_CouldNotCleanup
						+ cleanUpErr, ex);
			}
			throw ex;
		}finally{
			SmartContext.INSTANCE.setFilestoreLocation(datastore);
			HibernateManager.endSessionFactory(true, true);	
			//restore database parameter to main db
			SmartHibernateManager.setDatabaseParameter(SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB));
			SmartHibernateManager.setUserName(DbUser.LOGIN.getUserName(), DbUser.LOGIN.getPassword());
		}

		/* create a copy of the current files incase something goes wrong */
		progress.subTask(Messages.DerbyRestoreEngine_Progress_MovingFiles);
		Path dbFileBack = null;
		Path dataFileBack = null;
		try {
			dbFileBack = dbFile.getParent().normalize().resolve(dbFile.getFileName().toString() + ".bak"); //$NON-NLS-1$
			dataFileBack = dataFile.getParent().normalize().resolve(dataFile.getFileName() + ".bak"); //$NON-NLS-1$

			SmartUtils.moveDirectory(dbFile, dbFileBack);
			SmartUtils.moveDirectory(dataFile, dataFileBack);
		} catch (Exception ex) {
			// clean up temporary directory
			String cleanUpErr = cleanUp(new Path[] { temp, dbFileBack, dataFileBack });
			if (cleanUpErr.length() > 0) {
				throw new Exception(
						Messages.DerbyRestoreEngine_Error_CouldNotCreateTempCopy
						+ Messages.DerbyRestoreEngine_Error_CouldNotCleanup
						+ cleanUpErr
						+ "\n\n" //$NON-NLS-1$
						+ ex.getLocalizedMessage(), ex);
			}

			throw new Exception(
					Messages.DerbyRestoreEngine_Error_CouldNotCreateTempBackup
							+ ex.getLocalizedMessage(), ex);
		}
		progress.worked(1);

		/* restore the unzipped backup files */
		progress.subTask(Messages.DerbyRestoreEngine_Progress_RestoringFiles);
		try {
			SmartUtils.moveDirectory(extractedDb, dbFile);
			if (!Files.isDirectory(extractedFilestore)){
				//directory was empty
				SmartUtils.createDirectory(dataFile);
			}else{
				SmartUtils.moveDirectory(extractedFilestore, dataFile);
			}
		} catch (Exception ex) {
			// backup failed
			// try to restore copied files
			try {
				SmartUtils.deleteDirectory(dbFile);
				SmartUtils.deleteDirectory(dataFile);
				SmartUtils.moveDirectory(dbFileBack, dbFile);
				SmartUtils.moveDirectory(dataFileBack, dataFile);
			} catch (Exception ex2) {
				throw new Exception(
						Messages.DerbyRestoreEngine_Error_RestoreFailedCouldNotRevert
								+ ex.getLocalizedMessage() + "\n\n" + ex2.getLocalizedMessage(), //$NON-NLS-1$
						ex);
			}
			throw new Exception(
					Messages.DerbyRestoreEngine_Error_RestoreFailedRevertSuccessful
							+ ex.getLocalizedMessage(), ex);
		}
		progress.worked(1);

		/* clean up */
		progress.subTask(Messages.DerbyRestoreEngine_Progress_CleanUp);
		final String cleanUpErr = cleanUp(new Path[] { temp, dbFileBack, dataFileBack });
		if (cleanUpErr.length() > 0) {
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					MessageDialog
					.openWarning(
							Display.getDefault().getActiveShell(),
							Messages.DerbyRestoreEngine_Warning_Dialog_Title,
							Messages.DerbyRestoreEngine_Warning_Dialog_Message
									+ cleanUpErr);
				}
				
			});
			
		}

		progress.worked(1);

	}
	
	/**
	 * Validates the version of the backup database against what is currently
	 * in the system.  Throws an exception if there are missing plugins, extra plugins 
	 * or inconsistant versions.
	 * 
	 * 	@param versions the system plugins and associated versions
	 * 	@throws Exception
 	*/
	private static void validateConfiguration(HashMap<String, String> versions) throws Exception {
		try(Session s = HibernateManager.openSession()){
			List<?> tmpversions = s.createNativeQuery("SELECT plugin_id, version FROM " + SmartDB.PLUGIN_VERSION_TBL).list(); //$NON-NLS-1$
			List<String> missingFromBackup = new ArrayList<String>();
			List<String> missingFromSystem = new ArrayList<String>();
			List<String> versionError = new ArrayList<String>();
			List<String> all = new ArrayList<String>();
			
			for (Object x : tmpversions){
				String pluginid = (String) ((Object[])x)[0];
				String version = (String) ((Object[])x)[1];
				if (versions.get(pluginid) == null){
					missingFromSystem.add(pluginid);
				}else if (!versions.get(pluginid).equals(version)){
					versionError.add(MessageFormat.format(Messages.DerbyRestoreEngine_VersionError, new Object[]{pluginid, versions.get(pluginid), version}));
				}
				all.add(pluginid);
			}
			for (Iterator<String> iterator = versions.keySet().iterator(); iterator.hasNext();) {
				String system = (String) iterator.next();
				if (!all.contains(system)){
					missingFromBackup.add(system);
				}
				
			}
			
			if (missingFromBackup.size() > 0 || missingFromSystem.size() > 0 || versionError.size() > 0){
				StringBuilder sb = new StringBuilder();
				sb.append(Messages.DerbyRestoreEngine_ConfigurationError);
				if (missingFromBackup.size() > 0){
					StringBuilder tmp = new StringBuilder();
					for (String x : missingFromBackup){
						tmp.append(x);
						tmp.append(", "); //$NON-NLS-1$
					}
					tmp.deleteCharAt(tmp.length() - 1);
					tmp.deleteCharAt(tmp.length() - 1);
					
					sb.append("\n\n"); //$NON-NLS-1$
					sb.append(MessageFormat.format(Messages.DerbyRestoreEngine_MissingPlugins, new Object[]{tmp.toString()}));
				}
				if (missingFromSystem.size() > 0){
					StringBuilder tmp = new StringBuilder();
					for (String x : missingFromSystem){
						tmp.append(x);
						tmp.append(", "); //$NON-NLS-1$
					}
					tmp.deleteCharAt(tmp.length() - 1);
					tmp.deleteCharAt(tmp.length() - 1);
					
					sb.append("\n\n"); //$NON-NLS-1$
					sb.append(MessageFormat.format(Messages.DerbyRestoreEngine_ExtraPlugins, new Object[]{tmp.toString()}));
					
				}
				if (versionError.size() > 0){
					StringBuilder tmp = new StringBuilder();
					for (String x : versionError){
						tmp.append(x);
						tmp.append(", "); //$NON-NLS-1$
					}
					tmp.deleteCharAt(tmp.length() - 1);
					tmp.deleteCharAt(tmp.length() - 1);
					
					sb.append("\n\n"); //$NON-NLS-1$
					sb.append(MessageFormat.format(Messages.DerbyRestoreEngine_InconsistentVersions, new Object[]{tmp.toString()}));
					
				}
				throw new Exception(sb.toString());
			}
		}
	}

	private static String cleanUp(Path[] dirs) {
		StringBuilder strNoDelete = new StringBuilder();
		for (int i = 0; i < dirs.length; i++) {
			if (Files.exists(dirs[i])) {
				try {
					SmartUtils.deleteDirectory(dirs[i]);
				} catch (Exception ex) {
					
					SmartPlugIn.log(ex.getMessage(), ex);
					try {
						strNoDelete.append(dirs[i].normalize().toAbsolutePath().toString()+ "\n"); //$NON-NLS-1$
					} catch (Exception ex2) {
						SmartPlugIn.log(ex2.getMessage(), ex2);
						strNoDelete.append(dirs[i].normalize().toAbsolutePath().toString() + "\n"); //$NON-NLS-1$
					}
				}
			}
		}
		return strNoDelete.toString();
	}

	

}
