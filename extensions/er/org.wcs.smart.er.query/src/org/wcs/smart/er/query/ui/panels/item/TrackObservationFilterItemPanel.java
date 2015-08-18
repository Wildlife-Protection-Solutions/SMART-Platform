package org.wcs.smart.er.query.ui.panels.item;

import org.wcs.smart.er.query.model.SurveySummaryQuery;
import org.wcs.smart.query.QueryTypeManager;

public class TrackObservationFilterItemPanel extends FilterItemPanel {
	public static final String ID = "org.wcs.smart.er.query.survey.filterItemPanel.trackObservation"; //$NON-NLS-1$

	public TrackObservationFilterItemPanel() {
		super(QueryTypeManager.INSTANCE.findQueryType(SurveySummaryQuery.KEY));
	}

	@Override
	public String getId() {
		return ID;
	}
}
