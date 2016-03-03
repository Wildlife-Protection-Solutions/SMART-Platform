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
package org.wcs.smart.query.common.importexport;

import java.io.Closeable;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryColumn;

/**
 * A query exporter class for exporting observation and patrol
 * query results.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class SimpleQueryExporter {

	private Iterator<? extends IResultItem> data;
	private int dataSize;
	protected List<QueryColumn> queryColumns; 
	protected File outputFile;
	
	/**
	 * Exports all the data.
	 * 
	 * @param monitor a progress monitor for displaying results.
	 * 
	 * @return <code>true</code> if export successful, <code>false</code> otherwise
	 */
	protected boolean export(IProgressMonitor monitor) throws Exception{
		if (data == null) {
			throw new Exception(Messages.SimpleQueryExporter_Error_QueryNotRun);
		}
		
		monitor.beginTask(Messages.SimpleQueryExporter_Progress_ExportingFile, dataSize + 2);
		try {
			monitor.subTask(Messages.SimpleQueryExporter_Progress_InitializingWriter);
			init();
			monitor.worked(1);

			monitor.subTask(Messages.SimpleQueryExporter_Progress_WritingData);
			while (data.hasNext()) {
				IResultItem it = data.next();
				writeRow(it);
				monitor.worked(1);
				if (monitor.isCanceled()){
					return false;
				}
			}

			monitor.subTask(Messages.SimpleQueryExporter_Progress_CleanUp);
			finish();
			monitor.worked(1);

			monitor.done();
			return true;
		} catch (Exception ex) {
			throw new Exception(Messages.SimpleQueryExporter_Error_ExportFailed + ex.getLocalizedMessage(), ex);
		}finally{
			if (data instanceof Closeable){
				((Closeable) data).close();
			}
		}
	}
	
	/**
	 * Executes any tasks required before data is 
	 * written.  Here writers can be initialized
	 * and header lines written.
	 * 
	 * @throws Exception
	 */
	protected abstract void init() throws Exception;
	
	/**
	 * Writes the query results item row.
	 * @param row the row to write
	 * @throws Exception
	 */
	protected abstract void writeRow(IResultItem row) throws Exception;
	
	/**
	 * Executes any  tasks required after all
	 * data is written.
	 * @throws Exception
	 */
	protected abstract void finish() throws Exception;

	/**
	 * Sets the data to export
	 * @param data the query results to export
	 * @param queryColumns the columns to export
	 * @param outputFile the file to export to
	 */
	protected void setData(Collection<? extends IResultItem> data, List<QueryColumn> queryColumns, File outputFile ) {
		this.data = data != null ? data.iterator() : null;
		dataSize = data != null ? data.size() : 0;
		this.queryColumns = queryColumns;
		this.outputFile = outputFile;
	}

	/**
	 * Sets the data to export
	 * @param data the query results to export
	 * @param queryColumns the columns to export
	 * @param outputFile the file to export to
	 */
	protected void setData(IPagedQueryResultSet derbyResult, List<QueryColumn> queryColumns, File outputFile ) {
		this.data = derbyResult != null ? derbyResult.iterator(IPagedQueryResultSet.MAP_PAGE_SIZE) : null;
		this.dataSize = derbyResult != null ? derbyResult.getItemCount() : 0;
		this.queryColumns = queryColumns;
		this.outputFile = outputFile;
	}
	
}
