package org.wcs.smart.entity.query;

import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

public interface IEntityQueryColumnProvider {

	public QueryColumn[] getQueryColumns(String queryTypeKey, Query query);
	
	
}