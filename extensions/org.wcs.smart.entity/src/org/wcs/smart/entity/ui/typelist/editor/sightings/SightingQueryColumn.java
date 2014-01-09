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
package org.wcs.smart.entity.ui.typelist.editor.sightings;

import javax.naming.OperationNotSupportedException;

import org.wcs.smart.entity.query.SightingResultItem;
import org.wcs.smart.entity.ui.typelist.editor.sightings.SightingTableColumns.FixedColumns;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;


/**
 * A column available for output 
 * by a observation query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SightingQueryColumn extends QueryColumn{
	
	public SightingQueryColumn(String name, String key, ColumnType type) {
		super(name, key, type);
	}
	

	@Override
	public Object getValue(IResultItem item) {

		SightingResultItem ri = (SightingResultItem) item;
		if (getKey().equals(FixedColumns.ENTITY_ID.getKey())){
			return ri.getEntityId();
		}else if (getKey().equals(FixedColumns.CA_ID.getKey())){
			return ri.getConservationAreaId();
		}else if (getKey().equals(FixedColumns.CA_NAME.getKey())){
			return ri.getConservationAreaName();
		}else if (getKey().equals(FixedColumns.WAYPOINT_SOURCE.getKey())){
			return ri.getSourceId();
		}else if (getKey().equals(FixedColumns.WAYPOINT_ID.getKey())){
			return ri.getWaypointId();
		}else if (getKey().equals(FixedColumns.WAYPOINT_DATE.getKey())){
			return ri.getWaypointDateTime();
		}else if (getKey().equals(FixedColumns.WAYPOINT_TIME.getKey())){
			return ri.getWaypointDateTime();
		}else if (getKey().equals(FixedColumns.WAYPOINT_X.getKey())){
			return ri.getWaypointX();
		}else if (getKey().equals(FixedColumns.WAYPOINT_Y.getKey())){
			return ri.getWaypointY();
		}else if (getKey().equals(FixedColumns.WAYPOINT_DIRECTION.getKey())){
			return ri.getWaypointDirection();
		}else if (getKey().equals(FixedColumns.WAYPOINT_DISTANCE.getKey())){
			return ri.getWaypointDistance();
		}else if (getKey().equals(FixedColumns.WAYPOINT_COMMENT.getKey())){
			return ri.getWaypointComment();
		}
		
		Object x = ri.getEntityAttribute(getKey());
		if (x != null){
			return x;
		}
		return null;
	}

	@Override
	public QueryColumn clone() {
		throw new IllegalStateException("Cannot clone sighting columns.");
	}
	
}
