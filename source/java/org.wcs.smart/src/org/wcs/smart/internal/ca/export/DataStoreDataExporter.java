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

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.ca.export.CaExporter;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.ca.export.ICaDataExporter;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Conservation area data exporter that
 * includes the conservation area folder
 * in the filestore in the export. 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DataStoreDataExporter implements ICaDataExporter {

	public DataStoreDataExporter() {
	}

	@Override
	public int getRunLevel() {
		return 0;
	}
	
	/**
	 * <p>Copies the conservation area folder from the filestore
	 * to the export</p>
	 * 
	 * @see org.wcs.smart.ca.export.ICaDataExporter#exportData(org.wcs.smart.ca.export.ICaDataExportEngine, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void exportData(ICaDataExportEngine exportEngine,
			IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.DataStoreDataExporter_progress, 1);
		File filestore = new File(exportEngine.getExportLocation() + File.separator + CaExporter.FILESTORE_DIR);
		SmartUtils.createDirectory(filestore);
		File filestoreLocation = new File(exportEngine.getConservationArea().getFileDataStoreLocation());
		if (filestoreLocation.exists()){
			FileUtils.copyDirectory(filestoreLocation, filestore);
		}
		monitor.worked(1);
		monitor.done();

	}

}
