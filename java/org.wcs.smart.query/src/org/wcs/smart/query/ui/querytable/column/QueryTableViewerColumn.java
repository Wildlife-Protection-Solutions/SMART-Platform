package org.wcs.smart.query.ui.querytable.column;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;

public class QueryTableViewerColumn {
	
	private QueryTableColumn column;
	private TableViewerColumn tcolumn;
	
	public QueryTableViewerColumn(TableViewer viewer, QueryTableColumn column){
		this.column = column;
		
		tcolumn = new TableViewerColumn(viewer, SWT.NONE);
		tcolumn.getColumn().setText(column.getName());
		tcolumn.getColumn().setWidth(100);
		tcolumn.setLabelProvider(column.getLabelProvider());
	}
	
	public void show(){
		if (tcolumn.getColumn().getWidth() == 0){
			tcolumn.getColumn().setWidth(100);
			tcolumn.getColumn().setResizable(true);
		}
	}
	public void hide(){
		if (tcolumn.getColumn().getWidth() != 0){
			tcolumn.getColumn().setWidth(0);
			tcolumn.getColumn().setResizable(false);
		}
	}

	public QueryTableColumn getColumn(){
		return this.column;
	}
}
