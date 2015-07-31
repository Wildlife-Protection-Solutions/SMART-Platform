package org.wcs.smart.er.query.model;

import org.wcs.smart.query.model.QueryColumn;

public interface ISurveyQueryColumnProvider {
	
	/**
	 * 
	 * @param queryTypeKey survey query type key
	 * @param surveyDesignKey survey design associated with query or null
	 * @return
	 */
	public QueryColumn[] getQueryColumns(String queryTypeKey, String surveyDesignKey);
	
}
