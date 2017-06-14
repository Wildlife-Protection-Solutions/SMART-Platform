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
package org.wcs.smart.query.common.ui;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.model.IColumnAutoConfigQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Lazy query results table for a given query.
 * Provides similar to {@link QueryResultsTable} but uses lazy data provider
 * instead of loading all data into memory.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class QueryLazyResultsTable extends QueryResultsTable {

	private QueryLazyResultsContentProvider contentProvider;
	
	
	/**
	 * Creates the table widget
	 * @param parent the parent widget
	 * @param query the query being represented by the table.
	 * 
	 * @return the resulting table viewer.
	 */
	public TableViewer createTable(Composite parent){
		table = new TableViewer(parent, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.SINGLE);
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);

		contentProvider = new QueryLazyResultsContentProvider(table);
		table.setContentProvider(contentProvider);
		table.setItemCount(0);

		return table;
	}

	public void setInput(IPagedQueryResultSet result) {
		if (!table.getTable().isDisposed()){
			if (result == null){
				table.setItemCount(0);
				table.setInput(null);
			}else{
				table.setItemCount(result.getItemCount());
				table.setInput(result);
			}

			//update tooltip
			for(QueryTableViewerColumn t : tableViewerColumns){
				if (t.getColumn().getTooltip() != null){
					t.getTableColumn().getColumn().setToolTipText(t.getColumn().getTooltip());
				}
			}

		}
	}

	@Override
	protected IQueryColumnSorter getColumnSorter() {
		return contentProvider;
	}
	
	@Override
	public boolean isColumnDisplayed(SimpleQuery query, QueryColumn c) {
		if (query instanceof IColumnAutoConfigQuery) {
			IColumnAutoConfigQuery q = (IColumnAutoConfigQuery) query;
			if (q.isShowDataColumnsOnly()) {
				return contentProvider.isColumnDisplayed(c);
			}
		}
		return super.isColumnDisplayed(query, c);
	}


}
