package org.wcs.smart.query.model;

import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.IWaypointQueryResultItem;

public class WaypointGeometryQueryColumn extends QueryColumn implements IGeometryColumn{

	public static final String KEY = "wp:geometry"; //$NON-NLS-1$
	
	public WaypointGeometryQueryColumn() {
		super("Waypoint", KEY, ColumnType.GEOMETRY);
	}

	public boolean isDefaultGeometryColumn() {
		return true;
	}

	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof IGeometryResultItem iitem) {
			return iitem.asGeometry();
		}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryColumn clone() {
		WaypointGeometryQueryColumn clone = new WaypointGeometryQueryColumn();
		return clone;
	}

	@Override
	public Type getGeometryType() {
		return Type.POINT;
	}
}
