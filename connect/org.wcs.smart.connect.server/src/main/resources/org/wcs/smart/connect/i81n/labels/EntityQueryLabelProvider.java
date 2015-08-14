/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i81n.labels;

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
		if (item == FixedQueryColumn.FixedColumns.CA_ID) return "Conservation Area ID";
		if (item == FixedQueryColumn.FixedColumns.CA_NAME) return "Conservation Area Name";
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_SOURCE) return "Waypoint Source";
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_ID) return "Waypoint ID";
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DATE) return "Waypoint Date";
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_TIME) return "Waypoint Time";
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_X) return "X";
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_Y) return "Y";
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION) return "Direction";
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE) return "Distance";
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT) return "Comment";
		if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER) return "Observer";
		
		return null;
	}
}
