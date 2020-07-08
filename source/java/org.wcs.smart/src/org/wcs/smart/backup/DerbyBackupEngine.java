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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.SmartContext;
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
		return Paths.get(backupDir).resolve("SMART_" + format.format(new Date()) + ".bak.zip").normalize().toAbsolutePath().toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Backs up all SMART data to the given output file.
	 * 
	 * @param outputFile output file
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor. Accepts null, indicating that no progress should be
	 * @return <code>true</code> if backup successful, <code>false</code> if cancelled or failed
	 * @throws IOException
	 */
	//apache derby database backup: http://db.apache.org/derby/docs/10.0/manuals/admin/hubprnt43.html
	public static boolean backupSystem(Path outputFile, IProgressMonitor monitor) throws IOException{
		return backupSystem(outputFile, false, monitor);
	}
	
	/**
	 * Backs up all SMART data to the given output file, with the option to exclude the filestore
	 * and only backup the database.
	 * 
	 * @param outputFile output file
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor. Accepts null, indicating that no progress should be
	 * @param excludeFilestore <code>false</code> to exclude the entire filestore from the database; true otherwise
	 * @return <code>true</code> if backup successful, <code>false</code> if cancelled or failed
	 * @throws IOException
	 */
	public static boolean  backupSystem(Path outputFile, boolean excludeFilestore, IProgressMonitor monitor) throws IOException{
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DerbyBackupEngine_ProgressMessage, 10);
		if (Files.exists(outputFile)){
			try {
				Files.delete(outputFile);
			}catch(IOException ex) {
				throw new IllegalStateException(Messages.DerbyBackupEngine_DeleteOutputFileError + " '" + outputFile.toString() + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if (!Files.exists(outputFile.getParent())){
			//try to create backup directory
			try{
				SmartUtils.createDirectory(outputFile.getParent());
			}catch (Exception ex){
				throw new IllegalStateException(Messages.DerbyBackupEngine_CreateDirectoryError + " '" + outputFile.getParent().toString() + "' \n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				//here we freeze the database to put it into a state for copying
				NativeQuery<?> q = session.createNativeQuery("CALL SYSCS_UTIL.SYSCS_FREEZE_DATABASE()"); //$NON-NLS-1$
				q.executeUpdate();
				Path filestore = Paths.get(SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE));
				Path database = Paths.get(SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB));
							
				Path[] dirsToBackup = new Path[]{database};
				if (!excludeFilestore){
					if (!Files.exists(filestore)){
						SmartUtils.createDirectory(filestore);
					}
					dirsToBackup = new Path[]{filestore, database};
				}
			
				//Exclude temp directory from backup 
				//SmartContext.INSTANCE.getTempFilestoreLocation();			
				Set<Path> itemsToExclude = new HashSet<>();
				itemsToExclude.add(SmartContext.INSTANCE.getTempFilestoreLocation());
				
				if (ZipUtil.createZip(dirsToBackup, outputFile, itemsToExclude, progress.split(9))) {
					List<IBackupContributor> extensions = getBackupExtensions();
					SubMonitor sub = progress.split(1);
					sub.setWorkRemaining(extensions.size());
					for (IBackupContributor ext : extensions) {
						ext.process(outputFile, sub.split(1));
					}
					return true;
				}
				return false;
			}catch (OperationCanceledException ex) {
				return false;
			}finally{
				//now un-freeze			
				NativeQuery<?> q = session.createNativeQuery("CALL SYSCS_UTIL.SYSCS_UNFREEZE_DATABASE()"); //$NON-NLS-1$
				q.executeUpdate();
				session.getTransaction().rollback();
			}
		}
	}

	/**
	 * @return the default auto backup location
	 */
	public static String getDefaultAutoBackupLocation() { 
		String backupDir = SmartProperties.getInstance().getProperty(SmartProperties.PROP_BACKUP_DIR);
		return Paths.get(backupDir).normalize().toAbsolutePath().toString();
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
