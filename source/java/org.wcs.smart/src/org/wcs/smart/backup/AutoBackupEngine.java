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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.internal.Messages;

/**
 * Engine responsible for checking if the auto-backup is supposed to run,
 * then doing it.
 * 
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class AutoBackupEngine {
	/**
	 * prefix of backup file names
	 */
	private static final String BACKUP_FILENAME_PREFIX = "SMART-DB_BACKUP_"; //$NON-NLS-1$

	/**
	 * backup properties file
	 */
	private static final String SMART_BACKUP_PROPERTIES_FILE = "smart_backup.properties"; //$NON-NLS-1$
	
	/**
	 * Property name for how often backup should occur
	 */
	public static final String PROP_BACKUP_TIMER = "backup_timer"; //$NON-NLS-1$
	/**
	 * Property name for when backups should be deleted
	 */
	public static final String PROP_DELETE_TIMER = "delete_timer"; //$NON-NLS-1$
	/**
	 * Property name for backup location
	 */
	public static final String PROP_BACKUP_LOCATION = "backup_location"; //$NON-NLS-1$
	/**
	 * Property name for last backup time
	 */
	public static final String PROP_LASTBACKUP = "last_backup"; //$NON-NLS-1$

	/**
	 * Performs the auto backup
	 * @param shell shell for progress monitor
	 * @return <code>true</code> if backup ok, <code>false</code> if failed
	 */
	public static boolean autoBackup(final Shell shell){
		final Properties properties = getAutoBackupProperties();
		if(properties == null || properties.getProperty(PROP_BACKUP_TIMER) == null) return false; //no file exists

		try {
			final ProgressMonitorDialog pmdDialog = new ProgressMonitorDialog(shell);
			
			pmdDialog.run(true, true, new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				shell.getDisplay().syncExec(new Runnable(){
					@Override
					public void run() {
						pmdDialog.getShell().setText(Messages.AutoBackupEngine_ProgressDialogTitle);
					}
					
				});
				if (timerIsExpired(properties)){
					deleteOldFiles(properties);
						
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"); //$NON-NLS-1$
					Date date = new Date();
					final File tmp = new File(properties.getProperty(PROP_BACKUP_LOCATION));
					
					if(!tmp.exists()){						
						try{
							FileUtils.forceMkdir(tmp);
						}catch (final Exception ex){
							final String error = MessageFormat.format(Messages.AutoBackupEngine_MakeDirectoryFailed, new Object[]{ex.getLocalizedMessage()});
							shell.getDisplay().syncExec(new Runnable(){
							    public void run (){
							    	SmartPlugIn.displayLog(error, ex);
							        
							}
							});
							return;
						}
					}
					
					File f = new File(properties.getProperty(PROP_BACKUP_LOCATION) + File.separator + BACKUP_FILENAME_PREFIX + dateFormat.format(date) + ".zip"); //$NON-NLS-1$
					try{
						if(DerbyBackupEngine.backupSystem(f, monitor)){					
							//do nothing if success
						}else if (monitor.isCanceled()){
							shell.getDisplay().syncExec(new Runnable(){
							    public void run (){    
							        MessageDialog.openError(shell, Messages.AutoBackupEngine_AutoBackupFailed_Dialog_Title, Messages.AutoBackupEngine_AutoBackupCancelled_Dialog_Message);
							}
							});
						}else{
							shell.getDisplay().syncExec(new Runnable(){
							    public void run (){    
							        MessageDialog.openError(shell, Messages.AutoBackupEngine_AutoBackupFailed_Dialog_Title, Messages.AutoBackupEngine_AutoBackupDidNotFinish_Dialog_Message);
							}
								});

						}
					}catch (Exception ex){
						SmartPlugIn.displayLog(Messages.AutoBackupEngine_AutoBackupFailed_Error + ex.getLocalizedMessage(), ex);
					}
				}
			}

		});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.AutoBackupEngine_AutoBackupFailed_Error + ex.getLocalizedMessage(), ex);
			return false;
		}
		properties.setProperty(PROP_LASTBACKUP,String.valueOf((new java.util.Date()).getTime() / 1000)); //use seconds
		setAutoBackupProperties(properties);
		return true;
	}
	
	/**
	 * Removes old backup files that are no longer required.
	 * 
	 * @param properties backup properties file
	 */
	private static void deleteOldFiles(Properties properties) {
		Date cutoffDate = new Date();
		cutoffDate.setTime(cutoffDate.getTime() - (long)(Double.valueOf(properties.getProperty(PROP_DELETE_TIMER)) * 86400 * 1000)); //1 day in milliseconds 86k*1000

		File dir = new File(properties.getProperty(PROP_BACKUP_LOCATION));
		if (dir.listFiles() == null){
			//no files, nothing to delete
			return;
		}
		  for (File child : dir.listFiles()) {
			  if (child.getName().equals(".") || //$NON-NLS-1$
					  child.getName().equals("..")){ //$NON-NLS-1$
		      continue;  // Ignore the self and parent aliases.
		    }
		    if(child.lastModified() < cutoffDate.getTime() && child.getName().contains(BACKUP_FILENAME_PREFIX)){
		    	child.delete();
		    }
		  }

	}

	/**
	 * Determines if the backup needs to run.
	 * <p>Checkes the backup_timer properties
	 * against the current time.</p>
	 * @param properties current backup properties
	 * @return <code>true</code> if backup should run, <code>false</code> if not
	 */
	private static boolean timerIsExpired(Properties properties){
		//implement edge cases of  0 = always backup; and  -1 = off
		double days = Double.valueOf(properties.getProperty(PROP_BACKUP_TIMER));
		if (days < 0) return false;
		if (daysSinceBackup(properties) >= days || days == 0){
			return true;
		}
		return false;
	}
	
	/**
	 * Compute the number of days since the last backup.
	 * @param properties current backup properties
	 * @return the number of days since backup was last run
	 */
	private static long daysSinceBackup(Properties properties){
		long last = Long.valueOf(properties.getProperty(PROP_LASTBACKUP));
		Date time = new Date();
		long now = time.getTime() / 1000;  //use seconds
		long sec_dif = now - last;
		return sec_dif / 86400; //convert seconds to days
	}
	
	/**
	 * Reads the properties file for auto-backup config
	 * 
	 * @return a Properties object 
	 * @throws IOException if file not found
	 */
	public static Properties getAutoBackupProperties(){
		Properties properties = new Properties();
		try {
			String location = SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE) + SMART_BACKUP_PROPERTIES_FILE;
			File f = new File(location);
			if (!f.exists()){
				return properties;
			}
			FileInputStream fis = new FileInputStream(location);
			try{
				properties.load(fis);
			}finally{
				fis.close();
			}
		} catch (IOException e) {
			SmartPlugIn.log(Messages.AutoBackupEngine_ErrorReadingPropFile + "\n\n" + e.getLocalizedMessage(), e); //$NON-NLS-1$
			return properties;
		}
		return properties;
	}

	/**
	 * Saves the properties file for auto-backup config
	 * 
	 * @return true if successful false if the save failed 
	 */
	public static boolean setAutoBackupProperties(Properties prop){
		try {
			String location = SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE) + SMART_BACKUP_PROPERTIES_FILE;
			FileOutputStream fout = new FileOutputStream(location);
			try{
				prop.store(fout, null);
			}finally{
				fout.close();
			}
			return true;
		} catch (IOException e) {
			SmartPlugIn.displayLog(Messages.AutoBackupEngine_ErrorWirtingPropFile + "\n\n" + e.getLocalizedMessage(), e); //$NON-NLS-1$
			return false;
		}
	}
}
