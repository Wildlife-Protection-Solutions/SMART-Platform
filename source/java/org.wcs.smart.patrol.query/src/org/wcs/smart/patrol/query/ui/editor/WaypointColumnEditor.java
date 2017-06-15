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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.common.ui.edit.AbstractQueryColumnEditor;
import org.wcs.smart.query.ui.editor.IQueryEditor;

/**
 * Column editor for the waypoint columns in the query results table.
 * @author Emily
 *
 */
public class WaypointColumnEditor extends AbstractQueryColumnEditor {

	public WaypointColumnEditor (ColumnViewer viewer, FixedQueryColumn queryColumn, IQueryEditor editor ){
		super(viewer, queryColumn, editor);
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		switch (((FixedQueryColumn)queryColumn).getColumn()) {
		case WAYPOINT_COMMENT:
			return getTextCellEditor();
		case WAYPOINT_DIRECTION:
			return getDoubleCellEditor();
		case WAYPOINT_DISTANCE:
			return getDoubleCellEditor();
		case WAYPOINT_ID:
			return getIntegerCellEditor();
		case WAYPOINT_TIME:
			return getTimeCellEditor();
		case WAYPOINT_X:
			return getDoubleCellEditor();
		case WAYPOINT_Y:
			return getDoubleCellEditor();
		default:
			return null;
		}
	}
}
