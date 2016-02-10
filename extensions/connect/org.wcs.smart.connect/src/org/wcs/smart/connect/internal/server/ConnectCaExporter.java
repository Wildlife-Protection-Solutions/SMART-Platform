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
package org.wcs.smart.connect.internal.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.export.CaExporter;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.server.ICaExportPreprocessor;
import org.wcs.smart.util.SmartUtils;

/**
 * An extension of the Conservation Area exports that is specific to SMART
 * Connect exports.  This exporter runs the regular export process, but
 * before zipping it runs some additional processors to remove data
 * that should not be sent to Connect.
 * 
 * @author Emily
 *
 */
public class ConnectCaExporter extends CaExporter{

	/**
	 * Overrides the original export process to add the options
	 * to perform some preprocessing on the export file before zipping it up.
	 */
	@Override
	public void export(File destFile, IProgressMonitor monitor) throws Exception{
		
		File tempDir = SmartUtils.createTemporaryDirectory();
		try{
			exportToTempDirectory(tempDir, monitor);
			
			//delete temporary unnecessary files
			preprocess(tempDir);
			
			//zip up 
			zipTempDirectory(tempDir, destFile, monitor);
		}finally{
			try{
				FileUtils.deleteDirectory(tempDir);
			}catch(Exception ex){
				SmartPlugIn.log("Error deleting temporary continaing ca backup." + tempDir.getAbsolutePath(), ex); //$NON-NLS-1$
			}
		}
	}
	
	protected void preprocess(File tempDir){
		for (ICaExportPreprocessor processor: getExportProcessors()){
			processor.processExport(tempDir);
		}
	}
	
	private List<ICaExportPreprocessor> getExportProcessors(){
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<ICaExportPreprocessor> items = new ArrayList<ICaExportPreprocessor>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(ICaExportPreprocessor.EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				items.add((ICaExportPreprocessor)e.createExecutableExtension("class")); //$NON-NLS-1$
			}
		}catch (Exception ex){
			ConnectPlugIn.log(ex.getMessage(), ex);
		}
		return items;
	}
}
