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
package org.wcs.smart.er.ui.mision.editor;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.mision.editor.MissionDayComposite.OtColumn;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Comparator for waypoint table that sorts waypoints
 * by either time or id.  No other columns are supported for sorting.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class WaypointSorter extends ViewerComparator {
	
	private OtColumn sortColumn;
	private int direction = SWT.DOWN;
	private TableViewer sortTable;
	
	/**
	 * Creates a new sorter for given table
	 * @param sortTable
	 */
	public WaypointSorter(TableViewer sortTable){
		this.sortTable = sortTable;
	}
	
	/**
	 * Sets the sort column.
	 * @param sort the column to sort by
	 * @param tableColumnhe table column
	 */
	public void setSortColumn(
			OtColumn sort, 
			TableColumn tableColumn){
		
		if (sortColumn != null && sort == sortColumn){
			if (direction == SWT.DOWN){
				direction = SWT.UP;
			}else{
				direction = SWT.DOWN;
			}
		}
		this.sortColumn = sort;
		
		sortTable.getTable().setSortDirection(this.direction);
		sortTable.getTable().setSortColumn(tableColumn);
		sortTable.refresh();
	}
	
	@Override
	public int compare(Viewer viewer, Object object1, Object object2){
		if (sortColumn == null){
			//no sort column picked
			return 0;
		}
		if (direction == SWT.UP){
			return -compareValue(((SurveyWaypoint)object1).getWaypoint(), ((SurveyWaypoint)object2).getWaypoint());
		}else{
			return compareValue(((SurveyWaypoint)object1).getWaypoint(), ((SurveyWaypoint)object2).getWaypoint());
		}

	}
	
	private int compareValue(Waypoint wp1, Waypoint wp2){
		if (sortColumn == OtColumn.ID){
			return ((Integer)wp1.getId()).compareTo(((Integer)wp2.getId()));
		}else if (sortColumn == OtColumn.TIME){
			return wp1.getDateTime().compareTo(wp2.getDateTime());
			//return ((Integer)wp1.getTime()).compareTo(((Integer)wp2.getTime()));
		}else{
			//we don't support sorting on other columns at the moment
			return 0;
		}
	}
};

