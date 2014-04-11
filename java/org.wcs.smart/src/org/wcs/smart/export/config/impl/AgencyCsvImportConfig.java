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

import org.wcs.smart.export.AgencyCsvExporter;
import org.wcs.smart.export.AgencyCsvImporter;
import org.wcs.smart.export.config.AbstractCsvImportConfig;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.config.ICsvDataImporter;
import org.wcs.smart.export.dialog.CsvImportDialog;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Configuration for current {@link CsvImportDialog} to import
 * agencies and ranks into csv file.
 * 
 * Note the imported imports data into memory, it does not
 * save the results to the database.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class AgencyCsvImportConfig extends AbstractCsvImportConfig {

	AgencyCsvExporter exporter = new AgencyCsvExporter();
	AgencyCsvImporter importer = new AgencyCsvImporter();
	
	@Override
	public ICsvDataImporter getImporter() {
		return importer;
	}

	@Override
	public ICsvDataExporter getExporter() {
		return exporter;
	}
	
	@Override
	public String getDefaultFileName(){
		return SmartDB.getCurrentConservationArea().getId() + "_agencies"; //$NON-NLS-1$
	}
	
	@Override
	public String getInfo() {
		return Messages.CsvConfig_Agency_Import_Info + SmartUtils.LINE_SEPARATOR +
				Messages.CsvConfig_Agency_Import_Info_Content +
				SmartUtils.LINE_SEPARATOR + SmartUtils.LINE_SEPARATOR +
				Messages.CsvConfig_Agency_Example_Label +
				SmartUtils.LINE_SEPARATOR +
				Messages.CsvConfig_Agency_Example_HeaderRow +
				SmartUtils.LINE_SEPARATOR +
				Messages.CsvConfig_Agency_Example_ContentRow;
	}

	@Override
	public String getTitle() {
		return Messages.CsvConfig_Agency_Import_Title;
	}

	@Override
	public String getMessage() {
		return Messages.CsvConfig_Agency_Import_Message;
	}

	@Override
	public String getSuccessMessage() {
		return Messages.CsvConfig_Agency_Import_Success;
	}

	@Override
	public String getFailMessage() {
		return Messages.CsvConfig_Agency_Import_Fail;
	}

}
