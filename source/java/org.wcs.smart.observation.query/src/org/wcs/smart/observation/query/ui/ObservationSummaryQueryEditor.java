package org.wcs.smart.observation.query.ui;

import org.wcs.smart.observation.query.model.ObservationQueryFactory;
import org.wcs.smart.observation.query.model.types.ObservationSummaryQueryType;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.common.ui.SummaryEditor;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;

public class ObservationSummaryQueryEditor extends SummaryEditor {
	
	public static final String ID = "org.wcs.smart.observation.editor.summary"; //$NON-NLS-1$
	
	@Override
	public SummaryQuery createNewQuery() {
		return ObservationQueryFactory.createSummaryQuery();
	}

	@Override
	protected IDateFieldFilter[] getValidDateFilters() {
		return ObservationSummaryQueryType.validDateFields();
	}

}
