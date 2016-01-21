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
package org.wcs.smart.intelligence;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ca.export.ICaDataExportEngine;
import org.wcs.smart.ca.export.ICaDataExporter;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Informant;

/**
 * Informant Data Exporter that allow user to exclude secure informant data from export.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class InformantDataExporter implements ICaDataExporter {

	private static final String FILESTORE_DIR_NAME = "filestore"; //$NON-NLS-1$

	@Override
	public int getRunLevel() {
		return 20;
	}
	
	@Override
	public void exportData(ICaDataExportEngine exportEngine, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.InformantDataExporter_TaskLabel, 1);
		String aesPath = Informant.getDatastoreFolderPath(exportEngine.getExportLocation() + File.separator + FILESTORE_DIR_NAME);
		File aesFolder = new File(aesPath);
		if (aesFolder.exists() && aesFolder.listFiles().length > 0) {
			final boolean[] result = {false};
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					result[0] = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), Messages.InformantDataExporter_Dialog_Title, Messages.InformantDataExporter_Dialog_Message);
				}
			});
			if (result[0]) {
				FileUtils.forceDelete(aesFolder);
			}
		}
		monitor.worked(1);
		monitor.done();
	}

	/**
	 * No informant data in CCAA analysis
	 * @returns false
	 */
	@Override
	public boolean supportsCcaa() {
		return false;
	}
}
