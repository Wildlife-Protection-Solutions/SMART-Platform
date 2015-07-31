package org.wcs.smart.patrol.query.model;

import org.wcs.smart.query.model.QueryColumn;

public interface IPatrolQueryColumnProvider {

	public QueryColumn[] getQueryColumns(String queryTypeKey);
	
	
}