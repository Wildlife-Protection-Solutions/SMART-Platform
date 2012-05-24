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

import java.util.Comparator;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.observation.ObservationQueryColumn.ColumnType;
import org.wcs.smart.util.SmartUtils;

/**
 * A comparator for comparining query result items
 * and sorting the results table.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryResultItemComparator extends ViewerComparator{

	private TableViewer viewer;
	private QueryTableViewerColumn column = null;
	private int direction = SWT.DOWN;
		
	/**
	 * @param tableViewer table viewer being sorted
	 */
	public QueryResultItemComparator(TableViewer tableViewer){
		this.viewer = tableViewer;
	}	
	
	
	/**
	 * Sets the sort column
	 * @param sort the column to sort on
	 */
	public void setSortColumn(QueryTableViewerColumn sort){			
		if (column != null &&column == sort){
			if (direction == SWT.DOWN){
				direction = SWT.UP;
			}else{
				direction = SWT.DOWN;
			}
		}
		this.column = sort;
		
		viewer.getTable().setSortDirection(direction);
		viewer.getTable().setSortColumn(sort.getTableColumn().getColumn());
		viewer.refresh();
	}
			
		
	/**
	 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Viewer viewer, Object object1, Object object2){
		if (column == null){
			//no sort column picked
			return 0;
		}
		if (direction == SWT.UP){
			return -compareValue((QueryResultItem)object1, (QueryResultItem)object2);
		}else{
			return compareValue((QueryResultItem)object1, (QueryResultItem)object2);
		}
	}
		
	
	/**
	 * Compares to query result items
	 * @param s1
	 * @param s2
	 * @return
	 */
	private int compareValue(QueryResultItem s1, QueryResultItem s2){
		if (s1 == null && s2 == null){
			return 0;
		}else if (s1== null && s2 != null){
			return 1;
		}else if (s1 != null && s2 == null){
			return -1;
		}			
	
		Object data1 = column.getColumn().getValue(s1);
		Object data2 = column.getColumn().getValue(s2);
		
		
		ColumnType type = column.getColumn().getType();
		Comparator compare = null;
		if (type == ColumnType.INTEGER){
			compare = SmartUtils.nullIntegerComparator;
		}else if (type == ColumnType.DATE){
			compare = SmartUtils.nullDateComparator;
		}else if (type == ColumnType.BOOLEAN){
			compare = SmartUtils.nullBooleanComparator;
		}else if (type == ColumnType.NUMBER){
			compare = SmartUtils.nullDoubleComparator;
		}if (type == ColumnType.STRING){
			compare = SmartUtils.nullStringComparator;
		}
		if (compare != null){
			return compare.compare(data1, data2);
		}
		return 0;
	}
}
