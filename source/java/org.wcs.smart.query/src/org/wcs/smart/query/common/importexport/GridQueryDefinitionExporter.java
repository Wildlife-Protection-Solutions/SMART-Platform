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
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.xml.model.QueryPart;
import org.wcs.smart.query.xml.model.QueryType;

/**
 * Definition exporter for a gridded query.
 * 
 * @author Emily
 *
 */
public abstract class GridQueryDefinitionExporter extends DefinitionQueryExporter
		implements IQueryExporter {

	public GridQueryDefinitionExporter() {
	}
	
	/**
	 * @see org.wcs.smart.query.export.DefinitionQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public abstract boolean canExport(Query query);
	/**
	 * @see org.wcs.smart.query.export.DefinitionQueryExporter#writeQuerySpecifics(org.wcs.smart.query.model.Query, org.wcs.smart.query.xml.model.QueryType)
	 */
	@Override
	public void writeQuerySpecifics(Query query, QueryType xmlQuery)
			throws Exception {
		
		GriddedQuery gQuery = (GriddedQuery)query;
		
		QueryPart defPart = new QueryPart();
		defPart.setKey("definition"); //$NON-NLS-1$
		defPart.setValue( gQuery.getQueryDefinition().asQuery() );
		xmlQuery.getQueryPart().add(defPart);

		QueryPart crsPart = new QueryPart();
		crsPart.setKey("crs"); //$NON-NLS-1$
		crsPart.setValue(gQuery.getCrsDefinition());
		xmlQuery.getQueryPart().add(crsPart);
		
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			if (gQuery.getQueryDefinition().getValueFilter() != null){
				processFilter(gQuery.getQueryDefinition().getValueFilter().getFilter(), xmlQuery, s);
			}
			if (gQuery.getQueryDefinition().getRateFilter() != null){
				processFilter(gQuery.getQueryDefinition().getRateFilter().getFilter(), xmlQuery, s);
			}
		}finally{
			s.getTransaction().rollback();
			s.close();
		}
	}
	
	/*
	 * Process the filter
	 */
	protected abstract void processFilter(IFilter f, final QueryType qt, final Session session) throws Exception;

}
