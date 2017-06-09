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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryEditCommand;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Creates a query results table for a given query.
 * 
 * 
 * @author Emily
 *
 */
public abstract class QueryResultsTable {

	private static final String EDIT_SET_KEY = "EDIT"; //$NON-NLS-1$

	private static final String INITQUERYTABLE_JOBNAME = Messages.QueryResultsTable_InitQueryResultsTableJobName;
	
	protected TableViewer table;
	protected QueryTableViewerColumn[] tableViewerColumns;
	
	private QueryResultItemComparator sorter;
	private boolean editMode = false;
	
	private Query query = null;
	
	
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

	public boolean isColumnDisplayed(final SimpleQuery query, QueryColumn c) {
		return c.isVisible();
	}

	public void updateColumnsVisibility(final SimpleQuery query, final IProjectionProvider prjProvider) {
		List<QueryColumn> cols = query.computeQueryColumns(Locale.getDefault(), null, prjProvider);
		for (QueryTableViewerColumn column : tableViewerColumns){
			column.getColumn().setProjectionProvider(prjProvider);
			for (QueryColumn c : cols){
				if (column.getColumn().equals(c)){
					if (isColumnDisplayed(query, c)){
						column.show();
					}else{
						column.hide();
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Updates the edit mode of the table.
	 * @param canEdit
	 */
	public void setEditMode(boolean canEdit){
		this.editMode = canEdit;
		createMenu();
		for (QueryTableViewerColumn c : tableViewerColumns){
			if (c.getLabelProvider() instanceof QueryColumnLabelProvider){
				((QueryColumnLabelProvider)c.getLabelProvider()).setEditMode(canEdit);
			}
			
			if (editMode){
				EditingSupport support = getEditingSupport(c.getTableColumn().getViewer(), c.getColumn());
				if (support != null){
					c.getTableColumn().setEditingSupport(support);
				}else{
					c.getTableColumn().setEditingSupport(null);
				}
			}else{
				c.getTableColumn().setEditingSupport(null);
			}
		}
		
		if (canEdit && table.getData(EDIT_SET_KEY) == null){
			TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(table, new FocusCellHighlighter(table){});
			ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(table) {
				protected boolean isEditorActivationEvent(
						ColumnViewerEditorActivationEvent event) {
					return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
							|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
							|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
							|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
				}
			};
			
			TableViewerEditor.create(table, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.KEYBOARD_ACTIVATION);
			table.setData(EDIT_SET_KEY, Boolean.TRUE);
		}
		
		table.refresh(true);
	}
	
	private void createMenu(){
		
		Menu menuTable = table.getControl().getMenu();
		if(menuTable != null && !menuTable.isDisposed()){
			menuTable.dispose();
		}
		if (query == null) return;
		IQueryType queryType = QueryTypeManager.INSTANCE.findQueryType(query.getTypeKey());
		if (queryType.getResultProviders().length > 0) {
			menuTable = new Menu(table.getControl());
			table.getControl().setMenu(menuTable);

			List<IQueryEditCommand> editItems = new ArrayList<>();
			for (final IQueryResultInfoProvider item : queryType.getResultProviders()) {
				// Create menu item
				if (!SmartDB.isMultipleAnalysis() || item.supportsCcaa()){
					if (!(item instanceof IQueryEditCommand) || (item instanceof IQueryEditCommand && editMode)){
						if (item instanceof IQueryEditCommand){
							editItems.add((IQueryEditCommand) item);
							continue;
						}
						MenuItem miTest = new MenuItem(menuTable, SWT.NONE);
						if (item.getImage() != null){
							miTest.setImage(item.getImage());
						}
						miTest.setText(item.getName());
						miTest.addListener(SWT.Selection, e->{
							IStructuredSelection selection = (IStructuredSelection) table.getSelection();
							if (!selection.isEmpty()) {
								Object x = selection.getFirstElement();
								item.doWork(x);
							}
						});
					}
				}
			}
			if (!editItems.isEmpty()) new MenuItem(menuTable, SWT.SEPARATOR);
			
			for (final IQueryEditCommand item : editItems) {
				MenuItem miTest = new MenuItem(menuTable, SWT.NONE);
				if (item.getImage() != null){
					miTest.setImage(item.getImage());
				}
				miTest.setText(item.getName());
				miTest.addListener(SWT.Selection, e->{
					IStructuredSelection selection = (IStructuredSelection) table.getSelection();
					if (!selection.isEmpty()) {
						Object x = selection.getFirstElement();
						if (item.doWork(x, query.getCachedResults())){
							if (getTable().getContentProvider() instanceof QueryLazyResultsContentProvider) {
								((QueryLazyResultsContentProvider) getTable().getContentProvider()).clear();
							}
							getTable().refresh(true);
						}
					}
				});
			}
		}
	}
	
	public void initQuery(final SimpleQuery query, final IProjectionProvider prjProvider){
		if (tableViewerColumns != null){
			//columns already created; lets update visibility
			updateColumnsVisibility(query, prjProvider);
			return;
		}

		List<QueryColumn> cols = query.computeQueryColumns(Locale.getDefault(), null, prjProvider);
		tableViewerColumns = createColumns(table,cols);
		table.refresh(true);
		this.query = query;
		createMenu();
	}
	
	public void initQuery(final GriddedQuery query){
		if (tableViewerColumns != null){
			//columns already created
			return;
		}
		this.query = query;
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
						tableViewerColumns = createColumns(table, query.computeQueryColumns(Locale.getDefault(), null));
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
	
	/**
	 * Get the editing support for a given query column.  Can return null
	 * if query column not editable.
	 * 
	 * @param viewer
	 * @param column
	 * @return
	 */
	public EditingSupport getEditingSupport(ColumnViewer viewer, QueryColumn column){
		return null;
	}
	
	protected IQueryColumnSorter getColumnSorter() {
		return sorter;
	}
	
	public TableViewer getTable(){
		return this.table;
	}
	
}

