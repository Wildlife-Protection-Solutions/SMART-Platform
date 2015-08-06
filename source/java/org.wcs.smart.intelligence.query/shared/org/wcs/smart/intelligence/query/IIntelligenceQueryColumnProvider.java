package org.wcs.smart.intelligence.query;

import org.wcs.smart.query.model.QueryColumn;

public interface IIntelligenceQueryColumnProvider {

	public QueryColumn[] getQueryColumns(String queryTypeKey);
	
	
}