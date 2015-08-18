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
package org.wcs.smart.export.config.impl;

import org.wcs.smart.export.StationCsvExporter;
import org.wcs.smart.export.StationCsvImporter;
import org.wcs.smart.export.config.AbstractCsvImportConfig;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.config.ICsvDataImporter;
import org.wcs.smart.export.dialog.CsvImportDialog;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SharedUtils;

/**
 * Configuration for current {@link CsvImportDialog} to import
 * stations into csv file
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class StationCsvImportConfig extends AbstractCsvImportConfig {

	private StationCsvExporter exporter = new StationCsvExporter();
	private StationCsvImporter importer = new StationCsvImporter();
	
	@Override
	public ICsvDataImporter getImporter() {
		return importer;
	}

	@Override
	public String getDefaultFileName(){
		return SmartDB.getCurrentConservationArea().getId() + "_Stations.csv"; //$NON-NLS-1$
	}
	
	@Override
	public ICsvDataExporter getExporter() {
		return exporter;
	}
	
	@Override
	public boolean includeHasHeader() {
		return false;
	}

	@Override
	public String getHasHeaderText() {
		return Messages.StationCsvImportConfig_HeaderRequired;
	}

	@Override
	public String getInfo() {
		return Messages.StationCsvImportConfig_Message1
				+ SharedUtils.LINE_SEPARATOR
				+Messages.StationCsvImportConfig_Message2
				+ SharedUtils.LINE_SEPARATOR + SharedUtils.LINE_SEPARATOR
				+ Messages.StationCsvImportConfig_Message3;
	}

	@Override
	public String getTitle() {
		return Messages.StationCsvImportConfig_Title;
	}

	@Override
	public String getMessage() {
		return Messages.StationCsvImportConfig_Message;
	}

	@Override
	public String getSuccessMessage() {
		return Messages.StationCsvImportConfig_OkMessage;
	}

	@Override
	public String getFailMessage() {
		return Messages.StationCsvImportConfig_ErrorMessage;
	}

}
