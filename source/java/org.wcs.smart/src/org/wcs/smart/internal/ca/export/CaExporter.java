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
package org.wcs.smart.internal.ca.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.ca.export.ICaDataExporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Main process for exporting a conservation area.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CaExporter {

	/**
	 * The name of the conservation area data file in the exported
	 * data.
	 */
	public static final String CA_INFO_FILENAME = "conservationarea.dat"; //$NON-NLS-1$
	
	/**
	 * The name of the directory where the database data is stored
	 */
	public static final String DATABASE_DIR = "database"; //$NON-NLS-1$
	
	/**
	 * The name of the directory where the filestore data is 
	 * stored
	 */
	public static final String FILESTORE_DIR = "filestore"; //$NON-NLS-1$
	
	
	/**
	 * Export code extension point
	 */
	private static final String EXPORT_EXTENSION_ID = "org.wcs.smart.ca.export"; //$NON-NLS-1$
	
	/**
	 * @return the default backup file name based on the current date
	 */
	public static String getDefaultFileName(){
		String backupDir = SmartProperties.getInstance().getProperty(SmartProperties.PROP_BACKUP_DIR);
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd"); //$NON-NLS-1$
		try{
			return new File(backupDir + File.separator + "SMART_" + SmartDB.getCurrentConservationArea().getId() + "_" + format.format(new Date()) + ".bak.zip").getCanonicalPath(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}catch (Exception ex){
			return new File(backupDir + File.separator + "SMART_" + SmartDB.getCurrentConservationArea().getId() + "_" + format.format(new Date()) + ".bak.zip").getAbsolutePath(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
	
	/**
	 * Exports the current conservation area to the given file.
	 * @param destFile output file
	 * @param monitor progress monitor
	 * 
	 * 
	 */
	public void export(File destFile, IProgressMonitor monitor) throws Exception{
		
		List<ICaDataExporter> exporters = getExportExtensions();
		Collections.sort(exporters, new Comparator<ICaDataExporter>() {

			@Override
			public int compare(ICaDataExporter arg0, ICaDataExporter arg1) {
				return arg0.getRunLevel() - arg1.getRunLevel();
			}
			
		});
		
		Session session = HibernateManager.openSession();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		monitor.beginTask(Messages.CaExporter_ProgressExportCA, (exporters.size() + 2) * 10);
		try{
			File tempDir = SmartUtils.createTemporaryDirectory();
			try{
				/* write a conservation area info file */
				writeConservationAreaInfo(tempDir, ca);
				if (monitor.isCanceled()) return;
				monitor.worked(10);
			
				/* run through the exporters exporting data */
				
				ICaDataExportEngine engine = new DerbyCaDataExportEngine(tempDir, ca, session);
				for (ICaDataExporter exporter: exporters){
					if (monitor.isCanceled()) return;
					exporter.exportData(engine, new SubProgressMonitor(monitor, 10));
					monitor.worked(10);
				}
			
				/* zip up files */
				if (monitor.isCanceled()) return;
				ZipUtil.createZip(tempDir.listFiles(), destFile, new SubProgressMonitor(monitor,10));
				monitor.worked(10);
			}finally{
				try{
					FileUtils.deleteDirectory(tempDir);
				}catch(Exception ex){
					SmartPlugIn.log(Messages.CaExporter_Error_TempDirDelete + tempDir.getAbsolutePath(), ex);
				}
			}
		}finally{
			monitor.done();
			session.close();
		}
	}
	
	/**
	 * Writes a simple text file with conservation area information.
	 * 
	 * @param directory
	 * @param ca
	 * @throws IOException
	 */
	private void writeConservationAreaInfo(File directory, ConservationArea ca) throws IOException{
		FileWriter fw = new FileWriter(new File(directory, CA_INFO_FILENAME));
		fw.write(SmartUtils.encodeHex(ca.getUuid()));
		fw.write(SmartUtils.LINE_SEPARATOR);
		fw.write(ca.getId());
		fw.write(SmartUtils.LINE_SEPARATOR);
		fw.write(ca.getName());
		fw.write(SmartUtils.LINE_SEPARATOR);
		fw.write(ca.getDescription());
		fw.write(SmartUtils.LINE_SEPARATOR);
		fw.write(SmartProperties.getInstance().getProperty(SmartProperties.DB_VERSION_KEY));
		fw.close();
	}
	
	/**
	 * @return list of ca data exporter extension points
	 */
	private List<ICaDataExporter> getExportExtensions(){
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<ICaDataExporter> items = new ArrayList<ICaDataExporter>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXPORT_EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				items.add((ICaDataExporter)e.createExecutableExtension("caExporter")); //$NON-NLS-1$
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}
		return items;
	}
}
