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
package org.wcs.smart.report.export;

import java.nio.file.Path;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.report.model.Report;

/**
 * Report exporter interface.
 * 
 * @author egouge
 * @since 1.0.0
 */
public interface IReportExporter {

	/**
	 * Exports the list of files to the provided location
	 * <p>The user can assume they can overwrite the file provided or any files
	 * in the directory if a directory is provider.
	 * </p>
	 * @param file a file if only one report provided, a directory if multiple reports
	 * @param reports the reports to export
	 * @param reportParams report parameters
	 * @param monitor
	 * @throws Exception if error occurs during the export
	 */
	public void exportReport(Path file, Report report, HashMap<String, Object> reportParams, IProgressMonitor monitor) throws Exception ;
	
	/**
	 * @return the exporter name
	 */
	public String getName();
	
	/**
	 * @return the export output format
	 */
	public String getExportFormat();
	
	/**
	 * 
	 * @return <code>true</code> if the exporter needs report parameters populated, <code>false</code> otherwise
	 */
	public boolean requiresParameters();
}
