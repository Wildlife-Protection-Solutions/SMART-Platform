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
package org.wcs.smart.patrol.query.ui.editor;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.wcs.smart.observation.query.ui.AttributeColumnEditor;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn.FixedColumns;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Editing support manager for editing patrol query results in the 
 * results table.
 * 
 * @author Emily
 *
 */
public class QueryColumnEditingSupport {

	
	public static EditingSupport getCellEditor(ColumnViewer viewer, QueryColumn column, QueryResultsEditor editor){
		if (column instanceof FixedQueryColumn){
			return getCellEditor(viewer, (FixedQueryColumn)column, editor);
		}else if (column instanceof AttributeQueryColumn){
			return getCellEditor(viewer, (AttributeQueryColumn)column, editor);
		}else if (column instanceof CategoryQueryColumn){
			return getCellEditor(viewer, (CategoryQueryColumn)column, editor);
		}
		return null;
	}
	
	private static EditingSupport getCellEditor(ColumnViewer viewer, FixedQueryColumn column, QueryResultsEditor editor){
		if (column.getColumn() == FixedColumns.WAYPOINT_ID ||
			column.getColumn() == FixedColumns.WAYPOINT_TIME ||
			column.getColumn() == FixedColumns.WAYPOINT_X ||
			column.getColumn() == FixedColumns.WAYPOINT_Y ||
			column.getColumn() == FixedColumns.WAYPOINT_DISTANCE ||
			column.getColumn() == FixedColumns.WAYPOINT_COMMENT ||
			column.getColumn() == FixedColumns.WAYPOINT_DIRECTION){
			
			return new WaypointColumnEditor(viewer, column, editor);
		}
		return null;
	}
	
	private static EditingSupport getCellEditor(ColumnViewer viewer, AttributeQueryColumn column, QueryResultsEditor editor){
		return new AttributeColumnEditor(viewer, column, editor);
	}
	
	private static EditingSupport getCellEditor(ColumnViewer viewer, CategoryQueryColumn column, QueryResultsEditor editor){
		return new CategoryColumnEditor(viewer, column, editor);
	}
}
