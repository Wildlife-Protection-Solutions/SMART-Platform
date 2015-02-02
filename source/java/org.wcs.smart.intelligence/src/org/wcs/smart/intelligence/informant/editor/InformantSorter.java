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
package org.wcs.smart.intelligence.informant.editor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.intelligence.model.Informant;

/**
 * Sorter for informants table
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class InformantSorter extends ViewerComparator {

	private int direction = SWT.UP;
	private TableViewer sortTable;
	private Map<TableColumn, Comparator<Informant>> comparatorsMap = new HashMap<>();
	
	/**
	 * Creates a new sorter for given table
	 * @param sortTable
	 */
	public InformantSorter(TableViewer sortTable) {
		this.sortTable = sortTable;
	}
	
	public void set(TableColumn tableColumn, Comparator<Informant> comparator) {
		comparatorsMap.put(tableColumn, comparator);
	}

	public void setSortColumn(TableColumn tableColumn) {
		TableColumn sortColumn = sortTable.getTable().getSortColumn();
		
		if (sortColumn != null && tableColumn == sortColumn){
			if (direction == SWT.DOWN){
				direction = SWT.UP;
			}else{
				direction = SWT.DOWN;
			}
		}
		
		sortTable.getTable().setSortDirection(this.direction);
		sortTable.getTable().setSortColumn(tableColumn);
		sortTable.refresh();
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		TableColumn sortColumn = sortTable.getTable().getSortColumn();
		Comparator<Informant> comparator = comparatorsMap.get(sortColumn);
		if (comparator != null) {
			int result = comparator.compare((Informant) e1, (Informant) e2);
			return direction == SWT.UP ? result : -result;
		}
		return super.compare(viewer, e1, e2);
	}
	
}
