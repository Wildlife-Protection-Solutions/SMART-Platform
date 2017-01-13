package org.wcs.smart.i2.query;

import com.vividsolutions.jts.geom.Geometry;

public interface IGeometryResultItem extends IResultItem{

	public Geometry getGeometry();
}
