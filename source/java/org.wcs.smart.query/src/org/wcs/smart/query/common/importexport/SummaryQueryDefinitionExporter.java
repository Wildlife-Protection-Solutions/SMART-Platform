/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.query.common.importexport;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.date.Last30DaysDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;

/**
 * Summary query definition exporter
 * @author egouge
 * @since 1.0.0
 */
public abstract class SummaryQueryDefinitionExporter extends DefinitionQueryExporter {

	/**
	 * @see org.wcs.smart.query.export.DefinitionQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query instanceof SummaryQuery){
			return true;
		}
		return false;
	}

	/**
	 * @see org.wcs.smart.query.export.DefinitionQueryExporter#writeQuerySpecifics(org.wcs.smart.query.model.Query, org.wcs.smart.query.xml.model.QueryType)
	 */
	@Override
	public void writeQuerySpecifics(Query query, QueryType xmlQuery) throws Exception {
		SummaryQuery summary = (SummaryQuery) query;
		if (summary.getDateFilter() == null){
			summary.setDateFilter(new DateFilter(WaypointDateField.INSTANCE, Last30DaysDateFilter.INSTANCE));
		}
		
		QueryPart defPart = new QueryPart();
		defPart.setKey("definition"); //$NON-NLS-1$
		defPart.setValue( summary.getQuery() );
		
		xmlQuery.getQueryPart().add(defPart);
			
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			if (summary.getQueryDefinition().getValueFilter() != null){
				processFilter(summary.getQueryDefinition().getValueFilter().getFilter(), xmlQuery, s);
			}
			if (summary.getQueryDefinition().getRateFilter() != null){
				processFilter(summary.getQueryDefinition().getRateFilter().getFilter(), xmlQuery, s);
			}
			
			processGroupBy(summary.getQueryDefinition().getRowGroupByPart(), xmlQuery, s);
			processGroupBy(summary.getQueryDefinition().getColumnGroupByPart(), xmlQuery, s);

		}finally{
			s.getTransaction().rollback();
			s.close();
		}
	}

	/**
	 * Exports the group by part information
	 */
	protected abstract void processGroupBy(GroupByPart values, QueryType qt, Session session) throws Exception;
	
	/**
	 * Exports the filter information
	 */
	protected abstract void processFilter(IFilter f, QueryType qt, Session session) throws Exception;

}
