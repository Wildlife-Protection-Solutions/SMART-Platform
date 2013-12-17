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

package org.wcs.smart.datamodelmatcher.ui;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class TableColumnSorter extends ViewerComparator {
	public static final int ASC = 1;
	public static final int NONE = 0;
	public static final int DESC = -1;

	MatchSession ms;
	
	private int direction = 0;
	private TableColumn column = null;
	private int columnIndex = 0;
	final private TableViewer viewer;
	
	final private SelectionListener selectionHandler = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			TableColumn selectedColumn = (TableColumn) e.widget;
			TableColumnSorter.this.setColumn(selectedColumn);
		}
	};
	
	public TableColumnSorter(TableViewer viewer) {
		this.viewer = viewer;
		Assert.isTrue(this.viewer.getComparator() == null);
		viewer.setComparator(null);
		
		for (TableColumn tableColumn : viewer.getTable().getColumns()) {
			tableColumn.addSelectionListener(selectionHandler);
		}
	}

	public void setColumn(TableColumn selectedColumn) {
		if (column == selectedColumn) {
			switch (direction) {
			case ASC:
				direction = DESC;
				break;
			case DESC:
				direction = ASC;
				break;
			default:
				direction = ASC;
				break;
			}
		} else {
			this.column = selectedColumn;
			this.direction = ASC;
		}

		
		Table table = viewer.getTable();
		
		TableColumn[] columns = table.getColumns();
		for (int i = 0; i < columns.length; i++) {
			TableColumn theColumn = columns[i];
			if (theColumn == this.column) columnIndex = i;
		}
		
		ms.sort(direction, columnIndex);
		viewer.setInput(ms.getRows());
		viewer.refresh();
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		return direction * doCompare(viewer, e1, e2);
	}

	protected int doCompare(Viewer v, Object e1, Object e2) {
       Assert.isTrue(viewer == this.viewer);
       ILabelProvider labelProvider = (ILabelProvider) viewer.getLabelProvider(columnIndex);
		String t1 = labelProvider.getText(e1);
		String t2 = labelProvider.getText(e2);
		if (t1 == null) t1 = "";
		if (t2 == null) t2 = "";
		return t1.compareTo(t2);
	}
	
	public void setMs(MatchSession ms){
		this.ms = ms;
	}

}