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
package org.wcs.smart.entity.query.internal;

import java.util.Locale;

import org.wcs.smart.entity.query.IEntityQueryLabelProvider;
import org.wcs.smart.entity.query.model.columns.FixedQueryColumn;

/**
 * Query entity label provider implementation.
 * 
 * @author Emily
 *
 */
public class EntityQueryLabelProvider implements IEntityQueryLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == FixedQueryColumn.FixedColumns.CA_ID) return Messages.FixedQueryColumn_CaIdColumnName;
		if (item == FixedQueryColumn.FixedColumns.CA_NAME) return Messages.FixedQueryColumn_CaNameColumnName;
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_SOURCE) return Messages.FixedQueryColumn_WaypointSourceColumnName;
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_ID) return Messages.FixedQueryColumn_WaypointIdColumnName;
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DATE) return Messages.FixedQueryColumn_WaypointDateColumnName;
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_TIME) return Messages.FixedQueryColumn_WaypointTimeColumnName;
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_X) return Messages.FixedQueryColumn_xColumnName;
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_Y) return Messages.FixedQueryColumn_yColumnName;
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION) return Messages.FixedQueryColumn_DirectionColumnName;
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE) return Messages.FixedQueryColumn_DistanceColumnName;
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT) return Messages.FixedQueryColumn_CommentColumnName;
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER) return Messages.FixedQueryColumn_ObserverColumnName;
		
		return null;
	}
}
