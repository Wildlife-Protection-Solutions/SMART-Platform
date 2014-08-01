package org.wcs.smart.er.query.ui.columns;

import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

public class SurveyCategoryQueryColumn extends CategoryQueryColumn {

	public SurveyCategoryQueryColumn(String name, int level) {
		super(name, level);
	}

	@Override
	public Object getValue(IResultItem item) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryColumn clone() {
		return new SurveyCategoryQueryColumn(getName(), level);
	}

}
