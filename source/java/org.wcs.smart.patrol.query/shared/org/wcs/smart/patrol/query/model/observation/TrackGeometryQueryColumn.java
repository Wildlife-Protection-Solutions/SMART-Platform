package org.wcs.smart.patrol.query.model.observation;

import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.IGeometryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.IGeometryColumn.Type;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

public class TrackGeometryQueryColumn extends QueryColumn implements IGeometryColumn{

	public static final String KEY = "track:geometry"; //$NON-NLS-1$
	
	public TrackGeometryQueryColumn() {
		super("Track", KEY, ColumnType.GEOMETRY);
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
		TrackGeometryQueryColumn clone = new TrackGeometryQueryColumn();
		return clone;
	}


	@Override
	public Type getGeometryType() {
		return Type.MULTILINESTRING;
	}
}
