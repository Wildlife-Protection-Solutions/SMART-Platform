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

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.ca.export.ICaDataExporter;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Exports the db_versions table in the CA Export 
 * 
 * @author Emily
 *
 */
public class PlugInConfigurationExporter implements ICaDataExporter {

	public static final String CONFIG_TABLE_NAME = "db_versions"; //$NON-NLS-1$

	@Override
	public int getRunLevel() {
		return 0;
	}

	@Override
	public void exportData(ICaDataExportEngine exportEngine,
			IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.PlugInConfigurationExporter_ExportingVersions, 1);
		exportEngine.writeQuery(CONFIG_TABLE_NAME, "SELECT plugin_id, version FROM " + SmartDB.PLUGIN_VERSION_TBL); //$NON-NLS-1$
		monitor.done();
		
	}

}
