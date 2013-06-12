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
package org.wcs.smart.query.ui.patrol;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.PatrolQuery;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.observation.QueryColumn;
import org.wcs.smart.query.ui.querytable.QueryResultItemComparator;
import org.wcs.smart.query.ui.querytable.QueryTableViewerColumn;

/**
 * Creates a query results table for a given query.
 * 
 * 
 * @author Emily
 *
 */
public class PatrolResultsTable {

	private TableViewer table;
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
		
		table.setItemCount(0);
		sorter = new QueryResultItemComparator(table);
		table.setComparator(sorter);
		table.setContentProvider(ArrayContentProvider.getInstance());
		
		return table;
	}
	
	public void initQuery(final PatrolQuery query){
		if (tableViewerColumns != null){
			//columns already created
			return;
		}
		Job job = new Job(Messages.PatrolResultsTable_InitTableJobName){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// in display thread update table
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						tableViewerColumns = createColumns(table,query.getQueryColumns(), sorter);
						
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
	public void setInput(Collection<QueryResultItem> items){
		if (!table.getTable().isDisposed()){
			if (items == null){
				table.setItemCount(0);
				table.setInput(new QueryResultItem[]{});
			}else{
				table.setItemCount(items.size());
				table.setInput(items.toArray());
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
	private QueryTableViewerColumn[] createColumns(TableViewer viewer, List<QueryColumn> columns, QueryResultItemComparator sorter) {
		QueryTableViewerColumn[] viewers = new QueryTableViewerColumn[columns.size()];
		for (int i = 0; i < columns.size(); i++) {
			viewers[i] = new QueryTableViewerColumn(
					viewer,columns.get(i), sorter);
		}
		return viewers;
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
	}
	

	
}

