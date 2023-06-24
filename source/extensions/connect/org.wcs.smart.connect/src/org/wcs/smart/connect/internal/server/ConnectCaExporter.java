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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.ca.export.CaExporter;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.server.ICaExportPreprocessor;

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
	 * 
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor
	 */
	@Override
	public void export(Path destFile, HashMap<String, String> options, IProgressMonitor monitor) throws Exception{
		SubMonitor progress = SubMonitor.convert(monitor, "", 3); //$NON-NLS-1$
		
		ICaDataExportEngine engine = null;
		try{
			engine = exportData(options, progress.split(2));
			
			//delete temporary unnecessary files
			preprocess(engine);
			
			engine.createExportFile(destFile, progress.split(1));
		}catch(OperationCanceledException ex) {
			return;
		}finally{
			if (engine != null) engine.cleanUp();
		}
		
	}
	
	protected void preprocess(ICaDataExportEngine engine){
		for (ICaExportPreprocessor processor: getExportProcessors()){
			processor.processExport(engine);
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
