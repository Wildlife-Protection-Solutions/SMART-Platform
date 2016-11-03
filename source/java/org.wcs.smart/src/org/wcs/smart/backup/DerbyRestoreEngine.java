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

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.hibernate.SmartDB.DbUser;
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.UserNamePasswordDialog;
import org.wcs.smart.upgrade.UpgradeEngine;
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
	 * <li> there are no conservation areas in the database (empty database)</li>
	 * <li>the user enters a username/password that is an admin user in one of the 
	 * conservation areas in the database.</li>
	 * 
	 * @param currentShell the current shell for displaying input boxes
	 * @return <code>true</code> if can restore, <code>false</code> otherwise
	 */
	@SuppressWarnings("unchecked")
	public static boolean validateUserRestore(Shell currentShell) {
		// a backup can be restored if:
		// 1. there are no conservation areas
		// 2. the user is an admin user in at least one conservation area (we
		// don't care which one).

		Session session = HibernateManager.openSession();
		try {
			Long cnt = (Long) session.createCriteria(ConservationArea.class).add(Restrictions.ne("uuid", ConservationArea.MULTIPLE_CA)) //$NON-NLS-1$
					.setProjection(Projections.rowCount()).uniqueResult();
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

				List<Employee> matching = session.createCriteria(Employee.class)
						.add(Restrictions.eq("smartUserId", username).ignoreCase()) //$NON-NLS-1$
						.add(Restrictions.eq("smartUserLevel", SmartUserLevel.ADMIN)).list(); //$NON-NLS-1$
				
				boolean found = false;
				for (Employee e : matching){
					if (HibernateManager.validatePassword(password, e)){
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
		} finally {
			session.close();
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
	public static void restoreSystem(File backupFile, IProgressMonitor monitor)
			throws Exception {
		monitor.beginTask(Messages.DerbyRestoreEngine_Progress_RestoringFile, 7);
		if (!backupFile.exists()) {
			throw new Exception(Messages.DerbyRestoreEngine_Error_NoBackupFile + " '" + backupFile.getAbsolutePath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		monitor.setTaskName(Messages.DerbyRestoreEngine_Progress_ExtractingBackup);
		/* extract contents of backup file to temporary directory */
		File temp = SmartUtils.createTemporaryDirectory();
		
		try {
			ZipUtil.unzipFolder(backupFile, temp);
		} catch (Exception ex) {
			String cleanUpErr = cleanUp(new File[] { temp });
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
		monitor.worked(1);

		File dbFile = new File(SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB));
		File dataFile = new File(SmartProperties.getInstance().getProperty( SmartProperties.PROP_FILESTORE));
		if (!dataFile.exists()){
			dataFile.mkdir();
		}
		File extractedDb = new File(temp.getAbsolutePath() + File.separator
				+ dbFile.getName());
		File extractedFilestore = new File(temp.getAbsolutePath() + File.separator
				+ dataFile.getName());
		if (!extractedDb.exists()){
			String cleanUpErr = cleanUp(new File[] { temp });
			if (cleanUpErr.length() > 0) {
				throw new Exception(
						Messages.DerbyRestoreEngine_Error_NoDbInBackupFile
						+ "\n\n"  //$NON-NLS-1$
						+ Messages.DerbyRestoreEngine_Error_CouldNotCleanup
						+ cleanUpErr);
			}
			throw new Exception(Messages.DerbyRestoreEngine_Error_NoDbInBackupFile);
		}
		if (!extractedFilestore.exists()){
			Display.getDefault().syncExec(()->{
				MessageDialog.openWarning(Display.getDefault().getActiveShell(), Messages.DerbyRestoreEngine_FileStoreMissingTitle,
						Messages.DerbyRestoreEngine_FileStoreMissingMessage);	
			});
			
			extractedFilestore.mkdir();
		}
		
		/* get database versions */
		HashMap<String, String> versions = new HashMap<String, String>();
		Session s = HibernateManager.openSession();
		try{
			List<?> tmpversions = s.createSQLQuery("SELECT plugin_id, version FROM " + SmartDB.PLUGIN_VERSION_TBL).list(); //$NON-NLS-1$
			for (Object x : tmpversions){
				String pluginid = (String) ((Object[])x)[0];
				String version = (String) ((Object[])x)[1];
				versions.put(pluginid, version);
			}
		}finally{
			s.close();
		}
		
		/* shut down the database */
		monitor.setTaskName(Messages.DerbyRestoreEngine_Progress_ShutDown);
		HibernateManager.endSessionFactory(true, false);
		monitor.worked(1);

		/* connect to the extractedDb and verify version */
		SmartHibernateManager.setDatabaseParameter(extractedDb.getAbsolutePath());
		/* need to login as admin user to perform upgrade */
		SmartHibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		
		String datastore = SmartContext.INSTANCE.getFilestoreLocation();
		SmartContext.INSTANCE.setFilestoreLocation(SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE));
		try{
			SmartContext.INSTANCE.setFilestoreLocation(extractedFilestore.getAbsolutePath());
			UpgradeEngine upgrader = new UpgradeEngine();
			upgrader.upgradeSystem(new SubProgressMonitor(monitor, 1), versions);
			validateConfiguration(versions);
			upgrader.postProcess(new SubProgressMonitor(monitor, 1));
		}catch (Exception ex){
			HibernateManager.endSessionFactory(true, true);
			String cleanUpErr = cleanUp(new File[] { temp });
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
		monitor.setTaskName(Messages.DerbyRestoreEngine_Progress_MovingFiles);
		File dbFileBack = null;
		File dataFileBack = null;
		try {


			dbFileBack = new File(dbFile.getParentFile().getCanonicalPath()
					+ File.separator + dbFile.getName() + ".bak"); //$NON-NLS-1$
			dataFileBack = new File(dataFile.getParentFile().getCanonicalPath()
					+ File.separator + dataFile.getName() + ".bak"); //$NON-NLS-1$

			FileUtils.copyDirectory(dbFile, dbFileBack);
			FileUtils.copyDirectory(dataFile, dataFileBack);
		} catch (Exception ex) {
			// clean up temporary directory
			String cleanUpErr = cleanUp(new File[] { temp, dbFileBack,
					dataFileBack });
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
		monitor.worked(1);

		/* delete existing data */
		try {
			FileUtils.deleteDirectory(dbFile);
			FileUtils.deleteDirectory(dataFile);
		} catch (Exception ex) {
			String cleanUpErr = cleanUp(new File[] { temp, dbFileBack,
					dataFileBack });
			if (cleanUpErr.length() > 0) {
				throw new Exception(
						Messages.DerbyRestoreEngine_Error_Failure
								+ cleanUpErr
								+ "\n\n" //$NON-NLS-1$
								+ ex.getLocalizedMessage(), ex);
			}

			throw new Exception(
					Messages.DerbyRestoreEngine_Error_CouldNotDeleteCurrentData
							+ ex.getLocalizedMessage(), ex);
		}

		/* restore the unzipped backup files */
		monitor.setTaskName(Messages.DerbyRestoreEngine_Progress_RestoringFiles);
		try {
			
			FileUtils.copyDirectory(extractedDb, dbFile);

			
			if (extractedFilestore.isFile()){
				//directory was empty
				dataFile.mkdir();
			}else{
				FileUtils.copyDirectory(extractedFilestore, dataFile);
			}
		} catch (Exception ex) {
			// backup failed
			// try to restore copied files
			try {
				FileUtils.deleteDirectory(dbFile);
				FileUtils.deleteDirectory(dataFile);
				FileUtils.moveDirectory(dbFileBack, dbFile);
				FileUtils.moveDirectory(dataFileBack, dataFile);
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
		monitor.worked(1);

		/* clean up */
		monitor.setTaskName(Messages.DerbyRestoreEngine_Progress_CleanUp);
		final String cleanUpErr = cleanUp(new File[] { temp, dbFileBack, dataFileBack });
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

		monitor.worked(1);

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
		Session s;
		s = HibernateManager.openSession();
		try{
			List<?> tmpversions = s.createSQLQuery("SELECT plugin_id, version FROM " + SmartDB.PLUGIN_VERSION_TBL).list(); //$NON-NLS-1$
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
		}finally{
			s.close();
		}
	}

	private static String cleanUp(File[] dirs) {
		StringBuilder strNoDelete = new StringBuilder();
		for (int i = 0; i < dirs.length; i++) {
			if (dirs[i].exists()) {
				try {
					FileUtils.deleteDirectory(dirs[i]);
				} catch (Exception ex) {
					try {
						strNoDelete.append(dirs[i].getCanonicalPath() + "\n"); //$NON-NLS-1$
					} catch (Exception ex2) {
						strNoDelete.append(dirs[i].getAbsolutePath() + "\n"); //$NON-NLS-1$
					}
				}
			}
		}
		return strNoDelete.toString();
	}

	

}
