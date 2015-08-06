package org.wcs.smart.entity.query.internal;

import java.util.Locale;

import org.wcs.smart.entity.IEntityLabelProvider;
import org.wcs.smart.entity.query.model.columns.FixedQueryColumn;

public class EntityQueryLabelProvider implements IEntityLabelProvider {

	

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
