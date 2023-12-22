package org.wcs.smart.asset.query.model;

import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.IGeometryColumn;
import org.wcs.smart.query.model.QueryColumn;

public class PointGeometryQueryColumn extends QueryColumn implements IGeometryColumn{

	public static final String KEY = "geometry.point"; //$NON-NLS-1$
	
	public PointGeometryQueryColumn() {
		super("Point", KEY, ColumnType.GEOMETRY);
	}

	public boolean isDefaultGeometryColumn() {
		return true;
	}

	@Override
	public Object getValue(IResultItem item) {
		return null;
	}

	@Override
	public QueryColumn clone() {
		PointGeometryQueryColumn clone = new PointGeometryQueryColumn();
		return clone;
	}

	@Override
	public Type getGeometryType() {
		return Type.POINT;
	}
}
