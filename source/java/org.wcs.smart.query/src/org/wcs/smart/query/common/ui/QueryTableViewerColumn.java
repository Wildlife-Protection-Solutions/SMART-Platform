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

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.util.SmartUtils;

/**
 * A table viewer column for the query results table viewer.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryTableViewerColumn {
	
	private QueryColumn column;
	private TableViewerColumn tcolumn;

	/**
	 * Adds the given column to the table viewer.
	 * 
	 * @param viewer the table viewer
	 * @param column the column
	 */
	public QueryTableViewerColumn(TableViewer viewer, QueryColumn column, final IQueryColumnSorter sorter, CellLabelProvider lblProvider) {
		this.column = column;
		
		tcolumn = new TableViewerColumn(viewer, SWT.NONE);
		tcolumn.getColumn().setText(SmartUtils.formatStringForLabel(column.getName()));
		tcolumn.getColumn().setWidth(100);
		if (column.isVisible()){
			show();
		}else{
			hide();
		}
		tcolumn.setLabelProvider(lblProvider);
		
		if (lblProvider instanceof ReprojectingQueryColumnLabelProvder){
			tcolumn.getColumn().setToolTipText( ((ReprojectingQueryColumnLabelProvder)lblProvider).getProjection().getName() );
		}
		tcolumn.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				sorter.setSortColumn(QueryTableViewerColumn.this);
			}
			
		});
	}
	
	public boolean isVisible(){
		return tcolumn.getColumn().getWidth() > 0;
	}
	/**
	 * Shows the column
	 */
	public void show(){
		if (tcolumn.getColumn().getWidth() == 0){
			tcolumn.getColumn().setWidth(100);
			tcolumn.getColumn().setResizable(true);
		}
	}
	
	/**
	 * Hides the column
	 */
	public void hide(){
		if (tcolumn.getColumn().getWidth() != 0){
			tcolumn.getColumn().setWidth(0);
			tcolumn.getColumn().setResizable(false);
		}
	}

	/**
	 * @return the query results column represented by this table column
	 */
	public QueryColumn getColumn(){
		return this.column;
	}
	
	/**
	 * @return the table column 
	 */
	public TableViewerColumn getTableColumn(){
		return this.tcolumn;
	}
}
