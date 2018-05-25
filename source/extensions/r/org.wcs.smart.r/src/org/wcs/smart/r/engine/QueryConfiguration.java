package org.wcs.smart.r.engine;

import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;

public class QueryConfiguration {

	private Query query;
	private IQueryExporter exporter;
	private DateFilter dateFilter;
	
	public QueryConfiguration(Query query, IQueryExporter exporter, DateFilter dateFilter) {
		this.query = query;
		this.exporter = exporter;
		this.dateFilter = dateFilter;
	}
	
	public Query getQuery() {
		return this.query;
	}
	
	public IQueryExporter getQueryExporter() {
		return this.exporter;
	}
	
	public DateFilter getDateFilter() {
		return this.dateFilter;
	}
}
