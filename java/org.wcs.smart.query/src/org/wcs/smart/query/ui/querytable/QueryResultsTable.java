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
package org.wcs.smart.query.ui.querytable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolOptions;
import org.wcs.smart.query.model.QueryResultItem;

/**
 * Creates a query results table for a given query.
 * 
 * 
 * @author Emily
 *
 */
public class QueryResultsTable {

	private TableViewer table;
	private QueryTableColumn[] columns;
	private QueryTableViewerColumn[] tableViewerColumns;

	private QueryResultItemComparator sorter;
	
	public QueryTableColumn[] getColumns(){
		return this.columns;
	}
	/**
	 * Creates the table widget
	 * @param parent the parent widget
	 * @param query the query being represented by the table.
	 * 
	 * @return the resulting table viewer.
	 */
	public TableViewer createTable(Composite parent){
		table = new TableViewer(parent, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION);
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);
		table.setItemCount(0);
		sorter = new QueryResultItemComparator(table);
		table.setComparator(sorter);
		
		//lets start a job to get the table column
		Job job = new Job("Initialize query results table."){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				
				try {
					PatrolOptions patrolOps = PatrolHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
					session.beginTransaction();
					DataModel dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
					columns = findTableColumns(dm, patrolOps);

				} finally {
					session.getTransaction().rollback();
					session.close();
				}
				//in display thread update table
				Display.getDefault().asyncExec(new Runnable(){

					@Override
					public void run() {
						tableViewerColumns = createColumns(table, columns, sorter);
						//table.setContentProvider(new QueryResultLazyContentProvider());
						table.setContentProvider(ArrayContentProvider.getInstance());
						updateVisible(visibleColumnKeys);
						visibleColumnKeys = null;
					}});

				return Status.OK_STATUS;
			}
			
		};
		job.schedule();
		
		
		return table;
	}
	
	/**
	 * Updates the items in the table with the new list
	 * <p>If the new list is null; then the table is 
	 * emptied.</p>
	 * 
	 * @param items items to display in table
	 * 
	 */
	public void setInput(List<QueryResultItem> items){
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
	 * Finds all the table columns available for query output.
	 * 
	 * @param dm the conservation area data model
	 * @param ops the patrol options 
	 * 
	 * @return an array of query table columns
	 */
	private QueryTableColumn[] findTableColumns(DataModel dm, PatrolOptions ops) {
		ArrayList<QueryTableColumn> cols = new ArrayList<QueryTableColumn>();
		for (int i = 0; i < FixedTableColumn.FIXED_COLUMN.values().length; i++) {
			
			FixedTableColumn.FIXED_COLUMN item = FixedTableColumn.FIXED_COLUMN.values()[i];
			if (item == FixedTableColumn.FIXED_COLUMN.WAYPOINT_DIRECTION ||  
					item == FixedTableColumn.FIXED_COLUMN.WAYPOINT_DISTANCE){
				if (ops.getTrackDistanceDirection()){
					cols.add(new FixedTableColumn(item));
				}
			}else{
				cols.add(new FixedTableColumn(item));
			}
		}


		// add data model category columns
		int numCategory = 0;
		for (Category cat : dm.getActiveCategories()) {
			numCategory = Math.max(numCategory, getDepth(cat));
		}

		for (int i = 0; i < numCategory; i++) {
			cols.add(new CategoryTableColumn("Observation Category  " + i, i));
		}

		for (Attribute att : dm.getAttributes()) {
			String name = att.getName();
			cols.add(new AttributeTableColumn(name, att.getKeyId(), att.getType()));
		}

		return cols.toArray(new QueryTableColumn[cols.size()]);
	}

	/**
	 * Creates the table viewer columns.
	 * 
	 * @param viewer table viewer
	 * @param columns table column definition
	 * @return list of table viewer columns
	 */
	private static QueryTableViewerColumn[] createColumns(TableViewer viewer, QueryTableColumn[] columns, QueryResultItemComparator sorter) {
		QueryTableViewerColumn[] viewers = new QueryTableViewerColumn[columns.length];
		for (int i = 0; i < columns.length; i++) {
			viewers[i] = new QueryTableViewerColumn(viewer,columns[i], sorter);
		}
		return viewers;
	}

	/**
	 * Compute the maximum category depth.
	 * 
	 * @param cat category
	 * @return maximum depth
	 */
	private int getDepth(Category cat) {
		int maxDepth = 0;
		for (Category child : cat.getChildren()) {
			if (child.getIsActive()) {
				maxDepth = Math.max(maxDepth, getDepth(child));
			}
		}
		return maxDepth + 1;

	}
	
	private Set<String> visibleColumnKeys = null;
	/**
	 * 
	 * Updates the visible columns in the table.
	 * @param visibleColumns
	 */
	public void updateVisible(Set<String> visibleColumnKeys) {
		if (this.tableViewerColumns == null){
			//not yet initialized; save to be used when 
			//initialized
			this.visibleColumnKeys = visibleColumnKeys;
			return;
		}
		if (visibleColumnKeys == null){
			//show all
			for (int i = 0; i < tableViewerColumns.length; i ++){
				tableViewerColumns[i].show();
			}
		}else{
			for (int i = 0; i < tableViewerColumns.length; i ++){
				if (visibleColumnKeys.contains(tableViewerColumns[i].getColumn().getKey())){
					tableViewerColumns[i].show();
				}else{
					tableViewerColumns[i].hide();
				}
			}
		}
	}
	


}
