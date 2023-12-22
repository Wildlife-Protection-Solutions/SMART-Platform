package org.wcs.smart.asset.query.engine;

import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.QueryColumn;

public class AssetSummaryResults extends SummaryQueryResult implements IGeometryResultItem{

	@Override
	public Geometry asGeometry() {
		return null;
	}

	@Override
	public SimpleFeature toSimpleFeature(
			SimpleFeatureType type, 
			QueryColumn geometryColumn,
			List<QueryColumn> columns) {
		// TODO Auto-generated method stub
		return null;
	}

}
