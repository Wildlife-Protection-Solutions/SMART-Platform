package org.wcs.smart.observation.query.model.columns;

import org.wcs.smart.query.model.QueryColumn;

public interface IObservationQueryColumnProvider {

	public QueryColumn[] getQueryColumns(String queryTypeKey);
	
	
}
