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
package org.wcs.smart.i2.ui.editors.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;

/**
 * Lazy query results table for a given query.
 * Provides similar to {@link QueryResultsTable} but uses lazy data provider
 * instead of loading all data into memory.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class QueryLazyResultsTable {

	private QueryLazyResultsContentProvider contentProvider;
	
	protected TableViewer table;
	
	protected QueryTableViewerColumn[] tableViewerColumns;
	
//	private QueryResultItemComparator sorter;
	
	private IProjectionProvider prjProvider;
	
	public void clearColumns(){
		if(table.getTable().isDisposed()) return;
		
		table.getTable().setRedraw(false);
		try{
			for (TableColumn tc : table.getTable().getColumns()){
				tc.dispose();
			}
			tableViewerColumns = null;
//			if (getColumnSorter() != null){
//				getColumnSorter().setSortColumn(null);
//			}
		}finally{
			table.getTable().setRedraw(true);
		}
		setInput(null);
	}
	
	
	public void initQuery(final IntelRecordQuery query, final IProjectionProvider prjProvider){
		this.prjProvider = prjProvider;
		
		if (tableViewerColumns != null){
			//columns already created; lets update visibility
			if (query.getColumnFilter() == null){
				for (QueryTableViewerColumn column : tableViewerColumns){
					column.show();
				}
			}else{
				String[] bits = query.getColumnFilter().split(",");
				Set<String> visibleColumns = Arrays.stream(bits).collect(Collectors.toSet());
				for (QueryTableViewerColumn column : tableViewerColumns){
					if (visibleColumns.contains(column.getColumn().getKey())){
						column.show();
					}else{
						column.hide();
					}
				}
			}
			return;
		}

		//TODO: fix this
		List<IQueryColumn> cols = IntelQueryColumnProvider.getInstance().getQueryColumns(query, Locale.getDefault(), session);
		tableViewerColumns = createColumns(table,cols);
		table.refresh(true);
						
		//TODO: add menu to open source
//		IQueryType queryType = QueryTypeManager.INSTANCE.findQueryType(query.getTypeKey());
//		if (queryType.getResultProviders().length > 0) {
//			Menu menuTable = new Menu(table.getControl());
//			table.getControl().setMenu(menuTable);
//
//			for (final IQueryResultInfoProvider item : queryType.getResultProviders()) {
//				// Create menu item
//				if (!SmartDB.isMultipleAnalysis() || item.supportsCcaa()){
//					MenuItem miTest = new MenuItem(menuTable, SWT.NONE);
//					if (item.getImage() != null){
//						miTest.setImage(item.getImage());
//					}
//					miTest.setText(item.getName());
//					miTest.addSelectionListener(new SelectionAdapter() {
//						@Override
//						public void widgetSelected(SelectionEvent e) {
//							IStructuredSelection selection = (IStructuredSelection) table.getSelection();
//							if (!selection.isEmpty()) {
//								Object x = selection.getFirstElement();
//								item.doWork(x);
//							}
//						}
//					});
//				}
//			}
//		}
					

	}
	
	/**
	 * Creates the table viewer columns.
	 * 
	 * @param viewer table viewer
	 * @param columns table column definition
	 * @return list of table viewer columns
	 */
	private QueryTableViewerColumn[] createColumns(TableViewer viewer, List<IQueryColumn> columns) {
		QueryTableViewerColumn[] viewers = new QueryTableViewerColumn[columns.size()];
		for (int i = 0; i < columns.size(); i++) {
			viewers[i] = new QueryTableViewerColumn(viewer,columns.get(i), getLabelProvider(columns.get(i), columns));
		}
		return viewers;
	}

	/**
	 * Creates label providers for various columns
	 * @param column
	 * @return
	 */
	public CellLabelProvider getLabelProvider(IQueryColumn column, List<IQueryColumn> allColumns){
		return new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof IResultItem){
					return column.getValue((IResultItem)element, Locale.getDefault());
				}
				return "i don't know what to do with: " +element.getClass();
			}
		};
	}
	
	
//	protected IQueryColumnSorter getColumnSorter() {
//		return sorter;
//	}
	
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

//	@Override
//	protected IQueryColumnSorter getColumnSorter() {
//		return contentProvider;
//	}
	
}
