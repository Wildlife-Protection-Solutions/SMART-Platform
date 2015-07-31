package org.wcs.smart.patrol.query.engine.grids;

import org.wcs.smart.query.common.engine.ICellMerger;

public class PatrolExistsCellMerger implements ICellMerger<Boolean> {

	@Override
	public Boolean mergeCell(Boolean v1, Boolean v2) {
		if (v1 == null ) return v2;
		if (v2 == null ) return v1;
		return v1 || v2;
	}

	@Override
	public Double getFinalValue(Boolean value) {
		if (value) return 1.0;
		return 0.0;
	}

}
