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
package org.wcs.smart.ca.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.export.DerbyCaDataExportEngine;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Main process for exporting a conservation area.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CaExporter {

	/**
	 * Export code extension point
	 */
	private static final String EXPORT_EXTENSION_ID = "org.wcs.smart.caExport"; //$NON-NLS-1$
	
	/**
	 * @return the default backup file name based on the current date
	 */
	public static String getDefaultFileName(){
		String backupDir = SmartProperties.getInstance().getProperty(SmartProperties.PROP_BACKUP_DIR);
		
		StringBuilder fname = new StringBuilder();
		fname.append("SMART_"); //$NON-NLS-1$
		fname.append(SmartDB.getCurrentConservationArea().getId());
		fname.append("_"); //$NON-NLS-1$
		fname.append(DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())); //$NON-NLS-1$
		fname.append(".bak.zip"); //$NON-NLS-1$
		
		return Paths.get(backupDir).resolve(fname.toString())
				.normalize().toAbsolutePath().toString(); 
	}
	
	/**
	 * Exports the current conservation area to the given file.
	 * @param destFile output file
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor. Accepts null, indicating that no progress should be
	 * 
	 * 
	 */
	public void export(Path destFile,  HashMap<String,String> options, IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor, Messages.CaExporter_TaskName, 3);
		ICaDataExportEngine engine = null;
		try{
			engine = exportData(options, progress.split(2));
			engine.createExportFile(destFile, progress.split(1));
		}catch(OperationCanceledException ex) {
			return;
		}finally{
			if (engine != null) engine.cleanUp();
		}
	}
	
	/**
	 * 
	 * @param tempDir
	 * @param options
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor
	 * @throws Exception
	 */
	protected ICaDataExportEngine exportData(HashMap<String,String> options, IProgressMonitor monitor) throws Exception{

		SubMonitor progress = SubMonitor.convert(monitor, Messages.CaExporter_ProgressExportCA, 1);
		
		List<ICaDataExporter> exporters = getExportExtensions();
		Collections.sort(exporters, new Comparator<ICaDataExporter>() {
			@Override
			public int compare(ICaDataExporter arg0, ICaDataExporter arg1) {
				return arg0.getRunLevel() - arg1.getRunLevel();
			}	
		});
		
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		progress.setWorkRemaining( (exporters.size() + 2) * 10);
		
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			
			ICaDataExportEngine engine = new DerbyCaDataExportEngine(ca, session);
			
			/* write a conservation area info file */
			writeConservationAreaInfo(engine.getWorkingLocation(), ca);
			progress.checkCanceled();
			progress.worked(10);
			
			/* run through the exporters exporting data */
			
			engine.setExportOptions(options);
			List<ICaDataExporter> toRun = exporters.stream().filter(e->!(ca.getIsCcaa() && !e.supportsCcaa())).collect(Collectors.toList()); 
			
			progress.setWorkRemaining(toRun.size());
			for (ICaDataExporter exporter: toRun) {
				exporter.exportData(engine, progress.split(1));
			}
			
			progress.setWorkRemaining( 0 );
			session.getTransaction().rollback();
			
			return engine;
		}
	}
	
	protected void zipTempDirectory(Path tempDir, Path destFile, IProgressMonitor monitor) throws Exception{
		List<Path> files = null;
		try(Stream<Path> stream = Files.list(tempDir)){
			files = stream.collect(Collectors.toList());
		}
		ZipUtil.createZip(files, destFile, monitor);		
	}
	
	/**
	 * Writes a simple text file with conservation area information.
	 * 
	 * @param directory
	 * @param ca
	 * @throws IOException
	 */
	protected void writeConservationAreaInfo(Path directory, ConservationArea ca) throws IOException{
		try(BufferedWriter fw = Files.newBufferedWriter(directory.resolve(ICaDataExportEngine.CA_INFO_FILENAME))){
			fw.write(UuidUtils.uuidToString(ca.getUuid()));
			fw.write(SharedUtils.LINE_SEPARATOR);
			fw.write(ca.getId());
			fw.write(SharedUtils.LINE_SEPARATOR);
			fw.write(ca.getName());
			fw.write(SharedUtils.LINE_SEPARATOR);
			fw.write(ca.getDescription());
			fw.write(SharedUtils.LINE_SEPARATOR);
			fw.write(SmartProperties.getInstance().getProperty(SmartProperties.DB_VERSION_KEY));
		}
	}
	
	/**
	 * @return list of ca data exporter extension points
	 */
	protected List<ICaDataExporter> getExportExtensions(){
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
