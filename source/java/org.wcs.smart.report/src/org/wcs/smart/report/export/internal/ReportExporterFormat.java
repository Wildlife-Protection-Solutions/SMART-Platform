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
package org.wcs.smart.report.export.internal;

import org.wcs.smart.report.export.IExportFormat;
import org.wcs.smart.report.export.IReportExporter;

/**
 * Export format wrapper for report exporters.
 * @see IReportExporter
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportExporterFormat implements IExportFormat{

	private IReportExporter exporter;
	
	public ReportExporterFormat(IReportExporter exporter){
		this.exporter = exporter;
	}
	
	/**
	 * @see org.wcs.smart.report.export.IExportFormat#getName()
	 */
	@Override
	public String getName() {
		return exporter.getName();
	}

	/**
	 * @see org.wcs.smart.report.export.IExportFormat#getFileExtension()
	 */
	@Override
	public String getFileExtension() {
		return exporter.getExportFormat();
	}

	/**
	 * @see org.wcs.smart.report.export.IExportFormat#getExporter()
	 */
	@Override
	public Object getExporter() {
		return exporter;
	}
}
