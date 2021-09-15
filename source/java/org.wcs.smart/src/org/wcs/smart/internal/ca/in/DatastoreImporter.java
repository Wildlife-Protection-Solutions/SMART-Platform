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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.ca.export.ICaDataImportEngine;
import org.wcs.smart.ca.export.ICaDataImporter;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Imports the conservation area data store.  Imports the entire
 * datastore then deletes all folders that are not used by one of the 
 * plugins currently installed.
 * 
 * @author Emily
 *
 */
public class DatastoreImporter implements ICaDataImporter {

	private static final String FILESTORE_DIR_EXTENSION_ID = "org.wcs.smart.caFilestoreDir"; //$NON-NLS-1$
	
	@Override
	public void importData(ICaDataImportEngine engine, IProgressMonitor monitor)
			throws Exception {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DatastoreImporter_ImportFilestore, 1);
		try{
			importFileStore(engine.getImportDataDirectory(), engine.getConservationAreaUuid(), progress.split(1));
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.CaImporter_Error_FilestoreNotImported1 + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
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
	private void importFileStore(Path dir, UUID cauuid, IProgressMonitor monitor) throws IOException{
		monitor.subTask(Messages.CaImporter_Progress_ImportingFileStore);
		Path sourceFile = dir.resolve(ICaDataExportEngine.FILESTORE_DIR);
		
		
		Path destLocation = Paths.get(SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE))
				.resolve(UuidUtils.getDirectoryPath(cauuid));
		
		if (!Files.exists(destLocation)){
			SmartUtils.createDirectory(destLocation);
		}
		if (Files.isDirectory(sourceFile)){
			SmartUtils.copyDirectory(sourceFile, destLocation);
		}
		
		//now we want to remove any directories that are not supported
		//by one of the existing plugins
		List<String> validDirs = getFilestoreDirections();
		
		try(Stream<Path> stream = Files.list(destLocation)){
			stream.forEach(f->{
				if (Files.isDirectory(f)) {
					if (!validDirs.contains(f.getFileName().toString())){
						//this is not supported by any of the plugins so we want to remove it
						try{
							SmartUtils.deleteDirectory(f);
						}catch (IOException ex){
							SmartPlugIn.log(ex.getMessage(), ex);
						}
					}
				}
			});
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
