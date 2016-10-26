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
package org.wcs.smart.report;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.OdaDataSourceHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.wcs.smart.birt.ColumnBindingFixer;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.ui.internal.Messages;
import org.wcs.smart.report.model.Report;

/**
 * This class attempts to fix column binding errors for SMART Queries used in reports.
 * It cleans and refreshes the query column cache (using BIRT functions); then updates
 * the column hints (aliases).  This should fix binding problems when new columns 
 * are added to query results or removed from query results (which is likely to happen
 * when the data model changes). 
 * 
 * @author Emily
 *
 */
public class ReportQueryColumnBindingFixer {

	private Report report;
	
	public ReportQueryColumnBindingFixer() {}

	/**
	 * Fix the bindings for the report.
	 * @throws Exception
	 */
	public void fixReport(Report report, IProgressMonitor monitor) throws Exception{
		this.report = report;
		SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();
		ReportDesignHandle rdh = session.openDesign(ReportPlugIn.getDefault().getReportFile(report).getAbsolutePath());
		try{
			fixReport(rdh.getModuleHandle(), new SubProgressMonitor(monitor, 0));
			rdh.save();
		}finally{
			rdh.close();
		}

	}
	
	
	/**
	 * Fix the bindings for the report.
	 * @throws Exception
	 */
	public void fixReport(ModuleHandle reportHandle, IProgressMonitor monitor) throws Exception{

		List<?> datasets = reportHandle.getAllDataSets();
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			final DataSetHandle dataset = (DataSetHandle) iterator.next();
			monitor.subTask(MessageFormat.format(Messages.ReportQueryColumnBindingFixer_ProcessingMsg, dataset.getName() + (report == null ? "" : " [" + report.getName() + "]"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//update any datasets whose source is from SMART
			if (dataset.getDataSource() != null
					&& ((OdaDataSourceHandle) dataset.getDataSource())
							.getExtensionID().equals(SmartConnection.ODA_DATA_SOURCE_ID)) {
			
				ColumnBindingFixer.fixBindings(dataset);				
			}
		}
	}
	
}
