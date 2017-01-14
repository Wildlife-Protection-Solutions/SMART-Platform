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

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IResultItem;

/**
 * Displays results of a query in a table lazily loading
 * data from the database as requested.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class QueryLazyResultsTable extends Composite{

	protected TableViewer table;
	private Label resultCnt;
	private Menu tableMenu;
	
	protected QueryTableViewerColumn[] tableViewerColumns;
	
	private IPagedQueryResultSet currentResults;
	
	private IntelRecordObservationQuery query;
	
	public QueryLazyResultsTable(Composite parent){
		super(parent, SWT.NONE);
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		Composite comp = new Composite(this, SWT.NONE);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)comp.getLayout()).marginWidth = 0;
		((GridLayout)comp.getLayout()).marginHeight = 0;
		
		resultCnt = new Label(comp, SWT.NONE);
		resultCnt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		resultCnt.setText("Counter");
		
		createTable(comp);
		table.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		addListener(SWT.Dispose, (event)->disposeResults());
	}
	
	public void setQuery(IntelRecordObservationQuery query){
		this.query = query;
	}

	
	/*
	 * Creates the table widget
	 */
	private void createTable(Composite parent){
		table = new TableViewer(parent, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.SINGLE);
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);
		table.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setContentProvider(new QueryLazyResultsContentProvider(table));
		table.setItemCount(0);
	}	
	
	private void createTableColumns(List<IQueryColumn> columns){
		if (table.getTable().isDisposed()) return;
		
		//dispose of existing columns;
		table.getTable().setRedraw(false);
		try{
			for (TableColumn tc : table.getTable().getColumns()) tc.dispose();
			tableViewerColumns = null;
			
			tableViewerColumns = new QueryTableViewerColumn[columns.size()];
			for (int i = 0; i < columns.size(); i++) {
				tableViewerColumns[i] = new QueryTableViewerColumn(table,columns.get(i), ColumnLabelProviderGenerator.createLabelProvider(columns.get(i)), this);
			}
			
			addQueryFinders();
		}finally{
			table.getTable().setRedraw(true);	
		}
	}
	
	private void addQueryFinders(){
		if (query == null) return;
		List<IQuerySourceFinder> finders = IQuerySourceFinder.getQuerySources(query);
		if (tableMenu != null && !tableMenu.isDisposed()){
			tableMenu.dispose();
		}
		if (finders != null && finders.size() > 0){
			tableMenu = new Menu(table.getControl());
			table.getControl().setMenu(tableMenu);
			
			for (IQuerySourceFinder f : finders){
				MenuItem item = new MenuItem(tableMenu, SWT.PUSH);
				if (f.getImage() != null){
					item.setImage(f.getImage());
				}
				item.setText(f.getName());
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						IStructuredSelection selection = (IStructuredSelection) table.getSelection();
						if (!selection.isEmpty()) {
							Object x = selection.getFirstElement();
							if (x instanceof IResultItem) f.runAction((IResultItem)x);
						}
					}
				});
			}
		}
	}
	/*
	 * disposes of query results 
	 */
	private void disposeResults(){
		if (currentResults == null) return;
		final IPagedQueryResultSet toDispose = currentResults;
		Job disposeJob = new Job("dispose query results"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					toDispose.dispose(s);
					s.getTransaction().commit();
				}catch (Exception ex){
					Intelligence2PlugIn.log("Error disposing of query results", ex);
				}finally{
					s.close();
				}
				return Status.OK_STATUS;
			}
		};
		disposeJob.schedule();
	}
	
	public IPagedQueryResultSet getCurrentResults(){
		return this.currentResults;
	}
	
	/**
	 * Sets the table input
	 * @param result
	 */
	public void setInput(IPagedQueryResultSet result) {
		if (table.getTable().isDisposed()) return;
		disposeResults();
		
		currentResults = result;
		
		if (result == null){
			table.setItemCount(0);
			table.setInput(null);
			resultCnt.setText("");
		}else{
			table.setItemCount(result.getItemCount());
			table.setInput(result);	
			resultCnt.setText(MessageFormat.format("{0} observations",result.getItemCount()));
			
			createTableColumns( result.getQueryColumns() );
		}

		//update tooltip
		if (tableViewerColumns != null){
			for(QueryTableViewerColumn t : tableViewerColumns){
				if (t.getColumn().getTooltip() != null){
					t.getTableColumn().getColumn().setToolTipText(t.getColumn().getTooltip());
				}
			}
		}		
	}

	/**
	 * Sets the sort column & updates the query results
	 * 
	 * @param sortColumn
	 */
	public void setSortColumn(QueryTableViewerColumn sortColumn){
		if (currentResults != null){
			int sortDirection = table.getTable().getSortDirection();
			if (table.getTable().getSortColumn() == sortColumn.getTableColumn().getColumn()){
				if (sortDirection == SWT.DOWN || sortDirection == SWT.NONE){
					sortDirection = SWT.UP;
				}else{
					sortDirection = SWT.DOWN;
				}
			}else{
				sortDirection = SWT.UP;
			}
			table.getTable().setSortDirection(sortDirection);
			table.getTable().setSortColumn(sortColumn.getTableColumn().getColumn());

			//refresh results table
			currentResults.setSorting(sortColumn.getColumn(), sortDirection == SWT.UP ? IPagedQueryResultSet.SortDirection.UP : IPagedQueryResultSet.SortDirection.DOWN);
			table.setItemCount(0);
			table.setItemCount(currentResults.getItemCount());
			table.refresh(true);
		}
	}
	
}
