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

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Creates a query results table for a given query.
 * 
 * 
 * @author Emily
 *
 */
public abstract class QueryResultsTable {

	private static final String INITQUERYTABLE_JOBNAME = Messages.QueryResultsTable_InitQueryResultsTableJobName;
	
	protected TableViewer table;
	private QueryTableViewerColumn[] tableViewerColumns;
	
	private QueryResultItemComparator sorter;
	
	/**
	 * Creates the table widget
	 * @param parent the parent widget
	 * @param query the query being represented by the table.
	 * 
	 * @return the resulting table viewer.
	 */
	public TableViewer createTable(Composite parent){
		table = new TableViewer(parent, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);
		table.setContentProvider(ArrayContentProvider.getInstance());
		
		table.setItemCount(0);
		sorter = new QueryResultItemComparator(table);
		table.setComparator(sorter);
		
		return table;
	}
	
	public void clearColumns(){
		if(table.getTable().isDisposed()) return;
		
		table.getTable().setRedraw(false);
		try{
			for (TableColumn tc : table.getTable().getColumns()){
				tc.dispose();
			}
			tableViewerColumns = null;
			if (getColumnSorter() != null){
				getColumnSorter().setSortColumn(null);
			}
		}finally{
			table.getTable().setRedraw(true);
		}
		setInput(null);
	}
	
	
	public void initQuery(final SimpleQuery query){
		if (tableViewerColumns != null){
			//columns already created
			return;
		}
		Job job = new Job(INITQUERYTABLE_JOBNAME){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// in display thread update table
				table.getControl().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (table.getTable().isDisposed()){
							return;
						}
						List<QueryColumn> cols = query.getQueryColumns(Locale.getDefault(), null);
						tableViewerColumns = createColumns(table,cols);
						table.refresh(true);
					}
				});
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
	}
	
	public void initQuery(final GriddedQuery query){
		if (tableViewerColumns != null){
			//columns already created
			return;
		}
		Job job = new Job(INITQUERYTABLE_JOBNAME){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// in display thread update table
				table.getControl().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (table.getTable().isDisposed()){
							return;
						}
						tableViewerColumns = createColumns(table,query.getQueryColumns(Locale.getDefault(), null));
						table.refresh(true);
					}
				});
				return Status.OK_STATUS;
			}
		};
		
		job.schedule();
	}

	
	/**
	 * Updates the items in the table with the new list
	 * <p>If the new list is null; then the table is 
	 * emptied.</p>
	 * 
	 * @param items items to display in table
	 * 
	 */
	public void setInput(Collection<?> items){
		if (!table.getTable().isDisposed()){
			if (items == null){
				table.setItemCount(0);
				table.setInput(new Object[]{});
			}else{
				table.setItemCount(items.size());
				table.setInput(items);
			}
		}
	}
	
	/**
	 * Creates the table viewer columns.
	 * 
	 * @param viewer table viewer
	 * @param columns table column definition
	 * @return list of table viewer columns
	 */
	private QueryTableViewerColumn[] createColumns(TableViewer viewer, List<QueryColumn> columns) {
		QueryTableViewerColumn[] viewers = new QueryTableViewerColumn[columns.size()];
		for (int i = 0; i < columns.size(); i++) {
			viewers[i] = new QueryTableViewerColumn(viewer,columns.get(i), getColumnSorter(), getLabelProvider(columns.get(i), columns));
		}
		return viewers;
	}

	/**
	 * Creates label providers for various columns
	 * @param column
	 * @return
	 */
	public abstract CellLabelProvider getLabelProvider(QueryColumn column, List<QueryColumn> allColumns);
	
	
	protected IQueryColumnSorter getColumnSorter() {
		return sorter;
	}
	
	/**
	 * 
	 * Updates the visible columns in the table.
	 * @param visibleColumns
	 */
	public void updateVisible(List<QueryColumn> queryColumns) {
		if (this.tableViewerColumns == null){
			//not yet initialized 
			return;
		}

		table.getTable().setRedraw(false);
		try{
			if (queryColumns == null){
				//show all
				for (int i = 0; i < tableViewerColumns.length; i ++){
					tableViewerColumns[i].show();
				}
			}else{
				for (int i = 0; i < tableViewerColumns.length; i ++){
					for (QueryColumn column :queryColumns){
						if (column == tableViewerColumns[i].getColumn()){
							if (queryColumns.get(i).isVisible()){
								tableViewerColumns[i].show();
							}else{
								tableViewerColumns[i].hide();						
							}
							break;
						}
					}		
				}
			}
		}finally{
			table.getTable().setRedraw(true);
		}
	}
	

	
}

