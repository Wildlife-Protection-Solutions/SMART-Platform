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
package org.wcs.smart.internal.ca.in;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.export.ICaDataImporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB.DbUser;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.export.CaExporter;
import org.wcs.smart.internal.ca.export.PlugInConfigurationExporter;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Conservation importer.
 * 
 * @author Emily
 *
 */
public class CaImporter {

	/**
	 * Export code extension point
	 */
	private static final String IMPORT_EXTENSION_ID = "org.wcs.smart.ca.import"; //$NON-NLS-1$
	
	
	/**
	 * Imports conservation area data from a conservation 
	 * area backup file.
	 * 
	 * @param file the conservation area backup file
	 * @param monitor the progress monitor
	 * @throws Exception
	 */
	public static void importCa(File file, IProgressMonitor monitor) throws Exception{
		CaImporter importer = new CaImporter();
		importer.importCaFromFile(file, monitor);
	}
	
	/**
	 * Imports conservation area data from backup file.
	 * @param f
	 * @param monitor
	 * @throws Exception
	 */
	private void importCaFromFile(File f, IProgressMonitor monitor) throws Exception{
		if (!f.exists()){
			throw new IOException(Messages.CaImporter_Error_CouldNotFindImportFile + f.getAbsolutePath());
		}
		
		List<ICaDataImporter> importers = getImportExtensions();
		
		monitor.beginTask(Messages.CaImporter_Progress_ImportingCa, 50 + importers.size() * 10);
		
		//TODO: consider doing a disk space check to ensure
		//enough disk space for this operation
		monitor.subTask(Messages.CaImporter_Progress_BackupCurrent);
		HibernateManager.endSessionFactory(true);
		File dbBackup = backup();
		monitor.worked(10);
		
		monitor.subTask(Messages.CaImporter_Progress_UnzippingFile);
		File dir = unzipFile(f);
		
		/* need to login as admin user to restore */
		HibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		
		Session session = HibernateManager.openSession();
		try{
			monitor.worked(10);
			
			monitor.subTask(Messages.CaImporter_Progress_ValidatingCaImport);
			byte[] cauuid = validateConservationAreaInfo(dir, session);
			monitor.worked(10);
			
			monitor.subTask(Messages.CaImporter_ValidatingPluginProgressMessage);
			if (!validatePlugInConfiguration(dir, session)){
				return;
			}
			monitor.worked(10);
			
			
			CaImportEngine engine = new CaImportEngine(session, dir, cauuid);
			try{
				for (ICaDataImporter importer : getImportExtensions()){
					importer.importData(engine, new SubProgressMonitor(monitor, 10));
				}
			}catch (Exception ex){
				try{
					try{
						session.close();
					}catch (Exception ex2){}
					HibernateManager.endSessionFactory(true);
					restoreBackup(dbBackup);
				}catch (Exception e){
					throw new Exception(Messages.CaImporter_Error_ImportErrorBackupNoRestored + ex.getLocalizedMessage(), e);
				}
				throw new Exception(Messages.CaImporter_Error_SystemRestored + ex.getLocalizedMessage(), ex);
			}
			monitor.worked(10);			
		}finally{
			monitor.subTask(Messages.CaImporter_Progress_CleanUp);
			try{
				cleanUp(dbBackup);
			}catch (Exception ex){
				SmartPlugIn.log(Messages.CaImporter_Error_CleanUpFailed + dbBackup.toString(), ex);
			}
			try{
				cleanUp(dir);
			}catch (Exception ex){
				SmartPlugIn.log(Messages.CaImporter_Error_CleanUpFailed + dir.toString(), ex);
			}
			
			try{
				if (session.isOpen()){
					session.close();
				}
			}catch (Exception ex){
				SmartPlugIn.log(Messages.CaImporter_Error_CouldNotCloseSession, ex);
			}
			/* disconnect from admin user */
			HibernateManager.endSessionFactory(true);
			monitor.done();
		}
		
	}
	
	
	
	/**
	 * Creates a copy of the existing smart database as a restore point.
	 * @return the name of the copy of the database
	 * @throws Exception
	 */
	private File backup() throws Exception{
		File databaseDir = new File(SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB));
		File copyTo = new File(databaseDir.getParentFile(), "smartdb.bak"); //$NON-NLS-1$
		if (copyTo.exists()){
			FileUtils.deleteDirectory(copyTo);
		}
		
		FileUtils.copyDirectory(databaseDir, copyTo);
		
		return copyTo;
	}
	
	/**
	 * Deletes a given directory
	 * @param backup
	 * @throws Exception
	 */
	private void cleanUp(File backup) throws Exception{
		FileUtils.deleteDirectory(backup);
	}
	
	/**
	 * Restores the provided database backup.
	 * @param backup
	 * @throws Exception
	 */
	private void restoreBackup(File backup) throws Exception{
		File databaseDir = new File(SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB));
		FileUtils.deleteDirectory(databaseDir);
		FileUtils.copyDirectory(backup, databaseDir);
		cleanUp(backup);
	}
	
	
	/**
	 * Reads the conservation area information and validates
	 * that the conservation area does not exist in the
	 * database.
	 * 
	 * @param dir directory for conservation area information file
	 * @param session
	 * @return conservation area uuid
	 * @throws Exception
	 */
	private byte[] validateConservationAreaInfo(File dir, Session session) throws Exception{
		File caInfo = new File(dir, CaExporter.CA_INFO_FILENAME);
		String dbVersion = Messages.SmartPlugIn_UnknownVersion;
		
		BufferedReader reader = new BufferedReader(new FileReader(caInfo));
		byte[] uuid = null;
		
		try{
			String cauuid = reader.readLine();
			if (cauuid == null){
				throw new Exception(Messages.CaImporter_Error_NoCaIdentifierFound);
			}
			String id = reader.readLine();
			String name = reader.readLine();
			reader.readLine();	//description;
			
			String line = reader.readLine();
			if (line != null){
				dbVersion = line;
			}	
			session.beginTransaction();
			try{
				uuid = SmartUtils.decodeHex(cauuid);
				long cnt = (Long)session.createCriteria(ConservationArea.class).add(Restrictions.eq("uuid", uuid)).setProjection(Projections.rowCount()).list().get(0); //$NON-NLS-1$
				if (cnt != 0){				
					throw new Exception(MessageFormat.format(Messages.CaImporter_Error_CaAlreadyExists, new Object[]{name, id}));
				}
			
			}finally{
			
				reader.close();
			}
		}finally{
			reader.close();
		}
		
		/*validate backup file version */
		String smartDbVersion = SmartProperties.getInstance().getProperty(SmartProperties.DB_VERSION_KEY); 
		if (!dbVersion.equals(smartDbVersion)){
			throw new Exception(MessageFormat.format(Messages.CaImporter_InvalidExportVersion, new Object[]{dbVersion, smartDbVersion}));
		}
		return uuid;
	}
	
	/**
	 * Validates that the plugin version are consistent:
	 * <p>
	 * Checks that the software as the same version
	 * of all the plugins included in the backup.
	 * </p><p>
	 * If a plugin is missing from the software and included in the backup
	 * then the user is warned that they the related plugin data
	 * will not be imported and not included in future exports.
	 * </p><p>
	 * If a plugin has a different version from the software
	 * users will be unable to import.  They must update
	 * their systems first.
	 * </p>
	 * 
	 * 
	 * @param dir directory for conservation area information file
	 * @param session
	 * @throws Exception
	 */
	private boolean validatePlugInConfiguration(File dir, Session session) throws Exception{
		File caInfo = new File(new File(dir, CaExporter.DATABASE_DIR), PlugInConfigurationExporter.CONFIG_TABLE_NAME + ".dat"); //$NON-NLS-1$
		
		HashMap<String, String> versions = new HashMap<String, String>();
		CSVReader reader = new CSVReader(new FileReader(caInfo));
		try{
			String[] data= reader.readNext();
			while(data != null){
				versions.put(data[0], data[1]);
				data= reader.readNext();
			}
		}finally{
			reader.close();
		}
		
		session.beginTransaction();
		try{
			final StringBuilder warnings = new StringBuilder();
			for (Entry<String, String> e : versions.entrySet()){
				String requiredVersion = HibernateManager.getPlugInVersion(e.getKey(), session);
				if (requiredVersion == null){
					//plugin not found; warn user
					warnings.append(e.getKey());
					warnings.append(", "); //$NON-NLS-1$
				}else if (  !requiredVersion.equals(e.getValue()) ){
					//versions don't match; don't allow user to 
					throw new Exception(MessageFormat.format(Messages.CaImporter_VersionError, new Object[]{e.getKey(), requiredVersion, e.getValue()}));
				}
			}
			
			
			if (warnings.length() > 0){
				final boolean[] x = new boolean[]{true};
				warnings.deleteCharAt(warnings.length() - 1);
				warnings.deleteCharAt(warnings.length() - 1);
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						String msg = MessageFormat.format(Messages.CaImporter_ExtraDataWarning + "\n\n" + Messages.CaImporter_ExtraDataWarning2 + "\n\n" + Messages.CaImporter_ExtraDataWarning3, new Object[]{warnings.toString()});  //$NON-NLS-1$//$NON-NLS-2$
						if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(), Messages.CaImporter_WarningDialogTitle, msg)){
							x[0] = false;
						}
						
					}});
				if (!x[0]){
					return false;
				}
			}
			
		}finally{
			session.getTransaction().commit();
			reader.close();
		}
		return true;
	}
	
	/**
	 * Creates a temporary directory and unzip the
	 * file to created directory.
	 * 
	 * @param file the file to unzip
	 * @return the temporary directory
	 * @throws Exception
	 */
	private File unzipFile(File file) throws Exception{
		File temp = SmartUtils.createTemporaryDirectory();
		ZipUtil.unzipFolder(file, temp);
		return temp;
	}
	
	/**
	 * @return list of ca data exporter extension points
	 */
	private List<ICaDataImporter> getImportExtensions(){
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<ICaDataImporter> items = new ArrayList<ICaDataImporter>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IMPORT_EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				items.add((ICaDataImporter)e.createExecutableExtension("caImporter")); //$NON-NLS-1$
			}
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return items;
	}
}
