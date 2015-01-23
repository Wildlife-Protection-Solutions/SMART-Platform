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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Engine responsible for backing up the SMART system.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbyBackupEngine {

	/**
	 * Export code extension point
	 */
	private static final String BACKUP_EXTENSION_ID = "org.wcs.smart.backup"; //$NON-NLS-1$
	
	/**
	 * @return the default backup file name based on the current date
	 */
	public static String getDefaultFileName(){
		String backupDir = SmartProperties.getInstance().getProperty(SmartProperties.PROP_BACKUP_DIR);
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd"); //$NON-NLS-1$
		try{
			return new File(backupDir + File.separator + "SMART_" + format.format(new Date()) + ".bak.zip").getCanonicalPath(); //$NON-NLS-1$ //$NON-NLS-2$
		}catch (Exception ex){
			return new File(backupDir + File.separator + "SMART_" + format.format(new Date()) + ".bak.zip").getAbsolutePath();  //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/**
	 * Backs up all SMART data to the given output file.
	 * 
	 * @param outputFile output file
	 * @param monitor progress monitor
	 * @return <code>true</code> if backup successful, <code>false</code> if cancelled or failed
	 * @throws IOException
	 */
	//apache derby database backup: http://db.apache.org/derby/docs/10.0/manuals/admin/hubprnt43.html
	public static boolean  backupSystem(File outputFile, IProgressMonitor monitor) throws IOException{
		if (outputFile.exists()){
			if (!outputFile.delete()){
				throw new IllegalStateException(Messages.DerbyBackupEngine_DeleteOutputFileError + " '" + outputFile.getAbsolutePath() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if (!outputFile.getParentFile().exists()){
			//try to create backup directory
			try{
				SmartUtils.createDirectory(outputFile.getParentFile());
			}catch (Exception ex){
				throw new IllegalStateException(Messages.DerbyBackupEngine_CreateDirectoryError + " '" + outputFile.getParentFile().toString() + "' \n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		Session session = HibernateManager.openSession();
		try{
			try{
				//here we freeze the database to put it into a state for copying
				Query q = session.createSQLQuery("CALL SYSCS_UTIL.SYSCS_FREEZE_DATABASE()"); //$NON-NLS-1$
				q.executeUpdate();
				File filestore = new File (SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE));
				File database = new File (SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB));
			
				if (!filestore.exists()){
					filestore.mkdir();
				}
				
				File[] dirsToBackup = new File[]{filestore, database};
			
				monitor.beginTask(Messages.DerbyBackupEngine_ProgressMessage, 2);
			
				if (ZipUtil.createZip(dirsToBackup, outputFile, monitor)) {
					List<IBackupContributor> extensions = getBackupExtensions();
					for (IBackupContributor ext : extensions) {
						ext.process(outputFile, new SubProgressMonitor(monitor, 2));
					}
					return true;
				}
				return false;
			}finally{
				//now un-freeze			
				Query q = session.createSQLQuery("CALL SYSCS_UTIL.SYSCS_UNFREEZE_DATABASE()"); //$NON-NLS-1$
				q.executeUpdate();
			}
		}finally{
			session.close();
		}
	}

	/**
	 * @return the default auto backup location
	 */
	public static String getDefaultAutoBackupLocation() { 
		String backupDir = SmartProperties.getInstance().getProperty(SmartProperties.PROP_BACKUP_DIR);
		try{
			return new File(backupDir).getCanonicalPath();
		}catch (Exception ex){
			return new File(backupDir).getAbsolutePath(); 
		}
	}
	
	/**
	 * @return list of ca data exporter extension points
	 */
	private static List<IBackupContributor> getBackupExtensions() {
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IBackupContributor> items = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(BACKUP_EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				items.add((IBackupContributor)e.createExecutableExtension("class")); //$NON-NLS-1$
			}
		}catch (Exception ex){
			SmartPlugIn.log("Error getting backup extensions", ex); //$NON-NLS-1$
		}
		return items;
	}
	
}
