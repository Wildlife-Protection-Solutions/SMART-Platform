package org.wcs.smart.observation.query.ui;

import org.wcs.smart.observation.query.model.ObservationQueryFactory;
import org.wcs.smart.observation.query.model.types.ObservationGridQueryType;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.ui.GriddedEditor;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;

public class ObservationGriddedQueryEditor extends GriddedEditor {

	public static final String ID = "org.wcs.smart.observation.editor.gridded"; //$NON-NLS-1$
	
	@Override
	public GriddedQuery createQuery() {
		return ObservationQueryFactory.createGriddedQuery();
	}

	@Override
	protected IDateFieldFilter[] getDateFilterOptions() {
		return ObservationGridQueryType.validDateFields();
	}

}
