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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.export.ICaDataImportEngine;
import org.wcs.smart.ca.export.ICaDataImporter;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.export.CaExporter;
import org.wcs.smart.util.SmartUtils;

/**
 * Imports the conservation area data store.  Imports the entire
 * datastore then deletes all folders that are not used by one of the 
 * plugins currently installed.
 * 
 * @author Emily
 *
 */
public class DatastoreImporter implements ICaDataImporter {

	private static final String FILESTORE_DIR_EXTENSION_ID = "org.wcs.smart.ca.filestoredir"; //$NON-NLS-1$
	
	@Override
	public void importData(ICaDataImportEngine engine, IProgressMonitor monitor)
			throws Exception {
		try{
			monitor.beginTask(Messages.DatastoreImporter_ImportFilestore, 1);	
			importFileStore(engine.getImportDataDirectory(), engine.getConservationAreaUuid(), monitor);
			monitor.worked(1);
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.CaImporter_Error_FilestoreNotImported1 + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}finally{
			monitor.done();
		}
				
	}

	
	/**
	 * Imports the data from the backup filestore into
	 * the current filestore
	 *
	 * @param dir the source directory
	 * @param cauuid the conservation area uuid
	 * @param monitor progress monitor
	 * @throws IOException
	 */
	private void importFileStore(File dir, byte[] cauuid, IProgressMonitor monitor) throws IOException{
		monitor.setTaskName(Messages.CaImporter_Progress_ImportingFileStore);
		File sourceFile = new File(dir, CaExporter.FILESTORE_DIR);
		
		
		String filestore = SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE);
		filestore = filestore + File.separator + SmartUtils.getDirectoryPath(cauuid);
		File destLocation = new File(filestore);
		if (!destLocation.exists()){
			destLocation.mkdir();
		}
		if (sourceFile.isDirectory()){
			FileUtils.copyDirectory(sourceFile, destLocation);
		}
		
		//now we want to remove any directories that are not supported
		//by one of the existing plugins
		List<String> validDirs = getFilestoreDirections();
		for (File f : destLocation.listFiles()){
			if (f.isDirectory()){
				if (!validDirs.contains(f.getName())){
					//this is not supported by any of the plugins so we want to remove it
					try{
						FileUtils.deleteDirectory(f);
					}catch (IOException ex){
						SmartPlugIn.log(ex.getMessage(), ex);
					}
				}
			}
			
		}
	}
	
	/**
	 * @return list of ca data exporter extension points
	 */
	private List<String> getFilestoreDirections(){
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<String> items = new ArrayList<String>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(FILESTORE_DIR_EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				String dir = e.getAttribute("directory"); //$NON-NLS-1$
				if (dir != null && dir.length() > 0){
					items.add(dir);
				}
			}
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return items;
	}
}
