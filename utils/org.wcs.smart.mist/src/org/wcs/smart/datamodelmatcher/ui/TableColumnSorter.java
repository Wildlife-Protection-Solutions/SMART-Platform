package org.wcs.smart.datamodelmatcher.ui;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class TableColumnSorter extends ViewerComparator {
	public static final int ASC = 1;
	public static final int NONE = 0;
	public static final int DESC = -1;

	private int direction = 0;
	private TableColumn column = null;
	private int columnIndex = 0;
	final private TableViewer viewer;
	
	final private SelectionListener selectionHandler = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {

			TableColumnSorter sorter = (TableColumnSorter) TableColumnSorter.this.viewer.getComparator();
			Assert.isTrue(TableColumnSorter.this == sorter);
			TableColumn selectedColumn = (TableColumn) e.widget;
			Assert.isTrue(TableColumnSorter.this.viewer.getTable() == selectedColumn.getParent());
			TableColumnSorter.this.setColumn(selectedColumn);
		
			//TODO
			//supposed to make it only sort once and not each time the data is changed, seems to sort twice though... 
			//when you click the heading to sort and again when the data is changed the first time, then it's fine. weird...
		    stopSorting();
		}
	};
	
	public TableColumnSorter(TableViewer viewer) {
		this.viewer = viewer;
		Assert.isTrue(this.viewer.getComparator() == null);
		viewer.setComparator(this);
		
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
		switch (direction) {
		case ASC:
			table.setSortColumn(selectedColumn);
			table.setSortDirection(SWT.UP);
			break;
		case DESC:
			table.setSortColumn(selectedColumn);
			table.setSortDirection(SWT.DOWN);
			break;
		default:
			table.setSortColumn(null);
			table.setSortDirection(SWT.NONE);
			break;
		}

		TableColumn[] columns = table.getColumns();
		for (int i = 0; i < columns.length; i++) {
			TableColumn theColumn = columns[i];
			if (theColumn == this.column) columnIndex = i;
		}
		viewer.setComparator(null);
		viewer.setComparator(this);
		

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
	
	public void stopSorting(){
		Table table = viewer.getTable();
		this.direction = 0;
		table.setSortColumn(null);
		table.setSortDirection(SWT.NONE);
	}
}