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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.data.ui.dataset.DataSetUIUtil;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.OdaDataSourceHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.birt.report.model.api.StructureFactory;
import org.eclipse.birt.report.model.api.elements.structures.CachedMetaData;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
import org.eclipse.birt.report.model.api.elements.structures.OdaResultSetColumn;
import org.eclipse.birt.report.model.api.elements.structures.ResultSetColumn;
import org.eclipse.birt.report.model.elements.interfaces.IDataSetModel;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Display;
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
	@SuppressWarnings("unchecked")
	public void fixReport(ModuleHandle reportHandle, IProgressMonitor monitor) throws Exception{

		List<?> datasets = reportHandle.getAllDataSets();
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			final DataSetHandle dataset = (DataSetHandle) iterator.next();
			monitor.subTask(MessageFormat.format(Messages.ReportQueryColumnBindingFixer_ProcessingMsg, dataset.getName() + (report == null ? "" : " [" + report.getName() + "]"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//update any datasets whose source is from SMART
			if (dataset.getDataSource() != null
					&& ((OdaDataSourceHandle) dataset.getDataSource())
							.getExtensionID().equals(SmartConnection.ODA_DATA_SOURCE_ID)) {
			
				// refresh the columns in the query
				//not running in display thread causes invalid thread access
				runInDisplayThread(new Callable(){
					@Override
					public void execute() throws Exception {
						DataSetUIUtil.updateColumnCacheAfterCleanRs(dataset);
					}	
				});
				
				// update the column hints to include any new columns (in
				// particular the geometry columns); and remove any old
				//columns
				List<ResultSetColumn> metadata = (List<ResultSetColumn>) ((CachedMetaData) dataset
						.getProperty(IDataSetModel.CACHED_METADATA_PROP))
						.getProperty(dataset.getModule(), IDataSetModel.RESULT_SET_PROP);
				
				ArrayList<OdaResultSetColumn> resultset = (ArrayList<OdaResultSetColumn>) dataset.getProperty(IDataSetModel.RESULT_SET_PROP);
				final PropertyHandle columnHint = dataset.getPropertyHandle(IDataSetModel.COLUMN_HINTS_PROP);
				ArrayList<ColumnHint> hints = (ArrayList<ColumnHint>) dataset.getProperty(IDataSetModel.COLUMN_HINTS_PROP);
				
				ArrayList<ColumnHint> newHints = new ArrayList<ColumnHint>();
				for (OdaResultSetColumn c : resultset) {
					boolean found = false;
					if (hints != null){
						for (ColumnHint h : hints) {
							if (((String) h.getProperty(dataset.getModule(),ColumnHint.COLUMN_NAME_MEMBER)).equalsIgnoreCase(c.getColumnName())) {
								found = true;
								newHints.add(h);
								break;
							}
						}
					}
					if (!found) {
						// create a new column hint
						ColumnHint newHint = StructureFactory.createColumnHint();
						// metadata.get
						String name = c.getColumnName();
						for (ResultSetColumn cc : metadata) {
							if (cc.getPosition() == c.getPosition()) {
								name = cc.getColumnName();
								break;
							}
						}
						newHint.setProperty(ColumnHint.COLUMN_NAME_MEMBER,c.getColumnName());
						newHint.setProperty(ColumnHint.ALIAS_MEMBER, name);
						newHint.setProperty(ColumnHint.DISPLAY_NAME_MEMBER,name);
						newHint.setProperty(ColumnHint.HEADING_MEMBER, name);
						newHints.add(newHint);
					}
				}
				
				List<String> aliases = new ArrayList<String>();

				runInDisplayThread(new Callable(){
					@Override
					public void execute() throws Exception {
						//not running in display thread causes invalid thread access
						columnHint.clearValue();
					}	
				});
				
				for (final ColumnHint h : newHints){
					String alias = (String)h.getProperty(dataset.getModule(), ColumnHint.ALIAS_MEMBER);
					if (alias != null){
						//ensure duplicate aliases are not used
						if (aliases.contains(alias)){
							String root = alias;
							int cnt = 1;
							while(aliases.contains(alias)){
								alias = root + "_" + (cnt++); //$NON-NLS-1$
							}
							h.setProperty(ColumnHint.ALIAS_MEMBER, alias);
						}
						aliases.add(alias);
					}
					runInDisplayThread(new Callable(){
						@Override
						public void execute() throws Exception {
							//not running in display thread causes invalid thread access
							columnHint.addItem(h);
						}	
					});
					
				}
				
			}
		}
	}
	
	private void runInDisplayThread(final Callable c) throws Exception{
		final Exception[] ex = {null};
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				try {
					c.execute();
				} catch (Exception e) {
					ex[0] = e;
				}		
			}
		});
		if (ex[0] != null) throw ex[0];
	}
	
	private interface Callable{
		public void execute() throws Exception;
	}
}
