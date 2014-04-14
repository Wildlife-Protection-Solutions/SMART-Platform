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
import org.wcs.smart.export.config.AbstractCsvImportConfig;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.export.config.ICsvDataImporter;
import org.wcs.smart.export.dialog.CsvImportDialog;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Configuration for current {@link CsvImportDialog} to import
 * employee into csv file
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class EmployeeCsvImportConfig extends AbstractCsvImportConfig {

	private EmployeeCsvExporter exporter = new EmployeeCsvExporter();
	private EmployeeCsvImporter importer = new EmployeeCsvImporter();
	
	@Override
	public ICsvDataImporter getImporter() {
		return importer;
	}
	
	@Override
	public String getDefaultFileName(){
		return SmartDB.getCurrentConservationArea().getId() + "_employees.csv"; //$NON-NLS-1$
	}

	@Override
	public ICsvDataExporter getExporter() {
		return exporter;
	}
	
	@Override
	public boolean includeHasHeader() {
		return true;
	}

	@Override
	public String getHasHeaderText() {
		return Messages.ImportEmployeeDialog_IncludeHaderOp;
	}

	@Override
	public String getInfo() {
		return Messages.ImportEmployeeDialog_CSVFormat_1
				+ SmartUtils.LINE_SEPARATOR
				+ MessageFormat.format(
						Messages.ImportEmployeeDialog_CSVFormat_2, new Object[]{EmployeeCsvImporter.DATE_FORMAT, Employee.DB_MALE + "/" + Employee.DB_FEMALE, EmployeeCsvImporter.DATE_FORMAT, EmployeeCsvImporter.DATE_FORMAT}) //$NON-NLS-1$
				+ SmartUtils.LINE_SEPARATOR + SmartUtils.LINE_SEPARATOR
				+ Messages.ImportEmployeeDialog_CSVFormat_3;
	}

	@Override
	public String getTitle() {
		return Messages.ImportEmployeeDialog_DialogTitle;
	}

	@Override
	public String getMessage() {
		return Messages.ImportEmployeeDialog_DialogMessage;
	}

	@Override
	public String getSuccessMessage() {
		return Messages.ImportEmployeeDialog_SuccessMessage;
	}

	@Override
	public String getFailMessage() {
		return Messages.ImportEmployeeDialog_FailureMessage;
	}

}
