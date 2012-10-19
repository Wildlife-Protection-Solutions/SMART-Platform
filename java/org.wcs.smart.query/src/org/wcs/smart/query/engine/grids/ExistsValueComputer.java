package org.wcs.smart.query.engine.grids;

import com.vividsolutions.jts.geom.LineString;

public class ExistsValueComputer implements IValueComputer<Boolean> {

	@Override
	public Boolean computeValue(Boolean existingValue, Tile t, Grid gridDef, LineString ls)
			throws Exception {
		
		return Boolean.TRUE;
	}

}
