package org.wcs.smart.observation.query.internal;

import java.util.Locale;

import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.observation.query.view.IObservationQueryLabelProvider;

public class ObservationQueryLabelProvider implements
		IObservationQueryLabelProvider {

	@Override
	public String getLabel(Object key, Locale l) {
		if (key instanceof FixedQueryColumn.FixedColumns){
			switch((FixedQueryColumn.FixedColumns)key){
				case CA_ID:return Messages.FixedQueryColumn_CaIdColumnName;
				case CA_NAME: return Messages.FixedQueryColumn_CaNameColumnName;
				case WAYPOINT_SOURCE: return Messages.FixedQueryColumn_WaypointSourceColumnName;
				case WAYPOINT_ID: return Messages.FixedQueryColumn_WaypointIdColumnName;
				case WAYPOINT_DATE: return Messages.FixedQueryColumn_WaypointDateColumnName;
				case WAYPOINT_TIME: return Messages.FixedQueryColumn_WaypointTimeColumnName;
				case WAYPOINT_X: return Messages.FixedQueryColumn_xColumnName;
				case WAYPOINT_Y: return Messages.FixedQueryColumn_yColumnName;
				case WAYPOINT_DIRECTION: return Messages.FixedQueryColumn_DirectionColumnName;
				case WAYPOINT_DISTANCE: return Messages.FixedQueryColumn_DistanceColumnName;
				case WAYPOINT_COMMENT: return Messages.FixedQueryColumn_CommentColumnName;
				case WAYPOINT_OBSERVER: return Messages.FixedQueryColumn_ObserverColumnName;
			}
		}
		return null;
	}

	
}
