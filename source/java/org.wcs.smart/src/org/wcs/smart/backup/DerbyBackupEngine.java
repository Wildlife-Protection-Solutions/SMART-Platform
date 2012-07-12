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
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.hibernate.HibernateManager;
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
	 * @return the default backup file name based on the current date
	 */
	public static String getDefaultFileName(){
		String backupDir = SmartProperties.getInstance().getProperty(SmartProperties.BACKUP_DIRECTORY_KEY);
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		try{
			return new File(backupDir + File.separator + "SMART_" + format.format(new Date()) + ".bak.zip").getCanonicalPath();
		}catch (Exception ex){
			return new File(backupDir + File.separator + "SMART_" + format.format(new Date()) + ".bak.zip").getAbsolutePath(); 
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
				throw new IllegalStateException("Output file '" + outputFile.getAbsolutePath() + "' could not be deleted.");
			}
		}
		if (!outputFile.getParentFile().exists()){
			//try to create backup directory
			try{
				SmartUtils.createDirectory(outputFile.getParentFile());
			}catch (Exception ex){
				throw new IllegalStateException("Output directory : " + outputFile.getParentFile().toString() + " could not be created. \n\n" + ex.getMessage(), ex);
			}
		}
		
		Session session = HibernateManager.openSession();
		try{
			try{
				//here we freeze the database to put it into a state for copying
				Query q = session.createSQLQuery("CALL SYSCS_UTIL.SYSCS_FREEZE_DATABASE()");
				q.executeUpdate();
				File filestore = new File (SmartProperties.getInstance().getProperty(SmartProperties.FILESTORE_KEY));
				File database = new File (SmartProperties.getInstance().getProperty(SmartProperties.SMART_DB_KEY));
			
				if (!filestore.exists()){
					filestore.mkdir();
				}
				
				File[] dirsToBackup = new File[]{filestore, database};
			
				monitor.beginTask("Backing Up Database and Files", 2);
			
				return ZipUtil.createZip(dirsToBackup, outputFile, monitor);
			}finally{
				//now un-freeze			
				Query q = session.createSQLQuery("CALL SYSCS_UTIL.SYSCS_UNFREEZE_DATABASE()");
				q.executeUpdate();
			}
		}finally{
			session.close();
		}
	}

	public static String getDefaultAutoBackupLocation() {
		// TODO FILL OUT CODE TO setup a default auto-backup lcoation different from the regular backup, or maybe it's the same? 
		String backupDir = SmartProperties.getInstance().getProperty(SmartProperties.BACKUP_DIRECTORY_KEY);
		try{
			return new File(backupDir).getCanonicalPath();
		}catch (Exception ex){
			return new File(backupDir).getAbsolutePath(); 
		}
	}
}
