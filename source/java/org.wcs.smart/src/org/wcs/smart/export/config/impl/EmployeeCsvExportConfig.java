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

import java.text.MessageFormat;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.export.EmployeeCsvExporter;
import org.wcs.smart.export.EmployeeCsvImporter;
import org.wcs.smart.export.config.AbstractCsvExportConfig;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SharedUtils;

/**
 * Configuration for current {@link CsvExportDialog} to export
 * employees into csv file
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class EmployeeCsvExportConfig extends AbstractCsvExportConfig {

	private EmployeeCsvExporter exporter = new EmployeeCsvExporter();

	@Override
	public ICsvDataExporter getExporter() {
		return exporter;
	}
	
	@Override
	public String getDefaultFileName(){
		return SmartDB.getCurrentConservationArea().getId() + "_employees"; //$NON-NLS-1$
	}
	
	@Override
	public boolean includeHasHeader() {
		return true;
	}

	@Override
	public String getHasHeaderText() {
		return Messages.ExportEmployeeDialog_IncludeHaderOp;
	}

	@Override
	public String getInfo() {
		return Messages.ExportEmployeeDialog_CSVFormat_1
				+ SharedUtils.LINE_SEPARATOR
				+ MessageFormat.format(
						Messages.ExportEmployeeDialog_CSVFormat_2, new Object[]{EmployeeCsvImporter.DATE_FORMAT, Employee.DB_MALE + "/" + Employee.DB_FEMALE, EmployeeCsvImporter.DATE_FORMAT, EmployeeCsvImporter.DATE_FORMAT}); //$NON-NLS-1$
	}

	@Override
	public String getTitle() {
		return Messages.ExportEmployeeDialog_DialogTitle;
	}

	@Override
	public String getMessage() {
		return Messages.ExportEmployeeDialog_DialogMessage;
	}

	@Override
	public String getSuccessMessage() {
		return Messages.ExportEmployeeDialog_SuccessMessage;
	}

	@Override
	public String getFailMessage() {
		return Messages.ExportEmployeeDialog_FailureMessage;
	}

}
