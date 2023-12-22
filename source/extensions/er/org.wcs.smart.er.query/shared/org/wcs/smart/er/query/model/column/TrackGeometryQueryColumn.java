package org.wcs.smart.er.query.model.column;

import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.IGeometryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

public class TrackGeometryQueryColumn extends QueryColumn implements IGeometryColumn{

	
	public static final String KEY = "geometry.default";
	private IGeometryColumn.Type type;
	
	public TrackGeometryQueryColumn(IGeometryColumn.Type type) {
		super("Track", KEY, ColumnType.GEOMETRY);
		this.type = type;
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
		TrackGeometryQueryColumn clone = new TrackGeometryQueryColumn(type);
		return clone;
	}

	@Override
	public Type getGeometryType() {
		return type;
	}
}
