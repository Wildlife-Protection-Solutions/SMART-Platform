package org.wcs.smart.incident.ui;

import org.wcs.smart.common.filter.IUpdatableView;

public interface IIncidentFilteringView extends IUpdatableView {

	/**
	 * 
	 * @return the current active filter
	 */
	public IncidentFilter getFilter();
}
