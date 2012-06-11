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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.internal.UserNamePasswordDialog;

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
	public static boolean validateUserRestore(Shell currentShell) {
		// a backup can be restored if:
		// 1. there are no conservation areas
		// 2. the user is an admin user in at least one conservation area (we
		// don't care which one).

		Session session = HibernateManager.openSession();
		try {
			Long cnt = (Long) session.createCriteria(ConservationArea.class)
					.setProjection(Projections.rowCount()).uniqueResult();
			if (cnt == 0) {
				//there are not conservation areas
				return true;
			}

			// prompt for username and password
			boolean valid = false;
			UserNamePasswordDialog dialog = new UserNamePasswordDialog(
					currentShell,
					"Restore Backup File",
					"Please enter a username and password for one of the conservation areas in this database.  You must be an administrator in-order to restore a backup file.",
					"Restore");

			while (!valid) {
				if (dialog.open() == Window.CANCEL) {
					return false;
				}

				String username = dialog.getUserName();
				String password = dialog.getPassword();

				cnt = (Long) session
						.createCriteria(Employee.class)
						.add(Restrictions.eq("smartUserId", username))
						.add(Restrictions.eq("smartPassword", password))
						.add(Restrictions.eq("smartUserLevel",
								SmartUserLevel.ADMIN))
						.setProjection(Projections.rowCount()).uniqueResult();

				if (cnt == 0) {
					MessageDialog
							.openError(
									currentShell,
									"Invalid User",
									"The provided username and password is not a valid administrator user in any of the database conservation areas.");
				} else {
					valid = true;
				}
			}
			return true;
		} catch (Exception ex) {
			SmartPlugIn.displayLog(currentShell,
					"Error validating user for restore.", ex);
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
		monitor.beginTask("Restoring File", 5);
		if (!backupFile.exists()) {
			throw new Exception("File '" + backupFile.getAbsolutePath()
					+ " does not exist.");
		}

		monitor.setTaskName("Extracting contents of backup-file");
		/* extract contents of backup file to temporary directory */
		File temp = null;
		try {
			temp = File.createTempFile("smart",
					Long.toString(System.nanoTime()));
		} catch (Exception ex) {
			throw new Exception(
					"Could not create temporary directory for extracting backup."
							+ "\n\n " + ex.getMessage(), ex);
		}
		if (temp.exists()) {
			if (!temp.delete()) {
				throw new Exception(
						"Could not extract zip file to temporary working directory");
			}
		}
		if (!temp.mkdir()) {
			throw new Exception(
					"Could not extract zip file to temporary working directory");
		}

		try {
			unzipFolder(new ZipFile(backupFile), backupFile.length(), temp,
					new String[] { "null" });
		} catch (Exception ex) {
			String cleanUpErr = cleanUp(new File[] { temp });
			if (cleanUpErr.length() > 0) {
				throw new Exception(
						"Could not extract backup file.  The directories(s): "
								+ cleanUpErr
								+ " could not be removed and must be removed manually. \n\n"
								+ ex.getMessage(), ex);
			}
			throw new Exception("Could not extract backup file.\n\n"
					+ ex.getMessage(), ex);
		}
		monitor.worked(1);

		File dbFile = new File(SmartProperties.getInstance().getProperty(SmartProperties.SMART_DB_KEY));
		File dataFile = new File(SmartProperties.getInstance().getProperty( SmartProperties.FILESTORE_KEY));
		File extractedDb = new File(temp.getAbsolutePath() + File.separator
				+ dbFile.getName());
		File extractedFilestore = new File(temp.getAbsolutePath() + File.separator
				+ dataFile.getName());
		if (!extractedDb.exists()){
			String cleanUpErr = cleanUp(new File[] { temp });
			if (cleanUpErr.length() > 0) {
				throw new Exception(
						"Invalid backup file.  The backup file does not contain a SMART database.\n\n Error cleaning up: The directories(s): "
								+ cleanUpErr
								+ " could not be removed and must be removed manually. \n\n");
			}
			throw new Exception("Invalid backup file.  The backup file does not contain a SMART database");
		}
		if (!extractedFilestore.exists()){
			String cleanUpErr = cleanUp(new File[] { temp });
			if (cleanUpErr.length() > 0) {
				throw new Exception(
						"Invalid backup file.  The backup file does not contain a SMART filestore.\n\n Error cleaning up: The directories(s): "
								+ cleanUpErr
								+ " could not be removed and must be removed manually. \n\n");
			}
			throw new Exception("Invalid backup file.  The backup file does not contain a SMART filestore");
		}
		
		/* shut down the database */
		monitor.setTaskName("Shutting down database");
		HibernateManager.endSessionFactory(true);
		monitor.worked(1);

		/* create a copy of the current files incase something goes wrong */
		monitor.setTaskName("moving existing files");
		File dbFileBack = null;
		File dataFileBack = null;
		try {


			dbFileBack = new File(dbFile.getParentFile().getCanonicalPath()
					+ File.separator + dbFile.getName() + ".bak");
			dataFileBack = new File(dataFile.getParentFile().getCanonicalPath()
					+ File.separator + dataFile.getName() + ".bak");

			FileUtils.copyDirectory(dbFile, dbFileBack);
			FileUtils.copyDirectory(dataFile, dataFileBack);
		} catch (Exception ex) {
			// clean up temporary directory
			String cleanUpErr = cleanUp(new File[] { temp, dbFileBack,
					dataFileBack });
			if (cleanUpErr.length() > 0) {
				throw new Exception(
						"Failed to create temporary copies of existing data.  The directories(s): "
								+ cleanUpErr
								+ " could not be removed and must be removed manually. \n\n"
								+ ex.getMessage(), ex);
			}

			throw new Exception(
					"Failed to create temporary copies of existing data.\n\n"
							+ ex.getMessage(), ex);
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
						"Failed to delete existing data.  The system state is unknown.  You will must restore the system manually.\n\n  In addition, the directories(s): "
								+ cleanUpErr
								+ " could not be removed and must be removed manually. \n\n"
								+ ex.getMessage(), ex);
			}

			throw new Exception(
					"Failed to delete existing data. The system state is unknown.  You will must restore the system manually.\n\n"
							+ ex.getMessage(), ex);
		}

		/* restore the unzipped backup files */
		monitor.setTaskName("Restoring backup files");
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
						"Restore failed and system could not be returned to the original state.  You may be able to try the restore again or you may have to restore the system manually.\n\n"
								+ ex.getMessage() + "\n\n" + ex2.getMessage(),
						ex);
			}
			throw new Exception(
					"Restore failed. System successfully returned to original state.\n\n"
							+ ex.getMessage(), ex);
		}
		monitor.worked(1);

		/* clean up */
		monitor.setTaskName("Cleaning up");
		String cleanUpErr = cleanUp(new File[] { temp, dbFileBack, dataFileBack });
		if (cleanUpErr.length() > 0) {
			MessageDialog
					.openWarning(
							Display.getDefault().getActiveShell(),
							"Restore Warning",
							"The following directory created during the restore process could not be removed.  It is recommended you remove these directories manually:\n"
									+ cleanUpErr);
		}

		monitor.worked(1);

	}

	private static String cleanUp(File[] dirs) {
		StringBuilder strNoDelete = new StringBuilder();
		for (int i = 0; i < dirs.length; i++) {
			if (dirs[i].exists()) {
				try {
					FileUtils.deleteDirectory(dirs[i]);
				} catch (Exception ex) {
					try {
						strNoDelete.append(dirs[i].getCanonicalPath() + "\n");
					} catch (Exception ex2) {
						strNoDelete.append(dirs[i].getAbsolutePath() + "\n");
					}
				}
			}
		}
		return strNoDelete.toString();
	}

	private static boolean unzipFolder(ZipFile archiveFile, long size,
			File destinationLocation, String[] outputZipRootFolder)
			throws Exception {

		try {
			byte[] buf = new byte[65536];

			Enumeration<ZipArchiveEntry> entries = archiveFile.getEntries();
			while (entries.hasMoreElements()) {
				ZipArchiveEntry zipEntry = entries.nextElement();
				String name = zipEntry.getName();
				name = name.replace('\\', '/');
				int i = name.indexOf('/');
				if (i > 0) {
					outputZipRootFolder[0] = name.substring(0, i);
				}
				// name = name.substring(i + 1);

				File destinationFile = new File(destinationLocation, name);
				if (name.endsWith("/")) {
					if (!destinationFile.isDirectory()
							&& !destinationFile.mkdirs()) {
						throw new Exception(
								"Could not create temp directory: '"
										+ destinationFile.getPath());
					}
					continue;
				} else if (name.indexOf('/') != -1) {
					// Create the the parent directory if it doesn't exist
					File parentFolder = destinationFile.getParentFile();
					if (!parentFolder.isDirectory()) {
						if (!parentFolder.mkdirs()) {
							throw new Exception(
									"Could not create temp directory: '"
											+ parentFolder.getPath());
						}
					}
				}

				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(destinationFile);
					int n;
					InputStream entryContent = archiveFile
							.getInputStream(zipEntry);
					while ((n = entryContent.read(buf)) != -1) {
						if (n > 0) {
							fos.write(buf, 0, n);
						}
					}
				} finally {
					if (fos != null) {
						fos.close();
					}
				}
			}
			return true;

		} catch (IOException e) {
			throw new Exception("Unzip failed: " + e.getMessage(), e);
		} finally {
			try {
				archiveFile.close();
			} catch (IOException e) {
			}
		}

	}

}
