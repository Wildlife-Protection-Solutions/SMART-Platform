package org.wcs.smart.query.ui.querytable;

import java.util.List;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.ui.QueryResultLazyContentProvider;
import org.wcs.smart.query.ui.querytable.column.QueryTableViewerColumn;

/**
 * The query results table.
 * 
 * @author Emily
 *
 */
public class QueryResultsTable {

	private TableViewer table;
	private QueryTableViewerColumn[] columns;

	public QueryResultsTable(){
		
	}
	
	public QueryTableViewerColumn[] getColumns(){
		return this.columns;
	}
	
	public TableViewer createTable(Composite parent){
		Session session = HibernateManager.openSession();
		
		DataModel dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
		
		table = new TableViewer(parent, SWT.BORDER | SWT.VIRTUAL);
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);
		table.setItemCount(0);
		
		this.columns = QueryResultsTableColumn.createColumns(table, dm);
		table.setContentProvider(new QueryResultLazyContentProvider());
		
		session.close();
		return table;
	}
	
	public void setInput(List<QueryResultItem> items){
		table.setItemCount(items.size());
		table.setInput(items);
	}
}
